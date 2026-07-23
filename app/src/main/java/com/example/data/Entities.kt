package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String, // device unique id, e.g. sentinel-01
    val name: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val securityPatch: String,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val networkStatus: String, // "Wi-Fi (SentinelNet)", "LTE", "Offline"
    val storageTotalGb: Double,
    val storageUsedGb: Double,
    val ramTotalGb: Double,
    val ramUsedGb: Double,
    val isOnline: Boolean,
    val lastActiveTime: Long,
    val healthScore: Int, // 0 to 100
    val latitude: Double,
    val longitude: Double,
    val locationAccuracyMeters: Float,
    val isLostMode: Boolean,
    val customLostMessage: String = "",
    val customLostContact: String = "",
    val isLocked: Boolean = false,
    val isAlarmActive: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val message: String,
    val level: String, // "INFO", "WARNING", "CRITICAL"
    val deviceId: String
)

@Entity(tableName = "commands")
data class CommandEntity(
    @PrimaryKey val commandId: String, // e.g. uuid
    val type: String, // "GET_STATUS", "REQUEST_LOCATION", "LOCK_DEVICE", "START_LOST_MODE", "PLAY_ALARM", "WIPE_DEVICE"
    val targetDeviceId: String,
    val senderId: String,
    val timestamp: Long,
    val payloadJson: String,
    val status: String, // "Pending", "Sent", "Received", "Completed", "Failed"
    val signature: String
)
