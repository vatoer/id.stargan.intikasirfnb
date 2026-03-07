package id.stargan.intikasirfnb.domain.settings

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TerminalId

interface TenantSettingsRepository {
    suspend fun getByTenantId(tenantId: TenantId): TenantSettings?
    suspend fun save(settings: TenantSettings)
}

interface OutletSettingsRepository {
    suspend fun getByOutletId(outletId: OutletId): OutletSettings?
    suspend fun save(settings: OutletSettings)
}

interface TaxConfigRepository {
    suspend fun getById(id: TaxConfigId): TaxConfig?
    suspend fun getActiveByTenant(tenantId: TenantId): List<TaxConfig>
    suspend fun getAllByTenant(tenantId: TenantId): List<TaxConfig>
    suspend fun save(taxConfig: TaxConfig)
    suspend fun delete(id: TaxConfigId)
}

interface TerminalSettingsRepository {
    suspend fun getByTerminalId(terminalId: TerminalId): TerminalSettings?
    suspend fun save(settings: TerminalSettings)
}
