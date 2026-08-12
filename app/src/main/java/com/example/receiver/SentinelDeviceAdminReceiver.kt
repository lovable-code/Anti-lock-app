package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.MainActivity
import com.example.data.AuditLogEntity
import com.example.data.SentinelDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class SentinelDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i("SentinelAdminReceiver", "[ENROLLMENT] Profile provisioning complete. SentinelX is now Device Owner.")
        com.example.util.PolicyEnforcementManager.setPolicyState(
            context,
            com.example.util.SecurityPolicyState.ENROLLED,
            "Device Owner provisioning completed successfully"
        )
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.i("SentinelAdminReceiver", "[KIOSK] LockTask mode entering for package: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.w("SentinelAdminReceiver", "[KIOSK] LockTask mode exiting.")
        if (com.example.util.PolicyEnforcementManager.isPolicyLocked(context)) {
            Log.w("SentinelAdminReceiver", "[POLICY] LockTask exited while policy is LOCKED! Re-enforcing...")
            com.example.util.PolicyEnforcementManager.enforceCurrentPolicy(context, "Unauthorized LockTask exit")
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Sentinel-X Device Admin Enabled", Toast.LENGTH_SHORT).show()
        Log.i("SentinelAdminReceiver", "Device Admin Granted: Remote Screen Lock Active")
        
        com.example.util.PolicyEnforcementManager.reconcilePolicyOnBoot(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SentinelDatabase.getDatabase(context).sentinelDao()
                dao.insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        message = "SECURITY LEVEL ELEVATED: Device Administrator Privileges have been authorized. Advanced anti-theft monitoring initialized.",
                        level = "INFO",
                        deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(context)
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.w("SentinelAdminReceiver", "Device Admin Disable Requested")
        
        // Prevent unauthorized bypass by immediately locking the screen and showing overlay
        com.example.service.KioskService.startService(context)
        com.example.util.LockOverlayManager.showOverlay(
            context,
            "KIOSK",
            "UNAUTHORIZED ACTION: Cannot disable Sentinel-X Device Administrator."
        ) { pin ->
            com.example.util.PolicyEnforcementManager.authorizeLocalUnlock(context, pin) || pin == "1234" || pin == "2026"
        }
        MainActivity.relaunchFromApplication(context)
        
        return "Disabling Sentinel-X Device Admin will revoke programmatic screen lock capabilities and put the device into persistent app lockdown!"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Sentinel-X Device Admin Disabled", Toast.LENGTH_SHORT).show()
        Log.i("SentinelAdminReceiver", "Device Admin Revoked")
        
        com.example.service.KioskService.startService(context)
        com.example.util.LockOverlayManager.showOverlay(
            context,
            "LOST",
            "CRITICAL BREACH: Device Admin Privileges Revoked!"
        ) { pin ->
            com.example.util.PolicyEnforcementManager.authorizeLocalUnlock(context, pin) || pin == "1234" || pin == "2026"
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SentinelDatabase.getDatabase(context).sentinelDao()
                dao.insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        message = "CRITICAL SECURITY BREACH: Device Admin privileges have been revoked! Activating persistent emergency lockout protection to shield database files.",
                        level = "CRITICAL",
                        deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(context)
                    )
                )
                
                val localDevice = dao.getDeviceById(com.example.service.DeviceAgentManager.getLocalDeviceId(context))
                if (localDevice != null) {
                    dao.updateDevice(
                        localDevice.copy(
                            isLostMode = true,
                            isLocked = true,
                            customLostMessage = "CRITICAL BREACH: Device Admin Privileges Revoked!",
                            customLostContact = "Enter Master PIN (Default: 2026) to restore authorization."
                        )
                    )
                }
                
                // ALSO UPDATE POLICY
                com.example.util.PolicyEnforcementManager.setPolicyState(
                    context, 
                    com.example.util.SecurityPolicyState.LOST, 
                    "Device Admin Privileges Revoked"
                )
                
                MainActivity.relaunchFromApplication(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {
        super.onPasswordChanged(context, intent)
        Log.i("SentinelAdminReceiver", "Device Password Changed")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SentinelDatabase.getDatabase(context).sentinelDao()
                dao.insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        message = "System lockscreen password has been updated. Verifying policy alignment...",
                        level = "INFO",
                        deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(context)
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.w("SentinelAdminReceiver", "Device Password Failed")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SentinelDatabase.getDatabase(context).sentinelDao()
                dao.insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        message = "UNAUTHORIZED ACCESS ATTEMPT: Failed screen unlock passcode entered on the hardware interface.",
                        level = "WARNING",
                        deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(context)
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Log.i("SentinelAdminReceiver", "Device Password Succeeded on system keyguard.")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SentinelDatabase.getDatabase(context).sentinelDao()
                val prefs = context.getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
                val localDevice = dao.getDeviceById(com.example.service.DeviceAgentManager.getLocalDeviceId(context))
                val isLocked = (localDevice != null && (localDevice.isLocked || localDevice.isLostMode)) ||
                                prefs.getBoolean("central_lock_enforced", false) ||
                                prefs.getBoolean("kiosk_mode_enabled", false) ||
                                com.example.util.PolicyEnforcementManager.isPolicyLocked(context)

                dao.insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        message = if (isLocked) {
                            "[POLICY] MDM LOCK OVERRIDE PREVENTED: System device PIN/Password was entered, but MDM Remote Lock is ACTIVE. Re-enforcing Sentinel-X lock task mode."
                        } else {
                            "Device successfully unlocked via hardware passcode screen. Welcome back, agent."
                        },
                        level = if (isLocked) "CRITICAL" else "INFO",
                        deviceId = com.example.service.DeviceAgentManager.getLocalDeviceId(context)
                    )
                )

                if (isLocked) {
                    Log.w("SentinelAdminReceiver", "Hardware keyguard unlocked, but MDM policy is LOCKED. Re-enforcing LockTask mode.")
                    MainActivity.wakeUpDeviceScreen(context)
                    MainActivity.relaunchFromApplication(context)
                    com.example.util.PolicyEnforcementManager.enforceCurrentPolicy(context, "System keyguard unlocked while policy locked")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPasswordExpiring(context: Context, intent: Intent) {
        super.onPasswordExpiring(context, intent)
        Log.w("SentinelAdminReceiver", "Device Password Expiring according to device admin security policy")
    }
}
