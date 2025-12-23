"""
Import all models for easy access and Alembic auto-generation
"""
from app.db.base import Base
from app.models.student import Student
from app.models.faculty import Faculty
from app.models.class_model import Class
from app.models.room import Room
from app.models.session import Session
from app.models.attendance import Attendance
from app.models.device import Device
from app.models.scan_log import ScanLog

# Export all models
__all__ = [
    "Base",
    "Student",
    "Faculty",
    "Class",
    "Room",
    "Session",
    "Attendance",
    "Device",
    "ScanLog"
]
