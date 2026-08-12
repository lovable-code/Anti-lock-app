import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

idx = content.find("requestPermissions(permissionsToRequest.toTypedArray(), 101)")
if idx != -1:
    overlay_check = """
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
"""
    # We will insert it just after admin_check
    idx2 = content.find("e.printStackTrace()\n                }\n            }", idx)
    if idx2 != -1:
        insert_idx = idx2 + len("e.printStackTrace()\n                }\n            }")
        content = content[:insert_idx] + "\n" + overlay_check + content[insert_idx:]
    else:
        # fallback
        pass
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
print("Fixed main overlay")
