# Deprecated API Reference

Daftar API deprecated yang pernah ditemukan di codebase dan cara migrasinya.
Gunakan dokumen ini sebagai referensi saat menulis kode baru agar tidak menggunakan API yang sudah deprecated.

---

## Material Icons — AutoMirrored

| Deprecated | Replacement |
|---|---|
| `Icons.Default.Backspace` | `Icons.AutoMirrored.Filled.Backspace` |
| `Icons.Default.ReceiptLong` | `Icons.AutoMirrored.Filled.ReceiptLong` |
| `Icons.Default.ListAlt` | `Icons.AutoMirrored.Filled.ListAlt` |
| `Icons.Default.Logout` | `Icons.AutoMirrored.Filled.Logout` |
| `Icons.Default.ArrowBack` | `Icons.AutoMirrored.Filled.ArrowBack` |

**Import:**
```kotlin
// OLD
import androidx.compose.material.icons.filled.Backspace

// NEW
import androidx.compose.material.icons.automirrored.filled.Backspace
```

**Alasan:** Ikon yang memiliki arah (kiri/kanan) perlu di-mirror otomatis untuk RTL layout.

---

## Locale Constructor

| Deprecated | Replacement |
|---|---|
| `Locale("id", "ID")` | `Locale.forLanguageTag("id-ID")` |
| `Locale("id")` | `Locale.forLanguageTag("id")` |

```kotlin
// OLD
val idrFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

// NEW
val idrFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID"))
```

**Alasan:** Constructor `Locale(String, String)` deprecated di Java 19+. `forLanguageTag()` mengikuti standar BCP 47.

---

## EncryptedSharedPreferences & MasterKeys

| Deprecated | Replacement |
|---|---|
| `MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)` | `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()` |
| `EncryptedSharedPreferences.create(name, masterKeyAlias, context, ...)` | `EncryptedSharedPreferences.create(context, name, masterKey, ...)` |

```kotlin
// OLD
import androidx.security.crypto.MasterKeys
val prefs = EncryptedSharedPreferences.create(
    "license_store",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
)

// NEW
import androidx.security.crypto.MasterKey
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
val prefs = EncryptedSharedPreferences.create(
    context,
    "license_store",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
)
```

**Perhatikan:** Urutan parameter berubah — `context` jadi parameter pertama.

---

## Room — fallbackToDestructiveMigration

| Deprecated | Replacement |
|---|---|
| `.fallbackToDestructiveMigration()` | `.fallbackToDestructiveMigration(dropAllTables = true)` |

```kotlin
// OLD
Room.databaseBuilder(context, PosDatabase::class.java, "pos_database")
    .fallbackToDestructiveMigration()
    .build()

// NEW
Room.databaseBuilder(context, PosDatabase::class.java, "pos_database")
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
```

**Alasan:** API baru memaksa developer untuk eksplisit apakah semua tabel harus di-drop atau tidak.

---

## Material 3 — TabRow

| Deprecated | Replacement |
|---|---|
| `TabRow(...)` | `PrimaryTabRow(...)` atau `SecondaryTabRow(...)` |

```kotlin
// OLD
import androidx.compose.material3.TabRow
TabRow(selectedTabIndex = index) { ... }

// NEW
import androidx.compose.material3.PrimaryTabRow
PrimaryTabRow(selectedTabIndex = index) { ... }
```

**Alasan:** Material 3 membedakan antara Primary dan Secondary tab rows untuk konsistensi design system.

---

## Material 3 — MenuAnchorType

| Deprecated | Replacement |
|---|---|
| `MenuAnchorType` | `ExposedDropdownMenuAnchorType` |

```kotlin
// OLD
import androidx.compose.material3.MenuAnchorType
.menuAnchor(MenuAnchorType.PrimaryNotEditable)

// NEW
import androidx.compose.material3.ExposedDropdownMenuAnchorType
.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
```

---

## Kotlin 2.x — Annotation Target

Kotlin 2.x akan mengubah perilaku default target annotation pada constructor parameter.

```kotlin
// OLD — Warning: annotation target ambiguity
class MyClass @Inject constructor(
    @ApplicationContext private val context: Context
)

// NEW — Eksplisit target
class MyClass @Inject constructor(
    @param:ApplicationContext private val context: Context
)
```

**Alasan:** Mulai Kotlin 2.x, `@Qualifier` annotation pada constructor `val`/`var` parameter akan diterapkan ke **param + field**. Gunakan `@param:` untuk eksplisit.
Ref: [KT-73255](https://youtrack.jetbrains.com/issue/KT-73255)

---

## hiltViewModel — Known Deprecation (Benign)

| Status | Detail |
|---|---|
| **Import** | `androidx.hilt.navigation.compose.hiltViewModel` |
| **Warning** | "Moved to package: androidx.hilt.lifecycle.viewmodel.compose" |
| **Action** | **Abaikan untuk saat ini** |

**Alasan:** Warning ini menyesatkan. Artifact `hilt-lifecycle-viewmodel-compose` sudah di-remove dari AndroidX Hilt. Fungsi `hiltViewModel()` dari `hilt-navigation-compose:1.3.0` masih berfungsi normal. Warning ini akan hilang di versi library berikutnya.

---

## Quick Checklist untuk Kode Baru

- [ ] Gunakan `Icons.AutoMirrored.Filled.*` untuk ikon berarah (Backspace, ArrowBack, Logout, dll)
- [ ] Gunakan `Locale.forLanguageTag("id-ID")` bukan `Locale("id", "ID")`
- [ ] Gunakan `MasterKey.Builder` bukan `MasterKeys`
- [ ] Gunakan `PrimaryTabRow` / `SecondaryTabRow` bukan `TabRow`
- [ ] Gunakan `ExposedDropdownMenuAnchorType` bukan `MenuAnchorType`
- [ ] Gunakan `@param:` target untuk annotation di constructor parameter
- [ ] Gunakan `fallbackToDestructiveMigration(dropAllTables = true)` jika pakai Room destructive migration
