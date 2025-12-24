package com.intelliattend.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intelliattend.app.ui.splash.ShieldIcon

@Composable
fun AuthHubScreen(
    onLoginAsStudent: () -> Unit,
    onLoginAsFaculty: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D111B))
    ) {
        // Background glows
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-150).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E3B8A).copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2563EB).copy(alpha = 0.1f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo & Header
            Column(
                modifier = Modifier.padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ShieldIcon(modifier = Modifier.size(50.dp))
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "IntelliAttend",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Beyond Attendance, Into Insights",
                        color = Color(0xFF94A3B8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuthHubButton(
                    text = "Login as Student",
                    subtitle = "Scan QR & Track Progress",
                    onClick = onLoginAsStudent,
                    primary = true
                )
                
                AuthHubButton(
                    text = "Login as Faculty",
                    subtitle = "Manage Classes & Insights",
                    onClick = onLoginAsFaculty,
                    primary = false
                )
            }

            // Footer Links
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Privacy Policy", color = Color(0xFF64748B), fontSize = 12.sp)
                Text("Terms of Service", color = Color(0xFF64748B), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AuthHubButton(
    text: String,
    subtitle: String,
    onClick: () -> Unit,
    primary: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Color(0xFF1E3B8A) else Color(0xFF1E293B)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = if (primary) Color(0xFFBFDBFE) else Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (primary) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }
    }
}
