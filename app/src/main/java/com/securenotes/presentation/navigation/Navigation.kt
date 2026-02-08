package com.securenotes.presentation.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securenotes.core.security.BiometricAuthManager
import com.securenotes.presentation.ui.screen.AuthScreen
import com.securenotes.presentation.ui.screen.BackupScreen
import com.securenotes.presentation.ui.screen.NoteEditorScreen
import com.securenotes.presentation.ui.screen.NoteListScreen
import com.securenotes.presentation.ui.screen.TrashScreen

/**
 * Navigation routes for the app.
 */
object Routes {
    const val AUTH = "auth"
    const val NOTE_LIST = "notes"
    const val NOTE_EDITOR = "note/{noteId}"
    const val TRASH = "trash"
    const val BACKUP = "backup"
    
    fun noteEditor(noteId: Long = -1) = "note/$noteId"
}

/**
 * Main navigation component.
 */
@Composable
fun SecureNotesNavigation(
    startAuthenticated: Boolean = false,
    biometricAuthManager: BiometricAuthManager
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val startDestination = if (startAuthenticated) Routes.NOTE_LIST else Routes.AUTH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication screen
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.NOTE_LIST) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        
        // Note list screen
        composable(Routes.NOTE_LIST) {
            NoteListScreen(
                onNoteClick = { noteId ->
                    navController.navigate(Routes.noteEditor(noteId))
                },
                onLockedNoteClick = { _, onSuccess ->
                    val fragmentActivity = context.findFragmentActivity()
                    if (fragmentActivity != null) {
                        biometricAuthManager.authenticate(
                            activity = fragmentActivity,
                            onSuccess = onSuccess,
                            onError = { /* Silently fail */ }
                        )
                    } else {
                        onSuccess()
                    }
                },
                onCreateNote = {
                    navController.navigate(Routes.noteEditor(-1))
                },
                onTrashClick = {
                    navController.navigate(Routes.TRASH)
                },
                onBackupClick = {
                    navController.navigate(Routes.BACKUP)
                }
            )
        }
        
        // Note editor screen
        composable(
            route = Routes.NOTE_EDITOR,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            NoteEditorScreen(
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Trash screen
        composable(Routes.TRASH) {
            TrashScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Backup screen
        composable(Routes.BACKUP) {
            BackupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}
