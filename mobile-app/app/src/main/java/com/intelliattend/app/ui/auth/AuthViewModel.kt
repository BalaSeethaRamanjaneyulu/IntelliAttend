package com.intelliattend.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.intelliattend.app.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(authRepository.getCurrentUser())
    val user: StateFlow<FirebaseUser?> = _user

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    init {
        // If user is already logged in, fetch their profile
        authRepository.getCurrentUser()?.let {
            fetchUserProfile(it.uid)
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _user.value = user
                user?.uid?.let { fetchUserProfile(it) }
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _user.value = user
                user?.uid?.let { fetchUserProfile(it) }
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _user.value = user
                user?.uid?.let { fetchUserProfile(it) }
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _user.value = null
        _userRole.value = null
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            val result = authRepository.getUserProfile(uid)
            if (result.isSuccess) {
                _userRole.value = result.getOrNull()?.role
            } else {
                // Handle fetch error or default to student/error state
                _error.value = "Failed to fetch profile"
            }
        }
    }

    // Existing Phone Auth logic could be moved here too if needed
}
