import re

with open('app/src/main/java/com/example/service/SentinelForegroundService.kt', 'r') as f:
    content = f.read()

# Replace connectWebSocket and related WS functions with Firestore Listener
listener_code = """
    private fun startCommandListener() {
        val ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = DeviceAgentManager(applicationContext, repository).thisDeviceId
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(ownerId)
            .collection("devices").document(deviceId)
            .collection("commands")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                
                for (dc in snapshots.documentChanges) {
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = dc.document
                        val commandId = doc.id
                        val commandType = doc.getString("type") ?: continue
                        val payloadJson = doc.getString("payloadJson") ?: "{}"
                        
                        // Execute command
                        executeCommand(commandType, payloadJson)
                        
                        // Mark as executed
                        doc.reference.update("status", "Executed")
                    }
                }
            }
    }
    
    private fun executeCommand(commandType: String, payloadJson: String) {
        val deviceId = DeviceAgentManager(applicationContext, repository).thisDeviceId
        when (commandType) {
            "LOCK_DEVICE", "LOCK_COMMAND" -> {
                com.example.util.DeviceAdminHelper.lockDeviceScreenNow(applicationContext)
                com.example.util.PolicyEnforcementManager.enforceCurrentPolicy(applicationContext, "LOCK_DEVICE")
                sendLockCommand(applicationContext, "Remote LOCK_COMMAND executed", "+1-555-0199")
            }
            "UNLOCK_DEVICE" -> {
                com.example.util.PolicyEnforcementManager.enforceCurrentPolicy(applicationContext, "UNLOCK_DEVICE")
            }
            "WIPE_DEVICE" -> {
                com.example.util.DeviceAdminHelper.wipeDeviceNow(applicationContext)
            }
        }
    }
"""

content = re.sub(r'private fun connectWebSocket\(\) \{.*?(?=    private fun createNotificationChannel\(\))', listener_code, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/service/SentinelForegroundService.kt', 'w') as f:
    f.write(content)
