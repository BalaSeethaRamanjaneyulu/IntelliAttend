"""
Session model - represents an active attendance session
"""
from sqlalchemy import Column, String, Integer, ForeignKey, DateTime, Enum as SQLEnum
from sqlalchemy.orm import relationship
from datetime import datetime
import enum

from app.db.base import Base


class SessionStatusEnum(str, enum.Enum):
    ACTIVE = "active"
    EXPIRED = "expired"
    COMPLETED = "completed"


class Session(Base):
    __tablename__ = "sessions"
    
    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(String(100), unique=True, nullable=False, index=True)
    
    # Faculty and Class
    faculty_id = Column(Integer, ForeignKey("faculty.id"), nullable=False)
    class_id = Column(Integer, ForeignKey("classes.id"), nullable=False)
    
    # OTP for SmartBoard linking
    otp = Column(String(10), nullable=True)
    otp_expires_at = Column(DateTime, nullable=True)
    
    # QR Token configuration
    current_qr_token = Column(String(255), nullable=True)
    qr_sequence_number = Column(Integer, default=0)
    qr_refresh_interval = Column(Integer, default=5)  # seconds
    
    # Session timing
    start_time = Column(DateTime, default=datetime.utcnow)
    end_time = Column(DateTime, nullable=True)
    duration_minutes = Column(Integer, default=2)
    
    # Status
    status = Column(SQLEnum(SessionStatusEnum), default=SessionStatusEnum.ACTIVE)
    
    # Relationships
    faculty = relationship("Faculty", back_populates="sessions")
    class_obj = relationship("Class", back_populates="sessions")
    attendance_records = relationship("Attendance", back_populates="session")
    
    def __repr__(self):
        return f"<Session {self.session_id}: {self.status}>"
