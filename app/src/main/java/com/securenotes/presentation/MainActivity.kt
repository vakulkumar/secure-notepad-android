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
import com.securenotes.core.security.BiometricAuthManager
import com.securenotes.presentation.navigation.SecureNotesNavigation
import com.securenotes.presentation.ui.theme.SecureNotepadTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for Secure Notes.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // SECURITY: Enable FLAG_SECURE to prevent screenshots and screen recording
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
                    SecureNotesNavigation(
                        biometricAuthManager = biometricAuthManager
                    )
                }
            }
        }
    }
}
