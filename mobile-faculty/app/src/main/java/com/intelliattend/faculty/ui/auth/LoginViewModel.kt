package com.intelliattend.faculty.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.faculty.data.model.FacultyProfile
import com.intelliattend.faculty.data.model.TokenResponse
import com.intelliattend.faculty.data.repository.AuthRepository
import com.intelliattend.faculty.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<Resource<TokenResponse>?>(null)
    val loginState: StateFlow<Resource<TokenResponse>?> = _loginState.asStateFlow()
    
    private val _profileState = MutableStateFlow<Resource<FacultyProfile>?>(null)
    val profileState: StateFlow<Resource<FacultyProfile>?> = _profileState.asStateFlow()
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            authRepository.login(username, password).collect { resource ->
                _loginState.value = resource
                
                if (resource is Resource.Success) {
                    loadProfile()
                }
            }
        }
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getFacultyProfile().collect { resource ->
                _profileState.value = resource
            }
        }
    }
    
    fun resetLoginState() {
        _loginState.value = null
        _profileState.value = null
    }
}
