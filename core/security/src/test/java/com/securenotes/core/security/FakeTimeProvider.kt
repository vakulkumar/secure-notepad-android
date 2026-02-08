package com.securenotes.core.security

/**
 * Fake TimeProvider for unit testing.
 * Allows tests to control the current time.
 */
class FakeTimeProvider : TimeProvider {
    
    private var currentTime: Long = 0L
    
    override fun now(): Long = currentTime
    
    /**
     * Sets the current time for testing.
     */
    fun setTime(time: Long) {
        currentTime = time
    }
    
    /**
     * Advances the current time by the specified amount.
     */
    fun advanceBy(milliseconds: Long) {
        currentTime += milliseconds
    }
}
