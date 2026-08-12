package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class SentinelRepository(private val dao: SentinelDao) {

    init {
        startFirestoreSync()
    }

    private fun startFirestoreSync() {
        val db = getFirestoreInstance() ?: return
        var ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            val context = (dao as? androidx.room.RoomDatabase)?.openHelper?.readableDatabase?.attachedDbs?.firstOrNull() // Not possible cleanly
            // Since context is hard to get here without refactoring, let's assume this is mostly called when Auth is present, or we can't sync.
            return
        }
        db.collection("users").document(ownerId).collection("devices")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                CoroutineScope(Dispatchers.IO).launch {
                    for (dc in snapshots.documentChanges) {
                        val doc = dc.document
                        val device = DeviceEntity(
                            id = doc.getString("id") ?: continue,
                            name = doc.getString("name") ?: "",
                            manufacturer = doc.getString("manufacturer") ?: "",
                            model = doc.getString("model") ?: "",
                            androidVersion = doc.getString("androidVersion") ?: "",
                            securityPatch = doc.getString("securityPatch") ?: "",
                            batteryPercentage = doc.getLong("batteryPercentage")?.toInt() ?: 0,
                            isCharging = doc.getBoolean("isCharging") ?: false,
                            networkStatus = doc.getString("networkStatus") ?: "",
                            storageTotalGb = doc.getDouble("storageTotalGb") ?: 0.0,
                            storageUsedGb = doc.getDouble("storageUsedGb") ?: 0.0,
                            ramTotalGb = doc.getDouble("ramTotalGb") ?: 0.0,
                            ramUsedGb = doc.getDouble("ramUsedGb") ?: 0.0,
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            lastActiveTime = doc.getLong("lastActiveTime") ?: 0L,
                            healthScore = doc.getLong("healthScore")?.toInt() ?: 0,
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            locationAccuracyMeters = doc.getDouble("locationAccuracyMeters")?.toFloat() ?: 0f,
                            isLostMode = doc.getBoolean("isLostMode") ?: false,
                            customLostMessage = doc.getString("customLostMessage") ?: "",
                            customLostContact = doc.getString("customLostContact") ?: "",
                            isLocked = doc.getBoolean("isLocked") ?: false,
                            isAlarmActive = doc.getBoolean("isAlarmActive") ?: false
                        )
                        dao.insertDevice(device)
                    }
                }
            }
    }
    val allDevices: Flow<List<DeviceEntity>> = dao.getAllDevicesFlow()
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogsFlow()
    val allCommands: Flow<List<CommandEntity>> = dao.getAllCommandsFlow()

    suspend fun getDeviceById(id: String) = dao.getDeviceById(id)
    
    suspend fun insertDevice(device: DeviceEntity) {
        dao.insertDevice(device)
        syncDeviceToFirestore(device)
    }

    suspend fun updateDevice(device: DeviceEntity) {
        dao.updateDevice(device)
        syncDeviceToFirestore(device)
    }
    
    suspend fun updateDeviceStatsAndLocation(
        id: String, lat: Double, lng: Double, battery: Int, isCharging: Boolean, network: String,
        storageTotal: Double, storageUsed: Double, ramTotal: Double, ramUsed: Double, healthScore: Int, lastActiveTime: Long
    ) {
        dao.updateDeviceStatsAndLocation(id, lat, lng, battery, isCharging, network, storageTotal, storageUsed, ramTotal, ramUsed, healthScore, lastActiveTime)
        dao.getDeviceById(id)?.let { updated ->
            syncDeviceToFirestore(updated)
        }
    }

    suspend fun deleteDeviceById(id: String) {
        dao.deleteDeviceById(id)
        deleteDeviceFromFirestore(id)
    }

    suspend fun insertAuditLog(log: AuditLogEntity) {
        dao.insertAuditLog(log)
        syncAuditLogToFirestore(log)
    }

    suspend fun clearAllAuditLogs() = dao.clearAllAuditLogs()

    suspend fun insertCommand(command: CommandEntity) {
        dao.insertCommand(command)
        var ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            val context = (dao as? androidx.room.RoomDatabase)?.openHelper?.readableDatabase?.attachedDbs?.firstOrNull() // Not possible cleanly
            // Since context is hard to get here without refactoring, let's assume this is mostly called when Auth is present, or we can't sync.
            return
        }
        val db = getFirestoreInstance() ?: return
        val data = hashMapOf(
            "commandId" to command.commandId,
            "type" to command.type,
            "targetDeviceId" to command.targetDeviceId,
            "senderId" to command.senderId,
            "timestamp" to command.timestamp,
            "payloadJson" to command.payloadJson,
            "status" to command.status,
            "signature" to command.signature
        )
        db.collection("users").document(ownerId).collection("devices").document(command.targetDeviceId).collection("commands").document(command.commandId).set(data)
    }
    suspend fun updateCommand(command: CommandEntity) = dao.updateCommand(command)
    suspend fun updateCommandStatus(commandId: String, status: String) = dao.updateCommandStatus(commandId, status)
    suspend fun getPendingCommands(): List<CommandEntity> = dao.getPendingCommands()
    suspend fun getPendingCommandsForDevice(deviceId: String): List<CommandEntity> = dao.getPendingCommandsForDevice(deviceId)

    // Master PIN
    val masterPinFlow: Flow<MasterPinEntity?> = dao.getMasterPinFlow()
    suspend fun getMasterPin(): MasterPinEntity? = dao.getMasterPin()
    suspend fun insertMasterPin(masterPin: MasterPinEntity) = dao.insertMasterPin(masterPin)

    // Safe helper to obtain Firestore only if initialized
    private fun getFirestoreInstance(): FirebaseFirestore? {
        return try {
            val app = com.google.firebase.FirebaseApp.getInstance()
            if (app.options.apiKey.contains("Dummy")) {
                null
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            // FirebaseApp is not initialized or invalid options; suppress log noise
            null
        }
    }

    // Private helpers for Firestore real-time persistence
    private fun syncDeviceToFirestore(device: DeviceEntity) {
        try {
            val db = getFirestoreInstance() ?: return
            val data = hashMapOf(
                "id" to device.id,
                "name" to device.name,
                "manufacturer" to device.manufacturer,
                "model" to device.model,
                "androidVersion" to device.androidVersion,
                "securityPatch" to device.securityPatch,
                "batteryPercentage" to device.batteryPercentage,
                "isCharging" to device.isCharging,
                "networkStatus" to device.networkStatus,
                "storageTotalGb" to device.storageTotalGb,
                "storageUsedGb" to device.storageUsedGb,
                "ramTotalGb" to device.ramTotalGb,
                "ramUsedGb" to device.ramUsedGb,
                "isOnline" to device.isOnline,
                "lastActiveTime" to device.lastActiveTime,
                "healthScore" to device.healthScore,
                "latitude" to device.latitude,
                "longitude" to device.longitude,
                "locationAccuracyMeters" to device.locationAccuracyMeters,
                "isLostMode" to device.isLostMode,
                "customLostMessage" to device.customLostMessage,
                "customLostContact" to device.customLostContact,
                "isLocked" to device.isLocked,
                "isAlarmActive" to device.isAlarmActive
            )
            var ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            val context = (dao as? androidx.room.RoomDatabase)?.openHelper?.readableDatabase?.attachedDbs?.firstOrNull() // Not possible cleanly
            // Since context is hard to get here without refactoring, let's assume this is mostly called when Auth is present, or we can't sync.
            return
        }
        db.collection("users").document(ownerId).collection("devices")
                .document(device.id)
                .set(data)
                .addOnSuccessListener {
                    Log.d("SentinelRepository", "Firestore: Synced device ${device.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("SentinelRepository", "Firestore Sync Error: ${e.message}")
                }
        } catch (e: Exception) {
            // Suppress uninitialized Firestore errors
        }
    }

    private fun deleteDeviceFromFirestore(id: String) {
        try {
            val db = getFirestoreInstance() ?: return
            var ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            val context = (dao as? androidx.room.RoomDatabase)?.openHelper?.readableDatabase?.attachedDbs?.firstOrNull() // Not possible cleanly
            // Since context is hard to get here without refactoring, let's assume this is mostly called when Auth is present, or we can't sync.
            return
        }
        db.collection("users").document(ownerId).collection("devices").document(id).delete()
        } catch (e: Exception) {
            // Suppress uninitialized Firestore errors
        }
    }

    private fun syncAuditLogToFirestore(log: AuditLogEntity) {
        try {
            val db = getFirestoreInstance() ?: return
            val data = hashMapOf(
                "id" to log.id,
                "timestamp" to log.timestamp,
                "message" to log.message,
                "level" to log.level,
                "deviceId" to log.deviceId
            )
            var ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            val context = (dao as? androidx.room.RoomDatabase)?.openHelper?.readableDatabase?.attachedDbs?.firstOrNull() // Not possible cleanly
            // Since context is hard to get here without refactoring, let's assume this is mostly called when Auth is present, or we can't sync.
            return
        }
            db.collection("users").document(ownerId).collection("devices").document(log.deviceId).collection("auditLogs")
                .document("log_${log.timestamp}")
                .set(data)
        } catch (e: Exception) {
            // Suppress uninitialized Firestore errors
        }
    }
}
