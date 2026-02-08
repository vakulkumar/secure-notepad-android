package com.securenotes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.securenotes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notes.
 * 
 * Note: Search is performed on encrypted data. The repository layer
 * decrypts notes first, then performs in-memory search.
 */
@Dao
interface NoteDao {
    
    // ========== Active Notes (Not Deleted) ==========
    
    /**
     * Gets all active (non-deleted) notes ordered by modification time (newest first).
     */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>
    
    /**
     * Gets all active notes as a one-shot list (for search).
     */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    suspend fun getAllNotesOnce(): List<NoteEntity>
    
    /**
     * Gets a single note by ID.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?
    
    /**
     * Gets a note by ID as a Flow for observing changes.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?>
    
    /**
     * Gets active favorite notes.
     */
    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>
    
    // ========== Trash (Deleted Notes) ==========
    
    /**
     * Gets all deleted notes (trash).
     */
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>
    
    /**
     * Gets count of deleted notes.
     */
    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 1")
    suspend fun getDeletedNoteCount(): Int
    
    /**
     * Soft deletes a note (moves to trash).
     */
    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteNote(id: Long, deletedAt: Long = System.currentTimeMillis())
    
    /**
     * Restores a note from trash.
     */
    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreNote(id: Long)
    
    /**
     * Permanently deletes a note by ID.
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun permanentlyDeleteNote(id: Long)
    
    /**
     * Empties the trash (permanently deletes all soft-deleted notes).
     */
    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()
    
    // ========== Biometric Lock ==========
    
    /**
     * Updates the lock status of a note.
     */
    @Query("UPDATE notes SET isLocked = :isLocked WHERE id = :id")
    suspend fun updateNoteLock(id: Long, isLocked: Boolean)
    
    // ========== CRUD Operations ==========
    
    /**
     * Inserts a new note.
     * 
     * @return The row ID of the inserted note
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long
    
    /**
     * Updates an existing note.
     */
    @Update
    suspend fun updateNote(note: NoteEntity)
    
    /**
     * Deletes a note (permanent - use softDeleteNote for trash).
     */
    @Delete
    suspend fun deleteNote(note: NoteEntity)
    
    /**
     * Deletes a note by ID (permanent).
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
    
    /**
     * Deletes all notes (used for panic wipe).
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
    
    /**
     * Gets the count of active notes.
     */
    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0")
    suspend fun getNoteCount(): Int
    
    // ========== Backup/Restore ==========
    
    /**
     * Gets all notes including deleted (for backup).
     */
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesForBackup(): List<NoteEntity>
    
    /**
     * Inserts multiple notes (for restore).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)
}
