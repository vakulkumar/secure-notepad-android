package com.securenotes.domain.usecase

import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for updating an existing note.
 */
class UpdateNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Updates a note with new content.
     * 
     * @param note The note with updated content
     */
    suspend operator fun invoke(note: Note) {
        repository.updateNote(note)
    }
    
    /**
     * Updates a note by ID with new title and content.
     */
    suspend operator fun invoke(id: Long, title: String, content: String) {
        val existingNote = repository.getNoteById(id)
        existingNote?.let { note ->
            repository.updateNote(
                note.copy(
                    title = title.trim(),
                    content = content
                )
            )
        }
    }
    
    /**
     * Toggles the favorite status of a note.
     */
    suspend fun toggleFavorite(id: Long) {
        val note = repository.getNoteById(id)
        note?.let {
            repository.updateNote(it.copy(isFavorite = !it.isFavorite))
        }
    }
}
