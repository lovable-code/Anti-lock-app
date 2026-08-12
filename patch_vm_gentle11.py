with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip_until = -1
for i, line in enumerate(lines):
    if i < skip_until:
        continue
    
    # I added them starting at line ~1080
    if i > 1070 and "fun " in line and ("setTimedAutoLock" in line or "cancelTimedAutoLock" in line or "verifyDeviceConnection" in line or "setKioskModeEnabled" in line or "clearLogs" in line):
        if "triggerRemoteCommand" in lines[i+1]:
            print(f"Skipping lines {i+1} to {i+3}")
            skip_until = i + 3
            continue
        elif "viewModelScope.launch" in lines[i+1]:
            print(f"Skipping lines {i+1} to {i+5}")
            skip_until = i + 5
            continue
        elif "_isKioskModeEnabled.value = enabled" in lines[i+1]:
            print(f"Skipping lines {i+1} to {i+3}")
            skip_until = i + 3
            continue
    
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.writelines(new_lines)

