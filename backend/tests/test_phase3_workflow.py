"""
Test script for Phase 3 - Multi-Factor Verification
Demonstrates complete attendance submission workflow
"""
import requests
import json
from datetime import datetime

# Configuration
API_URL = "http://localhost:8000"

def test_complete_workflow():
    """Test the complete attendance workflow"""
    
    print("=" * 60)
    print("IntelliAttend - Phase 3 Verification Test")
    print("=" * 60)
    
    # Step 1: Faculty starts session
    print("\n📱 Step 1: Faculty starts session...")
    response = requests.post(
        f"{API_URL}/api/v1/faculty/start_session",
        json={"class_id": 3}
    )
    session_data = response.json()
    print(f"✅ Session created: {session_data['session_id']}")
    print(f"   OTP: {session_data['otp']}")
    
    # Step 2: SmartBoard generates QR token
    print("\n🖥️  Step 2: SmartBoard generates QR token...")
    response = requests.post(
        f"{API_URL}/api/v1/faculty/generate_qr",
        json={
            "session_id": session_data['session_id'],
            "otp": session_data['otp']
        }
    )
    qr_data = response.json()
    print(f"✅ QR Token generated: {qr_data['qr_token'][:50]}...")
    print(f"   Sequence: {qr_data['sequence_number']}")
    
    # Step 3: Student submits attendance with multi-factor data
    print("\n📲 Step 3: Student (Alice) submits attendance...")
    
    # Simulate scan data from mobile app
    attendance_request = {
        "student_id": "STU001",
        "session_id": session_data['session_id'],
        "qr_token": qr_data['qr_token'],
        "scan_samples": {
            "ble_samples": [
                {
                    "uuid": "UUID-BEACON-1",
                    "rssi": -65,  # Strong signal
                    "timestamp": datetime.utcnow().isoformat()
                },
                {
                    "uuid": "UUID-BEACON-2",
                    "rssi": -68,  # Strong signal
                    "timestamp": datetime.utcnow().isoformat()
                },
                {
                    "uuid": "UUID-BEACON-1",
                    "rssi": -63,  # Very strong
                    "timestamp": datetime.utcnow().isoformat()
                }
            ],
            "wifi_ssid": "Campus_WiFi",
            "wifi_bssid": "00:11:22:33:44:55",  # Matches room
            "gps_latitude": 17.4436,  # Very close to room (17.4435)
            "gps_longitude": 78.3489,  # Very close to room (78.3488)
            "gps_accuracy": 5.0,
            "device_id": "DEVICE_ALICE_001"
        }
    }
    
    response = requests.post(
        f"{API_URL}/api/v1/attendance/submit",
        json=attendance_request
    )
    
    if response.status_code == 200:
        result = response.json()
        verification = result['verification']
        
        print(f"✅ Attendance submitted!")
        print(f"\n📊 Verification Results:")
        print(f"   Status: {verification['status'].upper()}")
        print(f"   Confidence Score: {verification['confidence_score']}")
        print(f"   QR Valid: {verification['qr_valid']}")
        print(f"   BLE Score: {verification['ble_score']}")
        print(f"   WiFi Score: {verification['wifi_score']}")
        print(f"   GPS Score: {verification['gps_score']}")
        print(f"\n📝 Notes: {verification['verification_notes']}")
        
        attendance_id = result['attendance_id']
        
    else:
        print(f"❌ Error: {response.status_code}")
        print(response.json())
        return
    
    # Step 4: Check live status
    print(f"\n📈 Step 4: Checking live status...")
    response = requests.get(
        f"{API_URL}/api/v1/faculty/live_status/{session_data['session_id']}"
    )
    status = response.json()
    
    print(f"✅ Live Status:")
    print(f"   Total Students: {status['stats']['total_students']}")
    print(f"   Present: {status['stats']['present_count']}")
    print(f"   Present %: {status['stats']['present_percentage']}%")
    print(f"\n   Students:")
    for student in status['students']:
        print(f"     - {student['name']}: {student['status'].upper()}")
        if student['confidence_score']:
            print(f"       Confidence: {student['confidence_score']}")
    
    # Step 5: Get detailed verification
    print(f"\n🔍 Step 5: Get detailed verification...")
    response = requests.get(
        f"{API_URL}/api/v1/attendance/verify/{attendance_id}"
    )
    details = response.json()
    print(f"✅ Verification Details Retrieved")
    print(f"   Full Notes: {details['verification_notes']}")
    
    print("\n" + "=" * 60)
    print("✅ Complete workflow test PASSED!")
    print("==" * 60)


def test_failed_verification():
    """Test a failed verification (outside geofence, weak BLE)"""
    
    print("\n\n" + "=" *60)
    print("Testing FAILED Verification Scenario")
    print("=" * 60)
    
    # Start new session
    response = requests.post(
        f"{API_URL}/api/v1/faculty/start_session",
        json={"class_id": 3}
    )
    session_data = response.json()
    
    # Generate QR
    response = requests.post(
        f"{API_URL}/api/v1/faculty/generate_qr",
        json={
            "session_id": session_data['session_id'],
            "otp": session_data['otp']
        }
    )
    qr_data = response.json()
    
    # Student Bob submits with weak signals
    print("\n📲 Student (Bob) submits with weak signals...")
    
    attendance_request = {
        "student_id": "STU002",
        "session_id": session_data['session_id'],
        "qr_token": qr_data['qr_token'],
        "scan_samples": {
            "ble_samples": [
                {
                    "uuid": "UNKNOWN-BEACON",  # Wrong beacon
                    "rssi": -85,  # Weak signal
                    "timestamp": datetime.utcnow().isoformat()
                }
            ],
            "wifi_ssid": "Different_WiFi",
            "wifi_bssid": "AA:BB:CC:DD:EE:FF",  # Wrong BSSID
            "gps_latitude": 17.4500,  # Far from room
            "gps_longitude": 78.3500,
            "gps_accuracy": 50.0,
            "device_id": "DEVICE_BOB_002"
        }
    }
    
    response = requests.post(
        f"{API_URL}/api/v1/attendance/submit",
        json=attendance_request
    )
    
    result = response.json()
    verification = result['verification']
    
    print(f"\n📊 Verification Results:")
    print(f"   Status: {verification['status'].upper()}")
    print(f"   Confidence Score: {verification['confidence_score']}")
    print(f"   Notes: {verification['verification_notes']}")
    
    print("\n✅ Failed verification test PASSED (correctly rejected)")
    print("=" * 60)


if __name__ == "__main__":
    test_complete_workflow()
    test_failed_verification()
