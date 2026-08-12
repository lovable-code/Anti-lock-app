import sys

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

idx = content.find("var showPermissionsDialog by remember { mutableStateOf(false) }")
if idx != -1:
    content = content[:idx] + """
    var showPermissionsDialog by remember { 
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) ||
            !(context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager).isAdminActive(android.content.ComponentName(context, com.example.receiver.SentinelDeviceAdminReceiver::class.java))
        ) 
    }
""" + content[idx + len("var showPermissionsDialog by remember { mutableStateOf(false) }"):]

    with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
        f.write(content)
print("Fixed dashboard permissions")
