package id.stargan.intikasirfnb.licensing

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
