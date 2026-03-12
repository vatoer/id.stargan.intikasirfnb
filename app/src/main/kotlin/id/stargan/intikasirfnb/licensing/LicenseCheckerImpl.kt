package id.stargan.intikasirfnb.licensing

import id.stargan.intikasirfnb.feature.identity.ui.splash.LicenseChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LicenseCheckerImpl(
    private val licenseStorage: LicenseStorage,
    private val deviceIdProvider: DeviceIdProvider,
    private val licenseRevalidator: LicenseRevalidator,
    private val applicationScope: CoroutineScope,
) : LicenseChecker {

    override suspend fun hasValidLicense(): Boolean {
        val stored = licenseStorage.load() ?: return false
        val deviceId = deviceIdProvider.getDeviceId()
        return LicenseVerifier.isValid(stored, deviceId)
    }

    override fun revalidateInBackground() {
        applicationScope.launch {
            val valid = licenseRevalidator.checkOnline()
            if (!valid) {
                licenseStorage.clear()
            }
        }
    }
}
