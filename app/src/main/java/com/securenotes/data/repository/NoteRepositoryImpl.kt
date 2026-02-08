package com.securenotes.data.repository

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.securenotes.data.local.dao.NoteDao
import com.securenotes.data.local.entity.NoteEntity
import com.securenotes.domain.model.DecryptionError
import com.securenotes.domain.model.Note
import com.securenotes.domain.model.NoteWithStatus
import com.securenotes.domain.repository.NoteRepository
import com.securenotes.core.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.InvalidKeyException
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for notes with encryption/decryption.
 * 
 * Defense-in-Depth Encryption Strategy:
 * Layer 1: SQLCipher encrypts the entire database file
 * Layer 2: CryptoManager encrypts individual note content before storage
 * 
 * KNOWN TRADE-OFF (Scalability):
 * Because of field-level encryption, we cannot use SQL LIKE queries for search.
 * All notes must be decrypted in memory to search. This is acceptable for
 * typical note counts (<1000) but will cause performance issues at scale.
 * 
 * Future improvements:
 * - Implement blind indexing (hash of keywords) for searchable encryption
 * - Use chunked/streaming decryption for large note sets
 * - Consider FTS with searchable encryption scheme
 */
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val cryptoManager: CryptoManager
) : NoteRepository {

    // ========== Active Notes ==========

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { entity -> decryptNote(entity) }
        }
    }
    
    /**
     * Returns all notes with their decryption status.
     * Use this when you need to handle decryption errors in the UI.
     */
    fun getAllNotesWithStatus(): Flow<List<NoteWithStatus>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { entity -> decryptNoteWithStatus(entity) }
        }
    }

    override suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)?.let { decryptNote(it) }
    }
    
    /**
     * Gets a note by ID with decryption status for error handling.
     */
    suspend fun getNoteByIdWithStatus(id: Long): NoteWithStatus? {
        return noteDao.getNoteById(id)?.let { decryptNoteWithStatus(it) }
    }

    override fun getNoteByIdFlow(id: Long): Flow<Note?> {
        return noteDao.getNoteByIdFlow(id).map { entity ->
            entity?.let { decryptNote(it) }
        }
    }

    override suspend fun createNote(note: Note): Long {
        val entity = encryptNote(note.copy(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))
        return noteDao.insertNote(entity)
    }

    override suspend fun updateNote(note: Note) {
        val entity = encryptNote(note.copy(
            updatedAt = System.currentTimeMillis()
        ))
        noteDao.updateNote(entity)
    }

    override suspend fun softDeleteNote(id: Long) {
        noteDao.softDeleteNote(id, System.currentTimeMillis())
    }

    override suspend fun deleteNote(note: Note) {
        noteDao.deleteNoteById(note.id)
    }

    override suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
    }

    /**
     * Searches notes by decrypting and filtering in memory.
     * 
     * PERFORMANCE NOTE: This is O(n) where n = total notes.
     * For large note collections (>500), consider:
     * 1. Pagination with LIMIT/OFFSET before decryption
     * 2. Blind indexing for server-side search
     * 3. Background indexing with encrypted search tokens
     * 
     * Current implementation is suitable for personal note apps
     * with typical usage (<500 notes).
     */
    override suspend fun searchNotes(query: String): List<Note> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        
        val allNotes = noteDao.getAllNotesOnce()
        val lowerQuery = query.lowercase()
        
        // Process in chunks to avoid blocking UI thread for too long
        allNotes
            .asSequence()
            .map { decryptNote(it) }
            .filter { note ->
                note.title.lowercase().contains(lowerQuery) ||
                note.content.lowercase().contains(lowerQuery)
            }
            .toList()
    }

    override fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes().map { entities ->
            entities.map { decryptNote(it) }
        }
    }

    override suspend fun deleteAllNotes() {
        noteDao.deleteAllNotes()
    }

    override suspend fun getNoteCount(): Int {
        return noteDao.getNoteCount()
    }

    // ========== Trash (Recycle Bin) ==========

    override fun getDeletedNotes(): Flow<List<Note>> {
        return noteDao.getDeletedNotes().map { entities ->
            entities.map { entity -> decryptNote(entity) }
        }
    }

    override suspend fun restoreNote(id: Long) {
        noteDao.restoreNote(id)
    }

    override suspend fun permanentlyDeleteNote(id: Long) {
        noteDao.permanentlyDeleteNote(id)
    }

    override suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    override suspend fun getDeletedNoteCount(): Int {
        return noteDao.getDeletedNoteCount()
    }

    // ========== Biometric Lock ==========

    override suspend fun updateNoteLock(id: Long, isLocked: Boolean) {
        noteDao.updateNoteLock(id, isLocked)
    }

    // ========== Backup/Restore ==========

    override suspend fun getAllNotesForBackup(): List<Note> {
        return noteDao.getAllNotesForBackup().map { decryptNote(it) }
    }

    override suspend fun restoreNotesFromBackup(notes: List<Note>) {
        val entities = notes.map { note ->
            encryptNote(note)
        }
        noteDao.insertNotes(entities)
    }

    // ========== Encryption Helpers ==========

    private fun encryptNote(note: Note): NoteEntity {
        return NoteEntity(
            id = note.id,
            title = cryptoManager.encryptString(note.title),
            content = cryptoManager.encryptString(note.content),
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            isFavorite = note.isFavorite,
            colorTag = note.colorTag,
            isDeleted = note.isDeleted,
            deletedAt = note.deletedAt,
            isLocked = note.isLocked
        )
    }

    /**
     * Decrypts a note entity, returning a Note with error placeholder if decryption fails.
     * For simple cases where error handling is done elsewhere.
     */
    private fun decryptNote(entity: NoteEntity): Note {
        val titleResult = decryptField(entity.title)
        val contentResult = decryptField(entity.content)
        
        return Note(
            id = entity.id,
            title = titleResult.first,
            content = contentResult.first,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isFavorite = entity.isFavorite,
            colorTag = entity.colorTag,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            isLocked = entity.isLocked
        )
    }
    
    /**
     * Decrypts a note entity with full status information for error handling.
     * Use this when the UI needs to display recovery options.
     */
    private fun decryptNoteWithStatus(entity: NoteEntity): NoteWithStatus {
        val titleResult = decryptField(entity.title)
        val contentResult = decryptField(entity.content)
        
        val hasError = titleResult.second != null || contentResult.second != null
        val error = titleResult.second ?: contentResult.second
        
        val note = Note(
            id = entity.id,
            title = titleResult.first,
            content = contentResult.first,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isFavorite = entity.isFavorite,
            colorTag = entity.colorTag,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            isLocked = entity.isLocked
        )
        
        return if (hasError) {
            NoteWithStatus.withError(note, error ?: DecryptionError.UNKNOWN)
        } else {
            NoteWithStatus.success(note)
        }
    }
    
    /**
     * Decrypts a field and returns the result with any error.
     * Maps specific exceptions to DecryptionError types for proper UI handling.
     */
    private fun decryptField(encryptedValue: String): Pair<String, DecryptionError?> {
        return try {
            Pair(cryptoManager.decryptString(encryptedValue), null)
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Key was invalidated (biometrics changed, device reset)
            Pair("[Key Invalidated - Restore from Backup]", DecryptionError.KEY_INVALIDATED)
        } catch (e: InvalidKeyException) {
            // Authentication required or key issue
            Pair("[Authentication Required]", DecryptionError.AUTHENTICATION_REQUIRED)
        } catch (e: AEADBadTagException) {
            // Data corruption or tampering
            Pair("[Data Corrupted]", DecryptionError.DATA_CORRUPTED)
        } catch (e: Exception) {
            // Unknown error - log for debugging but don't expose details
            Pair("[Decryption Failed]", DecryptionError.UNKNOWN)
        }
    }
}
