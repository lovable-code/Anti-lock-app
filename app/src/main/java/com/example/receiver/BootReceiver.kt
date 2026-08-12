package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.SentinelForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("BootReceiver", "[BOOT] BootReceiver triggered with action: $action")
        
        if (action == "com.example.ACTION_TIMED_AUTO_LOCK") {
            val deviceId = intent.getStringExtra("target_device_id") ?: com.example.service.DeviceAgentManager.getLocalDeviceId(context)
            Log.w("BootReceiver", "🚨 BACKGROUND TIMED AUTO-LOCK ALARM EXPIRED for device '$deviceId'!")
            
            val prefs = context.getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("central_lock_enforced", true).apply()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = com.example.data.SentinelDatabase.getDatabase(context).sentinelDao()
                    val repo = com.example.data.SentinelRepository(dao)
                    val dev = repo.getDeviceById(com.example.service.DeviceAgentManager.getLocalDeviceId(context))
                    if (dev != null) {
                        repo.updateDevice(dev.copy(isLocked = true, customLostMessage = "TIMED AUTO-LOCK EXPIRED • DEVICE LOCKED BY OWNER POLICY"))
                    }
                    repo.insertAuditLog(
                        com.example.data.AuditLogEntity(
                            timestamp = System.currentTimeMillis(),
                            message = "🚨 BACKGROUND TIMED AUTO-LOCK EXPIRED: Device auto-locked after scheduled timer limit!",
                            level = "WARNING",
                            deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(context)
                        )
                    )
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error updating device lock status in DB", e)
                }
            }

            try {
                com.example.util.PolicyEnforcementManager.setPolicyState(
                    context,
                    com.example.util.SecurityPolicyState.LOCKED,
                    "Timed Auto-Lock Expired"
                )
                com.example.util.PolicyEnforcementManager.enforceCurrentPolicy(context, "Timed Auto-Lock Expired")
                com.example.util.DeviceAdminHelper.lockDeviceScreenNow(context)
                com.example.MainActivity.relaunchFromApplication(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error enforcing policy on timed auto lock", e)
            }
            return
        }

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.example.ACTION_PING_SERVICE"
        ) {
            Log.i("BootReceiver", "[BOOT] Reconciling persistent DPC security policy and initializing service.")
            try {
                com.example.util.PolicyEnforcementManager.reconcilePolicyOnBoot(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "[BOOT] Error reconciling policy on boot", e)
            }
            
            // Use WorkManager for background starts to avoid ForegroundServiceStartNotAllowedException on Android 12+
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.service.HeartbeatWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
            
            // Still try direct start as fallback for BOOT_COMPLETED which is typically allowed
            SentinelForegroundService.startService(context)
        }
    }
}
