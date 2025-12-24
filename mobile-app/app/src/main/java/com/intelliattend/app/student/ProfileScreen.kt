package com.intelliattend.app.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intelliattend.app.ui.theme.*

@Composable
fun ProfileScreen(
    onLogoutClick: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Profile & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Profile Image
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = PrimaryBlueLight
            ) {
                // Placeholder for profile image
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(32.dp),
                    tint = PrimaryBlue
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Ethan Carter",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Text(
                "Roll No: 2021CS001",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Text(
                "Computer Science, 3rd Year",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Device Info Section
            SectionHeader("Device Info")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard {
                SettingsItem("Registered Device ID", "1234567890")
                Divider(color = BorderLight, thickness = 1.dp)
                SettingsItem("Last Sync Time", "2024-01-15 10:00 AM")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Preferences Section
            SectionHeader("Preferences")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard {
                SettingsToggleItem("Notifications", true) {}
                Divider(color = BorderLight, thickness = 1.dp)
                SettingsToggleItem("Dark Mode", false) {}
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Logout Button
            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LogoutRed,
                    contentColor = LogoutRedText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Logout", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryBlue
        )
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
fun SettingsToggleItem(label: String, isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = PrimaryBlueLight,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
