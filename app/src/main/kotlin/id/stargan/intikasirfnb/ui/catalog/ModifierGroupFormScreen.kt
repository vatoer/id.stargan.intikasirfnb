package id.stargan.intikasirfnb.ui.catalog

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.ModifierOption
import id.stargan.intikasirfnb.domain.catalog.ModifierOptionId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsGroupHeader
import id.stargan.intikasirfnb.ui.settings.components.StickyBottomSaveBar

/**
 * Mutable state holder for a single option row in the form.
 */
private data class OptionFormState(
    val id: ModifierOptionId = ModifierOptionId.generate(),
    var name: String = "",
    var priceDeltaText: String = "",
    var sortOrder: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifierGroupFormScreen(
    viewModel: CatalogViewModel,
    editGroupId: ModifierGroupId? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEdit = editGroupId != null
    val existingGroup = if (editGroupId != null)
        uiState.modifierGroups.find { it.id == editGroupId } else null

    var formInitialized by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var sortOrderText by remember { mutableStateOf("") }
    var isRequired by remember { mutableStateOf(false) }
    var isMultiSelect by remember { mutableStateOf(false) }
    var maxSelectionText by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf<OptionFormState>() }
    var saved by remember { mutableStateOf(false) }

    // Initialize form when existing group data loads
    LaunchedEffect(existingGroup) {
        if (existingGroup != null && !formInitialized) {
            groupName = existingGroup.name
            sortOrderText = if (existingGroup.sortOrder != 0) existingGroup.sortOrder.toString() else ""
            isRequired = existingGroup.isRequired
            isMultiSelect = existingGroup.maxSelection > 1
            maxSelectionText = if (existingGroup.maxSelection > 1) existingGroup.maxSelection.toString() else ""
            options.clear()
            existingGroup.options
                .sortedBy { it.sortOrder }
                .forEach { opt ->
                    options.add(
                        OptionFormState(
                            id = opt.id,
                            name = opt.name,
                            priceDeltaText = if (opt.priceDelta.isZero()) ""
                            else opt.priceDelta.amount.let {
                                if (it.stripTrailingZeros().scale() <= 0) it.toBigInteger().toString()
                                else it.toPlainString()
                            },
                            sortOrder = opt.sortOrder
                        )
                    )
                }
            formInitialized = true
        }
    }

    // For new groups, start with one empty option
    LaunchedEffect(Unit) {
        if (!isEdit && options.isEmpty()) {
            options.add(OptionFormState(sortOrder = 1))
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
        val groupId = existingGroup?.id ?: ModifierGroupId.generate()

        val modifierOptions = options.mapIndexed { index, opt ->
            ModifierOption(
                id = opt.id,
                groupId = groupId,
                name = opt.name.trim(),
                priceDelta = opt.priceDeltaText.toBigDecimalOrNull()?.let { Money(it) } ?: Money.zero(),
                sortOrder = if (opt.sortOrder != 0) opt.sortOrder else index + 1
            )
        }

        val maxSel = if (isMultiSelect) {
            (maxSelectionText.toIntOrNull() ?: modifierOptions.size)
                .coerceIn(2, modifierOptions.size)
        } else 1

        val group = ModifierGroup(
            id = groupId,
            tenantId = tenantId,
            name = groupName.trim(),
            options = modifierOptions,
            isRequired = isRequired,
            minSelection = if (isRequired) 1 else 0,
            maxSelection = maxSel,
            sortOrder = sortOrderText.toIntOrNull() ?: 0,
            isActive = existingGroup?.isActive ?: true
        )
        viewModel.saveModifierGroup(group)
        saved = true
    }

    val canSave = groupName.isNotBlank() &&
            options.isNotEmpty() &&
            options.all { it.name.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Modifier Group" else "Tambah Modifier Group") },
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
                        placeholder = { Text("contoh: Ukuran, Level Gula, Level Pedas") },
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

            // --- Selection Rules ---
            SettingsGroupHeader(title = "Aturan Pilihan")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Selection type chips
                    Text(
                        "Tipe Pilihan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isMultiSelect,
                            onClick = { isMultiSelect = false },
                            label = { Text("Pilih 1") }
                        )
                        FilterChip(
                            selected = isMultiSelect,
                            onClick = {
                                isMultiSelect = true
                                if (maxSelectionText.isBlank()) {
                                    maxSelectionText = options.size.coerceAtLeast(2).toString()
                                }
                            },
                            label = { Text("Pilih Beberapa") }
                        )
                    }

                    // Max selection (only for multi-select)
                    if (isMultiSelect) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = maxSelectionText,
                            onValueChange = { v ->
                                maxSelectionText = v.filter { it.isDigit() }
                            },
                            label = { Text("Maksimal pilihan") },
                            supportingText = {
                                Text("Dari ${options.size} opsi tersedia")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Required toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Wajib dipilih", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isRequired,
                            onCheckedChange = { isRequired = it }
                        )
                    }

                    // Hint text
                    Spacer(modifier = Modifier.height(4.dp))
                    val maxSel = if (isMultiSelect) (maxSelectionText.toIntOrNull() ?: options.size) else 1
                    val hintText = when {
                        !isMultiSelect && isRequired ->
                            "Kasir wajib pilih 1 opsi"
                        !isMultiSelect && !isRequired ->
                            "Kasir boleh pilih 1 opsi atau lewati"
                        isMultiSelect && isRequired ->
                            "Kasir wajib pilih minimal 1, maksimal $maxSel opsi"
                        else ->
                            "Kasir boleh pilih hingga $maxSel opsi atau lewati"
                    }
                    Text(
                        hintText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // --- Options ---
            SettingsGroupHeader(title = "Opsi (${options.size})")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    options.forEachIndexed { index, optState ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        OptionRow(
                            index = index,
                            state = optState,
                            canDelete = options.size > 1,
                            onNameChange = { options[index] = optState.copy(name = it) },
                            onPriceChange = { options[index] = optState.copy(priceDeltaText = it) },
                            onDelete = { options.removeAt(index) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            options.add(OptionFormState(sortOrder = options.size + 1))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Opsi")
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun OptionRow(
    index: Int,
    state: OptionFormState,
    canDelete: Boolean,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
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
                    label = { Text("Nama Opsi *") },
                    placeholder = { Text("contoh: Regular, Large") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.priceDeltaText,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            onPriceChange(filtered)
                        }
                    },
                    label = { Text("Tambahan Harga") },
                    placeholder = { Text("0 = tidak ada tambahan") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("+Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus opsi",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
