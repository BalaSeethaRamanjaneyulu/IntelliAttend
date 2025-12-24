package com.intelliattend.app.student

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun QRScannerScreen(
    viewModel: AttendanceViewModel = hiltViewModel(),
    onScanComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val qrReader = remember { MultiFormatReader() }
    
    val attendanceState by viewModel.attendanceState.collectAsState()

    var lastScannedPayload by remember { mutableStateOf<String?>(null) }

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
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val buffer = imageProxy.planes[0].buffer
                        val data = ByteArray(buffer.remaining())
                        buffer.get(data)
                        val source = PlanarYUVLuminanceSource(
                            data, imageProxy.width, imageProxy.height, 0, 0, imageProxy.width, imageProxy.height, false
                        )
                        val bitmap = BinaryBitmap(HybridBinarizer(source))

                        try {
                            val result = qrReader.decode(bitmap)
                            val payload = result.text
                            if (payload != lastScannedPayload) {
                                lastScannedPayload = payload
                                viewModel.submitAttendance(payload)
                            }
                        } catch (e: Exception) {
                            // No QR found in this frame
                        } finally {
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
                    Text("Error: ${(attendanceState as AttendanceState.Error).message}", modifier = Modifier.align(Alignment.Center))
                }
                else -> {}
            }
        }
    }
}
