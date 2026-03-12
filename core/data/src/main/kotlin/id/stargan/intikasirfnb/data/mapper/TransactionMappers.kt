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
import id.stargan.intikasirfnb.domain.transaction.CommissionType
import id.stargan.intikasirfnb.domain.transaction.Payment
import id.stargan.intikasirfnb.domain.transaction.PaymentId
import id.stargan.intikasirfnb.domain.transaction.PaymentMethod
import id.stargan.intikasirfnb.domain.transaction.PlatformPayment
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SelectedAddOn
import id.stargan.intikasirfnb.domain.transaction.SelectedModifier
import id.stargan.intikasirfnb.domain.transaction.ServiceChargeLine
import id.stargan.intikasirfnb.domain.transaction.SplitBill
import id.stargan.intikasirfnb.domain.transaction.SplitBillEntry
import id.stargan.intikasirfnb.domain.transaction.SplitType
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
    customerName = customerName,
    lines = lines.map { it.toDomain() },
    payments = payments.map { it.toDomain() },
    taxLines = deserializeTaxLines(taxLinesJson),
    serviceCharge = deserializeServiceCharge(serviceChargeRate, serviceChargeAmount, serviceChargeIsIncluded),
    tip = tipAmount?.let { TipLine(Money(BigDecimal(it))) },
    platformPayment = deserializePlatformPayment(
        platformGrossAmount, platformCommissionPercent, platformCommissionType,
        platformCommissionAmount, platformNetAmount, platformName, platformOrderId
    ),
    splitBill = deserializeSplitBill(splitBillJson),
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
    customerName = customerName,
    status = status.name,
    notes = notes,
    taxLinesJson = serializeTaxLines(taxLines),
    serviceChargeRate = serviceCharge?.rate?.toPlainString(),
    serviceChargeAmount = serviceCharge?.chargeAmount?.amount?.toPlainString(),
    serviceChargeIsIncluded = serviceCharge?.isIncludedInPrice,
    tipAmount = tip?.amount?.amount?.toPlainString(),
    platformGrossAmount = platformPayment?.grossAmount?.amount?.toPlainString(),
    platformCommissionPercent = platformPayment?.commissionPercent?.toPlainString(),
    platformCommissionType = platformPayment?.commissionType?.name,
    platformCommissionAmount = platformPayment?.commissionAmount?.amount?.toPlainString(),
    platformNetAmount = platformPayment?.netAmount?.amount?.toPlainString(),
    platformName = platformPayment?.platformName,
    platformOrderId = platformPayment?.platformOrderId,
    splitBillJson = serializeSplitBill(splitBill),
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
    selectedAddOns = deserializeAddOns(addOnSnapshot),
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
    addOnSnapshot = serializeAddOns(selectedAddOns),
    notes = notes,
    isSentToKitchen = isSentToKitchen
)

// --- Payment ---

fun PaymentEntity.toDomain(): Payment = Payment(
    id = PaymentId(id),
    method = PaymentMethod.valueOf(method),
    amount = Money(BigDecimal(amountAmount), amountCurrency),
    reference = reference,
    payerIndex = payerIndex
)

fun Payment.toEntity(saleId: String): PaymentEntity = PaymentEntity(
    id = id.value,
    saleId = saleId,
    method = method.name,
    amountAmount = amount.amount.toPlainString(),
    amountCurrency = amount.currencyCode,
    reference = reference,
    payerIndex = payerIndex
)

// --- Table ---

fun TableEntity.toDomain(): Table = Table(
    id = TableId(id),
    outletId = OutletId(outletId),
    name = name,
    capacity = capacity,
    section = section,
    currentSaleId = currentSaleId?.let { SaleId(it) },
    isActive = isActive
)

fun Table.toEntity(): TableEntity = TableEntity(
    id = id.value,
    outletId = outletId.value,
    name = name,
    capacity = capacity,
    section = section,
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

// --- Add-on serialization (JSON via Android org.json) ---

private fun serializeAddOns(addOns: List<SelectedAddOn>): String? {
    if (addOns.isEmpty()) return null
    val arr = JSONArray()
    for (a in addOns) {
        val obj = JSONObject()
        obj.put("n", a.addOnName)
        obj.put("q", a.quantity)
        obj.put("u", a.unitPrice.amount.toPlainString())
        obj.put("t", a.totalPrice.amount.toPlainString())
        arr.put(obj)
    }
    return arr.toString()
}

private fun deserializeAddOns(json: String?): List<SelectedAddOn> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            SelectedAddOn(
                addOnName = obj.getString("n"),
                quantity = obj.getInt("q"),
                unitPrice = Money(BigDecimal(obj.getString("u"))),
                totalPrice = Money(BigDecimal(obj.getString("t")))
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

// --- PlatformPayment deserialization from flat columns ---

private fun deserializePlatformPayment(
    grossAmount: String?,
    commissionPercent: String?,
    commissionType: String?,
    commissionAmount: String?,
    netAmount: String?,
    platformName: String?,
    platformOrderId: String?
): PlatformPayment? {
    if (grossAmount == null || commissionAmount == null || netAmount == null || platformName == null) return null
    return PlatformPayment(
        grossAmount = Money(BigDecimal(grossAmount)),
        commissionPercent = commissionPercent?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
        commissionType = commissionType?.let {
            try { CommissionType.valueOf(it) } catch (_: Exception) { CommissionType.FROM_SELLING_PRICE }
        } ?: CommissionType.FROM_SELLING_PRICE,
        commissionAmount = Money(BigDecimal(commissionAmount)),
        netAmount = Money(BigDecimal(netAmount)),
        platformName = platformName,
        platformOrderId = platformOrderId
    )
}

// --- SplitBill serialization ---

private fun serializeSplitBill(splitBill: SplitBill?): String? {
    if (splitBill == null) return null
    val root = JSONObject()
    root.put("t", splitBill.type.name)
    val entries = JSONArray()
    for (e in splitBill.entries) {
        val obj = JSONObject()
        obj.put("pi", e.payerIndex)
        obj.put("l", e.label)
        obj.put("sa", e.shareAmount.amount.toPlainString())
        obj.put("pa", e.paidAmount.amount.toPlainString())
        if (e.lineIds.isNotEmpty()) {
            val ids = JSONArray()
            for (id in e.lineIds) ids.put(id.value)
            obj.put("li", ids)
        }
        entries.put(obj)
    }
    root.put("e", entries)
    return root.toString()
}

private fun deserializeSplitBill(json: String?): SplitBill? {
    if (json.isNullOrBlank()) return null
    return try {
        val root = JSONObject(json)
        val type = SplitType.valueOf(root.getString("t"))
        val arr = root.getJSONArray("e")
        val entries = (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val lineIds = if (obj.has("li")) {
                val ids = obj.getJSONArray("li")
                (0 until ids.length()).map { j -> OrderLineId(ids.getString(j)) }
            } else emptyList()
            SplitBillEntry(
                payerIndex = obj.getInt("pi"),
                label = obj.getString("l"),
                lineIds = lineIds,
                shareAmount = Money(BigDecimal(obj.getString("sa"))),
                paidAmount = Money(BigDecimal(obj.optString("pa", "0")))
            )
        }
        SplitBill(type = type, entries = entries)
    } catch (_: Exception) {
        null
    }
}
