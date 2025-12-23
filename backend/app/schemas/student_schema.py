"""
Pydantic schemas for student endpoints
"""
from pydantic import BaseModel, EmailStr
from typing import List, Optional
from datetime import time


class StudentProfile(BaseModel):
    """Student profile response"""
    id: int
    student_id: str
    name: str
    email: str
    class_id: int
    device_id: Optional[str]
    
    class Config:
        from_attributes = True


class ClassSchedule(BaseModel):
    """Class schedule item"""
    class_code: str
    name: str
    section: Optional[str]
    day_of_week: int
    start_time: time
    end_time: time
    room_number: str
    building: Optional[str]


class TimetableResponse(BaseModel):
    """Student timetable"""
    student: StudentProfile
    classes: List[ClassSchedule]
    geofence_data: dict  # Room GPS coordinates and Wi-Fi/BLE info


class RoomData(BaseModel):
    """Room sensor configuration for student app"""
    latitude: float
    longitude: float
    geofence_radius: int
    wifi_ssid: Optional[str]
    wifi_bssid: Optional[str]
    ble_beacons: List[str]  # UUID list
