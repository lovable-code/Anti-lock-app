import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

# I see triggerRemoteCommand is completely missing the body for the secondary one or something?
# wait, triggerRemoteCommand has:
# fun triggerRemoteCommand(
#         deviceId: String,
#         commandType: String,
#         payload: Map<String, String> = emptyMap(),
#         policyVersion: Int = 1
#     ) { ... }

methods = """
    fun resetGeofenceLocation(deviceId: String = agentManager.thisDeviceId) {}
    fun toggleDeviceLockOnline(deviceId: String, lock: Boolean = true) {}
"""
content = content.rsplit('}', 1)[0]
content += methods + "\n}"

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(content)

