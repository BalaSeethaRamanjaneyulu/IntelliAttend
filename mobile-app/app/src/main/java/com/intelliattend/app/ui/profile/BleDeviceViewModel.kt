package com.intelliattend.app.ui.profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val proximity: String,
    val isRegistered: Boolean = false
)

data class BleDeviceState(
    val devices: List<BleDevice> = emptyList(),
    val isScanning: Boolean = false,
    val registeredDevice: BleDevice? = null,
    val isScanningForRegistered: Boolean = false,
    val hasBluetoothPermission: Boolean = false
)

@HiltViewModel
class BleDeviceViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("ble_devices", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(BleDeviceState())
    val state: StateFlow<BleDeviceState> = _state.asStateFlow()

    private var scanCallback: ScanCallback? = null
    private val deviceMap = mutableMapOf<String, BleDevice>()

    init {
        checkPermissions()
        loadRegisteredDevice()
    }

    private fun checkPermissions() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
        
        _state.value = _state.value.copy(hasBluetoothPermission = hasPermission)
    }

    private fun loadRegisteredDevice() {
        val name = sharedPrefs.getString("registered_device_name", null)
        val address = sharedPrefs.getString("registered_device_address", null)
        
        if (name != null && address != null) {
            _state.value = _state.value.copy(
                registeredDevice = BleDevice(
                    name = name,
                    address = address,
                    rssi = 0,
                    proximity = "Unknown",
                    isRegistered = true
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (!_state.value.hasBluetoothPermission) {
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return
        }

        stopScanning() // Stop any existing scan
        deviceMap.clear()
        
        _state.value = _state.value.copy(isScanning = true, devices = emptyList())

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { scanResult ->
                    val device = BleDevice(
                        name = scanResult.device.name ?: "Unknown Device",
                        address = scanResult.device.address,
                        rssi = scanResult.rssi,
                        proximity = calculateProximity(scanResult.rssi),
                        isRegistered = scanResult.device.address == _state.value.registeredDevice?.address
                    )
                    
                    deviceMap[device.address] = device
                    updateDeviceList()
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _state.value = _state.value.copy(isScanning = false)
            }
        }

        try {
            bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(isScanning = false)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        scanCallback?.let { callback ->
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        scanCallback = null
        _state.value = _state.value.copy(isScanning = false, isScanningForRegistered = false)
    }

    fun registerDevice(device: BleDevice) {
        viewModelScope.launch {
            sharedPrefs.edit().apply {
                putString("registered_device_name", device.name)
                putString("registered_device_address", device.address)
                apply()
            }
            
            _state.value = _state.value.copy(registeredDevice = device.copy(isRegistered = true))
            updateDeviceList() // Update the list to reflect registration
        }
    }

    fun unregisterDevice() {
        viewModelScope.launch {
            sharedPrefs.edit().apply {
                remove("registered_device_name")
                remove("registered_device_address")
                apply()
            }
            
            _state.value = _state.value.copy(registeredDevice = null)
            updateDeviceList()
        }
    }

    @SuppressLint("MissingPermission")
    fun scanForRegisteredDevice() {
        val registered = _state.value.registeredDevice ?: return
        
        if (!_state.value.hasBluetoothPermission) {
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return
        }

        stopScanning()
        _state.value = _state.value.copy(isScanningForRegistered = true)

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { scanResult ->
                    if (scanResult.device.address == registered.address) {
                        val updatedDevice = registered.copy(
                            rssi = scanResult.rssi,
                            proximity = calculateProximity(scanResult.rssi)
                        )
                        _state.value = _state.value.copy(registeredDevice = updatedDevice)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _state.value = _state.value.copy(isScanningForRegistered = false)
            }
        }

        try {
            bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(isScanningForRegistered = false)
        }
    }

    private fun updateDeviceList() {
        val devices = deviceMap.values.map { device ->
            device.copy(isRegistered = device.address == _state.value.registeredDevice?.address)
        }.sortedByDescending { it.rssi }
        
        _state.value = _state.value.copy(devices = devices)
    }

    private fun calculateProximity(rssi: Int): String {
        val distance = 10.0.pow(((-69 - rssi) / (10 * 2.0)))
        return when {
            distance < 1.0 -> "Near (${String.format("%.1f", distance)}m)"
            distance < 3.0 -> "Medium (${String.format("%.1f", distance)}m)"
            else -> "Far (${String.format("%.1f", distance)}m)"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }
}
