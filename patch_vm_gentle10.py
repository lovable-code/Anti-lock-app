with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    # I added them starting at line ~1100
    if i > 1080 and "fun " in line and ("setTimedAutoLock" in line or "cancelTimedAutoLock" in line or "verifyDeviceConnection" in line or "setKioskModeEnabled" in line or "setGeofenceEnabled" in line or "setSafeZoneRadius" in line or "setSafeZoneToCurrentDeviceLocation" in line or "calculateDistanceMeters" in line or "simulateGeofenceBreach" in line or "resetGeofenceLocation" in line or "triggerRemoteCommand" in line or "unenrollDevice" in line or "toggleDeviceLockOnline" in line or "clearLogs" in line or "getLocalInstalledApplications" in line):
        if "{" in line and "}" in line and "viewModelScope" not in line:
             print(f"Skipping line {i+1}: {line.strip()}")
             continue
        if "}" in line and "emptyMap" in line: # triggerRemoteCommand at the end
             print(f"Skipping line {i+1}: {line.strip()}")
             continue
    
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.writelines(new_lines)

