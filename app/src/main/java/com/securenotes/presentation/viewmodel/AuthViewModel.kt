package com.securenotes.presentation.viewmodel

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.core.security.BiometricAuthManager
import com.securenotes.core.security.PinAuthManager
import com.securenotes.core.security.SecurePreferences
import com.securenotes.security.PanicManager
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
    private val panicManager: PanicManager,
    private val pinAuthManager: PinAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkBiometricCapability()
        checkPinEnabled()
        observeAuthResults()
    }

    private fun checkBiometricCapability() {
        val capability = biometricAuthManager.canAuthenticate()
        _uiState.update { 
            it.copy(
                biometricCapability = capability,
                canUseBiometric = capability == BiometricAuthManager.BiometricCapability.AVAILABLE
            )
        }
    }

    private fun checkPinEnabled() {
        _uiState.update { 
            it.copy(isPinEnabled = pinAuthManager.isPinEnabled())
        }
    }

    private fun observeAuthResults() {
        viewModelScope.launch {
            biometricAuthManager.authResult.collect { result ->
                when (result) {
                    is BiometricAuthManager.AuthResult.Success -> {
                        onAuthSuccess()
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

    private fun onAuthSuccess(isDuress: Boolean = false) {
        _uiState.update { 
            it.copy(
                isAuthenticated = true,
                isDuressMode = isDuress,
                authError = null
            )
        }
        securePreferences.isAppLocked = false
        securePreferences.recordActivity()
    }

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
     * Verifies PIN entry.
     * Returns true for standard unlock, triggers duress mode if duress PIN detected.
     */
    fun verifyPin(pin: String) {
        when (val result = pinAuthManager.verifyPin(pin)) {
            is PinAuthManager.PinResult.Success -> {
                onAuthSuccess(isDuress = false)
            }
            is PinAuthManager.PinResult.DuressTriggered -> {
                onAuthSuccess(isDuress = true)
                // Optionally trigger panic
                viewModelScope.launch {
                    panicManager.executePanic(PanicManager.PanicLevel.LOCK_ONLY)
                }
            }
            is PinAuthManager.PinResult.InvalidPin -> {
                _uiState.update { it.copy(authError = "Invalid PIN") }
            }
            is PinAuthManager.PinResult.PinNotSet -> {
                _uiState.update { it.copy(authError = "PIN not configured") }
            }
        }
    }

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

    fun clearError() {
        _uiState.update { it.copy(authError = null) }
    }

    data class AuthUiState(
        val isAuthenticated: Boolean = false,
        val isDuressMode: Boolean = false,
        val isAuthenticating: Boolean = false,
        val canUseBiometric: Boolean = false,
        val isPinEnabled: Boolean = false,
        val biometricCapability: BiometricAuthManager.BiometricCapability = 
            BiometricAuthManager.BiometricCapability.UNKNOWN,
        val authError: String? = null,
        val isPanicExecuting: Boolean = false,
        val panicExecuted: Boolean = false
    )
}
