with open('app/src/main/java/com/example/data/SentinelRepository.kt', 'r') as f:
    code = f.read()

target = """    suspend fun updateDeviceStatsAndLocation(
        id: String, lat: Double, lng: Double, battery: Int, isCharging: Boolean, network: String,
        storageTotal: Double, storageUsed: Double, ramTotal: Double, ramUsed: Double, healthScore: Int, lastActiveTime: Long
    ) {
        dao.updateDeviceStatsAndLocation(id, lat, lng, battery, isCharging, network, storageTotal, storageUsed, ramTotal, ramUsed, healthScore, lastActiveTime)
        // Optionally fetch and sync to firestore if needed, but not required for drift
    }"""

replacement = """    suspend fun updateDeviceStatsAndLocation(
        id: String, lat: Double, lng: Double, battery: Int, isCharging: Boolean, network: String,
        storageTotal: Double, storageUsed: Double, ramTotal: Double, ramUsed: Double, healthScore: Int, lastActiveTime: Long
    ) {
        dao.updateDeviceStatsAndLocation(id, lat, lng, battery, isCharging, network, storageTotal, storageUsed, ramTotal, ramUsed, healthScore, lastActiveTime)
        dao.getDeviceById(id)?.let { updated ->
            syncDeviceToFirestore(updated)
        }
    }"""

if target in code:
    code = code.replace(target, replacement)
    with open('app/src/main/java/com/example/data/SentinelRepository.kt', 'w') as f:
        f.write(code)
    print("Updated SentinelRepository.kt")
else:
    print("Target not found in SentinelRepository.kt")
