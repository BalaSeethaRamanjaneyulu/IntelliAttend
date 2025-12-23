package com.intelliattend.faculty

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
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.intelliattend.faculty.ui.auth.LoginScreen
import com.intelliattend.faculty.ui.dashboard.LiveDashboardScreen
import com.intelliattend.faculty.ui.home.HomeScreen
import com.intelliattend.faculty.ui.session.SessionCreateScreen
import com.intelliattend.faculty.ui.theme.IntelliAttendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            IntelliAttendTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Session state holder
                    var currentSessionId by remember { mutableStateOf<String?>(null) }
                    
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
                        
                        // Home
                        composable("home") {
                            HomeScreen(
                                onStartSession = {
                                    // For demo, use class_id = 3 (CSE101)
                                    navController.navigate("session_create")
                                },
                                onLogout = {
                                    currentSessionId = null
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // Session Create (with biometric)
                        composable("session_create") {
                            SessionCreateScreen(
                                classId = 3, // Demo: CSE101
                                activity = this@MainActivity,
                                onSessionCreated = { sessionId ->
                                    currentSessionId = sessionId
                                    navController.navigate("dashboard") {
                                        popUpTo("home")
                                    }
                                },
                                onBack = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        
                        // Live Dashboard
                        composable("dashboard") {
                            currentSessionId?.let { sessionId ->
                                LiveDashboardScreen(
                                    sessionId = sessionId,
                                    onBack = {
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
