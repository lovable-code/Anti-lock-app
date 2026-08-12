with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

count = 0
for i, line in enumerate(text.splitlines()):
    count += line.count('{')
    count -= line.count('}')

print(f"Final count: {count}")
