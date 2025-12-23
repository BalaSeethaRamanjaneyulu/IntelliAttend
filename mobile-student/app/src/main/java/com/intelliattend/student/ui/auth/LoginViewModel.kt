package com.intelliattend.student.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.student.data.model.TokenResponse
import com.intelliattend.student.data.model.UserProfile
import com.intelliattend.student.data.repository.AuthRepository
import com.intelliattend.student.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Login screen
 * Handles authentication logic and UI state
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<Resource<TokenResponse>?>(null)
    val loginState: StateFlow<Resource<TokenResponse>?> = _loginState.asStateFlow()
    
    private val _profileState = MutableStateFlow<Resource<UserProfile>?>(null)
    val profileState: StateFlow<Resource<UserProfile>?> = _profileState.asStateFlow()
    
    /**
     * Login with username and password
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            authRepository.login(username, password).collect { resource ->
                _loginState.value = resource
                
                // If login successful, fetch user profile
                if (resource is Resource.Success) {
                    loadUserProfile()
                }
            }
        }
    }
    
    /**
     * Load user profile after login
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.getUserProfile().collect { resource ->
                _profileState.value = resource
            }
        }
    }
    
    /**
     * Reset login state
     */
    fun resetLoginState() {
        _loginState.value = null
        _profileState.value = null
    }
}
