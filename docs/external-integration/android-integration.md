# Panduan Integrasi Android — AppReg License Server

Dokumen ini ditujukan untuk **developer Android pihak ke-3** yang mengintegrasikan app dengan AppReg License Server.

> **Untuk tim internal:** Konfigurasi server, key management, dan admin API ada di [`../backend/README.md`](../backend/README.md).

---

## Daftar Isi

1. [Ringkasan Sistem](#1-ringkasan-sistem)
2. [Prasyarat](#2-prasyarat)
3. [API Reference](#3-api-reference)
   - [3.1 Base URL & Headers](#31-base-url--headers)
   - [3.2 Rate Limiting](#32-rate-limiting)
   - [3.3 Error Handling](#33-error-handling)
   - [3.4 POST /activation/challenge](#34-post-activationchallenge)
   - [3.5 POST /activation/activate](#35-post-activationactivate)
   - [3.6 POST /activation/reactivate](#36-post-activationreactivate)
   - [3.7 GET /validate/{sn}](#37-get-validatesn)
4. [Alur Aktivasi (Step-by-Step)](#4-alur-aktivasi-step-by-step)
5. [Mode Operasi: Development vs Production](#5-mode-operasi-development-vs-production)
6. [Implementasi Android](#6-implementasi-android)
   - [6.1 Build Flavor](#61-build-flavor)
   - [6.2 AppConfig](#62-appconfig)
   - [6.3 Play Integrity Helper](#63-play-integrity-helper)
   - [6.4 Network Layer (Retrofit)](#64-network-layer-retrofit)
   - [6.5 Certificate Pinning](#65-certificate-pinning)
   - [6.6 DTO & Model](#66-dto--model)
   - [6.7 License Storage](#67-license-storage)
   - [6.8 Device ID Provider](#68-device-id-provider)
   - [6.9 Activation Repository](#69-activation-repository)
   - [6.10 Offline Verification](#610-offline-verification)
   - [6.11 Periodic Online Revalidation](#611-periodic-online-revalidation)
   - [6.12 ViewModel & UI Wiring](#612-viewmodel--ui-wiring)
7. [Diagram Alur](#7-diagram-alur)
8. [Checklist Migrasi Dev → Production](#8-checklist-migrasi-dev--production)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Ringkasan Sistem

AppReg adalah sistem aktivasi lisensi berbasis serial number (SN) dengan keamanan berlapis:

```
[TLS] → [Certificate Pinning] → [Challenge–Response Nonce]
  → [Play Integrity Verification] → [Device Binding]
  → [Server-Signed License (Ed25519)] → [Offline Verification]
  → [Periodic Online Revalidation]
```

**Konsep utama:**
- User memasukkan SN → app mengaktivasi ke server → server mengembalikan **signed license**
- Signed license disimpan lokal → app bisa berjalan **offline** (verifikasi signature tanpa network)
- Server bisa **revoke** kapan saja → app harus periodik cek ke server

---

## 2. Prasyarat

**Dari tim backend, Anda akan menerima:**

| Item | Keterangan | Contoh |
|------|-----------|--------|
| `BASE_URL` | URL server API | `https://appreg.stargan.id` |
| `PUBLIC_KEY_HEX` | Ed25519 public key (64 karakter hex) | `3dcdf4cd4c33b8ca...` |
| `APPLICATION_ID` | Package name yang didaftarkan | `id.stargan.sarikasir` |
| `CLOUD_PROJECT_NUMBER` | Nomor project Google Cloud | `123456789012` |
| Certificate PIN | SHA-256 hash public key sertifikat server | `sha256/AbCdEf...==` |
| Test SN | Serial number untuk testing | `SN-XXXXXXXXXXXX` |

**Dependencies Android (build.gradle.kts):**

```kotlin
dependencies {
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Play Integrity
    implementation("com.google.android.play:integrity:1.4.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Encrypted storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Ed25519 signature verification
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
}
```

---

## 3. API Reference

### 3.1 Base URL & Headers

```
Base URL: https://appreg.stargan.id/api/v1
```

Semua request menggunakan `Content-Type: application/json`. Tidak ada header autentikasi yang diperlukan untuk endpoint activation dan validation (admin endpoint terpisah, tidak diakses oleh app Android).

### 3.2 Rate Limiting

Setiap endpoint di-rate-limit per IP address. Jika limit terlampaui, server mengembalikan:

```
HTTP 429 Too Many Requests
Retry-After: <detik>
```

| Endpoint Group | Default Limit |
|---------------|---------------|
| `/activation/*` (challenge, activate, reactivate) | 10 request/menit |
| `/validate/*` | 30 request/menit |

**Rekomendasi:**
- Jangan retry otomatis saat 429, tampilkan pesan ke user: *"Terlalu banyak percobaan. Coba lagi dalam X detik."*
- Baca header `Retry-After` untuk countdown timer

### 3.3 Error Handling

Semua error menggunakan format konsisten:

```json
{
  "detail": "Deskripsi error dalam bahasa Indonesia"
}
```

| HTTP Code | Situasi |
|-----------|---------|
| `200` | Sukses |
| `400` | Validasi gagal (SN invalid, nonce expired, integrity check gagal, device limit tercapai) |
| `422` | Request body tidak valid (field wajib kosong, tipe salah) |
| `429` | Rate limit terlampaui |
| `500` | Server error |

---

### 3.4 POST /activation/challenge

**Langkah 1** — Minta nonce dari server sebelum aktivasi.

```
POST /api/v1/activation/challenge
Content-Type: application/json
```

**Request Body:**

```json
{
  "sn": "SN-XXXXXXXXXXXX",
  "device_id": "a1b2c3d4e5f6"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `sn` | string | Ya | Serial number yang akan diaktivasi |
| `device_id` | string | Ya | Device identifier (lihat [6.8 Device ID Provider](#68-device-id-provider)) |

**Response (200):**

```json
{
  "nonce": "dGhpc0lzQVJhbmRvbU5vbmNlVmFsdWVGb3JUZXN0aW5n...",
  "expires_at": "2026-03-07T12:05:00"
}
```

| Field | Tipe | Keterangan |
|-------|------|-----------|
| `nonce` | string | Base64url-encoded, 32 byte random. **Single-use**, expire dalam 5 menit |
| `expires_at` | string (ISO 8601, UTC) | Waktu kedaluwarsa nonce |

**Error yang mungkin:**
- `400` — SN tidak ditemukan atau tidak valid

---

### 3.5 POST /activation/activate

**Langkah 2** — Aktivasi SN pada device baru. Harus dipanggil setelah `/challenge`.

```
POST /api/v1/activation/activate
Content-Type: application/json
```

**Request Body:**

```json
{
  "sn": "SN-XXXXXXXXXXXX",
  "device_id": "a1b2c3d4e5f6",
  "device_hash": "sha256:abcdef1234567890...",
  "nonce": "dGhpc0lzQVJhbmRvbU5vbmNl...",
  "integrity_token": "eyJhbGciOiJBMjU2S1ci..."
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `sn` | string | Ya | Serial number |
| `device_id` | string | Ya | Device identifier (sama dengan yang dikirim ke `/challenge`) |
| `device_hash` | string | Tidak | Device fingerprint hash (opsional, untuk fraud detection) |
| `nonce` | string | Kondisional* | Nonce dari `/challenge` |
| `integrity_token` | string | Kondisional* | Token dari Google Play Integrity API |

> *`nonce` dan `integrity_token` **wajib** jika server dikonfigurasi dengan `PLAY_INTEGRITY_ENABLED=true` (production). Pada development mode, field ini boleh kosong/null.

**Response (200) — Sukses:**

```json
{
  "success": true,
  "signed_license": {
    "sn": "SN-XXXXXXXXXXXX",
    "application_id": "id.stargan.sarikasir",
    "device_id": "a1b2c3d4e5f6",
    "license_type": "pro",
    "max_devices": 1,
    "bound_devices": 1,
    "expiry": "2027-01-01T00:00:00",
    "signature": "a3f8b2c1d4e5...hex_ed25519_signature",
    "payload_base64": "eyJzbiI6IlNOLTEyMzQ1Njc4O...",
    "public_key_hex": "3dcdf4cd4c33b8ca15e7d67fd1..."
  },
  "error": null
}
```

| Field | Tipe | Keterangan |
|-------|------|-----------|
| `success` | boolean | `true` jika aktivasi berhasil |
| `signed_license` | object / null | License yang ditandatangani server (null jika gagal) |
| `signed_license.sn` | string | Serial number |
| `signed_license.application_id` | string | Package name yang terdaftar |
| `signed_license.device_id` | string | Device ID yang terikat |
| `signed_license.license_type` | string | Tipe license: `"pro"`, `"enterprise"`, dll |
| `signed_license.max_devices` | int | Maksimum device yang bisa terikat ke SN ini |
| `signed_license.bound_devices` | int | Jumlah device yang saat ini terikat |
| `signed_license.expiry` | string / null | Tanggal kedaluwarsa (ISO 8601) atau `null` (perpetual) |
| `signed_license.signature` | string | Ed25519 signature dalam hex |
| `signed_license.payload_base64` | string | Payload asli yang ditandatangani (base64) |
| `signed_license.public_key_hex` | string | Public key server (64 karakter hex) — **untuk referensi saja, gunakan hardcoded value** |
| `error` | string / null | Pesan error (null jika sukses) |

**Response (200) — Gagal:**

```json
{
  "success": false,
  "signed_license": null,
  "error": "SN sudah mencapai batas maksimum device"
}
```

**Error yang mungkin:**
- `400` — Nonce invalid/expired, integrity check gagal
- `422` — Field wajib kosong

---

### 3.6 POST /activation/reactivate

Re-aktivasi device yang **sudah pernah terikat** ke SN (setelah reinstall app). Flow dan request/response body **identik** dengan `/activate`.

```
POST /api/v1/activation/reactivate
Content-Type: application/json
```

**Request & response body:** Sama persis dengan [3.5 POST /activation/activate](#35-post-activationactivate).

**Perbedaan behavior:**
- `/activate` — buat binding baru (device baru)
- `/reactivate` — terbitkan ulang license untuk device yang sudah terikat (tidak buat binding baru)

**Kapan menggunakan `/reactivate`:**
- User reinstall app → license lokal hilang → perlu re-issue
- Device sudah terdaftar di server untuk SN tersebut

**Strategi di app:** Coba `/activate` dulu. Jika error "device already bound", otomatis panggil `/reactivate`.

---

### 3.7 GET /validate/{sn}

Periodic online check — cek apakah license masih valid (belum di-revoke/expired).

```
GET /api/v1/validate/{sn}?device_id=a1b2c3d4e5f6
```

| Parameter | Lokasi | Wajib | Keterangan |
|-----------|--------|-------|-----------|
| `sn` | path | Ya | Serial number |
| `device_id` | query | Tidak | Jika disertakan, server juga cek apakah device masih terikat |

**Response (200):**

```json
{
  "valid": true,
  "sn": "SN-XXXXXXXXXXXX",
  "application_id": "id.stargan.sarikasir",
  "status": "active",
  "license_type": "pro",
  "max_devices": 1,
  "bound_devices": 1,
  "bound_device_ids": ["a1b2c3d4e5f6"],
  "error": null
}
```

| Field | Tipe | Keterangan |
|-------|------|-----------|
| `valid` | boolean | `true` jika license masih aktif dan device terikat |
| `sn` | string | Serial number |
| `application_id` | string | Package name |
| `status` | string | Status license: `"unused"`, `"active"`, `"revoked"`, `"expired"` |
| `license_type` | string | Tipe license |
| `max_devices` | int | Max device yang diizinkan |
| `bound_devices` | int | Jumlah device saat ini |
| `bound_device_ids` | string[] | List device ID yang terikat |
| `error` | string / null | Pesan error |

**Jika `valid: false`:** App harus **hapus license lokal** dan arahkan user ke layar aktivasi.

Juga mendukung method `POST` dengan parameter yang sama (untuk flexibility).

---

## 4. Alur Aktivasi (Step-by-Step)

```
┌──────────────────────────────────────────────────────────┐
│ User input SN di layar aktivasi                          │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────┐
│ 1. POST /activation/challenge { sn, device_id }          │
│    → Terima nonce (berlaku 5 menit, single-use)          │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────┐
│ 2. PlayIntegrityHelper.requestToken(nonce)                │
│    → Dapat integrity_token dari Google Play               │
│    (DEV: return dummy token, server skip verifikasi)      │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────┐
│ 3. POST /activation/activate {                            │
│       sn, device_id, device_hash, nonce, integrity_token  │
│    }                                                      │
│    → Server:                                              │
│      a. Consume nonce (invalidate)                        │
│      b. Verifikasi integrity_token via Google API         │
│      c. Bind device_id ke SN                              │
│      d. Sign license payload dengan Ed25519               │
│    → Response: { signed_license }                         │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────┐
│ 4. Verify signature offline:                              │
│    Ed25519.verify(payload_base64, signature, PUBLIC_KEY)   │
│    → PENTING: gunakan PUBLIC_KEY_HEX yang di-hardcode,    │
│      JANGAN dari response server                          │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────┐
│ 5. Simpan signed_license ke EncryptedSharedPreferences    │
│    → App aktif                                            │
└──────────────────────────────────────────────────────────┘
```

---

## 5. Mode Operasi: Development vs Production

| Aspek | Development | Production |
|-------|-------------|------------|
| Google Play Console | Tidak diperlukan | Wajib (minimal Internal Testing track) |
| Play Integrity | Di-bypass (dummy token) | Aktif, diverifikasi server |
| Certificate Pinning | Di-skip untuk `debug` build | Aktif di `release` build |
| Server URL | `http://10.0.2.2:8000` (emulator) atau IP lokal | `https://appreg.stargan.id` |
| Server config | `PLAY_INTEGRITY_ENABLED=false` | `PLAY_INTEGRITY_ENABLED=true` |
| `CLOUD_PROJECT_NUMBER` | Nilai dummy (`0L`) | Nomor project Google Cloud asli |
| `CERTIFICATE_PINS` | List kosong | SHA-256 public key server |

---

## 6. Implementasi Android

### 6.1 Build Flavor

Gunakan build flavor untuk memisahkan implementasi dev dan production. Kode bypass **tidak boleh** masuk ke release build.

```kotlin
// build.gradle.kts (module :app)
android {
    flavorDimensions += "env"

    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://appreg.stargan.id\"")
        }
    }
}
```

Build variant yang dihasilkan:
- `devDebug` — development + debugging
- `prodRelease` — production release

### 6.2 AppConfig

Konstanta yang di-hardcode di app. **Nilai diberikan oleh tim backend.**

```kotlin
// src/main/java/.../AppConfig.kt
object AppConfig {
    /**
     * Ed25519 public key server (32 byte = 64 karakter hex).
     * Digunakan untuk verifikasi signature license secara OFFLINE.
     * JANGAN fetch dari server runtime — hardcode saja.
     */
    const val PUBLIC_KEY_HEX = "GANTI_DENGAN_NILAI_DARI_TIM_BACKEND"

    /**
     * Google Cloud project number. Digunakan oleh Play Integrity API.
     * Dev: 0L (diabaikan). Production: nomor asli dari Google Cloud Console.
     */
    const val CLOUD_PROJECT_NUMBER = 0L

    /**
     * SHA-256 pin sertifikat server untuk certificate pinning.
     * Dev: kosong (pinning di-skip pada debug build).
     * Production: diisi dengan SHA-256 hash dari public key sertifikat server.
     */
    val CERTIFICATE_PINS = listOf<String>()
}
```

### 6.3 Play Integrity Helper

**Dev source set** (`src/dev/java/.../security/PlayIntegrityHelper.kt`):

```kotlin
package id.stargan.appreg.security

import android.content.Context
import android.util.Log

/**
 * DEV: Play Integrity di-bypass.
 * Server harus dikonfigurasi dengan PLAY_INTEGRITY_ENABLED=false.
 */
class PlayIntegrityHelper(context: Context) {

    suspend fun prepare() {
        Log.d("PlayIntegrity", "[DEV] prepare() skipped")
    }

    suspend fun requestToken(nonce: String): String {
        Log.d("PlayIntegrity", "[DEV] returning dummy token")
        return "dev-mode-no-integrity-token"
    }
}
```

**Prod source set** (`src/prod/java/.../security/PlayIntegrityHelper.kt`):

```kotlin
package id.stargan.appreg.security

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import kotlinx.coroutines.tasks.await

class PlayIntegrityHelper(context: Context) {

    private val integrityManager: StandardIntegrityManager =
        IntegrityManagerFactory.createStandard(context)

    private var tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    suspend fun prepare() {
        val request = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
            .setCloudProjectNumber(AppConfig.CLOUD_PROJECT_NUMBER)
            .build()
        try {
            tokenProvider = integrityManager.prepareIntegrityToken(request).await()
        } catch (e: Exception) {
            Log.w("PlayIntegrity", "prepare() gagal: ${e.message}")
        }
    }

    suspend fun requestToken(nonce: String): String {
        val provider = tokenProvider ?: run {
            prepare()
            tokenProvider ?: throw IllegalStateException("IntegrityTokenProvider tidak tersedia")
        }
        val request = StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
            .setRequestHash(nonce)
            .build()
        return provider.request(request).await().token()
    }
}
```

### 6.4 Network Layer (Retrofit)

```kotlin
// src/main/java/.../data/api/AppRegApi.kt

interface AppRegApi {

    @POST("activation/challenge")
    suspend fun challenge(@Body body: ChallengeRequest): ChallengeResponse

    @POST("activation/activate")
    suspend fun activate(@Body body: ActivateRequest): ActivateResponse

    @POST("activation/reactivate")
    suspend fun reactivate(@Body body: ActivateRequest): ActivateResponse

    @GET("validate/{sn}")
    suspend fun validate(
        @Path("sn") sn: String,
        @Query("device_id") deviceId: String? = null,
    ): ValidationResponse
}
```

```kotlin
// src/main/java/.../data/api/NetworkModule.kt

object NetworkModule {

    fun provideApi(): AppRegApi {
        val client = provideOkHttpClient()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AppRegApi::class.java)
    }

    private fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // Certificate pinning — aktif hanya pada release build
        if (!BuildConfig.DEBUG && AppConfig.CERTIFICATE_PINS.isNotEmpty()) {
            val pinner = CertificatePinner.Builder().apply {
                AppConfig.CERTIFICATE_PINS.forEach { pin ->
                    add("appreg.stargan.id", pin)
                }
            }.build()
            builder.certificatePinner(pinner)
        }

        // Logging hanya pada debug
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }
}
```

### 6.5 Certificate Pinning

**Development:** Pinning di-skip (lihat `provideOkHttpClient()` di atas — `BuildConfig.DEBUG == true` melewati blok pinning).

**Production — generate pin:**

```bash
openssl s_client -connect appreg.stargan.id:443 -servername appreg.stargan.id < /dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl base64
```

Isi di `AppConfig.kt`:
```kotlin
val CERTIFICATE_PINS = listOf("sha256/HASIL_OUTPUT_DI_ATAS==")
```

> **Let's Encrypt:** Pastikan server menggunakan `reuse_key = True` di konfigurasi certbot, agar pin tidak berubah setiap renewal (90 hari). Jika private key server berlu diganti, ikuti prosedur [migrasi pin](#pin-migration) di bawah.

<a name="pin-migration"></a>
**Migrasi pin (jika private key server diganti):**
1. Generate key baru di server → hitung pin baru
2. Rilis update app dengan `listOf("sha256/pin_lama==", "sha256/pin_baru==")`
3. Tunggu semua user update
4. Deploy sertifikat baru ke server
5. Rilis update app dengan `listOf("sha256/pin_baru==")` saja

### 6.6 DTO & Model

```kotlin
// --- Request DTOs ---

data class ChallengeRequest(
    val sn: String,
    @SerializedName("device_id") val deviceId: String,
)

data class ActivateRequest(
    val sn: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_hash") val deviceHash: String? = null,
    val nonce: String? = null,
    @SerializedName("integrity_token") val integrityToken: String? = null,
)

// --- Response DTOs ---

data class ChallengeResponse(
    val nonce: String,
    @SerializedName("expires_at") val expiresAt: String,
)

data class ActivateResponse(
    val success: Boolean,
    @SerializedName("signed_license") val signedLicense: SignedLicenseDto? = null,
    val error: String? = null,
)

data class SignedLicenseDto(
    val sn: String,
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("license_type") val licenseType: String,
    @SerializedName("max_devices") val maxDevices: Int,
    @SerializedName("bound_devices") val boundDevices: Int,
    val expiry: String?,                // ISO 8601 atau null (perpetual)
    val signature: String,              // Hex Ed25519
    @SerializedName("payload_base64") val payloadBase64: String,
    @SerializedName("public_key_hex") val publicKeyHex: String,
)

data class ValidationResponse(
    val valid: Boolean,
    val sn: String,
    @SerializedName("application_id") val applicationId: String,
    val status: String,                 // "active", "revoked", "expired", "unused"
    @SerializedName("license_type") val licenseType: String,
    @SerializedName("max_devices") val maxDevices: Int,
    @SerializedName("bound_devices") val boundDevices: Int,
    @SerializedName("bound_device_ids") val boundDeviceIds: List<String>,
    val error: String? = null,
)
```

### 6.7 License Storage

Simpan signed license di `EncryptedSharedPreferences` (Android Keystore-backed encryption).

```kotlin
class LicenseStorage(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        "license_store",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val gson = Gson()

    fun save(license: SignedLicenseDto) {
        prefs.edit()
            .putString("signed_license", gson.toJson(license))
            .putLong("last_online_check", System.currentTimeMillis())
            .apply()
    }

    fun load(): SignedLicenseDto? {
        val json = prefs.getString("signed_license", null) ?: return null
        return try {
            gson.fromJson(json, SignedLicenseDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getLastOnlineCheck(): Long =
        prefs.getLong("last_online_check", 0L)

    fun updateLastOnlineCheck() {
        prefs.edit()
            .putLong("last_online_check", System.currentTimeMillis())
            .apply()
    }
}
```

### 6.8 Device ID Provider

Gunakan Widevine ID (survive factory reset) dengan fallback ke `ANDROID_ID`:

```kotlin
class DeviceIdProvider(private val context: Context) {

    fun getDeviceId(): String {
        return getWidevineId() ?: getAndroidId()
    }

    private fun getWidevineId(): String? {
        return try {
            val drm = android.media.MediaDrm(java.util.UUID(-0x121074568629b532L, -0x6C68E55A586B8B50L))
            val id = drm.getPropertyByteArray(android.media.MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            drm.close()
            id.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getAndroidId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}
```

### 6.9 Activation Repository

Orchestration layer — menggabungkan challenge, integrity token, dan aktivasi:

```kotlin
class ActivationRepository(
    private val api: AppRegApi,
    private val integrityHelper: PlayIntegrityHelper,
    private val licenseStorage: LicenseStorage,
    private val deviceIdProvider: DeviceIdProvider,
) {

    /**
     * Full activation flow:
     * 1. Request challenge (nonce)
     * 2. Request integrity token
     * 3. Activate on server
     * 4. Verify signature offline
     * 5. Save to encrypted storage
     */
    suspend fun activate(sn: String): Result<SignedLicenseDto> = runCatching {
        val deviceId = deviceIdProvider.getDeviceId()

        // Step 1 — Challenge
        val challenge = api.challenge(ChallengeRequest(sn = sn, deviceId = deviceId))

        // Step 2 — Integrity token
        val integrityToken = integrityHelper.requestToken(challenge.nonce)

        // Step 3 — Activate
        val response = api.activate(
            ActivateRequest(
                sn = sn,
                deviceId = deviceId,
                nonce = challenge.nonce,
                integrityToken = integrityToken,
            )
        )

        if (!response.success || response.signedLicense == null) {
            throw LicenseActivationException(response.error ?: "Aktivasi gagal")
        }

        val license = response.signedLicense

        // Step 4 — Verify signature (menggunakan hardcoded public key!)
        LicenseVerifier.verify(license)

        // Step 5 — Save
        licenseStorage.save(license)

        license
    }

    /**
     * Re-activation flow (same device after reinstall).
     * Coba activate dulu, jika error "already bound" maka reactivate.
     */
    suspend fun reactivate(sn: String): Result<SignedLicenseDto> = runCatching {
        val deviceId = deviceIdProvider.getDeviceId()

        val challenge = api.challenge(ChallengeRequest(sn = sn, deviceId = deviceId))
        val integrityToken = integrityHelper.requestToken(challenge.nonce)

        val response = api.reactivate(
            ActivateRequest(
                sn = sn,
                deviceId = deviceId,
                nonce = challenge.nonce,
                integrityToken = integrityToken,
            )
        )

        if (!response.success || response.signedLicense == null) {
            throw LicenseActivationException(response.error ?: "Re-aktivasi gagal")
        }

        val license = response.signedLicense
        LicenseVerifier.verify(license)
        licenseStorage.save(license)

        license
    }
}

class LicenseActivationException(message: String) : Exception(message)
```

### 6.10 Offline Verification

Verifikasi Ed25519 signature tanpa network — dipanggil setiap app startup.

```kotlin
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

object LicenseVerifier {

    /**
     * Verifikasi signed license:
     * 1. Decode payload_base64 → bytes
     * 2. Decode signature hex → bytes
     * 3. Ed25519 verify menggunakan PUBLIC_KEY_HEX (hardcoded)
     * 4. Cek device_id cocok dengan device saat ini
     * 5. Cek expiry belum lewat
     *
     * @throws SecurityException jika verifikasi gagal
     */
    fun verify(
        license: SignedLicenseDto,
        currentDeviceId: String? = null,
    ) {
        // 1 & 2 — Decode
        val payloadBytes = Base64.decode(license.payloadBase64, Base64.NO_WRAP)
        val signatureBytes = hexToBytes(license.signature)
        val publicKeyBytes = hexToBytes(AppConfig.PUBLIC_KEY_HEX)

        // 3 — Ed25519 verify
        val publicKey = Ed25519PublicKeyParameters(publicKeyBytes, 0)
        val verifier = Ed25519Signer()
        verifier.init(false, publicKey)
        verifier.update(payloadBytes, 0, payloadBytes.size)

        if (!verifier.verifySignature(signatureBytes)) {
            throw SecurityException("Signature license tidak valid")
        }

        // 4 — Device ID check (jika diberikan)
        if (currentDeviceId != null && license.deviceId != currentDeviceId) {
            throw SecurityException("Device ID tidak cocok")
        }

        // 5 — Expiry check
        if (license.expiry != null) {
            val expiryDate = java.time.Instant.parse(license.expiry)
            if (java.time.Instant.now().isAfter(expiryDate)) {
                throw SecurityException("License sudah kedaluwarsa")
            }
        }
    }

    /**
     * Quick check — return boolean instead of throwing.
     */
    fun isValid(license: SignedLicenseDto, currentDeviceId: String): Boolean {
        return try {
            verify(license, currentDeviceId)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
```

### 6.11 Periodic Online Revalidation

Cek ke server apakah license masih valid. Jalankan periodik (setiap launch, atau via WorkManager setiap 24 jam).

```kotlin
class LicenseRevalidator(
    private val api: AppRegApi,
    private val licenseStorage: LicenseStorage,
    private val deviceIdProvider: DeviceIdProvider,
) {
    companion object {
        /** Grace period: berapa lama app boleh offline tanpa online check (7 hari). */
        const val OFFLINE_GRACE_PERIOD_MS = 7 * 24 * 60 * 60 * 1000L
    }

    /**
     * @return true jika license masih valid, false jika harus deactivate
     */
    suspend fun checkOnline(): Boolean {
        val stored = licenseStorage.load() ?: return false
        val deviceId = deviceIdProvider.getDeviceId()

        return try {
            val response = api.validate(sn = stored.sn, deviceId = deviceId)
            if (!response.valid) {
                // License di-revoke atau expired di server → hapus lokal
                licenseStorage.clear()
                return false
            }
            licenseStorage.updateLastOnlineCheck()
            true
        } catch (e: Exception) {
            // Network error → cek grace period
            val lastCheck = licenseStorage.getLastOnlineCheck()
            val elapsed = System.currentTimeMillis() - lastCheck
            if (lastCheck > 0 && elapsed > OFFLINE_GRACE_PERIOD_MS) {
                // Sudah terlalu lama offline → deactivate untuk keamanan
                licenseStorage.clear()
                return false
            }
            // Masih dalam grace period → izinkan
            true
        }
    }
}
```

### 6.12 ViewModel & UI Wiring

```kotlin
sealed class ActivationState {
    object Idle : ActivationState()
    object Loading : ActivationState()
    data class Success(val license: SignedLicenseDto) : ActivationState()
    data class Error(val message: String) : ActivationState()
}

class ActivationViewModel(
    private val activationRepo: ActivationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val state: StateFlow<ActivationState> = _state.asStateFlow()

    fun activate(sn: String) {
        if (_state.value is ActivationState.Loading) return
        _state.value = ActivationState.Loading

        viewModelScope.launch {
            activationRepo.activate(sn)
                .onSuccess { license ->
                    _state.value = ActivationState.Success(license)
                }
                .onFailure { e ->
                    _state.value = ActivationState.Error(
                        e.message ?: "Aktivasi gagal"
                    )
                }
        }
    }

    fun reactivate(sn: String) {
        if (_state.value is ActivationState.Loading) return
        _state.value = ActivationState.Loading

        viewModelScope.launch {
            activationRepo.reactivate(sn)
                .onSuccess { license ->
                    _state.value = ActivationState.Success(license)
                }
                .onFailure { e ->
                    _state.value = ActivationState.Error(
                        e.message ?: "Re-aktivasi gagal"
                    )
                }
        }
    }

    fun resetState() {
        _state.value = ActivationState.Idle
    }
}
```

**Di Activity/Fragment:**

```kotlin
private val viewModel: ActivationViewModel by viewModels()

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    lifecycleScope.launch {
        viewModel.state.collect { state ->
            when (state) {
                is ActivationState.Idle -> showForm()
                is ActivationState.Loading -> showProgress("Mengaktivasi...")
                is ActivationState.Success -> {
                    showSuccess("License aktif: ${state.license.licenseType}")
                    navigateToMain()
                }
                is ActivationState.Error -> {
                    showError(state.message)
                    viewModel.resetState()
                }
            }
        }
    }
}

fun onActivateClick() {
    val sn = serialNumberInput.text.toString().trim()
    if (sn.isBlank()) {
        showError("Serial number tidak boleh kosong")
        return
    }
    viewModel.activate(sn)
}
```

**Startup license check (SplashActivity/MainActivity):**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val licenseStorage = LicenseStorage(this)
    val deviceId = DeviceIdProvider(this).getDeviceId()
    val stored = licenseStorage.load()

    if (stored != null && LicenseVerifier.isValid(stored, deviceId)) {
        // License valid offline → lanjut ke main
        navigateToMain()

        // Background: cek online juga
        lifecycleScope.launch {
            val revalidator = LicenseRevalidator(api, licenseStorage, DeviceIdProvider(this@MainActivity))
            if (!revalidator.checkOnline()) {
                navigateToActivation()
            }
        }
    } else {
        navigateToActivation()
    }
}
```

---

## 7. Diagram Alur

### Activation Sequence

```
┌─────────┐          ┌──────────┐          ┌────────────┐          ┌───────────┐
│ Android │          │  Server  │          │ Google Play│          │ Play API  │
└────┬────┘          └────┬─────┘          └─────┬──────┘          └─────┬─────┘
     │                    │                      │                       │
     │ POST /challenge    │                      │                       │
     │ {sn, device_id}    │                      │                       │
     │───────────────────►│                      │                       │
     │                    │                      │                       │
     │ {nonce, expires_at}│                      │                       │
     │◄───────────────────│                      │                       │
     │                    │                      │                       │
     │ requestToken(nonce)│                      │                       │
     │───────────────────────────────────────────►                       │
     │                    │                      │                       │
     │ integrity_token    │                      │                       │
     │◄───────────────────────────────────────────                       │
     │                    │                      │                       │
     │ POST /activate     │                      │                       │
     │ {sn, device_id,    │                      │                       │
     │  nonce, token}     │                      │                       │
     │───────────────────►│                      │                       │
     │                    │ decodeToken           │                       │
     │                    │──────────────────────────────────────────────►│
     │                    │                      │                       │
     │                    │ verdict (OK)         │                       │
     │                    │◄──────────────────────────────────────────────│
     │                    │                      │                       │
     │                    │ Bind device + sign    │                       │
     │                    │                      │                       │
     │ {signed_license}   │                      │                       │
     │◄───────────────────│                      │                       │
     │                    │                      │                       │
     │ verify(signature)  │                      │                       │
     │ save(license)      │                      │                       │
     │                    │                      │                       │
```

### Startup License Check

```
┌─────────────────────────────────────────────────────────────┐
│ App Startup                                                  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Load license dari EncryptedSharedPreferences                │
│       │                                                      │
│       ├── Tidak ada → navigateToActivation()                 │
│       │                                                      │
│       ▼                                                      │
│  Ed25519.verify(payload, signature, PUBLIC_KEY_HEX)          │
│       │                                                      │
│       ├── Signature invalid → navigateToActivation()         │
│       │                                                      │
│       ▼                                                      │
│  device_id == current device?                                │
│       │                                                      │
│       ├── Mismatch → navigateToActivation()                  │
│       │                                                      │
│       ▼                                                      │
│  Expiry belum lewat? (atau null = perpetual)                 │
│       │                                                      │
│       ├── Expired → navigateToActivation()                   │
│       │                                                      │
│       ▼                                                      │
│  App aktif ✓                                                 │
│       │                                                      │
│       └── Background: online revalidation                    │
│            └── valid=false → hapus license + deactivate      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 8. Checklist Migrasi Dev → Production

### Dari Tim Backend (sudah disediakan)

- [x] Server berjalan di HTTPS (`https://appreg.stargan.id`)
- [x] `PLAY_INTEGRITY_ENABLED=true`
- [x] `PLAY_INTEGRITY_PACKAGE_NAME` sesuai package name app
- [x] `SIGNING_PRIVATE_KEY` dan `SIGNING_PUBLIC_KEY` statis

### Google Play Console (developer Android)

- [ ] App terdaftar dengan package name yang benar
- [ ] Minimal satu APK/AAB di-upload ke Internal Testing track
- [ ] Google Play Integrity API di-enable di Google Cloud Console
- [ ] (Opsional) "Response testing" diaktifkan untuk sideload testing

### Android App

- [ ] `AppConfig.PUBLIC_KEY_HEX` diisi dengan nilai dari tim backend
- [ ] `AppConfig.CLOUD_PROJECT_NUMBER` diisi nomor project Google Cloud
- [ ] `AppConfig.CERTIFICATE_PINS` diisi SHA-256 pin sertifikat server
- [ ] Build menggunakan `prodRelease` variant
- [ ] `PlayIntegrityHelper` yang dipakai adalah dari source set `prod/`
- [ ] `BuildConfig.DEBUG` adalah `false` → certificate pinning aktif
- [ ] Verifikasi dengan `jadx` bahwa dummy token tidak ada di release APK

---

## 9. Troubleshooting

### `HTTP 429 Too Many Requests`

Rate limit terlampaui. Baca header `Retry-After` dan tampilkan countdown ke user. Default limit: 10 request/menit untuk activation endpoints.

### `Nonce tidak valid atau sudah kedaluwarsa`

Nonce TTL 5 menit, single-use. Kemungkinan:
- User membuka layar aktivasi lalu meninggalkan terlalu lama
- Nonce sudah dipakai (double-submit)

**Solusi:** Panggil `/challenge` tepat saat tombol Aktivasi ditekan, bukan saat layar dibuka. `ActivationRepository.activate()` sudah melakukan ini.

### Play Integrity: `APP_NOT_INSTALLED` / `APP_UID_MISMATCH`

APK tidak dikenal Google Play:
- APK di-sideload (bukan dari Play Store/Internal Testing)
- Package name tidak cocok dengan yang terdaftar

**Solusi development:** Gunakan flavor `dev` yang memakai `PlayIntegrityHelper` dummy.
**Solusi production:** Aktifkan "Response testing" di Play Console → Setup → Integrity API.

### Play Integrity: `GOOGLE_SERVER_UNAVAILABLE`

Google Play Services tidak tersedia atau error sementara. `PlayIntegrityHelper.prepare()` akan log warning dan `requestToken()` akan retry prepare otomatis.

### `SSLPeerUnverifiedException` (Certificate Pinning)

Pin tidak cocok dengan sertifikat server. Kemungkinan:
- Sertifikat diperbarui tanpa `reuse_key`
- Nilai di `CERTIFICATE_PINS` belum diperbarui

**Verifikasi pin:**
```bash
openssl s_client -connect appreg.stargan.id:443 < /dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl base64
```

### `Signature license tidak valid`

Ed25519 verification gagal. Kemungkinan:
- `PUBLIC_KEY_HEX` di app tidak cocok dengan server
- Payload telah dimanipulasi

**Solusi:** Pastikan `PUBLIC_KEY_HEX` di `AppConfig.kt` sama persis dengan `SIGNING_PUBLIC_KEY` di server `.env`.

### `Device ID tidak cocok`

`device_id` di payload license berbeda dengan device saat ini:
- Factory reset (ANDROID_ID berubah, tapi Widevine ID tetap)
- Emulator dengan konfigurasi berbeda antar sesi

`DeviceIdProvider` menggunakan Widevine ID (survive factory reset) dengan fallback ke ANDROID_ID.

### Aktivasi gagal tapi SN valid

Kemungkinan SN sudah mencapai `max_devices`. Cek error message di response. Jika device ini sudah pernah terikat (reinstall), gunakan `/reactivate` bukan `/activate`.
