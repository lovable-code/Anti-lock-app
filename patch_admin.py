with open('app/src/main/java/com/example/util/DeviceAdminHelper.kt', 'r') as f:
    text = f.read()

missing_methods = """
    fun wipeDeviceNow(context: Context): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isDeviceAdminActive(context)) {
                devicePolicyManager.wipeData(0)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
"""

text = text.replace("}", missing_methods + "\n}")

with open('app/src/main/java/com/example/util/DeviceAdminHelper.kt', 'w') as f:
    f.write(text)
