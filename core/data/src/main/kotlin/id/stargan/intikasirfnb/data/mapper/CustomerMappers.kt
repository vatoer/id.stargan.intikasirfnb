package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.CustomerEntity
import id.stargan.intikasirfnb.domain.customer.Address
import id.stargan.intikasirfnb.domain.customer.Customer
import id.stargan.intikasirfnb.domain.customer.CustomerId
import id.stargan.intikasirfnb.domain.identity.TenantId

fun CustomerEntity.toDomain(): Customer = Customer(
    id = CustomerId(id),
    tenantId = TenantId(tenantId),
    name = name,
    phone = phone,
    email = email,
    address = if (addressLine1 != null) Address(
        line1 = addressLine1,
        line2 = addressLine2,
        city = city,
        postalCode = postalCode
    ) else null,
    loyaltyPoints = loyaltyPoints
)

fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = id.value,
    tenantId = tenantId.value,
    name = name,
    phone = phone,
    email = email,
    addressLine1 = address?.line1,
    addressLine2 = address?.line2,
    city = address?.city,
    postalCode = address?.postalCode,
    loyaltyPoints = loyaltyPoints
)
