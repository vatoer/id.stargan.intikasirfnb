package id.stargan.intikasirfnb.domain.identity

import id.stargan.intikasirfnb.domain.shared.UlidGenerator

// --- Value objects (IDs) ---

@JvmInline
value class TenantId(val value: String) {
    companion object {
        fun generate() = TenantId(UlidGenerator.generate())
    }
}

@JvmInline
value class OutletId(val value: String) {
    companion object {
        fun generate() = OutletId(UlidGenerator.generate())
    }
}

@JvmInline
value class UserId(val value: String) {
    companion object {
        fun generate() = UserId(UlidGenerator.generate())
    }
}

@JvmInline
value class TerminalId(val value: String) {
    companion object {
        fun generate() = TerminalId(UlidGenerator.generate())
    }
}

/** Role name / permission identifier. */
@JvmInline
value class Role(val value: String)

@JvmInline
value class Permission(val value: String)

// --- Enums ---

enum class TerminalType {
    CASHIER,
    WAITER,
    KITCHEN_DISPLAY,
    MANAGER
}

enum class TerminalStatus {
    ACTIVE,
    SUSPENDED,
    DEREGISTERED
}

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

data class Terminal(
    val id: TerminalId,
    val tenantId: TenantId,
    val outletId: OutletId,
    val deviceName: String,
    val terminalType: TerminalType = TerminalType.CASHIER,
    val status: TerminalStatus = TerminalStatus.ACTIVE,
    val lastSyncAtMillis: Long? = null,
    val registeredAtMillis: Long = System.currentTimeMillis(),
    val syncEnabled: Boolean = false
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
