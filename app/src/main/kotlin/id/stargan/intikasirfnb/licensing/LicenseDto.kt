package id.stargan.intikasirfnb.licensing

import com.google.gson.annotations.SerializedName

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
    val expiry: String?,
    val signature: String,
    @SerializedName("payload_base64") val payloadBase64: String,
    @SerializedName("public_key_hex") val publicKeyHex: String,
)

data class ValidationResponse(
    val valid: Boolean,
    val sn: String,
    @SerializedName("application_id") val applicationId: String,
    val status: String,
    @SerializedName("license_type") val licenseType: String,
    @SerializedName("max_devices") val maxDevices: Int,
    @SerializedName("bound_devices") val boundDevices: Int,
    @SerializedName("bound_device_ids") val boundDeviceIds: List<String>,
    val error: String? = null,
)
