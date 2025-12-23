package com.intelliattend.student.data.repository

import com.intelliattend.student.data.local.PreferencesManager
import com.intelliattend.student.data.model.*
import com.intelliattend.student.data.remote.ApiService
import com.intelliattend.student.sensors.BLEScanner
import com.intelliattend.student.sensors.GPSLocationService
import com.intelliattend.student.sensors.WiFiManagerService
import com.intelliattend.student.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for attendance operations
 * Combines sensor data and submits to backend
 */
@Singleton
class AttendanceRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager,
    private val bleScanner: BLEScanner,
    private val gpsLocationService: GPSLocationService,
    private val wifiManagerService: WiFiManagerService
) {
    
    /**
     * Submit attendance with multi-factor data
     */
    fun submitAttendance(
        qrToken: String,
        sessionId: String,
        deviceId: String
    ): Flow<Resource<AttendanceSubmitResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = preferencesManager.accessToken.first()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }
            
            val studentId = preferencesManager.studentId.first()
            if (studentId == null) {
                emit(Resource.Error("Student ID not found"))
                return@flow
            }
            
            // Collect BLE samples
            val bleSamples = bleScanner.bleSamples.first()
            
            // Get GPS location
            val location = gpsLocationService.getCurrentLocationData()
            
            // Get WiFi info
            val wifi = wifiManagerService.getCurrentWiFiInfo()
            
            // Build scan sample data
            val scanSamples = ScanSampleData(
                bleSamples = bleSamples,
                wifiSsid = wifi?.ssid,
                wifiBssid = wifi?.bssid,
                gpsLatitude = location?.latitude,
                gpsLongitude = location?.longitude,
                gpsAccuracy = location?.accuracy,
                deviceId = deviceId
            )
            
            // Create request
            val request = AttendanceSubmitRequest(
                studentId = studentId,
                qrToken = qrToken,
                sessionId = sessionId,
                scanSamples = scanSamples
            )
            
            // Submit to API
            val response = apiService.submitAttendance("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Submission failed"
                emit(Resource.Error(errorMsg))
            }
            
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error submitting attendance"))
        }
    }
    
    /**
     * Get verification details for an attendance record
     */
    fun getVerificationDetails(
        attendanceId: Int
    ): Flow<Resource<VerificationResult>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = preferencesManager.accessToken.first()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }
            
            val response = apiService.getVerificationDetails(
                "Bearer $token",
                attendanceId
            )
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to load verification details"))
            }
            
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error loading details"))
        }
    }
    
    /**
     * Get current sensor status summary
     */
    fun getSensorStatus(): SensorStatus {
        val bleCount = bleScanner.bleSamples.value.size
        val gpsLocation = gpsLocationService.currentLocation.value
        val wifi = wifiManagerService.wifiInfo.value
        
        return SensorStatus(
            bleEnabled = bleScanner.isScanning.value,
            bleSampleCount = bleCount,
            gpsEnabled = gpsLocationService.isTracking.value,
            gpsAccuracy = gpsLocation?.accuracy,
            wifiConnected = wifi != null,
            wifiBssid = wifi?.bssid
        )
    }
}

/**
 * Sensor status summary
 */
data class SensorStatus(
    val bleEnabled: Boolean,
    val bleSampleCount: Int,
    val gpsEnabled: Boolean,
    val gpsAccuracy: Float?,
    val wifiConnected: Boolean,
    val wifiBssid: String?
)
