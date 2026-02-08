package com.securenotes.presentation.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.securenotes.presentation.ui.components.MarkdownText
import com.securenotes.presentation.viewmodel.NoteEditorViewModel

/**
 * Note editor screen with markdown preview and lock toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = uiState.hasUnsavedChanges) {
        viewModel.saveNote()
        onNavigateBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isNewNote) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.hasUnsavedChanges) {
                                viewModel.saveNote()
                            }
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Preview toggle
                    IconButton(onClick = { viewModel.togglePreviewMode() }) {
                        Icon(
                            imageVector = if (uiState.isPreviewMode) 
                                Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (uiState.isPreviewMode) "Edit" else "Preview",
                            tint = if (uiState.isPreviewMode) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Lock toggle
                    IconButton(onClick = { viewModel.toggleLock() }) {
                        Icon(
                            imageVector = if (uiState.isLocked) 
                                Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (uiState.isLocked) "Locked" else "Unlocked",
                            tint = if (uiState.isLocked) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Favorite toggle
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) 
                                Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.isFavorite) 
                                MaterialTheme.colorScheme.error 
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Save button
                    IconButton(
                        onClick = {
                            viewModel.saveNote()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .imePadding()
        ) {
            // Title input (always editable)
            BasicTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = MaterialTheme.typography.headlineSmall.fontWeight
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (uiState.title.isEmpty()) {
                        Text(
                            text = "Title",
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = MaterialTheme.typography.headlineSmall.fontWeight
                            )
                        )
                    }
                    innerTextField()
                }
            )

            // Content - Edit mode
            AnimatedVisibility(
                visible = !uiState.isPreviewMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BasicTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (uiState.content.isEmpty()) {
                            Text(
                                text = "Start typing... Use **bold**, *italic*, # headings, - lists",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Content - Preview mode (Markdown rendered)
            AnimatedVisibility(
                visible = uiState.isPreviewMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MarkdownText(
                    text = uiState.content.ifEmpty { "*No content to preview*" },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}
