with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

count = 0
in_block = False
start_block = 0
for i, line in enumerate(text.splitlines()):
    count += line.count('{')
    count -= line.count('}')
    
    if count >= 2 and not in_block:
        in_block = True
        start_block = i + 1
    elif count < 2 and in_block:
        in_block = False
        print(f"Block starting at {start_block} ended at {i+1}")
