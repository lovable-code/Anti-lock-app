import re
with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

# Fix runResiliencyDiagnostics
text = re.sub(
    r'(fun runResiliencyDiagnostics.*?_isDiagnosing\.value = false\n)(\s*)(})',
    r'\1\2    onComplete()\n\2\3',
    text, flags=re.DOTALL
)

# Fix autoHealAndReinforceTunnel
text = re.sub(
    r'(fun autoHealAndReinforceTunnel.*?deviceId = "system"\n\s*\)\n\s*\)\n)(\s*)(})',
    r'\1\2    onComplete()\n\2\3',
    text, flags=re.DOTALL
)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(text)
