package com.securenotes.domain.model

/**
 * Extension of Note with decryption status information.
 * Used when we need to track if a note had decryption issues.
 */
data class NoteWithStatus(
    val note: Note,
    val hasDecryptionError: Boolean = false,
    val error: DecryptionError? = null
) {
    /**
     * Returns true if this note is usable (no fatal decryption errors).
     */
    val isUsable: Boolean get() = !hasDecryptionError
    
    companion object {
        fun success(note: Note) = NoteWithStatus(note, hasDecryptionError = false)
        
        fun withError(note: Note, error: DecryptionError) = NoteWithStatus(
            note = note,
            hasDecryptionError = true,
            error = error
        )
    }
}
