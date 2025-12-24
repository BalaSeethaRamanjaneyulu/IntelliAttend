package com.intelliattend.app.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OTPScreen(
    viewModel: SessionViewModel = hiltViewModel(),
    sessionId: String
) {
    val otpValue by viewModel.otpValue.collectAsState()
    val timerValue by viewModel.timerValue.collectAsState()
    val attendeeCount by viewModel.attendeeCount.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.startSession(sessionId)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "$attendeeCount Students Scanned",
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = "Session: $sessionId",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Enter this OTP on the SmartBoard",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = otpValue.chunked(3).joinToString(" "),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp
                    ),
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Refreshing in ${timerValue}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = timerValue / 60f,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}
