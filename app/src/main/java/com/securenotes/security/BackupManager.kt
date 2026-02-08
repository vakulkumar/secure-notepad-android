package com.securenotes.security

import android.content.Context
import android.net.Uri
import com.securenotes.core.security.TimeProvider
import com.securenotes.domain.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encrypted backup and restore of notes.
 * 
 * Encryption: AES-256-GCM with PBKDF2 key derivation
 * - 100,000 iterations for key stretching
 * - 16-byte random salt
 * - 12-byte random IV for GCM
 * 
 * Backup format: [salt(16)][iv(12)][encrypted_data]
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    companion object {
        private const val PBKDF2_ITERATIONS = 100_000
        private const val SALT_SIZE = 16
        private const val IV_SIZE = 12
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Creates an encrypted backup of notes.
     */
    fun createBackup(notes: List<Note>, password: String, outputUri: Uri): Result<Unit> {
        return try {
            val backupData = BackupData(
                version = 1,
                timestamp = timeProvider.now(),
                notes = notes.map { it.toBackupNote() }
            )
            val jsonData = json.encodeToString(backupData)
            
            val salt = ByteArray(SALT_SIZE).apply { SecureRandom().nextBytes(this) }
            val iv = ByteArray(IV_SIZE).apply { SecureRandom().nextBytes(this) }
            
            val key = deriveKey(password, salt)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val encryptedData = cipher.doFinal(jsonData.toByteArray(Charsets.UTF_8))
            
            val output = ByteArrayOutputStream()
            output.write(salt)
            output.write(iv)
            output.write(encryptedData)
            
            context.contentResolver.openOutputStream(outputUri)?.use { stream ->
                stream.write(output.toByteArray())
            } ?: return Result.failure(Exception("Failed to open output stream"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores notes from an encrypted backup.
     */
    fun restoreBackup(inputUri: Uri, password: String): Result<List<Note>> {
        return try {
            val data = context.contentResolver.openInputStream(inputUri)?.use { stream ->
                stream.readBytes()
            } ?: return Result.failure(Exception("Failed to read backup file"))
            
            if (data.size < SALT_SIZE + IV_SIZE) {
                return Result.failure(Exception("Invalid backup file format"))
            }
            
            val salt = data.copyOfRange(0, SALT_SIZE)
            val iv = data.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
            val encryptedData = data.copyOfRange(SALT_SIZE + IV_SIZE, data.size)
            
            val key = deriveKey(password, salt)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decryptedData = cipher.doFinal(encryptedData)
            
            val jsonString = String(decryptedData, Charsets.UTF_8)
            val backupData = json.decodeFromString<BackupData>(jsonString)
            
            val notes = backupData.notes.map { it.toNote() }
            
            Result.success(notes)
        } catch (e: javax.crypto.AEADBadTagException) {
            Result.failure(Exception("Incorrect password or corrupted backup"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    @Serializable
    data class BackupData(
        val version: Int,
        val timestamp: Long,
        val notes: List<BackupNote>
    )

    @Serializable
    data class BackupNote(
        val id: Long,
        val title: String,
        val content: String,
        val createdAt: Long,
        val updatedAt: Long,
        val isFavorite: Boolean,
        val colorTag: Int?,
        val isDeleted: Boolean,
        val deletedAt: Long?,
        val isLocked: Boolean
    )

    private fun Note.toBackupNote() = BackupNote(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorite = isFavorite,
        colorTag = colorTag,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        isLocked = isLocked
    )

    private fun BackupNote.toNote() = Note(
        id = 0,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorite = isFavorite,
        colorTag = colorTag,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        isLocked = isLocked
    )
}
