"""
Pydantic schemas for attendance submission and verification
"""
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime


class BLESample(BaseModel):
    """Single BLE beacon detection"""
    uuid: str
    rssi: int
    timestamp: datetime


class ScanSampleData(BaseModel):
    """Collection of sensor scan samples from prewarm phase"""
    ble_samples: Optional[List[BLESample]] = []
    wifi_ssid: Optional[str] = None
    wifi_bssid: Optional[str] = None
    gps_latitude: Optional[float] = None
    gps_longitude: Optional[float] = None
    gps_accuracy: Optional[float] = None  # meters
    device_id: str


class AttendanceSubmitRequest(BaseModel):
    """Student attendance submission"""
    student_id: str  # Will be replaced by JWT token in production
    qr_token: str
    session_id: str
    scan_samples: ScanSampleData


class VerificationResult(BaseModel):
    """Verification result response"""
    status: str  # present/failed/pending
    confidence_score: float
    qr_valid: bool
    ble_score: float
    wifi_score: float
    gps_score: float
    verification_notes: str  # Changed from 'message'


class AttendanceSubmitResponse(BaseModel):
    """Response after attendance submission"""
    success: bool
    attendance_id: int
    verification: VerificationResult  # Changed from 'result'

