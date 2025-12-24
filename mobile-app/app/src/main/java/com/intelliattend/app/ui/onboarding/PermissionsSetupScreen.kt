package com.intelliattend.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

enum class PermissionStatus {
    ALLOWED, NOT_ALLOWED
}

data class Permission(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean,
    val androidPermissions: List<String>,
    var status: PermissionStatus = PermissionStatus.NOT_ALLOWED
)

@Composable
fun PermissionsSetupScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Define permissions based on SDK version
    val locationPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        // On older Android, Bluetooth needs Location permission (which is handled separately)
        // Legacy BLUETOOTH permissions are install-time.
        // We link this card to Location as well for simplicity, or just empty if handled by manifest
        emptyList() 
    }
    
    val cameraPermissions = listOf(Manifest.permission.CAMERA)

    // Helper to check permission status
    fun checkPermissionStatus(perms: List<String>): PermissionStatus {
        if (perms.isEmpty()) return PermissionStatus.ALLOWED
        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        return if (allGranted) PermissionStatus.ALLOWED else PermissionStatus.NOT_ALLOWED
    }

    // State for permissions list
    var permissions by remember {
        mutableStateOf(
            listOf(
                Permission(
                    "Location Access",
                    "Used to verify your proximity to the classroom.",
                    Icons.Default.LocationOn,
                    true,
                    locationPermissions,
                    checkPermissionStatus(locationPermissions)
                ),
                Permission(
                    "Bluetooth",
                    "Connects to classroom beacons for seamless check-in.",
                    Icons.Default.Bluetooth,
                    true,
                    bluetoothPermissions,
                    checkPermissionStatus(bluetoothPermissions)
                ),
                Permission(
                    "Camera",
                    "Used for scanning QR codes during manual attendance.",
                    Icons.Default.PhotoCamera,
                    true,
                    cameraPermissions,
                    checkPermissionStatus(cameraPermissions)
                )
            )
        )
    }

    // Permission Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Refresh status of all permissions when any result returns
        permissions = permissions.map { perm ->
            perm.copy(status = checkPermissionStatus(perm.androidPermissions))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D111B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Profile Setup",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Progress Indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(width = 6.dp, height = 6.dp).background(Color(0xFF1E293B), CircleShape))
                    Box(modifier = Modifier.size(width = 32.dp, height = 6.dp).background(Color(0xFF1E3B8A), CircleShape))
                    Box(modifier = Modifier.size(width = 6.dp, height = 6.dp).background(Color(0xFF1E293B), CircleShape))
                    Box(modifier = Modifier.size(width = 6.dp, height = 6.dp).background(Color(0xFF1E293B), CircleShape))
                }
            }

            // Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            text = "We need a few things to work",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Grant these permissions so IntelliAttend can automatically mark your attendance securely.",
                            color = Color(0xFF94A3B8),
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }

                items(permissions.size) { index ->
                    PermissionCard(
                        permission = permissions[index],
                        onAllow = {
                            if (permissions[index].androidPermissions.isNotEmpty()) {
                                launcher.launch(permissions[index].androidPermissions.toTypedArray())
                            } else {
                                // For legacy bluetooth or empty cases, just mark as allowed purely for UI if needed,
                                // but ideally we rely on checkPermissionStatus which returns ALLOWED if empty.
                                // If it was NOT_ALLOWED yet empty (shouldn't happen with our logic), we force refresh.
                                permissions = permissions.map { perm ->
                                    perm.copy(status = checkPermissionStatus(perm.androidPermissions))
                                }
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        // Fixed Bottom Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF0D111B).copy(alpha = 0.95f))
                .padding(16.dp)
                .padding(bottom = 24.dp)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3B8A)),
                // Only enable continue if all required permissions are granted?
                // For now, allowing continue regardless to avoid blocking user if they deny
                enabled = true 
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PermissionCard(
    permission: Permission,
    onAllow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (permission.status == PermissionStatus.ALLOWED)
                                Color(0xFF10B981).copy(alpha = 0.15f)
                            else
                                Color(0xFF334155),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = permission.icon,
                        contentDescription = null,
                        tint = if (permission.status == PermissionStatus.ALLOWED)
                            Color(0xFF10B981)
                        else
                            Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = permission.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (permission.isRequired) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF334155)
                            ) {
                                Text(
                                    text = "REQUIRED",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = permission.description,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Action Button
            if (permission.status == PermissionStatus.ALLOWED) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Allowed",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onAllow,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3B8A)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Allow",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
