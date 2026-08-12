text = """package com.example.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.example.service.SentinelDeviceAdminReceiver

@Suppress("DEPRECATION")
object DeviceAdminHelper {
    fun getAdminComponentName(context: Context): ComponentName {
        return ComponentName(context, SentinelDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return devicePolicyManager.isAdminActive(getAdminComponentName(context))
    }

    fun lockDeviceScreenNow(context: Context): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isDeviceAdminActive(context)) {
                devicePolicyManager.lockNow()
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deactivateDeviceAdmin(context: Context): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isDeviceAdminActive(context)) {
                devicePolicyManager.removeActiveAdmin(getAdminComponentName(context))
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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
}
"""
with open('app/src/main/java/com/example/util/DeviceAdminHelper.kt', 'w') as f:
    f.write(text)
