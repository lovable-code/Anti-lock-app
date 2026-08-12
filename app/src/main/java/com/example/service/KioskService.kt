package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KioskService : Service() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        private const val TAG = "KioskService"
        private const val NOTIFICATION_ID = 3004
        private const val CHANNEL_ID = "kiosk_mode_channel"
        private const val CHANNEL_NAME = "SentinelX Kiosk Guard Daemon"

        const val ACTION_START_KIOSK = "com.example.ACTION_START_KIOSK"
        const val ACTION_STOP_KIOSK = "com.example.ACTION_STOP_KIOSK"

        fun startService(context: Context) {
            val intent = Intent(context, KioskService::class.java).apply {
                action = ACTION_START_KIOSK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KioskService::class.java).apply {
                action = ACTION_STOP_KIOSK
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_KIOSK

        if (action == ACTION_STOP_KIOSK) {
            Log.i(TAG, "Stopping Kiosk Enforcement Service upon request.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        Log.i(TAG, "Starting Kiosk Mode Enforcement Service.")
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startKioskEnforcementLoop()

        return START_STICKY
    }

    private fun startKioskEnforcementLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val prefs = getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
                    val isKioskEnabled = prefs.getBoolean("kiosk_mode_enabled", false) ||
                        prefs.getBoolean("central_lock_enforced", false) ||
                        com.example.util.PolicyEnforcementManager.isPolicyLocked(applicationContext)

                    if (isKioskEnabled) {
                        // 1. Enforce App Foreground
                        if (!MainActivity.isMainActivityInForeground) {
                            Log.w(TAG, "Kiosk Violation: App exited foreground. Initiating secure relaunch...")
                            MainActivity.relaunchFromApplication(applicationContext)
                        }

                        // 2. Broadcast pinning reinforcement signal
                        val pinIntent = Intent("com.example.ACTION_ENFORCE_PINNING").apply {
                            setPackage(packageName)
                        }
                        sendBroadcast(pinIntent)
                    } else {
                        // Kiosk mode got turned off, stop service
                        Log.i(TAG, "Kiosk mode disabled in preferences. Shutting down guard daemon.")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in kiosk enforcement loop", e)
                }
                delay(1500L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Enforces non-bypassable screen pinning and launcher overriding for secure terminal kiosk policy."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            PendingIntent.getActivity(
                this,
                2002,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ SentinelX Kiosk Mode Active")
            .setContentText("Continuous lock task reinforcement & system interface lock active.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setColor(0xFF00E676.toInt()) // Emerald Neon Color
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "Kiosk mode daemon destroyed.")
    }
}
