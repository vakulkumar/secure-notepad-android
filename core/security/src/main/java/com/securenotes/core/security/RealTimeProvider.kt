package com.securenotes.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of TimeProvider.
 * Returns the actual system time.
 */
@Singleton
class RealTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
