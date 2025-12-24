package com.intelliattend.app.student

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QRScannerScreen(
    viewModel: AttendanceViewModel = hiltViewModel(),
    onScanComplete: () -> Unit,
    sessionId: String  // Pass session ID from navigation
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    val attendanceState by viewModel.attendanceState.collectAsState()
    val listenerState by viewModel.listenerState.collectAsState()

    var lastScannedPayload by remember { mutableStateOf<String?>(null) }
    
    // Start token listener when screen opens
    LaunchedEffect(sessionId) {
        viewModel.startTokenListener(sessionId)
    }
    
    // Stop listener when screen closes
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTokenListener()
        }
    }
    
    // ML Kit Scanner Client - CONFIGURED FOR QR CODES ONLY
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)  // CRITICAL: Only scan QR codes
            .build()
        BarcodeScanning.getClient(options)
    }
    
    // Frame counter for debugging
    val frameCount = remember { AtomicLong(0) }
    
    LaunchedEffect(Unit) {
        Log.d("QRScanner", "========================================")
        Log.d("QRScanner", "QR Scanner initialized")
        Log.d("QRScanner", "ML Kit configured for QR_CODE format only")
        Log.d("QRScanner", "Session ID: $sessionId")
        Log.d("QRScanner", "========================================")
    }

    Scaffold(
        topBar = { /* Add your top bar here */ }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                        val currentFrame = frameCount.incrementAndGet()
                        
                        // Log every 30 frames to show scanner is working
                        if (currentFrame % 30L == 0L) {
                            Log.d("QRScanner", "Frame #$currentFrame - Scanner active")
                        }
                        
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty()) {
                                        Log.d("QRScanner", "🎯 DETECTED ${barcodes.size} barcode(s)!")
                                    }
                                    
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        val format = barcode.format
                                        
                                        Log.d("QRScanner", "Barcode detected:")
                                        Log.d("QRScanner", "  Format: $format")
                                        Log.d("QRScanner", "  Value: ${rawValue?.take(50)}...")
                                        Log.d("QRScanner", "  Previous: ${lastScannedPayload?.take(50) ?: "none"}")
                                        
                                        if (!rawValue.isNullOrEmpty() && rawValue != lastScannedPayload) {
                                            Log.d("QRScanner", "✅ NEW QR CODE - Submitting attendance")
                                            lastScannedPayload = rawValue
                                            viewModel.submitAttendance(rawValue)
                                        } else if (rawValue == lastScannedPayload) {
                                            Log.d("QRScanner", "⚠️ Duplicate QR - Ignoring")
                                        }
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("QRScanner", "ML Kit scan error: ${exception.message}", exception)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            Log.w("QRScanner", "Null mediaImage at frame #$currentFrame")
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

            // Overlay for states
            when {
                listenerState is ListenerState.Listening -> {
                    Text(
                        "Listening for QR updates...",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                listenerState is ListenerState.TokenReceived -> {
                    val seq = (listenerState as ListenerState.TokenReceived).sequence
                    Text(
                        "Token ready (Seq: $seq)",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                listenerState is ListenerState.Error -> {
                    val error = (listenerState as ListenerState.Error).message
                    Text(
                        "Listener Error: $error",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            when (attendanceState) {
                is AttendanceState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AttendanceState.Success -> {
                    Text("Attendance Marked Successfully!", modifier = Modifier.align(Alignment.Center))
                    LaunchedEffect(Unit) {
                        onScanComplete()
                    }
                }
                is AttendanceState.Error -> {
                    Text(
                        "Error: ${(attendanceState as AttendanceState.Error).message}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
    }
}
