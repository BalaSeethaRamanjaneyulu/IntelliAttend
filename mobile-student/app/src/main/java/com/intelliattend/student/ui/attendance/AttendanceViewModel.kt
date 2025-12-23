package com.intelliattend.student.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.student.data.model.AttendanceSubmitResponse
import com.intelliattend.student.data.repository.AttendanceRepository
import com.intelliattend.student.data.repository.SensorStatus
import com.intelliattend.student.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Attendance Submission
 * Orchestrates sensor data collection and submission
 */
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {
    
    private val _submissionState = MutableStateFlow<Resource<AttendanceSubmitResponse>?>(null)
    val submissionState: StateFlow<Resource<AttendanceSubmitResponse>?> = _submissionState.asStateFlow()
    
    private val _sensorStatus = MutableStateFlow<SensorStatus?>(null)
    val sensorStatus: StateFlow<SensorStatus?> = _sensorStatus.asStateFlow()
    
    /**
     * Submit attendance with scanned QR token
     */
    fun submitAttendance(
        qrToken: String,
        sessionId: String,
        deviceId: String
    ) {
        viewModelScope.launch {
            attendanceRepository.submitAttendance(
                qrToken = qrToken,
                sessionId = sessionId,
                deviceId = deviceId
            ).collect { resource ->
                _submissionState.value = resource
            }
        }
    }
    
    /**
     * Refresh sensor status
     */
    fun refreshSensorStatus() {
        _sensorStatus.value = attendanceRepository.getSensorStatus()
    }
    
    /**
     * Reset submission state
     */
    fun resetSubmissionState() {
        _submissionState.value = null
    }
    
    /**
     * Extract session ID from QR token
     * Format: IATT_<session_id>_<seq>_<timestamp>_<signature>
     */
    fun extractSessionId(qrToken: String): String? {
        if (!qrToken.startsWith("IATT_")) return null
        
        val parts = qrToken.split("_")
        if (parts.size < 3) return null
        
        // Session ID is parts[1] + parts[2] (SESS_timestamp_random)
        return "${parts[1]}_${parts[2]}_${parts[3]}"
    }
}
