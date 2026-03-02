package id.stargan.intikasirfnb.domain.usecase.customer

import id.stargan.intikasirfnb.domain.customer.Customer
import id.stargan.intikasirfnb.domain.customer.CustomerId
import id.stargan.intikasirfnb.domain.customer.CustomerRepository

class GetCustomerByIdUseCase(private val customerRepository: CustomerRepository) {
    suspend operator fun invoke(customerId: CustomerId): Customer? =
        customerRepository.getById(customerId)
}
