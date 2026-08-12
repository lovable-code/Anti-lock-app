with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

count = 0
found_brace = False
for i, line in enumerate(text.splitlines()):
    count += line.count('{')
    count -= line.count('}')
    if count > 0:
        found_brace = True
        
    if found_brace and count == 0:
        print(f"Class SentinelViewModel CLOSED at line {i+1}: {line}")
        break
