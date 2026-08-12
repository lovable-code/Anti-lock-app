import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

# Remove the delay simulations from triggerRemoteCommand
content = re.sub(r'\s*// WebSocket connection state transitions.*?repository\.updateCommandStatus\(commandId, "Executed"\)', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(content)
