# Secure Notepad - Android

A production-ready, **100% offline** Android notepad application with enterprise-grade security features.

## 🔐 Security Features

| Feature | Implementation |
|---------|----------------|
| **Zero Network** | NO `INTERNET` permission - completely offline |
| **Database Encryption** | SQLCipher with 256-bit AES |
| **Key Management** | Android Keystore (hardware-backed) |
| **Authentication** | BiometricPrompt + Custom PIN with duress mode |
| **Defense-in-Depth** | Note content encrypted before SQLCipher storage |
| **UI Security** | `FLAG_SECURE` prevents screenshots |
| **Locked Notes** | Individual notes can be locked (content hidden until unlocked) |
| **Panic Button** | Emergency lock & cryptographic wipe |
| **Duress PIN** | Separate PIN triggers panic mode silently |
| **Obfuscation** | Aggressive ProGuard/R8 rules |

## 🏗️ Architecture

```
secure-notepad/
├── app/                    # Main application module
│   ├── presentation/       # UI layer (Compose, ViewModels)
│   ├── domain/             # Business logic
│   ├── data/               # Repository, Room database
│   └── security/           # App-specific security (Panic, Backup)
├── core/
│   └── security/           # Security module (reusable)
│       ├── CryptoManager   # AES-256-GCM encryption
│       ├── BiometricAuthManager
│       ├── PinAuthManager  # Custom PIN + duress mode
│       ├── SecurePreferences
│       └── DatabaseKeyManager
└── config/
    └── detekt/             # Static analysis rules
```

**Tech Stack:**
- **Clean Architecture** with Presentation, Domain, Data layers
- **MVVM** pattern with Jetpack Compose
- **Hilt** for dependency injection
- **Room** with SQLCipher for encrypted persistence
- **Material 3** design system
- **Detekt** for static code analysis

## 📱 Requirements

- Android 8.0 (API 26) or higher
- Device with biometric capability (fingerprint/face) recommended

## 🔧 Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run unit tests
./gradlew :core:security:test

# Run static analysis
./gradlew detekt
```

## 📦 Pre-built APK

Signed release APKs are available in the `releases/` directory:
- `SecureNotes-v2.1.0.apk` - Latest with PIN/duress mode

## 🔑 Encryption Flow

```
Android Keystore → CryptoManager → SQLCipher Passphrase
                         ↓
              Note Content Encryption (AES-256-GCM)
                         ↓
              SQLCipher Encrypted Database
```

## 🧪 Testing

| Test Type | Location |
|-----------|----------|
| Unit Tests | `core/security/src/test/` |
| Migration Tests | `app/src/androidTest/` |
| Static Analysis | Detekt with security rules |

## 📄 License

MIT License - See LICENSE file for details.
