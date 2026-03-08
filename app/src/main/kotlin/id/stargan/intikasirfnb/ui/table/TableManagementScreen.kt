package id.stargan.intikasirfnb.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TableStatus

// =============================================================
// Table Management Screen (Settings → Kelola Meja)
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableManagementScreen(
    viewModel: TableManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTable by remember { mutableStateOf<Table?>(null) }
    var deletingTable by remember { mutableStateOf<Table?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) { viewModel.observeTables() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Meja") },
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
                Icon(Icons.Default.Add, contentDescription = "Tambah Meja")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Section filter chips
            if (uiState.sections.isNotEmpty()) {
                SectionFilterRow(
                    sections = uiState.sections,
                    selectedSection = uiState.selectedSection,
                    onSectionSelected = { viewModel.selectSection(it) }
                )
            }

            // Summary bar
            val filteredTables = filterTables(uiState.tables, uiState.selectedSection)
            val available = filteredTables.count { it.isAvailable }
            val occupied = filteredTables.count { !it.isAvailable }
            TableSummaryBar(
                total = filteredTables.size,
                available = available,
                occupied = occupied
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredTables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TableBar,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Belum ada meja",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap + untuk menambah meja",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Table grid
                TableGrid(
                    tables = filteredTables,
                    onTableTap = { /* management mode: edit on tap */ editingTable = it },
                    onTableLongPress = { deletingTable = it },
                    isPickerMode = false
                )
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || editingTable != null) {
        TableFormDialog(
            table = editingTable,
            existingSections = uiState.sections,
            onDismiss = {
                showAddDialog = false
                editingTable = null
            },
            onSave = { id, name, capacity, section ->
                viewModel.saveTable(id, name, capacity, section)
                showAddDialog = false
                editingTable = null
            }
        )
    }

    // Delete confirmation
    if (deletingTable != null) {
        val table = deletingTable!!
        AlertDialog(
            onDismissRequest = { deletingTable = null },
            title = { Text("Hapus Meja") },
            text = {
                Text("Hapus meja \"${table.name}\"? Meja yang sedang digunakan tidak bisa dihapus.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTable(table.id)
                        deletingTable = null
                    }
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { deletingTable = null }) { Text("Batal") }
            }
        )
    }
}

// =============================================================
// Table Picker Bottom Sheet Content (for POS screen)
// =============================================================

@Composable
fun TablePickerContent(
    tables: List<Table>,
    sections: List<String>,
    selectedSection: String?,
    onSectionSelected: (String?) -> Unit,
    onTableSelected: (Table) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pilih Meja",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }

        // Section filter
        if (sections.isNotEmpty()) {
            SectionFilterRow(
                sections = sections,
                selectedSection = selectedSection,
                onSectionSelected = onSectionSelected
            )
        }

        // Summary
        val filtered = filterTables(tables, selectedSection)
        val available = filtered.count { it.isAvailable }
        TableSummaryBar(
            total = filtered.size,
            available = available,
            occupied = filtered.size - available
        )

        // Grid
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tidak ada meja tersedia",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            TableGrid(
                tables = filtered,
                onTableTap = { table ->
                    if (table.isAvailable) onTableSelected(table)
                },
                onTableLongPress = {},
                isPickerMode = true,
                modifier = Modifier.height(400.dp)
            )
        }
    }
}

// =============================================================
// Shared Components
// =============================================================

@Composable
private fun SectionFilterRow(
    sections: List<String>,
    selectedSection: String?,
    onSectionSelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedSection == null,
                onClick = { onSectionSelected(null) },
                label = { Text("Semua") }
            )
        }
        items(sections) { section ->
            FilterChip(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                label = { Text(section) }
            )
        }
    }
}

@Composable
private fun TableSummaryBar(total: Int, available: Int, occupied: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$total meja",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "$available kosong",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "$occupied terisi",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun TableGrid(
    tables: List<Table>,
    onTableTap: (Table) -> Unit,
    onTableLongPress: (Table) -> Unit,
    isPickerMode: Boolean,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(tables, key = { it.id.value }) { table ->
            TableCell(
                table = table,
                onClick = { onTableTap(table) },
                onLongClick = { onTableLongPress(table) },
                isPickerMode = isPickerMode
            )
        }
    }
}

@Composable
private fun TableCell(
    table: Table,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isPickerMode: Boolean
) {
    val isOccupied = !table.isAvailable
    val containerColor = when {
        isOccupied -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        isOccupied -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val borderColor = when {
        isOccupied -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPickerMode || !isOccupied) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOccupied) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Table icon
            Icon(
                Icons.Default.TableBar,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Table name
            Text(
                table.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Capacity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    "${table.capacity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            // Section label
            val sectionName = table.section
            if (sectionName != null) {
                Text(
                    sectionName,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status badge
            Spacer(modifier = Modifier.height(4.dp))
            val statusText = if (isOccupied) "Terisi" else "Kosong"
            Text(
                statusText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isOccupied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            // Edit/Delete in management mode
            if (!isPickerMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (!isOccupied) {
                        IconButton(
                            onClick = onLongClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableFormDialog(
    table: Table?,
    existingSections: List<String>,
    onDismiss: () -> Unit,
    onSave: (existingId: TableId?, name: String, capacity: Int, section: String?) -> Unit
) {
    val isEdit = table != null
    var name by remember { mutableStateOf(table?.name ?: "") }
    var capacityText by remember { mutableStateOf(table?.capacity?.toString() ?: "4") }
    var section by remember { mutableStateOf(table?.section ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Meja" else "Tambah Meja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Meja") },
                    placeholder = { Text("Contoh: A1, Meja 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter { c -> c.isDigit() } },
                    label = { Text("Kapasitas (kursi)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section (opsional)") },
                    placeholder = { Text("Contoh: Indoor, Outdoor, Smoking") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Quick section chips from existing sections
                if (existingSections.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(existingSections) { s ->
                            FilterChip(
                                selected = section == s,
                                onClick = { section = s },
                                label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cap = capacityText.toIntOrNull()?.coerceAtLeast(1) ?: 4
                    onSave(table?.id, name, cap, section.takeIf { it.isNotBlank() })
                },
                enabled = name.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun filterTables(tables: List<Table>, section: String?): List<Table> {
    if (section == null) return tables
    return tables.filter { it.section == section }
}
