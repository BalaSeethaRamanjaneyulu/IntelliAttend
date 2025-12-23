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

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    
    fun login(username: String, password: String): Flow<Resource<TokenResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = LoginRequest(
                username = username,
                password = password,
                role = "faculty"
            )
            
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                
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
    
    fun getFacultyProfile(): Flow<Resource<FacultyProfile>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = preferencesManager.accessToken.first()
            
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }
            
            val response = apiService.getFacultyProfile("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                
                preferencesManager.saveFacultyProfile(
                    facultyId = profile.facultyId,
                    name = profile.name,
                    email = profile.email,
                    department = profile.department
                )
                
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error("Failed to load profile"))
            }
            
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error loading profile"))
        }
    }
    
    suspend fun logout() {
        preferencesManager.clearAll()
    }
    
    suspend fun isLoggedIn(): Boolean {
        return preferencesManager.isLoggedIn.first()
    }
}
