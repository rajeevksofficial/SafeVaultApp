# SafeVault 🔐

**SafeVault** is a privacy-first, offline Android credential and sensitive note management application designed to run on **Android 6.0 (Marshmallow, API 23)** and all newer Android versions (up to Android 15/16).

SafeVault stores passwords, portal logins (Google, GitHub, Banking, Netflix, Social), and confidential notes (2FA recovery codes, transaction PINs, security questions) with **hardware-backed AES-256 GCM encryption** and **PBKDF2 Master PIN protection** without any cloud or internet dependencies.

---

## 🌟 Key Features

### 🛡️ Military-Grade Local Encryption & Security
- **AES-256 GCM Authenticated Encryption**:
  - Encrypts sensitive fields (passwords, notes) using `AES/GCM/NoPadding` backed by `AndroidKeyStore`.
  - Every record utilizes an isolated, cryptographically generated 12-byte Initialization Vector (IV).
- **Master PIN Protection**:
  - Derives key material with **PBKDF2WithHmacSHA256** using 65,536 iterations and a 16-byte random salt.
- **Change Master PIN Provision**:
  - Secure 3-step verification flow: verify current PIN &rarr; enter new PIN &rarr; confirm new PIN.
  - Generates fresh 16-byte PBKDF2 salt with continuous hardware Keystore access for stored secrets.
- **Anti-Snoop & Screen Protection**:
  - Built-in `FLAG_SECURE` prevents screenshots, screen recording, and previews in Android's recent apps switcher.
- **Auto-Clearing Sensitive Clipboard**:
  - Flags copied credentials with Android 13+ `EXTRA_IS_SENSITIVE`.
  - Automatically wipes clipboard contents after 30 seconds to prevent background app snooping.

---

### 📊 Interactive Tabular View (Table Format)
- **Seamless View Switcher**:
  - Toggle between **🗂️ Cards View** and **📊 Table View** with dedicated toggle pills showing live filtered counts.
- **Sorted by Title by Default**:
  - The table view automatically orders all credentials **alphabetically by Title (A → Z)** using case-insensitive sorting.
- **Interactive Sticky Header & Tap-to-Sort**:
  - Sticky header row with synchronized horizontal and vertical scrolling.
  - Tap column headers (**TITLE**, **PORTAL**, **USERNAME**, **CATEGORY**, **STRENGTH**, **FAV**) to toggle between Ascending (`▲`) and Descending (`▼`) sorting on the fly with visual highlight indicators.
- **8 Data Columns**:
  1. **`#`**: Consecutive row index (1, 2, 3...).
  2. **`Title`**: Credential title with colored avatar icon.
  3. **`Portal / URL`**: Web portal or service domain.
  4. **`Username / Email`**: Account identifier with a 1-tap copy button.
  5. **`Password`**: Masked by default (`••••••••`) with an **eye toggle** to reveal/hide in-place and a **copy** button.
  6. **`Category`**: Color-coded category badge pill (*Social, Email, Banking, Work, Shopping, Other*).
  7. **`Strength`**: Visual entropy strength meter badge (*Weak, Medium, Strong, Very Strong*).
  8. **`Fav`**: Interactive star button to favorite/unfavorite credentials instantly.
- **Direct Row Navigation**: Tap any row to open the complete details and editing screen.
- **Zebra Striping**: Alternating row backgrounds for high readability.

---

### 📥 CSV Import Provision (Bulk Data Loading)
- **Bulk Loading**: Import dozens or hundreds of credentials in a single step from any standard `.csv` file.
- **RFC 4180 Compliant Parser**:
  - Accurately handles commas within quotes, multiline notes, escaped double quotes (`""`), and UTF-8 encoding.
- **Intelligent Header Detection**:
  - Flexible column matching for `title`, `portal` / `url` / `website`, `username` / `email` / `login`, `password`, `category`, and `notes`.
- **Automatic AES-256 Encryption**:
  - Automatically generates unique 12-byte IVs, computes password strength entropy, and encrypts all imported credentials before writing to the local database.
- **Sample Template Helper**:
  - Includes a one-tap **"Copy Sample Template"** button in the import dialog for easy CSV creation.

---

### 📤 CSV Export Provision (Save & Share)
- **Full Credential Export**:
  - Decrypts and exports all vault records into a standard RFC 4180 CSV document.
- **Save to Storage**:
  - Uses Android's Storage Access Framework (`CreateDocument`), allowing you to save the `.csv` file directly to Downloads, Documents, or external storage without requiring legacy storage permissions.
- **Native File Sharing via FileProvider**:
  - Attaches the export as a true `.csv` file (`SafeVault_Export_YYYYMMDD_HHmmss.csv`) with MIME type `text/csv`.
  - Securely shares across **Bluetooth**, **Google Drive**, **Quick Share / Nearby Share**, **WhatsApp**, **Email**, and file managers without getting converted into `.html` or plain text wrappers.
