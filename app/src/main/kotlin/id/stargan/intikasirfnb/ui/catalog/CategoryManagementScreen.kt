package id.stargan.intikasirfnb.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kategori Menu") },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kategori")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val categories = uiState.categories
        val menuItems = uiState.menuItems

        if (categories.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada kategori",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tekan + untuk menambahkan kategori menu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Build parent→children map for hierarchical display
            val rootCategories = categories.filter { it.parentId == null }
            val childrenMap = categories.groupBy { it.parentId }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                rootCategories.forEach { parent ->
                    item(key = parent.id.value) {
                        val itemCount = menuItems.count { it.categoryId == parent.id }
                        CategoryCard(
                            category = parent,
                            itemCount = itemCount,
                            indent = 0,
                            onEdit = { editingCategory = parent },
                            onDelete = { viewModel.deleteCategory(parent.id) },
                            onToggleActive = {
                                viewModel.saveCategory(parent.copy(isActive = !parent.isActive))
                            }
                        )
                    }
                    // Show children
                    val children = childrenMap[parent.id] ?: emptyList()
                    children.forEach { child ->
                        item(key = child.id.value) {
                            val childItemCount = menuItems.count { it.categoryId == child.id }
                            CategoryCard(
                                category = child,
                                itemCount = childItemCount,
                                indent = 1,
                                onEdit = { editingCategory = child },
                                onDelete = { viewModel.deleteCategory(child.id) },
                                onToggleActive = {
                                    viewModel.saveCategory(child.copy(isActive = !child.isActive))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        val tenantId = uiState.tenantId
        if (tenantId != null) {
            CategoryDialog(
                category = null,
                tenantId = tenantId,
                parentCategories = uiState.categories.filter { it.parentId == null },
                onDismiss = { showAddDialog = false },
                onSave = {
                    viewModel.saveCategory(it)
                    showAddDialog = false
                }
            )
        }
    }

    // Edit dialog
    editingCategory?.let { cat ->
        val tenantId = uiState.tenantId
        if (tenantId != null) {
            CategoryDialog(
                category = cat,
                tenantId = tenantId,
                parentCategories = uiState.categories.filter { it.parentId == null && it.id != cat.id },
                onDismiss = { editingCategory = null },
                onSave = {
                    viewModel.saveCategory(it)
                    editingCategory = null
                }
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    itemCount: Int,
    indent: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 24).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (category.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (indent == 0) Icons.Default.Folder else Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name + item count
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (category.isActive)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "$itemCount item" + if (!category.isActive) " (nonaktif)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Active toggle
            Switch(
                checked = category.isActive,
                onCheckedChange = { onToggleActive() }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Edit
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Delete
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    category: Category?,
    tenantId: id.stargan.intikasirfnb.domain.identity.TenantId,
    parentCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var sortOrder by remember { mutableIntStateOf(category?.sortOrder ?: 0) }
    var selectedParentId by remember { mutableStateOf(category?.parentId) }
    var showParentPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Tambah Kategori" else "Edit Kategori") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    placeholder = { Text("contoh: Makanan, Minuman, Dessert") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = if (sortOrder == 0) "" else sortOrder.toString(),
                    onValueChange = { sortOrder = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 },
                    label = { Text("Urutan") },
                    placeholder = { Text("0 = default") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Parent category selector
                if (parentCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Kategori Induk (opsional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Current selection
                    val parentName = parentCategories.find { it.id == selectedParentId }?.name
                    TextButton(
                        onClick = { showParentPicker = !showParentPicker },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(parentName ?: "Tidak ada (kategori utama)")
                    }

                    if (showParentPicker) {
                        // "None" option
                        TextButton(
                            onClick = {
                                selectedParentId = null
                                showParentPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Tidak ada (kategori utama)",
                                color = if (selectedParentId == null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                        parentCategories.forEach { parent ->
                            TextButton(
                                onClick = {
                                    selectedParentId = parent.id
                                    showParentPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    parent.name,
                                    color = if (selectedParentId == parent.id)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        Category(
                            id = category?.id ?: CategoryId.generate(),
                            tenantId = tenantId,
                            name = name.trim(),
                            parentId = selectedParentId,
                            sortOrder = sortOrder,
                            isActive = category?.isActive ?: true
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
