with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if "fun generateNewPairingCode" in line and "rawCode" not in line and "return \"\"" in line:
        pass # ignore it, it's the duplicate! Wait, it was `fun generateNewPairingCode(): String { return "" }`
        continue
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.writelines(new_lines)

