package com.intelliattend.app.ui.profile

import android.util.Base64
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONObject
import java.util.concurrent.Executors

data class QrDebugData(
    val rawToken: String = "",
    val prefix: String = "",
    val payload: String = "",
    val signature: String = "",
    val decodedJson: String = "",
    val sessionId: String = "",
    val classId: String = "",
    val roomId: String = "",
    val subject: String = "",
    val sequence: String = "",
    val timestamp: String = "",
    val detectionTime: String = ""  // Local device time when QR was detected
)

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QrDebugScannerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var debugData by remember { mutableStateOf(QrDebugData()) }
    var lastScannedToken by remember { mutableStateOf<String?>(null) }
    
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            scanner.close()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        ) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                if (barcodes.isNotEmpty()) {
                                    val barcode = barcodes[0]
                                    val rawValue = barcode.rawValue
                                    
                                    if (!rawValue.isNullOrEmpty() && rawValue != lastScannedToken) {
                                        lastScannedToken = rawValue
                                        // Capture exact detection time
                                        val detectionTime = java.text.SimpleDateFormat(
                                            "HH:mm:ss.SSS", 
                                            java.util.Locale.getDefault()
                                        ).format(java.util.Date())
                                        debugData = parseQrDebugData(rawValue, detectionTime)
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                exception.printStackTrace()
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        "QR Code Debugger",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Debug Info Panel
            if (debugData.rawToken.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Color(0xFF1E2330),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .heightIn(max = 400.dp)
                    ) {
                        Text(
                            "🔍 QR Code Detected",
                            color = Color(0xFF10B981),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Detection Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "⏱️ Detected at:",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                debugData.detectionTime,
                                color = Color(0xFFFBBF24),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Encrypted Token
                        DebugSection(
                            title = "🔐 Encrypted Token",
                            content = debugData.rawToken,
                            mono = true
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Token Parts
                        DebugSection(
                            title = "📦 Token Structure",
                            content = "Prefix: ${debugData.prefix}\nPayload (B64): ${debugData.payload.take(40)}...\nSignature: ${debugData.signature}"
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Decrypted Data
                        DebugSection(
                            title = "🔓 Decrypted Data",
                            content = buildString {
                                appendLine("Session ID: ${debugData.sessionId}")
                                appendLine("Subject: ${debugData.subject}")
                                appendLine("Room ID: ${debugData.roomId}")
                                appendLine("Class ID: ${debugData.classId}")
                                appendLine("Sequence: ${debugData.sequence}")
                                appendLine("Timestamp: ${debugData.timestamp}")
                            },
                            valueColor = Color(0xFF3B82F6)
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Box(
                        modifier = Modifier.padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Point camera at QR code to debug",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebugSection(
    title: String,
    content: String,
    mono: Boolean = false,
    valueColor: Color = Color.White
) {
    Column {
        Text(
            title,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF111521)
        ) {
            Text(
                content,
                color = valueColor,
                fontSize = 11.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                modifier = Modifier.padding(12.dp),
                lineHeight = 16.sp
            )
        }
    }
}

fun parseQrDebugData(token: String, detectionTime: String): QrDebugData {
    return try {
        val parts = token.split("_")
        if (parts.size != 3) {
            return QrDebugData(rawToken = token, detectionTime = detectionTime)
        }
        
        val prefix = parts[0]
        val payloadB64 = parts[1]
        val signature = parts[2]
        
        // Add padding for Base64 decoding
        val paddedB64 = payloadB64 + "=".repeat((4 - payloadB64.length % 4) % 4)
        val jsonStr = String(Base64.decode(paddedB64, Base64.URL_SAFE or Base64.NO_WRAP))
        val json = JSONObject(jsonStr)
        
        QrDebugData(
            rawToken = token,
            prefix = prefix,
            payload = payloadB64,
            signature = signature,
            decodedJson = jsonStr,
            sessionId = json.optString("sid", "N/A"),
            classId = json.optString("cid", "N/A"),
            roomId = json.optString("rid", "N/A"),
            subject = json.optString("sub", "N/A"),
            sequence = json.optInt("seq", 0).toString(),
            timestamp = json.optLong("ts", 0).toString(),
            detectionTime = detectionTime
        )
    } catch (e: Exception) {
        QrDebugData(rawToken = token, detectionTime = detectionTime)
    }
}
