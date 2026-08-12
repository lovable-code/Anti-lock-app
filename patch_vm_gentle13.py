with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

methods_check = [
    "fun enrollNewDevice(",
    "fun getLocalInstalledApplications(",
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
    "fun clearLogs(",
    "fun resetGeofenceLocation("
]

for m in methods_check:
    if m not in text:
        print(f"MISSING: {m}")

