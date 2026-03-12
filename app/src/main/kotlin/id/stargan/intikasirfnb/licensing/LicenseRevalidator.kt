package id.stargan.intikasirfnb.licensing

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
                licenseStorage.clear()
                return false
            }
            licenseStorage.updateLastOnlineCheck()
            true
        } catch (_: Exception) {
            // Network error → cek grace period
            val lastCheck = licenseStorage.getLastOnlineCheck()
            val elapsed = System.currentTimeMillis() - lastCheck
            if (lastCheck > 0 && elapsed > OFFLINE_GRACE_PERIOD_MS) {
                licenseStorage.clear()
                return false
            }
            // Masih dalam grace period → izinkan
            true
        }
    }
}
