package com.intelliattend.student.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WiFi Manager Service
 * Detects WiFi SSID and BSSID for network-based verification
 */
@Singleton
class WiFiManagerService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    private val _wifiInfo = MutableStateFlow<WiFiData?>(null)
    val wifiInfo: StateFlow<WiFiData?> = _wifiInfo
    
    /**
     * Get current WiFi information
     */
    fun getCurrentWiFiInfo(): WiFiData? {
        // Check permission (required on Android 10+)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        if (!wifiManager.isWifiEnabled) {
            return null
        }
        
        try {
            val connectionInfo: WifiInfo = wifiManager.connectionInfo
            
            // Extract SSID (remove quotes)
            var ssid = connectionInfo.ssid
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length - 1)
            }
            
            // Extract BSSID (MAC address of access point)
            val bssid = connectionInfo.bssid
            
            // Check if actually connected
            if (ssid == "<unknown ssid>" || bssid == null) {
                return null
            }
            
            val wifiData = WiFiData(
                ssid = ssid,
                bssid = bssid.uppercase(),
                signalStrength = WifiManager.calculateSignalLevel(
                    connectionInfo.rssi,
                    5
                )
            )
            
            _wifiInfo.value = wifiData
            return wifiData
            
        } catch (e: SecurityException) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Check if connected to specific network
     */
    fun isConnectedTo(expectedBSSID: String): Boolean {
        val current = getCurrentWiFiInfo() ?: return false
        return current.bssid.equals(expectedBSSID, ignoreCase = true)
    }
    
    /**
     * Refresh WiFi information
     */
    fun refresh() {
        getCurrentWiFiInfo()
    }
}

/**
 * WiFi data container
 */
data class WiFiData(
    val ssid: String,
    val bssid: String,
    val signalStrength: Int // 0-4 level
)
