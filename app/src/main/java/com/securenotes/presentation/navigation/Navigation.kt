package com.securenotes.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securenotes.presentation.ui.screen.AuthScreen
import com.securenotes.presentation.ui.screen.NoteEditorScreen
import com.securenotes.presentation.ui.screen.NoteListScreen

/**
 * Navigation routes for the app.
 */
object Routes {
    const val AUTH = "auth"
    const val NOTE_LIST = "notes"
    const val NOTE_EDITOR = "note/{noteId}"
    
    fun noteEditor(noteId: Long = -1) = "note/$noteId"
}

/**
 * Main navigation component.
 */
@Composable
fun SecureNotesNavigation(
    startAuthenticated: Boolean = false
) {
    val navController = rememberNavController()
    
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
                onCreateNote = {
                    navController.navigate(Routes.noteEditor(-1))
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
    }
}
