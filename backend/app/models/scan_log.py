"""
ScanLog model - audit trail for all attendance verification attempts
"""
from sqlalchemy import Column, String, Integer, ForeignKey, DateTime, Float, JSON, Text
from sqlalchemy.orm import relationship
from datetime import datetime

from app.db.base import Base


class ScanLog(Base):
    __tablename__ = "scan_logs"
    
    id = Column(Integer, primary_key=True, index=True)
    
    # References
    attendance_id = Column(Integer, ForeignKey("attendance.id"), nullable=False)
    student_id = Column(Integer, ForeignKey("students.id"), nullable=False)
    session_id = Column(Integer, ForeignKey("sessions.id"), nullable=False)
    
    # QR data
    qr_token = Column(String(255), nullable=True)
    qr_scanned_at = Column(DateTime, nullable=True)
    
    # BLE scan data (JSON array of beacon detections)
    # Example: [{"uuid": "...", "rssi": -65, "timestamp": "..."}, ...]
    ble_samples = Column(JSON, nullable=True)
    
    # Wi-Fi data
    wifi_ssid = Column(String(100), nullable=True)
    wifi_bssid = Column(String(17), nullable=True)
    
    # GPS data
    gps_latitude = Column(Float, nullable=True)
    gps_longitude = Column(Float, nullable=True)
    gps_accuracy = Column(Float, nullable=True)  # meters
    
    # Device info
    device_id = Column(String(100), nullable=True)
    
    # Verification notes
    verification_notes = Column(Text, nullable=True)  # Error messages, warnings
    
    # Timestamp
    created_at = Column(DateTime, default=datetime.utcnow)
    
    # Relationships
    attendance = relationship("Attendance", back_populates="scan_log")
    student = relationship("Student")
    session = relationship("Session")
    
    def __repr__(self):
        return f"<ScanLog {self.id}: Student {self.student_id} in Session {self.session_id}>"
