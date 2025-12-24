"""
Session service for managing attendance sessions
"""
from fastapi import HTTPException, status
import firebase_admin
from firebase_admin import firestore
from app.core import firebase
from datetime import datetime
import secrets

class SessionService:
    """Handle session management using Firestore"""
    
    @staticmethod
    def _get_db():
        """Get Firestore client"""
        if not firebase_admin._apps:
            firebase.initialize_firebase()
        return firestore.client()

    @staticmethod
    async def verify_otp_and_mark_faculty(otp: str):
        """
        Verify OTP and mark faculty attendance
        
        This is the CRITICAL dual-purpose function:
        1. Validates OTP to authorize SmartBoard
        2. Logs faculty attendance (proves physical presence)
        
        Args:
            otp: 6-digit OTP entered on SmartBoard
            
        Returns:
            dict with session_id, faculty_id, and session details
        """
        db = SessionService._get_db()
        
        # Find active session with this OTP
        sessions_ref = db.collection('sessions')
        query = sessions_ref.where('otp', '==', otp).where('status', '==', 'active').limit(1)
        
        sessions = list(query.stream())
        
        if not sessions:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Invalid OTP or no active session found"
            )
        
        session_doc = sessions[0]
        session_data = session_doc.to_dict()
        faculty_id = session_data.get('faculty_id')
        
        if not faculty_id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Session has no associated faculty"
            )
        
        # CRITICAL: Mark faculty as PRESENT
        # This proves the faculty member physically went to the SmartBoard
        faculty_attendance_ref = db.collection('faculty_attendance').document()
        faculty_attendance_ref.set({
            'faculty_id': faculty_id,
            'session_id': session_doc.id,
            'timestamp': firestore.SERVER_TIMESTAMP,
            'status': 'present',
            'verified_by': 'smartboard_otp',
            'otp_used': otp,
            'course_id': session_data.get('course_id'),
            'device_id': session_data.get('device_id', 'unknown')
        })
        
        # Update session to mark it as "linked to SmartBoard"
        session_doc.reference.update({
            'smartboard_linked': True,
            'smartboard_link_time': firestore.SERVER_TIMESTAMP
        })
        
        return {
            'success': True,
            'session_id': session_doc.id,
            'faculty_id': faculty_id,
            'course_id': session_data.get('course_id'),
            'start_time': session_data.get('start_time'),
            'message': f'Faculty {faculty_id} marked present and SmartBoard authorized'
        }

    @staticmethod
    async def create_session(faculty_id: str, course_id: str, slot_id: str = None, device_id: str = None, biometric_verified: bool = False):
        """
        Create a new attendance session with schedule validation
        
        Args:
            faculty_id: Faculty member starting the session
            course_id: Course code
            slot_id: Timetable slot ID (for validation)
            device_id: Optional device identifier
            biometric_verified: Whether biometric auth was completed
            
        Returns:
            dict with session_id and otp
        """
        from app.services.schedule_service import ScheduleService
        from app.services.active_sessions_service import ActiveSessionsService
        
        db = SessionService._get_db()
        
        # If slot_id provided, validate schedule
        if slot_id:
            validation = await ScheduleService.validate_session_start(faculty_id, slot_id)
            slot_data = validation['slot_data']
            
            # Use slot data for session
            course_id = slot_data['subject_id']
            section_id = slot_data['section_id']
            classroom_id = slot_data['classroom_id']
        else:
            # Legacy mode without slot validation
            section_id = None
            classroom_id = None
        
        # Generate 6-digit OTP
        otp = ''.join([str(secrets.randbelow(10)) for _ in range(6)])
        
        # Get classroom details if available
        if classroom_id:
            classroom_ref = db.collection('classrooms').document(classroom_id)
            classroom_doc = classroom_ref.get()
            classroom_data = classroom_doc.to_dict() if classroom_doc.exists else {}
        else:
            classroom_data = {}
        
        # Create session document
        session_data = {
            'faculty_id': faculty_id,
            'subject_id': course_id,
            'section_id': section_id,
            'classroom_id': classroom_id,
            'slot_id': slot_id,
            'device_id': device_id,
            'otp': otp,
            'status': 'active',
            'biometric_verified': biometric_verified,
            'smartboard_linked': False,
            'created_at': firestore.SERVER_TIMESTAMP
        }
        
        # Add location data if classroom exists
        if classroom_data:
            session_data.update({
                'campus_id': classroom_data.get('campus_id'),
                'block_id': classroom_data.get('block_id'),
                'floor_id': classroom_data.get('floor_id'),
                'smartboard_id': classroom_data.get('devices', {}).get('smartboard_id'),
                'wifi_router_id': classroom_data.get('devices', {}).get('wifi_router_id'),
                'bluetooth_beacon_id': classroom_data.get('devices', {}).get('bluetooth_beacon_id'),
                'expected_location': {
                    'latitude': classroom_data.get('coordinates', {}).get('latitude'),
                    'longitude': classroom_data.get('coordinates', {}).get('longitude'),
                    'radius_meters': 50
                }
            })
        
        # Add timing
        if slot_id and slot_data:
            session_data['scheduled_start'] = slot_data.get('start_time')
            session_data['actual_start'] = firestore.SERVER_TIMESTAMP
        
        session_ref = db.collection('sessions').document()
        session_ref.set(session_data)
        
        session_id = session_ref.id
        
        # Initialize ActiveSessions collection for real-time token distribution
        # This enables Firestore listeners on mobile apps
        try:
            await ActiveSessionsService.create_active_session(
                session_id=session_id,
                class_id=section_id or 'UNKNOWN',
                room_id=classroom_id or 'UNKNOWN',
                subject_id=course_id
            )
        except Exception as e:
            print(f"⚠️ Failed to create ActiveSession: {e}")
            # Continue anyway - WebSocket will still work
        
        return {
            'session_id': session_id,
            'otp': otp,
            'classroom_id': classroom_id,
            'room_number': classroom_data.get('room_number'),
            'message': 'Session created. Enter OTP on SmartBoard to start.'
        }

    @staticmethod
    async def get_session_details(session_id: str):
        """Get session details"""
        db = SessionService._get_db()
        
        session_ref = db.collection('sessions').document(session_id)
        session_doc = session_ref.get()
        
        if not session_doc.exists:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Session not found"
            )
        
        return session_doc.to_dict()

    @staticmethod
    async def end_session(session_id: str):
        """End an active session"""
        from app.services.active_sessions_service import ActiveSessionsService
        
        db = SessionService._get_db()
        
        session_ref = db.collection('sessions').document(session_id)
        session_ref.update({
            'status': 'ended',
            'end_time': firestore.SERVER_TIMESTAMP
        })
        
        # Also end ActiveSession for cleanup
        try:
            await ActiveSessionsService.end_active_session(session_id)
        except Exception as e:
            print(f"⚠️ Failed to end ActiveSession: {e}")
        
        return {'success': True, 'message': 'Session ended'}
