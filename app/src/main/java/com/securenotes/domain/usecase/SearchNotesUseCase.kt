package com.securenotes.domain.usecase

import com.securenotes.domain.model.Note
import com.securenotes.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Use case for searching notes.
 * 
 * Note: Since note content is encrypted, search is performed
 * by decrypting all notes and searching in memory.
 */
class SearchNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    /**
     * Searches notes by query string.
     * Searches both title and content.
     * 
     * @param query The search query
     * @return List of matching notes
     */
    suspend operator fun invoke(query: String): List<Note> {
        if (query.isBlank()) return emptyList()
        return repository.searchNotes(query)
    }
}
