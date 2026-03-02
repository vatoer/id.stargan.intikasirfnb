package id.stargan.intikasirfnb.domain.supplier

import id.stargan.intikasirfnb.domain.identity.TenantId

interface SupplierRepository {
    suspend fun getById(id: SupplierId): Supplier?
    suspend fun save(supplier: Supplier)
    suspend fun listByTenant(tenantId: TenantId): List<Supplier>
}

interface PurchaseOrderRepository {
    suspend fun getById(id: PurchaseOrderId): PurchaseOrder?
    suspend fun save(order: PurchaseOrder)
    suspend fun listByTenant(tenantId: TenantId): List<PurchaseOrder>
}
