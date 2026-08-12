with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

count = 0
for i, line in enumerate(text.splitlines()):
    count += line.count('{')
    count -= line.count('}')
    if 100 <= i+1 <= 140:
        print(f"L{i+1} [count={count}]: {line.strip()}")
