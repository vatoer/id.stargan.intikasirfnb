package id.stargan.intikasirfnb.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.transaction.ChannelType
import id.stargan.intikasirfnb.domain.transaction.CommissionType
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.PlatformConfig
import id.stargan.intikasirfnb.domain.transaction.PlatformPaymentMethod
import id.stargan.intikasirfnb.domain.transaction.PriceAdjustmentType
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.TableMode
import id.stargan.intikasirfnb.domain.transaction.defaultTableMode
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.defaultFlow
import java.math.BigDecimal
import java.math.RoundingMode

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
    var showPlatformWizard by remember { mutableStateOf(false) }
    var editingPlatformChannel by remember { mutableStateOf<SalesChannel?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Platform wizard (full screen)
    if (showPlatformWizard || editingPlatformChannel != null) {
        val tenantId = uiState.tenantId
        if (tenantId != null) {
            PlatformChannelWizard(
                channel = editingPlatformChannel,
                tenantId = tenantId,
                onDismiss = {
                    showPlatformWizard = false
                    editingPlatformChannel = null
                },
                onSave = {
                    viewModel.saveChannel(it)
                    showPlatformWizard = false
                    editingPlatformChannel = null
                }
            )
            return // full screen, don't render scaffold
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
                    onEdit = {
                        if (channel.channelType == ChannelType.DELIVERY_PLATFORM) {
                            editingPlatformChannel = channel
                        } else {
                            editingChannel = channel
                        }
                    },
                    onDelete = { viewModel.deleteChannel(channel) },
                    onToggleActive = { viewModel.toggleActive(channel) }
                )
            }
        }
    }

    val tenantId = uiState.tenantId
    if (showAddDialog && tenantId != null) {
        ChannelTypePickerDialog(
            onDismiss = { showAddDialog = false },
            onTypeSelected = { type ->
                showAddDialog = false
                if (type == ChannelType.DELIVERY_PLATFORM) {
                    showPlatformWizard = true
                } else {
                    editingChannel = SalesChannel(
                        id = SalesChannelId.generate(),
                        tenantId = tenantId,
                        channelType = type,
                        name = "",
                        code = ""
                    )
                }
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
// Channel Type Picker — first step when adding new channel
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChannelTypePickerDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (ChannelType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Tipe Channel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChannelType.entries.forEach { type ->
                    Card(
                        onClick = { onTypeSelected(type) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                channelTypeIcon(type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(channelTypeLabel(type), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    channelTypeDescription(type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
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
                        " | ${orderFlowLabel(channel.defaultOrderFlow)}" +
                        if (channel.channelType == ChannelType.DINE_IN) " | ${tableModeLabel(channel.tableMode)}" else "",
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
                        val paymentLabel = platformPaymentMethodLabel(config.paymentMethod)
                        Text(
                            "${config.platformName} | Komisi: ${config.commissionPercent.toPlainString()}% | $paymentLabel",
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
// Generic Channel Dialog (non-platform channels)
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SalesChannelDialog(
    channel: SalesChannel?,
    tenantId: TenantId,
    onDismiss: () -> Unit,
    onSave: (SalesChannel) -> Unit
) {
    val isEdit = channel != null && channel.name.isNotBlank()
    var name by remember { mutableStateOf(channel?.name ?: "") }
    var code by remember { mutableStateOf(channel?.code ?: "") }
    val channelType = channel?.channelType ?: ChannelType.DINE_IN
    var sortOrder by remember { mutableStateOf(channel?.sortOrder?.toString() ?: "0") }
    var orderFlow by remember { mutableStateOf(channel?.defaultOrderFlow ?: channelType.defaultFlow()) }
    var tableMode by remember { mutableStateOf(channel?.tableMode ?: channelType.defaultTableMode()) }

    // Price adjustment
    var hasAdjustment by remember { mutableStateOf(channel?.priceAdjustmentType != null) }
    var adjustmentType by remember { mutableStateOf(channel?.priceAdjustmentType ?: PriceAdjustmentType.MARKUP_PERCENT) }
    var adjustmentValue by remember { mutableStateOf(channel?.priceAdjustmentValue?.toPlainString() ?: "") }

    val canSave = name.isNotBlank() && code.isNotBlank() &&
        (!hasAdjustment || adjustmentValue.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit ${channelTypeLabel(channelType)}" else "Tambah ${channelTypeLabel(channelType)}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name & Code
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Channel") },
                    placeholder = { Text("contoh: ${channelTypeLabel(channelType)}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(5) },
                    label = { Text("Kode (max 5 huruf)") },
                    placeholder = { Text("contoh: DI, TA") },
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

                // Table mode (only for DINE_IN)
                if (channelType == ChannelType.DINE_IN) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Pengaturan Meja", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TableMode.entries.forEach { mode ->
                            FilterChip(
                                selected = tableMode == mode,
                                onClick = { tableMode = mode },
                                label = { Text(tableModeLabel(mode), style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                    Text(
                        tableModeDescription(tableMode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        SalesChannel(
                            id = channel?.id ?: SalesChannelId.generate(),
                            tenantId = tenantId,
                            channelType = channelType,
                            name = name.trim(),
                            code = code.trim().uppercase(),
                            defaultOrderFlow = orderFlow,
                            tableMode = if (channelType == ChannelType.DINE_IN) tableMode else TableMode.NONE,
                            isActive = channel?.isActive ?: true,
                            sortOrder = sortOrder.toIntOrNull() ?: 0,
                            priceAdjustmentType = if (hasAdjustment) adjustmentType else null,
                            priceAdjustmentValue = if (hasAdjustment) adjustmentValue.toBigDecimalOrNull() else null
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
// PLATFORM CHANNEL WIZARD — full-screen multi-step setup
// ============================================================

private data class PlatformPreset(
    val name: String,
    val code: String,
    val defaultCommission: BigDecimal,
    val defaultMarkup: BigDecimal,
    val defaultPaymentMethod: PlatformPaymentMethod
)

private val PLATFORM_PRESETS = listOf(
    PlatformPreset("GoFood", "GF", BigDecimal("20"), BigDecimal("20"), PlatformPaymentMethod.PLATFORM_SETTLEMENT),
    PlatformPreset("GrabFood", "GB", BigDecimal("20"), BigDecimal("20"), PlatformPaymentMethod.PLATFORM_SETTLEMENT),
    PlatformPreset("ShopeeFood", "SF", BigDecimal("20"), BigDecimal("20"), PlatformPaymentMethod.PLATFORM_SETTLEMENT),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PlatformChannelWizard(
    channel: SalesChannel?,
    tenantId: TenantId,
    onDismiss: () -> Unit,
    onSave: (SalesChannel) -> Unit
) {
    val isEdit = channel != null
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    // Step 1: Platform selection
    var selectedPreset by remember { mutableStateOf<PlatformPreset?>(null) }
    var platformName by remember { mutableStateOf(channel?.platformConfig?.platformName ?: "") }
    var channelName by remember { mutableStateOf(channel?.name ?: "") }
    var channelCode by remember { mutableStateOf(channel?.code ?: "") }

    // Step 2: Commission & pricing
    var commissionPercent by remember { mutableStateOf(channel?.platformConfig?.commissionPercent?.toPlainString() ?: "") }
    var commissionType by remember { mutableStateOf(channel?.platformConfig?.commissionType ?: CommissionType.FROM_SELLING_PRICE) }
    var hasMarkup by remember { mutableStateOf(channel?.priceAdjustmentType != null) }
    var adjustmentType by remember { mutableStateOf(channel?.priceAdjustmentType ?: PriceAdjustmentType.MARKUP_PERCENT) }
    var adjustmentValue by remember { mutableStateOf(channel?.priceAdjustmentValue?.toPlainString() ?: "") }

    // Step 3: Payment & operations
    var paymentMethod by remember { mutableStateOf(channel?.platformConfig?.paymentMethod ?: PlatformPaymentMethod.PLATFORM_SETTLEMENT) }
    var requiresExternalOrderId by remember { mutableStateOf(channel?.platformConfig?.requiresExternalOrderId ?: true) }
    var autoConfirmOrder by remember { mutableStateOf(channel?.platformConfig?.autoConfirmOrder ?: false) }
    var orderFlow by remember { mutableStateOf(channel?.defaultOrderFlow ?: OrderFlowType.PAY_FIRST) }

    // Step 4: Summary (review)
    var sortOrder by remember { mutableStateOf(channel?.sortOrder?.toString() ?: "10") }

    // Apply preset
    fun applyPreset(preset: PlatformPreset) {
        selectedPreset = preset
        platformName = preset.name
        channelName = preset.name
        channelCode = preset.code
        commissionPercent = preset.defaultCommission.toPlainString()
        paymentMethod = preset.defaultPaymentMethod
        if (!isEdit) {
            hasMarkup = true
            adjustmentType = PriceAdjustmentType.MARKUP_PERCENT
            adjustmentValue = preset.defaultMarkup.toPlainString()
        }
    }

    val stepValid = when (currentStep) {
        0 -> platformName.isNotBlank() && channelName.isNotBlank() && channelCode.isNotBlank()
        1 -> commissionPercent.toBigDecimalOrNull() != null &&
            (!hasMarkup || adjustmentValue.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true)
        2 -> true
        3 -> true
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Platform" else "Tambah Platform") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onDismiss()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                Column {
                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { (currentStep + 1f) / totalSteps },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Langkah ${currentStep + 1} dari $totalSteps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (currentStep > 0) {
                                OutlinedButton(onClick = { currentStep-- }) {
                                    Text("Kembali")
                                }
                            }
                            if (currentStep < totalSteps - 1) {
                                Button(
                                    onClick = { currentStep++ },
                                    enabled = stepValid
                                ) {
                                    Text("Lanjut")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val config = PlatformConfig(
                                            platformName = platformName.trim(),
                                            commissionPercent = commissionPercent.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                                            commissionType = commissionType,
                                            paymentMethod = paymentMethod,
                                            requiresExternalOrderId = requiresExternalOrderId,
                                            autoConfirmOrder = autoConfirmOrder
                                        )
                                        onSave(
                                            SalesChannel(
                                                id = channel?.id ?: SalesChannelId.generate(),
                                                tenantId = tenantId,
                                                channelType = ChannelType.DELIVERY_PLATFORM,
                                                name = channelName.trim(),
                                                code = channelCode.trim().uppercase(),
                                                defaultOrderFlow = orderFlow,
                                                isActive = channel?.isActive ?: true,
                                                sortOrder = sortOrder.toIntOrNull() ?: 10,
                                                priceAdjustmentType = if (hasMarkup) adjustmentType else null,
                                                priceAdjustmentValue = if (hasMarkup) adjustmentValue.toBigDecimalOrNull() else null,
                                                platformConfig = config
                                            )
                                        )
                                    },
                                    enabled = stepValid
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simpan")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            label = "wizard_step"
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    0 -> WizardStepPlatform(
                        isEdit = isEdit,
                        selectedPreset = selectedPreset,
                        platformName = platformName,
                        channelName = channelName,
                        channelCode = channelCode,
                        onPresetSelected = { applyPreset(it) },
                        onPlatformNameChanged = { platformName = it },
                        onChannelNameChanged = { channelName = it },
                        onChannelCodeChanged = { channelCode = it.uppercase().take(5) }
                    )
                    1 -> WizardStepCommission(
                        commissionPercent = commissionPercent,
                        commissionType = commissionType,
                        hasMarkup = hasMarkup,
                        adjustmentType = adjustmentType,
                        adjustmentValue = adjustmentValue,
                        onCommissionPercentChanged = { commissionPercent = it.filter { c -> c.isDigit() || c == '.' } },
                        onCommissionTypeChanged = { commissionType = it },
                        onHasMarkupChanged = { hasMarkup = it },
                        onAdjustmentTypeChanged = { adjustmentType = it },
                        onAdjustmentValueChanged = { adjustmentValue = it.filter { c -> c.isDigit() || c == '.' } }
                    )
                    2 -> WizardStepPaymentOps(
                        paymentMethod = paymentMethod,
                        requiresExternalOrderId = requiresExternalOrderId,
                        autoConfirmOrder = autoConfirmOrder,
                        orderFlow = orderFlow,
                        onPaymentMethodChanged = { paymentMethod = it },
                        onRequiresExternalOrderIdChanged = { requiresExternalOrderId = it },
                        onAutoConfirmOrderChanged = { autoConfirmOrder = it },
                        onOrderFlowChanged = { orderFlow = it }
                    )
                    3 -> WizardStepSummary(
                        platformName = platformName,
                        channelName = channelName,
                        channelCode = channelCode,
                        commissionPercent = commissionPercent,
                        commissionType = commissionType,
                        hasMarkup = hasMarkup,
                        adjustmentType = adjustmentType,
                        adjustmentValue = adjustmentValue,
                        paymentMethod = paymentMethod,
                        orderFlow = orderFlow,
                        requiresExternalOrderId = requiresExternalOrderId,
                        autoConfirmOrder = autoConfirmOrder,
                        sortOrder = sortOrder,
                        onSortOrderChanged = { sortOrder = it.filter { c -> c.isDigit() } }
                    )
                }
            }
        }
    }
}

// --- Step 1: Platform selection ---

@Composable
private fun WizardStepPlatform(
    isEdit: Boolean,
    selectedPreset: PlatformPreset?,
    platformName: String,
    channelName: String,
    channelCode: String,
    onPresetSelected: (PlatformPreset) -> Unit,
    onPlatformNameChanged: (String) -> Unit,
    onChannelNameChanged: (String) -> Unit,
    onChannelCodeChanged: (String) -> Unit
) {
    WizardSectionTitle("Pilih Platform Delivery")
    Text(
        "Pilih dari template populer atau buat custom.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Preset cards
    PLATFORM_PRESETS.forEach { preset ->
        val isSelected = selectedPreset?.name == preset.name
        Card(
            onClick = { if (!isEdit) onPresetSelected(preset) },
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(preset.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Komisi ~${preset.defaultCommission.toPlainString()}% | Markup ~${preset.defaultMarkup.toPlainString()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Custom option
    Card(
        onClick = {
            if (!isEdit) {
                onPlatformNameChanged("")
                onChannelNameChanged("")
                onChannelCodeChanged("")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selectedPreset == null && platformName.isNotBlank()) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Platform Lainnya (Custom)", style = MaterialTheme.typography.titleSmall)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()

    // Editable fields
    WizardSectionTitle("Detail Channel")
    OutlinedTextField(
        value = platformName,
        onValueChange = onPlatformNameChanged,
        label = { Text("Nama Platform") },
        placeholder = { Text("contoh: GoFood") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = channelName,
        onValueChange = onChannelNameChanged,
        label = { Text("Nama Channel (tampil di kasir)") },
        placeholder = { Text("contoh: GoFood") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = channelCode,
        onValueChange = onChannelCodeChanged,
        label = { Text("Kode (max 5 huruf)") },
        placeholder = { Text("contoh: GF") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

// --- Step 2: Commission & pricing ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WizardStepCommission(
    commissionPercent: String,
    commissionType: CommissionType,
    hasMarkup: Boolean,
    adjustmentType: PriceAdjustmentType,
    adjustmentValue: String,
    onCommissionPercentChanged: (String) -> Unit,
    onCommissionTypeChanged: (CommissionType) -> Unit,
    onHasMarkupChanged: (Boolean) -> Unit,
    onAdjustmentTypeChanged: (PriceAdjustmentType) -> Unit,
    onAdjustmentValueChanged: (String) -> Unit
) {
    WizardSectionTitle("Komisi Platform")
    Text(
        "Berapa persen yang dipotong platform dari setiap transaksi.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = commissionPercent,
        onValueChange = onCommissionPercentChanged,
        label = { Text("Komisi (%)") },
        placeholder = { Text("contoh: 20") },
        leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(18.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    // Commission base
    Text("Dasar Perhitungan Komisi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = commissionType == CommissionType.FROM_SELLING_PRICE,
            onClick = { onCommissionTypeChanged(CommissionType.FROM_SELLING_PRICE) },
            label = { Text("Dari Harga Jual", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
        )
        FilterChip(
            selected = commissionType == CommissionType.FROM_MENU_PRICE,
            onClick = { onCommissionTypeChanged(CommissionType.FROM_MENU_PRICE) },
            label = { Text("Dari Harga Menu", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
        )
    }
    Text(
        when (commissionType) {
            CommissionType.FROM_SELLING_PRICE -> "Komisi dihitung dari harga yang customer bayar (setelah markup)."
            CommissionType.FROM_MENU_PRICE -> "Komisi dihitung dari harga menu asli (sebelum markup)."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    // Markup section
    WizardSectionTitle("Penyesuaian Harga")
    Text(
        "Naikkan harga di platform untuk menutup komisi. Best practice: markup = komisi.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Aktifkan Markup/Diskon", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = hasMarkup, onCheckedChange = onHasMarkupChanged)
    }

    AnimatedVisibility(visible = hasMarkup) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PriceAdjustmentType.entries.forEach { type ->
                    FilterChip(
                        selected = adjustmentType == type,
                        onClick = { onAdjustmentTypeChanged(type) },
                        label = { Text(adjustmentTypeLabel(type), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            OutlinedTextField(
                value = adjustmentValue,
                onValueChange = onAdjustmentValueChanged,
                label = {
                    Text(
                        if (adjustmentType == PriceAdjustmentType.MARKUP_PERCENT || adjustmentType == PriceAdjustmentType.DISCOUNT_PERCENT)
                            "Nilai (%)" else "Nilai (Rp)"
                    )
                },
                leadingIcon = {
                    Icon(
                        if (adjustmentType == PriceAdjustmentType.MARKUP_PERCENT || adjustmentType == PriceAdjustmentType.DISCOUNT_PERCENT)
                            Icons.Default.Percent else Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Profit simulation
    val commission = commissionPercent.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val markup = if (hasMarkup && (adjustmentType == PriceAdjustmentType.MARKUP_PERCENT))
        adjustmentValue.toBigDecimalOrNull() ?: BigDecimal.ZERO else BigDecimal.ZERO

    if (commission > BigDecimal.ZERO || markup > BigDecimal.ZERO) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SimulationCard(commission, commissionType, markup, hasMarkup, adjustmentType, adjustmentValue)
    }
}

// --- Step 3: Payment method & operations ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WizardStepPaymentOps(
    paymentMethod: PlatformPaymentMethod,
    requiresExternalOrderId: Boolean,
    autoConfirmOrder: Boolean,
    orderFlow: OrderFlowType,
    onPaymentMethodChanged: (PlatformPaymentMethod) -> Unit,
    onRequiresExternalOrderIdChanged: (Boolean) -> Unit,
    onAutoConfirmOrderChanged: (Boolean) -> Unit,
    onOrderFlowChanged: (OrderFlowType) -> Unit
) {
    WizardSectionTitle("Metode Pembayaran Platform")
    Text(
        "Bagaimana platform membayar ke merchant.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
        onClick = { onPaymentMethodChanged(PlatformPaymentMethod.PLATFORM_SETTLEMENT) },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (paymentMethod == PlatformPaymentMethod.PLATFORM_SETTLEMENT)
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (paymentMethod == PlatformPaymentMethod.PLATFORM_SETTLEMENT)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settlement oleh Platform", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Platform kumpulkan uang dari customer, lalu transfer ke merchant secara berkala (harian/mingguan). Komisi sudah dipotong.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Card(
        onClick = { onPaymentMethodChanged(PlatformPaymentMethod.CASH_ON_DELIVERY) },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (paymentMethod == PlatformPaymentMethod.CASH_ON_DELIVERY)
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (paymentMethod == PlatformPaymentMethod.CASH_ON_DELIVERY)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cash on Delivery (COD)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Driver bayar tunai ke merchant saat ambil pesanan. Komisi dibayar terpisah ke platform.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    WizardSectionTitle("Operasional")

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Wajib External Order ID", style = MaterialTheme.typography.bodyMedium)
            Text("Nomor pesanan dari aplikasi platform", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = requiresExternalOrderId, onCheckedChange = onRequiresExternalOrderIdChanged)
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Auto-Confirm ke Dapur", style = MaterialTheme.typography.bodyMedium)
            Text("Langsung kirim ke dapur tanpa konfirmasi kasir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = autoConfirmOrder, onCheckedChange = onAutoConfirmOrderChanged)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    WizardSectionTitle("Alur Pemesanan")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OrderFlowType.entries.forEach { flow ->
            FilterChip(
                selected = orderFlow == flow,
                onClick = { onOrderFlowChanged(flow) },
                label = { Text(orderFlowLabel(flow), style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }
    Text(orderFlowDescription(orderFlow), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// --- Step 4: Summary ---

@Composable
private fun WizardStepSummary(
    platformName: String,
    channelName: String,
    channelCode: String,
    commissionPercent: String,
    commissionType: CommissionType,
    hasMarkup: Boolean,
    adjustmentType: PriceAdjustmentType,
    adjustmentValue: String,
    paymentMethod: PlatformPaymentMethod,
    orderFlow: OrderFlowType,
    requiresExternalOrderId: Boolean,
    autoConfirmOrder: Boolean,
    sortOrder: String,
    onSortOrderChanged: (String) -> Unit
) {
    WizardSectionTitle("Ringkasan Konfigurasi")
    Text(
        "Pastikan semua pengaturan sudah benar sebelum menyimpan.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow("Platform", platformName)
            SummaryRow("Nama Channel", channelName)
            SummaryRow("Kode", channelCode)
            HorizontalDivider()
            SummaryRow("Komisi", "${commissionPercent}%")
            SummaryRow(
                "Dasar Komisi",
                if (commissionType == CommissionType.FROM_SELLING_PRICE) "Harga Jual" else "Harga Menu"
            )
            if (hasMarkup) {
                val label = priceAdjustmentLabel(adjustmentType, adjustmentValue.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                SummaryRow("Penyesuaian Harga", label)
            } else {
                SummaryRow("Penyesuaian Harga", "Tidak ada")
            }
            HorizontalDivider()
            SummaryRow("Pembayaran", platformPaymentMethodLabel(paymentMethod))
            SummaryRow("Alur", orderFlowLabel(orderFlow))
            SummaryRow("External Order ID", if (requiresExternalOrderId) "Wajib" else "Opsional")
            SummaryRow("Auto-Confirm", if (autoConfirmOrder) "Ya" else "Tidak")
        }
    }

    // Simulation
    val commission = commissionPercent.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val markup = if (hasMarkup && adjustmentType == PriceAdjustmentType.MARKUP_PERCENT)
        adjustmentValue.toBigDecimalOrNull() ?: BigDecimal.ZERO else BigDecimal.ZERO
    if (commission > BigDecimal.ZERO || markup > BigDecimal.ZERO) {
        SimulationCard(commission, commissionType, markup, hasMarkup, adjustmentType, adjustmentValue)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    OutlinedTextField(
        value = sortOrder,
        onValueChange = onSortOrderChanged,
        label = { Text("Urutan Tampil di Kasir") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

// ============================================================
// Shared Wizard Components
// ============================================================

@Composable
private fun WizardSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

/** Commission/profit simulation with example Rp 25.000 item */
@Composable
private fun SimulationCard(
    commission: BigDecimal,
    commissionType: CommissionType,
    markup: BigDecimal,
    hasMarkup: Boolean,
    adjustmentType: PriceAdjustmentType,
    adjustmentValue: String
) {
    val basePrice = BigDecimal("25000")

    // Calculate selling price
    val sellingPrice = if (hasMarkup) {
        val adjVal = adjustmentValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
        when (adjustmentType) {
            PriceAdjustmentType.MARKUP_PERCENT ->
                basePrice.multiply(BigDecimal.ONE + adjVal.divide(BigDecimal(100), 4, RoundingMode.HALF_UP))
            PriceAdjustmentType.MARKUP_FIXED -> basePrice.add(adjVal)
            PriceAdjustmentType.DISCOUNT_PERCENT ->
                basePrice.multiply(BigDecimal.ONE - adjVal.divide(BigDecimal(100), 4, RoundingMode.HALF_UP))
            PriceAdjustmentType.DISCOUNT_FIXED -> basePrice.subtract(adjVal)
        }.setScale(0, RoundingMode.HALF_UP)
    } else basePrice

    // Calculate commission
    val commissionBase = when (commissionType) {
        CommissionType.FROM_SELLING_PRICE -> sellingPrice
        CommissionType.FROM_MENU_PRICE -> basePrice
    }
    val commissionAmount = commissionBase.multiply(commission).divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
    val netReceived = sellingPrice.subtract(commissionAmount)
    val profitVsBase = netReceived.subtract(basePrice)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Simulasi (item Rp 25.000)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            SummaryRow("Harga Menu", "Rp ${formatNumber(basePrice)}")
            SummaryRow("Harga di Platform", "Rp ${formatNumber(sellingPrice)}")
            SummaryRow("Komisi ${commission.toPlainString()}%", "- Rp ${formatNumber(commissionAmount)}")
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Diterima Merchant", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    "Rp ${formatNumber(netReceived)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (netReceived >= basePrice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            if (profitVsBase.signum() != 0) {
                val sign = if (profitVsBase.signum() > 0) "+" else ""
                Text(
                    "$sign Rp ${formatNumber(profitVsBase.abs())} vs harga menu",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (profitVsBase.signum() > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ============================================================
// Utility functions
// ============================================================

private fun formatNumber(value: BigDecimal): String {
    val long = value.toLong()
    return java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("id-ID")).format(long)
}

private fun channelTypeLabel(type: ChannelType): String = when (type) {
    ChannelType.DINE_IN -> "Dine In"
    ChannelType.TAKE_AWAY -> "Take Away"
    ChannelType.DELIVERY_PLATFORM -> "Platform Delivery"
    ChannelType.OWN_DELIVERY -> "Delivery Sendiri"
}

private fun channelTypeDescription(type: ChannelType): String = when (type) {
    ChannelType.DINE_IN -> "Pelanggan makan di tempat"
    ChannelType.TAKE_AWAY -> "Pelanggan bungkus/bawa pulang"
    ChannelType.DELIVERY_PLATFORM -> "GoFood, GrabFood, ShopeeFood, dll"
    ChannelType.OWN_DELIVERY -> "Kurir milik sendiri"
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

private fun tableModeLabel(mode: TableMode): String = when (mode) {
    TableMode.REQUIRED -> "Wajib Meja"
    TableMode.OPTIONAL -> "Meja Opsional"
    TableMode.NONE -> "Tanpa Meja"
}

private fun tableModeDescription(mode: TableMode): String = when (mode) {
    TableMode.REQUIRED -> "Kasir wajib pilih meja saat membuat pesanan. Cocok untuk restoran dan food court dengan antar ke meja."
    TableMode.OPTIONAL -> "Kasir boleh pilih meja atau lewati. Cocok untuk cafe yang fleksibel."
    TableMode.NONE -> "Tidak pakai meja. Pelanggan duduk bebas atau ambil sendiri. Cocok untuk fast-food (McD, BK)."
}

private fun platformPaymentMethodLabel(method: PlatformPaymentMethod): String = when (method) {
    PlatformPaymentMethod.PLATFORM_SETTLEMENT -> "Settlement Platform"
    PlatformPaymentMethod.CASH_ON_DELIVERY -> "Cash on Delivery"
}
