package com.securenotes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Secure Notes.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SecureNotesApp : Application()
