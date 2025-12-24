package com.intelliattend.app.ui.scanner

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Base64
import com.intelliattend.app.network.ApiService
import com.intelliattend.app.network.ScanQRRequest
import com.intelliattend.app.auth.AuthRepository
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.pow

data class ScanResult(
    val sessionId: String,
    val subject: String,
    val room: String,
    val timestamp: Long
)

data class ScannerUiState(
    val isTorchOn: Boolean = false,
    val zoomRatio: Float = 1f,
    val lastScannedCode: String? = null,
    val isScanning: Boolean = true,
    val isDetected: Boolean = false,
    val scanResult: ScanResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    fun onTorchToggled() {
        val newState = !_uiState.value.isTorchOn
        cameraControl?.enableTorch(newState)
        _uiState.value = _uiState.value.copy(isTorchOn = newState)
    }

    fun onZoomChanged(ratio: Float) {
        cameraControl?.setZoomRatio(ratio)
        _uiState.value = _uiState.value.copy(zoomRatio = ratio)
    }

    fun stopScanning() {
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun startScanning() {
        _uiState.value = _uiState.value.copy(
            isScanning = true, 
            lastScannedCode = null, 
            isDetected = false,
            scanResult = null,
            isLoading = false,
            errorMessage = null
        )
    }

    private fun parseQrToken(token: String): ScanResult? {
        try {
            android.util.Log.d("QrScanner", "Parsing token: $token")
            // Format: <PREFIX>_<base64_payload>_<signature>
            val parts = token.split("_")
            if (parts.size != 3) {
                android.util.Log.w("QrScanner", "❌ Invalid format: Expected 3 parts, got ${parts.size}")
                return null
            }
            
            val prefix = parts[0]
            if (prefix != "QR" && prefix != "IATT") {
                android.util.Log.w("QrScanner", "❌ Invalid prefix: Got '$prefix', expected 'QR' or 'IATT'")
                return null
            }

            val payloadB64 = parts[1]
            // Add necessary padding for Base64 decoding
            val paddedB64 = payloadB64 + "=".repeat((4 - payloadB64.length % 4) % 4)
            
            android.util.Log.d("QrScanner", "Decoding payload: $paddedB64")
            val jsonStr = String(Base64.decode(paddedB64, Base64.URL_SAFE or Base64.NO_WRAP))
            android.util.Log.d("QrScanner", "Decoded JSON: $jsonStr")
            val json = JSONObject(jsonStr)

            return ScanResult(
                sessionId = json.optString("sid", "Unknown"),
                subject = json.optString("sub", "Unknown Subject"),
                room = json.optString("rid", "Unknown Room"),
                timestamp = json.optLong("ts", 0L)
            )
        } catch (e: Exception) {
            android.util.Log.e("QrScanner", "❌ Parse Error: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    private fun markAttendance(token: String, result: ScanResult, onCodeScanned: (ScanResult) -> Unit) {
        val studentId = authRepository.getCurrentUser()?.uid ?: "UnknownUser"
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val response = apiService.scanQr(
                    ScanQRRequest(
                        session_id = result.sessionId,
                        student_id = studentId,
                        qr_token = token
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isDetected = true,
                        scanResult = result
                    )
                    onCodeScanned(result)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.body()?.message ?: "Verification failed",
                        isScanning = true,
                        lastScannedCode = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Network error",
                    isScanning = true,
                    lastScannedCode = null
                )
            }
        }
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onCodeScanned: (ScanResult) -> Unit
    ) {
        val context = previewView.context
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
                processImageProxy(imageProxy, onCodeScanned)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        imageProxy: ImageProxy,
        onCodeScanned: (ScanResult) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && _uiState.value.isScanning && !_uiState.value.isLoading) {
            android.util.Log.v("QrScanner", "Processing frame...")
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        android.util.Log.d("QrScanner", "Detected ${barcodes.size} barcodes")
                        val barcode = barcodes[0]
                        val rawValue = barcode.rawValue
                        
                        // Auto-zoom logic
                        barcode.boundingBox?.let { box ->
                            val imageWidth = image.width.toFloat()
                            val imageHeight = image.height.toFloat()
                            val boxArea = (box.width() * box.height()).toFloat()
                            val imageArea = imageWidth * imageHeight
                            val ratio = boxArea / imageArea
                            
                            if (ratio < 0.01f && _uiState.value.zoomRatio < 3f) {
                                onZoomChanged(_uiState.value.zoomRatio + 0.5f)
                            }
                        }

                        if (rawValue != null && rawValue != _uiState.value.lastScannedCode) {
                            android.util.Log.d("QrScanner", "🎯 Barcode Raw Value: $rawValue")
                            val scanResult = parseQrToken(rawValue)
                            if (scanResult != null) {
                                android.util.Log.d("QrScanner", "✅ Valid QR context: ${scanResult.subject} at ${scanResult.room}")
                                _uiState.value = _uiState.value.copy(
                                    lastScannedCode = rawValue,
                                    isScanning = false
                                )
                                markAttendance(rawValue, scanResult, onCodeScanned)
                            } else {
                                android.util.Log.w("QrScanner", "⚠️ Scanned code failed validation/parsing")
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = "Invalid QR code format",
                                    lastScannedCode = rawValue // Set this to prevent spamming the same invalid code
                                )
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        scanner.close()
    }
}
