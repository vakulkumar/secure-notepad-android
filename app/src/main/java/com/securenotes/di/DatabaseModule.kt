package com.securenotes.di

import android.content.Context
import androidx.room.Room
import com.securenotes.data.local.NoteDatabase
import com.securenotes.data.local.dao.NoteDao
import com.securenotes.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Hilt module providing Room database with SQLCipher encryption.
 * 
 * Encryption Flow:
 * 1. DatabaseKeyManager retrieves passphrase from CryptoManager
 * 2. CryptoManager derives passphrase from Android Keystore key
 * 3. SupportFactory configures SQLCipher with this passphrase
 * 4. Room database uses the encrypted SQLite through SQLCipher
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideNoteDatabase(
        @ApplicationContext context: Context,
        databaseKeyManager: DatabaseKeyManager
    ): NoteDatabase {
        // Get passphrase from Keystore-backed key manager
        val passphrase = databaseKeyManager.getDatabasePassphraseBytes()
        
        // Create SQLCipher factory with the passphrase
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            NoteDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideNoteDao(database: NoteDatabase): NoteDao {
        return database.noteDao()
    }
}
