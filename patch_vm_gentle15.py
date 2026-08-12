import re
with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

count = 0
for i, line in enumerate(text.splitlines()):
    count += line.count('{')
    count -= line.count('}')
    if count == 0 and i > 50:
        print(f"Class closed at line {i+1}: {line}")

print(f"Final count: {count}")
