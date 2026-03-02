package id.stargan.intikasirfnb.domain.identity

import java.util.UUID

// --- Value objects (IDs) ---

@JvmInline
value class TenantId(val value: String) {
    companion object {
        fun generate() = TenantId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class OutletId(val value: String) {
    companion object {
        fun generate() = OutletId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class UserId(val value: String) {
    companion object {
        fun generate() = UserId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class TerminalId(val value: String) {
    companion object {
        fun generate() = TerminalId(UUID.randomUUID().toString())
    }
}

/** Role name / permission identifier. */
@JvmInline
value class Role(val value: String)

@JvmInline
value class Permission(val value: String)

// --- Entities / Aggregates ---

data class Tenant(
    val id: TenantId,
    val name: String,
    val isActive: Boolean = true
)

data class Outlet(
    val id: OutletId,
    val tenantId: TenantId,
    val name: String,
    val address: String? = null,
    val isActive: Boolean = true
)

data class User(
    val id: UserId,
    val tenantId: TenantId,
    val email: String,
    val displayName: String,
    val pinHash: String = "",
    val outletIds: List<OutletId> = emptyList(),
    val roles: List<Role> = emptyList(),
    val isActive: Boolean = true
) {
    fun hasAccessToOutlet(outletId: OutletId): Boolean =
        outletIds.isEmpty() || outletIds.contains(outletId)
}
