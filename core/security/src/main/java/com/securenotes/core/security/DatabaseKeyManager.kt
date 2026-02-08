package com.securenotes.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * DatabaseKeyManager handles the SQLCipher database encryption passphrase.
 * 
 * The passphrase is derived from the Android Keystore through CryptoManager,
 * ensuring the raw key is never stored in SharedPreferences or code.
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    private val cryptoManager: CryptoManager
) {
    
    /**
     * Gets the SQLCipher database passphrase.
     * The passphrase is derived from Android Keystore keys.
     */
    fun getDatabasePassphrase(): CharArray {
        val passphraseBytes = cryptoManager.getDatabasePassphrase()
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
     */
    fun invalidateKey() {
        cryptoManager.deleteAllKeys()
    }
}
