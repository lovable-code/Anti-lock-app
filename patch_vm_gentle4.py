import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

# I see triggerRemoteCommand at the end with empty body. But there is ALREADY one triggerRemoteCommand in the class.
# That causes ambiguity. Let's remove the empty one.

content = content.replace("fun triggerRemoteCommand(deviceId: String, type: String, map: Map<String, String> = emptyMap()) {}", "")

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(content)

