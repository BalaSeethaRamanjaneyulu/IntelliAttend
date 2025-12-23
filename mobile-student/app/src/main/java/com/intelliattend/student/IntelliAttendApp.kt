package com.intelliattend.student

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for IntelliAttend Student App
 * Entry point for Hilt dependency injection
 */
@HiltAndroidApp
class IntelliAttendApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize app-level components
        instance = this
    }
    
    companion object {
        lateinit var instance: IntelliAttendApp
            private set
    }
}
