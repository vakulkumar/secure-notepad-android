package com.securenotes.domain.usecase

import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for deleting notes.
 */
class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Deletes a note.
     * 
     * @param note The note to delete
     */
    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
    
    /**
     * Deletes a note by ID.
     * 
     * @param id The ID of the note to delete
     */
    suspend operator fun invoke(id: Long) {
        repository.deleteNoteById(id)
    }
    
    /**
     * Deletes all notes (used for panic wipe).
     */
    suspend fun deleteAll() {
        repository.deleteAllNotes()
    }
}
