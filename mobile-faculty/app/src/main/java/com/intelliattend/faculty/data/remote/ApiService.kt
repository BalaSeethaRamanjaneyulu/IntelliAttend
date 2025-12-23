package com.intelliattend.faculty.data.remote

import com.intelliattend.faculty.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for Faculty
 */
interface ApiService {
    
    // ============= Authentication =============
    
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>
    
    @GET("api/v1/auth/me")
    suspend fun getFacultyProfile(
        @Header("Authorization") token: String
    ): Response<FacultyProfile>
    
    // ============= Session Management =============
    
    @POST("api/v1/faculty/start_session")
    suspend fun startSession(
        @Header("Authorization") token: String,
        @Body request: StartSessionRequest
    ): Response<StartSessionResponse>
    
    @POST("api/v1/faculty/generate_qr")
    suspend fun generateQR(
        @Header("Authorization") token: String,
        @Body request: GenerateQRRequest
    ): Response<GenerateQRResponse>
    
    @GET("api/v1/faculty/live_status/{sessionId}")
    suspend fun getLiveStatus(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): Response<LiveStatusResponse>
    
    // ============= Classes (placeholder) =============
    
    @GET("api/v1/faculty/classes")
    suspend fun getClasses(
        @Header("Authorization") token: String
    ): Response<ClassListResponse>
}
