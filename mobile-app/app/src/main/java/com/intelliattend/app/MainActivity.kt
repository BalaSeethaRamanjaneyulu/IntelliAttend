package com.intelliattend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.intelliattend.app.ui.auth.AuthHubScreen
import com.intelliattend.app.ui.auth.LoginScreen
import com.intelliattend.app.ui.faculty.LiveMonitorScreen
import com.intelliattend.app.ui.faculty.SmartBoardLinkScreen
import com.intelliattend.app.ui.history.AttendanceHistoryScreen
import com.intelliattend.app.ui.onboarding.PermissionsSetupScreen
import com.intelliattend.app.ui.onboarding.ProfileSetupScreen
import com.intelliattend.app.ui.profile.DeveloperInfoScreen
import com.intelliattend.app.ui.profile.ProfileScreen
import com.intelliattend.app.ui.profile.BleDeviceListScreen
import com.intelliattend.app.ui.profile.QrDebugScannerScreen
import com.intelliattend.app.ui.scanner.AttendanceSuccessScreen
import com.intelliattend.app.ui.scanner.QrScannerScreen
import com.intelliattend.app.ui.scanner.ScanResult
import com.intelliattend.app.ui.splash.SplashScreen
import com.intelliattend.app.ui.student.StudentDashboardScreen
import com.intelliattend.app.ui.theme.IntelliAttendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            IntelliAttendTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("splash") {
                            SplashScreen(onTimeout = {
                                navController.navigate("auth_hub") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            })
                        }

                        composable("auth_hub") {
                            AuthHubScreen(
                                onLoginAsStudent = { navController.navigate("login") },
                                onLoginAsFaculty = { navController.navigate("login") }
                            )
                        }

                        composable("login") {
                            LoginScreen(onLoginSuccess = { role ->
                                navController.navigate("profile_setup") {
                                    popUpTo("login") { inclusive = true }
                                }
                            })
                        }

                        composable("profile_setup") {
                            ProfileSetupScreen(
                                onNext = { navController.navigate("permissions_setup") },
                                onBack = { navController.popBackStack() },
                                onSkip = { navController.navigate("permissions_setup") }
                            )
                        }

                        composable("permissions_setup") {
                            PermissionsSetupScreen(
                                onContinue = { 
                                    navController.navigate("student_dashboard") {
                                        popUpTo("permissions_setup") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("student_dashboard") {
                            StudentDashboardScreen(
                                onScanQR = { navController.navigate("qr_scanner") },
                                onAttendanceHistory = { navController.navigate("attendance_history") },
                                onProfile = { navController.navigate("profile") },
                                onSettings = { /* Handle settings */ }
                            )
                        }

                        composable("qr_scanner") {
                            QrScannerScreen(
                                onBack = { navController.popBackStack() },
                                onResult = { result ->
                                    navController.navigate("attendance_success/${result.subject}/${result.room}") {
                                        popUpTo("qr_scanner") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "attendance_success/{subject}/{room}",
                            arguments = listOf(
                                navArgument("subject") { type = NavType.StringType },
                                navArgument("room") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val subject = backStackEntry.arguments?.getString("subject") ?: ""
                            val room = backStackEntry.arguments?.getString("room") ?: ""
                            AttendanceSuccessScreen(
                                subject = subject,
                                room = room,
                                onDone = {
                                    navController.popBackStack("student_dashboard", inclusive = false)
                                }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                onBack = { navController.popBackStack() },
                                onDeveloperInfo = { navController.navigate("developer_info") },
                                onLogout = {
                                    navController.navigate("auth_hub") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("developer_info") {
                            DeveloperInfoScreen(
                                onBack = { navController.popBackStack() },
                                onViewBleDevices = { navController.navigate("ble_device_list") },
                                onQrDebug = { navController.navigate("qr_debug_scanner") }
                            )
                        }

                        composable("qr_debug_scanner") {
                            QrDebugScannerScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("ble_device_list") {
                            BleDeviceListScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("attendance_history") {
                            AttendanceHistoryScreen(
                                onBack = { navController.popBackStack() },
                                onItemClick = { /* Handle item click */ }
                            )
                        }

                        composable("faculty_dashboard") {
                            LiveMonitorScreen(
                                onBack = { navController.popBackStack() },
                                onSettings = { /* Handle settings */ }
                            )
                        }

                        composable("smart_board_link") {
                            SmartBoardLinkScreen(
                                onClose = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
