package com.securenotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.securenotes.data.local.dao.NoteDao
import com.securenotes.data.local.entity.NoteEntity

/**
 * Room database with SQLCipher encryption.
 * 
 * The database is encrypted using a passphrase derived from Android Keystore.
 * See DatabaseModule for SQLCipher configuration.
 * 
 * Encryption Flow:
 * 1. DatabaseKeyManager gets passphrase from CryptoManager
 * 2. CryptoManager derives passphrase from Keystore-backed key
 * 3. SQLCipher SupportFactory uses passphrase to encrypt all database pages
 * 4. All queries/writes go through SQLCipher transparent encryption
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    
    abstract fun noteDao(): NoteDao
    
    companion object {
        const val DATABASE_NAME = "secure_notes_db"
    }
}
