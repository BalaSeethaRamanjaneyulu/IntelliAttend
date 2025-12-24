package com.intelliattend.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val departments = listOf("Computer Science", "Engineering", "Arts & Humanities", "Business")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D111B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Basic Info",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSkip) {
                    Text("Skip", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                }
            }

            // Progress Indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(width = 32.dp, height = 6.dp).background(Color(0xFF1E3B8A), CircleShape))
                    Box(modifier = Modifier.size(width = 32.dp, height = 6.dp).background(Color(0xFF1E293B), CircleShape))
                    Box(modifier = Modifier.size(width = 32.dp, height = 6.dp).background(Color(0xFF1E293B), CircleShape))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Step 1 of 3", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Set up your profile",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Let's start with the basics.",
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Avatar Section
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .background(Color(0xFF1F2937), CircleShape)
                            .border(4.dp, Color(0xFF1F2937), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E3B8A), CircleShape)
                            .border(2.dp, Color(0xFF0D111B), CircleShape)
                            .clickable { /* Handle photo upload */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Upload Photo",
                    color = Color(0xFF3B82F6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { /* Handle upload */ }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Full Name
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Full Name", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Alex Johnson", color = Color(0xFF64748B)) },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1F2937),
                                unfocusedContainerColor = Color(0xFF1F2937),
                                focusedBorderColor = Color(0xFF1E3B8A),
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Department
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Department", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = department,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                placeholder = { Text("Select Department", color = Color(0xFF64748B)) },
                                leadingIcon = { Icon(Icons.Default.School, null, tint = Color(0xFF94A3B8)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1F2937),
                                    unfocusedContainerColor = Color(0xFF1F2937),
                                    focusedBorderColor = Color(0xFF1E3B8A),
                                    unfocusedBorderColor = Color.Transparent,
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF1F2937))
                            ) {
                                departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept, color = Color.White) },
                                        onClick = {
                                            department = dept
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Role (Read Only)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current Role", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = "Student",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Color(0xFF64748B)) },
                            trailingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF94A3B8),
                                unfocusedTextColor = Color(0xFF94A3B8),
                                focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Role is determined by your university login.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Bottom Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF0D111B))
                .padding(16.dp)
                .padding(bottom = 24.dp)
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3B8A))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("Next Step", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
