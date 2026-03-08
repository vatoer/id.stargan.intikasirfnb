package id.stargan.intikasirfnb.ui.settlement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SettlementStatus
import id.stargan.intikasirfnb.domain.usecase.transaction.SettlementSummary
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idrFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
}
private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

// ============================================================
// Main Screen
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    viewModel: SettlementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    // Settlement detail bottom sheet
    uiState.selectedSettlement?.let { settlement ->
        if (!uiState.showSettleDialog) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissDetail() },
                sheetState = sheetState
            ) {
                SettlementDetailContent(
                    settlement = settlement,
                    channelName = uiState.channels[settlement.channelId]?.name,
                    onSettle = { viewModel.showSettleDialog(settlement) },
                    onDispute = { notes -> viewModel.markDisputed(settlement.id, notes) },
                    onCancel = { viewModel.cancelSettlement(settlement.id) }
                )
            }
        }
    }

    // Settle dialog
    if (uiState.showSettleDialog && uiState.selectedSettlement != null) {
        SettleDialog(
            settlement = uiState.selectedSettlement!!,
            onConfirm = { amount, ref -> viewModel.markSettled(uiState.selectedSettlement!!.id, amount, ref) },
            onDismiss = { viewModel.dismissSettleDialog() }
        )
    }

    // Batch settle dialog
    if (uiState.showBatchSettleDialog) {
        val selectedIds = uiState.selectedForBatch
        val selectedSettlements = uiState.settlements.filter { it.id in selectedIds }
        val totalExpected = selectedSettlements.fold(Money.zero()) { acc, s -> acc + s.expectedAmount }

        BatchSettleDialog(
            count = selectedIds.size,
            totalExpected = totalExpected,
            onConfirm = { amount, ref -> viewModel.batchSettle(amount, ref) },
            onDismiss = { viewModel.dismissBatchSettleDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rekonsiliasi Platform") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary card
            uiState.summary?.let { summary ->
                SummaryCard(summary = summary)
            }

            // Tabs
            SettlementTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::selectTab
            )

            // Channel filter chips
            if (uiState.channels.isNotEmpty()) {
                ChannelFilterRow(
                    channels = uiState.channels,
                    selectedChannelId = uiState.selectedChannelId,
                    onChannelSelected = viewModel::filterByChannel
                )
            }

            // Batch action bar
            AnimatedVisibility(
                visible = uiState.selectedTab == SettlementTab.PENDING &&
                    uiState.settlements.any { it.status == SettlementStatus.PENDING }
            ) {
                BatchActionBar(
                    selectedCount = uiState.selectedForBatch.size,
                    totalCount = uiState.settlements.count { it.status == SettlementStatus.PENDING },
                    onSelectAll = { viewModel.selectAllPending() },
                    onClear = { viewModel.clearBatchSelection() },
                    onBatchSettle = { viewModel.showBatchSettleDialog() }
                )
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.settlements.isEmpty() -> {
                    EmptyState(tab = uiState.selectedTab)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.settlements, key = { it.id.value }) { settlement ->
                            SettlementCard(
                                settlement = settlement,
                                channelName = uiState.channels[settlement.channelId]?.name,
                                isBatchMode = uiState.selectedTab == SettlementTab.PENDING,
                                isSelected = settlement.id in uiState.selectedForBatch,
                                onToggleSelect = { viewModel.toggleBatchSelection(settlement.id) },
                                onClick = { viewModel.selectSettlement(settlement) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Summary Card
// ============================================================

@Composable
private fun SummaryCard(summary: SettlementSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Ringkasan Bulan Ini",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    icon = Icons.Default.Pending,
                    label = "Belum Cair",
                    value = idrFormat.format(summary.totalPending.amount),
                    color = MaterialTheme.colorScheme.error
                )
                SummaryItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Sudah Cair",
                    value = idrFormat.format(summary.totalSettled.amount),
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryItem(
                    icon = Icons.Default.AccountBalance,
                    label = "Komisi",
                    value = idrFormat.format(summary.totalCommission.amount),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (summary.pendingCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${summary.pendingCount} transaksi menunggu pencairan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// Tabs
// ============================================================

@Composable
private fun SettlementTabs(
    selectedTab: SettlementTab,
    onTabSelected: (SettlementTab) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        SettlementTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        when (tab) {
                            SettlementTab.PENDING -> "Belum Cair"
                            SettlementTab.SETTLED -> "Sudah Cair"
                            SettlementTab.ALL -> "Semua"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

// ============================================================
// Channel Filter Row
// ============================================================

@Composable
private fun ChannelFilterRow(
    channels: Map<SalesChannelId, SalesChannel>,
    selectedChannelId: SalesChannelId?,
    onChannelSelected: (SalesChannelId?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        items(channels.values.toList().sortedBy { it.sortOrder }) { channel ->
            FilterChip(
                selected = selectedChannelId == channel.id,
                onClick = { onChannelSelected(channel.id) },
                label = { Text(channel.name, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        Icons.Default.DeliveryDining,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    }
}

// ============================================================
// Batch Action Bar
// ============================================================

@Composable
private fun BatchActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onBatchSettle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (selectedCount == totalCount) onClear() else onSelectAll() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.SelectAll,
                    contentDescription = "Pilih semua",
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                "$selectedCount/$totalCount dipilih",
                style = MaterialTheme.typography.labelMedium
            )
        }

        Button(
            onClick = onBatchSettle,
            enabled = selectedCount > 0,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text("Cairkan ${selectedCount} item", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ============================================================
// Settlement Card
// ============================================================

@Composable
private fun SettlementCard(
    settlement: PlatformSettlement,
    channelName: String?,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for batch mode
            if (isBatchMode && settlement.status == SettlementStatus.PENDING) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                // Row 1: Platform name + status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            settlement.platformName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        channelName?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    SettlementStatusBadge(status = settlement.status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Sale count + date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${settlement.saleIds.size} transaksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dateTimeFormat.format(Date(settlement.createdAtMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 3: Expected + commission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "Piutang",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            idrFormat.format(settlement.expectedAmount.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (settlement.status == SettlementStatus.PENDING)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Komisi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            idrFormat.format(settlement.commissionTotal.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Show settled amount if settled/partial
                settlement.settledAmount?.let { settled ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Dicairkan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            idrFormat.format(settled.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Show difference if any
                    settlement.difference()?.let { diff ->
                        if (diff.amount.compareTo(BigDecimal.ZERO) != 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Selisih",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    idrFormat.format(diff.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Platform reference
                settlement.platformReference?.let { ref ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Ref: $ref",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================
// Status Badge
// ============================================================

@Composable
private fun SettlementStatusBadge(status: SettlementStatus) {
    val (label, color, textColor, icon) = when (status) {
        SettlementStatus.PENDING -> StatusConfig(
            "Belum Cair",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Pending
        )
        SettlementStatus.SETTLED -> StatusConfig(
            "Cair",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle
        )
        SettlementStatus.PARTIAL -> StatusConfig(
            "Sebagian",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.Warning
        )
        SettlementStatus.DISPUTED -> StatusConfig(
            "Dispute",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )
        SettlementStatus.CANCELLED -> StatusConfig(
            "Batal",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Close
        )
    }

    Row(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = textColor)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

private data class StatusConfig(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val icon: ImageVector
)

// ============================================================
// Settlement Detail (Bottom Sheet)
// ============================================================

@Composable
private fun SettlementDetailContent(
    settlement: PlatformSettlement,
    channelName: String?,
    onSettle: () -> Unit,
    onDispute: (String?) -> Unit,
    onCancel: () -> Unit
) {
    var showDisputeInput by remember { mutableStateOf(false) }
    var disputeNotes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    settlement.platformName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                channelName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SettlementStatusBadge(status = settlement.status)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Info rows
        DetailRow("ID Settlement", settlement.id.value.takeLast(12).uppercase())
        DetailRow("Jumlah Transaksi", "${settlement.saleIds.size} order")
        DetailRow("Dibuat", dateTimeFormat.format(Date(settlement.createdAtMillis)))
        settlement.settlementDate?.let {
            DetailRow("Tanggal Cair", dateTimeFormat.format(Date(it)))
        }
        settlement.platformReference?.let {
            DetailRow("Referensi Platform", it)
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Financial breakdown
        Text(
            "Rincian Keuangan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        DetailRow("Piutang (Net)", idrFormat.format(settlement.expectedAmount.amount))
        DetailRow("Komisi Platform", idrFormat.format(settlement.commissionTotal.amount))

        settlement.settledAmount?.let { settled ->
            DetailRow("Jumlah Cair", idrFormat.format(settled.amount), bold = true)
            settlement.difference()?.let { diff ->
                if (diff.amount.compareTo(BigDecimal.ZERO) != 0) {
                    val isUnderpaid = diff.amount > BigDecimal.ZERO
                    DetailRow(
                        if (isUnderpaid) "Kurang Bayar" else "Lebih Bayar",
                        idrFormat.format(diff.amount.abs()),
                        bold = true
                    )
                }
            }
        }

        // Notes
        settlement.notes?.let { notes ->
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Catatan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action buttons (only for PENDING)
        if (settlement.status == SettlementStatus.PENDING) {
            Button(
                onClick = onSettle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tandai Sudah Cair", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dispute section
            AnimatedVisibility(visible = showDisputeInput) {
                Column {
                    OutlinedTextField(
                        value = disputeNotes,
                        onValueChange = { disputeNotes = it },
                        label = { Text("Alasan dispute") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onDispute(disputeNotes.takeIf { it.isNotBlank() }) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kirim Dispute")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDisputeInput = !showDisputeInput },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (showDisputeInput) "Batal" else "Dispute")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Batalkan")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ============================================================
// Settle Dialog
// ============================================================

@Composable
private fun SettleDialog(
    settlement: PlatformSettlement,
    onConfirm: (Money, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(settlement.expectedAmount.amount.toPlainString()) }
    var reference by remember { mutableStateOf("") }
    val parsedAmount = amountText.toBigDecimalOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Pencairan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Platform: ${settlement.platformName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Piutang: ${idrFormat.format(settlement.expectedAmount.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Jumlah cair") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Rp ") },
                    shape = RoundedCornerShape(12.dp)
                )

                // Show discrepancy warning
                if (parsedAmount != null && parsedAmount.compareTo(settlement.expectedAmount.amount) != 0) {
                    val diff = settlement.expectedAmount.amount.subtract(parsedAmount)
                    val isUnder = diff > BigDecimal.ZERO
                    Text(
                        if (isUnder) "Kurang Rp ${idrFormat.format(diff).removePrefix("Rp")}"
                        else "Lebih Rp ${idrFormat.format(diff.abs()).removePrefix("Rp")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("No. referensi (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedAmount?.let { amt ->
                        onConfirm(Money(amt), reference.takeIf { it.isNotBlank() })
                    }
                },
                enabled = parsedAmount != null && parsedAmount > BigDecimal.ZERO
            ) {
                Text("Konfirmasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// ============================================================
// Batch Settle Dialog
// ============================================================

@Composable
private fun BatchSettleDialog(
    count: Int,
    totalExpected: Money,
    onConfirm: (Money, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(totalExpected.amount.toPlainString()) }
    var reference by remember { mutableStateOf("") }
    val parsedAmount = amountText.toBigDecimalOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Pencairan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "$count settlement dipilih",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Total piutang: ${idrFormat.format(totalExpected.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Total jumlah cair") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Rp ") },
                    shape = RoundedCornerShape(12.dp)
                )

                if (parsedAmount != null && parsedAmount.compareTo(totalExpected.amount) != 0) {
                    val diff = totalExpected.amount.subtract(parsedAmount)
                    Text(
                        "Selisih: ${idrFormat.format(diff.abs())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("No. referensi (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedAmount?.let { amt ->
                        onConfirm(Money(amt), reference.takeIf { it.isNotBlank() })
                    }
                },
                enabled = parsedAmount != null && parsedAmount > BigDecimal.ZERO
            ) {
                Text("Cairkan Semua")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// ============================================================
// Empty State
// ============================================================

@Composable
private fun EmptyState(tab: SettlementTab) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                when (tab) {
                    SettlementTab.PENDING -> Icons.Default.Pending
                    SettlementTab.SETTLED -> Icons.Default.CheckCircle
                    SettlementTab.ALL -> Icons.Default.Receipt
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                when (tab) {
                    SettlementTab.PENDING -> "Tidak ada piutang platform"
                    SettlementTab.SETTLED -> "Belum ada pencairan"
                    SettlementTab.ALL -> "Belum ada settlement"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tab == SettlementTab.PENDING) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Settlement otomatis dibuat saat transaksi platform selesai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
