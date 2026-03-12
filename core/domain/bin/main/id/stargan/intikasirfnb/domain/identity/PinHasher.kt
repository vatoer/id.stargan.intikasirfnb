package id.stargan.intikasirfnb.domain.identity

interface PinHasher {
    fun hash(pin: String): String
    fun verify(pin: String, hash: String): Boolean
}
