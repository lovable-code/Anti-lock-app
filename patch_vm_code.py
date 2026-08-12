with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    code = f.read()

target = """fun generateNewPairingCode(): String {
        val rawCode = UUID.randomUUID().toString().take(6).uppercase()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("pairingRequests").document(rawCode).set(mapOf("ownerId" to uid, "timestamp" to System.currentTimeMillis(), "status" to "pending"))
        }"""

replacement = """fun generateNewPairingCode(): String {
        val rawCode = UUID.randomUUID().toString().take(6).uppercase()
        try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("pairingRequests").document(rawCode).set(mapOf("ownerId" to uid, "timestamp" to System.currentTimeMillis(), "status" to "pending"))
            }
        } catch (e: Exception) {
            Log.w("SentinelViewModel", "Could not sync pairing code to Firestore: ${e.message}")
        }"""

if target in code:
    code = code.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
        f.write(code)
    print("Successfully patched generateNewPairingCode")
else:
    print("Target not found in SentinelViewModel.kt")
