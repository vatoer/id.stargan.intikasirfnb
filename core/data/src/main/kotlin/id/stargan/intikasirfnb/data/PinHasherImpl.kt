package id.stargan.intikasirfnb.data

import id.stargan.intikasirfnb.domain.identity.PinHasher
import java.security.MessageDigest

class PinHasherImpl : PinHasher {
    override fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun verify(pin: String, hash: String): Boolean {
        return hash(pin) == hash
    }
}
