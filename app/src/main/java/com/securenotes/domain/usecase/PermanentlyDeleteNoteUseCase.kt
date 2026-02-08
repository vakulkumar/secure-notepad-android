package com.securenotes.domain.usecase

import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for permanently deleting a note.
 */
class PermanentlyDeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Permanently deletes a note (cannot be recovered).
     */
    suspend operator fun invoke(noteId: Long) {
        repository.permanentlyDeleteNote(noteId)
    }
}
