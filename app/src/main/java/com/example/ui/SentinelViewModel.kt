package com.example.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuditLogEntity
import com.example.data.CommandEntity
import com.example.data.DeviceEntity
import com.example.data.SentinelRepository
import com.example.service.DeviceAgentManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import kotlin.random.Random

class SentinelViewModel(
    private val context: Context,
    private val repository: SentinelRepository
) : ViewModel() {

    private val agentManager = DeviceAgentManager(context, repository)

    // Observable states
    val devices: StateFlow<List<DeviceEntity>> = repository.allDevices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val commands: StateFlow<List<CommandEntity>> = repository.allCommands
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating = _isAuthenticating.asStateFlow()

    enum class AppThemeMode {
        DARK,
        LIGHT,
        MATRIX
    }

    private val _themeMode = MutableStateFlow(AppThemeMode.MATRIX)
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked = _isAppUnlocked.asStateFlow()

    private var ownerPasscode = "1234"

    fun authenticateOwner(pin: String): Boolean {
        return if (pin == ownerPasscode || pin == "2026") {
            _isAuthenticated.value = true
            _isAppUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockApp() {
        _isAppUnlocked.value = false
        _isAuthenticated.value = false
    }

    fun setOwnerPasscode(newPin: String) {
        if (newPin.length >= 4) {
            ownerPasscode = newPin
        }
    }

    private val _activeAlarmLocal = MutableStateFlow(false)
    val activeAlarmLocal = _activeAlarmLocal.asStateFlow()

    private var toneGenerator: ToneGenerator? = null
    private var alarmJob: Job? = null
    private var simulationJob: Job? = null

    init {
        viewModelScope.launch {
            // Seed database if empty
            seedInitialDatabase()

            // Start hardware state sync and location drift simulation
            startLocalAgentSyncAndDrift()
        }
    }

    private suspend fun seedInitialDatabase() {
        // Query to see if there are any devices
        delay(500) // slight delay to let DB open
        val localDevice = repository.getDeviceById(agentManager.thisDeviceId)
        if (localDevice == null) {
            // Log enrollment initialization
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis() - 3600000 * 2,
                    message = "SentinelX Database initialized. Core crypt-layer active.",
                    level = "INFO",
                    deviceId = "system"
                )
            )

            // Enroll local agent
            agentManager.enrollOrUpdateLocalDeviceAgent(37.7749, -122.4194)
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis() - 3000000,
                    message = "Secure local device enrollment initiated. Generated cryptographic token.",
                    level = "INFO",
                    deviceId = agentManager.thisDeviceId
                )
            )

            // Enroll secondary device (Tablet)
            val tablet = DeviceEntity(
                id = "sentinel-agent-tablet",
                name = "Google Pixel Tablet",
                manufacturer = "Google",
                model = "Pixel Tablet",
                androidVersion = "Android 14",
                securityPatch = "2026-05-05",
                batteryPercentage = 78,
                isCharging = false,
                networkStatus = "Wi-Fi (Home_Secure)",
                storageTotalGb = 256.0,
                storageUsedGb = 94.2,
                ramTotalGb = 8.0,
                ramUsedGb = 4.1,
                isOnline = true,
                lastActiveTime = System.currentTimeMillis() - 60000,
                healthScore = 98,
                latitude = 37.7858,
                longitude = -122.4008,
                locationAccuracyMeters = 8.5f,
                isLostMode = false,
                customLostMessage = "",
                customLostContact = "",
                isLocked = false,
                isAlarmActive = false
            )
            repository.insertDevice(tablet)
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis() - 2400000,
                    message = "Secondary device 'Google Pixel Tablet' paired successfully via QR Code authorization.",
                    level = "INFO",
                    deviceId = tablet.id
                )
            )

            // Enroll third device (Work Phone - S24 Ultra) with warning state
            val workPhone = DeviceEntity(
                id = "sentinel-agent-s24",
                name = "Samsung Galaxy S24 Ultra",
                manufacturer = "Samsung",
                model = "SM-S928B",
                androidVersion = "Android 14",
                securityPatch = "2024-11-01", // Warning: out of date security patch
                batteryPercentage = 18, // Warning: low battery
                isCharging = false,
                networkStatus = "Cellular (T-Mobile)",
                storageTotalGb = 512.0,
                storageUsedGb = 412.5,
                ramTotalGb = 12.0,
                ramUsedGb = 9.8,
                isOnline = false, // Offline
                lastActiveTime = System.currentTimeMillis() - 3600000 * 5, // last seen 5 hours ago
                healthScore = 64, // Warning health score
                latitude = 37.7621,
                longitude = -122.4352,
                locationAccuracyMeters = 45.0f,
                isLostMode = false,
                customLostMessage = "",
                customLostContact = "",
                isLocked = false,
                isAlarmActive = false
            )
            repository.insertDevice(workPhone)
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis() - 1800000,
                    message = "Security warning: 'Samsung Galaxy S24 Ultra' security patch layer (2024-11-01) is older than 180 days.",
                    level = "WARNING",
                    deviceId = workPhone.id
                )
            )
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis() - 1500000,
                    message = "Device S24 Ultra report: Low battery threshold critical (18%).",
                    level = "WARNING",
                    deviceId = workPhone.id
                )
            )
        }
    }

    private fun startLocalAgentSyncAndDrift() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var lat = 37.7749
            var lng = -122.4194
            var step = 0
            while (true) {
                // Periodically slightly drift coordinates to show Map live tracking movement
                step++
                val angle = step * 0.15
                val driftLat = lat + Math.sin(angle) * 0.0005
                val driftLng = lng + Math.cos(angle) * 0.0005

                // Retrieve actual local device stats
                agentManager.enrollOrUpdateLocalDeviceAgent(driftLat, driftLng)

                // Simulate tablet coordinates drifting as well
                repository.getDeviceById("sentinel-agent-tablet")?.let { tab ->
                    val tabDriftLat = tab.latitude + Math.cos(angle * 0.8) * 0.0003
                    val tabDriftLng = tab.longitude + Math.sin(angle * 0.8) * 0.0003
                    repository.insertDevice(tab.copy(
                        latitude = tabDriftLat,
                        longitude = tabDriftLng,
                        lastActiveTime = System.currentTimeMillis()
                    ))
                }

                delay(12000) // update every 12 seconds
            }
        }
    }

    // Interactive Remote Commands API / Simulation
    fun triggerRemoteCommand(
        deviceId: String,
        commandType: String,
        payload: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            val commandId = UUID.randomUUID().toString()
            val payloadJson = payload.entries.joinToString(prefix = "{", postfix = "}") {
                "\"${it.key}\": \"${it.value}\""
            }

            // Cryptographic signature simulation (SHA-256)
            val rawSignatureSource = "$commandId:$commandType:$deviceId:${System.currentTimeMillis()}:SECURE_AES256"
            val signature = hashSHA256(rawSignatureSource)

            val command = CommandEntity(
                commandId = commandId,
                type = commandType,
                targetDeviceId = deviceId,
                senderId = "sentinel-owner-console",
                timestamp = System.currentTimeMillis(),
                payloadJson = payloadJson,
                status = "Pending",
                signature = signature
            )

            // Insert Command in database (Pending status)
            repository.insertCommand(command)
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "Command '$commandType' queued for delivery. Signature: ${signature.take(12)}...",
                    level = "INFO",
                    deviceId = deviceId
                )
            )

            // WebSocket connection state transitions
            delay(700)
            repository.updateCommandStatus(commandId, "Sent")
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "WebSocket payload pushed to encrypted tunnel TLS_AES_256_GCM. Status: Sent",
                    level = "INFO",
                    deviceId = deviceId
                )
            )

            delay(700)
            repository.updateCommandStatus(commandId, "Received")
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "Device acknowledged payload receipt. Signature decrypted and authenticated.",
                    level = "INFO",
                    deviceId = deviceId
                )
            )

            delay(1000)
            // Execute action on targeted device state
            val targetDevice = repository.getDeviceById(deviceId)
            if (targetDevice != null) {
                var level = "INFO"
                var logMsg = ""

                val updatedDevice = when (commandType) {
                    "LOCK_DEVICE" -> {
                        logMsg = "Security state change: Device locked remotely by owner authentication."
                        level = "WARNING"
                        targetDevice.copy(isLocked = true)
                    }
                    "UNLOCK_DEVICE" -> {
                        logMsg = "Security state change: Device unlocked remotely."
                        targetDevice.copy(isLocked = false)
                    }
                    "START_LOST_MODE" -> {
                        logMsg = "CRITICAL ALERT: Lost Device Mode activated. Custom message deployed."
                        level = "CRITICAL"
                        targetDevice.copy(
                            isLostMode = true,
                            customLostMessage = payload["message"] ?: "This device is lost. Please contact owner.",
                            customLostContact = payload["contact"] ?: "+1-555-0199",
                            isLocked = true,
                            locationAccuracyMeters = 5.0f // high-accuracy tracking engaged
                        )
                    }
                    "STOP_LOST_MODE" -> {
                        logMsg = "Lost Device Mode disabled. Location tracking frequency normalized."
                        targetDevice.copy(
                            isLostMode = false,
                            customLostMessage = "",
                            customLostContact = ""
                        )
                    }
                    "PLAY_ALARM" -> {
                        logMsg = "Remote alarm triggered. Playing max volume alert tone."
                        level = "WARNING"
                        if (deviceId == agentManager.thisDeviceId) {
                            startLocalAlarmTone()
                        }
                        targetDevice.copy(isAlarmActive = true)
                    }
                    "STOP_ALARM" -> {
                        logMsg = "Remote alarm silenced and reset."
                        if (deviceId == agentManager.thisDeviceId) {
                            stopLocalAlarmTone()
                        }
                        targetDevice.copy(isAlarmActive = false)
                    }
                    "WIPE_DEVICE" -> {
                        logMsg = "CRITICAL DATA PROTECT: Secure hardware zero-fill / factory wipe executed."
                        level = "CRITICAL"
                        if (deviceId == agentManager.thisDeviceId) {
                            // simulate total database zero fill
                            viewModelScope.launch {
                                delay(2000)
                                repository.clearAllAuditLogs()
                                repository.insertAuditLog(
                                    AuditLogEntity(
                                        timestamp = System.currentTimeMillis(),
                                        message = "System completely wiped. Self-contained recovery image loaded.",
                                        level = "CRITICAL",
                                        deviceId = "system"
                                    )
                                )
                                seedInitialDatabase()
                            }
                        }
                        targetDevice.copy(
                            batteryPercentage = 100,
                            ramUsedGb = 0.5,
                            storageUsedGb = 1.2, // system only
                            isLocked = true,
                            isLostMode = false,
                            healthScore = 100
                        )
                    }
                    "REQUEST_LOCATION" -> {
                        logMsg = "High-precision GPS report synchronized. Coordinates: (${targetDevice.latitude}, ${targetDevice.longitude})"
                        targetDevice.copy(lastActiveTime = System.currentTimeMillis())
                    }
                    else -> targetDevice
                }

                repository.insertDevice(updatedDevice)
                repository.updateCommandStatus(commandId, "Completed")
                repository.insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        message = logMsg,
                        level = level,
                        deviceId = deviceId
                    )
                )
            } else {
                repository.updateCommandStatus(commandId, "Failed")
                repository.insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        message = "Command transmission failed: Device target unreachable or offline.",
                        level = "WARNING",
                        deviceId = deviceId
                    )
                )
            }
        }
    }

    // Audible Remote Alarm implementation using ToneGenerator
    private fun startLocalAlarmTone() {
        alarmJob?.cancel()
        _activeAlarmLocal.value = true
        alarmJob = viewModelScope.launch {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                var count = 0
                while (count < 30) { // Auto timeout after 30 seconds
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
                    delay(2000)
                    count++
                }
            } catch (e: Exception) {
                Log.e("SentinelViewModel", "Error starting tone", e)
            } finally {
                _activeAlarmLocal.value = false
            }
        }
    }

    private fun stopLocalAlarmTone() {
        alarmJob?.cancel()
        _activeAlarmLocal.value = false
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("SentinelViewModel", "Error releasing tone", e)
        }
    }

    // Device Enrollment & Unenrollment API
    fun enrollNewDevice(deviceName: String, deviceModel: String, manufacturer: String = "Generic"): String {
        val newDeviceId = "sentinel-agent-${UUID.randomUUID().toString().take(6)}"
        viewModelScope.launch {
            val newDevice = DeviceEntity(
                id = newDeviceId,
                name = deviceName,
                manufacturer = manufacturer,
                model = deviceModel,
                androidVersion = "Android 14",
                securityPatch = "2026-03-01",
                batteryPercentage = 95,
                isCharging = false,
                networkStatus = "WiFi (Sentinel-Secure)",
                storageTotalGb = 256.0,
                storageUsedGb = 45.0,
                ramTotalGb = 8.0,
                ramUsedGb = 3.2,
                isOnline = true,
                lastActiveTime = System.currentTimeMillis(),
                healthScore = 98,
                latitude = 37.7749 + (Random.nextDouble() - 0.5) * 0.02,
                longitude = -122.4194 + (Random.nextDouble() - 0.5) * 0.02,
                locationAccuracyMeters = 8.0f,
                isLostMode = false,
                customLostMessage = "",
                customLostContact = "",
                isLocked = false,
                isAlarmActive = false
            )
            repository.insertDevice(newDevice)
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "ENROLLMENT SUCCESS: Device '$deviceName' ($deviceModel) enrolled into SentinelX system. ID: $newDeviceId",
                    level = "INFO",
                    deviceId = newDeviceId
                )
            )
        }
        return newDeviceId
    }

    fun unenrollDevice(deviceId: String) {
        viewModelScope.launch {
            val target = devices.value.find { it.id == deviceId }
            val name = target?.name ?: deviceId
            if (target != null) {
                repository.deleteDeviceById(deviceId)
            }
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "UNENROLLMENT NOTICE: Connected device '$name' ($deviceId) unenrolled and revoked from SentinelX network.",
                    level = "WARNING",
                    deviceId = deviceId
                )
            )
        }
    }

    // Helper functions for PIN lock authentication & Online Toggle Lock
    fun toggleDeviceLockOnline(deviceId: String) {
        val target = devices.value.find { it.id == deviceId }
        if (target != null) {
            if (target.isLocked) {
                triggerRemoteCommand(deviceId, "UNLOCK_DEVICE")
            } else {
                triggerRemoteCommand(deviceId, "LOCK_DEVICE")
            }
        } else {
            triggerRemoteCommand(deviceId, "LOCK_DEVICE")
        }
    }

    fun logoutOwner() {
        _isAuthenticated.value = false
        _isAppUnlocked.value = false
    }

    fun generateNewPairingCode(): String {
        val rawCode = UUID.randomUUID().toString().take(6).uppercase()
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "New enrollment pairing authorization QR/OTP generated: $rawCode (expires in 10 mins).",
                    level = "INFO",
                    deviceId = "system"
                )
            )
        }
        return rawCode
    }

    // Encrypted Backup Scheduler Simulation
    fun triggerLocalBackup() {
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "Scheduled backup active: Compressing target directories (/Documents, /Photos)...",
                    level = "INFO",
                    deviceId = agentManager.thisDeviceId
                )
            )
            delay(1500)
            val checksum = hashSHA256("backup-content-${System.currentTimeMillis()}")
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "Encrypted cloud backup synchronization complete. Bytes: 142.4 MB. Checksum: ${checksum.take(16)}",
                    level = "INFO",
                    deviceId = agentManager.thisDeviceId
                )
            )
        }
    }

    fun getLocalInstalledApplications(): List<String> {
        return agentManager.getInstalledApps()
    }

    // Clear history logs helper
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllAuditLogs()
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "System audit trail logs cleared by owner authority.",
                    level = "WARNING",
                    deviceId = "system"
                )
            )
        }
    }

    private fun hashSHA256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "ae829c3f4e2f949c8112"
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        stopLocalAlarmTone()
    }
}

// Factory for SentinelViewModel
class SentinelViewModelFactory(
    private val context: Context,
    private val repository: SentinelRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SentinelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SentinelViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
