package com.securenotes.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.di.ApplicationScope
import com.securenotes.domain.model.Note
import com.securenotes.domain.usecase.CreateNoteUseCase
import com.securenotes.domain.usecase.GetNotesUseCase
import com.securenotes.domain.usecase.ToggleNoteLockUseCase
import com.securenotes.domain.usecase.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the note editor screen.
 * 
 * CRITICAL: Uses ApplicationScope for save operations to prevent data loss
 * when the user navigates away before the save completes.
 */
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNotesUseCase: GetNotesUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val toggleNoteLockUseCase: ToggleNoteLockUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L
    
    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()
    
    // Track save job to allow awaiting completion
    private var saveJob: Job? = null

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

    fun toggleLock() {
        val currentState = _uiState.value
        val newLockState = !currentState.isLocked
        
        _uiState.update { 
            it.copy(
                isLocked = newLockState,
                hasUnsavedChanges = true
            )
        }
        
        if (!currentState.isNewNote && currentState.noteId > 0) {
            viewModelScope.launch {
                toggleNoteLockUseCase(currentState.noteId, newLockState)
            }
        }
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    /**
     * Saves the note using ApplicationScope to survive ViewModel destruction.
     * 
     * SENIOR PATTERN: Critical writes (encryption + DB) must use a scope that
     * survives the UI lifecycle. Using ApplicationScope ensures the save
     * completes even if the user navigates away immediately.
     * 
     * @return Job that can be awaited to ensure save completion before navigation
     */
    fun saveNote(): Job? {
        val state = _uiState.value
        
        if (state.title.isBlank() && state.content.isBlank()) {
            return null
        }
        
        // Cancel any pending save to avoid duplicates
        saveJob?.cancel()
        
        // Use ApplicationScope - this survives ViewModel destruction
        saveJob = applicationScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            try {
                if (state.isNewNote) {
                    createNote(state)
                } else {
                    updateNote(state)
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
        
        return saveJob
    }

    private suspend fun createNote(state: NoteEditorUiState) {
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
    }

    private suspend fun updateNote(state: NoteEditorUiState) {
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
    
    /**
     * Saves the note and blocks until complete.
     * Use this when you MUST wait for save before navigating.
     */
    suspend fun saveNoteAndWait(): Boolean {
        return saveNote()?.also { it.join() } != null
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
