package com.harsha.myapplication.ui.security

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * System fingerprint / face prompt used as the first way to unlock. Anything other than a successful scan
 * (Cancel / "Use PIN", back, too many tries, no hardware) simply leaves the custom PIN screen showing.
 *
 * Only used on Android 9+ (API 28): there the prompt is drawn by the system. Older versions go straight to the PIN.
 */
object BiometricUnlock {

    private var showing = false

    fun findActivity(context: Context): FragmentActivity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is FragmentActivity) return c
            c = c.baseContext
        }
        return null
    }

    fun isAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the system prompt. [onSuccess] runs only after a real match; every other outcome is silent. */
    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit) {
        if (showing || !isAvailable(activity)) return
        showing = true
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                showing = false
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                showing = false // cancelled, negative button, lockout...: the PIN screen is already underneath
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Ministry Assistant")
            .setSubtitle("Use your fingerprint")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText("Use PIN")
            .setConfirmationRequired(false)
            .build()
        runCatching {
            BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
        }.onFailure { showing = false }
    }
}
