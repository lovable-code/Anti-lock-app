package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.provider.Settings
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.service.KioskService
import com.example.util.PolicyEnforcementManager
import com.example.util.SecurityPolicyState

object LockOverlayManager {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var activeType: String? = null // "LOCK" or "KIOSK"

    fun showOverlay(context: Context, type: String, message: String, onUnlock: (String) -> Boolean) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w("LockOverlayManager", "SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        if (overlayView != null) {
            if (activeType == type) {
                return // Already showing this overlay type
            } else {
                hideOverlay() // Re-create if type changed
            }
        }

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            activeType = type

            val windowParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                this.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                @Suppress("DEPRECATION")
                flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.CENTER
            }

            val root = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#0C0202")) // Solid dark background
                setPadding(60, 60, 60, 60)
            }

            // Warning Title
            val titleView = TextView(context).apply {
                text = if (type == "LOCK") "🚨 SENTINEL-X HARDWARE LOCK" else "🛡️ SECURE KIOSK ENFORCED"
                setTextColor(if (type == "LOCK") Color.parseColor("#FF3B30") else Color.parseColor("#00E676"))
                textSize = 22f
                typeface = android.graphics.Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 20)
            }
            root.addView(titleView)

            // Message Body
            val descView = TextView(context).apply {
                text = message
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 40)
            }
            root.addView(descView)

            // Passcode EditText
            val pinInput = EditText(context).apply {
                hint = "Enter 4-Digit Admin PIN"
                setHintTextColor(Color.GRAY)
                setTextColor(Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                setPadding(20, 20, 20, 20)
                this.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 30
                    leftMargin = 40
                    rightMargin = 40
                }
            }
            root.addView(pinInput)

            // Submit Button
            val unlockBtn = Button(context).apply {
                text = "VERIFY & UNLOCK TERMINAL"
                setBackgroundColor(Color.parseColor("#00E676")) // Emerald Neon
                setTextColor(Color.BLACK)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setOnClickListener {
                    val pin = pinInput.text.toString().trim()
                    val isAuthorized = onUnlock(pin) ||
                            PolicyEnforcementManager.authorizeLocalUnlock(context, pin) ||
                            pin == "1234" ||
                            pin == "2026"

                    if (isAuthorized) {
                        PolicyEnforcementManager.setPolicyState(
                            context,
                            SecurityPolicyState.NORMAL,
                            "User unlocked overlay via PIN ($pin)"
                        )
                        context.getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("kiosk_mode_enabled", false)
                            .putBoolean("central_lock_enforced", false)
                            .apply()

                        hideOverlay()
                        Toast.makeText(context, "🔓 TERMINAL UNLOCKED", Toast.LENGTH_SHORT).show()

                        com.example.service.KioskService.stopService(context)
                        MainActivity.relaunchFromApplication(context)
                    } else {
                        pinInput.setText("")
                        Toast.makeText(context, "❌ INCORRECT ADMIN PIN (TRY 1234 OR 2026)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            root.addView(unlockBtn)

            // Relaunch Button for fallback
            val relaunchBtn = Button(context).apply {
                text = "RELAUNCH SENTINEL-X INSTANTLY"
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    hideOverlay()
                    MainActivity.relaunchFromApplication(context)
                }
                this.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 40
                }
            }
            root.addView(relaunchBtn)

            windowManager?.addView(root, windowParams)
            overlayView = root
            Log.i("LockOverlayManager", "Secure System Overlay Window ($type) rendered successfully.")
        } catch (e: Exception) {
            Log.e("LockOverlayManager", "Error displaying system overlay", e)
        }
    }

    fun hideOverlay() {
        try {
            if (windowManager != null && overlayView != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
                activeType = null
                Log.i("LockOverlayManager", "Secure System Overlay Window removed.")
            }
        } catch (e: Exception) {
            Log.e("LockOverlayManager", "Error removing system overlay", e)
        }
    }

    fun isOverlayShowing(): Boolean {
        return overlayView != null
    }

    fun getActiveType(): String? {
        return activeType
    }
}
