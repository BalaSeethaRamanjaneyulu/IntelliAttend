"""
Attendance Service - Handle student attendance marking via QR scan
"""
from fastapi import HTTPException, status
import firebase_admin
from firebase_admin import firestore
from app.core import firebase
from datetime import datetime
from typing import Dict
import math

class AttendanceService:
    """Handle student attendance operations"""
    
    @staticmethod
    def _get_db():
        """Get Firestore client"""
        if not firebase_admin._apps:
            firebase.initialize_firebase()
        return firestore.client()
    
    @staticmethod
    def _calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """
        Calculate distance between two GPS coordinates in meters
        Using Haversine formula
        """
        R = 6371000  # Earth radius in meters
        
        phi1 = math.radians(lat1)
        phi2 = math.radians(lat2)
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lon2 - lon1)
        
        a = math.sin(delta_phi/2)**2 + \
            math.cos(phi1) * math.cos(phi2) * \
            math.sin(delta_lambda/2)**2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
        
        return R * c
    
    @staticmethod
    async def mark_attendance(
        session_id: str,
        student_id: str,
        qr_token: str = None,
        location_data: Dict = None
    ):
        """
        Mark student attendance via QR scan
        
        Args:
            session_id: Active session ID
            student_id: Student ID
            qr_token: Encrypted QR token (optional for now)
            location_data: GPS, WiFi, Bluetooth data
        
        Returns:
            Attendance record
        """
        db = AttendanceService._get_db()
        
        # Get session
        session_ref = db.collection('sessions').document(session_id)
        session_doc = session_ref.get()
        
        if not session_doc.exists:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Session not found"
            )
        
        session_data = session_doc.to_dict()
        
        # Check session is active
        if session_data.get('status') != 'active':
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Session is not active"
            )
        
        # Check if student already marked present
        existing = db.collection('student_attendance') \
            .where('session_id', '==', session_id) \
            .where('student_id', '==', student_id) \
            .limit(1) \
            .stream()
        
        for doc in existing:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Attendance already marked for this session"
            )
        
        # Verify student enrollment (if section_id available)
        if session_data.get('section_id'):
            student_ref = db.collection('students').document(student_id)
            student_doc = student_ref.get()
            
            if not student_doc.exists:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Student not found"
                )
            
            student_data = student_doc.to_dict()
            if student_data.get('section_id') != session_data.get('section_id'):
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Student not enrolled in this section"
                )
        
        # Location verification
        verification_status = {
            'gps_verified': False,
            'wifi_verified': False,
            'bluetooth_verified': False,
            'distance_meters': None
        }
        
        if location_data and session_data.get('expected_location'):
            # GPS verification
            if location_data.get('gps'):
                student_lat = location_data['gps'].get('latitude')
                student_lng = location_data['gps'].get('longitude')
                expected_lat = session_data['expected_location'].get('latitude')
                expected_lng = session_data['expected_location'].get('longitude')
                
                if all([student_lat, student_lng, expected_lat, expected_lng]):
                    distance = AttendanceService._calculate_distance(
                        student_lat, student_lng,
                        expected_lat, expected_lng
                    )
                    verification_status['distance_meters'] = round(distance, 2)
                    
                    # Within 50 meters?
                    radius = session_data['expected_location'].get('radius_meters', 50)
                    verification_status['gps_verified'] = distance <= radius
            
            # WiFi verification
            if location_data.get('wifi_bssid'):
                # Get classroom WiFi
                classroom_id = session_data.get('classroom_id')
                if classroom_id:
                    classroom_ref = db.collection('classrooms').document(classroom_id)
                    classroom_doc = classroom_ref.get()
                    if classroom_doc.exists:
                        classroom_data = classroom_doc.to_dict()
                        expected_bssid = classroom_data.get('wifi_bssid')
                        verification_status['wifi_verified'] = \
                            location_data['wifi_bssid'] == expected_bssid
            
            # Bluetooth verification
            if location_data.get('bluetooth_beacon'):
                beacon_minor = location_data['bluetooth_beacon'].get('minor')
                beacon_id = session_data.get('bluetooth_beacon_id')
                if beacon_id:
                    beacon_ref = db.collection('bluetooth_beacons').document(beacon_id)
                    beacon_doc = beacon_ref.get()
                    if beacon_doc.exists:
                        beacon_data = beacon_doc.to_dict()
                        verification_status['bluetooth_verified'] = \
                            beacon_minor == beacon_data.get('minor')
        
        # Determine status based on verification
        # Require at least GPS OR (WiFi + Bluetooth)
        if verification_status['gps_verified'] or \
           (verification_status['wifi_verified'] and verification_status['bluetooth_verified']):
            attendance_status = 'present'
        else:
            attendance_status = 'suspicious'  # Marked but location doesn't match
        
        # Create attendance record
        attendance_data = {
            'student_id': student_id,
            'session_id': session_id,
            'subject_id': session_data.get('subject_id'),
            'section_id': session_data.get('section_id'),
            'classroom_id': session_data.get('classroom_id'),
            'status': attendance_status,
            'verified_by': 'qr_scan',
            'verification_data': {
                **verification_status,
                'location_provided': location_data if location_data else {}
            },
            'timestamp': firestore.SERVER_TIMESTAMP
        }
        
        attendance_ref = db.collection('student_attendance').document()
        attendance_ref.set(attendance_data)
        
        # Notify WebSocket clients of new attendance
        try:
            from app.api.v1.websocket import notify_attendance_marked
            import asyncio
            
            # Get student name if available
            student_name = None
            if student_doc.exists:
                student_name = student_data.get('name')
            
            # Send WebSocket notification (non-blocking)
            asyncio.create_task(notify_attendance_marked(session_id, student_name))
        except Exception as e:
            # Don't fail attendance if WebSocket fails
            print(f"WebSocket notification failed: {e}")
        
        return {
            'success': True,
            'attendance_id': attendance_ref.id,
            'status': attendance_status,
            'verification': verification_status,
            'message': 'Attendance marked successfully' if attendance_status == 'present' 
                      else 'Attendance marked but location verification failed'
        }
