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
    
    /**
     * Gets all notes as a Flow for reactive updates.
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
     * 
     * @return The ID of the created note
     */
    suspend fun createNote(note: Note): Long
    
    /**
     * Updates an existing note.
     */
    suspend fun updateNote(note: Note)
    
    /**
     * Deletes a note.
     */
    suspend fun deleteNote(note: Note)
    
    /**
     * Deletes a note by ID.
     */
    suspend fun deleteNoteById(id: Long)
    
    /**
     * Searches notes by query.
     * Searches both title and content.
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
     * Gets the count of all notes.
     */
    suspend fun getNoteCount(): Int
}
