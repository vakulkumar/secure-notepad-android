package com.securenotes.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PanicManager provides emergency security features:
 * - Instant app lock
 * - Complete data wipe (nuclear option)
 * 
 * This is a scaffold for the "Panic Button" feature.
 * In a production app, this could be triggered by:
 * - A secret gesture
 * - A duress PIN
 * - A hardware button combination
 * - Remote wipe command (requires network, not applicable here)
 */
@Singleton
class PanicManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    private val securePreferences: SecurePreferences
) {
    
    companion object {
        private const val DATABASE_NAME = "secure_notes.db"
    }
    
    /**
     * Represents the severity of panic action.
     */
    enum class PanicLevel {
        /** Just lock the app, data remains */
        LOCK_ONLY,
        /** Delete all encryption keys and database, making data unrecoverable */
        CRYPTOGRAPHIC_WIPE,
        /** Delete all data files completely */
        FULL_WIPE
    }

    /**
     * Executes panic action based on the specified level.
     * 
     * @param level The severity of the panic action
     * @return Result indicating success or failure
     */
    suspend fun executePanic(level: PanicLevel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when (level) {
                PanicLevel.LOCK_ONLY -> {
                    lockApp()
                }
                PanicLevel.CRYPTOGRAPHIC_WIPE -> {
                    lockApp()
                    // Delete encryption keys
                    cryptoManager.deleteAllKeys()
                    // Also delete the database so app can start fresh
                    // Without this, the app would crash trying to open the 
                    // encrypted database with a new (different) key
                    deleteDatabase()
                }
                PanicLevel.FULL_WIPE -> {
                    lockApp()
                    cryptoManager.deleteAllKeys()
                    deleteAllData()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Locks the app immediately.
     */
    private fun lockApp() {
        securePreferences.isAppLocked = true
        securePreferences.lastLockTime = 0L // Force re-authentication
    }

    /**
     * Deletes the encrypted database file.
     * This is necessary after cryptographic wipe to allow the app to restart cleanly.
     */
    private fun deleteDatabase() {
        // Delete the main database file
        context.deleteDatabase(DATABASE_NAME)
        
        // Also delete any SQLCipher journal files
        val databasesDir = File(context.applicationInfo.dataDir, "databases")
        if (databasesDir.exists()) {
            databasesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(DATABASE_NAME)) {
                    file.delete()
                }
            }
        }
    }

    /**
     * Deletes all application data.
     */
    private fun deleteAllData() {
        // Delete databases
        val databasesDir = File(context.applicationInfo.dataDir, "databases")
        deleteRecursively(databasesDir)
        
        // Delete shared preferences
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        deleteRecursively(prefsDir)
        
        // Delete files
        val filesDir = context.filesDir
        deleteRecursively(filesDir)
        
        // Delete cache
        val cacheDir = context.cacheDir
        deleteRecursively(cacheDir)
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        file.delete()
    }

    /**
     * Checks if panic wipe is enabled by user.
     */
    fun isPanicWipeEnabled(): Boolean {
        return securePreferences.isPanicWipeEnabled
    }

    /**
     * Enables/disables panic wipe feature.
     */
    fun setPanicWipeEnabled(enabled: Boolean) {
        securePreferences.isPanicWipeEnabled = enabled
    }
}
