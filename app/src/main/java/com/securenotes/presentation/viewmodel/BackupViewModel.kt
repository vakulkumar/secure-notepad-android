package com.securenotes.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.domain.repository.NoteRepository
import com.securenotes.security.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val noteRepository: NoteRepository
) : ViewModel() {

    data class BackupUiState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val showPasswordDialog: Boolean = false,
        val dialogMode: DialogMode = DialogMode.EXPORT,
        val pendingUri: Uri? = null,
        val message: String? = null,
        val error: String? = null
    )
    
    enum class DialogMode { EXPORT, IMPORT }

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onExportRequest(uri: Uri) {
        _uiState.update { 
            it.copy(
                showPasswordDialog = true,
                dialogMode = DialogMode.EXPORT,
                pendingUri = uri
            )
        }
    }

    fun onImportRequest(uri: Uri) {
        _uiState.update { 
            it.copy(
                showPasswordDialog = true,
                dialogMode = DialogMode.IMPORT,
                pendingUri = uri
            )
        }
    }

    fun dismissPasswordDialog() {
        _uiState.update { 
            it.copy(
                showPasswordDialog = false,
                pendingUri = null
            )
        }
    }

    fun onPasswordConfirm(password: String) {
        val state = _uiState.value
        val uri = state.pendingUri ?: return
        
        _uiState.update { it.copy(showPasswordDialog = false) }
        
        when (state.dialogMode) {
            DialogMode.EXPORT -> exportBackup(uri, password)
            DialogMode.IMPORT -> importBackup(uri, password)
        }
    }

    private fun exportBackup(uri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            
            try {
                val notes = noteRepository.getAllNotesForBackup()
                val result = backupManager.createBackup(notes, password, uri)
                
                result.fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(
                                isExporting = false,
                                message = "Backup created successfully (${notes.size} notes)"
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { 
                            it.copy(
                                isExporting = false,
                                error = "Export failed: ${e.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isExporting = false,
                        error = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun importBackup(uri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, error = null) }
            
            try {
                val result = backupManager.restoreBackup(uri, password)
                
                result.fold(
                    onSuccess = { notes ->
                        noteRepository.restoreNotesFromBackup(notes)
                        _uiState.update { 
                            it.copy(
                                isImporting = false,
                                message = "Restored ${notes.size} notes successfully"
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { 
                            it.copy(
                                isImporting = false,
                                error = e.message ?: "Import failed"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isImporting = false,
                        error = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
