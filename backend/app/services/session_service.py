"""
Session management service for attendance sessions
Handles OTP generation, session creation, and lifecycle
"""
import random
import string
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from fastapi import HTTPException, status

from app.models.session import Session as SessionModel, SessionStatusEnum
from app.models.faculty import Faculty
from app.models.class_model import Class
from app.core.config import settings


class SessionService:
    """Handle session creation and management"""
    
    @staticmethod
    def generate_otp(length: int = 6) -> str:
        """
        Generate a random numeric OTP
        
        Args:
            length: Number of digits in OTP (default 6)
            
        Returns:
            String of random digits
        """
        return ''.join(random.choices(string.digits, k=length))
    
    @staticmethod
    def generate_session_id() -> str:
        """
        Generate unique session ID
        Format: SESS_<timestamp>_<random>
        """
        timestamp = datetime.utcnow().strftime('%Y%m%d%H%M%S')
        random_suffix = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
        return f"SESS_{timestamp}_{random_suffix}"
    
    @staticmethod
    def create_session(
        db: Session,
        faculty_id: int,
        class_id: int
    ) -> SessionModel:
        """
        Create new attendance session with OTP
        
        Args:
            db: Database session
            faculty_id: ID of faculty creating session
            class_id: ID of class for this session
            
        Returns:
            Created session with OTP
            
        Raises:
            HTTPException: if faculty or class not found
        """
        # Verify faculty exists
        faculty = db.query(Faculty).filter(Faculty.id == faculty_id).first()
        if not faculty:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Faculty not found"
            )
        
        # Verify class exists
        class_obj = db.query(Class).filter(Class.id == class_id).first()
        if not class_obj:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Class not found"
            )
        
        # Generate OTP and calculate expiration
        otp = SessionService.generate_otp(settings.OTP_LENGTH)
        otp_expires_at = datetime.utcnow() + timedelta(minutes=settings.OTP_EXPIRY_MINUTES)
        
        # Create session
        session = SessionModel(
            session_id=SessionService.generate_session_id(),
            faculty_id=faculty_id,
            class_id=class_id,
            otp=otp,
            otp_expires_at=otp_expires_at,
            qr_sequence_number=0,
            qr_refresh_interval=settings.QR_REFRESH_INTERVAL_SECONDS,
            start_time=datetime.utcnow(),
            duration_minutes=settings.SESSION_DURATION_MINUTES,
            status=SessionStatusEnum.ACTIVE
        )
        
        db.add(session)
        db.commit()
        db.refresh(session)
        
        return session
    
    @staticmethod
    def verify_otp(db: Session, session_id: str, otp: str) -> SessionModel:
        """
        Verify OTP for a session
        
        Args:
            db: Database session
            session_id: Session ID to verify
            otp: OTP to check
            
        Returns:
            Session if OTP is valid
            
        Raises:
            HTTPException: if session not found, OTP invalid/expired
        """
        session = db.query(SessionModel).filter(
            SessionModel.session_id == session_id
        ).first()
        
        if not session:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Session not found"
            )
        
        # Check if OTP expired
        if datetime.utcnow() > session.otp_expires_at:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="OTP has expired"
            )
        
        # Verify OTP matches
        if session.otp != otp:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid OTP"
            )
        
        return session
    
    @staticmethod
    def get_active_session(db: Session, session_id: str) -> SessionModel:
        """
        Get active session by ID
        
        Args:
            db: Database session
            session_id: Session ID to retrieve
            
        Returns:
            Active session
            
        Raises:
            HTTPException: if session not found or expired
        """
        session = db.query(SessionModel).filter(
            SessionModel.session_id == session_id,
            SessionModel.status == SessionStatusEnum.ACTIVE
        ).first()
        
        if not session:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Active session not found"
            )
        
        # Check if session has expired based on duration
        if session.start_time:
            session_end = session.start_time + timedelta(minutes=session.duration_minutes)
            if datetime.utcnow() > session_end:
                # Mark as expired
                session.status = SessionStatusEnum.EXPIRED
                session.end_time = session_end
                db.commit()
                
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Session has expired"
                )
        
        return session
    
    @staticmethod
    def end_session(db: Session, session_id: str) -> SessionModel:
        """
        End an active session
        
        Args:
            db: Database session
            session_id: Session to end
            
        Returns:
            Ended session
        """
        session = SessionService.get_active_session(db, session_id)
        session.status = SessionStatusEnum.COMPLETED
        session.end_time = datetime.utcnow()
        db.commit()
        db.refresh(session)
        
        return session

    @staticmethod
    def get_live_session_data(db: Session, session: SessionModel) -> dict:
        """
        Get live session data (stats + student statuses)
        
        Returns:
            Dict containing stats and student list
        """
        from app.models.student import Student
        from app.models.attendance import Attendance, AttendanceStatusEnum
        
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
                
                if att.status == AttendanceStatusEnum.PRESENT:
                    present_count += 1
                elif att.status == AttendanceStatusEnum.PENDING:
                    pending_count += 1
                elif att.status == AttendanceStatusEnum.FAILED:
                    failed_count += 1
            else:
                status_str = "absent"
            
            student_statuses.append({
                "student_id": student.student_id,
                "name": student.name,
                "status": status_str
            })
        
        # Calculate stats
        total_students = len(students)
        present_percentage = (present_count / total_students * 100) if total_students > 0 else 0
        
        stats = {
            "total_students": total_students,
            "present_count": present_count,
            "pending_count": pending_count,
            "failed_count": failed_count,
            "present_percentage": round(present_percentage, 2)
        }
        
        return {
            "stats": stats,
            "students": student_statuses
        }
