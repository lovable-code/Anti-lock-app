with open('app/src/main/java/com/example/service/HeartbeatWorker.kt', 'r') as f:
    text = f.read()

text = text.replace("SentinelForegroundService.startService(context)", 
"""try {
                SentinelForegroundService.startService(context)
            } catch (e: Exception) {
                Log.w("HeartbeatWorker", "Could not start ForegroundService from background: ${e.message}")
            }""")

with open('app/src/main/java/com/example/service/HeartbeatWorker.kt', 'w') as f:
    f.write(text)
