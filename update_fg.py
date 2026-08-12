with open('app/src/main/java/com/example/service/SentinelForegroundService.kt', 'r') as f:
    code = f.read()

target = """    private fun startCommandListener() {
        val ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = DeviceAgentManager(applicationContext, repository).thisDeviceId

        com.google.firebase.firestore.FirebaseFirestore.getInstance()"""

replacement = """    private fun startCommandListener() {
        try {
            val ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            val deviceId = DeviceAgentManager(applicationContext, repository).thisDeviceId

            com.google.firebase.firestore.FirebaseFirestore.getInstance()"""

if target in code:
    code = code.replace(target, replacement)
    # Find end of startCommandListener and wrap with catch
    # Let's rewrite startCommandListener completely

start_idx = code.find("private fun startCommandListener()")
end_idx = code.find("private fun executeCommand(", start_idx)

new_listener = """private fun startCommandListener() {
        try {
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
                            val commandType = doc.getString("type") ?: continue
                            val payloadJson = doc.getString("payloadJson") ?: "{}"

                            // Execute command
                            executeCommand(commandType, payloadJson)

                            // Mark as executed
                            doc.reference.update("status", "Executed")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Command listener initialization skipped: ${e.message}")
        }
    }

    """

if start_idx != -1 and end_idx != -1:
    code = code[:start_idx] + new_listener + code[end_idx:]
    with open('app/src/main/java/com/example/service/SentinelForegroundService.kt', 'w') as f:
        f.write(code)
    print("Successfully replaced startCommandListener")
else:
    print(f"Indices: start={start_idx}, end={end_idx}")
