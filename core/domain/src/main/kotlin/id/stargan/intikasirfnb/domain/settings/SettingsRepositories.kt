package id.stargan.intikasirfnb.domain.settings

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId

interface TenantSettingsRepository {
    suspend fun getByTenantId(tenantId: TenantId): TenantSettings?
    suspend fun save(settings: TenantSettings)
}

interface OutletSettingsRepository {
    suspend fun getByOutletId(outletId: OutletId): OutletSettings?
    suspend fun save(settings: OutletSettings)
}
