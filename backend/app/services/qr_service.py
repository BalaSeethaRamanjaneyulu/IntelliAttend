"""
QR Token generation service with HMAC-SHA256 signing
Generates secure, time-limited QR tokens for attendance verification
"""
import hmac
import hashlib
import base64
import json
from datetime import datetime
from typing import Tuple, Dict, Any
from sqlalchemy.orm import Session

from app.models.session import Session as SessionModel
from app.core.config import settings
from app.core.constants import QR_TOKEN_PREFIX


class QRService:
    """Handle QR token generation and validation"""
    
    @staticmethod
    def generate_qr_token(
        db: Session,
        session: SessionModel
    ) -> Tuple[str, int]:
        """
        Generate a cryptographically signed QR token with rich payload
        
        Token format: IATT_<base64_json_payload>_<signature>
        
        Payload (JSON):
        {
            "sid": session_id,
            "cid": class_id,
            "rid": room_id,
            "sub": subject_id (class_code),
            "seq": sequence,
            "ts": timestamp
        }
        
        Args:
            db: Database session
            session: Active session to generate token for
            
        Returns:
            Tuple of (qr_token, sequence_number)
        """
        # Increment sequence number
        session.qr_sequence_number += 1
        sequence = session.qr_sequence_number
        
        # Current timestamp (Unix epoch)
        timestamp = int(datetime.utcnow().timestamp())
        
        # Prepare rich payload
        # Ensure relationships are loaded or accessible
        class_obj = session.class_obj
        room_id = class_obj.room_id if class_obj else None
        subject_id = class_obj.class_code if class_obj else "UNKNOWN"
        
        payload_data = {
            "sid": session.session_id,
            "cid": session.class_id,
            "rid": room_id,
            "sub": subject_id,
            "seq": sequence,
            "ts": timestamp
        }
        
        # Serialize and encode payload
        json_str = json.dumps(payload_data, separators=(',', ':'))
        payload_b64 = base64.urlsafe_b64encode(json_str.encode('utf-8')).decode('utf-8').rstrip('=')
        
        # Generate HMAC signature
        signature = QRService._generate_signature(payload_b64)
        
        # Construct final token
        qr_token = f"{QR_TOKEN_PREFIX}_{payload_b64}_{signature}"
        
        # Update session
        session.current_qr_token = qr_token
        db.commit()
        
        return qr_token, sequence
    
    @staticmethod
    def _generate_signature(payload: str) -> str:
        """
        Generate HMAC-SHA256 signature for payload
        
        Args:
            payload: String data to sign
            
        Returns:
            Base64-encoded signature (URL-safe, truncated to 16 chars)
        """
        # Create HMAC using JWT secret as key
        key = settings.JWT_SECRET.encode('utf-8')
        message = payload.encode('utf-8')
        
        signature_bytes = hmac.new(key, message, hashlib.sha256).digest()
        
        # Base64 encode (URL-safe) and truncate to 16 characters
        signature_b64 = base64.urlsafe_b64encode(signature_bytes).decode('utf-8')
        return signature_b64[:16]
    
    @staticmethod
    def verify_qr_token(token: str, session_id: str) -> Dict[str, Any]:
        """
        Verify QR token signature and expiration
        
        Args:
            token: QR token to verify
            session_id: Expected session ID
            
        Returns:
            Dict with verification result
        """
        try:
            # Parse token
            parts = token.split('_')
            if len(parts) != 3:  # IATT_payload_signature
                return {"valid": False, "reason": "Invalid token format"}
            
            prefix, payload_b64, signature = parts
            
            # Verify prefix
            if prefix != QR_TOKEN_PREFIX:
                return {"valid": False, "reason": "Invalid token prefix"}
            
            # Verify signature first
            expected_signature = QRService._generate_signature(payload_b64)
            if signature != expected_signature:
                return {"valid": False, "reason": "Invalid signature"}
            
            # Decode payload
            try:
                # Add padding if needed
                padding = '=' * (-len(payload_b64) % 4)
                json_str = base64.urlsafe_b64decode(payload_b64 + padding).decode('utf-8')
                payload = json.loads(json_str)
            except Exception:
                return {"valid": False, "reason": "Invalid payload encoding"}
            
            # Verify session ID matches
            if payload.get("sid") != session_id:
                return {"valid": False, "reason": "Session ID mismatch"}
            
            # Extract timestamp and sequence
            timestamp = payload.get("ts")
            sequence = payload.get("seq")
            
            if timestamp is None or sequence is None:
                return {"valid": False, "reason": "Missing timestamp or sequence"}
                
            # Check token expiration (7 seconds)
            current_time = int(datetime.utcnow().timestamp())
            if current_time - timestamp > settings.QR_TOKEN_EXPIRY_SECONDS:
                return {
                    "valid": False,
                    "reason": "Token expired",
                    "timestamp": timestamp,
                    "sequence": sequence
                }
            
            # Token is valid
            return {
                "valid": True,
                "timestamp": timestamp,
                "sequence": sequence,
                "payload": payload
            }
            
        except Exception as e:
            return {"valid": False, "reason": f"Token parsing error: {str(e)}"}
    
    @staticmethod
    def is_token_replay(token: str, used_tokens_cache: set) -> bool:
        """
        Check if token has been used before (replay attack detection)
        """
        return token in used_tokens_cache

