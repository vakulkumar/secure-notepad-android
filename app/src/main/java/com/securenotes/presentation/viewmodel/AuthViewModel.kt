package com.securenotes.presentation.viewmodel

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.security.BiometricAuthManager
import com.securenotes.security.PanicManager
import com.securenotes.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screen.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val biometricAuthManager: BiometricAuthManager,
    private val securePreferences: SecurePreferences,
    private val panicManager: PanicManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkBiometricCapability()
        observeAuthResults()
    }

    /**
     * Checks device biometric capability.
     */
    private fun checkBiometricCapability() {
        val capability = biometricAuthManager.canAuthenticate()
        _uiState.update { 
            it.copy(
                biometricCapability = capability,
                canUseBiometric = capability == BiometricAuthManager.BiometricCapability.AVAILABLE
            )
        }
    }

    /**
     * Observes authentication results from BiometricAuthManager.
     */
    private fun observeAuthResults() {
        viewModelScope.launch {
            biometricAuthManager.authResult.collect { result ->
                when (result) {
                    is BiometricAuthManager.AuthResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isAuthenticated = true,
                                authError = null
                            )
                        }
                        securePreferences.isAppLocked = false
                        securePreferences.recordActivity()
                    }
                    is BiometricAuthManager.AuthResult.Failed -> {
                        _uiState.update { 
                            it.copy(authError = "Authentication failed. Try again.")
                        }
                    }
                    is BiometricAuthManager.AuthResult.Error -> {
                        _uiState.update { 
                            it.copy(authError = result.message)
                        }
                    }
                }
            }
        }
    }

    /**
     * Initiates biometric authentication.
     */
    fun authenticate(activity: FragmentActivity) {
        _uiState.update { it.copy(isAuthenticating = true, authError = null) }
        
        biometricAuthManager.authenticate(
            activity = activity,
            title = "Secure Notes",
            subtitle = "Authenticate to access your notes",
            description = "Use your fingerprint or PIN to unlock"
        )
        
        _uiState.update { it.copy(isAuthenticating = false) }
    }

    /**
     * Executes panic action.
     */
    fun executePanic(level: PanicManager.PanicLevel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPanicExecuting = true) }
            
            val result = panicManager.executePanic(level)
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isPanicExecuting = false,
                        panicExecuted = true
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isPanicExecuting = false,
                        authError = "Panic action failed: ${error.message}"
                    )
                }
            }
        }
    }

    /**
     * Clears the error message.
     */
    fun clearError() {
        _uiState.update { it.copy(authError = null) }
    }

    /**
     * Data class representing the UI state.
     */
    data class AuthUiState(
        val isAuthenticated: Boolean = false,
        val isAuthenticating: Boolean = false,
        val canUseBiometric: Boolean = false,
        val biometricCapability: BiometricAuthManager.BiometricCapability = 
            BiometricAuthManager.BiometricCapability.UNKNOWN,
        val authError: String? = null,
        val isPanicExecuting: Boolean = false,
        val panicExecuted: Boolean = false
    )
}
