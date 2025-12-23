package com.intelliattend.student.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.intelliattend.student.data.repository.SensorStatus
import com.intelliattend.student.utils.Resource

/**
 * Attendance Submission Screen
 * Shows sensor status and submits attendance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceSubmitScreen(
    qrToken: String,
    deviceId: String,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()
    val sensorStatus by viewModel.sensorStatus.collectAsStateWithLifecycle()
    
    // Extract session ID from QR token
    val sessionId = remember(qrToken) {
        viewModel.extractSessionId(qrToken) ?: ""
    }
    
    // Refresh sensor status on load
    LaunchedEffect(Unit) {
        viewModel.refreshSensorStatus()
    }
    
    // Handle successful submission
    LaunchedEffect(submissionState) {
        if (submissionState is Resource.Success) {
            onSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submit Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // QR Token Info
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "QR Token Scanned",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = qrToken.take(40) + "...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Session: $sessionId",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sensor Status
            sensorStatus?.let { status ->
                SensorStatusCard(status)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Submit Button
            Button(
                onClick = {
                    viewModel.submitAttendance(qrToken, sessionId, deviceId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = submissionState !is Resource.Loading
            ) {
                if (submissionState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, "Submit", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Attendance", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            // Error message
            if (submissionState is Resource.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (submissionState as Resource.Error).message ?: "Submission failed",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Success result
            if (submissionState is Resource.Success) {
                Spacer(modifier = Modifier.height(16.dp))
                
                val result = (submissionState as Resource.Success).data
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result?.verification?.status == "present") {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (result?.verification?.status == "present") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                "Status",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = result?.verification?.status?.uppercase() ?: "UNKNOWN",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Confidence: ${result?.verification?.confidenceScore ?: 0f}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Verification Breakdown:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        result?.verification?.let { v ->
                            ScoreRow("QR Valid", if (v.qrValid) "✓" else "✗")
                            ScoreRow("BLE Score", "${v.bleScore}")
                            ScoreRow("WiFi Score", "${v.wifiScore}")
                            ScoreRow("GPS Score", "${v.gpsScore}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorStatusCard(status: SensorStatus) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sensor Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            StatusRow(
                icon = Icons.Default.Bluetooth,
                label = "BLE Scanner",
                value = if (status.bleEnabled) "${status.bleSampleCount} samples" else "Disabled",
                isActive = status.bleEnabled
            )
            
            StatusRow(
                icon = Icons.Default.LocationOn,
                label = "GPS Location",
                value = status.gpsAccuracy?.let { "${it.toInt()}m accuracy" } ?: "No signal",
                isActive = status.gpsEnabled
            )
            
            StatusRow(
                icon = Icons.Default.Wifi,
                label = "WiFi",
                value = status.wifiBssid ?: "Not connected",
                isActive = status.wifiConnected
            )
        }
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isActive) {
            Icon(
                Icons.Default.CheckCircle,
                "Active",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}
