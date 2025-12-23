package com.intelliattend.student

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.intelliattend.student.services.WarmScanService
import com.intelliattend.student.ui.attendance.AttendanceSubmitScreen
import com.intelliattend.student.ui.auth.LoginScreen
import com.intelliattend.student.ui.home.HomeScreen
import com.intelliattend.student.ui.scanner.QRScannerScreen
import com.intelliattend.student.ui.theme.IntelliAttendTheme
import com.intelliattend.student.utils.DeviceIdGenerator
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity
 * Entry point for the app UI with complete navigation
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            IntelliAttendTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    
                    // Get device ID
                    val deviceId = remember {
                        DeviceIdGenerator.getDeviceId(context)
                    }
                    
                    // Scanned QR token holder
                    var scannedQRToken by remember { mutableStateOf<String?>(null) }
                    
                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        // Login
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // Home Dashboard
                        composable("home") {
                            HomeScreen(
                                onScanQR = {
                                    // Start warm scan service
                                    WarmScanService.start(context)
                                    navController.navigate("qr_scanner")
                                },
                                onLogout = {
                                    // Stop warm scan if running
                                    WarmScanService.stop(context)
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // QR Scanner
                        composable("qr_scanner") {
                            QRScannerScreen(
                                onQRScanned = { qrToken ->
                                    scannedQRToken = qrToken
                                    navController.navigate("attendance_submit") {
                                        popUpTo("home")
                                    }
                                },
                                onBack = {
                                    WarmScanService.stop(context)
                                    navController.navigateUp()
                                }
                            )
                        }
                        
                        // Attendance Submission
                        composable("attendance_submit") {
                            val qrToken = scannedQRToken ?: ""
                            
                            AttendanceSubmitScreen(
                                qrToken = qrToken,
                                deviceId = deviceId,
                                onSuccess = {
                                    // Stop warm scan
                                    WarmScanService.stop(context)
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onBack = {
                                    navController.navigateUp()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
