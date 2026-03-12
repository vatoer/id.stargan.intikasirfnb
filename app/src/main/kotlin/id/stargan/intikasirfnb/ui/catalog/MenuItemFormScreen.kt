package id.stargan.intikasirfnb.ui.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.AddOnGroupId
import id.stargan.intikasirfnb.domain.catalog.MenuItemAddOnLink
import id.stargan.intikasirfnb.domain.catalog.MenuItemModifierLink
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsGroupHeader
import id.stargan.intikasirfnb.ui.settings.components.StickyBottomSaveBar
import java.io.File
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItemFormScreen(
    viewModel: CatalogViewModel,
    editItemId: ProductId? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isEdit = editItemId != null

    // Resolve existing item once data is loaded
    val existingItem = if (editItemId != null) uiState.menuItems.find { it.id == editItemId } else null
    var formInitialized by remember { mutableStateOf(false) }

    // Form state
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<CategoryId?>(null) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var sortOrder by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // Modifier link state: groupId -> LinkConfig (enabled, isRequired, min, max)
    data class ModifierLinkConfig(
        val enabled: Boolean = false,
        val isRequired: Boolean = false,
        val minSelection: Int = 0,
        val maxSelection: Int = 1,
        val existingLinkId: String? = null // preserve ID on edit
    )
    val modifierLinkConfigs = remember { mutableStateMapOf<String, ModifierLinkConfig>() }
    var modifierLinksLoaded by remember { mutableStateOf(false) }

    // Add-on link state: groupId -> AddOnLinkConfig (enabled, existingLinkId)
    data class AddOnLinkConfig(
        val enabled: Boolean = false,
        val existingLinkId: String? = null
    )
    val addOnLinkConfigs = remember { mutableStateMapOf<String, AddOnLinkConfig>() }
    var addOnLinksLoaded by remember { mutableStateOf(false) }

    // Load existing modifier links for edit mode
    LaunchedEffect(editItemId) {
        if (editItemId != null && !modifierLinksLoaded) {
            val links = viewModel.getLinksForItem(editItemId)
            links.forEach { link ->
                modifierLinkConfigs[link.modifierGroupId.value] = ModifierLinkConfig(
                    enabled = true,
                    isRequired = link.isRequired,
                    minSelection = link.minSelection,
                    maxSelection = link.maxSelection,
                    existingLinkId = link.id
                )
            }
            modifierLinksLoaded = true
        }
        if (editItemId != null && !addOnLinksLoaded) {
            val addOnLinks = viewModel.getAddOnLinksForItem(editItemId)
            addOnLinks.forEach { link ->
                addOnLinkConfigs[link.addOnGroupId.value] = AddOnLinkConfig(
                    enabled = true,
                    existingLinkId = link.id
                )
            }
            addOnLinksLoaded = true
        }
    }

    // Initialize form when existing item is loaded
    LaunchedEffect(existingItem) {
        if (existingItem != null && !formInitialized) {
            name = existingItem.name
            description = existingItem.description ?: ""
            priceText = existingItem.basePrice.amount.let {
                if (it.stripTrailingZeros().scale() <= 0) it.toBigInteger().toString()
                else it.toPlainString()
            }
            selectedCategoryId = existingItem.categoryId
            imageUri = existingItem.imageUri
            sortOrder = if (existingItem.sortOrder != 0) existingItem.sortOrder.toString() else ""
            formInitialized = true
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

    fun copyImageToInternal(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val dir = File(context.filesDir, "menu_images")
            dir.mkdirs()
            val file = File(dir, "menu_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> inputStream.copyTo(out) }
            inputStream.close()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = copyImageToInternal(it) }
    }

    fun save() {
        val tenantId = uiState.tenantId ?: return
        val catId = selectedCategoryId ?: return
        val price = priceText.toBigDecimalOrNull() ?: return

        val itemId = existingItem?.id ?: ProductId.generate()

        // Build modifier links from config
        val links = modifierLinkConfigs.entries
            .filter { it.value.enabled }
            .mapIndexed { index, (groupId, config) ->
                MenuItemModifierLink(
                    id = config.existingLinkId ?: UlidGenerator.generate(),
                    menuItemId = itemId,
                    modifierGroupId = ModifierGroupId(groupId),
                    sortOrder = index,
                    isRequired = config.isRequired,
                    minSelection = config.minSelection,
                    maxSelection = config.maxSelection
                )
            }

        // Build add-on links from config
        val addOnLinks = addOnLinkConfigs.entries
            .filter { it.value.enabled }
            .mapIndexed { index, (groupId, config) ->
                MenuItemAddOnLink(
                    id = config.existingLinkId ?: UlidGenerator.generate(),
                    menuItemId = itemId,
                    addOnGroupId = AddOnGroupId(groupId),
                    sortOrder = index
                )
            }

        val item = MenuItem(
            id = itemId,
            tenantId = tenantId,
            categoryId = catId,
            name = name.trim(),
            description = description.trim().ifBlank { null },
            imageUri = imageUri,
            basePrice = Money(price),
            sortOrder = sortOrder.toIntOrNull() ?: 0,
            isActive = existingItem?.isActive ?: true,
            modifierLinks = links,
            addOnLinks = addOnLinks
        )
        viewModel.saveMenuItem(item)
        viewModel.saveModifierLinks(itemId, links)
        viewModel.saveAddOnLinks(itemId, addOnLinks)
        saved = true
    }

    val canSave = name.isNotBlank() &&
            selectedCategoryId != null &&
            (priceText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } ?: false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Menu" else "Tambah Menu") },
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
                label = if (isEdit) "Simpan Perubahan" else "Simpan Menu"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Image ---
            SettingsGroupHeader(title = "Foto Menu", modifier = Modifier.padding(top = 8.dp))
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (imageUri != null) {
                        Box {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(imageUri!!))
                                    .build(),
                                contentDescription = "Foto menu",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { galleryLauncher.launch("image/*") },
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUri = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus foto",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tambah Foto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ketuk untuk memilih foto dari galeri",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Basic Info ---
            SettingsGroupHeader(title = "Informasi Menu")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Menu *") },
                        placeholder = { Text("contoh: Nasi Goreng Spesial") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi") },
                        placeholder = { Text("contoh: Nasi goreng dengan telur dan ayam") },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { newVal ->
                            // Only allow digits and one decimal point
                            val filtered = newVal.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                priceText = filtered
                            }
                        },
                        label = { Text("Harga (Rp) *") },
                        placeholder = { Text("contoh: 25000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("Rp ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Category ---
            SettingsGroupHeader(title = "Kategori *")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    CategoryDropdown(
                        categories = uiState.categories.filter { it.isActive },
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { selectedCategoryId = it }
                    )
                }
            }

            // --- Modifier Groups ---
            SettingsGroupHeader(title = "Modifier / Add-on")
            if (uiState.modifierGroups.isEmpty()) {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Belum ada modifier group. Buat dulu di menu Catalog → Modifier.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Pilih modifier group yang berlaku untuk menu ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.modifierGroups.filter { it.isActive }.forEachIndexed { index, group ->
                            val config = modifierLinkConfigs[group.id.value] ?: ModifierLinkConfig()
                            val optionCount = group.options.size

                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            // Group toggle row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = config.enabled,
                                    onCheckedChange = { checked ->
                                        // Smart defaults when enabling
                                        if (checked) {
                                            modifierLinkConfigs[group.id.value] = config.copy(
                                                enabled = true,
                                                isRequired = false,
                                                minSelection = 0,
                                                maxSelection = 1
                                            )
                                        } else {
                                            modifierLinkConfigs[group.id.value] = config.copy(enabled = false)
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "${optionCount} opsi" +
                                                group.options.take(3).joinToString(prefix = " (", postfix = if (optionCount > 3) ", ...)" else ")") { it.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Config detail (shown when enabled)
                            if (config.enabled) {
                                // Determine selection type from min/max
                                // PILIH_SATU: max=1 (single-select, optional or required)
                                // PILIH_BEBERAPA: max>1 (multi-select)
                                val isMultiSelect = config.maxSelection > 1

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 48.dp, end = 8.dp, bottom = 8.dp)
                                ) {
                                    // --- Selection Type chips ---
                                    Text(
                                        "Tipe Pilihan",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = !isMultiSelect,
                                            onClick = {
                                                modifierLinkConfigs[group.id.value] = config.copy(
                                                    maxSelection = 1,
                                                    minSelection = if (config.isRequired) 1 else 0
                                                )
                                            },
                                            label = { Text("Pilih 1") }
                                        )
                                        FilterChip(
                                            selected = isMultiSelect,
                                            onClick = {
                                                modifierLinkConfigs[group.id.value] = config.copy(
                                                    maxSelection = optionCount,
                                                    minSelection = if (config.isRequired) 1 else 0
                                                )
                                            },
                                            label = { Text("Pilih Beberapa") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // --- Required toggle ---
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Wajib dipilih", style = MaterialTheme.typography.bodyMedium)
                                        Switch(
                                            checked = config.isRequired,
                                            onCheckedChange = { required ->
                                                modifierLinkConfigs[group.id.value] = config.copy(
                                                    isRequired = required,
                                                    minSelection = if (required) 1 else 0
                                                )
                                            }
                                        )
                                    }

                                    // --- Max selection (only for multi-select) ---
                                    if (isMultiSelect) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = config.maxSelection.toString(),
                                            onValueChange = { v ->
                                                val max = v.filter { it.isDigit() }.toIntOrNull() ?: 1
                                                modifierLinkConfigs[group.id.value] = config.copy(
                                                    maxSelection = max.coerceIn(1, optionCount)
                                                )
                                            },
                                            label = { Text("Maksimal pilihan") },
                                            supportingText = { Text("Dari $optionCount opsi tersedia") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // --- Hint text ---
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val hintText = when {
                                        !isMultiSelect && config.isRequired ->
                                            "Kasir wajib pilih 1 opsi dari ${group.name}"
                                        !isMultiSelect && !config.isRequired ->
                                            "Kasir boleh pilih 1 opsi atau lewati"
                                        isMultiSelect && config.isRequired ->
                                            "Kasir wajib pilih minimal 1, maksimal ${config.maxSelection} opsi"
                                        else ->
                                            "Kasir boleh pilih hingga ${config.maxSelection} opsi atau lewati"
                                    }
                                    Text(
                                        hintText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Add-on Groups ---
            SettingsGroupHeader(title = "Add-on")
            if (uiState.addOnGroups.isEmpty()) {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Belum ada add-on group. Buat dulu di menu Catalog \u2192 Add-on.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Pilih add-on group yang berlaku untuk menu ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.addOnGroups.filter { it.isActive }.forEachIndexed { index, group ->
                            val config = addOnLinkConfigs[group.id.value] ?: AddOnLinkConfig()
                            val itemCount = group.items.size

                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = config.enabled,
                                    onCheckedChange = { checked ->
                                        addOnLinkConfigs[group.id.value] = config.copy(enabled = checked)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "${itemCount} item" +
                                                group.items.take(3).joinToString(prefix = " (", postfix = if (itemCount > 3) ", ...)" else ")") {
                                                    "${it.name} Rp${it.price.amount.toBigInteger()}"
                                                },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Sort Order ---
            SettingsGroupHeader(title = "Pengaturan Lain")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = sortOrder,
                        onValueChange = { sortOrder = it.filter { c -> c.isDigit() } },
                        label = { Text("Urutan Tampil") },
                        placeholder = { Text("0 = default") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: CategoryId?,
    onCategorySelected: (CategoryId) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.find { it.id == selectedCategoryId }?.name ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Pilih Kategori") },
            placeholder = { Text("Pilih kategori menu") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                val indent = if (category.parentId != null) "    " else ""
                DropdownMenuItem(
                    text = { Text("$indent${category.name}") },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
