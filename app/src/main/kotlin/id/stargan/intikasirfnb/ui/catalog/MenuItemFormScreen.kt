package id.stargan.intikasirfnb.ui.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.shared.Money
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

        val item = MenuItem(
            id = existingItem?.id ?: ProductId.generate(),
            tenantId = tenantId,
            categoryId = catId,
            name = name.trim(),
            description = description.trim().ifBlank { null },
            imageUri = imageUri,
            basePrice = Money(price),
            sortOrder = sortOrder.toIntOrNull() ?: 0,
            isActive = existingItem?.isActive ?: true,
            modifierLinks = existingItem?.modifierLinks ?: emptyList()
        )
        viewModel.saveMenuItem(item)
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
