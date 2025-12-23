"""
Application constants for verification and system configuration
"""

# Verification weights (must sum to 1.0)
VERIFICATION_WEIGHTS = {
    "qr": 0.4,
    "ble": 0.3,
    "wifi": 0.2,
    "gps": 0.1
}

# Attendance status
class AttendanceStatus:
    PRESENT = "present"
    PENDING = "pending"
    FAILED = "failed"
    ABSENT = "absent"

# User roles
class UserRole:
    STUDENT = "student"
    FACULTY = "faculty"
    ADMIN = "admin"

# Session status
class SessionStatus:
    ACTIVE = "active"
    EXPIRED = "expired"
    COMPLETED = "completed"

# QR token configuration
QR_TOKEN_PREFIX = "IATT"  # IntelliAttend Token
QR_SEQUENCE_LENGTH = 6

# BLE beacon configuration
BLE_SCAN_TIMEOUT_SECONDS = 12
BLE_MIN_RSSI = -90  # Minimum detectable signal
BLE_MAX_RSSI = -30  # Maximum expected signal

# GPS configuration
GPS_ACCURACY_THRESHOLD_METERS = 50  # Required accuracy
EARTH_RADIUS_METERS = 6371000  # For Haversine calculation

# Wi-Fi configuration
WIFI_MATCH_REQUIRED_FIELDS = ["ssid", "bssid"]

# Error messages
ERROR_MESSAGES = {
    "invalid_credentials": "Invalid username or password",
    "token_expired": "Token has expired",
    "qr_expired": "QR code has expired",
    "confidence_low": "Attendance verification failed - insufficient confidence",
    "already_marked": "Attendance already marked for this session",
    "session_not_found": "Session not found or expired",
    "unauthorized": "Unauthorized access"
}
