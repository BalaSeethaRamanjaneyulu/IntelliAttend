package com.intelliattend.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("api/v1/attendance/submit")
    suspend fun submitAttendance(
        @Header("Authorization") idToken: String,
        @Body request: AttendanceRequest
    ): Response<AttendanceResponse>
    
    @POST("api/v1/attendance/validation-failure")
    suspend fun reportValidationFailure(
        @Header("Authorization") idToken: String,
        @Body request: ValidationFailureRequest
    ): Response<GenericResponse>

    @POST("attendance/scan-qr")
    suspend fun scanQr(
        @Body request: ScanQRRequest
    ): Response<ScanQRResponse>
}

data class ScanQRRequest(
    val session_id: String,
    val student_id: String,
    val qr_token: String,
    val location_data: LocationData? = null
)

data class LocationData(
    val gps: GPSLocation? = null,
    val wifi_ssid: String? = null,
    val wifi_bssid: String? = null,
    val bluetooth_beacon: BluetoothBeacon? = null
)

data class GPSLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null
)

data class BluetoothBeacon(
    val uuid: String,
    val major: Int,
    val minor: Int
)

data class ScanQRResponse(
    val success: Boolean,
    val message: String,
    val attendance_id: String? = null,
    val status: String? = null
)

data class AttendanceRequest(
    val qr_payload: String,
    val device_fid: String,
    val integrity_token: String,
    val session_id: String,
    val scan_timestamp: Long  // Unix timestamp (seconds)
)

data class ValidationFailureRequest(
    val session_id: String,
    val scanned_payload: String,
    val scan_timestamp: Long,
    val validation_error: String,
    val cached_token_preview: String
)

data class AttendanceResponse(
    val success: Boolean,
    val message: String
)

data class GenericResponse(
    val success: Boolean,
    val message: String
)
