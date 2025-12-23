package com.intelliattend.student.data.repository

import com.intelliattend.student.data.local.PreferencesManager
import com.intelliattend.student.data.model.*
import com.intelliattend.student.data.remote.ApiService
import com.intelliattend.student.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations
 * Single source of truth for auth-related data
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    
    /**
     * Login student with credentials
     */
    fun login(username: String, password: String): Flow<Resource<TokenResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = LoginRequest(
                username = username,
                password = password,
                role = "student"
            )
            
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                
                // Save tokens
                preferencesManager.saveTokens(
                    tokenResponse.accessToken,
                    tokenResponse.refreshToken
                )
                
                emit(Resource.Success(tokenResponse))
            } else {
                emit(Resource.Error("Invalid credentials"))
            }
            
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Login failed"))
        }
    }
    
    /**
     * Get user profile after login
     */
    fun getUserProfile(): Flow<Resource<UserProfile>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = preferencesManager.accessToken.first()
            
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }
            
            val response = apiService.getUserProfile("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                
                // Save user profile
                preferencesManager.saveUserProfile(
                    studentId = profile.studentId,
                    name = profile.name,
                    email = profile.email,
                    classId = profile.classId
                )
                
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error("Failed to load profile"))
            }
            
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error loading profile"))
        }
    }
    
    /**
     * Logout user
     */
    suspend fun logout() {
        preferencesManager.clearAll()
    }
    
    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return preferencesManager.isLoggedIn.first()
    }
}
