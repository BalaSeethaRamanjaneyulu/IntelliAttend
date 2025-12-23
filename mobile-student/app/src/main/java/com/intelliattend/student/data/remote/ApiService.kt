package com.intelliattend.student.data.remote

import com.intelliattend.student.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface
 * Defines all endpoints for communication with backend
 */
interface ApiService {
    
    // ============= Authentication =============
    
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>
    
    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") token: String
    ): Response<TokenResponse>
    
    @GET("api/v1/auth/me")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfile>
    
    // ============= Student =============
    
    @GET("api/v1/student/timetable")
    suspend fun getTimetable(
        @Header("Authorization") token: String
    ): Response<TimetableResponse>
    
    // ============= Attendance =============
    
    @POST("api/v1/attendance/submit")
    suspend fun submitAttendance(
        @Header("Authorization") token: String,
        @Body request: AttendanceSubmitRequest
    ): Response<AttendanceSubmitResponse>
    
    @GET("api/v1/attendance/verify/{attendanceId}")
    suspend fun getVerificationDetails(
        @Header("Authorization") token: String,
        @Path("attendanceId") attendanceId: Int
    ): Response<VerificationResult>
}
