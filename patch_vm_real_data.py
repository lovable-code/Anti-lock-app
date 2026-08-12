with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    code = f.read()

# 1. Replace seedDatabase with real local agent enrollment only
seed_start = code.find("            // Enroll local agent\n            agentManager.enrollOrUpdateLocalDeviceAgent(")
seed_end = code.find("            repository.insertAuditLog(\n                AuditLogEntity(\n                    timestamp = System.currentTimeMillis() - 1500000,")

if seed_start != -1 and seed_end != -1:
    # Find end of audit log block for workPhone
    real_seed_end = code.find("        }", seed_end)
    replacement_seed = """            // Enroll real local agent only (no mock devices)
            agentManager.enrollOrUpdateLocalDeviceAgent()
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "Secure local device enrollment completed. Real hardware agent active.",
                    level = "INFO",
                    deviceId = agentManager.thisDeviceId
                )
            )
        }"""
    # Replace from seed_start to real_seed_end
    old_block = code[seed_start:real_seed_end + 9] # include ending brace
    code = code.replace(old_block, replacement_seed)
    print("Cleaned database seed block")
else:
    print(f"Seed bounds not found: start={seed_start}, end={seed_end}")

# 2. Update startLocalAgentSyncAndDrift to use real GPS only without mock drift
drift_start = code.find("    private fun startLocalAgentSyncAndDrift() {")
drift_end = code.find("    // Geofencing Control API", drift_start)

if drift_start != -1 and drift_end != -1:
    real_sync_code = """    private fun startLocalAgentSyncAndDrift() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                try {
                    // Update local hardware device telemetry using real GPS location
                    val realDevice = agentManager.enrollOrUpdateLocalDeviceAgent()
                    evaluateGeofence(realDevice)
                } catch (e: Exception) {
                    Log.e("SentinelViewModel", "Error updating local device telemetry: ${e.message}")
                }
                delay(15000) // Update every 15 seconds
            }
        }
    }

    """
    code = code[:drift_start] + real_sync_code + code[drift_end:]
    print("Replaced startLocalAgentSyncAndDrift with real GPS tracking loop")

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(code)

