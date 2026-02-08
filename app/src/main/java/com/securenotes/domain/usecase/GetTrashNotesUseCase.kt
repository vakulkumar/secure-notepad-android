package com.securenotes.domain.usecase

import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting trash (deleted) notes.
 */
class GetTrashNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Gets all deleted notes as a Flow.
     */
    operator fun invoke(): Flow<List<Note>> {
        return repository.getDeletedNotes()
    }
    
    /**
     * Gets the count of deleted notes.
     */
    suspend fun getCount(): Int {
        return repository.getDeletedNoteCount()
    }
}
