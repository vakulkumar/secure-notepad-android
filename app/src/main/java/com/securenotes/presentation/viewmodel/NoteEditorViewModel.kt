package com.securenotes.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.domain.model.Note
import com.securenotes.domain.usecase.CreateNoteUseCase
import com.securenotes.domain.usecase.GetNotesUseCase
import com.securenotes.domain.usecase.ToggleNoteLockUseCase
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
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val toggleNoteLockUseCase: ToggleNoteLockUseCase
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L
    
    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId > 0) {
            loadNote(noteId)
        }
    }

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
                        isLocked = note.isLocked,
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

    fun onTitleChange(title: String) {
        _uiState.update { 
            it.copy(
                title = title,
                hasUnsavedChanges = true
            )
        }
    }

    fun onContentChange(content: String) {
        _uiState.update { 
            it.copy(
                content = content,
                hasUnsavedChanges = true
            )
        }
    }

    fun toggleFavorite() {
        _uiState.update { 
            it.copy(
                isFavorite = !it.isFavorite,
                hasUnsavedChanges = true
            )
        }
    }

    /**
     * Toggles the lock status of the note.
     */
    fun toggleLock() {
        val currentState = _uiState.value
        val newLockState = !currentState.isLocked
        
        _uiState.update { 
            it.copy(
                isLocked = newLockState,
                hasUnsavedChanges = true
            )
        }
        
        // If editing existing note, also update in database directly
        if (!currentState.isNewNote && currentState.noteId > 0) {
            viewModelScope.launch {
                toggleNoteLockUseCase(currentState.noteId, newLockState)
            }
        }
    }

    /**
     * Toggles between edit and preview mode.
     */
    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    fun saveNote(): Boolean {
        val state = _uiState.value
        
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
                            isFavorite = state.isFavorite,
                            isLocked = state.isLocked
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
                            isFavorite = state.isFavorite,
                            isLocked = state.isLocked
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    data class NoteEditorUiState(
        val noteId: Long = 0,
        val title: String = "",
        val content: String = "",
        val isFavorite: Boolean = false,
        val isLocked: Boolean = false,
        val isNewNote: Boolean = true,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val hasUnsavedChanges: Boolean = false,
        val isPreviewMode: Boolean = false,
        val error: String? = null
    )
}
