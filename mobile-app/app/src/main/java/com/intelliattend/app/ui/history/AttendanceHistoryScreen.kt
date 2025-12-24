package com.intelliattend.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    onBack: () -> Unit,
    onItemClick: (HistoryItem) -> Unit
) {
    val historyItems = remember {
        listOf(
            HistoryItem("CS101", "Intro to Comp Sci", "Oct 24", "10:00 AM", 92, "Present"),
            HistoryItem("MA202", "Calculus II", "Oct 22", "02:00 PM", 45, "Critical", isCritical = true),
            HistoryItem("EN202", "Adv. Literature", "Oct 21", "09:00 AM", 88, "Present"),
            HistoryItem("PH101", "Physics Lab I", "Oct 19", "01:30 PM", 76, "Average"),
            HistoryItem("CS101", "Intro to Comp Sci", "Oct 17", "10:00 AM", 95, "Present")
        )
    }

    Scaffold(
        containerColor = Color(0xFF0D111B),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add Record */ },
                containerColor = Color(0xFF1E3B8A),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    "Attendance History",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Stats Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Classes",
                    value = "24",
                    icon = Icons.Default.School,
                    iconColor = Color(0xFF1E3B8A),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Avg. Rate",
                    value = "82%",
                    icon = Icons.Default.TrendingUp,
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Filters
            HistoryFilters()

            Spacer(modifier = Modifier.height(16.dp))

            // Low Attendance Toggle
            var lowOnly by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF1E2330), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Low Attendance Only", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Show classes below 75%", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = lowOnly,
                    onCheckedChange = { lowOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1E3B8A),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "RECENT HISTORY",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // History List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(if (lowOnly) historyItems.filter { it.percentage < 75 } else historyItems) { item ->
                    HistoryItemView(item, onClick = { onItemClick(item) })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, iconColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                Text(title.uppercase(), color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilters() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("This Month") },
                trailingIcon = { Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1E3B8A),
                    selectedLabelColor = Color.White,
                    selectedTrailingIconColor = Color.White
                ),
                border = null
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("All Subjects") },
                trailingIcon = { Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF1E2330),
                    labelColor = Color(0xFFE2E8F0)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color(0xFF334155),
                    borderWidth = 1.dp
                )
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Date Range") },
                trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF1E2330),
                    labelColor = Color(0xFFE2E8F0)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color(0xFF334155),
                    borderWidth = 1.dp
                )
            )
        }
    }
}

@Composable
fun HistoryItemView(item: HistoryItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E2330))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        if (item.isCritical) {
            Box(modifier = Modifier.width(4.dp).height(80.dp).align(Alignment.CenterStart).background(Color(0xFFEF4444)))
        }

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (item.isCritical) Color(0xFF1E293B) else Color(0xFF1E3B8A).copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        item.courseCode,
                        color = if (item.isCritical) Color(0xFF94A3B8) else Color(0xFF3B82F6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(item.courseName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Event, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        Text("${item.date} • ${item.time}", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${item.percentage}%",
                        color = if (item.isCritical) Color(0xFFEF4444) else Color(0xFF10B981),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(item.status.uppercase(), color = if (item.isCritical) Color(0xFFEF4444).copy(alpha = 0.7f) else Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF334155))
            }
        }
    }
}

data class HistoryItem(
    val courseCode: String,
    val courseName: String,
    val date: String,
    val time: String,
    val percentage: Int,
    val status: String,
    val isCritical: Boolean = false
)
