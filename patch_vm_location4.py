import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

# Add missing methods INSIDE the class
methods_to_add = """
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float { return 0f }
    fun setGeofenceEnabled(deviceId: String, enabled: Boolean) {}
    fun setSafeZoneRadius(deviceId: String, radius: Float) {}
    fun setSafeZoneToCurrentDeviceLocation(deviceId: String) {}
    fun simulateGeofenceBreach(deviceId: String) {}
    fun getLocalInstalledApplications(): List<com.example.data.AppInfo> { return emptyList() }
    fun verifyDeviceConnection(deviceId: String) {}
    fun cancelTimedAutoLock(deviceId: String) {}
    fun unenrollDevice(deviceId: String) {}
    fun toggleDeviceLockOnline(deviceId: String, lock: Boolean) {}
    fun setTimedAutoLock(deviceId: String, durationMs: Long) {}
    fun setKioskModeEnabled(enabled: Boolean) {}
    fun clearLogs() {}
"""

content = content.replace("fun generateNewPairingCode(): String {", "    fun generateNewPairingCode(): String {\n        val rawCode = java.util.UUID.randomUUID().toString().take(6).uppercase()\n        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid\n        if (uid != null) {\n            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection(\"pairingRequests\").document(rawCode).set(mapOf(\"ownerId\" to uid, \"timestamp\" to System.currentTimeMillis(), \"status\" to \"pending\"))\n        }\n        return rawCode\n    }\n\n" + methods_to_add + "\n\n    fun DUPLICATE_METHOD")

# then remove DUPLICATE_METHOD to just replace it correctly. Actually, let's just use `rsplit('}', 1)` again. 
