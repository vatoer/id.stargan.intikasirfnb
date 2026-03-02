package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.OutletEntity
import id.stargan.intikasirfnb.data.local.entity.TenantEntity
import id.stargan.intikasirfnb.data.local.entity.UserEntity
import id.stargan.intikasirfnb.domain.identity.Outlet
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.Role
import id.stargan.intikasirfnb.domain.identity.Tenant
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.User
import id.stargan.intikasirfnb.domain.identity.UserId

fun TenantEntity.toDomain(): Tenant = Tenant(
    id = TenantId(id),
    name = name,
    isActive = isActive
)

fun Tenant.toEntity(): TenantEntity = TenantEntity(
    id = id.value,
    name = name,
    isActive = isActive
)

fun OutletEntity.toDomain(): Outlet = Outlet(
    id = OutletId(id),
    tenantId = TenantId(tenantId),
    name = name,
    address = address,
    isActive = isActive
)

fun Outlet.toEntity(): OutletEntity = OutletEntity(
    id = id.value,
    tenantId = tenantId.value,
    name = name,
    address = address,
    isActive = isActive
)

fun UserEntity.toDomain(): User = User(
    id = UserId(id),
    tenantId = TenantId(tenantId),
    email = email,
    displayName = displayName,
    pinHash = pinHash,
    outletIds = outletIdsCsv.split(",").filter { it.isNotBlank() }.map { OutletId(it.trim()) },
    roles = rolesCsv.split(",").filter { it.isNotBlank() }.map { Role(it.trim()) },
    isActive = isActive
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id.value,
    tenantId = tenantId.value,
    email = email,
    displayName = displayName,
    pinHash = pinHash,
    outletIdsCsv = outletIds.joinToString(",") { it.value },
    rolesCsv = roles.joinToString(",") { it.value },
    isActive = isActive
)
