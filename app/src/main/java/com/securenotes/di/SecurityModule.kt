package com.securenotes.di

import com.securenotes.security.CryptoManager
import com.securenotes.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing security-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager {
        return CryptoManager()
    }
    
    @Provides
    @Singleton
    fun provideDatabaseKeyManager(
        cryptoManager: CryptoManager
    ): DatabaseKeyManager {
        return DatabaseKeyManager(cryptoManager)
    }
}
