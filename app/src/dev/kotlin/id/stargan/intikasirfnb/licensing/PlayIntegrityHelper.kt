package id.stargan.intikasirfnb.licensing

import android.content.Context
import android.util.Log

/**
 * DEV: Play Integrity di-bypass.
 * Server harus dikonfigurasi dengan PLAY_INTEGRITY_ENABLED=false.
 */
class PlayIntegrityHelper(context: Context) {

    suspend fun prepare() {
        Log.d("PlayIntegrity", "[DEV] prepare() skipped")
    }

    suspend fun requestToken(nonce: String): String {
        Log.d("PlayIntegrity", "[DEV] returning dummy token")
        return "dev-mode-no-integrity-token"
    }
}
