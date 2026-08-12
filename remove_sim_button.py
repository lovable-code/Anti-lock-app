import sys

with open('app/src/main/java/com/example/ui/screens/LocationScreen.kt', 'r') as f:
    lines = f.readlines()

start = -1
end = -1
for i, line in enumerate(lines):
    if "Button(" in line and "onClick = {" in lines[i+1] and "activeDevice?.let { dev ->" in lines[i+2] and "if (geofenceBreached)" in lines[i+3]:
        start = i
        brace_count = 0
        for j in range(i, len(lines)):
            brace_count += lines[j].count('{')
            brace_count -= lines[j].count('}')
            if brace_count == 0 and "}" in lines[j]:
                end = j
                break
        break

if start != -1 and end != -1:
    with open('app/src/main/java/com/example/ui/screens/LocationScreen.kt', 'w') as f:
        f.writelines(lines[:start])
        f.writelines(lines[end+1:])
    print("Removed simulation button.")
else:
    print("Could not find button.")
