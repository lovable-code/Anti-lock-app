import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

methods = """
    fun triggerRemoteCommand(deviceId: String, commandType: String, payload: Map<String, String> = emptyMap(), policyVersion: Int = 1) {}
"""
content = content.rsplit('}', 1)[0]
content += methods + "\n}"

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(content)

