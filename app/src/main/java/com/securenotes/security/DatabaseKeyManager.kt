package com.securenotes.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * DatabaseKeyManager handles the SQLCipher database encryption passphrase.
 * 
 * The passphrase is derived from the Android Keystore through CryptoManager,
 * ensuring the raw key is never stored in SharedPreferences or code.
 * 
 * Key Flow:
 * 1. CryptoManager generates/retrieves database key from Keystore
 * 2. Key is used to derive a consistent passphrase
 * 3. Passphrase is passed to SQLCipher for database encryption
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    private val cryptoManager: CryptoManager
) {
    
    /**
     * Gets the SQLCipher database passphrase.
     * The passphrase is derived from Android Keystore keys.
     * 
     * @return CharArray passphrase for SQLCipher
     */
    fun getDatabasePassphrase(): CharArray {
        val passphraseBytes = cryptoManager.getDatabasePassphrase()
        // Convert to hex string for SQLCipher compatibility
        return bytesToHex(passphraseBytes).toCharArray()
    }

    /**
     * Gets the passphrase as ByteArray for SQLCipher SupportFactory.
     */
    fun getDatabasePassphraseBytes(): ByteArray {
        return cryptoManager.getDatabasePassphrase()
    }

    /**
     * Converts byte array to hexadecimal string.
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt()
            result.append(hexChars[i shr 4 and 0x0F])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    /**
     * Invalidates the database key (used for panic wipe).
     * After this, the database cannot be decrypted.
     */
    fun invalidateKey() {
        cryptoManager.deleteAllKeys()
    }
}
