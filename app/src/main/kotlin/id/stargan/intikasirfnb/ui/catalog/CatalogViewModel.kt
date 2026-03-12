package id.stargan.intikasirfnb.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.catalog.AddOnGroup
import id.stargan.intikasirfnb.domain.catalog.AddOnGroupId
import id.stargan.intikasirfnb.domain.catalog.AddOnGroupRepository
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository
import id.stargan.intikasirfnb.domain.catalog.MenuItemAddOnLink
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.MenuItemModifierLink
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
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
    val addOnGroups: List<AddOnGroup> = emptyList(),
    val tenantId: TenantId? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val categoryRepository: CategoryRepository,
    private val menuItemRepository: MenuItemRepository,
    private val modifierGroupRepository: ModifierGroupRepository,
    private val addOnGroupRepository: AddOnGroupRepository
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
                val addOnGroups = addOnGroupRepository.listByTenant(tenantId)

                _uiState.update {
                    it.copy(
                        categories = categories,
                        menuItems = menuItems,
                        modifierGroups = modifierGroups,
                        addOnGroups = addOnGroups,
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
                addOnGroupRepository.deleteAllLinksForItem(id)
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

    // --- Add-on Group ---

    fun saveAddOnGroup(group: AddOnGroup) {
        viewModelScope.launch {
            try {
                require(group.name.isNotBlank()) { "Nama add-on group harus diisi" }
                require(group.items.isNotEmpty()) { "Minimal harus ada 1 item" }
                group.items.forEach { item ->
                    require(item.name.isNotBlank()) { "Nama item tidak boleh kosong" }
                    require(item.price.isPositive()) { "Harga item harus lebih dari 0" }
                }
                addOnGroupRepository.save(group)
                reloadAddOnGroups()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteAddOnGroup(id: AddOnGroupId) {
        viewModelScope.launch {
            try {
                addOnGroupRepository.delete(id)
                reloadAddOnGroups()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Gagal hapus: add-on mungkin masih digunakan menu item")
                }
            }
        }
    }

    // --- Add-on Links (assign add-on groups to menu items) ---

    suspend fun getAddOnLinksForItem(menuItemId: ProductId): List<MenuItemAddOnLink> {
        return addOnGroupRepository.getLinksForItem(menuItemId)
    }

    fun saveAddOnLinks(menuItemId: ProductId, links: List<MenuItemAddOnLink>) {
        viewModelScope.launch {
            try {
                addOnGroupRepository.deleteAllLinksForItem(menuItemId)
                links.forEach { link ->
                    addOnGroupRepository.saveLink(link)
                }
                reloadMenuItems()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Gagal menyimpan add-on: ${e.message}") }
            }
        }
    }

    // --- Modifier Links (assign modifier groups to menu items) ---

    suspend fun getLinksForItem(menuItemId: ProductId): List<MenuItemModifierLink> {
        return modifierGroupRepository.getLinksForItem(menuItemId)
    }

    fun saveModifierLinks(menuItemId: ProductId, links: List<MenuItemModifierLink>) {
        viewModelScope.launch {
            try {
                modifierGroupRepository.deleteAllLinksForItem(menuItemId)
                links.forEach { link ->
                    modifierGroupRepository.saveLink(link)
                }
                reloadMenuItems()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Gagal menyimpan modifier: ${e.message}") }
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

    private suspend fun reloadAddOnGroups() {
        val tenantId = _uiState.value.tenantId ?: return
        val groups = addOnGroupRepository.listByTenant(tenantId)
        _uiState.update { it.copy(addOnGroups = groups) }
    }
}
