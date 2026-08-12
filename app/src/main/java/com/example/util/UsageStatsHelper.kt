package com.example.util

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object UsageStatsHelper {
    private const val TAG = "UsageStatsHelper"

    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun getForegroundPackage(context: Context): String? {
        if (!isUsageAccessGranted(context)) return null

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 60,
            time
        )

        if (stats != null) {
            var latestStats: android.app.usage.UsageStats? = null
            for (usageStat in stats) {
                if (latestStats == null || usageStat.lastTimeUsed > latestStats.lastTimeUsed) {
                    latestStats = usageStat
                }
            }
            return latestStats?.packageName
        }
        return null
    }

    fun isHomeScreenForeground(context: Context): Boolean {
        val currentForeground = getForegroundPackage(context) ?: return false
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val homePackage = resolveInfo?.activityInfo?.packageName
        
        Log.d(TAG, "Foreground: $currentForeground, Home: $homePackage")
        // Also consider SystemUI as a potential bypass source (Recents/Notification shade)
        return currentForeground == homePackage || currentForeground == "com.android.systemui"
    }

    fun isRecentsForeground(context: Context): Boolean {
        val currentForeground = getForegroundPackage(context) ?: return false
        return currentForeground == "com.android.systemui" || currentForeground.contains("launcher") || currentForeground.contains("recents")
    }

    fun isSettingsForeground(context: Context): Boolean {
        val currentForeground = getForegroundPackage(context) ?: return false
        return currentForeground == "com.android.settings" || currentForeground.contains(".settings")
    }

    fun isAppForeground(context: Context): Boolean {
        val currentForeground = getForegroundPackage(context) ?: return false
        return currentForeground == context.packageName
    }
}
