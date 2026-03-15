package id.stargan.intikasirfnb.ui.inventory

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.inventory.StockLevel
import id.stargan.intikasirfnb.domain.inventory.StockMovementType
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockManagementScreen(
    viewModel: StockViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var adjustDialog by remember { mutableStateOf<StockLevel?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stok")
                        if (uiState.lowStockCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text("${uiState.lowStockCount}")
                            }
                        }
                    }
                },
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
            if (uiState.unregisteredItems.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Item Stok")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Cari produk...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.filteredStock.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Belum ada data stok",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tekan + untuk menambahkan item ke stok",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredStock, key = { it.id.value }) { stock ->
                        StockCard(
                            stock = stock,
                            onAdjust = { adjustDialog = stock }
                        )
                    }
                }
            }
        }
    }

    // Adjust dialog
    adjustDialog?.let { stock ->
        StockAdjustDialog(
            stock = stock,
            onDismiss = { adjustDialog = null },
            onAdjust = { qty, type, notes ->
                viewModel.adjustStock(stock, qty, type, notes)
                adjustDialog = null
            },
            onReceive = { qty, notes ->
                viewModel.receiveStock(stock, qty, notes)
                adjustDialog = null
            }
        )
    }

    // Add item dialog
    if (showAddDialog && uiState.unregisteredItems.isNotEmpty()) {
        AddStockItemDialog(
            items = uiState.unregisteredItems,
            onDismiss = { showAddDialog = false },
            onAdd = { item, qty ->
                viewModel.registerItem(item, qty)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun StockCard(
    stock: StockLevel,
    onAdjust: () -> Unit
) {
    Card(
        onClick = onAdjust,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stock.isLowStock)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stock.productName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stock.unitOfMeasure.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (stock.isLowStock) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Stok rendah",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                stock.quantity.stripTrailingZeros().toPlainString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (stock.isLowStock) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StockAdjustDialog(
    stock: StockLevel,
    onDismiss: () -> Unit,
    onAdjust: (BigDecimal, StockMovementType, String?) -> Unit,
    onReceive: (BigDecimal, String?) -> Unit
) {
    var mode by remember { mutableStateOf("adjust") } // "adjust" or "receive"
    var qtyText by remember { mutableStateOf(if (mode == "adjust") stock.quantity.stripTrailingZeros().toPlainString() else "") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stock.productName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stok saat ini: ${stock.quantity.stripTrailingZeros().toPlainString()} ${stock.unitOfMeasure.name}",
                    style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "adjust",
                        onClick = {
                            mode = "adjust"
                            qtyText = stock.quantity.stripTrailingZeros().toPlainString()
                        },
                        label = { Text("Koreksi") }
                    )
                    FilterChip(
                        selected = mode == "receive",
                        onClick = {
                            mode = "receive"
                            qtyText = ""
                        },
                        label = { Text("Terima Barang") }
                    )
                    FilterChip(
                        selected = mode == "waste",
                        onClick = {
                            mode = "waste"
                            qtyText = ""
                        },
                        label = { Text("Waste") }
                    )
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = {
                        Text(when (mode) {
                            "adjust" -> "Jumlah baru"
                            "receive" -> "Jumlah masuk"
                            else -> "Jumlah waste"
                        })
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = qtyText.toBigDecimalOrNull() ?: return@TextButton
                    val notesTrimmed = notes.trim().takeIf { it.isNotBlank() }
                    when (mode) {
                        "adjust" -> onAdjust(qty, StockMovementType.ADJUSTMENT, notesTrimmed)
                        "receive" -> onReceive(qty, notesTrimmed)
                        "waste" -> {
                            val newQty = (stock.quantity - qty).coerceAtLeast(BigDecimal.ZERO)
                            onAdjust(newQty, StockMovementType.WASTE, notesTrimmed)
                        }
                    }
                },
                enabled = qtyText.toBigDecimalOrNull() != null
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun AddStockItemDialog(
    items: List<id.stargan.intikasirfnb.domain.catalog.MenuItem>,
    onDismiss: () -> Unit,
    onAdd: (id.stargan.intikasirfnb.domain.catalog.MenuItem, BigDecimal) -> Unit
) {
    var selectedItem by remember { mutableStateOf<id.stargan.intikasirfnb.domain.catalog.MenuItem?>(null) }
    var qtyText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Item ke Stok") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pilih menu item:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(items, key = { it.id.value }) { item ->
                        Card(
                            onClick = { selectedItem = item },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedItem?.id == item.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                item.name,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (selectedItem != null) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Stok awal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val item = selectedItem ?: return@TextButton
                    val qty = qtyText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onAdd(item, qty)
                },
                enabled = selectedItem != null
            ) { Text("Tambah") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
