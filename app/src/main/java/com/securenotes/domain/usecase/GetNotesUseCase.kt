package com.securenotes.domain.usecase

import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting notes.
 */
class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Gets all notes as a Flow.
     */
    operator fun invoke(): Flow<List<Note>> {
        return repository.getAllNotes()
    }
    
    /**
     * Gets a single note by ID.
     */
    suspend fun getById(id: Long): Note? {
        return repository.getNoteById(id)
    }
    
    /**
     * Gets a note by ID as a Flow.
     */
    fun getByIdFlow(id: Long): Flow<Note?> {
        return repository.getNoteByIdFlow(id)
    }
    
    /**
     * Gets favorite notes.
     */
    fun getFavorites(): Flow<List<Note>> {
        return repository.getFavoriteNotes()
    }
    
    /**
     * Gets the count of all notes.
     */
    suspend fun getCount(): Int {
        return repository.getNoteCount()
    }
}
