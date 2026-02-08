package com.securenotes.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PinAuthManager handles custom PIN authentication with duress support.
 * 
 * Features:
 * - User PIN verification
 * - Duress PIN detection (shows empty database)
 * - PBKDF2 hashing for PIN storage (migrated from SHA-256)
 */
@Singleton
class PinAuthManager @Inject constructor(
    private val securePreferences: SecurePreferences
) {

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8

        private const val PBKDF2_ITERATIONS = 100_000
        private const val PBKDF2_KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
        private const val HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val V2_PREFIX = "v2:"
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

        // Check duress PIN first
        val duressHash = securePreferences.duressPinHash
        if (duressHash != null && verifyHash(pin, duressHash)) {
            if (!duressHash.startsWith(V2_PREFIX)) {
                securePreferences.duressPinHash = hashPin(pin)
            }
            return PinResult.DuressTriggered
        }
        
        // Check user PIN
        val userHash = securePreferences.userPinHash
        if (userHash != null && verifyHash(pin, userHash)) {
            if (!userHash.startsWith(V2_PREFIX)) {
                securePreferences.userPinHash = hashPin(pin)
            }
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
        val userHash = securePreferences.userPinHash
        if (userHash != null && verifyHash(pin, userHash)) {
            return false
        }
        
        securePreferences.duressPinHash = hashPin(pin)
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
     * Hashes PIN using PBKDF2.
     */
    private fun hashPin(pin: String): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)

        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(HASH_ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded

        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }

        return "$V2_PREFIX$PBKDF2_ITERATIONS:$saltHex:$hashHex"
    }

    private fun verifyHash(pin: String, storedHash: String): Boolean {
        if (storedHash.startsWith(V2_PREFIX)) {
            try {
                val parts = storedHash.split(":")
                if (parts.size != 4) return false
                val iterations = parts[1].toInt()
                val saltHex = parts[2]
                val expectedHashHex = parts[3]

                val salt = hexToBytes(saltHex)
                val expectedHash = hexToBytes(expectedHashHex)

                val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PBKDF2_KEY_LENGTH)
                val factory = SecretKeyFactory.getInstance(HASH_ALGORITHM)
                val computedHash = factory.generateSecret(spec).encoded

                return MessageDigest.isEqual(computedHash, expectedHash)
            } catch (e: Exception) {
                return false
            }
        } else {
            val legacyHash = hashPinLegacy(pin)
            return storedHash.equals(legacyHash, ignoreCase = true)
        }
    }

    private fun hashPinLegacy(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
