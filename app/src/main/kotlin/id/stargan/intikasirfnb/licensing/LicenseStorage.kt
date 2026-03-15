package id.stargan.intikasirfnb.licensing

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

class LicenseStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "license_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val gson = Gson()

    fun save(license: SignedLicenseDto) {
        prefs.edit()
            .putString("signed_license", gson.toJson(license))
            .putLong("last_online_check", System.currentTimeMillis())
            .apply()
    }

    fun load(): SignedLicenseDto? {
        val json = prefs.getString("signed_license", null) ?: return null
        return try {
            gson.fromJson(json, SignedLicenseDto::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getLastOnlineCheck(): Long =
        prefs.getLong("last_online_check", 0L)

    fun updateLastOnlineCheck() {
        prefs.edit()
            .putLong("last_online_check", System.currentTimeMillis())
            .apply()
    }
}
