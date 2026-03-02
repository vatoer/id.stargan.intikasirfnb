package id.stargan.intikasirfnb.domain.supplier

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import java.util.UUID

@JvmInline
value class SupplierId(val value: String) {
    companion object {
        fun generate() = SupplierId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class PurchaseOrderId(val value: String) {
    companion object {
        fun generate() = PurchaseOrderId(UUID.randomUUID().toString())
    }
}

data class Supplier(
    val id: SupplierId,
    val tenantId: TenantId,
    val name: String,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val address: String? = null
)

data class PurchaseOrderLine(
    val productId: ProductId,
    val quantity: Int,
    val unitPrice: Money
)

data class PurchaseOrder(
    val id: PurchaseOrderId,
    val tenantId: TenantId,
    val supplierId: SupplierId,
    val lines: List<PurchaseOrderLine>,
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT
)

enum class PurchaseOrderStatus { DRAFT, CONFIRMED, RECEIVED, CANCELLED }
