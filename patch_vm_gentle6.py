import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

# Make double sure the correct methods are present for compiling
# The problem might be the class is missing the correct signature, or maybe `triggerRemoteCommand` was deleted?

methods_check = [
    "fun triggerRemoteCommand(deviceId: String, commandType: String", # Was in the original 
    "fun enrollNewDevice(deviceName: String",
    "fun getLocalInstalledApplications()",
    "fun calculateDistanceMeters(",
    "fun setGeofenceEnabled(",
    "fun setSafeZoneRadius(",
    "fun setSafeZoneToCurrentDeviceLocation(",
    "fun simulateGeofenceBreach(",
    "fun verifyDeviceConnection(",
    "fun cancelTimedAutoLock(",
    "fun unenrollDevice(",
    "fun toggleDeviceLockOnline(",
    "fun setTimedAutoLock(",
    "fun setKioskModeEnabled(",
    "fun clearLogs()",
    "fun resetGeofenceLocation("
]

for m in methods_check:
    if m not in content:
        print(f"MISSING: {m}")

