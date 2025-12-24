"""
Attendance API routes
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.attendance_service import AttendanceService
from typing import Dict, Optional

router = APIRouter()

class GPSLocation(BaseModel):
    latitude: float
    longitude: float
    accuracy: Optional[float] = None

class BluetoothBeacon(BaseModel):
    uuid: str
    major: int
    minor: int

class LocationData(BaseModel):
    gps: Optional[GPSLocation] = None
    wifi_ssid: Optional[str] = None
    wifi_bssid: Optional[str] = None
    bluetooth_beacon: Optional[BluetoothBeacon] = None

class ScanQRRequest(BaseModel):
    session_id: str
    student_id: str
    qr_token: Optional[str] = None
    location_data: Optional[LocationData] = None


@router.post("/scan-qr")
async def scan_qr(request: ScanQRRequest):
    """
    Mark student attendance by scanning QR code
    
    Student scans QR on SmartBoard and sends:
    - session_id: From QR code
    - student_id: Their ID
    - location_data: GPS, WiFi, Bluetooth for verification
    
    Returns attendance record with verification status
    """
    location_dict = request.location_data.dict() if request.location_data else None
    
    return await AttendanceService.mark_attendance(
        request.session_id,
        request.student_id,
        request.qr_token,
        location_dict
    )


@router.get("/student/{student_id}/history")
async def get_student_attendance_history(student_id: str, limit: int = 50):
    """
    Get student's attendance history
    
    Returns list of attendance records
    """
    from app.services.attendance_service import AttendanceService
    db = AttendanceService._get_db()
    
    attendance_records = db.collection('student_attendance') \
        .where('student_id', '==', student_id) \
        .order_by('timestamp', direction='DESCENDING') \
        .limit(limit) \
        .stream()
    
    records = []
    for doc in attendance_records:
        data = doc.to_dict()
        records.append({
            'attendance_id': doc.id,
            'session_id': data.get('session_id'),
            'subject_id': data.get('subject_id'),
            'status': data.get('status'),
            'timestamp': data.get('timestamp'),
            'verification': data.get('verification_data', {})
        })
    
    return {
        'student_id': student_id,
        'total_records': len(records),
        'records': records
    }
