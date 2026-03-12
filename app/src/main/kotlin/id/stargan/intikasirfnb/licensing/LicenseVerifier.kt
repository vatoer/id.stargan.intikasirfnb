package id.stargan.intikasirfnb.licensing

import android.util.Base64
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.time.Instant

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
        val payloadBytes = Base64.decode(license.payloadBase64, Base64.NO_WRAP)
        val signatureBytes = hexToBytes(license.signature)
        val publicKeyBytes = hexToBytes(AppConfig.PUBLIC_KEY_HEX)

        val publicKey = Ed25519PublicKeyParameters(publicKeyBytes, 0)
        val verifier = Ed25519Signer()
        verifier.init(false, publicKey)
        verifier.update(payloadBytes, 0, payloadBytes.size)

        if (!verifier.verifySignature(signatureBytes)) {
            throw SecurityException("Signature license tidak valid")
        }

        if (currentDeviceId != null && license.deviceId != currentDeviceId) {
            throw SecurityException("Device ID tidak cocok")
        }

        if (license.expiry != null) {
            val expiryDate = Instant.parse(license.expiry)
            if (Instant.now().isAfter(expiryDate)) {
                throw SecurityException("License sudah kedaluwarsa")
            }
        }
    }

    fun isValid(license: SignedLicenseDto, currentDeviceId: String): Boolean {
        return try {
            verify(license, currentDeviceId)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
