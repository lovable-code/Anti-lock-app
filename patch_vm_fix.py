with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    lines = f.readlines()

out = []
for line in lines:
    if line.strip() == '// WebSocket connection state transitions':
        out.append('        }\n    }\n')
        continue
    if '// WebSocket connection state transitions' in line:
        pass # ignore
    out.append(line)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.writelines(out)
