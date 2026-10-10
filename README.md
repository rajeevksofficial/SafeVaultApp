# SafeVault 🔐

**SafeVault** is a privacy-first, offline Android credential and sensitive note management application designed to run on **Android 6.0 (Marshmallow, API 23)** and all newer Android versions (up to Android 15/16).

SafeVault stores passwords, portal logins (Facebook, Gmail, Netflix, banking), and confidential notes (2FA recovery codes, transaction PINs, security questions) with **hardware-backed AES-256 GCM encryption** and **PBKDF2 Master PIN protection** without any cloud dependencies.

---

## 🌟 Key Features

- **🛡️ AES-256 GCM Authenticated Encryption**:
  - Encrypts sensitive fields (passwords, notes) using `AES/GCM/NoPadding` backed by `AndroidKeyStore`.
  - Every record utilizes an isolated, cryptographically generated 12-byte Initialization Vector (IV).
- **🔒 Master PIN Protection**:
  - Derives key material with **PBKDF2WithHmacSHA256** using 65,536 iterations and a 16-byte random salt.
- **📱 Android 6.0 (API 23+) Backwards Compatible**:
  - `minSdk = 23`, `compileSdk = 36`, `targetSdk = 36`.
  - Implements a pure-Kotlin RFC 4648 Base64 codec (`SafeBase64.kt`), ensuring zero dependency on Android 8.0+ (`java.util.Base64`) or unmocked JVM stub dependencies in JUnit tests.
- **🔑 Change Master PIN Provision**:
  - Secure 3-step verification flow: verify current PIN &rarr; enter new PIN &rarr; confirm new PIN.
  - Generates fresh 16-byte PBKDF2 salt with continuous hardware Keystore access for stored secrets.
- **🚫 Anti-Snoop & Screen Protection**:
  - Built-in `FLAG_SECURE` prevents screenshots, screen recording, and previews in Android's recent apps switcher.
- **📋 Auto-Clearing Sensitive Clipboard**:
  - Flags copied credentials with Android 13+ `EXTRA_IS_SENSITIVE`.
  - Automatically clears clipboard content after 30 seconds to prevent background app snooping.
- **⚡ Quick Portal Presets**:
  - One-tap chips for *Gmail, Facebook, Instagram, GitHub, Twitter, Netflix, Amazon, Banking* auto-populate portal URLs, icons, and categories.
- **🎲 Secure Password Generator & Strength Meter**:
  - `SecureRandom` password generator with length slider (8–32 chars) and character toggles (uppercase, lowercase, numbers, symbols).
  - Real-time entropy evaluator categorized as *Weak, Medium, Strong, Very Strong*.
- **🔍 Fast Search & Category Filters**:
  - Live filtering by title, username, or portal URL.
  - Horizontal filter chips: *All Items, ⭐ Favorites, Social, Email, Banking, Work, Shopping, Other*.

---

## 🏛️ Architecture & Tech Stack

- **Language**: Kotlin 2.0
- **UI Architecture**: ViewBinding, Material Components 3, XML Layouts
- **Database**: SQLite (`VaultDatabaseHelper.kt`) with thread-safe access
- **Crypto & Keystore**:
  - `AndroidKeyStore` (`AES/GCM/NoPadding`)
  - `SecretKeyFactory` (`PBKDF2WithHmacSHA256`)
  - `java.security.SecureRandom`
  - RFC 4648 Base64 engine (`SafeBase64.kt`)
- **Compatibility**: Android 6.0 Marshmallow (API 23) to Android 15/16 (API 36)

---

## 📂 Project Structure

```
SafeVaultApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/safevault/app/
│   │   │   │   ├── AddEditCredentialActivity.kt  # Add/Edit form & Password Generator
│   │   │   │   ├── AuthActivity.kt              # Master PIN authentication & setup
│   │   │   │   ├── BaseSecureActivity.kt        # FLAG_SECURE screen protection
│   │   │   │   ├── CategoryAdapter.kt           # Horizontal category filter chips
│   │   │   │   ├── ChangePinActivity.kt         # Change Master PIN (3-step flow)
│   │   │   │   ├── ClipboardHelper.kt           # Auto-clearing sensitive clipboard
│   │   │   │   ├── CredentialDetailActivity.kt  # Credential view & reveal toggle
│   │   │   │   ├── MainActivity.kt              # Dashboard, search, card list
│   │   │   │   ├── PasswordUtils.kt             # SecureRandom generator & entropy meter
│   │   │   │   ├── SafeBase64.kt                # Pure-Kotlin RFC 4648 Base64 codec
│   │   │   │   ├── SecurityManager.kt           # AES-256 GCM & PBKDF2 crypto core
│   │   │   │   ├── VaultDatabaseHelper.kt       # SQLite database store & CRUD
│   │   │   │   ├── VaultItem.kt                 # Data model
│   │   │   │   └── VaultItemAdapter.kt          # Vault card RecyclerView adapter
│   │   │   ├── res/                             # Layouts, vector drawables, colors, styles
│   │   │   └── AndroidManifest.xml
│   │   └── test/java/com/safevault/app/
│   │       └── SafeVaultSecurityTest.kt         # AES-256 GCM & PBKDF2 JUnit tests
│   └── build.gradle.kts
├── SafeVaultApp.apk                             # Pre-compiled standalone APK (6.72 MB)
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 or JDK 21 (e.g. Android Studio JBR)
- Android SDK with API 36 platforms & build-tools installed

### Build via Gradle
```bash
# Clone the repository
git clone https://github.com/rajeevksofficial/SafeVaultApp.git
cd SafeVaultApp

# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk` (and `SafeVaultApp.apk` at root).

---

## 🔑 Initial Demo Credentials
On first install, the app launches with default starter records (*Facebook, Gmail, Netflix, HDFC NetBanking*) and a default Master PIN of:
`1234`
*(You can reset or change credentials and PIN at any time)*.

---

## 📄 License
Open source under the [Apache License 2.0](LICENSE).
