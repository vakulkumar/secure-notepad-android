package com.securenotes.core.security

/**
 * TimeProvider interface for testable time-dependent operations.
 * 
 * Use this instead of System.currentTimeMillis() directly to enable
 * unit testing of time-based logic (e.g., auto-lock, session timeout).
 */
interface TimeProvider {
    /**
     * Returns the current time in milliseconds since epoch.
     */
    fun now(): Long
}
