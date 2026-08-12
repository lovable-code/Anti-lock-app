with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("fun runResiliencyDiagnostics() {", "fun runResiliencyDiagnostics(onComplete: () -> Unit = {}) {")
text = text.replace("_isDiagnosing.value = false\n        }", "_isDiagnosing.value = false\n            onComplete()\n        }")
text = text.replace("fun autoHealAndReinforceTunnel() {", "fun autoHealAndReinforceTunnel(onComplete: () -> Unit = {}) {")
text = text.replace("deviceId = \"system\"\n                )\n            )", "deviceId = \"system\"\n                )\n            )\n            onComplete()")

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(text)
