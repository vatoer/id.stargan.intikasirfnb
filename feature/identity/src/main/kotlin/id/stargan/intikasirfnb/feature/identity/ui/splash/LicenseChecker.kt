package id.stargan.intikasirfnb.feature.identity.ui.splash

/**
 * Abstraction for license checking, implemented in :app module
 * where the full licensing infrastructure lives.
 */
interface LicenseChecker {
    /** Returns true if a valid, unexpired license exists locally */
    suspend fun hasValidLicense(): Boolean

    /** Performs background online revalidation (non-blocking, fire-and-forget) */
    fun revalidateInBackground()
}
