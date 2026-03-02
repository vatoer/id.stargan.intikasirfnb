package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.OrderLineEntity
import id.stargan.intikasirfnb.data.local.entity.PaymentEntity
import id.stargan.intikasirfnb.data.local.entity.SaleEntity
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.catalog.ProductRef
import id.stargan.intikasirfnb.domain.customer.CustomerId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import id.stargan.intikasirfnb.domain.transaction.CashierSessionStatus
import id.stargan.intikasirfnb.domain.transaction.OrderChannel
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.Payment
import id.stargan.intikasirfnb.domain.transaction.PaymentMethod
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.data.local.entity.TableEntity
import id.stargan.intikasirfnb.data.local.entity.CashierSessionEntity
import java.math.BigDecimal

fun SaleEntity.toDomain(
    lines: List<OrderLineEntity>,
    payments: List<PaymentEntity>
): Sale = Sale(
    id = SaleId(id),
    outletId = OutletId(outletId),
    channel = OrderChannel.valueOf(channel),
    tableId = tableId?.let { TableId(it) },
    externalOrderId = externalOrderId,
    cashierId = cashierId?.let { UserId(it) },
    customerId = customerId?.let { CustomerId(it) },
    lines = lines.map { it.toDomain() },
    payments = payments.map { it.toDomain() },
    status = SaleStatus.valueOf(status),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun OrderLineEntity.toDomain(): OrderLine = OrderLine(
    productRef = ProductRef(
        productId = ProductId(productId),
        name = productName,
        price = Money(BigDecimal(unitPriceAmount), unitPriceCurrency),
        taxCode = null
    ),
    quantity = quantity,
    unitPrice = Money(BigDecimal(unitPriceAmount), unitPriceCurrency),
    discountAmount = Money(BigDecimal(discountAmount), unitPriceCurrency),
    modifierSnapshot = modifierSnapshot
)

fun PaymentEntity.toDomain(): Payment = Payment(
    method = PaymentMethod.valueOf(method),
    amount = Money(BigDecimal(amountAmount), amountCurrency),
    reference = reference
)

fun Sale.toEntity(): SaleEntity = SaleEntity(
    id = id.value,
    outletId = outletId.value,
    channel = channel.name,
    tableId = tableId?.value,
    externalOrderId = externalOrderId,
    cashierId = cashierId?.value,
    customerId = customerId?.value,
    status = status.name,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun OrderLine.toEntity(saleId: String, index: Int): OrderLineEntity = OrderLineEntity(
    id = "${saleId}_line_$index",
    saleId = saleId,
    productId = productRef.productId.value,
    productName = productRef.name,
    quantity = quantity,
    unitPriceAmount = unitPrice.amount.toPlainString(),
    unitPriceCurrency = unitPrice.currencyCode,
    discountAmount = discountAmount.amount.toPlainString(),
    modifierSnapshot = modifierSnapshot
)

fun Payment.toEntity(saleId: String, index: Int): PaymentEntity = PaymentEntity(
    id = "${saleId}_pay_$index",
    saleId = saleId,
    method = method.name,
    amountAmount = amount.amount.toPlainString(),
    amountCurrency = amount.currencyCode,
    reference = reference
)

fun TableEntity.toDomain(): Table = Table(
    id = TableId(id),
    outletId = OutletId(outletId),
    name = name,
    capacity = capacity,
    currentSaleId = currentSaleId?.let { SaleId(it) },
    isActive = isActive
)

fun Table.toEntity(): TableEntity = TableEntity(
    id = id.value,
    outletId = outletId.value,
    name = name,
    capacity = capacity,
    currentSaleId = currentSaleId?.value,
    isActive = isActive
)

fun CashierSessionEntity.toDomain(): CashierSession = CashierSession(
    id = TerminalId(terminalId),
    outletId = OutletId(outletId),
    userId = UserId(userId),
    openAtMillis = openAtMillis,
    closeAtMillis = closeAtMillis,
    openingFloat = Money(BigDecimal(openingFloatAmount), openingFloatCurrency),
    status = CashierSessionStatus.valueOf(status)
)

fun CashierSession.toEntity(): CashierSessionEntity = CashierSessionEntity(
    terminalId = id.value,
    outletId = outletId.value,
    userId = userId.value,
    openAtMillis = openAtMillis,
    closeAtMillis = closeAtMillis,
    openingFloatAmount = openingFloat.amount.toPlainString(),
    openingFloatCurrency = openingFloat.currencyCode,
    status = status.name
)
