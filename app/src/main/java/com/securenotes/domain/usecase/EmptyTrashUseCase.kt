package com.securenotes.domain.usecase

import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for emptying the trash.
 */
class EmptyTrashUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Permanently deletes all notes in trash.
     */
    suspend operator fun invoke() {
        repository.emptyTrash()
    }
}
