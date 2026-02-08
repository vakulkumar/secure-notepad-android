package com.securenotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing notes in the encrypted SQLCipher database.
 * 
 * Note: The content is already encrypted by CryptoManager before storage
 * for defense-in-depth (double encryption with SQLCipher).
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Note title (encrypted before storage) */
    val title: String,
    
    /** Note content (encrypted before storage) */
    val content: String,
    
    /** Creation timestamp in milliseconds */
    val createdAt: Long,
    
    /** Last modification timestamp in milliseconds */
    val updatedAt: Long,
    
    /** Whether the note is marked as favorite */
    val isFavorite: Boolean = false,
    
    /** Color tag for the note (stored as color int) */
    val colorTag: Int? = null,
    
    // ========== Feature 1: Recycle Bin ==========
    
    /** Whether the note is soft-deleted (in trash) */
    val isDeleted: Boolean = false,
    
    /** Timestamp when the note was deleted (for auto-cleanup) */
    val deletedAt: Long? = null,
    
    // ========== Feature 2: Biometric Lock ==========
    
    /** Whether the note requires biometric authentication to view */
    val isLocked: Boolean = false
)
