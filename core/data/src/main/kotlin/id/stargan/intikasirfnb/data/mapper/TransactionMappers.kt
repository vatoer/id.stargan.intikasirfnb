package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.CashierSessionEntity
import id.stargan.intikasirfnb.data.local.entity.OrderLineEntity
import id.stargan.intikasirfnb.data.local.entity.PaymentEntity
import id.stargan.intikasirfnb.data.local.entity.SaleEntity
import id.stargan.intikasirfnb.data.local.entity.TableEntity
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.catalog.ProductRef
import id.stargan.intikasirfnb.domain.customer.CustomerId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import id.stargan.intikasirfnb.domain.transaction.CashierSessionId
import id.stargan.intikasirfnb.domain.transaction.CashierSessionStatus
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.Payment
import id.stargan.intikasirfnb.domain.transaction.PaymentId
import id.stargan.intikasirfnb.domain.transaction.PaymentMethod
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SelectedModifier
import id.stargan.intikasirfnb.domain.transaction.ServiceChargeLine
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TaxLine
import id.stargan.intikasirfnb.domain.transaction.TipLine
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

// --- Sale ---

fun SaleEntity.toDomain(
    lines: List<OrderLineEntity>,
    payments: List<PaymentEntity>
): Sale = Sale(
    id = SaleId(id),
    outletId = OutletId(outletId),
    channelId = SalesChannelId(channelId),
    orderFlow = try { OrderFlowType.valueOf(orderFlow) } catch (_: Exception) { OrderFlowType.PAY_FIRST },
    receiptNumber = receiptNumber,
    queueNumber = queueNumber,
    tableId = tableId?.let { TableId(it) },
    externalOrderId = externalOrderId,
    cashierId = cashierId?.let { UserId(it) },
    customerId = customerId?.let { CustomerId(it) },
    lines = lines.map { it.toDomain() },
    payments = payments.map { it.toDomain() },
    taxLines = deserializeTaxLines(taxLinesJson),
    serviceCharge = deserializeServiceCharge(serviceChargeRate, serviceChargeAmount, serviceChargeIsIncluded),
    tip = tipAmount?.let { TipLine(Money(BigDecimal(it))) },
    status = SaleStatus.valueOf(status),
    notes = notes,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun Sale.toEntity(): SaleEntity = SaleEntity(
    id = id.value,
    outletId = outletId.value,
    channelId = channelId.value,
    orderFlow = orderFlow.name,
    receiptNumber = receiptNumber,
    queueNumber = queueNumber,
    tableId = tableId?.value,
    externalOrderId = externalOrderId,
    cashierId = cashierId?.value,
    customerId = customerId?.value,
    status = status.name,
    notes = notes,
    taxLinesJson = serializeTaxLines(taxLines),
    serviceChargeRate = serviceCharge?.rate?.toPlainString(),
    serviceChargeAmount = serviceCharge?.chargeAmount?.amount?.toPlainString(),
    serviceChargeIsIncluded = serviceCharge?.isIncludedInPrice,
    tipAmount = tip?.amount?.amount?.toPlainString(),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

// --- OrderLine ---

fun OrderLineEntity.toDomain(): OrderLine = OrderLine(
    id = OrderLineId(id),
    productRef = ProductRef(
        productId = ProductId(productId),
        name = productName,
        price = Money(BigDecimal(unitPriceAmount), unitPriceCurrency),
        taxCode = null
    ),
    quantity = quantity,
    unitPrice = Money(BigDecimal(unitPriceAmount), unitPriceCurrency),
    discountAmount = Money(BigDecimal(discountAmount), unitPriceCurrency),
    selectedModifiers = deserializeModifiers(modifierSnapshot),
    notes = notes,
    isSentToKitchen = isSentToKitchen
)

fun OrderLine.toEntity(saleId: String): OrderLineEntity = OrderLineEntity(
    id = id.value,
    saleId = saleId,
    productId = productRef.productId.value,
    productName = productRef.name,
    quantity = quantity,
    unitPriceAmount = unitPrice.amount.toPlainString(),
    unitPriceCurrency = unitPrice.currencyCode,
    discountAmount = discountAmount.amount.toPlainString(),
    modifierSnapshot = serializeModifiers(selectedModifiers),
    notes = notes,
    isSentToKitchen = isSentToKitchen
)

// --- Payment ---

fun PaymentEntity.toDomain(): Payment = Payment(
    id = PaymentId(id),
    method = PaymentMethod.valueOf(method),
    amount = Money(BigDecimal(amountAmount), amountCurrency),
    reference = reference
)

fun Payment.toEntity(saleId: String): PaymentEntity = PaymentEntity(
    id = id.value,
    saleId = saleId,
    method = method.name,
    amountAmount = amount.amount.toPlainString(),
    amountCurrency = amount.currencyCode,
    reference = reference
)

// --- Table ---

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

// --- CashierSession ---

fun CashierSessionEntity.toDomain(): CashierSession = CashierSession(
    id = CashierSessionId(id),
    terminalId = TerminalId(terminalId),
    outletId = OutletId(outletId),
    userId = UserId(userId),
    openAtMillis = openAtMillis,
    closeAtMillis = closeAtMillis,
    openingFloat = Money(BigDecimal(openingFloatAmount), openingFloatCurrency),
    closingCash = closingCashAmount?.let { Money(BigDecimal(it), closingCashCurrency ?: "IDR") },
    expectedCash = expectedCashAmount?.let { Money(BigDecimal(it), expectedCashCurrency ?: "IDR") },
    notes = notes,
    status = CashierSessionStatus.valueOf(status)
)

fun CashierSession.toEntity(): CashierSessionEntity = CashierSessionEntity(
    id = id.value,
    terminalId = terminalId.value,
    outletId = outletId.value,
    userId = userId.value,
    openAtMillis = openAtMillis,
    closeAtMillis = closeAtMillis,
    openingFloatAmount = openingFloat.amount.toPlainString(),
    openingFloatCurrency = openingFloat.currencyCode,
    closingCashAmount = closingCash?.amount?.toPlainString(),
    closingCashCurrency = closingCash?.currencyCode,
    expectedCashAmount = expectedCash?.amount?.toPlainString(),
    expectedCashCurrency = expectedCash?.currencyCode,
    notes = notes,
    status = status.name
)

// --- Modifier serialization (JSON via Android org.json) ---

private fun serializeModifiers(modifiers: List<SelectedModifier>): String? {
    if (modifiers.isEmpty()) return null
    val arr = JSONArray()
    for (m in modifiers) {
        val obj = JSONObject()
        obj.put("g", m.groupName)
        obj.put("o", m.optionName)
        obj.put("p", m.priceDelta.amount.toPlainString())
        arr.put(obj)
    }
    return arr.toString()
}

private fun deserializeModifiers(json: String?): List<SelectedModifier> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            SelectedModifier(
                groupName = obj.getString("g"),
                optionName = obj.getString("o"),
                priceDelta = Money(BigDecimal(obj.optString("p", "0")))
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

// --- TaxLine serialization ---

private fun serializeTaxLines(taxLines: List<TaxLine>): String? {
    if (taxLines.isEmpty()) return null
    val arr = JSONArray()
    for (t in taxLines) {
        val obj = JSONObject()
        obj.put("n", t.taxName)
        obj.put("r", t.taxRate.toPlainString())
        obj.put("i", t.isIncludedInPrice)
        obj.put("ta", t.taxableAmount.amount.toPlainString())
        obj.put("a", t.taxAmount.amount.toPlainString())
        arr.put(obj)
    }
    return arr.toString()
}

private fun deserializeTaxLines(json: String?): List<TaxLine> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            TaxLine(
                taxName = obj.getString("n"),
                taxRate = BigDecimal(obj.getString("r")),
                isIncludedInPrice = obj.getBoolean("i"),
                taxableAmount = Money(BigDecimal(obj.getString("ta"))),
                taxAmount = Money(BigDecimal(obj.getString("a")))
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

// --- ServiceChargeLine deserialization from flat columns ---

private fun deserializeServiceCharge(
    rate: String?,
    amount: String?,
    isIncluded: Boolean?
): ServiceChargeLine? {
    if (rate == null || amount == null) return null
    return ServiceChargeLine(
        rate = BigDecimal(rate),
        isIncludedInPrice = isIncluded ?: false,
        baseAmount = Money.zero(),
        chargeAmount = Money(BigDecimal(amount))
    )
}
