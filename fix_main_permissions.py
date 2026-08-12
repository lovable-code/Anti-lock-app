import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

idx = content.find("requestPermissions(permissionsToRequest.toTypedArray(), 101)")
if idx != -1:
    admin_check = """
            if (!(getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager).isAdminActive(android.content.ComponentName(this, com.example.receiver.SentinelDeviceAdminReceiver::class.java))) {
                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, android.content.ComponentName(this@MainActivity, com.example.receiver.SentinelDeviceAdminReceiver::class.java))
                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "SentinelX requires Device Admin access to enforce remote lock and wipe capabilities.")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
"""
    content = content[:idx] + "requestPermissions(permissionsToRequest.toTypedArray(), 101)\n" + admin_check + content[idx + len("requestPermissions(permissionsToRequest.toTypedArray(), 101)"):]

    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
print("Fixed main permissions")
