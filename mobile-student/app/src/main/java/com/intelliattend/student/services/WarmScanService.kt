package com.intelliattend.student.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.intelliattend.student.R
import com.intelliattend.student.sensors.BLEScanner
import com.intelliattend.student.sensors.GPSLocationService
import com.intelliattend.student.sensors.WiFiManagerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Warm Scan Background Service
 * Collects sensor data in background before attendance marking
 * Starts 3 minutes before class begins
 */
@AndroidEntryPoint
class WarmScanService : Service() {
    
    @Inject
    lateinit var bleScanner: BLEScanner
    
    @Inject
    lateinit var gpsLocationService: GPSLocationService
    
    @Inject
    lateinit var wifiManagerService: WiFiManagerService
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    
    private var scanJob: Job? = null
    
    companion object {
        const val CHANNEL_ID = "warm_scan_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.intelliattend.student.START_WARM_SCAN"
        const val ACTION_STOP = "com.intelliattend.student.STOP_WARM_SCAN"
        
        fun start(context: Context) {
            val intent = Intent(context, WarmScanService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, WarmScanService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startWarmScan()
            }
            ACTION_STOP -> {
                stopWarmScan()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopWarmScan()
        serviceJob.cancel()
    }
    
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun startWarmScan() {
        // Start all sensors
        bleScanner.startScanning()
        gpsLocationService.startTracking()
        
        // Periodic WiFi refresh
        scanJob = serviceScope.launch {
            while (isActive) {
                wifiManagerService.refresh()
                delay(5000) // Refresh every 5 seconds
            }
        }
    }
    
    private fun stopWarmScan() {
        scanJob?.cancel()
        bleScanner.stopScanning()
        gpsLocationService.stopTracking()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Warm Scan Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background sensor data collection"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, com.intelliattend.student.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IntelliAttend - Warm Scan Active")
            .setContentText("Collecting sensor data for attendance...")
            .setSmallIcon(R.drawable.ic_notification)  // Need to create this
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