- **Security Confirmation**:
  - Displays a security advisory reminder before export regarding plaintext password protection.

---

### 🎲 Password Tools & Presets
- **Quick Portal Presets**:
  - One-tap chips for *Gmail, Facebook, Instagram, GitHub, Twitter, Netflix, Amazon, Banking* auto-populate portal URLs, icons, and categories.
- **Secure Password Generator**:
  - Cryptographically secure `SecureRandom` generator with adjustable length (8–32 chars) and character toggles (uppercase, lowercase, numbers, symbols).
- **Entropy & Strength Evaluator**:
  - Evaluates password strength in real time.
- **Fast Search & Category Filters**:
  - Instant live search by title, portal, or username.
  - Category filter chips: *All Items, ⭐ Favorites, Social, Email, Banking, Work, Shopping, Other*.

---

## 🏛️ Architecture & Tech Stack

- **Language**: Kotlin 2.0
- **UI Architecture**: ViewBinding, Material Components 3, Custom XML Layouts, RecyclerView (Cards + Tabular Grid)
- **Database**: SQLite (`VaultDatabaseHelper.kt`) with thread-safe operations and batch transactions
- **File Sharing**: Android `FileProvider` with scoped cache sharing
- **Crypto & Keystore**:
  - `AndroidKeyStore` (`AES/GCM/NoPadding`)
  - `SecretKeyFactory` (`PBKDF2WithHmacSHA256`, 65,536 iterations)
  - `java.security.SecureRandom`
  - Pure-Kotlin RFC 4648 Base64 engine (`SafeBase64.kt`)
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
│   │   │   │   ├── CsvExporter.kt               # RFC 4180 CSV export & decrypt engine
│   │   │   │   ├── CsvImporter.kt               # RFC 4180 CSV parser & auto-encryption
│   │   │   │   ├── MainActivity.kt              # Dashboard, search, card & table view switcher
│   │   │   │   ├── PasswordUtils.kt             # SecureRandom generator & entropy meter
│   │   │   │   ├── SafeBase64.kt                # Pure-Kotlin RFC 4648 Base64 codec
│   │   │   │   ├── SecurityManager.kt           # AES-256 GCM & PBKDF2 crypto core
│   │   │   │   ├── VaultDatabaseHelper.kt       # SQLite database store & batch CRUD
│   │   │   │   ├── VaultItem.kt                 # Data model
│   │   │   │   ├── VaultItemAdapter.kt          # Card layout RecyclerView adapter
│   │   │   │   └── VaultTableAdapter.kt         # Tabular format data table adapter
│   │   │   ├── res/
│   │   │   │   ├── drawable/                    # Vector icons, chips, and table row/header backgrounds
│   │   │   │   ├── layout/                      # Activity and row layouts (cards, table rows)
│   │   │   │   ├── values/                      # Themes, colors, strings
│   │   │   │   └── xml/
│   │   │   │       └── file_paths.xml           # FileProvider paths for CSV export
│   │   │   └── AndroidManifest.xml              # Manifest with FileProvider & activities
│   │   └── test/java/com/safevault/app/
│   │       ├── CsvExporterTest.kt               # CSV export & RFC 4180 escaping tests
│   │       ├── CsvImporterTest.kt               # CSV import & round-trip tests
│   │       ├── SafeVaultSecurityTest.kt         # AES-256 GCM & PBKDF2 JUnit tests
│   │       └── TableSortTest.kt                 # Table view title sorting tests
│   └── build.gradle.kts
├── SafeVaultApp.apk                             # Pre-compiled standalone APK
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📝 CSV Format & Sample Template

When importing credentials, SafeVault supports standard CSV formats:

```csv
title,portal,username,password,category,notes
GitHub,github.com,octocat,G1t#Hub_P@ss!99,Work,Recovery codes stored in safe
Google Account,accounts.google.com,user@gmail.com,G00gle!Sec#2026,Email,Primary personal account
Netflix,netflix.com,family@home.com,Net#Fl1x$Stream,Social,Standard 4K plan
HDFC NetBanking,netbanking.hdfcbank.com,hdfc_user_44,Hdfc#Bank!Pass99,Banking,Customer ID: 12345678
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
`app/build/outputs/apk/debug/app-debug.apk` (and `SafeVaultApp.apk` at the repository root).

---

## 🔑 Initial Demo Credentials
On first install, the app launches with default starter records (*Facebook, Gmail, Netflix, HDFC NetBanking*) and a default Master PIN of:
`1234`
*(You can reset or change credentials and PIN at any time)*.

---

## 📄 License
Open source under the [Apache License 2.0](LICENSE).
