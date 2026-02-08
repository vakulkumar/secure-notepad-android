package com.securenotes.core.security.di

import com.securenotes.core.security.RealTimeProvider
import com.securenotes.core.security.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing security-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindTimeProvider(
        realTimeProvider: RealTimeProvider
    ): TimeProvider
}
