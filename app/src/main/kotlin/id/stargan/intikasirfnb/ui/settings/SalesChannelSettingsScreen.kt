package id.stargan.intikasirfnb.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.transaction.ChannelType
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.PlatformConfig
import id.stargan.intikasirfnb.domain.transaction.PriceAdjustmentType
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.defaultFlow
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesChannelSettingsScreen(
    viewModel: SalesChannelViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<SalesChannel?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Channel Penjualan") },
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
                Icon(Icons.Default.Add, contentDescription = "Tambah Channel")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (uiState.channels.isEmpty() && !uiState.isLoading) {
                item {
                    Text(
                        text = "Belum ada channel. Tekan + untuk menambahkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }
            items(uiState.channels, key = { it.id.value }) { channel ->
                SalesChannelCard(
                    channel = channel,
                    onEdit = { editingChannel = channel },
                    onDelete = { viewModel.deleteChannel(channel) },
                    onToggleActive = { viewModel.toggleActive(channel) }
                )
            }
        }
    }

    val tenantId = uiState.tenantId
    if (showAddDialog && tenantId != null) {
        SalesChannelDialog(
            channel = null,
            tenantId = tenantId,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveChannel(it)
                showAddDialog = false
            }
        )
    }

    editingChannel?.let { channel ->
        SalesChannelDialog(
            channel = channel,
            tenantId = channel.tenantId,
            onDismiss = { editingChannel = null },
            onSave = {
                viewModel.saveChannel(it)
                editingChannel = null
            }
        )
    }
}

// ============================================================
// Channel Card
// ============================================================

