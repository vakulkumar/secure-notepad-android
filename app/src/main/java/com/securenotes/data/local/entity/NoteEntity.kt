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
    val colorTag: Int? = null
)
