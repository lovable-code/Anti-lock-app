with open('app/src/main/java/com/example/util/DeviceAdminHelper.kt', 'r') as f:
    text = f.read()

stubs = """
    fun isDeviceOwnerApp(context: Context): Boolean = false
    fun isLockTaskPermitted(context: Context): Boolean = false
    fun setCameraDisabled(context: Context, disabled: Boolean): Boolean = false
    fun getRequestAdminIntent(context: Context): android.content.Intent {
        return android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponentName(context))
        }
    }
    fun setLockTaskPackages(context: Context, packages: Array<String>) {}
    fun setLockTaskFeatures(context: Context, features: Int) {}
    fun clearUserRestrictions(context: Context) {}
"""

text = text.replace("}", stubs + "\n}")

with open('app/src/main/java/com/example/util/DeviceAdminHelper.kt', 'w') as f:
    f.write(text)
