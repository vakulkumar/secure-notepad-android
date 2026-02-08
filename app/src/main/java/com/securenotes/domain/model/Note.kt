package com.securenotes.domain.model

/**
 * Domain model for a note.
 * 
 * This is the clean, unencrypted representation of a note
 * used throughout the domain and presentation layers.
 */
data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val colorTag: Int? = null,
    
    // Feature 1: Recycle Bin
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    
    // Feature 2: Biometric Lock
    val isLocked: Boolean = false
) {
    companion object {
        /**
         * Creates an empty note for the editor.
         */
        fun empty() = Note(
            title = "",
            content = ""
        )
    }
    
    /**
     * Returns a preview of the content (first 100 characters).
     */
    fun contentPreview(maxLength: Int = 100): String {
        return if (content.length <= maxLength) {
            content
        } else {
            content.take(maxLength) + "..."
        }
    }
    
    /**
     * Checks if the note has any content.
     */
    fun hasContent(): Boolean = title.isNotBlank() || content.isNotBlank()
}
