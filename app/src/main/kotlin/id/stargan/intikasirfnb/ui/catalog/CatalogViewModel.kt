package id.stargan.intikasirfnb.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TenantId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val categories: List<Category> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val modifierGroups: List<ModifierGroup> = emptyList(),
    val tenantId: TenantId? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val categoryRepository: CategoryRepository,
    private val menuItemRepository: MenuItemRepository,
    private val modifierGroupRepository: ModifierGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet() ?: return@launch
                val tenantId = outlet.tenantId

                val categories = categoryRepository.listByTenant(tenantId)
                val menuItems = menuItemRepository.listByTenant(tenantId)
                val modifierGroups = modifierGroupRepository.listByTenant(tenantId)

                _uiState.update {
                    it.copy(
                        categories = categories,
                        menuItems = menuItems,
                        modifierGroups = modifierGroups,
                        tenantId = tenantId,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // --- Category ---

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.save(category)
                reloadCategories()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteCategory(id: CategoryId) {
        viewModelScope.launch {
            try {
                categoryRepository.delete(id)
                reloadCategories()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Gagal hapus: kategori mungkin masih memiliki menu item")
                }
            }
        }
    }

    // --- Menu Item ---

    fun saveMenuItem(menuItem: MenuItem) {
        viewModelScope.launch {
            try {
                require(menuItem.name.isNotBlank()) { "Nama menu harus diisi" }
                require(menuItem.basePrice.isPositive()) { "Harga harus lebih dari 0" }
                menuItemRepository.save(menuItem)
                reloadMenuItems()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteMenuItem(id: ProductId) {
        viewModelScope.launch {
            try {
                modifierGroupRepository.deleteAllLinksForItem(id)
                menuItemRepository.delete(id)
                reloadMenuItems()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Gagal hapus menu item: ${e.message}") }
            }
        }
    }

    fun getMenuItemById(id: ProductId): MenuItem? {
        return _uiState.value.menuItems.find { it.id == id }
    }

    // --- Modifier Group ---

    fun saveModifierGroup(group: ModifierGroup) {
        viewModelScope.launch {
            try {
                require(group.name.isNotBlank()) { "Nama modifier group harus diisi" }
                require(group.options.isNotEmpty()) { "Minimal harus ada 1 opsi" }
                group.options.forEach { opt ->
                    require(opt.name.isNotBlank()) { "Nama opsi tidak boleh kosong" }
                }
                modifierGroupRepository.save(group)
                reloadModifierGroups()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteModifierGroup(id: ModifierGroupId) {
        viewModelScope.launch {
            try {
                modifierGroupRepository.delete(id)
                reloadModifierGroups()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Gagal hapus: modifier mungkin masih digunakan menu item")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun reloadCategories() {
        val tenantId = _uiState.value.tenantId ?: return
        val categories = categoryRepository.listByTenant(tenantId)
        _uiState.update { it.copy(categories = categories) }
    }

    private suspend fun reloadMenuItems() {
        val tenantId = _uiState.value.tenantId ?: return
        val menuItems = menuItemRepository.listByTenant(tenantId)
        _uiState.update { it.copy(menuItems = menuItems) }
    }

    private suspend fun reloadModifierGroups() {
        val tenantId = _uiState.value.tenantId ?: return
        val groups = modifierGroupRepository.listByTenant(tenantId)
        _uiState.update { it.copy(modifierGroups = groups) }
    }
}
