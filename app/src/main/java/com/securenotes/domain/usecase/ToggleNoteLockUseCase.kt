package com.securenotes.domain.usecase

import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for toggling note lock status.
 */
class ToggleNoteLockUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Sets the lock status of a note.
     * When locked, the note requires biometric authentication to view.
     */
    suspend operator fun invoke(noteId: Long, isLocked: Boolean) {
        repository.updateNoteLock(noteId, isLocked)
    }
}
