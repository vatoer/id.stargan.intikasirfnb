package id.stargan.intikasirfnb.domain.shared

import com.github.f4b6a3.ulid.UlidCreator

object UlidGenerator {
    fun generate(): String = UlidCreator.getMonotonicUlid().toString()
}
