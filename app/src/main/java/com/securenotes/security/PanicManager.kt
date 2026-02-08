package com.securenotes.security

import android.content.Context
import com.securenotes.core.security.CryptoManager
import com.securenotes.core.security.SecurePreferences
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
 * Can be triggered by:
 * - A secret gesture
 * - A duress PIN
 * - A hardware button combination
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
     */
    suspend fun executePanic(level: PanicLevel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when (level) {
                PanicLevel.LOCK_ONLY -> {
                    lockApp()
                }
                PanicLevel.CRYPTOGRAPHIC_WIPE -> {
                    lockApp()
                    cryptoManager.deleteAllKeys()
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

    private fun lockApp() {
        securePreferences.isAppLocked = true
        securePreferences.lastLockTime = 0L
    }

    private fun deleteDatabase() {
        context.deleteDatabase(DATABASE_NAME)
        
        val databasesDir = File(context.applicationInfo.dataDir, "databases")
        if (databasesDir.exists()) {
            databasesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(DATABASE_NAME)) {
                    file.delete()
                }
            }
        }
    }

    private fun deleteAllData() {
        val databasesDir = File(context.applicationInfo.dataDir, "databases")
        deleteRecursively(databasesDir)
        
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        deleteRecursively(prefsDir)
        
        val filesDir = context.filesDir
        deleteRecursively(filesDir)
        
        val cacheDir = context.cacheDir
        deleteRecursively(cacheDir)
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        file.delete()
    }

    fun isPanicWipeEnabled(): Boolean {
        return securePreferences.isPanicWipeEnabled
    }

    fun setPanicWipeEnabled(enabled: Boolean) {
        securePreferences.isPanicWipeEnabled = enabled
    }
}
