# Secure Notepad - Android

A production-ready, **100% offline** Android notepad application with enterprise-grade security features.

## 🔐 Security Features

| Feature | Implementation |
|---------|----------------|
| **Zero Network** | NO `INTERNET` permission - completely offline |
| **Database Encryption** | SQLCipher with 256-bit AES |
| **Key Management** | Android Keystore (hardware-backed) |
| **Authentication** | BiometricPrompt with PIN fallback |
| **Defense-in-Depth** | Note content encrypted before SQLCipher storage |
| **UI Security** | `FLAG_SECURE` prevents screenshots |
| **Obfuscation** | Aggressive ProGuard/R8 rules |
| **Panic Button** | Emergency lock & cryptographic wipe |

## 🏗️ Architecture

- **Clean Architecture** with Presentation, Domain, Data layers
- **MVVM** pattern with Jetpack Compose
- **Hilt** for dependency injection
- **Room** with SQLCipher for encrypted persistence
- **Material 3** design system

## 📱 Requirements

- Android 8.0 (API 26) or higher
- Device with biometric capability (fingerprint/face) recommended

## 🔧 Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore)
./gradlew assembleRelease
```

## 📦 Pre-built APK

A signed release APK is available in the `releases/` directory.

## 🔑 Encryption Flow

```
Android Keystore → CryptoManager → SQLCipher Passphrase
                         ↓
              Note Content Encryption
                         ↓
              SQLCipher Encrypted Database
```

## 📄 License

MIT License - See LICENSE file for details.
