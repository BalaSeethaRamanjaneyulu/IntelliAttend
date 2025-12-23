package com.intelliattend.faculty.data.repository

import com.intelliattend.faculty.data.local.PreferencesManager
import com.intelliattend.faculty.data.model.*
import com.intelliattend.faculty.data.remote.ApiService
import com.intelliattend.faculty.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for session management operations
 */
@Singleton
class SessionRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    
    /**
     * Start attendance session
     */
    fun startSession(classId: Int): Flow<Resource<StartSessionResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = preferencesManager.accessToken.first()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }
            
            val request = StartSessionRequest(classId = classId)
            val response = apiService.startSession("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to start session"))
            }
            
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error starting session"))
        }
    }
    
    /**
     * Generate QR token
     */
    fun generateQR(sessionId: String, otp: String): Flow<Resource<GenerateQRResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = preferencesManager.accessToken.first()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }
            
            val request = GenerateQRRequest(sessionId = sessionId, otp = otp)
            val response = apiService.generateQR("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to generate QR"))
            }
            
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error generating QR"))
        }
    }
    
    /**
     * Get live attendance status
     */
    fun getLiveStatus(sessionId: String): Flow<Resource<LiveStatusResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = preferencesManager.accessToken.first()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }
            
            val response = apiService.getLiveStatus("Bearer $token", sessionId)
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to load status"))
            }
            
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error loading status"))
        }
    }
}
