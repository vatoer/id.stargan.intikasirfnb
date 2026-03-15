package id.stargan.intikasirfnb.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.customer.Customer
import id.stargan.intikasirfnb.domain.customer.CustomerId
import id.stargan.intikasirfnb.domain.customer.CustomerRepository
import id.stargan.intikasirfnb.domain.identity.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerUiState(
    val customers: List<Customer> = emptyList(),
    val tenantId: id.stargan.intikasirfnb.domain.identity.TenantId? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val filteredCustomers: List<Customer>
        get() = if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone?.contains(searchQuery) == true
        }
}

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val tenantId = sessionManager.getCurrentOutlet()?.tenantId ?: return@launch
                val customers = customerRepository.listByTenant(tenantId)
                _uiState.update { it.copy(customers = customers, tenantId = tenantId, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                customerRepository.save(customer)
                loadCustomers()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteCustomer(id: CustomerId) {
        viewModelScope.launch {
            try {
                customerRepository.delete(id)
                loadCustomers()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
