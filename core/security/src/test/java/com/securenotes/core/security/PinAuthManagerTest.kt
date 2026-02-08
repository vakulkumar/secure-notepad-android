package com.securenotes.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times

class PinAuthManagerTest {

    private lateinit var securePreferences: SecurePreferences
    private lateinit var pinAuthManager: PinAuthManager

    @Before
    fun setUp() {
        securePreferences = mock()
        pinAuthManager = PinAuthManager(securePreferences)
    }

    @Test
    fun `setUserPin stores PBKDF2 hash`() {
        // Given
        val pin = "1234"

        // When
        val result = pinAuthManager.setUserPin(pin)

        // Then
        assertTrue(result)

        val captor = argumentCaptor<String>()
        verify(securePreferences).userPinHash = captor.capture()
        verify(securePreferences).isPinEnabled = true

        val storedHash = captor.firstValue
        // Check format: v2:100000:hexSalt:hexHash
        assertTrue("Hash should start with v2 prefix", storedHash.startsWith("v2:100000:"))
        val parts = storedHash.split(":")
        assertEquals(4, parts.size)
        assertEquals(32, parts[2].length) // 16 bytes salt in hex -> 32 chars
        assertEquals(64, parts[3].length) // 32 bytes hash in hex -> 64 chars
    }

    @Test
    fun `verifyPin verifies legacy SHA-256 hash correctly`() {
        // Given
        val pin = "1234"
        val expectedHash = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"

        whenever(securePreferences.isPinEnabled).thenReturn(true)
        whenever(securePreferences.userPinHash).thenReturn(expectedHash)
        whenever(securePreferences.duressPinHash).thenReturn(null)

        // When
        val result = pinAuthManager.verifyPin(pin)

        // Then
        assertTrue(result is PinAuthManager.PinResult.Success)
    }

    @Test
    fun `verifyPin migrates legacy SHA-256 hash to PBKDF2`() {
        // Given
        val pin = "1234"
        val legacyHash = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"

        whenever(securePreferences.isPinEnabled).thenReturn(true)
        whenever(securePreferences.userPinHash).thenReturn(legacyHash)
        whenever(securePreferences.duressPinHash).thenReturn(null)

        // When
        val result = pinAuthManager.verifyPin(pin)

        // Then
        assertTrue(result is PinAuthManager.PinResult.Success)

        // Verify migration
        val captor = argumentCaptor<String>()
        verify(securePreferences).userPinHash = captor.capture()
        val newHash = captor.firstValue
        assertTrue("New hash should be V2", newHash.startsWith("v2:100000:"))
        assertFalse("New hash should differ from legacy", newHash == legacyHash)
    }

    @Test
    fun `verifyPin verifies legacy duress SHA-256 hash correctly and migrates`() {
        // Given
        val pin = "9999"
        val expectedHash = "888df25ae35772424a560c7152a1de794440e0ea5cfee62828333a456a506e05" // SHA-256 of "9999"

        whenever(securePreferences.isPinEnabled).thenReturn(true)
        whenever(securePreferences.userPinHash).thenReturn("some_other_hash")
        whenever(securePreferences.duressPinHash).thenReturn(expectedHash)

        // When
        val result = pinAuthManager.verifyPin(pin)

        // Then
        assertTrue(result is PinAuthManager.PinResult.DuressTriggered)

        // Verify migration
        val captor = argumentCaptor<String>()
        verify(securePreferences).duressPinHash = captor.capture()
        val newHash = captor.firstValue
        assertTrue("New hash should be V2", newHash.startsWith("v2:100000:"))
    }

    @Test
    fun `verifyPin verifies new PBKDF2 hash`() {
        // Given
        val pin = "5678"
        // Use setUserPin logic to generate a valid V2 hash
        pinAuthManager.setUserPin(pin)

        val captor = argumentCaptor<String>()
        verify(securePreferences).userPinHash = captor.capture()
        val v2Hash = captor.firstValue

        whenever(securePreferences.isPinEnabled).thenReturn(true)
        whenever(securePreferences.userPinHash).thenReturn(v2Hash)
        whenever(securePreferences.duressPinHash).thenReturn(null)

        // When
        val result = pinAuthManager.verifyPin(pin)

        // Then
        assertTrue(result is PinAuthManager.PinResult.Success)

        // Verify NO migration happened (setter called only once during setUserPin)
        verify(securePreferences, times(1)).userPinHash = any()
    }

    @Test
    fun `setDuressPin ensures different hash from user pin`() {
        // Given user pin "1234" is set (V2)
        val pin = "1234"
        pinAuthManager.setUserPin(pin)

        val captor = argumentCaptor<String>()
        verify(securePreferences).userPinHash = captor.capture()
        val userV2Hash = captor.firstValue

        whenever(securePreferences.userPinHash).thenReturn(userV2Hash)

        // When trying to set duress pin same as user pin
        val result = pinAuthManager.setDuressPin(pin)

        // Then
        assertFalse(result)
        // verify duressPinHash was NOT set
        verify(securePreferences, times(0)).duressPinHash = any()
    }
}
