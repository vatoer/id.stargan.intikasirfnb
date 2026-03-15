package id.stargan.intikasirfnb.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.transaction.ChannelType
import id.stargan.intikasirfnb.domain.transaction.PaymentMethod
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val idrFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
    maximumFractionDigits = 0
}

private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
private val timeFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID"))

private val detailDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: TransactionHistoryViewModel,
    onNavigateBack: () -> Unit,
    onResumeDraft: (saleId: String) -> Unit = {},
    onResumePayment: (saleId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Transaction detail bottom sheet
    uiState.detail?.let { detail ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissDetail() },
            sheetState = sheetState
        ) {
            TransactionDetailContent(
                detail = detail,
                onResumeDraft = { saleId ->
                    viewModel.dismissDetail()
                    onResumeDraft(saleId)
                },
                onResumePayment = { saleId ->
                    viewModel.dismissDetail()
                    onResumePayment(saleId)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Cari"
                        )
                    }
                    IconButton(onClick = { viewModel.loadData(isRefresh = true) }) {
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
            // Search bar
            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.search(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Cari no. struk, nama produk...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.search("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Summary stats bar
            if (!uiState.isLoading) {
                SummaryStatsBar(uiState.summary)
            }

            // Date range selector
            DateRangeSelector(
                selectedRange = uiState.selectedDateRange,
                onRangeSelected = viewModel::selectDateRange
            )

            // Status + Channel filter chips
            FilterChipsRow(
                uiState = uiState,
                onStatusSelected = viewModel::filterByStatus,
                onChannelSelected = viewModel::filterByChannel
            )

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

                uiState.filteredSales.isEmpty() -> {
                    EmptyState(uiState)
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.loadData(isRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SalesListContent(
                            sales = uiState.filteredSales,
                            channels = uiState.channels,
                            onSaleClick = viewModel::selectSale
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Summary Stats Bar
// ============================================================

@Composable
private fun SummaryStatsBar(summary: SalesSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Receipt,
                label = "Transaksi",
                value = "${summary.count}"
            )
            StatItem(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = "Pendapatan",
                value = idrFormat.format(summary.totalRevenue.amount)
            )
            StatItem(
                icon = Icons.Default.AttachMoney,
                label = "Rata-rata",
                value = idrFormat.format(summary.avgTicket.amount)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
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
// Date Range Selector
// ============================================================

@Composable
private fun DateRangeSelector(
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(DateRange.entries.toList()) { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(dateRangeLabel(range)) }
            )
        }
    }
}

// ============================================================
// Filter Chips (Status + Channel)
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipsRow(
    uiState: TransactionHistoryUiState,
    onStatusSelected: (SaleStatus?) -> Unit,
    onChannelSelected: (SalesChannelId?) -> Unit
) {
    val hasChannels = uiState.channels.isNotEmpty()
    val hasActiveFilter = uiState.selectedStatus != null || uiState.selectedChannelId != null

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Status filters
        items(SaleStatus.entries.toList()) { status ->
            FilterChip(
                selected = uiState.selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(statusLabel(status), style = MaterialTheme.typography.labelSmall) },
                leadingIcon = if (uiState.selectedStatus == status) {
                    {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusDotColor(status), RoundedCornerShape(50))
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = statusChipColor(status)
                )
            )
        }

        // Channel filters
        if (hasChannels) {
            items(uiState.channels.values.toList().sortedBy { it.sortOrder }) { channel ->
                FilterChip(
                    selected = uiState.selectedChannelId == channel.id,
                    onClick = { onChannelSelected(channel.id) },
                    label = { Text(channel.name, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            channelTypeIcon(channel.channelType),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

// ============================================================
// Empty State
// ============================================================

@Composable
private fun EmptyState(uiState: TransactionHistoryUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (uiState.allSales.isEmpty()) "Belum ada transaksi"
                else "Tidak ada transaksi yang cocok",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.allSales.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Coba ubah filter atau kata kunci pencarian",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ============================================================
// Sales List Content (grouped by date)
// ============================================================

@Composable
private fun SalesListContent(
    sales: List<Sale>,
    channels: Map<SalesChannelId, SalesChannel>,
    onSaleClick: (String) -> Unit
) {
    val today = dateFormat.format(Date())
    val yesterday = Calendar.getInstance().let {
        it.add(Calendar.DAY_OF_MONTH, -1)
        dateFormat.format(it.time)
    }

    val groupedSales = sales.groupBy { sale ->
        dateFormat.format(Date(sale.createdAtMillis))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        groupedSales.forEach { (date, daySales) ->
            // Date header
            item(key = "header_$date") {
                DateHeader(
                    date = date,
                    displayLabel = when (date) {
                        today -> "Hari Ini"
                        yesterday -> "Kemarin"
                        else -> date
                    },
                    count = daySales.size,
                    total = daySales
                        .filter { it.status == SaleStatus.COMPLETED || it.status == SaleStatus.PAID }
                        .fold(java.math.BigDecimal.ZERO) { acc, sale ->
                            acc.add(sale.totalAmount().amount)
                        }
                )
            }

            // Sale cards
            items(daySales, key = { it.id.value }) { sale ->
                SaleCard(
                    sale = sale,
                    channel = channels[sale.channelId],
                    onClick = { onSaleClick(sale.id.value) }
                )
            }
        }
    }
}

// ============================================================
// Date Header
// ============================================================

@Composable
private fun DateHeader(
    date: String,
    displayLabel: String,
    count: Int,
    total: java.math.BigDecimal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count transaksi",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (total > java.math.BigDecimal.ZERO) {
            Text(
                text = idrFormat.format(total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ============================================================
// Sale Card
// ============================================================

@Composable
private fun SaleCard(
    sale: Sale,
    channel: SalesChannel?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Receipt# + Channel badge + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: receipt + time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.receiptNumber
                            ?: "#${sale.id.value.takeLast(8).uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeFormat.format(Date(sale.createdAtMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Right: status
                StatusBadge(status = sale.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Channel badge + external order ID (if any)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (channel != null) {
                    ChannelBadge(channel = channel)
                }
                sale.externalOrderId?.let { extId ->
                    Text(
                        text = extId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Row 3: Item summary (compact)
            if (sale.lines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                val itemSummary = buildString {
                    val maxShow = 2
                    sale.lines.take(maxShow).forEachIndexed { i, line ->
                        if (i > 0) append(", ")
                        append("${line.quantity}x ${line.productRef.name}")
                    }
                    val remaining = sale.lines.size - maxShow
                    if (remaining > 0) append(" +$remaining lainnya")
                }
                Text(
                    text = itemSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Payment method icons + Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment methods
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val methods = sale.payments.map { it.method }.distinct()
                    methods.forEach { method ->
                        Icon(
                            paymentMethodIcon(method),
                            contentDescription = paymentMethodLabel(method),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (methods.isNotEmpty()) {
                        Text(
                            text = methods.joinToString(", ") { paymentMethodLabel(it) },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total
                Text(
                    text = idrFormat.format(sale.totalAmount().amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ============================================================
// Badges
// ============================================================

@Composable
private fun StatusBadge(status: SaleStatus) {
    Box(
        modifier = Modifier
            .background(statusChipColor(status), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = statusLabel(status),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = statusTextColor(status)
        )
    }
}

@Composable
private fun ChannelBadge(channel: SalesChannel) {
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            channelTypeIcon(channel.channelType),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ============================================================
// Transaction Detail (Bottom Sheet Content)
// ============================================================

@Composable
private fun TransactionDetailContent(
    detail: TransactionDetailState,
    onResumeDraft: (saleId: String) -> Unit = {},
    onResumePayment: (saleId: String) -> Unit = {}
) {
    val sale = detail.sale ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding()
    ) {
        // Header: outlet info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (detail.outletName.isNotBlank()) {
                Text(
                    detail.outletName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            detail.outletAddress?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            detail.outletPhone?.let {
                Text(
                    "Telp: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        DetailDivider(char = '=')

        // Transaction info
        sale.receiptNumber?.let { DetailRow("No. Struk", it) }
        DetailRow("Tanggal", detailDateFormat.format(Date(sale.createdAtMillis)))
        detail.cashierName?.let { DetailRow("Kasir", it) }
        detail.channelName?.let { DetailRow("Channel", it) }
        sale.externalOrderId?.let { DetailRow("Order Ext.", it) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Status",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatusBadge(status = sale.status)
        }

        DetailDivider()

        // Order lines
        sale.lines.forEach { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        line.productRef.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (line.selectedModifiers.isNotEmpty()) {
                        Text(
                            line.selectedModifiers.joinToString(", ") { it.optionName },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${line.quantity} x ${idrFormat.format(line.effectiveUnitPrice().amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!line.notes.isNullOrBlank()) {
                        Text(
                            "* ${line.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    idrFormat.format(line.lineTotal().amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        DetailDivider()

        // Totals
        DetailRow("Subtotal", idrFormat.format(sale.subtotal().amount))
        sale.taxLines.forEach { tax ->
            val label = if (tax.isIncludedInPrice) "${tax.taxName} (inkl.)" else tax.taxName
            DetailRow(label, idrFormat.format(tax.taxAmount.amount))
        }
        sale.serviceCharge?.let { sc ->
            val label = if (sc.isIncludedInPrice) "SC (inkl.)" else "Service Charge"
            DetailRow(label, idrFormat.format(sc.chargeAmount.amount))
        }
        sale.tip?.let { tip ->
            DetailRow("Tip", idrFormat.format(tip.amount.amount))
        }

        DetailDivider(char = '=')

        // Grand total
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "TOTAL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                idrFormat.format(sale.totalAmount().amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        DetailDivider()

        // Payments
        sale.payments.forEach { payment ->
            DetailRow(paymentMethodLabel(payment.method), idrFormat.format(payment.amount.amount))
            payment.reference?.let { ref ->
                if (ref.isNotBlank()) {
                    Text(
                        "  Ref: $ref",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (sale.changeDue().isPositive()) {
            DetailRow("Kembali", idrFormat.format(sale.changeDue().amount), bold = true)
        }

        // Notes
        if (!sale.notes.isNullOrBlank()) {
            DetailDivider()
            Text(
                "Catatan: ${sale.notes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Resume buttons for incomplete transactions
        when (sale.status) {
            SaleStatus.DRAFT, SaleStatus.OPEN -> {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onResumeDraft(sale.id.value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Lanjutkan Transaksi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            SaleStatus.CONFIRMED -> {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onResumePayment(sale.id.value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Lanjutkan Pembayaran",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            else -> {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Terima kasih atas kunjungan Anda",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DetailDivider(char: Char = '-') {
    Text(
        char.toString().repeat(40),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outlineVariant,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

// ============================================================
// Helper functions
// ============================================================

@Composable
private fun statusChipColor(status: SaleStatus) = when (status) {
    SaleStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
    SaleStatus.OPEN -> MaterialTheme.colorScheme.tertiaryContainer
    SaleStatus.CONFIRMED -> MaterialTheme.colorScheme.tertiaryContainer
    SaleStatus.PAID -> MaterialTheme.colorScheme.secondaryContainer
    SaleStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
    SaleStatus.VOIDED -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun statusTextColor(status: SaleStatus) = when (status) {
    SaleStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
    SaleStatus.OPEN -> MaterialTheme.colorScheme.onTertiaryContainer
    SaleStatus.CONFIRMED -> MaterialTheme.colorScheme.onTertiaryContainer
    SaleStatus.PAID -> MaterialTheme.colorScheme.onSecondaryContainer
    SaleStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
    SaleStatus.VOIDED -> MaterialTheme.colorScheme.onErrorContainer
}

@Composable
private fun statusDotColor(status: SaleStatus) = when (status) {
    SaleStatus.DRAFT -> MaterialTheme.colorScheme.outline
    SaleStatus.OPEN -> MaterialTheme.colorScheme.tertiary
    SaleStatus.CONFIRMED -> MaterialTheme.colorScheme.tertiary
    SaleStatus.PAID -> MaterialTheme.colorScheme.secondary
    SaleStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    SaleStatus.VOIDED -> MaterialTheme.colorScheme.error
}

private fun statusLabel(status: SaleStatus) = when (status) {
    SaleStatus.DRAFT -> "Draft"
    SaleStatus.OPEN -> "Di Dapur"
    SaleStatus.CONFIRMED -> "Dikonfirmasi"
    SaleStatus.PAID -> "Dibayar"
    SaleStatus.COMPLETED -> "Selesai"
    SaleStatus.VOIDED -> "Batal"
}

private fun dateRangeLabel(range: DateRange) = when (range) {
    DateRange.TODAY -> "Hari Ini"
    DateRange.YESTERDAY -> "Kemarin"
    DateRange.THIS_WEEK -> "Minggu Ini"
    DateRange.THIS_MONTH -> "Bulan Ini"
    DateRange.ALL -> "Semua"
}

private fun channelTypeIcon(type: ChannelType): ImageVector = when (type) {
    ChannelType.DINE_IN -> Icons.Default.Restaurant
    ChannelType.TAKE_AWAY -> Icons.Default.ShoppingBag
    ChannelType.DELIVERY_PLATFORM -> Icons.Default.DeliveryDining
    ChannelType.OWN_DELIVERY -> Icons.Default.LocalShipping
}

private fun paymentMethodIcon(method: PaymentMethod): ImageVector = when (method) {
    PaymentMethod.CASH -> Icons.Default.Payments
    PaymentMethod.CARD -> Icons.Default.CreditCard
    PaymentMethod.E_WALLET -> Icons.Default.AccountBalanceWallet
    PaymentMethod.TRANSFER -> Icons.Default.SwapHoriz
    PaymentMethod.PLATFORM_GOFOOD,
    PaymentMethod.PLATFORM_GRABFOOD,
    PaymentMethod.PLATFORM_SHOPEEFOOD,
    PaymentMethod.PLATFORM_OTHER -> Icons.Default.DeliveryDining
    PaymentMethod.OTHER -> Icons.Default.MonetizationOn
}

private fun paymentMethodLabel(method: PaymentMethod) = when (method) {
    PaymentMethod.CASH -> "Tunai"
    PaymentMethod.CARD -> "Kartu"
    PaymentMethod.E_WALLET -> "E-Wallet"
    PaymentMethod.TRANSFER -> "Transfer"
    PaymentMethod.PLATFORM_GOFOOD -> "GoFood"
    PaymentMethod.PLATFORM_GRABFOOD -> "GrabFood"
    PaymentMethod.PLATFORM_SHOPEEFOOD -> "ShopeeFood"
    PaymentMethod.PLATFORM_OTHER -> "Platform Lain"
    PaymentMethod.OTHER -> "Lainnya"
}
