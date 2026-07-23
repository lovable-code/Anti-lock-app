package com.example.data

import kotlinx.coroutines.flow.Flow

class SentinelRepository(private val dao: SentinelDao) {
    val allDevices: Flow<List<DeviceEntity>> = dao.getAllDevicesFlow()
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogsFlow()
    val allCommands: Flow<List<CommandEntity>> = dao.getAllCommandsFlow()

    suspend fun getDeviceById(id: String) = dao.getDeviceById(id)
    suspend fun insertDevice(device: DeviceEntity) = dao.insertDevice(device)
    suspend fun updateDevice(device: DeviceEntity) = dao.updateDevice(device)
    suspend fun deleteDeviceById(id: String) = dao.deleteDeviceById(id)

    suspend fun insertAuditLog(log: AuditLogEntity) = dao.insertAuditLog(log)
    suspend fun clearAllAuditLogs() = dao.clearAllAuditLogs()

    suspend fun insertCommand(command: CommandEntity) = dao.insertCommand(command)
    suspend fun updateCommand(command: CommandEntity) = dao.updateCommand(command)
    suspend fun updateCommandStatus(commandId: String, status: String) = dao.updateCommandStatus(commandId, status)
}
