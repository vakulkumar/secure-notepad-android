package com.securenotes.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BiometricAuthManager handles biometric authentication using BiometricPrompt API.
 * 
 * Features:
 * - Fingerprint/Face unlock support
 * - Falls back to device credentials (PIN/Password/Pattern) if biometrics fail
 * - Checks device capability for biometric hardware
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _authResult = Channel<AuthResult>(Channel.BUFFERED)
    val authResult: Flow<AuthResult> = _authResult.receiveAsFlow()

    /**
     * Checks if the device can use biometric authentication.
     */
    fun canAuthenticate(): BiometricCapability {
        val biometricManager = BiometricManager.from(context)
        
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.SECURITY_UPDATE_REQUIRED
            else -> BiometricCapability.UNKNOWN
        }
    }

    /**
     * Checks if biometric-only authentication is available.
     */
    fun canAuthenticateBiometricOnly(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == 
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the biometric prompt for authentication.
     * Falls back to device credentials if biometrics are not available.
     * 
     * @param activity The FragmentActivity to show the prompt
     * @param title Title for the prompt
     * @param subtitle Subtitle for the prompt
     * @param description Description for the prompt
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate",
        subtitle: String = "Verify your identity to access notes",
        description: String = "Use your fingerprint or PIN to unlock"
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _authResult.trySend(AuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val error = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> AuthError.USER_CANCELED
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> AuthError.LOCKOUT
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> AuthError.NO_BIOMETRICS
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> AuthError.NO_DEVICE_CREDENTIAL
                    else -> AuthError.UNKNOWN
                }
                _authResult.trySend(AuthResult.Error(error, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                _authResult.trySend(AuthResult.Failed)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            // Allow biometrics or device credential (PIN/Password/Pattern)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Represents biometric authentication capability of the device.
     */
    enum class BiometricCapability {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NOT_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNKNOWN
    }

    /**
     * Represents the result of an authentication attempt.
     */
    sealed class AuthResult {
        data object Success : AuthResult()
        data object Failed : AuthResult()
        data class Error(val error: AuthError, val message: String) : AuthResult()
    }

    /**
     * Authentication error types.
     */
    enum class AuthError {
        USER_CANCELED,
        LOCKOUT,
        NO_BIOMETRICS,
        NO_DEVICE_CREDENTIAL,
        UNKNOWN
    }
}
