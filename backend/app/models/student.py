"""
Student model - represents students who mark attendance
"""
from sqlalchemy import Column, String, Integer, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from datetime import datetime

from app.db.base import Base


class Student(Base):
    __tablename__ = "students"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(String(50), unique=True, nullable=False, index=True)
    name = Column(String(100), nullable=False)
    email = Column(String(100), unique=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    
    # Class assignment
    class_id = Column(Integer, ForeignKey("classes.id"), nullable=False)
    
    # Biometric data
    face_id_hash = Column(String(255), nullable=True)  # Hashed biometric data
    
    # Device management
    device_id = Column(String(100), nullable=True)  # Registered device unique ID
    
    # Timestamps
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    class_assigned = relationship("Class", back_populates="students")
    attendance_records = relationship("Attendance", back_populates="student")
    
    def __repr__(self):
        return f"<Student {self.student_id}: {self.name}>"
