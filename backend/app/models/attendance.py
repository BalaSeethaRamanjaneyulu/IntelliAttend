"""
Attendance model - records student attendance with confidence scores
"""
from sqlalchemy import Column, String, Integer, ForeignKey, DateTime, Float, Enum as SQLEnum
from sqlalchemy.orm import relationship
from datetime import datetime
import enum

from app.db.base import Base


class AttendanceStatusEnum(str, enum.Enum):
    PRESENT = "present"
    PENDING = "pending"
    FAILED = "failed"
    ABSENT = "absent"


class Attendance(Base):
    __tablename__ = "attendance"
    
    id = Column(Integer, primary_key=True, index=True)
    
    # References
    session_id = Column(Integer, ForeignKey("sessions.id"), nullable=False)
    student_id = Column(Integer, ForeignKey("students.id"), nullable=False)
    
    # Verification results
    status = Column(SQLEnum(AttendanceStatusEnum), default=AttendanceStatusEnum.PENDING)
    confidence_score = Column(Float, nullable=True)  # 0.0 to 1.0
    
    # Individual factor scores (for audit)
    qr_valid = Column(Integer, default=0)  # 1 or 0
    ble_score = Column(Float, default=0.0)  # 0.0 to 1.0
    wifi_score = Column(Float, default=0.0)  # 0.0 to 1.0
    gps_score = Column(Float, default=0.0)  # 0.0 to 1.0
    
    # Submission details
    submitted_at = Column(DateTime, default=datetime.utcnow)
    verified_at = Column(DateTime, nullable=True)
    
    # Manual override (by faculty)
    manually_overridden = Column(Integer, default=0)
    override_reason = Column(String(255), nullable=True)
    
    # Relationships
    session = relationship("Session", back_populates="attendance_records")
    student = relationship("Student", back_populates="attendance_records")
    scan_log = relationship("ScanLog", back_populates="attendance", uselist=False)
    
    def __repr__(self):
        return f"<Attendance {self.student_id} in Session {self.session_id}: {self.status}>"
