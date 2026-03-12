package id.stargan.intikasirfnb.licensing

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
