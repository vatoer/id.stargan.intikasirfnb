package id.stargan.intikasirfnb.ui.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.catalog.AddOnGroup
import id.stargan.intikasirfnb.domain.catalog.AddOnGroupId
import id.stargan.intikasirfnb.domain.catalog.AddOnItem
import id.stargan.intikasirfnb.domain.catalog.AddOnItemId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsGroupHeader
import id.stargan.intikasirfnb.ui.settings.components.StickyBottomSaveBar

/**
 * Mutable state holder for a single add-on item row in the form.
 */
private data class AddOnItemFormState(
    val id: AddOnItemId = AddOnItemId.generate(),
    var name: String = "",
    var priceText: String = "",
    var maxQtyText: String = "5",
    var sortOrder: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOnGroupFormScreen(
    viewModel: CatalogViewModel,
    editGroupId: AddOnGroupId? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEdit = editGroupId != null
    val existingGroup = if (editGroupId != null)
        uiState.addOnGroups.find { it.id == editGroupId } else null

    var formInitialized by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var sortOrderText by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<AddOnItemFormState>() }
    var saved by remember { mutableStateOf(false) }

    // Initialize form when existing group data loads
    LaunchedEffect(existingGroup) {
        if (existingGroup != null && !formInitialized) {
            groupName = existingGroup.name
            sortOrderText = if (existingGroup.sortOrder != 0) existingGroup.sortOrder.toString() else ""
            items.clear()
            existingGroup.items
                .sortedBy { it.sortOrder }
                .forEach { item ->
                    items.add(
                        AddOnItemFormState(
                            id = item.id,
                            name = item.name,
                            priceText = if (item.price.isZero()) ""
                            else item.price.amount.let {
                                if (it.stripTrailingZeros().scale() <= 0) it.toBigInteger().toString()
                                else it.toPlainString()
                            },
                            maxQtyText = item.maxQty.toString(),
                            sortOrder = item.sortOrder
                        )
                    )
                }
            formInitialized = true
        }
    }

    // For new groups, start with one empty item
    LaunchedEffect(Unit) {
        if (!isEdit && items.isEmpty()) {
            items.add(AddOnItemFormState(sortOrder = 1))
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(saved) {
        if (saved) onNavigateBack()
    }

    fun save() {
        val tenantId = uiState.tenantId ?: return
        val groupId = existingGroup?.id ?: AddOnGroupId.generate()

        val addOnItems = items.mapIndexed { index, itemState ->
            AddOnItem(
                id = itemState.id,
                groupId = groupId,
                name = itemState.name.trim(),
                price = itemState.priceText.toBigDecimalOrNull()?.let { Money(it) } ?: Money.zero(),
                maxQty = itemState.maxQtyText.toIntOrNull() ?: 5,
                sortOrder = if (itemState.sortOrder != 0) itemState.sortOrder else index + 1
            )
        }

        val group = AddOnGroup(
            id = groupId,
            tenantId = tenantId,
            name = groupName.trim(),
            items = addOnItems,
            sortOrder = sortOrderText.toIntOrNull() ?: 0,
            isActive = existingGroup?.isActive ?: true
        )
        viewModel.saveAddOnGroup(group)
        saved = true
    }

    val canSave = groupName.isNotBlank() &&
            items.isNotEmpty() &&
            items.all { it.name.isNotBlank() && (it.priceText.toBigDecimalOrNull()?.let { p -> p > java.math.BigDecimal.ZERO } ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Add-on Group" else "Tambah Add-on Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            StickyBottomSaveBar(
                visible = canSave,
                onSave = ::save,
                label = if (isEdit) "Simpan Perubahan" else "Simpan"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Group Info ---
            SettingsGroupHeader(title = "Informasi Group", modifier = Modifier.padding(top = 8.dp))
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Nama Group *") },
                        placeholder = { Text("contoh: Extra Topping, Extra Shot") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sortOrderText,
                        onValueChange = { sortOrderText = it.filter { c -> c.isDigit() } },
                        label = { Text("Urutan Tampil") },
                        placeholder = { Text("0 = default") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Items ---
            SettingsGroupHeader(title = "Item Add-on (${items.size})")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    items.forEachIndexed { index, itemState ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        AddOnItemRow(
                            index = index,
                            state = itemState,
                            canDelete = items.size > 1,
                            onNameChange = { items[index] = itemState.copy(name = it) },
                            onPriceChange = { items[index] = itemState.copy(priceText = it) },
                            onMaxQtyChange = { items[index] = itemState.copy(maxQtyText = it) },
                            onDelete = { items.removeAt(index) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            items.add(AddOnItemFormState(sortOrder = items.size + 1))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Item")
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AddOnItemRow(
    index: Int,
    state: AddOnItemFormState,
    canDelete: Boolean,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onMaxQtyChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(32.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Nama Item *") },
                    placeholder = { Text("contoh: Extra Cheese, Extra Shot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.priceText,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            onPriceChange(filtered)
                        }
                    },
                    label = { Text("Harga *") },
                    placeholder = { Text("contoh: 5000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.maxQtyText,
                    onValueChange = { newVal ->
                        onMaxQtyChange(newVal.filter { it.isDigit() })
                    },
                    label = { Text("Maks Qty") },
                    placeholder = { Text("5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus item",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
