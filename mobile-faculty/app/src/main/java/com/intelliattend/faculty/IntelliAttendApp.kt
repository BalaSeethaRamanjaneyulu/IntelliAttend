package com.intelliattend.faculty

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for IntelliAttend Faculty App
 * Entry point for Hilt dependency injection
 */
@HiltAndroidApp
class IntelliAttendApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: IntelliAttendApp
            private set
    }
}
