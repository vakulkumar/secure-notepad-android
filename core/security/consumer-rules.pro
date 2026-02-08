# Consumer ProGuard rules for :core:security module

# Keep all public classes in this module
-keep class com.securenotes.core.security.** { *; }

# Android Keystore
-keep class android.security.keystore.** { *; }
