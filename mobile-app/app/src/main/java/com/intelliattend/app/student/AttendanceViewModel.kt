package com.intelliattend.app.student

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.app.security.SecurityService
import com.intelliattend.app.device.DeviceBindingService
import com.intelliattend.app.auth.AuthRepository
import com.intelliattend.app.network.ApiService
import com.intelliattend.app.network.AttendanceRequest
import com.intelliattend.app.network.ValidationFailureRequest
import com.intelliattend.app.session.ActiveSessionRepository
import com.intelliattend.app.session.TokenCacheService
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
    private val apiService: ApiService,
    private val activeSessionRepository: ActiveSessionRepository,
    private val tokenCacheService: TokenCacheService
) : ViewModel() {

    private val _attendanceState = MutableStateFlow<AttendanceState>(AttendanceState.Idle)
    val attendanceState = _attendanceState.asStateFlow()
    
    private val _listenerState = MutableStateFlow<ListenerState>(ListenerState.Idle)
    val listenerState = _listenerState.asStateFlow()

    /**
     * Start listening to ActiveSessions for real-time token updates
     * Call this when QR scanner screen opens
     * 
     * @param sessionId Session to listen to (get from faculty or API)
     */
    fun startTokenListener(sessionId: String) {
        viewModelScope.launch {
            _listenerState.value = ListenerState.Listening(sessionId)
            
            activeSessionRepository.listenToActiveSession(sessionId)
                .collect { result ->
                    result.fold(
                        onSuccess = { tokenUpdate ->
                            // Update cache with new token
                            tokenCacheService.updateCache(tokenUpdate)
                            _listenerState.value = ListenerState.TokenReceived(tokenUpdate.sequence)
                        },
                        onFailure = { error ->
                            _listenerState.value = ListenerState.Error(
                                error.message ?: "Failed to listen to session"
                            )
                        }
                    )
                }
        }
    }
    
    /**
     * Stop listening when leaving scanner screen
     */
    fun stopTokenListener() {
        tokenCacheService.clearCache()
        _listenerState.value = ListenerState.Idle
    }

    /**
     * Submit attendance after scanning QR
     * 
     * Flow:
     * 1. Log scan attempt with system time
     * 2. Validate QR locally using TokenCacheService
     * 3. If rejected, send validation failure report to server
     * 4. If valid, submit attendance
     * 
     * @param qrPayload Scanned QR code string
     */
    fun submitAttendance(qrPayload: String) {
        viewModelScope.launch {
            _attendanceState.value = AttendanceState.Loading

            // CRITICAL: Capture system time at scan moment
            val scanSystemTime = System.currentTimeMillis()
            val scanTimestamp = scanSystemTime / 1000  // Unix timestamp
            
            Log.d(TAG, "========================================")
            Log.d(TAG, "QR SCANNED")
            Log.d(TAG, "System Time: $scanSystemTime ms")
            Log.d(TAG, "Unix Timestamp: $scanTimestamp")
            Log.d(TAG, "Payload Preview: ${qrPayload.take(50)}...")
            Log.d(TAG, "========================================")

            // Step 1: Local validation using cached tokens
            val (isValid, validationError) = tokenCacheService.validateScannedToken(qrPayload)
            
            if (!isValid) {
                Log.e(TAG, "❌ VALIDATION FAILED")
                Log.e(TAG, "Reason: $validationError")
                Log.e(TAG, "Scan Time: $scanTimestamp")
                Log.e(TAG, "Cached Token: ${tokenCacheService.getCurrentToken()?.take(50)}")
                
                // Send validation failure report to server for analysis
                try {
                    val sessionId = tokenCacheService.getSessionId() ?: "unknown"
                    val user = authRepository.getCurrentUser()
                    val idToken = user?.getIdToken(false)?.await()?.token
                    
                    if (idToken != null) {
                        apiService.reportValidationFailure(
                            idToken = "Bearer $idToken",
                            request = ValidationFailureRequest(
                                session_id = sessionId,
                                scanned_payload = qrPayload,
                                scan_timestamp = scanTimestamp,
                                validation_error = validationError ?: "Unknown error",
                                cached_token_preview = tokenCacheService.getCurrentToken()?.take(100) ?: "none"
                            )
                        )
                        Log.d(TAG, "Validation failure reported to server")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to report validation failure: ${e.message}")
                }
                
                _attendanceState.value = AttendanceState.Error(
                    validationError ?: "QR validation failed"
                )
                return@launch
            }
            
            Log.d(TAG, "✅ VALIDATION PASSED")
            Log.d(TAG, "Proceeding to attendance submission...")

            // Step 2: Get device and user info
            val fid = deviceBindingService.getFirebaseInstallationId()
            if (fid == null) {
                Log.e(TAG, "Failed to get device FID")
                _attendanceState.value = AttendanceState.Error("Failed to retrieve Device ID")
                return@launch
            }

            // Generate nonce for integrity check
            val nonce = generateNonce(qrPayload, fid)
            val integrityToken = securityService.getIntegrityToken(nonce)

            if (integrityToken == null) {
                Log.e(TAG, "Play Integrity check failed")
                _attendanceState.value = AttendanceState.Error("Security integrity check failed")
                return@launch
            }

            val user = authRepository.getCurrentUser()
            val idToken = user?.getIdToken(true)?.await()?.token
            
            if (idToken == null) {
                Log.e(TAG, "Firebase Auth token invalid")
                _attendanceState.value = AttendanceState.Error("User session expired")
                return@launch
            }

            // Step 3: Submit attendance to backend
            try {
                val sessionId = tokenCacheService.getSessionId() ?: "unknown"
                
                Log.d(TAG, "Submitting attendance...")
                Log.d(TAG, "Session ID: $sessionId")
                Log.d(TAG, "Scan Timestamp: $scanTimestamp")
                
                val response = apiService.submitAttendance(
                    idToken = "Bearer $idToken",
                    request = AttendanceRequest(
                        qr_payload = qrPayload,
                        device_fid = fid,
                        integrity_token = integrityToken,
                        session_id = sessionId,
                        scan_timestamp = scanTimestamp  // Send scan time to server
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "✅ Attendance marked successfully!")
                    _attendanceState.value = AttendanceState.Success
                } else {
                    val errorMsg = response.body()?.message ?: "Attendance submission failed"
                    Log.e(TAG, "Server rejected attendance: $errorMsg")
                    _attendanceState.value = AttendanceState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error during submission: ${e.message}", e)
                _attendanceState.value = AttendanceState.Error("Network error: ${e.message}")
            }
        }
    }
    
    companion object {
        private const val TAG = "AttendanceViewModel"
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

sealed class ListenerState {
    object Idle : ListenerState()
    data class Listening(val sessionId: String) : ListenerState()
    data class TokenReceived(val sequence: Int) : ListenerState()
    data class Error(val message: String) : ListenerState()
}
