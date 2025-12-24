package com.intelliattend.app.ui.faculty

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiveMonitorScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val students = remember {
        listOf(
            StudentState("Jane D.", Status.VERIFIED),
            StudentState("Mark R.", Status.VERIFIED),
            StudentState("Sarah L.", Status.VERIFIED, isNew = true),
            StudentState("John D.", Status.UNVERIFIED, initials = "JD"),
            StudentState("Mike T.", Status.FLAGGED),
            StudentState("Emma W.", Status.VERIFIED),
            StudentState("David B.", Status.VERIFIED),
            StudentState("Alex K.", Status.UNVERIFIED, initials = "AK"),
            StudentState("Sophia P.", Status.VERIFIED),
            StudentState("James L.", Status.VERIFIED),
            StudentState("Lucas M.", Status.VERIFIED),
            StudentState("Chloe R.", Status.UNVERIFIED, initials = "CR")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D111B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            HeaderSection(onBack, onSettings)

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                KPISection()
                Spacer(modifier = Modifier.height(24.dp))
                GridSection(students)
            }

            // Bottom Actions
            BottomActionsSection()
        }
    }
}

@Composable
fun HeaderSection(onBack: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CS101 - Intro to AI", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BlinkingDot()
                Text("Live Monitor", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, null, tint = Color.White)
        }
    }
}

@Composable
fun BlinkingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkingDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(Color(0xFFEF4444).copy(alpha = alpha), CircleShape)
            .border(1.dp, Color(0xFFEF4444), CircleShape)
    )
}

@Composable
fun KPISection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Attendance Rate", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("80%", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Text("+12%", color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "24/30", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Checked In", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
            LinearProgressIndicator(
                progress = 0.8f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Color(0xFF1E3B8A),
                trackColor = Color(0xFF334155)
            )
        }
    }
}

@Composable
fun GridSection(students: List<StudentState>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Seating Grid", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = { /* Filter */ }) {
            Icon(Icons.Default.FilterList, null, tint = Color(0xFF1E3B8A), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Filter", color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(students) { student ->
            StudentItem(student)
        }
    }
}

@Composable
fun StudentItem(student: StudentState) {
    val infiniteTransition = rememberInfiniteTransition(label = "NewJoinPulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Pulse for new joins
            if (student.isNew) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(pulseSize)
                        .border(2.dp, Color(0xFF10B981).copy(alpha = pulseAlpha), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(
                        width = 2.dp,
                        color = when (student.status) {
                            Status.VERIFIED -> Color(0xFF10B981)
                            Status.FLAGGED -> Color(0xFFEF4444)
                            Status.UNVERIFIED -> Color(0xFF475569)
                        },
                        shape = CircleShape
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2330)),
                contentAlignment = Alignment.Center
            ) {
                if (student.initials != null) {
                    Text(student.initials, color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF64748B), modifier = Modifier.size(32.dp))
                }
            }

            // Status Badge
            if (student.status != Status.UNVERIFIED) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (student.status == Status.VERIFIED) Color(0xFF10B981) else Color(0xFFEF4444),
                            shape = CircleShape
                        )
                        .border(1.dp, Color(0xFF1E2330), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (student.status == Status.VERIFIED) Icons.Default.Check else Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Text(
            text = student.name,
            color = when (student.status) {
                Status.VERIFIED -> Color(0xFFD1D5DB)
                Status.FLAGGED -> Color(0xFFEF4444)
                Status.UNVERIFIED -> Color(0xFF64748B)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BottomActionsSection() {
    Surface(
        color = Color(0xFF1E2330),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* Lock */ },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lock", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { /* End */ },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3B8A))
            ) {
                Icon(Icons.Default.StopCircle, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("End Session", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class StudentState(
    val name: String,
    val status: Status,
    val initials: String? = null,
    val isNew: Boolean = false
)

enum class Status {
    VERIFIED, UNVERIFIED, FLAGGED
}
