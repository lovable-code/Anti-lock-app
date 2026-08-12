with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

count = 0
found = False
for i, line in enumerate(text.splitlines()):
    if "class SentinelViewModel" in line:
        found = True
    if found:
        count += line.count('{')
        count -= line.count('}')
        if count == 0 and i > 40:
            print(f"Class SentinelViewModel CLOSED at line {i+1}: {line.strip()}")
            break
