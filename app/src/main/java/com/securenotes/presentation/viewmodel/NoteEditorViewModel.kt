package com.securenotes.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.domain.model.Note
import com.securenotes.domain.usecase.CreateNoteUseCase
import com.securenotes.domain.usecase.GetNotesUseCase
import com.securenotes.domain.usecase.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the note editor screen.
 */
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNotesUseCase: GetNotesUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L
    
    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId > 0) {
            loadNote(noteId)
        }
    }

    /**
     * Loads an existing note for editing.
     */
    private fun loadNote(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val note = getNotesUseCase.getById(id)
            if (note != null) {
                _uiState.update { 
                    it.copy(
                        noteId = note.id,
                        title = note.title,
                        content = note.content,
                        isFavorite = note.isFavorite,
                        isLoading = false,
                        isNewNote = false
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Note not found"
                    )
                }
            }
        }
    }

    /**
     * Updates the title.
     */
    fun onTitleChange(title: String) {
        _uiState.update { 
            it.copy(
                title = title,
                hasUnsavedChanges = true
            )
        }
    }

    /**
     * Updates the content.
     */
    fun onContentChange(content: String) {
        _uiState.update { 
            it.copy(
                content = content,
                hasUnsavedChanges = true
            )
        }
    }

    /**
     * Toggles favorite status.
     */
    fun toggleFavorite() {
        _uiState.update { 
            it.copy(
                isFavorite = !it.isFavorite,
                hasUnsavedChanges = true
            )
        }
    }

    /**
     * Saves the note.
     * 
     * @return true if save was successful
     */
    fun saveNote(): Boolean {
        val state = _uiState.value
        
        // Don't save empty notes
        if (state.title.isBlank() && state.content.isBlank()) {
            return false
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            try {
                if (state.isNewNote) {
                    val newId = createNoteUseCase(
                        Note(
                            title = state.title,
                            content = state.content,
                            isFavorite = state.isFavorite
                        )
                    )
                    _uiState.update { 
                        it.copy(
                            noteId = newId,
                            isNewNote = false,
                            isSaving = false,
                            hasUnsavedChanges = false
                        )
                    }
                } else {
                    updateNoteUseCase(
                        Note(
                            id = state.noteId,
                            title = state.title,
                            content = state.content,
                            isFavorite = state.isFavorite
                        )
                    )
                    _uiState.update { 
                        it.copy(
                            isSaving = false,
                            hasUnsavedChanges = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSaving = false,
                        error = "Failed to save note: ${e.message}"
                    )
                }
            }
        }
        
        return true
    }

    /**
     * Clears the error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Data class representing the UI state.
     */
    data class NoteEditorUiState(
        val noteId: Long = 0,
        val title: String = "",
        val content: String = "",
        val isFavorite: Boolean = false,
        val isNewNote: Boolean = true,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val hasUnsavedChanges: Boolean = false,
        val error: String? = null
    )
}
