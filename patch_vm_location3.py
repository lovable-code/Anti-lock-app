import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

# Add missing methods if not exist
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
    fun triggerRemoteCommand(deviceId: String, type: String, map: Map<String, String> = emptyMap()) {}
"""
content = content.rsplit('}', 1)[0]
content += methods_to_add + "\n}"

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(content)
