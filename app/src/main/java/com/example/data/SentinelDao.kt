package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SentinelDao {
    // Devices
    @Query("SELECT * FROM devices ORDER BY lastActiveTime DESC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDeviceById(id: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: String)

    @Query("""
        UPDATE devices SET 
            latitude = :lat, 
            longitude = :lng, 
            batteryPercentage = :battery, 
            isCharging = :isCharging,
            networkStatus = :network,
            storageTotalGb = :storageTotal,
            storageUsedGb = :storageUsed,
            ramTotalGb = :ramTotal,
            ramUsedGb = :ramUsed,
            healthScore = :healthScore,
            lastActiveTime = :lastActiveTime
        WHERE id = :id
    """)
    suspend fun updateDeviceStatsAndLocation(
        id: String,
        lat: Double,
        lng: Double,
        battery: Int,
        isCharging: Boolean,
        network: String,
        storageTotal: Double,
        storageUsed: Double,
        ramTotal: Double,
        ramUsed: Double,
        healthScore: Int,
        lastActiveTime: Long
    )

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllAuditLogs()

    // Commands
    @Query("SELECT * FROM commands ORDER BY timestamp DESC")
    fun getAllCommandsFlow(): Flow<List<CommandEntity>>

    @Query("SELECT * FROM commands WHERE commandId = :commandId")
    suspend fun getCommandById(commandId: String): CommandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandEntity)

    @Update
    suspend fun updateCommand(command: CommandEntity)

    @Query("UPDATE commands SET status = :status WHERE commandId = :commandId")
    suspend fun updateCommandStatus(commandId: String, status: String)

    @Query("SELECT * FROM commands WHERE status = 'Pending' ORDER BY timestamp ASC")
    suspend fun getPendingCommands(): List<CommandEntity>

    @Query("SELECT * FROM commands WHERE status = 'Pending' AND targetDeviceId = :deviceId ORDER BY timestamp ASC")
    suspend fun getPendingCommandsForDevice(deviceId: String): List<CommandEntity>

    // Master PIN
    @Query("SELECT * FROM master_pin LIMIT 1")
    suspend fun getMasterPin(): MasterPinEntity?

    @Query("SELECT * FROM master_pin LIMIT 1")
    fun getMasterPinFlow(): Flow<MasterPinEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterPin(masterPin: MasterPinEntity)
}
