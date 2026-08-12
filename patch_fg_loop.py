with open('app/src/main/java/com/example/service/SentinelForegroundService.kt', 'r') as f:
    code = f.read()

target = """    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (true) {
                // Background monitoring loop
                kotlinx.coroutines.delay(10000)
            }
        }
    }"""

replacement = """    private fun startMonitoringLoop() {
        serviceScope.launch {
            val agentManager = DeviceAgentManager(applicationContext, repository)
            while (true) {
                try {
                    val updatedDevice = agentManager.enrollOrUpdateLocalDeviceAgent()
                    Log.d(TAG, "ForegroundService GPS Telemetry Updated: Lat ${updatedDevice.latitude}, Lng ${updatedDevice.longitude}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in GPS telemetry background loop: ${e.message}")
                }
                kotlinx.coroutines.delay(15000)
            }
        }
    }"""

if target in code:
    code = code.replace(target, replacement)
    with open('app/src/main/java/com/example/service/SentinelForegroundService.kt', 'w') as f:
        f.write(code)
    print("Updated startMonitoringLoop in SentinelForegroundService.kt")
else:
    print("Target not found in SentinelForegroundService.kt")
