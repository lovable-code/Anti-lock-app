package com.example.service

import android.app.ActivityOptions
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
import com.example.data.AuditLogEntity
import com.example.data.SentinelDatabase
import com.example.data.SentinelRepository
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.util.DeviceAdminHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.provider.Settings

class SentinelForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: SentinelRepository
    @Volatile private var isWebSocketConnected = false
    private var heartbeatCount = 0L
    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    private var securityReceiver: BroadcastReceiver? = null
    private var toneGenerator: android.media.ToneGenerator? = null
    private var serviceAlarmJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        com.example.util.PolicyEnforcementManager.reconcilePolicyOnBoot(applicationContext)
        val dao = SentinelDatabase.getDatabase(applicationContext).sentinelDao()
        repository = SentinelRepository(dao)
        createNotificationChannel()
        startCommandListener()
        registerSecurityReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SERVICE

        when (action) {
            ACTION_STOP_SERVICE -> {
                Log.i(TAG, "Stopping SentinelForegroundService upon user request.")
                webSocket?.close(1000, "Service Stopping")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_LOCK_COMMAND -> {
                Log.w(TAG, "Direct ACTION_LOCK_COMMAND intent received in Foreground Service.")
                serviceScope.launch {
                    processLockPayload(
                        commandType = "LOCK_COMMAND",
                        payloadMessage = intent?.getStringExtra(EXTRA_LOCK_MESSAGE) ?: "System-Level Remote Lock Enforced via Foreground Service",
                        payloadContact = intent?.getStringExtra(EXTRA_LOCK_CONTACT) ?: "+1-555-0199"
                    )
                }
            }
            ACTION_START_SERVICE -> {
                val notification = buildNotification(
                    title = "SentinelX Anti-Theft Security Active",
                    contentText = "Persistent Cloud Security Monitor Initialized"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val locationGranted = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    
                    var foregroundServiceType = 0
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        foregroundServiceType = foregroundServiceType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    }
                    
                    if (locationGranted) {
                        try {
                            if (com.example.MainActivity.isMainActivityInForeground) {
                                foregroundServiceType = foregroundServiceType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                            }
                        } catch (e: Exception) {
                            Log.w("SentinelForegroundService", "Could not check foreground status for location FGS")
                        }
                    }

                    try {
                        if (foregroundServiceType != 0) {
                            startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
                        } else {
                            startForeground(NOTIFICATION_ID, notification)
                        }
                    } catch (e: Exception) {
                        Log.e("SentinelForegroundService", "FGS start failed: ${e.message}. Retrying with minimal type.")
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                            } else {
                                startForeground(NOTIFICATION_ID, notification)
                            }
                        } catch (e2: Exception) {
                            startForeground(NOTIFICATION_ID, notification)
                        }
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                startCommandListener()
                startMonitoringLoop()
                scheduleSelfHealingAlarm()
            }
        }
        return START_STICKY
    }

    private fun scheduleSelfHealingAlarm() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            val alarmIntent = Intent(applicationContext, com.example.receiver.BootReceiver::class.java).apply {
                action = "com.example.ACTION_PING_SERVICE"
            }
            val pendingAlarm = PendingIntent.getBroadcast(
                applicationContext,
                3003,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Register a repeating wake-up alarm every 60 seconds to ensure the sentinel loop remains online
            alarmManager.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 60000L,
                60000L,
                pendingAlarm
            )
            Log.i(TAG, "Self-Healing AlarmManager Daemon Scheduled Successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule self-healing alarm", e)
        }
    }

    
    private fun startCommandListener() {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = try { auth.currentUser } catch (e: Exception) { null }
            val ownerId = currentUser?.uid ?: run {
                Log.w(TAG, "No authenticated user found for command listener. Retrying later.")
                return
            }
            
            val deviceId = DeviceAgentManager(applicationContext, repository).thisDeviceId

            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            firestore.collection("users").document(ownerId)
                .collection("devices").document(deviceId)
                .collection("commands")
                .whereEqualTo("status", "Pending")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "Command snapshot listener error: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    for (dc in snapshots.documentChanges) {
                        if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val doc = dc.document
                            val commandType = doc.getString("type") ?: continue
                            val payloadJson = doc.getString("payloadJson") ?: "{}"

                            // Execute command
                            executeCommand(commandType, payloadJson)

                            // Mark as executed
                            doc.reference.update("status", "Executed")
                        }
                    }
                }

            // Sync other devices from Firestore to local Room DB
            firestore.collection("users").document(ownerId)
                .collection("devices")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "Device list snapshot listener error: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener
                    
                    serviceScope.launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            // Don't overwrite local device with cloud state (local is source of truth for self)
                            if (id == deviceId) continue
                            
                            val name = doc.getString("name") ?: "Unknown Device"
                            val manufacturer = doc.getString("manufacturer") ?: ""
                            val model = doc.getString("model") ?: ""
                            val androidVersion = doc.getString("androidVersion") ?: ""
                            val securityPatch = doc.getString("securityPatch") ?: ""
                            val batteryPercentage = doc.getLong("batteryPercentage")?.toInt() ?: 100
                            val isCharging = doc.getBoolean("isCharging") ?: false
                            val networkStatus = doc.getString("networkStatus") ?: "Connected"
                            val storageTotalGb = doc.getDouble("storageTotalGb") ?: 128.0
                            val storageUsedGb = doc.getDouble("storageUsedGb") ?: 0.0
                            val ramTotalGb = doc.getDouble("ramTotalGb") ?: 8.0
                            val ramUsedGb = doc.getDouble("ramUsedGb") ?: 0.0
                            val isOnline = doc.getBoolean("isOnline") ?: false
                            val lastActiveTime = doc.getLong("lastActiveTime") ?: System.currentTimeMillis()
                            val healthScore = doc.getLong("healthScore")?.toInt() ?: 100
                            val latitude = doc.getDouble("latitude") ?: 0.0
                            val longitude = doc.getDouble("longitude") ?: 0.0
                            val isLostMode = doc.getBoolean("isLostMode") ?: false
                            val isLocked = doc.getBoolean("isLocked") ?: false
                            val isAlarmActive = doc.getBoolean("isAlarmActive") ?: false
                            val customLostMessage = doc.getString("customLostMessage") ?: ""
                            val customLostContact = doc.getString("customLostContact") ?: ""

                            val remoteDevice = com.example.data.DeviceEntity(
                                id = id,
                                name = name,
                                manufacturer = manufacturer,
                                model = model,
                                androidVersion = androidVersion,
                                securityPatch = securityPatch,
                                batteryPercentage = batteryPercentage,
                                isCharging = isCharging,
                                networkStatus = networkStatus,
                                storageTotalGb = storageTotalGb,
                                storageUsedGb = storageUsedGb,
                                ramTotalGb = ramTotalGb,
                                ramUsedGb = ramUsedGb,
                                isOnline = isOnline,
                                lastActiveTime = lastActiveTime,
                                healthScore = healthScore,
                                latitude = latitude,
                                longitude = longitude,
                                locationAccuracyMeters = 10f,
                                isLostMode = isLostMode,
                                customLostMessage = customLostMessage,
                                customLostContact = customLostContact,
                                isLocked = isLocked,
                                isAlarmActive = isAlarmActive
                            )
                            repository.insertDevice(remoteDevice)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Sync listeners initialization failed: ${e.message}")
        }
    }

    private fun executeCommand(commandType: String, payloadJson: String) {
        val deviceId = DeviceAgentManager(applicationContext, repository).thisDeviceId
        when (commandType) {
            "LOCK_DEVICE", "LOCK_COMMAND" -> {
                com.example.util.DeviceAdminHelper.lockDeviceScreenNow(applicationContext)
                com.example.util.PolicyEnforcementManager.enforceCurrentPolicy(applicationContext, "LOCK_DEVICE")
                sendLockCommand(applicationContext, "Remote LOCK_COMMAND executed", "+1-555-0199")
            }
            "UNLOCK_DEVICE" -> {
                com.example.util.PolicyEnforcementManager.enforceCurrentPolicy(applicationContext, "UNLOCK_DEVICE")
            }
            "WIPE_DEVICE" -> {
                com.example.util.DeviceAdminHelper.wipeDeviceNow(applicationContext)
            }
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SentinelX Security Channel"
            val descriptionText = "Maintains persistent cloud connection for real-time remote lock commands"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private var lastNotifiedTitle = ""
    private var lastNotifiedContent = ""
    private var lastNotificationTime = 0L

    private fun updateNotificationText(title: String, contentText: String, isHighPriority: Boolean = false) {
        val now = System.currentTimeMillis()
        // Deduplicate and throttle notifications to prevent NotifAttentionHelper noisy muting
        if (title == lastNotifiedTitle && contentText == lastNotifiedContent && (now - lastNotificationTime) < 15000L) {
            return
        }
        lastNotifiedTitle = title
        lastNotifiedContent = contentText
        lastNotificationTime = now

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildNotification(title, contentText, isHighPriority))
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }

    private fun buildNotification(title: String, contentText: String, isHighPriority: Boolean = false): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        val stopIntent = Intent(this, SentinelForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(if (isHighPriority) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (isHighPriority) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_SERVICE)

        if (isHighPriority) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Pause Monitor",
            stopPendingIntent
        )

        return builder.build()
    }

    private fun registerSecurityReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        securityReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return
                serviceScope.launch {
                    try {
                        val localDevice = repository.getDeviceById(com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext)) ?: return@launch
                        val isLocked = localDevice.isLocked || localDevice.isLostMode
                        
                        when (action) {
                            Intent.ACTION_POWER_DISCONNECTED -> {
                                repository.insertAuditLog(
                                    AuditLogEntity(
                                        timestamp = System.currentTimeMillis(),
                                        message = "CHARGER DISCONNECT EVENT: External power supply disconnected from hardware interface.",
                                        level = "INFO",
                                        deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext)
                                    )
                                )
                                if (isLocked) {
                                    repository.insertAuditLog(
                                        AuditLogEntity(
                                            timestamp = System.currentTimeMillis(),
                                            message = "🚨 THEFT CONTAINMENT TRIGGER: Power disconnected while in lost/locked mode! Playing maximum volume warning siren.",
                                            level = "CRITICAL",
                                            deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext)
                                        )
                                    )
                                    repository.updateDevice(localDevice.copy(isAlarmActive = true))
                                    playServiceEmergencySiren()
                                }
                            }
                            Intent.ACTION_POWER_CONNECTED -> {
                                repository.insertAuditLog(
                                    AuditLogEntity(
                                        timestamp = System.currentTimeMillis(),
                                        message = "CHARGER CONNECT EVENT: External power supply connected to hardware interface.",
                                        level = "INFO",
                                        deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext)
                                    )
                                )
                            }
                            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                                val isAirplaneModeOn = Settings.Global.getInt(
                                    context.contentResolver,
                                    Settings.Global.AIRPLANE_MODE_ON, 0
                                ) != 0
                                
                                repository.insertAuditLog(
                                    AuditLogEntity(
                                        timestamp = System.currentTimeMillis(),
                                        message = "AIRPLANE MODE CHANGE: Network isolation state updated. Enabled = $isAirplaneModeOn",
                                        level = if (isAirplaneModeOn) "WARNING" else "INFO",
                                        deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext)
                                    )
                                )
                                
                                if (isAirplaneModeOn && isLocked) {
                                    repository.insertAuditLog(
                                        AuditLogEntity(
                                            timestamp = System.currentTimeMillis(),
                                            message = "🚨 THEFT CONTAINMENT TRIGGER: Airplane Mode activated while device is in locked/lost mode. Attempted network isolation detected! Triggering local acoustic deterrent.",
                                            level = "CRITICAL",
                                            deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext)
                                        )
                                    )
                                    repository.updateDevice(localDevice.copy(isAlarmActive = true))
                                    playServiceEmergencySiren()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        registerReceiver(securityReceiver, filter)
    }

    private fun unregisterSecurityReceiver() {
        securityReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        securityReceiver = null
        stopServiceEmergencySiren()
    }

    private fun playServiceEmergencySiren() {
        serviceAlarmJob?.cancel()
        serviceAlarmJob = serviceScope.launch {
            try {
                toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                var count = 0
                while (count < 30) {
                    toneGenerator?.startTone(android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
                    delay(2000)
                    count++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing service siren", e)
            }
        }
    }

    private fun stopServiceEmergencySiren() {
        serviceAlarmJob?.cancel()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "Application Task Removed / Force-Closed by User. Self-healing SentinelForegroundService restarting.")

        val restartServiceIntent = Intent(applicationContext, SentinelForegroundService::class.java).apply {
            action = ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartServiceIntent)
        } else {
            applicationContext.startService(restartServiceIntent)
        }
    }


    private suspend fun processLockPayload(commandType: String, payloadMessage: String, payloadContact: String) {
        val prefs = applicationContext.getSharedPreferences("sentinel_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("central_lock_enforced", true)
            .putString("lost_mode_message", payloadMessage)
            .putString("lost_mode_contact", payloadContact)
            .apply()
        com.example.util.DeviceAdminHelper.lockDeviceScreenNow(applicationContext)
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            val agentManager = DeviceAgentManager(applicationContext, repository)
            while (true) {
                try {
                    val updatedDevice = agentManager.enrollOrUpdateLocalDeviceAgent()
                    Log.d(TAG, "ForegroundService GPS Telemetry Updated: Lat ${updatedDevice.latitude}, Lng ${updatedDevice.longitude}")

                    // Check background auto-lock expiration
                    val prefs = applicationContext.getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
                    val localDeviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext)
                    val expireTime = prefs.getLong("timed_lock_expire_$localDeviceId", 0L)
                    if (expireTime > 0L && System.currentTimeMillis() >= expireTime) {
                        prefs.edit().remove("timed_lock_expire_$localDeviceId").apply()
                        Log.w(TAG, "🚨 Foreground Service detected expired timed lock timer ($expireTime <= ${System.currentTimeMillis()})! Locking device now.")
                        prefs.edit().putBoolean("central_lock_enforced", true).apply()

                        val dev = repository.getDeviceById(localDeviceId)
                        if (dev != null) {
                            repository.updateDevice(dev.copy(isLocked = true, customLostMessage = "TIMED AUTO-LOCK EXPIRED • DEVICE LOCKED BY OWNER POLICY"))
                        }

                        com.example.util.PolicyEnforcementManager.setPolicyState(applicationContext, com.example.util.SecurityPolicyState.LOCKED, "Timed Auto-Lock Expired")
                        com.example.util.PolicyEnforcementManager.enforceCurrentPolicy(applicationContext, "Timed Auto-Lock Expired")
                        com.example.util.DeviceAdminHelper.lockDeviceScreenNow(applicationContext)
                        com.example.MainActivity.relaunchFromApplication(applicationContext)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in GPS telemetry background loop: ${e.message}")
                }
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "SentinelForegroundService onDestroy invoked.")
        unregisterSecurityReceiver()
        webSocket?.close(1000, "Service Destroyed")
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "SentinelFGService"
        const val CHANNEL_ID = "sentinel_security_service_channel"
        const val NOTIFICATION_ID = 8801

        const val ACTION_START_SERVICE = "com.example.service.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_SERVICE"
        const val ACTION_LOCK_COMMAND = "com.example.service.ACTION_LOCK_COMMAND"

        const val EXTRA_LOCK_MESSAGE = "extra_lock_message"
        const val EXTRA_LOCK_CONTACT = "extra_lock_contact"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, SentinelForegroundService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("SentinelForegroundService", "Failed to start service: ${e.message}")
            }
        }

        fun sendLockCommand(context: Context, message: String = "", contact: String = "") {
            try {
                val intent = Intent(context, SentinelForegroundService::class.java).apply {
                    action = ACTION_LOCK_COMMAND
                    putExtra(EXTRA_LOCK_MESSAGE, message)
                    putExtra(EXTRA_LOCK_CONTACT, contact)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("SentinelForegroundService", "Failed to send lock command: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SentinelForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}

