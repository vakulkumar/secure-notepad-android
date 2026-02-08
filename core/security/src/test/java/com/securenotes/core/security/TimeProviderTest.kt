package com.securenotes.core.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TimeProvider implementations.
 */
class TimeProviderTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider

    @Before
    fun setup() {
        fakeTimeProvider = FakeTimeProvider()
    }

    @Test
    fun `FakeTimeProvider returns set time`() {
        fakeTimeProvider.setTime(1000L)
        assertEquals(1000L, fakeTimeProvider.now())
    }

    @Test
    fun `FakeTimeProvider advances time correctly`() {
        fakeTimeProvider.setTime(1000L)
        fakeTimeProvider.advanceBy(500L)
        assertEquals(1500L, fakeTimeProvider.now())
    }

    @Test
    fun `auto-lock logic with FakeTimeProvider`() {
        // Simulate auto-lock timeout logic
        val timeout = 5 * 60 * 1000L // 5 minutes
        val lastLockTime = 1000L
        
        fakeTimeProvider.setTime(lastLockTime)
        
        // Time hasn't passed timeout
        fakeTimeProvider.setTime(lastLockTime + 1000L)
        assertFalse(shouldAutoLock(fakeTimeProvider.now(), lastLockTime, timeout))
        
        // Time has passed timeout
        fakeTimeProvider.setTime(lastLockTime + timeout + 1)
        assertTrue(shouldAutoLock(fakeTimeProvider.now(), lastLockTime, timeout))
    }
    
    private fun shouldAutoLock(currentTime: Long, lastLockTime: Long, timeout: Long): Boolean {
        return (currentTime - lastLockTime) > timeout
    }
}
