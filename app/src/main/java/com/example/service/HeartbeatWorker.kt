package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.SentinelDatabase
import com.example.data.SentinelRepository
import com.example.data.AuditLogEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HeartbeatWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i("HeartbeatWorker", "Periodic WorkManager Sentinel Heartbeat initiated.")
        val context = applicationContext
        
        try {
            // 1. Ensure Foreground service is running (Self-Healing / Prevention of being killed)
            try {
                SentinelForegroundService.startService(context)
            } catch (e: Exception) {
                Log.w("HeartbeatWorker", "Could not start ForegroundService from background: ${e.message}")
            }
            
            val database = SentinelDatabase.getDatabase(context)
            val repository = SentinelRepository(database.sentinelDao())
            
            // Increment local heartbeat counters if any
            val localDevice = repository.getDeviceById(com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext))
            if (localDevice != null) {
                val updatedDevice = localDevice.copy(
                    lastActiveTime = System.currentTimeMillis()
                )
                repository.updateDevice(updatedDevice)
                
                // 2. Add Firestore persistent sync!
                syncDeviceStateToFirestore(updatedDevice)
            }
            
            // Insert audit log to Room database
            val logMessage = "🛡️ WorkManager Daemon Heartbeat triggered. Keep-alive verified. Service reinforced. Dispatched status telemetry to SentinelX cloud."
            val auditLog = AuditLogEntity(
                timestamp = System.currentTimeMillis(),
                message = logMessage,
                level = "INFO",
                deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(applicationContext)
            )
            repository.insertAuditLog(auditLog)
            
            // Sync audit log to Firestore
            syncAuditLogToFirestore(auditLog)

            Log.i("HeartbeatWorker", "WorkManager heartbeat completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("HeartbeatWorker", "Error executing periodic heartbeat task", e)
            Result.retry()
        }
    }

    private fun syncDeviceStateToFirestore(device: com.example.data.DeviceEntity) {
        try {
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "id" to device.id,
                "name" to device.name,
                "model" to device.model,
                "manufacturer" to device.manufacturer,
                "isLocked" to device.isLocked,
                "isLostMode" to device.isLostMode,
                "customLostMessage" to device.customLostMessage,
                "customLostContact" to device.customLostContact,
                "isAlarmActive" to device.isAlarmActive,
                "latitude" to device.latitude,
                "longitude" to device.longitude,
                "lastActiveTime" to device.lastActiveTime
            )
            val ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.collection("users").document(ownerId).collection("devices")
                .document(device.id)
                .set(data)
                .addOnSuccessListener {
                    Log.d("HeartbeatWorker", "Successfully synced device ${device.id} to Firestore.")
                }
                .addOnFailureListener { e ->
                    Log.e("HeartbeatWorker", "Firestore sync failed (expected if google-services.json is missing): ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("HeartbeatWorker", "Firestore error: ${e.message}")
        }
    }

    private fun syncAuditLogToFirestore(log: AuditLogEntity) {
        try {
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "id" to log.id,
                "timestamp" to log.timestamp,
                "message" to log.message,
                "level" to log.level,
                "deviceId" to log.deviceId
            )
            val ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.collection("users").document(ownerId).collection("auditLogs")
                .document("log_${log.timestamp}")
                .set(data)
                .addOnSuccessListener {
                    Log.d("HeartbeatWorker", "Successfully synced log to Firestore.")
                }
                .addOnFailureListener { e ->
                    Log.e("HeartbeatWorker", "Firestore sync failed for log: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("HeartbeatWorker", "Firestore error: ${e.message}")
        }
    }
}
