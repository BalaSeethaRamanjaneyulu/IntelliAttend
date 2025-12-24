package com.intelliattend.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("submit-attendance")
    suspend fun submitAttendance(
        @Header("Authorization") idToken: String,
        @Body request: AttendanceRequest
    ): Response<AttendanceResponse>
}

data class AttendanceRequest(
    val qr_payload: String,
    val device_fid: String,
    val integrity_token: String,
    val session_id: String
)

data class AttendanceResponse(
    val success: Boolean,
    val message: String
)
