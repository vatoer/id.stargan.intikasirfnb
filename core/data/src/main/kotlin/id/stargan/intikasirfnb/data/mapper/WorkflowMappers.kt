package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.KitchenTicketEntity
import id.stargan.intikasirfnb.data.local.entity.KitchenTicketItemEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.workflow.KitchenStationType
import id.stargan.intikasirfnb.domain.workflow.KitchenTicket
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketId
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketItem
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketStatus

fun KitchenTicketEntity.toDomain(items: List<KitchenTicketItem>): KitchenTicket = KitchenTicket(
    id = KitchenTicketId(id),
    saleId = SaleId(saleId),
    outletId = OutletId(outletId),
    items = items,
    station = KitchenStationType.valueOf(station),
    status = KitchenTicketStatus.valueOf(status),
    assignedTo = assignedTo?.let { UserId(it) },
    tableName = tableName,
    channelName = channelName,
    ticketNumber = ticketNumber,
    createdAtMillis = createdAtMillis,
    startedAtMillis = startedAtMillis,
    readyAtMillis = readyAtMillis,
    servedAtMillis = servedAtMillis
)

fun KitchenTicket.toEntity(): KitchenTicketEntity = KitchenTicketEntity(
    id = id.value,
    saleId = saleId.value,
    outletId = outletId.value,
    station = station.name,
    status = status.name,
    assignedTo = assignedTo?.value,
    tableName = tableName,
    channelName = channelName,
    ticketNumber = ticketNumber,
    createdAtMillis = createdAtMillis,
    startedAtMillis = startedAtMillis,
    readyAtMillis = readyAtMillis,
    servedAtMillis = servedAtMillis
)

fun KitchenTicketItemEntity.toDomain(): KitchenTicketItem = KitchenTicketItem(
    id = id,
    orderLineId = OrderLineId(orderLineId),
    productName = productName,
    quantity = quantity,
    modifiers = modifiers,
    notes = notes
)

fun KitchenTicketItem.toEntity(ticketId: String): KitchenTicketItemEntity = KitchenTicketItemEntity(
    id = id,
    ticketId = ticketId,
    orderLineId = orderLineId.value,
    productName = productName,
    quantity = quantity,
    modifiers = modifiers,
    notes = notes
)
