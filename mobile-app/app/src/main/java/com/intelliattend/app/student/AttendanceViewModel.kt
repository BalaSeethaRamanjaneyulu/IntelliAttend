package com.intelliattend.app.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.app.security.SecurityService
import com.intelliattend.app.device.DeviceBindingService
import com.intelliattend.app.auth.AuthRepository
import com.intelliattend.app.network.ApiService
import com.intelliattend.app.network.AttendanceRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val securityService: SecurityService,
    private val deviceBindingService: DeviceBindingService,
    private val authRepository: AuthRepository,
    private val apiService: ApiService
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

            val user = authRepository.getCurrentUser()
            val idToken = user?.getIdToken(true)?.await()?.token
            
            if (idToken == null) {
                _attendanceState.value = AttendanceState.Error("User session expired")
                return@launch
            }

            try {
                val response = apiService.submitAttendance(
                    idToken = "Bearer $idToken",
                    request = AttendanceRequest(
                        qr_payload = qrPayload,
                        device_fid = fid,
                        integrity_token = integrityToken,
                        session_id = extractSessionId(qrPayload)
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    _attendanceState.value = AttendanceState.Success
                } else {
                    _attendanceState.value = AttendanceState.Error(response.body()?.message ?: "Attendance submission failed")
                }
            } catch (e: Exception) {
                _attendanceState.value = AttendanceState.Error("Network error: ${e.message}")
            }
        }
    }

    private fun extractSessionId(payload: String): String {
        // Basic extraction logic (assuming format: session=UUID&...)
        return payload.split("&").find { it.startsWith("session=") }?.substringAfter("=") ?: "unknown"
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
