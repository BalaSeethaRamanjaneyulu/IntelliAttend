package com.intelliattend.app.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intelliattend.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudentDashboardScreen(
    viewModel: StudentDashboardViewModel = hiltViewModel(),
    onScanClick: () -> Unit,
    onViewTimetableClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            HeaderSection()
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Today's Classes",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        history = (uiState as DashboardUiState.Success).history,
                        onScanClick = onScanClick,
                        onViewTimetableClick = onViewTimetableClick
                    )
                }
                is DashboardUiState.Error -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = (uiState as DashboardUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Hi, Rahul",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("👋", fontSize = 24.sp)
            }
            Text(
                "Ready for your classes?",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
        
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = PrimaryBlueLight
            ) {
                // Placeholder for profile image
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = PrimaryBlue
                )
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
            }
        }
    }
}

@Composable
fun DashboardContent(
    history: List<AttendanceRecord>,
    onScanClick: () -> Unit,
    onViewTimetableClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Highlight Card (First item in today's classes)
        HighlightClassCard()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Other classes
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                ClassCard("Physics Lab", "Dr. Verma", "11:00 AM - 1:00 PM")
            }
            item {
                ClassCard("English Literature", "Ms. Kapoor", "2:00 PM - 3:00 PM")
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                QuickActionsSection(onScanClick, onViewTimetableClick)
            }
        }
    }
}

@Composable
fun HighlightClassCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, SuccessGreen)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Starts in 12 mins",
                    style = MaterialTheme.typography.labelLarge,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Calculus 101",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    "Prof. Sharma",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    "9:00 AM - 10:00 AM",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = PrimaryBlueLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Σ", color = PrimaryBlue, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
fun ClassCard(title: String, instructor: String, time: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    instructor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = PrimaryBlueLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (title.contains("Lab")) Icons.Outlined.Science else Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(onScanClick: () -> Unit, onViewTimetableClick: () -> Unit) {
    Column {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Join Session (Scan QR)", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onViewTimetableClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueLight)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "View Timetable", 
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
