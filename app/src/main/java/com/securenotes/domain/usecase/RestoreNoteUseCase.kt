package com.securenotes.domain.usecase

import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for restoring a note from trash.
 */
class RestoreNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Restores a note from trash.
     */
    suspend operator fun invoke(noteId: Long) {
        repository.restoreNote(noteId)
    }
}
