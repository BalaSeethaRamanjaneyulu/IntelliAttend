package com.intelliattend.app.faculty

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intelliattend.app.qr.QRUtils

@Composable
fun QRGeneratorScreen(
    viewModel: SessionViewModel = hiltViewModel(),
    sessionId: String
) {
    val qrContent by viewModel.qrContent.collectAsState()
    val timerValue by viewModel.timerValue.collectAsState()

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
            Text(
                text = "Session: $sessionId",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (qrContent.isNotEmpty()) {
                val bitmap = remember(qrContent) {
                    QRUtils.generateQRCode(qrContent)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Dynamic QR Code",
                        modifier = Modifier.size(300.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Refreshing in ${timerValue}s",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = timerValue / 30f,
                modifier = Modifier.width(300.dp)
            )
        }
    }
}
