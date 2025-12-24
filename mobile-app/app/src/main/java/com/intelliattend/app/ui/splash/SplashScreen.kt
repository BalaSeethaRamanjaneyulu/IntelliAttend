package com.intelliattend.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alphaAnim"
    )
    
    val translateYAnim = animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 20.dp,
        animationSpec = tween(durationMillis = 800, easing = EaseOutQuad),
        label = "translateYAnim"
    )

    val progressAnim = animateFloatAsState(
        targetValue = if (startAnimation) 0.4f else 0f,
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "progressAnim"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(3000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D111B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .graphicsLayer(
                    alpha = alphaAnim.value,
                    translationY = translateYAnim.value.value
                )
        ) {
            // Logo Container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        color = Color(0xFF2B46A1),
                        shape = RoundedCornerShape(40.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                ShieldIcon(modifier = Modifier.size(70.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Brand Title
            Text(
                text = "IntelliAttend",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "Smart. Secure. Seamless Attendance.",
                color = Color(0xFF94A3B8),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnim.value)
                        .background(Color(0xFF3B82F6), RoundedCornerShape(50))
                )
            }
        }

        // Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Version 1.0.0",
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "© 2023 IntelliSystem Inc.",
                color = Color(0xFF475569),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ShieldIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Shield Path
        val shieldPath = Path().apply {
            moveTo(width * 0.5f, 0f)
            lineTo(width * 0.05f, height * 0.16f)
            lineTo(width * 0.05f, height * 0.46f)
            quadraticBezierTo(
                width * 0.05f, height * 0.7f,
                width * 0.5f, height
            )
            quadraticBezierTo(
                width * 0.95f, height * 0.7f,
                width * 0.95f, height * 0.46f)
            lineTo(width * 0.95f, height * 0.16f)
            close()
        }
        drawPath(shieldPath, Color.White, style = Fill)

        // Checkmark Path
        val checkPath = Path().apply {
            moveTo(width * 0.25f, height * 0.52f)
            lineTo(width * 0.45f, height * 0.68f)
            lineTo(width * 0.75f, height * 0.35f)
        }
        // Instead of Stroke, we could use Fill if we draw it as a thick line/shape
        // For simplicity, let's draw it as a thick path
        drawPath(
            path = Path().apply {
                moveTo(width * 0.42f, height * 0.68f)
                lineTo(width * 0.25f, height * 0.52f)
                lineTo(width * 0.31f, height * 0.46f)
                lineTo(width * 0.42f, height * 0.57f)
                lineTo(width * 0.69f, height * 0.30f)
                lineTo(width * 0.75f, height * 0.36f)
                close()
            },
            color = Color(0xFF2B46A1),
            style = Fill
        )
    }
}
