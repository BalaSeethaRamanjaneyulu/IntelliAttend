package com.intelliattend.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit // returns role: "student" or "faculty"
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("IntelliAttend Login", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isOtpSent
        )

        if (isOtpSent) {
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("OTP") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (!isOtpSent) {
                    isOtpSent = true
                } else {
                    // Logic to determine role (mocking for now)
                    val role = if (phoneNumber.endsWith("0")) "faculty" else "student"
                    onLoginSuccess(role)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isOtpSent) "Verify OTP" else "Send OTP")
        }
    }
}
