package com.securenotes.di

import com.securenotes.core.security.CryptoManager
import com.securenotes.data.local.dao.NoteDao
import com.securenotes.data.repository.NoteRepositoryImpl
import com.securenotes.domain.repository.NoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao,
        cryptoManager: CryptoManager
    ): NoteRepository {
        return NoteRepositoryImpl(noteDao, cryptoManager)
    }
}
