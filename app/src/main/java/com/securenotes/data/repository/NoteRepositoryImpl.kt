package com.securenotes.data.repository

import com.securenotes.data.local.dao.NoteDao
import com.securenotes.data.local.entity.NoteEntity
import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import com.securenotes.security.CryptoManager
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
 * 
 * This ensures that even if SQLCipher is somehow bypassed,
 * the note content remains encrypted with Keystore-backed keys.
 */
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val cryptoManager: CryptoManager
) : NoteRepository {

    /**
     * Gets all notes, decrypting content on the fly.
     */
    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { entity -> decryptNote(entity) }
        }
    }

    /**
     * Gets a single note by ID.
     */
    override suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)?.let { decryptNote(it) }
    }

    /**
     * Gets a note as a Flow for observing changes.
     */
    override fun getNoteByIdFlow(id: Long): Flow<Note?> {
        return noteDao.getNoteByIdFlow(id).map { entity ->
            entity?.let { decryptNote(it) }
        }
    }

    /**
     * Creates a new note with encrypted content.
     * 
     * @return The ID of the created note
     */
    override suspend fun createNote(note: Note): Long {
        val entity = encryptNote(note.copy(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))
        return noteDao.insertNote(entity)
    }

    /**
     * Updates an existing note with encrypted content.
     */
    override suspend fun updateNote(note: Note) {
        val entity = encryptNote(note.copy(
            updatedAt = System.currentTimeMillis()
        ))
        noteDao.updateNote(entity)
    }

    /**
     * Deletes a note.
     */
    override suspend fun deleteNote(note: Note) {
        noteDao.deleteNoteById(note.id)
    }

    /**
     * Deletes a note by ID.
     */
    override suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
    }

    /**
     * Searches notes by query.
     * Since content is encrypted in DB, we decrypt all notes and search in memory.
     * 
     * For large note collections, consider implementing a secure search index.
     */
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

    /**
     * Gets favorite notes.
     */
    override fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes().map { entities ->
            entities.map { decryptNote(it) }
        }
    }

    /**
     * Deletes all notes (used for panic wipe).
     */
    override suspend fun deleteAllNotes() {
        noteDao.deleteAllNotes()
    }

    /**
     * Gets the count of all notes.
     */
    override suspend fun getNoteCount(): Int {
        return noteDao.getNoteCount()
    }

    /**
     * Encrypts note content before storage.
     */
    private fun encryptNote(note: Note): NoteEntity {
        return NoteEntity(
            id = note.id,
            title = cryptoManager.encryptString(note.title),
            content = cryptoManager.encryptString(note.content),
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            isFavorite = note.isFavorite,
            colorTag = note.colorTag
        )
    }

    /**
     * Decrypts note content after retrieval.
     */
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
            colorTag = entity.colorTag
        )
    }
}
