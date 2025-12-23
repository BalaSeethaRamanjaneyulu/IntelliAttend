package com.intelliattend.faculty.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.faculty.data.model.GenerateQRResponse
import com.intelliattend.faculty.data.model.StartSessionResponse
import com.intelliattend.faculty.data.repository.SessionRepository
import com.intelliattend.faculty.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    private val _sessionState = MutableStateFlow<Resource<StartSessionResponse>?>(null)
    val sessionState: StateFlow<Resource<StartSessionResponse>?> = _sessionState.asStateFlow()
    
    private val _qrState = MutableStateFlow<Resource<GenerateQRResponse>?>(null)
    val qrState: StateFlow<Resource<GenerateQRResponse>?> = _qrState.asStateFlow()
    
    fun startSession(classId: Int) {
        viewModelScope.launch {
            sessionRepository.startSession(classId).collect { resource ->
                _sessionState.value = resource
            }
        }
    }
    
    fun generateQR(sessionId: String, otp: String) {
        viewModelScope.launch {
            sessionRepository.generateQR(sessionId, otp).collect { resource ->
                _qrState.value = resource
            }
        }
    }
    
    fun resetSessionState() {
        _sessionState.value = null
        _qrState.value = null
    }
}
