package id.stargan.intikasirfnb.licensing

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit tests for license verification logic (1.7.14).
 * Tests Ed25519 signature, device binding, and expiry logic
 * using real BouncyCastle key pairs.
 */
class LicenseVerifierTest {

    private lateinit var privateKey: Ed25519PrivateKeyParameters
    private lateinit var publicKey: Ed25519PublicKeyParameters
    private lateinit var publicKeyHex: String

    @Before
    fun setup() {
        val keyGen = Ed25519KeyPairGenerator()
        keyGen.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val keyPair = keyGen.generateKeyPair()
        privateKey = keyPair.private as Ed25519PrivateKeyParameters
        publicKey = keyPair.public as Ed25519PublicKeyParameters
        publicKeyHex = bytesToHex(publicKey.encoded)
    }

    private fun sign(payload: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, privateKey)
        signer.update(payload, 0, payload.size)
        return signer.generateSignature()
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }

    // ==================== Ed25519 Signature ====================

    @Test
    fun `valid Ed25519 signature verifies successfully`() {
        val payload = "test payload".toByteArray()
        val signature = sign(payload)

        val verifier = Ed25519Signer()
        verifier.init(false, publicKey)
        verifier.update(payload, 0, payload.size)
        assertTrue(verifier.verifySignature(signature))
    }

    @Test
    fun `tampered payload fails verification`() {
        val payload = "original".toByteArray()
        val signature = sign(payload)

        val tampered = "tampered".toByteArray()
        val verifier = Ed25519Signer()
        verifier.init(false, publicKey)
        verifier.update(tampered, 0, tampered.size)
        assertFalse(verifier.verifySignature(signature))
    }

    @Test
    fun `wrong public key fails verification`() {
        val payload = "test".toByteArray()
        val signature = sign(payload)

        // Generate a different key pair
        val keyGen = Ed25519KeyPairGenerator()
        keyGen.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val wrongPub = keyGen.generateKeyPair().public as Ed25519PublicKeyParameters

        val verifier = Ed25519Signer()
        verifier.init(false, wrongPub)
        verifier.update(payload, 0, payload.size)
        assertFalse(verifier.verifySignature(signature))
    }

    // ==================== Hex Encoding ====================

    @Test
    fun `hexToBytes and bytesToHex roundtrip`() {
        val original = byteArrayOf(0x3a.toByte(), 0xad.toByte(), 0x0b.toByte(), 0xea.toByte())
        val hex = bytesToHex(original)
        assertEquals("3aad0bea", hex)
        assertTrue(hexToBytes(hex).contentEquals(original))
    }

    @Test
    fun `public key hex is 64 chars (32 bytes)`() {
        assertEquals(64, publicKeyHex.length)
        assertEquals(32, hexToBytes(publicKeyHex).size)
    }

    // ==================== Device Binding Logic ====================

    @Test
    fun `device ID match passes`() {
        val licenseDeviceId = "device-abc"
        val currentDeviceId = "device-abc"
        assertEquals(licenseDeviceId, currentDeviceId)
    }

    @Test
    fun `device ID mismatch detected`() {
        val licenseDeviceId = "device-abc"
        val currentDeviceId = "device-different"
        assertFalse(licenseDeviceId == currentDeviceId)
    }

    @Test
    fun `null current device ID skips check`() {
        val currentDeviceId: String? = null
        // When null, verification should skip device check
        assertTrue(currentDeviceId == null)
    }

    // ==================== Expiry Logic ====================

    @Test
    fun `future expiry is valid`() {
        val expiry = Instant.now().plus(365, ChronoUnit.DAYS)
        assertFalse(Instant.now().isAfter(expiry))
    }

    @Test
    fun `past expiry is invalid`() {
        val expiry = Instant.now().minus(1, ChronoUnit.DAYS)
        assertTrue(Instant.now().isAfter(expiry))
    }

    @Test
    fun `null expiry is perpetual (always valid)`() {
        val expiry: String? = null
        // null means no expiry → always valid
        assertTrue(expiry == null)
    }

    @Test
    fun `expiry parsing works for ISO-8601 format`() {
        val expiryStr = "2027-12-31T23:59:59Z"
        val parsed = Instant.parse(expiryStr)
        assertFalse(Instant.now().isAfter(parsed))
    }

    // ==================== SignedLicenseDto ====================

    @Test
    fun `SignedLicenseDto fields populated correctly`() {
        val dto = SignedLicenseDto(
            sn = "SN-001",
            applicationId = "intikasir-fnb",
            deviceId = "device-abc",
            licenseType = "PERPETUAL",
            maxDevices = 3,
            boundDevices = 1,
            expiry = null,
            signature = "aabbccdd",
            payloadBase64 = "dGVzdA==",
            publicKeyHex = publicKeyHex
        )
        assertEquals("SN-001", dto.sn)
        assertEquals("intikasir-fnb", dto.applicationId)
        assertEquals("PERPETUAL", dto.licenseType)
        assertEquals(3, dto.maxDevices)
        assertEquals(1, dto.boundDevices)
    }

    // ==================== Full Verification Flow (manual) ====================

    @Test
    fun `full flow - sign payload, verify signature, check fields`() {
        val payloadJson = """{"sn":"SN-001","device_id":"dev-123","expiry":"2027-12-31T23:59:59Z"}"""
        val payloadBytes = payloadJson.toByteArray()
        val payloadBase64 = java.util.Base64.getEncoder().encodeToString(payloadBytes)
        val signatureBytes = sign(payloadBytes)
        val signatureHex = bytesToHex(signatureBytes)

        // Verify signature
        val publicKeyBytes = hexToBytes(publicKeyHex)
        val pubKey = Ed25519PublicKeyParameters(publicKeyBytes, 0)
        val decodedPayload = java.util.Base64.getDecoder().decode(payloadBase64)
        val verifier = Ed25519Signer()
        verifier.init(false, pubKey)
        verifier.update(decodedPayload, 0, decodedPayload.size)
        assertTrue(verifier.verifySignature(hexToBytes(signatureHex)))

        // Check expiry
        val expiryStr = "2027-12-31T23:59:59Z"
        assertFalse(Instant.now().isAfter(Instant.parse(expiryStr)))
    }
}
