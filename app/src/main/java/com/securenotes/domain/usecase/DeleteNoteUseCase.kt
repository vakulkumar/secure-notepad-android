package com.securenotes.domain.usecase

import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for deleting notes.
 * 
 * Default behavior is soft delete (move to trash).
 * Use PermanentlyDeleteNoteUseCase for permanent deletion.
 */
class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Soft deletes a note (moves to trash).
     * 
     * @param note The note to delete
     */
    suspend operator fun invoke(note: Note) {
        repository.softDeleteNote(note.id)
    }
    
    /**
     * Soft deletes a note by ID (moves to trash).
     * 
     * @param id The ID of the note to delete
     */
    suspend operator fun invoke(id: Long) {
        repository.softDeleteNote(id)
    }
    
    /**
     * Deletes all notes permanently (used for panic wipe).
     */
    suspend fun deleteAll() {
        repository.deleteAllNotes()
    }
}
