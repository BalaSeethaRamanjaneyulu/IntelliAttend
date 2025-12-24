"""
Validation Failure Logging Endpoint
Receives validation failures from mobile app for analysis
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from datetime import datetime
from firebase_admin import firestore

router = APIRouter()

class ValidationFailureReport(BaseModel):
    session_id: str
    scanned_payload: str
    scan_timestamp: int  # Unix timestamp
    validation_error: str
    cached_token_preview: str

@router.post("/validation-failure")
async def log_validation_failure(report: ValidationFailureReport):
    """
    Log validation failures for debugging and analysis
    
    This helps diagnose:
    - Clock sync issues
    - QR scanning problems
    - Token rotation timing issues
    """
    db = firestore.client()
    
    try:
        # Calculate server time offset
        server_time = datetime.now().timestamp()
        time_offset = server_time - report.scan_timestamp
        
        # Log to console for immediate visibility
        print("=" * 60)
        print("🔴 VALIDATION FAILURE REPORT")
        print(f"Session ID: {report.session_id}")
        print(f"Scan Time: {datetime.fromtimestamp(report.scan_timestamp)}")
        print(f"Server Time: {datetime.fromtimestamp(server_time)}")
        print(f"Time Offset: {time_offset:.2f}s")
        print(f"Error: {report.validation_error}")
        print(f"Scanned: {report.scanned_payload[:50]}...")
        print(f"Cached: {report.cached_token_preview[:50]}...")
        print("=" * 60)
        
        # Store in Firestore for analysis
        doc_data = {
            "session_id": report.session_id,
            "scanned_payload": report.scanned_payload,
            "scan_timestamp": report.scan_timestamp,
            "server_timestamp": server_time,
            "time_offset_seconds": time_offset,
            "validation_error": report.validation_error,
            "cached_token_preview": report.cached_token_preview,
            "logged_at": datetime.now()
        }
        
        db.collection("ValidationFailures").add(doc_data)
        
        return {
            "success": True,
            "message": "Validation failure logged",
            "time_offset": time_offset
        }
        
    except Exception as e:
        print(f"Error logging validation failure: {e}")
        raise HTTPException(status_code=500, detail=str(e))
