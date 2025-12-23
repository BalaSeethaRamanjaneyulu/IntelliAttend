package com.intelliattend.faculty.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for Faculty API communication
 */

// ============= Authentication =============

data class LoginRequest(
    val username: String,
    val password: String,
    val role: String = "faculty"
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

data class FacultyProfile(
    @SerializedName("faculty_id")
    val facultyId: String,
    val name: String,
    val email: String,
    val department: String
)

// ============= Session Management =============

data class StartSessionRequest(
    @SerializedName("class_id")
    val classId: Int
)

data class StartSessionResponse(
    val otp: String,
    @SerializedName("expires_at")
    val expiresAt: String,
    @SerializedName("session_id")
    val sessionId: String
)

data class GenerateQRRequest(
    @SerializedName("session_id")
    val sessionId: String,
    val otp: String
)

data class GenerateQRResponse(
    @SerializedName("qr_token")
    val qrToken: String,
    @SerializedName("sequence_number")
    val sequenceNumber: Int,
    @SerializedName("expires_at")
    val expiresAt: String
)

// ============= Live Attendance =============

data class LiveStatusResponse(
    @SerializedName("session_id")
    val sessionId: String,
    val status: String,
    val stats: AttendanceStats,
    val students: List<StudentStatus>
)

data class AttendanceStats(
    @SerializedName("total_students")
    val totalStudents: Int,
    @SerializedName("present_count")
    val presentCount: Int,
    @SerializedName("failed_count")
    val failedCount: Int,
    @SerializedName("pending_count")
    val pendingCount: Int,
    @SerializedName("present_percentage")
    val presentPercentage: Float
)

data class StudentStatus(
    @SerializedName("student_id")
    val studentId: String,
    val name: String,
    val status: String,
    @SerializedName("confidence_score")
    val confidenceScore: Float?,
    @SerializedName("submitted_at")
    val submittedAt: String?
)

// ============= Classes =============

data class ClassInfo(
    val id: Int,
    @SerializedName("class_code")
    val classCode: String,
    val name: String,
    val section: String,
    @SerializedName("room_number")
    val roomNumber: String
)

data class ClassListResponse(
    val classes: List<ClassInfo>
)
