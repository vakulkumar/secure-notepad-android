package com.securenotes.data.repository

import com.securenotes.data.local.dao.NoteDao
import com.securenotes.data.local.entity.NoteEntity
import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import com.securenotes.core.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for notes with encryption/decryption.
 * 
 * Defense-in-Depth Encryption Strategy:
 * Layer 1: SQLCipher encrypts the entire database file
 * Layer 2: CryptoManager encrypts individual note content before storage
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

    override suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)?.let { decryptNote(it) }
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

    override suspend fun searchNotes(query: String): List<Note> {
        if (query.isBlank()) return emptyList()
        
        val allNotes = noteDao.getAllNotesOnce()
        val lowerQuery = query.lowercase()
        
        return allNotes
            .map { decryptNote(it) }
            .filter { note ->
                note.title.lowercase().contains(lowerQuery) ||
                note.content.lowercase().contains(lowerQuery)
            }
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

    private fun decryptNote(entity: NoteEntity): Note {
        return Note(
            id = entity.id,
            title = try {
                cryptoManager.decryptString(entity.title)
            } catch (e: Exception) {
                "[Decryption Failed]"
            },
            content = try {
                cryptoManager.decryptString(entity.content)
            } catch (e: Exception) {
                "[Content Unavailable]"
            },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isFavorite = entity.isFavorite,
            colorTag = entity.colorTag,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            isLocked = entity.isLocked
        )
    }
}
