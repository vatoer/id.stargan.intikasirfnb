package id.stargan.intikasirfnb.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.settings.TaxConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfigId
import id.stargan.intikasirfnb.domain.settings.TaxScope
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTax by remember { mutableStateOf<TaxConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Pajak") },
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
                Icon(Icons.Default.Add, contentDescription = "Tambah Pajak")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            if (uiState.taxConfigs.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada pajak. Tekan + untuk menambahkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }
            items(uiState.taxConfigs, key = { it.id.value }) { tax ->
                TaxConfigCard(
                    tax = tax,
                    onEdit = { editingTax = tax },
                    onDelete = { viewModel.deleteTaxConfig(tax.id) },
                    onToggleActive = {
                        viewModel.saveTaxConfig(tax.copy(isActive = !tax.isActive))
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        val tenantId = uiState.outletSettings?.tenantId
        if (tenantId != null) {
            TaxConfigDialog(
                taxConfig = null,
                tenantId = tenantId,
                onDismiss = { showAddDialog = false },
                onSave = {
                    viewModel.saveTaxConfig(it)
                    showAddDialog = false
                }
            )
        }
    }

    editingTax?.let { tax ->
        TaxConfigDialog(
            taxConfig = tax,
            tenantId = tax.tenantId,
            onDismiss = { editingTax = null },
            onSave = {
                viewModel.saveTaxConfig(it)
                editingTax = null
            }
        )
    }
}

@Composable
private fun TaxConfigCard(
    tax: TaxConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tax.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${tax.rate.toPlainString()}%" +
                        if (tax.isIncludedInPrice) " (inklusif)" else " (eksklusif)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (tax.scope) {
                        TaxScope.ALL_ITEMS -> "Semua item"
                        TaxScope.SPECIFIC_CATEGORIES -> "Kategori tertentu"
                        TaxScope.SPECIFIC_ITEMS -> "Item tertentu"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = tax.isActive, onCheckedChange = { onToggleActive() })
            Spacer(modifier = Modifier.width(8.dp))
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
private fun TaxConfigDialog(
    taxConfig: TaxConfig?,
    tenantId: id.stargan.intikasirfnb.domain.identity.TenantId,
    onDismiss: () -> Unit,
    onSave: (TaxConfig) -> Unit
) {
    var name by remember { mutableStateOf(taxConfig?.name ?: "") }
    var rate by remember { mutableStateOf(taxConfig?.rate?.toPlainString() ?: "") }
    var isIncludedInPrice by remember { mutableStateOf(taxConfig?.isIncludedInPrice ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (taxConfig == null) "Tambah Pajak" else "Edit Pajak") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pajak") },
                    placeholder = { Text("contoh: PPN, PB1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Tarif (%)") },
                    placeholder = { Text("contoh: 11") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sudah termasuk harga", modifier = Modifier.weight(1f))
                    Switch(checked = isIncludedInPrice, onCheckedChange = { isIncludedInPrice = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedRate = rate.toBigDecimalOrNull() ?: return@TextButton
                    onSave(
                        TaxConfig(
                            id = taxConfig?.id ?: TaxConfigId.generate(),
                            tenantId = tenantId,
                            name = name.trim(),
                            rate = parsedRate,
                            scope = taxConfig?.scope ?: TaxScope.ALL_ITEMS,
                            isIncludedInPrice = isIncludedInPrice,
                            isActive = taxConfig?.isActive ?: true,
                            sortOrder = taxConfig?.sortOrder ?: 0
                        )
                    )
                },
                enabled = name.isNotBlank() && rate.toBigDecimalOrNull() != null
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
