package id.stargan.intikasirfnb.licensing

import id.stargan.intikasirfnb.BuildConfig

object AppConfig {
    /**
     * Ed25519 public key server (32 byte = 64 karakter hex).
     * Dikonfigurasi via custom.properties → PUBLIC_KEY_HEX
     */
    val PUBLIC_KEY_HEX: String = BuildConfig.PUBLIC_KEY_HEX

    /**
     * Google Cloud project number. Digunakan oleh Play Integrity API.
     * Dikonfigurasi via custom.properties → CLOUD_PROJECT_NUMBER
     */
    val CLOUD_PROJECT_NUMBER: Long = BuildConfig.CLOUD_PROJECT_NUMBER

    /**
     * Hostname untuk certificate pinning (OkHttp CertificatePinner).
     * Dikonfigurasi via custom.properties → CERT_PIN_HOSTNAME
     */
    val CERT_PIN_HOSTNAME: String = BuildConfig.CERT_PIN_HOSTNAME

    /**
     * SHA-256 pin sertifikat server untuk certificate pinning.
     * Dikonfigurasi via custom.properties → CERTIFICATE_PINS (comma-separated).
     * Kosong = pinning disabled.
     */
    val CERTIFICATE_PINS: List<String> = BuildConfig.CERTIFICATE_PINS
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
