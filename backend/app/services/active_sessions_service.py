"""
ActiveSessions Service - Manages Firestore real-time token synchronization
Handles token rotation, storage, and cleanup for active attendance sessions
"""
import firebase_admin
from firebase_admin import firestore
from app.core import firebase as firebase_init
from app.utils.token_generator import TokenGenerator
from datetime import datetime, timezone
from typing import Dict, Optional
import asyncio


class ActiveSessionsService:
    """
    Manages the ActiveSessions Firestore collection for real-time QR token distribution
    
    Collection Structure:
    ActiveSessions/{session_id}:
        - currentToken: Latest valid token
        - previousToken: Previous token (for grace period)
        - currentTimestamp: Current token generation time
        - previousTimestamp: Previous token generation time
        - currentExpiry: When current token expires
        - previousExpiry: When previous token expires
        - serverTime: Server timestamp (for clock sync)
        - sequence: Rotation sequence number
        - sessionId: Reference to parent session
        - status: 'active' | 'expired'
    """
    
    COLLECTION_NAME = "ActiveSessions"
    
    @staticmethod
    def _get_db():
        """Get Firestore client"""
        if not firebase_admin._apps:
            firebase_init.initialize_firebase()
        return firestore.client()
    
    @staticmethod
    async def create_active_session(
        session_id: str,
        class_id: str = "UNKNOWN",
        room_id: str = "UNKNOWN",
        subject_id: str = "UNKNOWN"
    ) -> Dict:
        """
        Initialize ActiveSessions document for a new session
        
        Args:
            session_id: Active session identifier
            class_id: Class/section identifier
            room_id: Physical classroom
            subject_id: Course code
            
        Returns:
            Dict with initial token data
        """
        db = ActiveSessionsService._get_db()
        
        # STABILIZATION: Expire any existing active sessions to prevent "phantom" rotations
        try:
            old_sessions = db.collection(ActiveSessionsService.COLLECTION_NAME)\
                .where('status', '==', 'active')\
                .stream()
            
            for doc in old_sessions:
                if doc.id != session_id:
                    doc.reference.update({'status': 'expired', 'endedAt': firestore.SERVER_TIMESTAMP})
                    print(f"🧹 Cleaned up orphaned session: {doc.id}")
        except Exception as e:
            print(f"⚠️ Warning during session cleanup: {e}")

        # Generate initial token - Sequence ALWAYS starts at 1
        token_data = TokenGenerator.generate_token(
            session_id=session_id,
            class_id=class_id,
            room_id=room_id,
            subject_id=subject_id,
            sequence=1
        )
        
        # Create ActiveSessions document
        doc_data = {
            'sessionId': session_id,
            'currentToken': token_data['token'],
            'previousToken': None,
            'currentTimestamp': token_data['timestamp'],
            'previousTimestamp': None,
            'currentExpiry': token_data['expiry'],
            'previousExpiry': None,
            'serverTime': firestore.SERVER_TIMESTAMP,
            'sequence': 1,  # Explicitly set to 1
            'status': 'active',
            'classId': class_id,
            'roomId': room_id,
            'subjectId': subject_id,
            'createdAt': firestore.SERVER_TIMESTAMP,
            'lastRotation': firestore.SERVER_TIMESTAMP
        }
        
        active_session_ref = db.collection(ActiveSessionsService.COLLECTION_NAME).document(session_id)
        active_session_ref.set(doc_data)
        
        print(f"✅ ActiveSession created: {session_id} (Seq: 1)")
        
        return token_data
    
    @staticmethod
    async def rotate_token(session_id: str) -> Optional[Dict]:
        """
        Rotate token for a session (called every 5 seconds by scheduler)
        
        Moves currentToken → previousToken
        Generates new currentToken
        
        Args:
            session_id: Session to rotate token for
            
        Returns:
            New token data if successful, None if session not active
        """
        db = ActiveSessionsService._get_db()
        
        active_session_ref = db.collection(ActiveSessionsService.COLLECTION_NAME).document(session_id)
        active_session_doc = active_session_ref.get()
        
        if not active_session_doc.exists:
            print(f"⚠️ ActiveSession not found: {session_id}")
            return None
        
        active_data = active_session_doc.to_dict()
        
        if active_data.get('status') != 'active':
            print(f"⚠️ ActiveSession not active: {session_id}")
            return None
        
        # Generate new token
        next_sequence = active_data.get('sequence', 0) + 1
        new_token_data = TokenGenerator.generate_token(
            session_id=session_id,
            class_id=active_data.get('classId', 'UNKNOWN'),
            room_id=active_data.get('roomId', 'UNKNOWN'),
            subject_id=active_data.get('subjectId', 'UNKNOWN'),
            sequence=next_sequence
        )
        
        # Update document: current → previous, new → current
        update_data = {
            'previousToken': active_data.get('currentToken'),
            'previousTimestamp': active_data.get('currentTimestamp'),
            'previousExpiry': active_data.get('currentExpiry'),
            'currentToken': new_token_data['token'],
            'currentTimestamp': new_token_data['timestamp'],
            'currentExpiry': new_token_data['expiry'],
            'sequence': next_sequence,
            'serverTime': firestore.SERVER_TIMESTAMP,
            'lastRotation': firestore.SERVER_TIMESTAMP
        }
        
        active_session_ref.update(update_data)
        
        print(f"🔄 Token rotated for {session_id} - Seq: {next_sequence}")
        
        return new_token_data
    
    @staticmethod
    async def get_active_session(session_id: str) -> Optional[Dict]:
        """
        Get current ActiveSession data
        
        Args:
            session_id: Session identifier
            
        Returns:
            ActiveSession document data or None
        """
        db = ActiveSessionsService._get_db()
        
        active_session_ref = db.collection(ActiveSessionsService.COLLECTION_NAME).document(session_id)
        active_session_doc = active_session_ref.get()
        
        if not active_session_doc.exists:
            return None
        
        return active_session_doc.to_dict()
    
    @staticmethod
    async def end_active_session(session_id: str) -> bool:
        """
        Mark ActiveSession as expired
        
        Args:
            session_id: Session to end
            
        Returns:
            True if successful, False otherwise
        """
        db = ActiveSessionsService._get_db()
        
        active_session_ref = db.collection(ActiveSessionsService.COLLECTION_NAME).document(session_id)
        
        if not active_session_ref.get().exists:
            return False
        
        active_session_ref.update({
            'status': 'expired',
            'endedAt': firestore.SERVER_TIMESTAMP
        })
        
        print(f"⏹️ ActiveSession ended: {session_id}")
        
        return True
    
    @staticmethod
    async def get_all_active_sessions() -> list:
        """
        Get all active session IDs for token rotation scheduler
        Includes stabilization check to expire stale sessions
        
        Returns:
            List of session IDs with status='active'
        """
        db = ActiveSessionsService._get_db()
        
        active_sessions = db.collection(ActiveSessionsService.COLLECTION_NAME) \
            .where('status', '==', 'active') \
            .stream()
        
        active_ids = []
        now = datetime.now(timezone.utc).timestamp()
        
        for doc in active_sessions:
            data = doc.to_dict()
            created_at = data.get('createdAt')
            
            # STABILIZATION: If session is orphaned (created > 2 hours ago), expire it
            if created_at:
                # Handle both datetime objects and timestamps
                if hasattr(created_at, 'timestamp'):
                    created_ts = created_at.timestamp()
                else:
                    created_ts = float(created_at)
                
                if (now - created_ts) > 7200: # 2 hours
                    doc.reference.update({'status': 'expired', 'endedAt': firestore.SERVER_TIMESTAMP})
                    print(f"🧹 Expired stale orphaned session: {doc.id}")
                    continue
            
            active_ids.append(doc.id)
        
        return active_ids
    
    @staticmethod
    async def validate_token_against_session(token: str, session_id: str) -> tuple[bool, Optional[str]]:
        """
        Validate a scanned token against a specific session's active tokens
        
        This is for additional server-side validation after mobile app
        has already done local validation
        
        Args:
            token: Scanned QR token
            session_id: Expected session ID
            
        Returns:
            (is_valid, error_message)
        """
        # First validate token structure and timestamp
        is_valid, payload, error_msg = TokenGenerator.validate_token(token)
        
        if not is_valid:
            return False, error_msg
        
        # Check session ID matches
        token_session_id = payload.get('sid')
        if token_session_id != session_id:
            return False, f"Token session mismatch: expected {session_id}, got {token_session_id}"
        
        # Get ActiveSession data
        active_session_data = await ActiveSessionsService.get_active_session(session_id)
        
        if not active_session_data:
            return False, "ActiveSession not found"
        
        if active_session_data.get('status') != 'active':
            return False, "Session is not active"
        
        # Check if token matches current OR previous token (grace period)
        current_token = active_session_data.get('currentToken')
        previous_token = active_session_data.get('previousToken')
        
        if token == current_token or token == previous_token:
            return True, None
        
        return False, "Token does not match current or previous session tokens"
