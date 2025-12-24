package com.intelliattend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.intelliattend.app.faculty.QRGeneratorScreen
import com.intelliattend.app.student.QRScannerScreen
import com.intelliattend.app.ui.auth.LoginScreen
import com.intelliattend.app.ui.theme.IntelliAttendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IntelliAttendTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(onLoginSuccess = { role ->
                                if (role == "faculty") {
                                    navController.navigate("faculty_qr/CS101")
                                } else {
                                    navController.navigate("student_scanner")
                                }
                            })
                        }
                        composable("student_scanner") {
                            QRScannerScreen(onScanComplete = {
                                // Navigate to success or dashboard
                            })
                        }
                        composable("faculty_qr/{sessionId}") { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                            QRGeneratorScreen(sessionId = sessionId)
                        }
                    }
                }
            }
        }
    }
}
