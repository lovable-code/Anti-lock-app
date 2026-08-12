import re

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    content = f.read()

# Only patch generateNewPairingCode inside SentinelViewModel to use Firebase
# This keeps all original methods intact.
new_pairing_code = """
    fun generateNewPairingCode(): String {
        val rawCode = UUID.randomUUID().toString().take(6).uppercase()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("pairingRequests").document(rawCode).set(mapOf("ownerId" to uid, "timestamp" to System.currentTimeMillis(), "status" to "pending"))
        }
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    message = "New enrollment pairing authorization QR/OTP generated: $rawCode (expires in 10 mins).",
                    level = "INFO",
                    deviceId = "system"
                )
            )
        }
        return rawCode
    }
"""

content = re.sub(r'    fun generateNewPairingCode\(\): String \{.*?return rawCode\n    \}', new_pairing_code.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(content)
