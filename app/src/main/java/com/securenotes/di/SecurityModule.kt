package com.securenotes.di

import android.content.Context
import com.securenotes.security.CryptoManager
import com.securenotes.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideCryptoManager(
        @ApplicationContext context: Context
    ): CryptoManager {
        return CryptoManager(context)
    }
    
    @Provides
    @Singleton
    fun provideDatabaseKeyManager(
        cryptoManager: CryptoManager
    ): DatabaseKeyManager {
        return DatabaseKeyManager(cryptoManager)
    }
}
