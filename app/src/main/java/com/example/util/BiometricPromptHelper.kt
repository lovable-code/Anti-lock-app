package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricPromptHelper {

    private const val TAG = "BiometricPromptHelper"

    fun findFragmentActivity(context: Context): FragmentActivity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val result = biometricManager.canAuthenticate(authenticators)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricStatus(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometrics / Device PIN Hardware Ready"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware on device (PIN fallback available)"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware currently unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometrics enrolled (PIN fallback available)"
            else -> "Biometric authentication status unknown"
        }
    }

    fun authenticate(
        context: Context,
        title: String = "Biometric Security Authorization",
        subtitle: String = "SentinelX Security Verification",
        description: String = "Scan fingerprint or face to authorize security operation",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = findFragmentActivity(context)
        if (activity == null) {
            Log.e(TAG, "FragmentActivity not found for BiometricPrompt")
            onError("Activity context error for biometric authentication")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.i(TAG, "Biometric Authentication Succeeded!")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(TAG, "Biometric Authentication Error [$errorCode]: $errString")
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric Authentication Failed (fingerprint/face mismatch)")
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    com.example.MainActivity.isBiometricPromptActive = false
                    callback.onAuthenticationSucceeded(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    com.example.MainActivity.isBiometricPromptActive = false
                    callback.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    // Don't set false here as it might retry or just fail one attempt
                    callback.onAuthenticationFailed()
                }
            })

            com.example.MainActivity.isBiometricPromptActive = true
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            com.example.MainActivity.isBiometricPromptActive = false
            Log.e(TAG, "Failed to launch BiometricPrompt", e)
            onError(e.localizedMessage ?: "Biometric prompt initialization failed")
        }
    }
}
