package com.intelliattend.student.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for API communication
 */

// ============= Authentication =============

data class LoginRequest(
    val username: String,
    val password: String,
    val role: String = "student"
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Int
)

data class UserProfile(
    @SerializedName("student_id")
    val studentId: String,
    val name: String,
    val email: String,
    @SerializedName("class_id")
    val classId: Int
)

// ============= Timetable =============

data class ClassSchedule(
    @SerializedName("class_code")
    val classCode: String,
    val name: String,
    val section: String,
    @SerializedName("day_of_week")
    val dayOfWeek: Int,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("end_time")
    val endTime: String,
    val room: RoomData
)

data class RoomData(
    @SerializedName("room_number")
    val roomNumber: String,
    val building: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("geofence_radius")
    val geofenceRadius: Int,
    @SerializedName("wifi_ssid")
    val wifiSsid: String?,
    @SerializedName("wifi_bssid")
    val wifiBssid: String?,
    @SerializedName("ble_beacons")
    val bleBeacons: List<String>
)

data class TimetableResponse(
    val schedule: List<ClassSchedule>
)

// ============= Attendance Submission =============

data class BLESample(
    val uuid: String,
    val rssi: Int,
    val timestamp: String
)

data class ScanSampleData(
    @SerializedName("ble_samples")
    val bleSamples: List<BLESample> = emptyList(),
    @SerializedName("wifi_ssid")
    val wifiSsid: String? = null,
    @SerializedName("wifi_bssid")
    val wifiBssid: String? = null,
    @SerializedName("gps_latitude")
    val gpsLatitude: Double? = null,
    @SerializedName("gps_longitude")
    val gpsLongitude: Double? = null,
    @SerializedName("gps_accuracy")
    val gpsAccuracy: Float? = null,
    @SerializedName("device_id")
    val deviceId: String
)

data class AttendanceSubmitRequest(
    @SerializedName("student_id")
    val studentId: String,
    @SerializedName("qr_token")
    val qrToken: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("scan_samples")
    val scanSamples: ScanSampleData
)

data class VerificationResult(
    val status: String,
    @SerializedName("confidence_score")
    val confidenceScore: Float,
    @SerializedName("qr_valid")
    val qrValid: Boolean,
    @SerializedName("ble_score")
    val bleScore: Float,
    @SerializedName("wifi_score")
    val wifiScore: Float,
    @SerializedName("gps_score")
    val gpsScore: Float,
    @SerializedName("verification_notes")
    val verificationNotes: String
)

data class AttendanceSubmitResponse(
    val success: Boolean,
    @SerializedName("attendance_id")
    val attendanceId: Int,
    val verification: VerificationResult
)

// ============= Error Response =============

data class ErrorResponse(
    val detail: String
)
