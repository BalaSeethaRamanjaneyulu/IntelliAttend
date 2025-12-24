package com.intelliattend.app.ui.profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

data class DeviceDiagnostics(
    val wifiSsid: String = "Not Connected",
    val wifiSignal: String = "N/A",
    val ipAddress: String = "N/A",
    val isWifiConnected: Boolean = false,
    val hasWifiPermission: Boolean = false,
    
    val bluetoothName: String = "N/A",
    val bluetoothAddress: String = "N/A",
    val nearestBeaconUuid: String = "No beacons detected",
    val beaconProximity: String = "N/A",
    val beaconMajor: String = "N/A",
    val beaconMinor: String = "N/A",
    val isBluetoothScanning: Boolean = false,
    val hasBluetoothPermission: Boolean = false,
    
    // Registered device tracking
    val hasRegisteredDevice: Boolean = false,
    val registeredDeviceName: String = "No device registered",
    val registeredDeviceId: String = "N/A",
    val targetBeaconRssi: String = "N/A",
    val targetBeaconProximity: String = "Scanning...",
    val isTargetWithinRange: Boolean = false,
    val targetRangeStatus: String = "Searching...",
    
    val latitude: String = "N/A",
    val longitude: String = "N/A",
    val accuracy: String = "N/A",
    val geofenceStatus: String = "Unknown",
    val isGpsActive: Boolean = false,
    val hasLocationPermission: Boolean = false,
    
    val deviceModel: String = Build.MODEL,
    val deviceManufacturer: String = Build.MANUFACTURER,
    val osVersion: String = "Android ${Build.VERSION.RELEASE}",
    val sdkVersion: String = "API ${Build.VERSION.SDK_INT}",
    val deviceId: String = "N/A",
    val appVersion: String = "N/A"
)

