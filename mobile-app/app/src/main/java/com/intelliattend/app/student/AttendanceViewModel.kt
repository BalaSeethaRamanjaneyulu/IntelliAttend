package com.intelliattend.app.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.app.security.SecurityService
import com.intelliattend.app.device.DeviceBindingService
import com.intelliattend.app.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val securityService: SecurityService,
    private val deviceBindingService: DeviceBindingService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _attendanceState = MutableStateFlow<AttendanceState>(AttendanceState.Idle)
    val attendanceState = _attendanceState.asStateFlow()

    fun submitAttendance(qrPayload: String) {
        viewModelScope.launch {
            _attendanceState.value = AttendanceState.Loading

            val fid = deviceBindingService.getFirebaseInstallationId()
            if (fid == null) {
                _attendanceState.value = AttendanceState.Error("Failed to retrieve Device ID")
                return@launch
            }

            // Generate nonce for integrity check
            val nonce = generateNonce(qrPayload, fid)
            val integrityToken = securityService.getIntegrityToken(nonce)

            if (integrityToken == null) {
                _attendanceState.value = AttendanceState.Error("Security integrity check failed")
                return@launch
            }

            // Call backend API here...
            // For now, simulating success
            _attendanceState.value = AttendanceState.Success
        }
    }

    private fun generateNonce(payload: String, fid: String): String {
        // Simple nonce generation logic
        return (payload + fid + System.currentTimeMillis()).hashCode().toString()
    }
}

sealed class AttendanceState {
    object Idle : AttendanceState()
    object Loading : AttendanceState()
    object Success : AttendanceState()
    data class Error(val message: String) : AttendanceState()
}
