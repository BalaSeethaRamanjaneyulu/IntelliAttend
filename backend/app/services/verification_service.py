"""
Attendance verification service
Multi-factor verification using QR, BLE, Wi-Fi, and GPS
"""
import math
from typing import Dict, List, Optional
from datetime import datetime
from sqlalchemy.orm import Session

from app.models.session import Session as SessionModel
from app.models.student import Student
from app.models.room import Room
from app.models.attendance import Attendance, AttendanceStatusEnum
from app.models.scan_log import ScanLog
from app.schemas.attendance_schema import BLESample, ScanSampleData, VerificationResult
from app.services.qr_service import QRService
from app.core.config import settings
from app.core.constants import (
    QR_TOKEN_PREFIX,
    VERIFICATION_WEIGHTS
)

# Extract weights for easy access
WEIGHT_QR = VERIFICATION_WEIGHTS["qr"]
WEIGHT_BLE = VERIFICATION_WEIGHTS["ble"]
WEIGHT_WIFI = VERIFICATION_WEIGHTS["wifi"]
WEIGHT_GPS = VERIFICATION_WEIGHTS["gps"]



class VerificationService:
    """Multi-factor attendance verification"""
    
    @staticmethod
    def verify_attendance(
        db: Session,
        student_id: int,
        session: SessionModel,
        qr_token: str,
        scan_data: ScanSampleData
    ) -> VerificationResult:
        """
        Verify attendance using multi-factor authentication
        
        Factors:
        1. QR Token (40%) - HMAC signature + timestamp
        2. BLE Proximity (30%) - Beacon RSSI signals
        3. Wi-Fi (20%) - BSSID matching
        4. GPS (10%) - Geofence distance
        
        Args:
            db: Database session
            student_id: Student ID
            session: Active session
            qr_token: Scanned QR token
            scan_data: BLE, Wi-Fi, GPS scan data
            
        Returns:
            VerificationResult with scores and decision
        """
        # Get room configuration
        room = db.query(Room).filter(Room.id == session.class_obj.room_id).first()
        
        # Initialize scores
        qr_score = 0.0
        ble_score = 0.0
        wifi_score = 0.0
        gps_score = 0.0
        notes = []
        
        # 1. Verify QR Token (40%)
        qr_result = QRService.verify_qr_token(qr_token, session.session_id)
        if qr_result["valid"]:
            qr_score = 1.0
            notes.append("QR token valid")
        else:
            notes.append(f"QR verification failed: {qr_result['reason']}")
        
        # 2. Verify BLE Proximity (30%)
        if scan_data.ble_samples and room.ble_beacons:
            ble_score = VerificationService._calculate_ble_score(
                scan_data.ble_samples,
                room.ble_beacons
            )
            notes.append(f"BLE score: {ble_score:.2f} ({len(scan_data.ble_samples)} samples)")
        else:
            notes.append("No BLE data")
        
        # 3. Verify Wi-Fi (20%)
        if scan_data.wifi_bssid and room.wifi_bssid:
            if scan_data.wifi_bssid.upper() == room.wifi_bssid.upper():
                wifi_score = 1.0
                notes.append(f"Wi-Fi BSSID matched: {scan_data.wifi_bssid}")
            else:
                notes.append(f"Wi-Fi BSSID mismatch (expected: {room.wifi_bssid}, got: {scan_data.wifi_bssid})")
        else:
            notes.append("No Wi-Fi data")
        
        # 4. Verify GPS (10%)
        if scan_data.gps_latitude and scan_data.gps_longitude:
            distance = VerificationService._calculate_gps_distance(
                scan_data.gps_latitude,
                scan_data.gps_longitude,
                room.latitude,
                room.longitude
            )
            
            if distance <= room.geofence_radius:
                # Score based on distance (closer = better)
                gps_score = max(0, 1 - (distance / room.geofence_radius))
                notes.append(f"GPS within geofence: {distance:.1f}m")
            else:
                notes.append(f"GPS outside geofence: {distance:.1f}m (limit: {room.geofence_radius}m)")
        else:
            notes.append("No GPS data")
        
        # Calculate weighted confidence score
        confidence = (
            WEIGHT_QR * qr_score +
            WEIGHT_BLE * ble_score +
            WEIGHT_WIFI * wifi_score +
            WEIGHT_GPS * gps_score
        )
        
        # Determine attendance status
        if confidence >= settings.CONFIDENCE_THRESHOLD:
            status = AttendanceStatusEnum.PRESENT
            notes.append(f"✅ PRESENT (confidence: {confidence:.2f})")
        else:
            status = AttendanceStatusEnum.FAILED
            notes.append(f"❌ FAILED (confidence: {confidence:.2f}, threshold: {settings.CONFIDENCE_THRESHOLD})")
        
        return VerificationResult(
            status=status.value,
            confidence_score=round(confidence, 3),
            qr_valid=qr_score == 1.0,
            ble_score=round(ble_score, 3),
            wifi_score=round(wifi_score, 3),
            gps_score=round(gps_score, 3),
            verification_notes="; ".join(notes)
        )
    
    @staticmethod
    def _calculate_ble_score(
        ble_samples: List[BLESample],
        room_beacons: List[str]
    ) -> float:
        """
        Calculate BLE proximity score
        
        Rules:
        - Must detect at least 2 beacon hits
        - RSSI must be > -70 dBm (configurable)
        - Score = (valid_hits / total_samples) * beacon_match_ratio
        
        Args:
            ble_samples: List of BLE scan samples
            room_beacons: Expected beacon UUIDs for room
            
        Returns:
            Score between 0.0 and 1.0
        """
        if not ble_samples or not room_beacons:
            return 0.0
        
        valid_hits = 0
        beacon_hits = set()
        
        for sample in ble_samples:
            # Check if beacon UUID matches room
            if sample.uuid in room_beacons:
                # Check if RSSI is strong enough
                if sample.rssi > settings.BLE_RSSI_THRESHOLD:
                    valid_hits += 1
                    beacon_hits.add(sample.uuid)
        
        # Must have minimum number of hits
        if len(beacon_hits) < settings.MIN_BLE_HITS:
            return 0.0
        
        # Calculate score based on hit ratio and beacon coverage
        hit_ratio = valid_hits / len(ble_samples)
        beacon_coverage = len(beacon_hits) / len(room_beacons)
        
        return (hit_ratio + beacon_coverage) / 2
    
    @staticmethod
    def _calculate_gps_distance(
        lat1: float,
        lon1: float,
        lat2: float,
        lon2: float
    ) -> float:
        """
        Calculate distance between two GPS coordinates using Haversine formula
        
        Args:
            lat1, lon1: First coordinate (student)
            lat2, lon2: Second coordinate (room)
            
        Returns:
            Distance in meters
        """
        # Earth radius in meters
        R = 6371000
        
        # Convert to radians
        lat1_rad = math.radians(lat1)
        lat2_rad = math.radians(lat2)
        delta_lat = math.radians(lat2 - lat1)
        delta_lon = math.radians(lon2 - lon1)
        
        # Haversine formula
        a = (
            math.sin(delta_lat / 2) ** 2 +
            math.cos(lat1_rad) * math.cos(lat2_rad) *
            math.sin(delta_lon / 2) ** 2
        )
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        distance = R * c
        
        return distance
    
    @staticmethod
    def save_attendance_record(
        db: Session,
        student_id: int,
        session_id: int,
        verification: VerificationResult,
        scan_data: ScanSampleData,
        qr_token: str
    ) -> Attendance:
        """
        Save attendance record and scan log
        
        Args:
            db: Database session
            student_id: Student ID
            session_id: Session ID
            verification: Verification result
            scan_data: Original scan data
            qr_token: QR token scanned
            
        Returns:
            Created attendance record
        """
        # Create attendance record
        attendance = Attendance(
            session_id=session_id,
            student_id=student_id,
            status=AttendanceStatusEnum(verification.status),
            confidence_score=verification.confidence_score,
            qr_valid=verification.qr_valid,
            ble_score=verification.ble_score,
            wifi_score=verification.wifi_score,
            gps_score=verification.gps_score,
            submitted_at=datetime.utcnow(),
            verified_at=datetime.utcnow(),
            manually_overridden=False
        )
        
        db.add(attendance)
        db.flush()
        
        # Create scan log for audit trail
        # Convert BLE samples to JSON-serializable format
        ble_samples_json = []
        if scan_data.ble_samples:
            for sample in scan_data.ble_samples:
                ble_samples_json.append({
                    "uuid": sample.uuid,
                    "rssi": sample.rssi,
                    "timestamp": sample.timestamp.isoformat() if hasattr(sample.timestamp, 'isoformat') else str(sample.timestamp)
                })
        
        scan_log = ScanLog(
            attendance_id=attendance.id,
            student_id=student_id,
            session_id=session_id,
            qr_token=qr_token,
            qr_scanned_at=datetime.utcnow(),
            ble_samples=ble_samples_json,
            wifi_ssid=scan_data.wifi_ssid,
            wifi_bssid=scan_data.wifi_bssid,
            gps_latitude=scan_data.gps_latitude,
            gps_longitude=scan_data.gps_longitude,
            gps_accuracy=scan_data.gps_accuracy,
            device_id=scan_data.device_id,
            verification_notes=verification.verification_notes
        )
        
        db.add(scan_log)
        db.commit()
        db.refresh(attendance)
        
        return attendance
