package com.securenotes.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * CryptoManager handles all cryptographic operations using Android Keystore.
 * 
 * Security Features:
 * - AES-256-GCM encryption (authenticated encryption)
 * - Hardware-backed key storage when available
 * - Keys never leave the Keystore
 * 
 * Encryption Key Flow:
 * 1. Master key generated/retrieved from Android Keystore
 * 2. Master key used to encrypt/decrypt note content
 * 3. Database passphrase stored in EncryptedSharedPreferences (encrypted once, reused)
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "secure_notes_master_key"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        private const val GCM_TAG_LENGTH = 128
        
        private const val PREFS_NAME = "secure_notes_db_prefs"
        private const val PREF_DB_PASSPHRASE = "db_passphrase_encrypted"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     */
    fun encrypt(plainText: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText)
        
        return EncryptedData(iv, cipherText)
    }

    /**
     * Decrypts ciphertext using AES-256-GCM.
     */
    fun decrypt(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)
        
        return cipher.doFinal(encryptedData.cipherText)
    }

    /**
     * Encrypts a string and returns Base64-encoded result.
     */
    fun encryptString(plainText: String): String {
        val encrypted = encrypt(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + encrypted.iv.size + encrypted.cipherText.size)
        combined[0] = encrypted.iv.size.toByte()
        System.arraycopy(encrypted.iv, 0, combined, 1, encrypted.iv.size)
        System.arraycopy(encrypted.cipherText, 0, combined, 1 + encrypted.iv.size, encrypted.cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts a Base64-encoded encrypted string.
     */
    fun decryptString(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        val ivLength = combined[0].toInt() and 0xFF
        val iv = combined.copyOfRange(1, 1 + ivLength)
        val cipherText = combined.copyOfRange(1 + ivLength, combined.size)
        
        val decrypted = decrypt(EncryptedData(iv, cipherText))
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * Gets or creates the master encryption key in Android Keystore.
     */
    private fun getOrCreateMasterKey(): SecretKey {
        val existingKey = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createMasterKey()
    }

    private fun createMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER)
        
        val keyGenSpec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build()
        
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Generates the database encryption passphrase.
     * This passphrase is generated ONCE and stored encrypted in SharedPreferences.
     * This ensures the same passphrase is used across app launches.
     * 
     * @return ByteArray passphrase for SQLCipher (32 bytes = 256 bits)
     */
    fun getDatabasePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val existingEncrypted = prefs.getString(PREF_DB_PASSPHRASE, null)
        
        if (existingEncrypted != null) {
            // Decrypt and return existing passphrase
            return try {
                Base64.decode(decryptString(existingEncrypted), Base64.NO_WRAP)
            } catch (e: Exception) {
                // If decryption fails, generate new passphrase
                generateAndStorePassphrase(prefs)
            }
        } else {
            // Generate new passphrase
            return generateAndStorePassphrase(prefs)
        }
    }
    
    private fun generateAndStorePassphrase(prefs: android.content.SharedPreferences): ByteArray {
        // Generate random 32-byte passphrase
        val passphrase = ByteArray(32)
        java.security.SecureRandom().nextBytes(passphrase)
        
        // Encrypt and store
        val passphraseBase64 = Base64.encodeToString(passphrase, Base64.NO_WRAP)
        val encrypted = encryptString(passphraseBase64)
        prefs.edit().putString(PREF_DB_PASSPHRASE, encrypted).apply()
        
        return passphrase
    }

    /**
     * Checks if the master key exists in Keystore.
     */
    fun hasMasterKey(): Boolean {
        return keyStore.containsAlias(MASTER_KEY_ALIAS)
    }

    /**
     * Deletes all keys (used for panic wipe).
     */
    fun deleteAllKeys() {
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            keyStore.deleteEntry(MASTER_KEY_ALIAS)
        }
        // Also clear stored passphrase
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    /**
     * Data class to hold encrypted data with IV.
     */
    data class EncryptedData(
        val iv: ByteArray,
        val cipherText: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncryptedData
            if (!iv.contentEquals(other.iv)) return false
            if (!cipherText.contentEquals(other.cipherText)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = iv.contentHashCode()
            result = 31 * result + cipherText.contentHashCode()
            return result
        }
    }
}
