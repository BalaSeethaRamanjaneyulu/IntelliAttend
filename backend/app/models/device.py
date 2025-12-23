"""
Device model - whitelisted student devices
"""
from sqlalchemy import Column, String, Integer, ForeignKey, DateTime, Boolean
from sqlalchemy.orm import relationship
from datetime import datetime

from app.db.base import Base


class Device(Base):
    __tablename__ = "devices"
    
    id = Column(Integer, primary_key=True, index=True)
    
    # Student reference
    student_id = Column(Integer, ForeignKey("students.id"), nullable=False)
    
    # Device information
    device_id = Column(String(100), unique=True, nullable=False, index=True)
    device_name = Column(String(100), nullable=True)  # e.g., "Samsung Galaxy S21"
    device_model = Column(String(100), nullable=True)
    os_version = Column(String(50), nullable=True)  # e.g., "Android 13"
    
    # Security
    is_active = Column(Boolean, default=True)
    last_used_at = Column(DateTime, nullable=True)
    
    # Timestamps
    registered_at = Column(DateTime, default=datetime.utcnow)
    
    # Relationships
    student = relationship("Student")
    
    def __repr__(self):
        return f"<Device {self.device_id}: {self.device_name}>"
