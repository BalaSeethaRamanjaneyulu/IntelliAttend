package com.intelliattend.app.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intelliattend.app.ui.theme.*

data class TimetableSession(
    val title: String,
    val time: String,
    val room: String,
    val color: Color
)

@Composable
fun TimetableScreen() {
    var selectedDay by remember { mutableStateOf(15) }
    
    val days = listOf(
        DayData(15, "Mon"),
        DayData(16, "Tue"),
        DayData(17, "Wed"),
        DayData(18, "Thu"),
        DayData(19, "Fri")
    )

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Timetable",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Weekly Calendar",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(days) { day ->
                    DayItem(
                        day = day,
                        isSelected = selectedDay == day.date,
                        onClick = { selectedDay = day.date }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            TimetableList()
        }
    }
}

data class DayData(val date: Int, val dayName: String)

@Composable
fun DayItem(day: DayData, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(width = 72.dp, height = 96.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue else Color.White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                day.date.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (isSelected) Color.White else TextPrimary
            )
            Text(
                day.dayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
            )
        }
    }
}

@Composable
fun TimetableList() {
    val sessions = listOf(
        TimetableSession("Introduction to Programming", "8:00 AM - 9:00 AM", "Room 201", PrimaryBlue),
        TimetableSession("Data Structures and Algorithms", "10:00 AM - 11:00 AM", "Room 202", SecondaryBlue.copy(alpha = 0.4f)),
        TimetableSession("Database Management Systems", "1:00 PM - 2:00 PM", "Room 203", PrimaryBlue.copy(alpha = 0.2f))
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(sessions) { session ->
            TimetableSessionCard(session)
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TimetableSessionCard(session: TimetableSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored status bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(session.color)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    session.time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    session.room,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            // Placeholder for icon/image as seen in design
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E353D) // Dark blue-green from design
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Small illustrative icon
                    if (session.title.contains("Programming")) {
                       Icon(Icons.Default.Code, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    } else if (session.title.contains("Database")) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    } else {
                        Text(
                            session.title.take(2).uppercase(),
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
