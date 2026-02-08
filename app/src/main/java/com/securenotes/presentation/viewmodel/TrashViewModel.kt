package com.securenotes.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.domain.model.Note
import com.securenotes.domain.usecase.EmptyTrashUseCase
import com.securenotes.domain.usecase.GetTrashNotesUseCase
import com.securenotes.domain.usecase.PermanentlyDeleteNoteUseCase
import com.securenotes.domain.usecase.RestoreNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Trash screen.
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val getTrashNotesUseCase: GetTrashNotesUseCase,
    private val restoreNoteUseCase: RestoreNoteUseCase,
    private val permanentlyDeleteNoteUseCase: PermanentlyDeleteNoteUseCase,
    private val emptyTrashUseCase: EmptyTrashUseCase
) : ViewModel() {

    data class TrashUiState(
        val notes: List<Note> = emptyList(),
        val isLoading: Boolean = true,
        val showEmptyTrashConfirm: Boolean = false
    )

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadTrashNotes()
    }

    private fun loadTrashNotes() {
        viewModelScope.launch {
            getTrashNotesUseCase().collect { notes ->
                _uiState.update { it.copy(notes = notes, isLoading = false) }
            }
        }
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch {
            restoreNoteUseCase(noteId)
        }
    }

    fun permanentlyDeleteNote(noteId: Long) {
        viewModelScope.launch {
            permanentlyDeleteNoteUseCase(noteId)
        }
    }

    fun showEmptyTrashConfirmation() {
        _uiState.update { it.copy(showEmptyTrashConfirm = true) }
    }

    fun hideEmptyTrashConfirmation() {
        _uiState.update { it.copy(showEmptyTrashConfirm = false) }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            emptyTrashUseCase()
            _uiState.update { it.copy(showEmptyTrashConfirm = false) }
        }
    }
}
