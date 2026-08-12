with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

count = 0
last_count_1_line = 0
for i, line in enumerate(text.splitlines()):
    count += line.count('{')
    count -= line.count('}')
    if count == 1:
        last_count_1_line = i + 1

print(f"Last time count was 1 was at line {last_count_1_line}")
