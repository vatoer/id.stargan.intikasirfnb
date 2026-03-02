package id.stargan.intikasirfnb.domain.usecase.customer

import id.stargan.intikasirfnb.domain.customer.Customer
import id.stargan.intikasirfnb.domain.customer.CustomerRepository

class SaveCustomerUseCase(private val customerRepository: CustomerRepository) {
    suspend operator fun invoke(customer: Customer) {
        customerRepository.save(customer)
    }
}
