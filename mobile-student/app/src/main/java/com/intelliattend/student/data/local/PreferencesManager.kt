package com.intelliattend.student.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local storage for user preferences and tokens
 * Using DataStore for secure, asynchronous storage
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.dataStore
    
    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val STUDENT_ID = stringPreferencesKey("student_id")
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val STUDENT_EMAIL = stringPreferencesKey("student_email")
        val CLASS_ID = stringPreferencesKey("class_id")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }
    
    // Save tokens
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }
    
    // Get access token
    val accessToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }
    
    // Get refresh token
    val refreshToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }
    
    // Save user profile
    suspend fun saveUserProfile(
        studentId: String,
        name: String,
        email: String,
        classId: Int
    ) {
        dataStore.edit { preferences ->
            preferences[STUDENT_ID] = studentId
            preferences[STUDENT_NAME] = name
            preferences[STUDENT_EMAIL] = email
            preferences[CLASS_ID] = classId.toString()
        }
    }
    
    // Get student ID
    val studentId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[STUDENT_ID]
    }
    
    // Save device ID
    suspend fun saveDeviceId(deviceId: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_ID] = deviceId
        }
    }
    
    // Get device ID
    val deviceId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[DEVICE_ID]
    }
    
    // Clear all data (logout)
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    // Check if user is logged in
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN] != null
    }
}
