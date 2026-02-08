package com.securenotes.di

import android.content.Context
import androidx.room.Room
import com.securenotes.core.security.DatabaseKeyManager
import com.securenotes.data.local.NoteDatabase
import com.securenotes.data.local.dao.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Hilt module providing Room database with SQLCipher encryption.
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
        val passphrase = databaseKeyManager.getDatabasePassphraseBytes()
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            NoteDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(NoteDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideNoteDao(database: NoteDatabase): NoteDao {
        return database.noteDao()
    }
}
