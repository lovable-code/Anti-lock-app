import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "// WebSocket connection state transitions" in line:
        skip = True
    
    if skip and 'repository.updateCommandStatus(commandId, "Executed")' in line:
        skip = False
        continue
        
    if not skip:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.writelines(new_lines)
