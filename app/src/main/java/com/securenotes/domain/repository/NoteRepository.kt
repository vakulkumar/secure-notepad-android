package com.securenotes.domain.repository

import com.securenotes.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for notes.
 * 
 * Defines the contract for data operations.
 * Implementation handles encryption/decryption transparently.
 */
interface NoteRepository {
    
    // ========== Active Notes ==========
    
    /**
     * Gets all active (non-deleted) notes as a Flow.
     */
    fun getAllNotes(): Flow<List<Note>>
    
    /**
     * Gets a single note by ID.
     */
    suspend fun getNoteById(id: Long): Note?
    
    /**
     * Gets a note as a Flow for observing changes.
     */
    fun getNoteByIdFlow(id: Long): Flow<Note?>
    
    /**
     * Creates a new note.
     * @return The ID of the created note
     */
    suspend fun createNote(note: Note): Long
    
    /**
     * Updates an existing note.
     */
    suspend fun updateNote(note: Note)
    
    /**
     * Soft deletes a note (moves to trash).
     */
    suspend fun softDeleteNote(id: Long)
    
    /**
     * Permanently deletes a note.
     */
    suspend fun deleteNote(note: Note)
    
    /**
     * Deletes a note by ID (permanent).
     */
    suspend fun deleteNoteById(id: Long)
    
    /**
     * Searches notes by query.
     */
    suspend fun searchNotes(query: String): List<Note>
    
    /**
     * Gets favorite notes.
     */
    fun getFavoriteNotes(): Flow<List<Note>>
    
    /**
     * Deletes all notes.
     */
    suspend fun deleteAllNotes()
    
    /**
     * Gets the count of active notes.
     */
    suspend fun getNoteCount(): Int
    
    // ========== Trash (Recycle Bin) ==========
    
    /**
     * Gets all deleted notes (trash).
     */
    fun getDeletedNotes(): Flow<List<Note>>
    
    /**
     * Restores a note from trash.
     */
    suspend fun restoreNote(id: Long)
    
    /**
     * Permanently deletes a note from trash.
     */
    suspend fun permanentlyDeleteNote(id: Long)
    
    /**
     * Empties the trash (deletes all soft-deleted notes).
     */
    suspend fun emptyTrash()
    
    /**
     * Gets the count of deleted notes.
     */
    suspend fun getDeletedNoteCount(): Int
    
    // ========== Biometric Lock ==========
    
    /**
     * Updates the lock status of a note.
     */
    suspend fun updateNoteLock(id: Long, isLocked: Boolean)
    
    // ========== Backup/Restore ==========
    
    /**
     * Gets all notes including deleted (for backup).
     */
    suspend fun getAllNotesForBackup(): List<Note>
    
    /**
     * Inserts multiple notes (for restore).
     */
    suspend fun restoreNotesFromBackup(notes: List<Note>)
}