@HiltViewModel
class DeveloperInfoViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val sharedPrefs = context.getSharedPreferences("ble_devices", Context.MODE_PRIVATE)

    private val _diagnostics = MutableStateFlow(DeviceDiagnostics())
    val diagnostics: StateFlow<DeviceDiagnostics> = _diagnostics.asStateFlow()

    // Campus coordinates for geofence (example - replace with actual coordinates)
    private val campusLat = 37.7749
    private val campusLng = -122.4194
    private val campusRadius = 500.0 // meters
    
    // 12-second timeout for registered device detection
    private val SCAN_TIMEOUT_MS = 12000L
    private var scanStartTime = 0L
    private var registeredDeviceFound = false

    init {
        loadDeviceInfo()
        loadRegisteredDevice()
        loadWifiInfo()
        loadBluetoothInfo()
        loadLocationInfo()
    }

    private fun loadDeviceInfo() {
        viewModelScope.launch {
            try {
                val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val appVersion = "${packageInfo.versionName} (Build ${packageInfo.versionCode})"

                _diagnostics.value = _diagnostics.value.copy(
                    deviceId = deviceId ?: "Unknown",
                    appVersion = appVersion
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadWifiInfo() {
        viewModelScope.launch {
            try {
                val hasPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
                _diagnostics.value = _diagnostics.value.copy(hasWifiPermission = hasPermission)
                
                if (hasPermission) {
                    
                    val wifiInfo: WifiInfo? = wifiManager.connectionInfo
                    
                    if (wifiInfo != null && wifiInfo.networkId != -1) {
                        val ssid = wifiInfo.ssid.replace("\"", "")
                        val rssi = wifiInfo.rssi
                        val signalStrength = "$rssi dBm"
                        val ip = formatIpAddress(wifiInfo.ipAddress)
                        
                        _diagnostics.value = _diagnostics.value.copy(
                            wifiSsid = ssid,
                            wifiSignal = signalStrength,
                            ipAddress = ip,
                            isWifiConnected = true
                        )
                    } else {
                        _diagnostics.value = _diagnostics.value.copy(
                            wifiSsid = "Not Connected",
                            isWifiConnected = false
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadBluetoothInfo() {
        viewModelScope.launch {
            try {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                } else {
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                }
                _diagnostics.value = _diagnostics.value.copy(hasBluetoothPermission = hasPermission)
                
                val bluetoothAdapter = bluetoothManager?.adapter
                
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                    val name = bluetoothAdapter.name ?: "Unknown"
                    val address = bluetoothAdapter.address ?: "N/A"
                    
                    _diagnostics.value = _diagnostics.value.copy(
                        bluetoothName = name,
                        bluetoothAddress = address
                    )

                    // Start scanning for registered device if permission granted and device is registered
                    if (hasPermission && _diagnostics.value.hasRegisteredDevice) {
                        startRegisteredDeviceScan()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRegisteredDevice() {
        val name = sharedPrefs.getString("registered_device_name", null)
        val address = sharedPrefs.getString("registered_device_address", null)
        
        if (name != null && address != null) {
            _diagnostics.value = _diagnostics.value.copy(
                hasRegisteredDevice = true,
                registeredDeviceName = name,
                registeredDeviceId = address
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRegisteredDeviceScan() {
        val registeredDeviceId = _diagnostics.value.registeredDeviceId
        if (registeredDeviceId == "N/A") return

        try {
            val bluetoothAdapter = bluetoothManager?.adapter
            val scanner = bluetoothAdapter?.bluetoothLeScanner

            if (scanner != null) {
                registeredDeviceFound = false
                scanStartTime = System.currentTimeMillis()
                _diagnostics.value = _diagnostics.value.copy(
                    isBluetoothScanning = true,
                    targetRangeStatus = "Searching...",
                    targetBeaconProximity = "Scanning..."
                )

                scanner.startScan(object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        result?.let { scanResult ->
                            val currentTime = System.currentTimeMillis()
                            val elapsedTime = currentTime - scanStartTime
                            
                            // Check if this is our registered device
                            if (scanResult.device.address == registeredDeviceId) {
                                registeredDeviceFound = true
                                val rssi = scanResult.rssi
                                val proximity = calculateProximity(rssi)
                                val distance = calculateDistance(rssi)
                                val withinRange = distance < 3.0 // Within 3 meters
                                
                                _diagnostics.value = _diagnostics.value.copy(
                                    targetBeaconRssi = "$rssi dBm",
                                    targetBeaconProximity = proximity,
                                    isTargetWithinRange = withinRange,
                                    targetRangeStatus = if (withinRange) "Within Range" else "Out of Range"
                                )
                            }
                            
                            // Stop after 12 seconds
                            if (elapsedTime >= SCAN_TIMEOUT_MS) {
                                scanner.stopScan(this)
                                _diagnostics.value = _diagnostics.value.copy(
                                    isBluetoothScanning = false,
                                    targetRangeStatus = if (registeredDeviceFound) 
                                        _diagnostics.value.targetRangeStatus 
                                    else "Device Not Found"
                                )
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        _diagnostics.value = _diagnostics.value.copy(
                            isBluetoothScanning = false,
                            targetRangeStatus = "Scan Failed"
                        )
                    }
                })
                
                // Set timeout to stop scan after 12 seconds
                viewModelScope.launch {
                    kotlinx.coroutines.delay(SCAN_TIMEOUT_MS)
                    if (_diagnostics.value.isBluetoothScanning) {
                        try {
                            scanner.stopScan(object : ScanCallback() {})
                            _diagnostics.value = _diagnostics.value.copy(
                                isBluetoothScanning = false,
                                targetRangeStatus = if (registeredDeviceFound) 
                                    _diagnostics.value.targetRangeStatus 
                                else "Device Not Found"
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun calculateDistance(rssi: Int): Double {
        return 10.0.pow(((-69 - rssi) / (10 * 2.0)))
    }

    @SuppressLint("MissingPermission")
    private fun loadLocationInfo() {
        viewModelScope.launch {
            try {
                val hasPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                _diagnostics.value = _diagnostics.value.copy(hasLocationPermission = hasPermission)
                
                if (hasPermission) {
                    
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location: Location? ->
                            location?.let {
                                val lat = String.format("%.4f° %s", abs(it.latitude), 
                                    if (it.latitude >= 0) "N" else "S")
                                val lng = String.format("%.4f° %s", abs(it.longitude), 
                                    if (it.longitude >= 0) "E" else "W")
                                val acc = "+/- ${it.accuracy.toInt()}m"
                                val geofence = checkGeofence(it.latitude, it.longitude)

                                _diagnostics.value = _diagnostics.value.copy(
                                    latitude = lat,
                                    longitude = lng,
                                    accuracy = acc,
                                    geofenceStatus = geofence,
                                    isGpsActive = true
                                )
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatIpAddress(ip: Int): String {
        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }

    private fun calculateProximity(rssi: Int): String {
        val distance = 10.0.pow(((-69 - rssi) / (10 * 2.0)))
        return when {
            distance < 1.0 -> "Near (${String.format("%.1f", distance)}m)"
            distance < 3.0 -> "Medium (${String.format("%.1f", distance)}m)"
            else -> "Far (${String.format("%.1f", distance)}m)"
        }
    }

    private fun checkGeofence(lat: Double, lng: Double): String {
        val distance = calculateDistance(lat, lng, campusLat, campusLng)
        return if (distance <= campusRadius) "Inside Campus" else "Outside Campus"
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun refresh() {
        loadWifiInfo()
        loadBluetoothInfo()
        loadLocationInfo()
    }

    override fun onCleared() {
        super.onCleared()
        // Stop BLE scanning when ViewModel is cleared
        try {
            bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(object : ScanCallback() {})
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
