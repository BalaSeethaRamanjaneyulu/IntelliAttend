"""
Pydantic schemas for faculty endpoints
"""
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime


class SessionCreateRequest(BaseModel):
    """Request to create new session"""
    class_id: int


class OTPResponse(BaseModel):
    """OTP for SmartBoard linking"""
    otp: str
    expires_at: datetime
    session_id: str


class QRGenerateRequest(BaseModel):
    """Request to generate QR token"""
    session_id: str
    otp: str  # Verify OTP before generating QR


class QRTokenResponse(BaseModel):
    """Generated QR token"""
    qr_token: str
    sequence_number: int
    expires_at: datetime


class AttendanceStats(BaseModel):
    """Live attendance statistics"""
    total_students: int
    present_count: int
    pending_count: int
    failed_count: int
    present_percentage: float


class StudentAttendanceStatus(BaseModel):
    """Individual student status"""
    student_id: str
    name: str
    status: str  # present/pending/failed
    confidence_score: Optional[float]
    submitted_at: Optional[datetime]


class LiveStatusResponse(BaseModel):
    """Live session status for faculty dashboard"""
    session_id: str
    status: str  # active/expired/completed
    stats: AttendanceStats
    students: List[StudentAttendanceStatus]
