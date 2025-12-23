package com.intelliattend.student.utils

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Device ID Generator
 * Creates unique device identifier for attendance tracking
 */
object DeviceIdGenerator {
    
    private const val PREF_KEY_DEVICE_ID = "device_id_generated"
    
    /**
     * Get or generate device ID
     * Uses Android ID first, falls back to UUID
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        
        // Check if we already generated one
        val saved = prefs.getString(PREF_KEY_DEVICE_ID, null)
        if (saved != null) {
            return saved
        }
        
        // Try to get Android ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        val deviceId = if (androidId != null && androidId.isNotBlank() && androidId != "9774d56d682e549c") {
            // Use Android ID if valid (9774d56d682e549c is emulator default)
            "ANDROID_$androidId"
        } else {
            // Generate UUID as fallback
            "UUID_${UUID.randomUUID()}"
        }
        
        // Save for future use
        prefs.edit().putString(PREF_KEY_DEVICE_ID, deviceId).apply()
        
        return deviceId
    }
}
