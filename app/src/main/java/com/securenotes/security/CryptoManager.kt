package com.securenotes.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CryptoManager handles all cryptographic operations using Android Keystore.
 * 
 * Security Features:
 * - AES-256-GCM encryption (authenticated encryption)
 * - Hardware-backed key storage when available
 * - Keys never leave the Keystore
 * - User authentication required for key access (optional)
 * 
 * Encryption Key Flow:
 * 1. Master key generated/retrieved from Android Keystore
 * 2. Master key used to encrypt/decrypt note content
 * 3. Database passphrase derived from master key for SQLCipher
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "secure_notes_master_key"
        private const val DATABASE_KEY_ALIAS = "secure_notes_db_key"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * 
     * @param plainText The data to encrypt
     * @return EncryptedData containing IV and ciphertext
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
     * 
     * @param encryptedData The encrypted data containing IV and ciphertext
     * @return Decrypted plaintext bytes
     */
    fun decrypt(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)
        
        return cipher.doFinal(encryptedData.cipherText)
    }

    /**
     * Encrypts a string and returns Base64-encoded result.
     * Format: [IV_LENGTH][IV][CIPHERTEXT]
     */
    fun encryptString(plainText: String): String {
        val encrypted = encrypt(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + encrypted.iv.size + encrypted.cipherText.size)
        combined[0] = encrypted.iv.size.toByte()
        System.arraycopy(encrypted.iv, 0, combined, 1, encrypted.iv.size)
        System.arraycopy(encrypted.cipherText, 0, combined, 1 + encrypted.iv.size, encrypted.cipherText.size)
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    /**
     * Decrypts a Base64-encoded encrypted string.
     */
    fun decryptString(encryptedText: String): String {
        val combined = android.util.Base64.decode(encryptedText, android.util.Base64.NO_WRAP)
        val ivLength = combined[0].toInt()
        val iv = combined.copyOfRange(1, 1 + ivLength)
        val cipherText = combined.copyOfRange(1 + ivLength, combined.size)
        
        val decrypted = decrypt(EncryptedData(iv, cipherText))
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * Gets or creates the master encryption key in Android Keystore.
     * 
     * Key Properties:
     * - AES-256 key
     * - GCM mode with no padding
     * - Hardware-backed when available
     * - Not exportable (stays in Keystore)
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
            // Hardware-backed when available
            .setUserAuthenticationRequired(false) // Set to true for additional security
            .build()
        
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Generates the database encryption passphrase from Keystore.
     * This passphrase is used by SQLCipher to encrypt the Room database.
     * 
     * @return ByteArray passphrase for SQLCipher (32 bytes = 256 bits)
     */
    fun getDatabasePassphrase(): ByteArray {
        val existingKey = keyStore.getEntry(DATABASE_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        val key = existingKey?.secretKey ?: createDatabaseKey()
        
        // Use the encoded key as passphrase
        // Note: On Android Keystore, encoded returns null for security
        // So we derive a passphrase by encrypting a fixed value
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        // Encrypt a fixed seed to derive consistent passphrase
        val seed = "SECURE_NOTES_DB_PASSPHRASE_SEED".toByteArray()
        return cipher.doFinal(seed)
    }

    private fun createDatabaseKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER)
        
        val keyGenSpec = KeyGenParameterSpec.Builder(
            DATABASE_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(false) // Need deterministic output for passphrase
            .build()
        
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
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
        if (keyStore.containsAlias(DATABASE_KEY_ALIAS)) {
            keyStore.deleteEntry(DATABASE_KEY_ALIAS)
        }
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
