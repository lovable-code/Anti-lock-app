import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

methods = """
    fun resetGeofenceLocation(deviceId: String) {}
    fun toggleDeviceLockOnline(deviceId: String) {}
"""

if "fun resetGeofenceLocation" not in content:
    content = content.rsplit('}', 1)[0]
    content += methods + "\n}"
    
    with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
        f.write(content)

