package id.stargan.intikasirfnb.licensing

import android.content.Context
import android.media.MediaDrm
import android.provider.Settings
import java.util.UUID

class DeviceIdProvider(private val context: Context) {

    fun getDeviceId(): String {
        return getWidevineId() ?: getAndroidId()
    }

    private fun getWidevineId(): String? {
        return try {
            val widevineUuid = UUID(-0x121074568629b532L, -0x6C68E55A586B8B50L)
            val drm = MediaDrm(widevineUuid)
            val id = drm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            drm.close()
            id.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun getAndroidId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}
