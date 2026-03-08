package id.stargan.intikasirfnb.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TableRepository
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.usecase.transaction.AssignTableUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.DeleteTableUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.SaveTableUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TableManagementUiState(
    val tables: List<Table> = emptyList(),
    val sections: List<String> = emptyList(),
    val selectedSection: String? = null, // null = semua section
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    // For POS table picker mode
    val isPickerMode: Boolean = false,
    val currentSaleId: SaleId? = null
)

@HiltViewModel
class TableManagementViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val tableRepository: TableRepository,
    private val saveTableUseCase: SaveTableUseCase,
    private val deleteTableUseCase: DeleteTableUseCase,
    private val assignTableUseCase: AssignTableUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableManagementUiState())
    val uiState: StateFlow<TableManagementUiState> = _uiState.asStateFlow()

    init {
        loadTables()
    }

    fun loadTables() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")
                val tables = tableRepository.listByOutlet(outlet.id)
                val sections = tableRepository.listSections(outlet.id)
                _uiState.update {
                    it.copy(
                        tables = tables,
                        sections = sections,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun observeTables() {
        viewModelScope.launch {
            val outlet = sessionManager.getCurrentOutlet() ?: return@launch
            tableRepository.streamByOutlet(outlet.id).collect { tables ->
                val sections = tables.mapNotNull { it.section }.distinct().sorted()
                _uiState.update {
                    it.copy(tables = tables, sections = sections, isLoading = false)
                }
            }
        }
    }

    fun selectSection(section: String?) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    fun saveTable(
        existingId: TableId?,
        name: String,
        capacity: Int,
        section: String?
    ) {
        viewModelScope.launch {
            try {
                val outlet = sessionManager.getCurrentOutlet()
                    ?: error("Outlet tidak ditemukan")
                val table = if (existingId != null) {
                    val existing = tableRepository.getById(existingId)
                        ?: error("Meja tidak ditemukan")
                    existing.copy(
                        name = name.trim(),
                        capacity = capacity,
                        section = section?.trim()?.takeIf { it.isNotBlank() }
                    )
                } else {
                    Table(
                        id = TableId.generate(),
                        outletId = outlet.id,
                        name = name.trim(),
                        capacity = capacity,
                        section = section?.trim()?.takeIf { it.isNotBlank() }
                    )
                }
                saveTableUseCase(table).getOrThrow()
                loadTables()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteTable(tableId: TableId) {
        viewModelScope.launch {
            try {
                deleteTableUseCase(tableId).getOrThrow()
                loadTables()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun assignTableToSale(tableId: TableId, saleId: SaleId) {
        viewModelScope.launch {
            try {
                assignTableUseCase(saleId, tableId).getOrThrow()
                loadTables()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun setPickerMode(saleId: SaleId?) {
        _uiState.update {
            it.copy(isPickerMode = saleId != null, currentSaleId = saleId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
