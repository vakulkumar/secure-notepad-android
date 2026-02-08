package com.securenotes.core.security

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PinAuthManager handles custom PIN authentication with duress support.
 * 
 * Features:
 * - User PIN verification
 * - Duress PIN detection (shows empty database)
 * - SHA-256 hashing for PIN storage
 */
@Singleton
class PinAuthManager @Inject constructor(
    private val securePreferences: SecurePreferences
) {

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8
    }

    sealed class PinResult {
        data object Success : PinResult()
        data object DuressTriggered : PinResult()
        data object InvalidPin : PinResult()
        data object PinNotSet : PinResult()
    }

    /**
     * Verifies the entered PIN.
     */
    fun verifyPin(pin: String): PinResult {
        if (!securePreferences.isPinEnabled) {
            return PinResult.PinNotSet
        }

        val pinHash = hashPin(pin)
        
        // Check duress PIN first
        val duressHash = securePreferences.duressPinHash
        if (duressHash != null && pinHash == duressHash) {
            return PinResult.DuressTriggered
        }
        
        // Check user PIN
        val userHash = securePreferences.userPinHash
        if (userHash != null && pinHash == userHash) {
            return PinResult.Success
        }
        
        return PinResult.InvalidPin
    }

    /**
     * Sets the user's primary PIN.
     */
    fun setUserPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        
        securePreferences.userPinHash = hashPin(pin)
        securePreferences.isPinEnabled = true
        return true
    }

    /**
     * Sets the duress PIN (optional).
     */
    fun setDuressPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        
        // Ensure duress PIN is different from user PIN
        val duressHash = hashPin(pin)
        if (duressHash == securePreferences.userPinHash) {
            return false
        }
        
        securePreferences.duressPinHash = duressHash
        return true
    }

    /**
     * Removes the duress PIN.
     */
    fun removeDuressPin() {
        securePreferences.duressPinHash = null
    }

    /**
     * Disables PIN authentication entirely.
     */
    fun disablePin() {
        securePreferences.isPinEnabled = false
        securePreferences.userPinHash = null
        securePreferences.duressPinHash = null
    }

    /**
     * Checks if PIN authentication is enabled.
     */
    fun isPinEnabled(): Boolean = securePreferences.isPinEnabled

    /**
     * Checks if duress PIN is configured.
     */
    fun hasDuressPin(): Boolean = securePreferences.duressPinHash != null

    /**
     * Validates PIN format.
     */
    fun isValidPin(pin: String): Boolean {
        return pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }
    }

    /**
     * Hashes PIN using SHA-256.
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
