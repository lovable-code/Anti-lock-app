import sys

with open('app/src/main/java/com/example/ui/components/PermissionsRationaleDialog.kt', 'r') as f:
    content = f.read()

has_admin = "var isAdminActive by remember" in content

if not has_admin:
    idx = content.find("val runtimePermissionsGranted = permissionsState.allPermissionsGranted")
    if idx != -1:
        admin_state = """
    var isAdminActive by remember {
        mutableStateOf(
            (context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager)
                .isAdminActive(android.content.ComponentName(context, com.example.receiver.SentinelDeviceAdminReceiver::class.java))
        )
    }
"""
        content = content[:idx] + admin_state + content[idx:]

    idx2 = content.find("val allGranted = runtimePermissionsGranted && hasOverlayPermission")
    if idx2 != -1:
        content = content[:idx2] + "val allGranted = runtimePermissionsGranted && hasOverlayPermission && isAdminActive\n    " + content[idx2 + len("val allGranted = runtimePermissionsGranted && hasOverlayPermission"):]
    
    # Now find where to put the new card
    idx3 = content.find("// Privacy Commitment Note")
    if idx3 != -1:
        admin_card = """
                // Rationale Card 4: Device Admin
                PermissionRationaleCard(
                    title = "Device Administrator",
                    description = "Required to enforce device lock, wipe data on unauthorized access, and prevent app uninstallation by thieves.",
                    icon = Icons.Filled.Security,
                    isGranted = isAdminActive,
                    testTag = "admin_rationale_card"
                )
"""
        content = content[:idx3] + admin_card + content[idx3:]

    # Now add intent to launch device admin settings in the request button
    idx4 = content.find("if (!hasOverlayPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {")
    if idx4 != -1:
        admin_intent = """
                        if (!isAdminActive) {
                            val componentName = android.content.ComponentName(context, com.example.receiver.SentinelDeviceAdminReceiver::class.java)
                            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "SentinelX requires Device Admin access to enforce remote lock and wipe capabilities.")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
"""
        content = content[:idx4] + admin_intent + content[idx4:]
    
    with open('app/src/main/java/com/example/ui/components/PermissionsRationaleDialog.kt', 'w') as f:
        f.write(content)
print("Fixed admin dialog")
