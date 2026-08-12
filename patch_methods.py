import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

# Let's check for duplicate methods
import collections
methods = re.findall(r'fun\s+([a-zA-Z0-9_]+)\(', text)
counter = collections.Counter(methods)
for method, count in counter.items():
    if count > 1:
        print(f"Duplicate method: {method}")

