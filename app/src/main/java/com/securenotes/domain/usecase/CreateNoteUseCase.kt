package com.securenotes.domain.usecase

import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for creating a new note.
 */
class CreateNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Creates a new note.
     * 
     * @param title The note title
     * @param content The note content
     * @return The ID of the created note
     */
    suspend operator fun invoke(title: String, content: String): Long {
        val note = Note(
            title = title.trim(),
            content = content
        )
        return repository.createNote(note)
    }
    
    /**
     * Creates a new note from a Note object.
     * 
     * @param note The note to create
     * @return The ID of the created note
     */
    suspend operator fun invoke(note: Note): Long {
        return repository.createNote(note)
    }
}
