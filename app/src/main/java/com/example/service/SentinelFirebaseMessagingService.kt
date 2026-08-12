package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AuditLogEntity
import com.example.data.CommandEntity
import com.example.data.SentinelDatabase
import com.example.util.PolicyEnforcementManager
import com.example.util.SecurityPolicyState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SentinelFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Token received: $token")
        
        // Save locally
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        
        // Sync to cloud
        syncTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM Message received from: ${remoteMessage.from}")
        
        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.w(TAG, "Received FCM message with empty data payload")
            return
        }
        
        val commandId = data["commandId"] ?: return
        val targetDeviceId = data["targetDeviceId"] ?: return
        val localDeviceId = DeviceAgentManager.getLocalDeviceId(applicationContext)

        if (targetDeviceId != localDeviceId && !localDeviceId.contains(targetDeviceId)) {
            Log.w(TAG, "FCM Command target ($targetDeviceId) does not match this device ($localDeviceId). Ignoring.")
            return
        }

        // Process durable command lifecycle by fetching from Firestore
        processIncomingCommand(commandId, localDeviceId)
    }

    private fun syncTokenToFirestore(token: String) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = DeviceAgentManager.getLocalDeviceId(applicationContext)
        
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(ownerId)
                .collection("devices").document(deviceId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully synced FCM token to Firestore for device $deviceId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync FCM token to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Firestore instance for FCM token sync", e)
        }
    }

    private fun processIncomingCommand(commandId: String, targetDeviceId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val dbFs = FirebaseFirestore.getInstance()
                
                // Fetch authoritative command from Firestore
                val cmdDoc = dbFs.collection("users").document(ownerId)
                    .collection("devices").document(targetDeviceId)
                    .collection("commands").document(commandId).get().await()
                    
                if (!cmdDoc.exists()) {
                    Log.w(TAG, "Command $commandId not found in Firestore. Ignoring.")
                    return@launch
                }
                
                val commandType = cmdDoc.getString("type") ?: ""
                val status = cmdDoc.getString("status") ?: ""
                val payloadJson = cmdDoc.getString("payloadJson") ?: "{}"
                val senderId = cmdDoc.getString("senderId") ?: ""
                val timestamp = cmdDoc.getLong("timestamp") ?: System.currentTimeMillis()
                
                if (status != "CREATED" && status != "Pending" && status != "RECEIVED") {
                    Log.w(TAG, "Command $commandId already processed (Status: $status). Ignoring.")
                    return@launch
                }
                
                val db = SentinelDatabase.getDatabase(applicationContext)
                val dao = db.sentinelDao()
                
                val existingCmd = dao.getCommandById(commandId)
                if (existingCmd != null && (existingCmd.status == "SUCCEEDED" || existingCmd.status == "FAILED")) {
                    Log.w(TAG, "Command $commandId already fully processed locally. Ignoring.")
                    return@launch
                }
                
                updateCloudCommandStatus(commandId, targetDeviceId, "RECEIVED")

                // 1. Audit log the receipt of FCM command
                dao.insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        message = "FCM Command authenticated from Cloud: [$commandType] ID: $commandId",
                        level = "INFO",
                        deviceId = targetDeviceId
                    )
                )

                // 2. Persist command entity locally
                if (existingCmd == null) {
                    val command = CommandEntity(
                        commandId = commandId,
                        type = commandType,
                        targetDeviceId = targetDeviceId,
                        senderId = senderId,
                        timestamp = timestamp,
                        payloadJson = payloadJson,
                        status = "EXECUTING",
                        signature = "FIRESTORE_AUTHENTICATED"
                    )
                    dao.insertCommand(command)
                } else {
                    dao.updateCommandStatus(commandId, "EXECUTING")
                }
                updateCloudCommandStatus(commandId, targetDeviceId, "EXECUTING")

                // 3. Execute policy action according to command type
                when (commandType) {
                    "LOCK", "LOCK_DEVICE", "LOST_MODE", "FORCE_LOCK" -> {
                        PolicyEnforcementManager.setPolicyState(
                            applicationContext,
                            SecurityPolicyState.LOCKED,
                            "FCM Remote Lock Command ($commandId)"
                        )
                        PolicyEnforcementManager.enforceCurrentPolicy(applicationContext, "FCM Remote Lock")
                        dao.updateCommandStatus(commandId, "SUCCEEDED")
                        updateCloudCommandStatus(commandId, targetDeviceId, "SUCCEEDED")
                    }
                    "UNLOCK", "UNLOCK_DEVICE", "STOP_LOST_MODE" -> {
                        PolicyEnforcementManager.setPolicyState(
                            applicationContext,
                            SecurityPolicyState.NORMAL,
                            "FCM Remote Unlock Command ($commandId)"
                        )
                        PolicyEnforcementManager.enforceCurrentPolicy(applicationContext, "FCM Remote Unlock")
                        dao.updateCommandStatus(commandId, "SUCCEEDED")
                        updateCloudCommandStatus(commandId, targetDeviceId, "SUCCEEDED")
                    }
                    "SIREN", "ALARM" -> {
                        PolicyEnforcementManager.setPolicyState(
                            applicationContext,
                            SecurityPolicyState.LOCKED,
                            "FCM Siren Alarm Command ($commandId)"
                        )
                        PolicyEnforcementManager.enforceCurrentPolicy(applicationContext, "FCM Siren Alarm")
                        dao.updateCommandStatus(commandId, "SUCCEEDED")
                        updateCloudCommandStatus(commandId, targetDeviceId, "SUCCEEDED")
                    }
                    "LOCATE" -> {
                        startForegroundServiceIfNeeded()
                        dao.updateCommandStatus(commandId, "SUCCEEDED")
                        updateCloudCommandStatus(commandId, targetDeviceId, "SUCCEEDED")
                    }
                    "WIPE" -> {
                        if (PolicyEnforcementManager.isDpcActive(applicationContext)) {
                            updateCloudCommandStatus(commandId, targetDeviceId, "SUCCEEDED")
                            com.example.util.DeviceAdminHelper.wipeDeviceNow(applicationContext)
                            dao.updateCommandStatus(commandId, "SUCCEEDED")
                        } else {
                            dao.updateCommandStatus(commandId, "FAILED_PERMISSIONS")
                            updateCloudCommandStatus(commandId, targetDeviceId, "FAILED_PERMISSIONS")
                        }
                    }
                    else -> {
                        Log.w(TAG, "Unknown FCM Command Type: $commandType")
                        dao.updateCommandStatus(commandId, "UNKNOWN_TYPE")
                        updateCloudCommandStatus(commandId, targetDeviceId, "UNKNOWN_TYPE")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process FCM incoming command", e)
            }
        }
    }

    private fun startForegroundServiceIfNeeded() {
        try {
            val serviceIntent = Intent(applicationContext, SentinelForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SentinelForegroundService from FCM", e)
        }
    }

    private suspend fun updateCloudCommandStatus(commandId: String, targetDeviceId: String, status: String) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(ownerId)
                .collection("devices").document(targetDeviceId)
                .collection("commands").document(commandId)
                .update("status", status).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cloud command status", e)
        }
    }

    companion object {
        private const val TAG = "SentinelFCM"
        private const val PREFS_NAME = "sentinel_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}
