# ============================================================================
# SECURE NOTEPAD - PROGUARD/R8 RULES
# Production-ready obfuscation configuration for maximum code protection
# ============================================================================

# --------------------------------------------------------------------------
# GENERAL ANDROID RULES
# --------------------------------------------------------------------------

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --------------------------------------------------------------------------
# KOTLIN SPECIFIC RULES
# --------------------------------------------------------------------------

# Keep Kotlin Metadata for reflection (required for Hilt)
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --------------------------------------------------------------------------
# HILT / DAGGER RULES
# --------------------------------------------------------------------------

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager.FragmentContextWrapper { *; }

# Keep Hilt generated components
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# --------------------------------------------------------------------------
# ROOM DATABASE RULES
# --------------------------------------------------------------------------

-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Room uses reflection for type converters
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# --------------------------------------------------------------------------
# SQLCIPHER RULES
# --------------------------------------------------------------------------

-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.* { *; }
-dontwarn net.sqlcipher.**

# --------------------------------------------------------------------------
# SECURITY CRYPTO RULES
# --------------------------------------------------------------------------

-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# --------------------------------------------------------------------------
# BIOMETRIC RULES
# --------------------------------------------------------------------------

-keep class androidx.biometric.** { *; }

# --------------------------------------------------------------------------
# JETPACK COMPOSE RULES
# --------------------------------------------------------------------------

# Keep Compose runtime classes
-keep class androidx.compose.runtime.** { *; }

# Keep @Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }

# --------------------------------------------------------------------------
# APPLICATION SPECIFIC RULES
# --------------------------------------------------------------------------

# Keep domain models (needed for Room entity mapping)
-keep class com.securenotes.domain.model.** { *; }
-keep class com.securenotes.data.local.entity.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# --------------------------------------------------------------------------
# AGGRESSIVE OBFUSCATION
# --------------------------------------------------------------------------

# Enable aggressive optimizations
-optimizationpasses 5
-overloadaggressively
-repackageclasses 'o'
-allowaccessmodification

# Obfuscate package private access
-flattenpackagehierarchy

# Remove debug info
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# --------------------------------------------------------------------------
# PREVENT DECOMPILATION INSIGHTS
# --------------------------------------------------------------------------

# Obfuscate resource names
-adaptresourcefilenames
-adaptresourcefilecontents

# Remove unused code aggressively
-dontnote **
