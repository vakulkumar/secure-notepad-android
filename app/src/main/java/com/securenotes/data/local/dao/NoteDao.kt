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
    
    /**
     * Gets all notes ordered by modification time (newest first).
     */
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>
    
    /**
     * Gets all notes as a one-shot list (for search).
     */
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
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
     * Gets favorite notes.
     */
    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>
    
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
     * Deletes a note.
     */
    @Delete
    suspend fun deleteNote(note: NoteEntity)
    
    /**
     * Deletes a note by ID.
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
    
    /**
     * Deletes all notes (used for panic wipe).
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
    
    /**
     * Gets the count of all notes.
     */
    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int
}