@Composable
private fun SalesChannelCard(
    channel: SalesChannel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                channelTypeIcon(channel.channelType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(channel.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    channelTypeLabel(channel.channelType) + " | Kode: ${channel.code}" +
                        " | ${orderFlowLabel(channel.defaultOrderFlow)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                channel.priceAdjustmentType?.let { type ->
                    val value = channel.priceAdjustmentValue ?: BigDecimal.ZERO
                    Text(
                        priceAdjustmentLabel(type, value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (channel.channelType == ChannelType.DELIVERY_PLATFORM) {
                    channel.platformConfig?.let { config ->
                        Text(
                            "${config.platformName} | Komisi: ${config.commissionPercent.toPlainString()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Switch(checked = channel.isActive, onCheckedChange = { onToggleActive() })
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ============================================================
// Add/Edit Dialog
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SalesChannelDialog(
    channel: SalesChannel?,
    tenantId: TenantId,
    onDismiss: () -> Unit,
    onSave: (SalesChannel) -> Unit
) {
    val isEdit = channel != null
    var name by remember { mutableStateOf(channel?.name ?: "") }
    var code by remember { mutableStateOf(channel?.code ?: "") }
    var channelType by remember { mutableStateOf(channel?.channelType ?: ChannelType.DINE_IN) }
    var sortOrder by remember { mutableStateOf(channel?.sortOrder?.toString() ?: "0") }
    var orderFlow by remember { mutableStateOf(channel?.defaultOrderFlow ?: channelType.defaultFlow()) }

    // Price adjustment
    var hasAdjustment by remember { mutableStateOf(channel?.priceAdjustmentType != null) }
    var adjustmentType by remember { mutableStateOf(channel?.priceAdjustmentType ?: PriceAdjustmentType.MARKUP_PERCENT) }
    var adjustmentValue by remember { mutableStateOf(channel?.priceAdjustmentValue?.toPlainString() ?: "") }

    // Platform config (for DELIVERY_PLATFORM)
    var platformName by remember { mutableStateOf(channel?.platformConfig?.platformName ?: "") }
    var commissionPercent by remember { mutableStateOf(channel?.platformConfig?.commissionPercent?.toPlainString() ?: "") }
    var requiresExternalOrderId by remember { mutableStateOf(channel?.platformConfig?.requiresExternalOrderId ?: true) }
    var autoConfirmOrder by remember { mutableStateOf(channel?.platformConfig?.autoConfirmOrder ?: false) }

    val canSave = name.isNotBlank() && code.isNotBlank() &&
        (!hasAdjustment || adjustmentValue.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true) &&
        (channelType != ChannelType.DELIVERY_PLATFORM || platformName.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Channel" else "Tambah Channel") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Channel Type
                Text("Tipe Channel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChannelType.entries.forEach { type ->
                        FilterChip(
                            selected = channelType == type,
                            onClick = {
                                channelType = type
                                // Auto-update order flow to match channel type default (only for new channels)
                                if (channel == null) orderFlow = type.defaultFlow()
                            },
                            label = { Text(channelTypeLabel(type), style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(channelTypeIcon(type), contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                // Name & Code
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Channel") },
                    placeholder = { Text("contoh: Dine In, GoFood") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(5) },
                    label = { Text("Kode (max 5 huruf)") },
                    placeholder = { Text("contoh: DI, TA, GF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it.filter { c -> c.isDigit() } },
                    label = { Text("Urutan") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Order flow
                Text("Alur Pemesanan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OrderFlowType.entries.forEach { flow ->
                        FilterChip(
                            selected = orderFlow == flow,
                            onClick = { orderFlow = flow },
                            label = { Text(orderFlowLabel(flow), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
                Text(
                    orderFlowDescription(orderFlow),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Price adjustment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Penyesuaian Harga", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    Switch(checked = hasAdjustment, onCheckedChange = { hasAdjustment = it })
                }

                AnimatedVisibility(visible = hasAdjustment) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PriceAdjustmentType.entries.forEach { type ->
                                FilterChip(
                                    selected = adjustmentType == type,
                                    onClick = { adjustmentType = type },
                                    label = { Text(adjustmentTypeLabel(type), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = adjustmentValue,
                            onValueChange = { adjustmentValue = it.filter { c -> c.isDigit() || c == '.' } },
                            label = {
                                Text(
                                    if (adjustmentType == PriceAdjustmentType.MARKUP_PERCENT || adjustmentType == PriceAdjustmentType.DISCOUNT_PERCENT)
                                        "Nilai (%)" else "Nilai (Rp)"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Platform config (DELIVERY_PLATFORM only)
                AnimatedVisibility(visible = channelType == ChannelType.DELIVERY_PLATFORM) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Konfigurasi Platform", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = platformName,
                            onValueChange = { platformName = it },
                            label = { Text("Nama Platform") },
                            placeholder = { Text("contoh: GoFood, GrabFood") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = commissionPercent,
                            onValueChange = { commissionPercent = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Komisi Platform (%)") },
                            placeholder = { Text("contoh: 20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Wajib External Order ID", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Switch(checked = requiresExternalOrderId, onCheckedChange = { requiresExternalOrderId = it })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto-Confirm Order", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Switch(checked = autoConfirmOrder, onCheckedChange = { autoConfirmOrder = it })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val platformConfig = if (channelType == ChannelType.DELIVERY_PLATFORM) {
                        PlatformConfig(
                            platformName = platformName.trim(),
                            commissionPercent = commissionPercent.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            requiresExternalOrderId = requiresExternalOrderId,
                            autoConfirmOrder = autoConfirmOrder
                        )
                    } else null

                    onSave(
                        SalesChannel(
                            id = channel?.id ?: SalesChannelId.generate(),
                            tenantId = tenantId,
                            channelType = channelType,
                            name = name.trim(),
                            code = code.trim().uppercase(),
                            defaultOrderFlow = orderFlow,
                            isActive = channel?.isActive ?: true,
                            sortOrder = sortOrder.toIntOrNull() ?: 0,
                            priceAdjustmentType = if (hasAdjustment) adjustmentType else null,
                            priceAdjustmentValue = if (hasAdjustment) adjustmentValue.toBigDecimalOrNull() else null,
                            platformConfig = platformConfig
                        )
                    )
                },
                enabled = canSave
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// ============================================================
// Utility functions
// ============================================================

private fun channelTypeLabel(type: ChannelType): String = when (type) {
    ChannelType.DINE_IN -> "Dine In"
    ChannelType.TAKE_AWAY -> "Take Away"
    ChannelType.DELIVERY_PLATFORM -> "Platform Delivery"
    ChannelType.OWN_DELIVERY -> "Delivery Sendiri"
}

private fun channelTypeIcon(type: ChannelType): ImageVector = when (type) {
    ChannelType.DINE_IN -> Icons.Default.Restaurant
    ChannelType.TAKE_AWAY -> Icons.Default.ShoppingBag
    ChannelType.DELIVERY_PLATFORM -> Icons.Default.DeliveryDining
    ChannelType.OWN_DELIVERY -> Icons.Default.Storefront
}

private fun adjustmentTypeLabel(type: PriceAdjustmentType): String = when (type) {
    PriceAdjustmentType.MARKUP_PERCENT -> "Markup %"
    PriceAdjustmentType.MARKUP_FIXED -> "Markup Rp"
    PriceAdjustmentType.DISCOUNT_PERCENT -> "Diskon %"
    PriceAdjustmentType.DISCOUNT_FIXED -> "Diskon Rp"
}

private fun priceAdjustmentLabel(type: PriceAdjustmentType, value: BigDecimal): String = when (type) {
    PriceAdjustmentType.MARKUP_PERCENT -> "Markup +${value.toPlainString()}%"
    PriceAdjustmentType.MARKUP_FIXED -> "Markup +Rp ${value.toLong()}"
    PriceAdjustmentType.DISCOUNT_PERCENT -> "Diskon -${value.toPlainString()}%"
    PriceAdjustmentType.DISCOUNT_FIXED -> "Diskon -Rp ${value.toLong()}"
}

private fun orderFlowLabel(flow: OrderFlowType): String = when (flow) {
    OrderFlowType.PAY_FIRST -> "Bayar Dulu"
    OrderFlowType.PAY_LAST -> "Bayar Akhir"
    OrderFlowType.PAY_FLEXIBLE -> "Fleksibel"
}

private fun orderFlowDescription(flow: OrderFlowType): String = when (flow) {
    OrderFlowType.PAY_FIRST -> "Pelanggan bayar di kasir, dapat nomor antrian, lalu pesanan disiapkan."
    OrderFlowType.PAY_LAST -> "Pesanan dikirim ke dapur dulu, bayar setelah selesai makan."
    OrderFlowType.PAY_FLEXIBLE -> "Kasir bisa pilih bayar dulu atau bayar akhir per transaksi."
}
