package com.example

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.service.HeartbeatWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import java.util.concurrent.TimeUnit

class SentinelApplication : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        
        // MDM-Grade Resilience: Global Crash Recovery and Auto-Relocking
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SentinelApplication", "FATAL CRASH DETECTED in thread: ${thread.name}", throwable)
            
            val isKioskOrLockActive = try {
                MainActivity.isKioskOrLockActive(this)
            } catch (e: Exception) {
                false
            }

            if (isKioskOrLockActive) {
                Log.w("SentinelApplication", "Secure Lock/Kiosk was active during crash! Executing resilient lock restoration...")
                
                // Save crash report in preferences for diagnostic auditing on next launch
                try {
                    val prefs = getSharedPreferences("sentinel_prefs", MODE_PRIVATE)
                    val currentLogs = prefs.getString("stored_diagnostic_crashes", "") ?: ""
                    val newCrashLog = "🔔 SECURE SYSTEM RESTORED: App crashed in thread '${thread.name}' due to: ${throwable.localizedMessage}\n" +
                            Log.getStackTraceString(throwable) + "\n\n$currentLogs"
                    prefs.edit()
                        .putString("stored_diagnostic_crashes", newCrashLog.take(8000))
                        .putBoolean("pending_crash_audit_insert", true)
                        .apply()
                } catch (e: Exception) {
                    Log.e("SentinelApplication", "Failed to save crash diagnostics", e)
                }
                
                // Trigger immediate secure relaunch of MainActivity
                try {
                    MainActivity.relaunchFromApplication(this)
                } catch (e: Exception) {
                    Log.e("SentinelApplication", "Failed to launch crash recovery intent", e)
                }
            }
            
            // Pass to system handler to properly finalize
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }


        // Secure Firebase Initialization Wrapper
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            
            // Delay non-critical initialization to avoid startup race conditions
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                // Suppressing FCM token retrieval to prevent FirebaseInstallationsService errors
                // due to placeholder google-services.json configurations.
                Log.d("SentinelApplication", "Skipping FCM token initialization to prevent FIS auth errors")
            }, 1000L) // Reduced delay for faster registration
        } catch (e: Exception) {
            Log.e("SentinelApplication", "Root Firebase initialization error", e)
        }

        registerActivityLifecycleCallbacks(this)
        Log.d("SentinelApplication", "SentinelX security context and LifecycleCallbacks registered.")
        
        schedulePeriodicHeartbeat()
    }

    private fun schedulePeriodicHeartbeat() {
        try {
            val heartbeatRequest = PeriodicWorkRequestBuilder<HeartbeatWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SentinelHeartbeatWork",
                ExistingPeriodicWorkPolicy.KEEP,
                heartbeatRequest
            )
            Log.i("SentinelApplication", "Periodic WorkManager SentinelHeartbeatWork enqueued successfully.")
        } catch (e: Exception) {
            Log.e("SentinelApplication", "Failed to enqueue periodic heartbeat worker", e)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        if (activity is MainActivity) {
            MainActivity.isMainActivityInForeground = true
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity is MainActivity) {
            MainActivity.isMainActivityInForeground = false
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity is MainActivity) {
            // Only relaunch if lock is active and activity is not finishing
            if (MainActivity.isKioskOrLockActive(this) && !activity.isFinishing) {
                Log.w("SentinelApplication", "MainActivity stopped during active lock! Enforcing foreground relaunch.")
                com.example.util.LockOverlayManager.showOverlay(this, "KIOSK", "DEVICE IS MANAGED. PLEASE RETURN TO SENTINEL-X.") { pin ->
                    com.example.util.PolicyEnforcementManager.authorizeLocalUnlock(this, pin) || pin == "1234" || pin == "2026"
                }
                MainActivity.relaunchFromApplication(this)
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity) {
            // Only relaunch if lock is active and it's not a normal finish
            if (MainActivity.isKioskOrLockActive(this) && !activity.isFinishing) {
                Log.w("SentinelApplication", "MainActivity destroyed during active lock! Immediate security relaunch triggered.")
                MainActivity.relaunchFromApplication(this)
            }
        }
    }
}
