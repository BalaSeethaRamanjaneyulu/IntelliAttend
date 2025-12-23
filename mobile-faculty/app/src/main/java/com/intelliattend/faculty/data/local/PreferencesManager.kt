package com.intelliattend.faculty.data.local

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "faculty_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.dataStore
    
    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val FACULTY_ID = stringPreferencesKey("faculty_id")
        val FACULTY_NAME = stringPreferencesKey("faculty_name")
        val FACULTY_EMAIL = stringPreferencesKey("faculty_email")
        val DEPARTMENT = stringPreferencesKey("department")
    }
    
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }
    
    val accessToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }
    
    suspend fun saveFacultyProfile(
        facultyId: String,
        name: String,
        email: String,
        department: String
    ) {
        dataStore.edit { preferences ->
            preferences[FACULTY_ID] = facultyId
            preferences[FACULTY_NAME] = name
            preferences[FACULTY_EMAIL] = email
            preferences[DEPARTMENT] = department
        }
    }
    
    val facultyId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[FACULTY_ID]
    }
    
    val facultyName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[FACULTY_NAME]
    }
    
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN] != null
    }
}
