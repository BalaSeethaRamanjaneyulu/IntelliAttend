"""
Faculty API routes - complete implementation for Phase 2
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.schemas.faculty_schema import (
    SessionCreateRequest,
    OTPResponse,
    QRGenerateRequest,
    QRTokenResponse,
    LiveStatusResponse,
    AttendanceStats,
    StudentAttendanceStatus
)
from app.services.session_service import SessionService
from app.services.qr_service import QRService
from app.models.attendance import Attendance, AttendanceStatusEnum
from app.models.student import Student

router = APIRouter()


@router.post("/start_session", response_model=OTPResponse)
async def start_session(
    session_data: SessionCreateRequest,
    db: Session = Depends(get_db)
):
    """
    Start new attendance session and generate OTP
    
    Flow:
    1. Faculty selects class and requests new session
    2. Server creates session with 6-digit OTP
    3. OTP expires in 5 minutes
    4. Faculty displays OTP on their device
    5. SmartBoard/Admin enters OTP to link session
    
    Returns:
        OTP, expiration time, and session ID
    """
    # TODO: Get faculty_id from JWT token (for now, use faculty_id=1)
    faculty_id = 1  # Will extract from JWT in future
    
    # Create session
    session = SessionService.create_session(
        db=db,
        faculty_id=faculty_id,
        class_id=session_data.class_id
    )
    
    return OTPResponse(
        otp=session.otp,
        expires_at=session.otp_expires_at,
        session_id=session.session_id
    )


@router.post("/generate_qr", response_model=QRTokenResponse)
async def generate_qr_token(
    qr_request: QRGenerateRequest,
    db: Session = Depends(get_db)
):
    """
    Generate dynamic QR token after OTP verification
    
    Flow:
    1. SmartBoard sends OTP + session_id to verify linkage
    2. Server validates OTP
    3. If valid, generates first QR token (HMAC-SHA256 signed)
    4. Returns token to SmartBoard for display
    5. WebSocket will handle subsequent token refreshes every 5s
    
    Returns:
        QR token string, sequence number, and expiration time
    """
    # Verify OTP
    session = SessionService.verify_otp(
        db=db,
        session_id=qr_request.session_id,
        otp=qr_request.otp
    )
    
    # Generate QR token
    qr_token, sequence = QRService.generate_qr_token(db, session)
    
    # Calculate expiration
    from datetime import datetime, timedelta
    expires_at = datetime.utcnow() + timedelta(seconds=7)
    
    return QRTokenResponse(
        qr_token=qr_token,
        sequence_number=sequence,
        expires_at=expires_at
    )


@router.get("/live_status/{session_id}", response_model=LiveStatusResponse)
async def get_live_status(
    session_id: str,
    db: Session = Depends(get_db)
):
    """
    Get live attendance status for active session
    
    Used by:
    - Faculty app to monitor attendance in real-time
    - SmartBoard portal to display stats
    
    Returns:
        Session status, attendance statistics, and student list with statuses
    """
    # Get active session
    session = SessionService.get_active_session(db, session_id)
    
    # Get all students in the class
    students = db.query(Student).filter(
        Student.class_id == session.class_id
    ).all()
    
    # Get attendance records for this session
    attendance_records = db.query(Attendance).filter(
        Attendance.session_id == session.id
    ).all()
    
    # Create attendance lookup
    attendance_map = {att.student_id: att for att in attendance_records}
    
    # Build student status list
    student_statuses = []
    present_count = 0
    pending_count = 0
    failed_count = 0
    
    for student in students:
        if student.id in attendance_map:
            att = attendance_map[student.id]
            status_str = att.status.value
            confidence = att.confidence_score
            submitted_at = att.submitted_at
            
            if att.status == AttendanceStatusEnum.PRESENT:
                present_count += 1
            elif att.status == AttendanceStatusEnum.PENDING:
                pending_count += 1
            elif att.status == AttendanceStatusEnum.FAILED:
                failed_count += 1
        else:
            status_str = "absent"
            confidence = None
            submitted_at = None
        
        student_statuses.append(StudentAttendanceStatus(
            student_id=student.student_id,
            name=student.name,
            status=status_str,
            confidence_score=confidence,
            submitted_at=submitted_at
        ))
    
    # Calculate stats
    total_students = len(students)
    present_percentage = (present_count / total_students * 100) if total_students > 0 else 0
    
    stats = AttendanceStats(
        total_students=total_students,
        present_count=present_count,
        pending_count=pending_count,
        failed_count=failed_count,
        present_percentage=round(present_percentage, 2)
    )
    
    return LiveStatusResponse(
        session_id=session.session_id,
        status=session.status.value,
        stats=stats,
        students=student_statuses
    )
