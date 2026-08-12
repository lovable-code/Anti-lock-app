package com.example.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.receiver.SentinelDeviceAdminReceiver

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

    fun isDeviceOwnerApp(context: Context): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }
    fun isLockTaskPermitted(context: Context): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return devicePolicyManager.isLockTaskPermitted(context.packageName)
    }
    fun setCameraDisabled(context: Context, disabled: Boolean): Pair<Boolean, String> {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isDeviceAdminActive(context)) {
                devicePolicyManager.setCameraDisabled(getAdminComponentName(context), disabled)
                Pair(true, "Success")
            } else {
                Pair(false, "Device Admin is not active.")
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Pair(false, "Requires Device Owner on Android 11+")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: "Unknown error")
        }
    }
    fun getRequestAdminIntent(context: Context): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponentName(context))
        }
    }
    fun setLockTaskPackages(context: Context, packages: Array<String>) {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isDeviceAdminActive(context) && isDeviceOwnerApp(context)) {
                devicePolicyManager.setLockTaskPackages(getAdminComponentName(context), packages)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun setLockTaskFeatures(context: Context, features: Int) {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isDeviceAdminActive(context) && isDeviceOwnerApp(context)) {
                devicePolicyManager.setLockTaskFeatures(getAdminComponentName(context), features)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun clearUserRestrictions(context: Context) {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isDeviceAdminActive(context) && isDeviceOwnerApp(context)) {
                devicePolicyManager.clearUserRestriction(getAdminComponentName(context), android.os.UserManager.DISALLOW_FACTORY_RESET)
                devicePolicyManager.clearUserRestriction(getAdminComponentName(context), android.os.UserManager.DISALLOW_SAFE_BOOT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
