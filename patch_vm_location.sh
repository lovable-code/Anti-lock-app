cat << 'INNER_EOF' > app/src/main/java/com/example/ui/SentinelViewModel_location_fix.py
import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("fun getLocalInstalledApplications", "fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float { return 0f }\n    fun setGeofenceEnabled(deviceId: String, enabled: Boolean) {}\n    fun setSafeZoneRadius(deviceId: String, radius: Float) {}\n    fun setSafeZoneToCurrentDeviceLocation(deviceId: String) {}\n    fun simulateGeofenceBreach(deviceId: String) {}\n    fun getLocalInstalledApplications")

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(content)
INNER_EOF
python3 app/src/main/java/com/example/ui/SentinelViewModel_location_fix.py
