with open('app/src/main/java/com/example/service/SentinelForegroundService.kt', 'r') as f:
    text = f.read()

target_block = """    private fun startCommandListener() {
        val ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = DeviceAgentManager(applicationContext, repository).thisDeviceId

        com.google.firebase.firestore.FirebaseFirestore.getInstance()"""

replacement_block = """    private fun startCommandListener() {
        try {
            val ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            val deviceId = DeviceAgentManager(applicationContext, repository).thisDeviceId

            com.google.firebase.firestore.FirebaseFirestore.getInstance()"""

text = text.replace(target_block, replacement_block)

# Add closing catch at end of startCommandListener if needed or inspect
with open('app/src/main/java/com/example/service/SentinelForegroundService.kt', 'w') as f:
    f.write(text)
