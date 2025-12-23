"""
Attendance API routes - complete implementation for Phase 3
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.schemas.attendance_schema import (
    AttendanceSubmitRequest,
    AttendanceSubmitResponse,
    VerificationResult
)
from app.services.verification_service import VerificationService
from app.services.session_service import SessionService
from app.models.student import Student
from app.models.attendance import Attendance

router = APIRouter()


@router.post("/submit", response_model=AttendanceSubmitResponse)
async def submit_attendance(
    attendance_data: AttendanceSubmitRequest,
    db: Session = Depends(get_db)
):
    """
    Submit attendance with multi-factor verification
    
    Student Flow:
    1. Student receives notification 3 min before class
    2. App starts "warm scan" - collects BLE, Wi-Fi, GPS samples
    3. Student opens app, places fingerprint (biometric auth)
    4. Camera activates, student scans QR from SmartBoard
    5. App combines QR token + prewarm scan data
    6. Sends everything to this endpoint
    7. Server verifies all factors and returns result
    
    Verification Process:
    - QR token: HMAC signature + 7-second expiry (40%)
    - BLE proximity: ≥2 beacon hits with RSSI > -70dBm (30%)
    - Wi-Fi: BSSID matching (20%)
    - GPS: Distance < geofence radius (10%)
    - Decision: confidence ≥ 0.6 → PRESENT, otherwise FAILED
    
    Returns:
        Verification result with confidence breakdown
    """
    # TODO: Get student_id from JWT token (for now, passed in request)
    # In production: student_id = get_current_user(token).id
    
    # Get student
    student = db.query(Student).filter(
        Student.student_id == attendance_data.student_id
    ).first()
    
    if not student:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Student not found"
        )
    
    # Get active session
    session = SessionService.get_active_session(db, attendance_data.session_id)
    
    # Check if student already submitted for this session
    existing = db.query(Attendance).filter(
        Attendance.session_id == session.id,
        Attendance.student_id == student.id
    ).first()
    
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Attendance already submitted for this session"
        )
    
    # Verify attendance using multi-factor verification
    verification = VerificationService.verify_attendance(
        db=db,
        student_id=student.id,
        session=session,
        qr_token=attendance_data.qr_token,
        scan_data=attendance_data.scan_samples
    )
    
    # Save attendance record and scan log
    attendance = VerificationService.save_attendance_record(
        db=db,
        student_id=student.id,
        session_id=session.id,
        verification=verification,
        scan_data=attendance_data.scan_samples,
        qr_token=attendance_data.qr_token
    )
    
    # Broadcast update via WebSocket
    try:
        from app.api.v1.websocket import broadcast_attendance_update
        
        # Get fresh session data (stats + students)
        live_data = SessionService.get_live_session_data(db, session)
        
        await broadcast_attendance_update(
            session.session_id, 
            live_data["stats"],
            live_data["students"]
        )
    except Exception as e:
        print(f"Failed to broadcast attendance update: {e}")
    
    return AttendanceSubmitResponse(
        success=True,
        attendance_id=attendance.id,
        verification=verification
    )


@router.get("/verify/{attendance_id}", response_model=VerificationResult)
async def get_verification_details(
    attendance_id: int,
    db: Session = Depends(get_db)
):
    """
    Get detailed verification results for an attendance record
    
    Used by:
    - Faculty to review failed/pending submissions
    - Students to see why they failed
    - Admin for audit purposes
    
    Returns:
        Complete verification breakdown with all factor scores
    """
    attendance = db.query(Attendance).filter(
        Attendance.id == attendance_id
    ).first()
    
    if not attendance:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Attendance record not found"
        )
    
    # Get scan log for notes
    from app.models.scan_log import ScanLog
    scan_log = db.query(ScanLog).filter(
        ScanLog.attendance_id == attendance_id
    ).first()
    
    return VerificationResult(
        status=attendance.status.value,
        confidence_score=attendance.confidence_score,
        qr_valid=attendance.qr_valid,
        ble_score=attendance.ble_score,
        wifi_score=attendance.wifi_score,
        gps_score=attendance.gps_score,
        verification_notes=scan_log.verification_notes if scan_log else "No notes available"
    )
