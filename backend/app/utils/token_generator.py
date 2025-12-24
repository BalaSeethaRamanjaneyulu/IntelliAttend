"""
Token Generator for Dynamic QR Codes
Generates secure, timestamped, encrypted tokens for attendance verification
"""
import hmac
import hashlib
import base64
import json
from datetime import datetime, timezone
from typing import Dict, Optional, Tuple
from app.core.config import settings


class TokenGenerator:
    """
    Generates and validates secure QR tokens with embedded timestamps
    
    Token Format: QR_{base64_payload}_{signature}
    Payload contains: session_id, timestamp, sequence, room_id, etc.
    """
    
    QR_TOKEN_PREFIX = "QR"
    TOKEN_VALIDITY_SECONDS = 5
    GRACE_PERIOD_SECONDS = 2  # Additional buffer for clock sync issues
    
    @staticmethod
    def generate_token(
        session_id: str,
        class_id: str = "UNKNOWN",
        room_id: str = "UNKNOWN",
        subject_id: str = "UNKNOWN",
        sequence: int = 0
    ) -> Dict[str, any]:
        """
        Generate a new QR token with embedded timestamp
        
        Args:
            session_id: Active session identifier
            class_id: Class/section identifier
            room_id: Physical classroom location
            subject_id: Course/subject code
            sequence: Rotation sequence number
            
        Returns:
            Dict containing:
                - token: Full encrypted QR token string
                - timestamp: Unix timestamp of generation
                - expiry: Unix timestamp when token expires
                - sequence: Sequence number
        """
        timestamp = int(datetime.now(timezone.utc).timestamp())
        expiry = timestamp + TokenGenerator.TOKEN_VALIDITY_SECONDS
        
        # Construct payload
        payload_data = {
            "sid": session_id,
            "cid": class_id,
            "rid": room_id,
            "sub": subject_id,
            "seq": sequence,
            "ts": timestamp  # CRITICAL: Timestamp for validation
        }
        
        # Serialize and encode payload
        json_str = json.dumps(payload_data, separators=(',', ':'))
        payload_b64 = base64.urlsafe_b64encode(
            json_str.encode('utf-8')
        ).decode('utf-8').rstrip('=')
        
        # Generate HMAC signature
        signature = TokenGenerator._generate_signature(payload_b64)
        
        # Construct final token
        token = f"{TokenGenerator.QR_TOKEN_PREFIX}_{payload_b64}_{signature}"
        
        return {
            "token": token,
            "timestamp": timestamp,
            "expiry": expiry,
            "sequence": sequence,
            "payload": payload_data
        }
    
    @staticmethod
    def _generate_signature(payload: str) -> str:
        """
        Generate HMAC-SHA256 signature for payload
        
        Args:
            payload: Base64-encoded payload string
            
        Returns:
            Base64-encoded signature (truncated to 16 chars for QR size)
        """
        secret_key = settings.QR_SECRET_KEY.encode('utf-8')
        signature = hmac.new(
            secret_key,
            payload.encode('utf-8'),
            hashlib.sha256
        ).digest()
        
        # Encode and truncate for QR size optimization
        sig_b64 = base64.urlsafe_b64encode(signature).decode('utf-8').rstrip('=')
        return sig_b64[:16]  # First 16 chars provide sufficient security
    
    @staticmethod
    def validate_token(token: str) -> Tuple[bool, Optional[Dict], Optional[str]]:
        """
        Validate QR token signature and timestamp
        
        Args:
            token: The scanned QR token string
            
        Returns:
            Tuple of (is_valid, payload_dict, error_message)
            - is_valid: True if token is valid and not expired
            - payload_dict: Decoded payload data if valid, None otherwise
            - error_message: Error description if invalid, None if valid
        """
        try:
            # Parse token format
            parts = token.split('_')
            if len(parts) != 3:
                return False, None, "Invalid token format"
            
            prefix, payload_b64, signature = parts
            
            if prefix != TokenGenerator.QR_TOKEN_PREFIX:
                return False, None, "Invalid token prefix"
            
            # Verify signature
            expected_signature = TokenGenerator._generate_signature(payload_b64)
            if signature != expected_signature:
                return False, None, "Invalid signature - token may be tampered"
            
            # Decode payload
            # Add padding if needed for base64 decoding
            payload_b64_padded = payload_b64 + '=' * (4 - len(payload_b64) % 4)
            payload_json = base64.urlsafe_b64decode(payload_b64_padded).decode('utf-8')
            payload_data = json.loads(payload_json)
            
            # Validate timestamp
            token_timestamp = payload_data.get('ts')
            if not token_timestamp:
                return False, None, "Missing timestamp in token"
            
            current_timestamp = int(datetime.now(timezone.utc).timestamp())
            age_seconds = current_timestamp - token_timestamp
            
            # Check if token is within validity window + grace period
            max_age = TokenGenerator.TOKEN_VALIDITY_SECONDS + TokenGenerator.GRACE_PERIOD_SECONDS
            
            if age_seconds < 0:
                return False, None, f"Token from future (clock skew detected: {abs(age_seconds)}s)"
            
            if age_seconds > max_age:
                return False, None, f"Token expired ({age_seconds}s old, max {max_age}s)"
            
            # Token is valid!
            return True, payload_data, None
            
        except Exception as e:
            return False, None, f"Token validation error: {str(e)}"
    
    @staticmethod
    def extract_session_id(token: str) -> Optional[str]:
        """
        Extract session ID from token without full validation
        Useful for quick lookups before full validation
        
        Args:
            token: QR token string
            
        Returns:
            Session ID if extractable, None otherwise
        """
        try:
            parts = token.split('_')
            if len(parts) != 3:
                return None
            
            payload_b64 = parts[1]
            payload_b64_padded = payload_b64 + '=' * (4 - len(payload_b64) % 4)
            payload_json = base64.urlsafe_b64decode(payload_b64_padded).decode('utf-8')
            payload_data = json.loads(payload_json)
            
            return payload_data.get('sid')
            
        except Exception:
            return None
