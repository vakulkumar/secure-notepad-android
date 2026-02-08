package com.securenotes.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SecurePreferences provides encrypted SharedPreferences for storing lightweight settings.
 * 
 * Uses EncryptedSharedPreferences backed by Android Keystore for:
 * - AES-256-GCM encryption for values
 * - AES-256-SIV encryption for keys (deterministic)
 * - Hardware-backed master key when available
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_FILE_NAME = "secure_notes_encrypted_prefs"
        
        // Preference keys
        const val KEY_APP_LOCKED = "app_locked"
        const val KEY_LAST_LOCK_TIME = "last_lock_time"
        const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_PANIC_WIPE_ENABLED = "panic_wipe_enabled"
        const val KEY_FIRST_LAUNCH = "first_launch"
        
        // Default values
        const val DEFAULT_AUTO_LOCK_TIMEOUT = 5 * 60 * 1000L // 5 minutes
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // App Lock State
    var isAppLocked: Boolean
        get() = encryptedPrefs.getBoolean(KEY_APP_LOCKED, true)
        set(value) = encryptedPrefs.edit().putBoolean(KEY_APP_LOCKED, value).apply()

    var lastLockTime: Long
        get() = encryptedPrefs.getLong(KEY_LAST_LOCK_TIME, 0L)
        set(value) = encryptedPrefs.edit().putLong(KEY_LAST_LOCK_TIME, value).apply()

    var autoLockTimeout: Long
        get() = encryptedPrefs.getLong(KEY_AUTO_LOCK_TIMEOUT, DEFAULT_AUTO_LOCK_TIMEOUT)
        set(value) = encryptedPrefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, value).apply()

    var isBiometricEnabled: Boolean
        get() = encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = encryptedPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var isPanicWipeEnabled: Boolean
        get() = encryptedPrefs.getBoolean(KEY_PANIC_WIPE_ENABLED, false)
        set(value) = encryptedPrefs.edit().putBoolean(KEY_PANIC_WIPE_ENABLED, value).apply()

    var isFirstLaunch: Boolean
        get() = encryptedPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = encryptedPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    /**
     * Checks if the app should auto-lock based on timeout.
     */
    fun shouldAutoLock(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastLock = lastLockTime
        val timeout = autoLockTimeout
        return (currentTime - lastLock) > timeout
    }

    /**
     * Records the current time as the last activity time.
     */
    fun recordActivity() {
        lastLockTime = System.currentTimeMillis()
    }

    /**
     * Clears all encrypted preferences (used for panic wipe).
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}
