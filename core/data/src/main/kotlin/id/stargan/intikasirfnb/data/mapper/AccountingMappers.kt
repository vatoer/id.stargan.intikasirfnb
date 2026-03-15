package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.AccountEntity
import id.stargan.intikasirfnb.data.local.entity.JournalEntity
import id.stargan.intikasirfnb.domain.accounting.Account
import id.stargan.intikasirfnb.domain.accounting.AccountId
import id.stargan.intikasirfnb.domain.accounting.AccountType
import id.stargan.intikasirfnb.domain.accounting.Journal
import id.stargan.intikasirfnb.domain.accounting.JournalEntry
import id.stargan.intikasirfnb.domain.accounting.JournalId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

fun JournalEntity.toDomain(): Journal = Journal(
    id = JournalId(id),
    outletId = OutletId(outletId),
    entries = deserializeEntries(entriesJson),
    description = description,
    referenceType = referenceType,
    referenceId = referenceId,
    createdAtMillis = createdAtMillis
)

fun Journal.toEntity(): JournalEntity = JournalEntity(
    id = id.value,
    outletId = outletId.value,
    description = description,
    referenceType = referenceType,
    referenceId = referenceId,
    entriesJson = serializeEntries(entries),
    createdAtMillis = createdAtMillis
)

fun AccountEntity.toDomain(): Account = Account(
    id = AccountId(id),
    code = code,
    name = name,
    type = try { AccountType.valueOf(type) } catch (_: Exception) { AccountType.ASSET }
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id.value,
    code = code,
    name = name,
    type = type.name
)

private fun serializeEntries(entries: List<JournalEntry>): String {
    val arr = JSONArray()
    entries.forEach { e ->
        val obj = JSONObject()
        obj.put("accountId", e.accountId.value)
        obj.put("accountName", e.accountName)
        obj.put("debit", e.debit.amount.toPlainString())
        obj.put("credit", e.credit.amount.toPlainString())
        e.description?.let { obj.put("desc", it) }
        arr.put(obj)
    }
    return arr.toString()
}

private fun deserializeEntries(json: String): List<JournalEntry> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val debit = BigDecimal(obj.optString("debit", "0"))
            val credit = BigDecimal(obj.optString("credit", "0"))
            JournalEntry(
                accountId = AccountId(obj.getString("accountId")),
                accountName = obj.optString("accountName", ""),
                debit = Money(debit),
                credit = Money(credit),
                description = obj.optString("desc", null)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
