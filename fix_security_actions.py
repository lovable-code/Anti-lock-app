import sys

with open('app/src/main/java/com/example/ui/screens/SecurityActionsScreen.kt', 'r') as f:
    content = f.read()

# See if there's a banner for isAdminActive
if "if (!isAdminActive) {" not in content:
    idx = content.find("if (!hasOverlayPermission) {")
    if idx != -1:
        admin_banner = """
        // Device Admin Permission Banner if missing
        if (!isAdminActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DangerRed, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Admin Permission Warning",
                            tint = DangerRed,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DEVICE ADMIN REQUIRED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = DangerRed
                            )
                            Text(
                                text = "Mandatory for Lock, Wipe, and Uninstall Protection to function.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 14.sp
                            )
                        }
                        Button(
                            onClick = {
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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("request_admin_permission_btn")
                        ) {
                            Text("GRANT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
"""
        content = content[:idx] + admin_banner + content[idx:]
        with open('app/src/main/java/com/example/ui/screens/SecurityActionsScreen.kt', 'w') as f:
            f.write(content)
print("Fixed security actions")
