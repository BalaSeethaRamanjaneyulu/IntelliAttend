package com.intelliattend.student.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GPS Location Service
 * Tracks device location with high accuracy for geofencing
 */
@Singleton
class GPSLocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                _currentLocation.value = location
            }
        }
    }
    
    /**
     * Start location tracking
     */
    fun startTracking(): Boolean {
        // Check permissions
        if (!hasLocationPermission()) {
            return false
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // 5 seconds interval
        )
            .setMinUpdateIntervalMillis(2000L) // 2 seconds fastest
            .setMaxUpdateDelayMillis(10000L) // 10 seconds max delay
            .build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            _isTracking.value = true
            
            // Get last known location immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    _currentLocation.value = it
                }
            }
            
            return true
        } catch (e: SecurityException) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Stop location tracking
     */
    fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            _isTracking.value = false
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get current location data
     */
    fun getCurrentLocationData(): LocationData? {
        val location = _currentLocation.value ?: return null
        
        return LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy
        )
    }
    
    /**
     * Calculate distance to target location (meters)
     */
    fun getDistanceTo(targetLat: Double, targetLon: Double): Float? {
        val current = _currentLocation.value ?: return null
        
        val results = FloatArray(1)
        Location.distanceBetween(
            current.latitude,
            current.longitude,
            targetLat,
            targetLon,
            results
        )
        
        return results[0]
    }
    
    /**
     * Check if within geofence radius
     */
    fun isWithinGeofence(
        targetLat: Double,
        targetLon: Double,
        radiusMeters: Float
    ): Boolean {
        val distance = getDistanceTo(targetLat, targetLon) ?: return false
        return distance <= radiusMeters
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Location data container
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)
