package com.securenotes.domain.model

/**
 * Sealed class representing the result of decryption operations.
 * Propagates specific failure reasons to enable proper UI handling.
 * 
 * SENIOR PATTERN: Use Result types instead of silent fallbacks
 * to enable proper error propagation and recovery flows.
 */
sealed class DecryptionResult<out T> {
    
    /**
     * Decryption succeeded with the plaintext content.
     */
    data class Success<T>(val data: T) : DecryptionResult<T>()
    
    /**
     * Decryption failed due to a specific error.
     */
    data class Failure(val error: DecryptionError) : DecryptionResult<Nothing>()
    
    /**
     * Returns the data if success, or null if failure.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    /**
     * Returns the data if success, or the default if failure.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> default
    }
    
    /**
     * Returns the error if failure, or null if success.
     */
    fun errorOrNull(): DecryptionError? = when (this) {
        is Success -> null
        is Failure -> error
    }
    
    /**
     * Maps the success value using the provided transform.
     */
    inline fun <R> map(transform: (T) -> R): DecryptionResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
}

/**
 * Specific decryption errors for proper handling in the UI layer.
 */
enum class DecryptionError {
    /**
     * Key was invalidated (e.g., user removed biometrics, factory reset).
     * Recovery: User must restore from backup or acknowledge data loss.
     */
    KEY_INVALIDATED,
    
    /**
     * Authentication required before decryption can proceed.
     * Recovery: Prompt user to re-authenticate.
     */
    AUTHENTICATION_REQUIRED,
    
    /**
     * Data is corrupted and cannot be decrypted.
     * Recovery: Restore from backup.
     */
    DATA_CORRUPTED,
    
    /**
     * Unknown error during decryption.
     */
    UNKNOWN
}
