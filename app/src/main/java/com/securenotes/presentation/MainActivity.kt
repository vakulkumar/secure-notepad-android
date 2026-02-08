package com.securenotes.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.securenotes.presentation.navigation.SecureNotesNavigation
import com.securenotes.presentation.ui.theme.SecureNotepadTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Secure Notes.
 * 
 * Extends FragmentActivity (required by BiometricPrompt).
 * 
 * Security Features:
 * - FLAG_SECURE: Prevents screenshots, screen recording, and 
 *   showing content in recent apps switcher
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // SECURITY: Enable FLAG_SECURE to prevent screenshots and screen recording
        // This also hides the app content in the recent apps switcher
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        enableEdgeToEdge()
        
        setContent {
            SecureNotepadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecureNotesNavigation()
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Additional security: You could lock the app here after a timeout
    }
    
    override fun onStop() {
        super.onStop()
        // The app will require re-authentication when resumed
        // This is handled by the ViewModel and SecurePreferences
    }
}
