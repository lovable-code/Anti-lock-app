with open('app/src/main/java/com/example/util/DeviceAdminHelper.kt', 'r') as f:
    text = f.read()

# Just replace everything after the first `wipeDeviceNow` definition up to the end with a single clean copy
import re
text = re.sub(r'    fun wipeDeviceNow.*', '', text, flags=re.DOTALL)

missing_methods = """
    fun wipeDeviceNow(context: Context): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            if (isDeviceAdminActive(context)) {
                devicePolicyManager.wipeData(0)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
"""

with open('app/src/main/java/com/example/util/DeviceAdminHelper.kt', 'w') as f:
    f.write(text.strip() + "\n" + missing_methods)
