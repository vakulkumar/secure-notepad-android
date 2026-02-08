package com.securenotes.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for Application-scoped CoroutineScope.
 * Use this for critical operations that must survive ViewModel destruction.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Module providing application-level coroutine scopes.
 * 
 * IMPORTANT: Use ApplicationScope for critical writes (save, encrypt, database ops)
 * that must complete even if the UI is destroyed (e.g., user navigates away).
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopesModule {
    
    /**
     * Provides an application-scoped CoroutineScope that survives ViewModel destruction.
     * Uses SupervisorJob so one child failure doesn't cancel siblings.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
