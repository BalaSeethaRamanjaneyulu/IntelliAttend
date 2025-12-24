package com.intelliattend.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DeveloperInfoScreen(
    onBack: () -> Unit,
    onViewBleDevices: () -> Unit = {},
    onQrDebug: () -> Unit = {},
    viewModel: DeveloperInfoViewModel = hiltViewModel()
) {
    val diagnostics by viewModel.diagnostics.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111521))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = Color(0xFF111521).copy(alpha = 0.95f),
                modifier = Modifier.statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Developer Info",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                // Connectivity Section
                item {
                    InfoSection(
                        title = "Connectivity",
                        icon = Icons.Default.Wifi,
                        iconColor = Color(0xFF1748CF),
                        status = if (diagnostics.isWifiConnected) "Connected" else "Disconnected",
                        statusColor = if (diagnostics.isWifiConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                        hasPermission = diagnostics.hasWifiPermission
                    ) {
                        if (diagnostics.hasWifiPermission) {
                            InfoRow(label = "SSID", value = diagnostics.wifiSsid)
                            InfoRow(
                                label = "Signal Strength", 
                                value = diagnostics.wifiSignal,
                                icon = if (diagnostics.isWifiConnected) Icons.Default.SignalWifi4Bar else null,
                                iconColor = Color(0xFF10B981)
                            )
                            InfoRow(label = "IP Address", value = diagnostics.ipAddress, isMono = true, canCopy = true)
                        } else {
                            PermissionWarning()
                        }
                    }
                }

                // Bluetooth Section with Registered Device Tracking
                item {
                    InfoSection(
                        title = "Bluetooth (BLE)",
                        icon = Icons.Default.Bluetooth,
                        iconColor = Color(0xFF6366F1),
                        status = if (diagnostics.isBluetoothScanning) "Scanning" else "Idle",
                        statusColor = if (diagnostics.isBluetoothScanning) Color(0xFF3B82F6) else Color(0xFF94A3B8),
                        hasPermission = diagnostics.hasBluetoothPermission
                    ) {
                        if (diagnostics.hasBluetoothPermission) {
                            InfoRow(label = "Device Name", value = diagnostics.bluetoothName)
                            InfoRow(label = "Device Address", value = diagnostics.bluetoothAddress, isMono = true)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Registered Device Section
                            if (diagnostics.hasRegisteredDevice) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E3B8A).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Registered Target Device",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        InfoRow(
                                            label = "Device Name",
                                            value = diagnostics.registeredDeviceName
                                        )
                                        InfoRow(
                                            label = "Device ID",
                                            value = diagnostics.registeredDeviceId,
                                            isMono = true
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Target Beacon Tracking
                                        Text(
                                            text = "Target Beacon Detection",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                InfoRow(
                                                    label = "Signal (RSSI)",
                                                    value = diagnostics.targetBeaconRssi
                                                )
                                            }
                                            Box(modifier = Modifier.weight(1f)) {
                                                InfoRow(
                                                    label = "Proximity",
                                                    value = diagnostics.targetBeaconProximity
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Range Status Badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (diagnostics.targetRangeStatus) {
                                                "Within Range" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                "Out of Range" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                "Device Not Found" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                "Searching..." -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                else -> Color(0xFF94A3B8).copy(alpha = 0.2f)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when (diagnostics.targetRangeStatus) {
                                                        "Within Range" -> Icons.Default.CheckCircle
                                                        "Out of Range" -> Icons.Default.Cancel
                                                        "Device Not Found" -> Icons.Default.SearchOff
                                                        else -> Icons.Default.Search
                                                    },
                                                    contentDescription = null,
                                                    tint = when (diagnostics.targetRangeStatus) {
                                                        "Within Range" -> Color(0xFF10B981)
                                                        "Out of Range" -> Color(0xFFEF4444)
                                                        "Device Not Found" -> Color(0xFFEF4444)
                                                        "Searching..." -> Color(0xFF3B82F6)
                                                        else -> Color(0xFF94A3B8)
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Status: ${diagnostics.targetRangeStatus}",
                                                    color = when (diagnostics.targetRangeStatus) {
                                                        "Within Range" -> Color(0xFF10B981)
                                                        "Out of Range" -> Color(0xFFEF4444)
                                                        "Device Not Found" -> Color(0xFFEF4444)
                                                        "Searching..." -> Color(0xFF3B82F6)
                                                        else -> Color(0xFF94A3B8)
                                                    },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF334155).copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "No target device registered. Visit 'View All BLE Devices' to register one.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedButton(
                                onClick = onViewBleDevices,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF6366F1)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BluetoothSearching,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View All BLE Devices", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            PermissionWarning()
                        }
                    }
                }

                // Location Section
                item {
                    InfoSection(
                        title = "Location Services",
                        icon = Icons.Default.LocationOn,
                        iconColor = Color(0xFFF97316),
                        showCheck = diagnostics.isGpsActive,
                        hasPermission = diagnostics.hasLocationPermission
                     ) {
                        if (diagnostics.hasLocationPermission) {
                            // Map Placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(128.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E2330))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // GPS indicator
                                if (diagnostics.isGpsActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFF1E3B8A).copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color(0xFF1748CF), CircleShape)
                                                .border(2.dp, Color.White, CircleShape)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LocationOff,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                
                                Text(
                                    text = if (diagnostics.isGpsActive) "GPS Active" else "GPS Inactive",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f)) { 
                                    InfoRow(label = "Latitude", value = diagnostics.latitude, isMono = true) 
                                }
                                Box(modifier = Modifier.weight(1f)) { 
                                    InfoRow(label = "Longitude", value = diagnostics.longitude, isMono = true) 
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f)) { 
                                    InfoRow(label = "Accuracy", value = diagnostics.accuracy) 
                                }
                                Box(modifier = Modifier.weight(1f)) { 
                                    InfoRow(
                                        label = "Geofence", 
                                        value = diagnostics.geofenceStatus,
                                        valueColor = if (diagnostics.geofenceStatus == "Inside Campus") 
                                            Color(0xFF10B981) else Color(0xFFEF4444)
                                    ) 
                                }
                            }
                        } else {
                            PermissionWarning()
                        }
                    }
                }

                // Device Info Section
                item {
                    InfoSection(
                        title = "Device Info",
                        icon = Icons.Default.Smartphone,
                        iconColor = Color(0xFF94A3B8)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) { 
                                InfoRow(label = "Model", value = diagnostics.deviceModel) 
                            }
                            Box(modifier = Modifier.weight(1f)) { 
                                InfoRow(label = "Manufacturer", value = diagnostics.deviceManufacturer) 
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) { 
                                InfoRow(label = "OS Version", value = diagnostics.osVersion) 
                            }
                            Box(modifier = Modifier.weight(1f)) { 
                                InfoRow(label = "SDK", value = diagnostics.sdkVersion, isMono = true) 
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "Device ID (Android ID)", value = diagnostics.deviceId, isMono = true, canCopy = true)
                        InfoRow(label = "App Version", value = diagnostics.appVersion, isMono = true)
                    }
                }
            }
        }

        // Bottom Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Button(
                onClick = onQrDebug,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1748CF))
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("QR Code Debugger", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    status: String? = null,
    statusColor: Color = Color.Gray,
    showCheck: Boolean = false,
    hasPermission: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(iconColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                    Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                if (status != null) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                            Text(text = status, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else if (showCheck) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    iconColor: Color = Color.White,
    isMono: Boolean = false,
    canCopy: Boolean = false,
    valueColor: Color = Color.White
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (isMono) androidx.compose.ui.text.font.FontFamily.Monospace else null
                )
            }
            if (canCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionWarning() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFEF4444).copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Permission not granted. Data unavailable.",
                color = Color(0xFFEF4444),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
