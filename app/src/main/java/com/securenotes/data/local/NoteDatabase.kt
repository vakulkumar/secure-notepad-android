package com.securenotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    
    abstract fun noteDao(): NoteDao
    
    companion object {
        const val DATABASE_NAME = "secure_notes_db"
        
        /**
         * Migration from version 1 to 2:
         * - Adds isDeleted column for soft delete (Recycle Bin)
         * - Adds deletedAt column for trash timestamp
         * - Adds isLocked column for biometric lock per note
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isDeleted column with default false (0)
                database.execSQL(
                    "ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
                // Add deletedAt column (nullable)
                database.execSQL(
                    "ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL"
                )
                // Add isLocked column with default false (0)
                database.execSQL(
                    "ALTER TABLE notes ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
