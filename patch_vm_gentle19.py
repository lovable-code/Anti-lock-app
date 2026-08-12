with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

count = 0
found_class = False
for i, line in enumerate(text.splitlines()):
    if "class SentinelViewModel" in line:
        found_class = True
    
    count += line.count('{')
    count -= line.count('}')
    
    if found_class and count == 0:
        print(f"Class SentinelViewModel CLOSED at line {i+1}: {line}")
        break
