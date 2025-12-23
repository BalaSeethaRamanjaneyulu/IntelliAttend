package com.intelliattend.faculty.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.intelliattend.faculty.utils.BiometricHelper
import com.intelliattend.faculty.utils.BiometricStatus
import com.intelliattend.faculty.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCreateScreen(
    classId: Int,
    activity: FragmentActivity,
    onSessionCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    var biometricAuthenticated by remember { mutableStateOf(false) }
    var showBiometricError by remember { mutableStateOf<String?>(null) }
    
    val biometricHelper = remember { BiometricHelper(activity) }
    
    // Start session immediately after biometric authentication
    LaunchedEffect(biometricAuthenticated) {
        if (biometricAuthenticated && sessionState == null) {
            viewModel.startSession(classId)
        }
    }
    
    // Navigate to dashboard when session created  
    LaunchedEffect(sessionState) {
        if (sessionState is Resource.Success) {
            val sessionId = (sessionState as Resource.Success).data?.sessionId
            if (sessionId != null) {
                onSessionCreated(sessionId)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Session") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!biometricAuthenticated) {
                // Show biometric prompt
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Biometric",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Biometric Authentication Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Authenticate to start attendance session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        when (biometricHelper.isBiometricAvailable()) {
                            is BiometricStatus.Available -> {
                                biometricHelper.authenticate(
                                    onSuccess = {
                                        biometricAuthenticated = true
                                        showBiometricError = null
                                    },
                                    onError = { error ->
                                        showBiometricError = error
                                    },
                                    onFailed = {
                                        showBiometricError = "Authentication failed. Please try again."
                                    }
                                )
                            }
                            is BiometricStatus.NoHardware -> {
                                showBiometricError = "No biometric hardware available"
                            }
                            is BiometricStatus.NoneEnrolled -> {
                                showBiometricError = "No biometric enrolled. Please set up fingerprint/face unlock."
                            }
                            else -> {
                                showBiometricError = "Biometric authentication unavailable"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, "Authenticate", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authenticate", style = MaterialTheme.typography.titleMedium)
                }
                
                showBiometricError?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                // Biometric authenticated, show session creation status
                when (val state = sessionState) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Creating session...",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    is Resource.Success -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Session Created!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "OTP",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.data?.otp ?: "",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Enter this OTP on SmartBoard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Redirecting to dashboard...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    is Resource.Error -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = state.message ?: "Failed to create session",
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(onClick = { viewModel.startSession(classId) }) {
                            Text("Retry")
                        }
                    }
                    
                    null -> {
                        // Initial state after authentication
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
