cat << 'INNER_EOF' > app/src/main/java/com/example/ui/SentinelViewModel_location_fix2.py
import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

# Replace multiple generateNewPairingCode
content = re.sub(r'fun generateNewPairingCode.*?return rawCode\n    }', 'fun generateNewPairingCode(): String { return "" }', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(content)
INNER_EOF
python3 app/src/main/java/com/example/ui/SentinelViewModel_location_fix2.py
