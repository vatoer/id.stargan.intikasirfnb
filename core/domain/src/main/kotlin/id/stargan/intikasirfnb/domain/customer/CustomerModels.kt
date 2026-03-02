package id.stargan.intikasirfnb.domain.customer

import id.stargan.intikasirfnb.domain.identity.TenantId
import java.util.UUID

@JvmInline
value class CustomerId(val value: String) {
    companion object {
        fun generate() = CustomerId(UUID.randomUUID().toString())
    }
}

data class Address(
    val line1: String,
    val line2: String? = null,
    val city: String? = null,
    val postalCode: String? = null
)

data class Customer(
    val id: CustomerId,
    val tenantId: TenantId,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: Address? = null,
    val loyaltyPoints: Int = 0
)
