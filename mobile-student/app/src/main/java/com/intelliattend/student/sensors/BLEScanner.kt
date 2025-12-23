package com.intelliattend.student.sensors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.intelliattend.student.data.model.BLESample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant

/**
 * BLE Beacon Scanner Service
 * Scans for configured beacon UUIDs and collects RSSI samples
 */
@Singleton
class BLEScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    
    private val _bleSamples = MutableStateFlow<List<BLESample>>(emptyList())
    val bleSamples: StateFlow<List<BLESample>> = _bleSamples
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            // Extract UUID from scan record
            val uuid = result.device.address // Simplified - in production, parse actual UUID
            val rssi = result.rssi
            val timestamp = Instant.now().toString()
            
            // Add sample to list
            val sample = BLESample(
                uuid = uuid,
                rssi = rssi,
                timestamp = timestamp
            )
            
            val currentSamples = _bleSamples.value.toMutableList()
            currentSamples.add(sample)
            
            // Keep only last 50 samples
            if (currentSamples.size > 50) {
                currentSamples.removeAt(0)
            }
            
            _bleSamples.value = currentSamples
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _isScanning.value = false
        }
    }
    
    /**
     * Start BLE scanning
     */
    fun startScanning(): Boolean {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        
        // Check if Bluetooth is enabled
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return false
        }
        
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        
        // Configure scan settings for low latency
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        try {
            bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
            _isScanning.value = true
            return true
        } catch (e: SecurityException) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Stop BLE scanning
     */
    fun stopScanning() {
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            _isScanning.value = false
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    /**
     * Clear collected samples
     */
    fun clearSamples() {
        _bleSamples.value = emptyList()
    }
    
    /**
     * Get samples for specific UUIDs
     */
    fun getSamplesForUUIDs(expectedUUIDs: List<String>): List<BLESample> {
        return _bleSamples.value.filter { sample ->
            expectedUUIDs.any { uuid ->
                sample.uuid.contains(uuid, ignoreCase = true)
            }
        }
    }
    
    /**
     * Calculate average RSSI for a UUID
     */
    fun getAverageRSSI(uuid: String): Int? {
        val samples = _bleSamples.value.filter {
            it.uuid.contains(uuid, ignoreCase = true)
        }
        
        return if (samples.isNotEmpty()) {
            samples.map { it.rssi }.average().toInt()
        } else {
            null
        }
    }
}
