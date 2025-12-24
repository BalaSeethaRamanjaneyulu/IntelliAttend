package com.intelliattend.app.ui.scanner

import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onResult: (ScanResult) -> Unit,
    viewModel: QrScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    viewModel.bindCamera(lifecycleOwner, this) { result ->
                        onResult(result)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        }

        // Error Message
        uiState.errorMessage?.let { error ->
            val context = LocalContext.current
            LaunchedEffect(error) {
                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                viewModel.startScanning() // Reset scanning after error
            }
        }

        // Dimmed Overlay with Cutout
        ScannerOverlay(modifier = Modifier.fillMaxSize())

        // UI Controls
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.onTorchToggled() },
                        modifier = Modifier.background(
                            if (uiState.isTorchOn) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = { /* Handle more info */ },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Instructions Text
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Align QR code within frame",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Scanner Frame area is in the weight(1f) spacer area

            Spacer(modifier = Modifier.weight(1f))

            // Zoom Control
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1.0x", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    Text("ZOOM", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("5.0x", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                }
                Slider(
                    value = uiState.zoomRatio,
                    onValueChange = { viewModel.onZoomChanged(it) },
                    valueRange = 1f..5f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4285F4),
                        activeTrackColor = Color(0xFF4285F4),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            // Bottom Gallery Button
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { /* Open Gallery */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload from gallery", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Bottom Sheet Info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.Gray.copy(alpha = 0.5f), CircleShape))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scan QR code for attendance",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Point camera at the classroom or event QR code",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("INTELLIATTEND", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.size(3.dp).background(Color.Gray, CircleShape))
                        Text("CLASSROOM", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.size(3.dp).background(Color.Gray, CircleShape))
                        Text("FACULTY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Scanning Laser Animation
        ScanningLaser(modifier = Modifier.align(Alignment.Center))
        
        // Corner Borders
        ScannerFrame(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        val rectSize = Size(256.dp.toPx(), 256.dp.toPx())
        val rectOffset = Offset(
            (size.width - rectSize.width) / 2,
            (size.height - rectSize.height) / 2
        )
        val rect = RoundRect(
            rectOffset.x,
            rectOffset.y,
            rectOffset.x + rectSize.width,
            rectOffset.y + rectSize.height,
            cornerRadius
        )
        
        val clipPath = Path().apply {
            addRoundRect(rect)
        }
        
        clipPath(clipPath, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ScannerFrame(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(256.dp)) {
        // Corner Borders
        val colorTl = Color(0xFFEA4335)
        val colorTr = Color(0xFFFBBC04)
        val colorBl = Color(0xFF4285F4)
        val colorBr = Color(0xFF34A853)
        val strokeWidth = 5.dp
        val cornerSize = 40.dp
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top Left
            drawPath(
                path = Path().apply {
                    moveTo(0f, cornerSize.toPx())
                    lineTo(0f, 12.dp.toPx()) // Start of curve
                    arcTo(Rect(0f, 0f, 24.dp.toPx(), 24.dp.toPx()), 180f, 90f, false)
                    lineTo(cornerSize.toPx(), 0f)
                },
                color = colorTl,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            
            // Top Right
            drawPath(
                path = Path().apply {
                    moveTo(size.width - cornerSize.toPx(), 0f)
                    lineTo(size.width - 12.dp.toPx(), 0f)
                    arcTo(Rect(size.width - 24.dp.toPx(), 0f, size.width, 24.dp.toPx()), 270f, 90f, false)
                    lineTo(size.width, cornerSize.toPx())
                },
                color = colorTr,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            
            // Bottom Left
            drawPath(
                path = Path().apply {
                    moveTo(0f, size.height - cornerSize.toPx())
                    lineTo(0f, size.height - 12.dp.toPx())
                    arcTo(Rect(0f, size.height - 24.dp.toPx(), 24.dp.toPx(), size.height), 90f, 90f, false)
                    lineTo(cornerSize.toPx(), size.height)
                },
                color = colorBl,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            
            // Bottom Right
            drawPath(
                path = Path().apply {
                    moveTo(size.width, size.height - cornerSize.toPx())
                    lineTo(size.width, size.height - 12.dp.toPx())
                    arcTo(Rect(size.width - 24.dp.toPx(), size.height - 24.dp.toPx(), size.width, size.height), 0f, 90f, false)
                    lineTo(size.width - cornerSize.toPx(), size.height)
                },
                color = colorBr,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        
        // Thin white border
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(2.dp)
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
        )
    }
}

@Composable
fun ScanningLaser(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 256.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "position"
    )

    Box(modifier = modifier.size(256.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).offset(y = laserPosition.dp)) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF4285F4).copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
        }
    }
}
