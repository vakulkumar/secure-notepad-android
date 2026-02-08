package com.securenotes.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.domain.model.Note
import com.securenotes.domain.usecase.DeleteNoteUseCase
import com.securenotes.domain.usecase.GetNotesUseCase
import com.securenotes.domain.usecase.SearchNotesUseCase
import com.securenotes.domain.usecase.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the note list screen.
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    /**
     * Loads all notes from the repository.
     */
    private fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getNotesUseCase().collect { notes ->
                _uiState.update { 
                    it.copy(
                        notes = notes,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Updates the search query and performs search.
     */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        if (query.isBlank()) {
            loadNotes()
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isSearching = true) }
                val results = searchNotesUseCase(query)
                _uiState.update { 
                    it.copy(
                        notes = results,
                        isSearching = false
                    )
                }
            }
        }
    }

    /**
     * Clears the search query.
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
        loadNotes()
    }

    /**
     * Deletes a note.
     */
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            deleteNoteUseCase(note)
        }
    }

    /**
     * Toggles the favorite status of a note.
     */
    fun toggleFavorite(noteId: Long) {
        viewModelScope.launch {
            updateNoteUseCase.toggleFavorite(noteId)
        }
    }

    /**
     * Data class representing the UI state.
     */
    data class NotesUiState(
        val notes: List<Note> = emptyList(),
        val searchQuery: String = "",
        val isLoading: Boolean = false,
        val isSearching: Boolean = false,
        val error: String? = null
    )
}
