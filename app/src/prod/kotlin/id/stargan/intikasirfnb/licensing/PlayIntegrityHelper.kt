package id.stargan.intikasirfnb.licensing

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
