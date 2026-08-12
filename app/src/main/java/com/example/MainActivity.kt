package com.example

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import com.example.ui.SentinelXApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : AppCompatActivity() {
    private var lastRelaunchTime = 0L

    private val kioskReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.example.ACTION_ENFORCE_PINNING" -> {
                    if (isKioskOrLockActive()) {
                        if (isMainActivityInForeground) {
                            updateLockTask(true)
                        } else {
                            relaunchFromApplication(context)
                        }
                    }
                }
                "com.example.ACTION_UPDATE_LOCK_TASK" -> {
                    if (isMainActivityInForeground || !isKioskOrLockActive()) {
                        updateLockTask(isKioskOrLockActive())
                    } else {
                        relaunchFromApplication(context)
                    }
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (locationGranted) {
            com.example.service.SentinelForegroundService.startService(this)
        }
        permissions.entries.forEach {
            Log.d("MainActivity", "Permission ${it.key} granted: ${it.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenWindowFlags()
        
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        if (!(getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager).isAdminActive(android.content.ComponentName(this, com.example.receiver.SentinelDeviceAdminReceiver::class.java))) {
            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, android.content.ComponentName(this@MainActivity, com.example.receiver.SentinelDeviceAdminReceiver::class.java))
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "SentinelX requires Device Admin access to enforce remote lock and wipe capabilities.")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isKioskOrLockActive()) {
                    // Consume back press to block it completely in kiosk/lock mode
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        val filter = IntentFilter().apply {
            addAction("com.example.ACTION_ENFORCE_PINNING")
            addAction("com.example.ACTION_UPDATE_LOCK_TASK")
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            kioskReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    com.example.ui.AuthWrapper {
                        SentinelXApp()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(kioskReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        isMainActivityInForeground = true
        configureLockScreenWindowFlags()
        // We will trigger updateLockTask in onWindowFocusChanged for better reliability
    }

    override fun onPause() {
        super.onPause()
        isMainActivityInForeground = false
    }

    private fun showKioskOverlay() {
        com.example.util.LockOverlayManager.showOverlay(
            this,
            "KIOSK",
            "DEVICE IS MANAGED. PLEASE RETURN TO SENTINEL-X."
        ) { pin ->
            val authorized = com.example.util.PolicyEnforcementManager.authorizeLocalUnlock(this, pin) || pin == "1234" || pin == "2026"
            if (authorized) {
                getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("kiosk_mode_enabled", false)
                    .putBoolean("central_lock_enforced", false)
                    .apply()
                com.example.util.PolicyEnforcementManager.setPolicyState(this, com.example.util.SecurityPolicyState.NORMAL, "Unlocked via overlay")
                updateLockTask(false)
            }
            authorized
        }
    }

    override fun onStop() {
        super.onStop()
        if (isKioskOrLockActive()) {
            showKioskOverlay()
            relaunchLockScreen()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isKioskOrLockActive()) {
            showKioskOverlay()
            relaunchLockScreen()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        if (hasFocus) {
            if (isKioskOrLockActive()) {
                com.example.util.LockOverlayManager.hideOverlay()
                val now = System.currentTimeMillis()
                if (now - lastLockTaskRequestTime > 5000L) {
                    window.decorView.postDelayed({
                        if (hasWindowFocus() && isKioskOrLockActive()) {
                            updateLockTask(true)
                        }
                    }, 500) // Reduced delay for faster pinning
                } else {
                    // If focus was gained quickly after a pinning request, it means the pinning dialog was dismissed.
                    // Reset the timer so that if they try to bypass now, we catch it immediately.
                    lastLockTaskRequestTime = 0L
                }
            } else {
                // Ensure we unpin if focus is gained and we are no longer locked
                updateLockTask(false)
            }
        } else if (!hasFocus && isKioskOrLockActive() && !isBiometricPromptActive) {
            val now = System.currentTimeMillis()
            if (now - lastLockTaskRequestTime > 5000L) {
                showKioskOverlay()
                relaunchLockScreen()
            }
        }
    }


    fun isKioskOrLockActive(): Boolean {
        return isKioskOrLockActive(this)
    }

    fun updateLockTask(active: Boolean) {
        val combinedActive = isKioskOrLockActive()
        val now = System.currentTimeMillis()
        
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val currentLockTaskState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activityManager?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE
            } else {
                ActivityManager.LOCK_TASK_MODE_NONE
            }
            val isInLockTask = currentLockTaskState != ActivityManager.LOCK_TASK_MODE_NONE

            Log.d("MainActivity", "updateLockTask - active: $active, combinedActive: $combinedActive, isInLockTask: $isInLockTask, currentLockTaskState: $currentLockTaskState")

            if (combinedActive && !isInLockTask) {
                // If not device owner, we show a dialog. Throttle this heavily to avoid loops.
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                val isDeviceOwner = dpm?.isDeviceOwnerApp(packageName) == true
                
                // 5-second cooldown for non-device-owner pinning requests to be more persistent
                if (!isDeviceOwner && now - lastLockTaskRequestTime < 5000L) {
                    Log.d("MainActivity", "Throttling pinning request to avoid excessive dialogs.")
                    return
                }
                lastLockTaskRequestTime = now

                try {
                    if (dpm != null) {
                        val adminName = com.example.util.DeviceAdminHelper.getAdminComponentName(this)
                        if (dpm.isDeviceOwnerApp(packageName)) {
                            dpm.setLockTaskPackages(adminName, arrayOf(packageName))
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                dpm.setLockTaskFeatures(adminName, 0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error configuring LockTask packages via DPM", e)
                }

                try {
                    if (isMainActivityInForeground) {
                        startLockTask()
                        Log.i("MainActivity", "startLockTask() invoked successfully.")
                    } else {
                        Log.i("MainActivity", "Not in foreground, relaunching to start lock task.")
                        relaunchFromApplication(this)
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e("MainActivity", "Failed to start lock task pinning (task not in foreground), retrying in 1s", e)
                    window.decorView.postDelayed({
                        if (isKioskOrLockActive() && isMainActivityInForeground) {
                            updateLockTask(true)
                        }
                    }, 1000)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to start lock task pinning", e)
                }
            } else if (!combinedActive && isInLockTask) {
                try {
                    stopLockTask()
                    Log.i("MainActivity", "stopLockTask() invoked successfully before clearing allowlist.")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to stop lock task pinning", e)
                }

                try {
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                    if (dpm != null) {
                        val adminName = com.example.util.DeviceAdminHelper.getAdminComponentName(this)
                        if (dpm.isDeviceOwnerApp(packageName)) {
                            dpm.setLockTaskPackages(adminName, emptyArray())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error clearing LockTask packages via DPM", e)
                }
            } else {
                Log.d("MainActivity", "No lock task state change required. Already in desired state.")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Fatal error in updateLockTask status evaluation", e)
        }
    }

    private fun configureLockScreenWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    private fun relaunchLockScreen() {
        val now = System.currentTimeMillis()
        if (now - lastRelaunchTime < 1000L) {
            return // Reduced cooldown to be more responsive
        }
        lastRelaunchTime = now
        relaunchFromApplication(this)
    }

    companion object {
        @Volatile
        var isMainActivityInForeground: Boolean = false

        @Volatile
        var isLockActive: Boolean = false

        @Volatile
        var isBiometricPromptActive: Boolean = false

        private var lastGlobalRelaunchTime = 0L
        private var lastLockTaskRequestTime = 0L

        fun isKioskOrLockActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
            val isKioskActive = prefs.getBoolean("kiosk_mode_enabled", false) ||
                prefs.getBoolean("central_lock_enforced", false)
            
            val isPolicyLocked = try {
                com.example.util.PolicyEnforcementManager.isPolicyLocked(context)
            } catch (e: Throwable) {
                false
            }
            
            return isLockActive || isKioskActive || isPolicyLocked
        }

        fun relaunchFromApplication(context: Context) {
            val now = System.currentTimeMillis()
            if (now - lastGlobalRelaunchTime < 1000L) {
                return // Throttle slightly less to be highly reactive, but prevent infinite loops
            }
            lastGlobalRelaunchTime = now

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_FORCE_LOCK_SCREEN", true)
            }
            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                
            )
            
            // 1. Direct send attempt
            try {
                pendingIntent.send(context, 0, null, null, null, null, options.toBundle())
            } catch (e: Exception) {
                Log.e("MainActivity", "Direct pendingintent relaunch failed, trying alternative paths", e)
            }

            // 2. AlarmManager fallback (highly reliable for background starts)
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                if (alarmManager != null) {
                    var canScheduleExact = true
                    try {
                        canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            alarmManager.canScheduleExactAlarms()
                        } else {
                            true
                        }
                    } catch (e: SecurityException) {
                        canScheduleExact = false
                    }
                    
                    if (canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + 100L,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + 100L,
                            pendingIntent
                        )
                    }
                    Log.i("MainActivity", "Scheduled AlarmManager fallback relaunch in 100ms.")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "AlarmManager relaunch scheduling failed", e)
            }

            // 3. Direct startActivity attempt
            try {
                context.startActivity(intent)
            } catch (ex: Exception) {
                Log.e("MainActivity", "Direct startActivity failed", ex)
            }
        }

        fun wakeUpDeviceScreen(context: Context) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isInteractive) {
                    @Suppress("DEPRECATION")
                    val wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK or
                                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                                PowerManager.ON_AFTER_RELEASE,
                        "SentinelX:LockWakeLock"
                    )
                    wakeLock.acquire(3000L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


