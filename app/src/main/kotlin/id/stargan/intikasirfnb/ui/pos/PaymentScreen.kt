package id.stargan.intikasirfnb.ui.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PrintDisabled
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.transaction.PaymentMethod
import id.stargan.intikasirfnb.domain.transaction.Sale
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idrFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
}

private val WIDE_BREAKPOINT = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    onNavigateBack: () -> Unit,
    onNewTransaction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.printStatus) {
        if (uiState.printStatus == PrintStatus.SUCCESS) {
            snackbarHostState.showSnackbar("Struk berhasil dicetak")
            viewModel.resetPrintStatus()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isCompleted) "Pembayaran Berhasil" else "Pembayaran")
                },
                navigationIcon = {
                    if (!uiState.isCompleted) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.isCompleted -> {
                PaymentCompleteContent(
                    uiState = uiState,
                    onPrint = viewModel::printReceipt,
                    onNewTransaction = onNewTransaction,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            uiState.sale != null -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (maxWidth >= WIDE_BREAKPOINT) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            OrderSummaryPanel(
                                uiState = uiState,
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                            )
                            VerticalDivider()
                            PaymentInputPanel(
                                uiState = uiState,
                                onMethodSelected = viewModel::selectPaymentMethod,
                                onAmountInputChanged = viewModel::updateAmountInput,
                                onReferenceChanged = viewModel::updatePaymentReference,
                                onQuickCash = viewModel::selectQuickCash,
                                onAddStaged = viewModel::addStagedPayment,
                                onRemoveStaged = viewModel::removeStagedPayment,
                                onProcessPayment = viewModel::processPayment,
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        PaymentPhoneLayout(
                            uiState = uiState,
                            onMethodSelected = viewModel::selectPaymentMethod,
                            onAmountInputChanged = viewModel::updateAmountInput,
                            onReferenceChanged = viewModel::updatePaymentReference,
                            onQuickCash = viewModel::selectQuickCash,
                            onAddStaged = viewModel::addStagedPayment,
                            onRemoveStaged = viewModel::removeStagedPayment,
                            onProcessPayment = viewModel::processPayment,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Phone: single-column payment layout
// ============================================================

@Composable
private fun PaymentPhoneLayout(
    uiState: PaymentUiState,
    onMethodSelected: (PaymentMethod) -> Unit,
    onAmountInputChanged: (String) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onQuickCash: (Long) -> Unit,
    onAddStaged: () -> Unit,
    onRemoveStaged: (Int) -> Unit,
    onProcessPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Total tagihan card
            TotalBillCard(uiState)

            Spacer(modifier = Modifier.height(16.dp))

            // Payment method selection
            Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            PaymentMethodRow(uiState.selectedMethod, onMethodSelected)

            Spacer(modifier = Modifier.height(16.dp))

            // Amount input
            AmountInputSection(uiState, onAmountInputChanged, onQuickCash)
            Spacer(modifier = Modifier.height(12.dp))

            // Non-cash reference
            AnimatedVisibility(visible = uiState.selectedMethod != PaymentMethod.CASH) {
                Column {
                    OutlinedTextField(
                        value = uiState.paymentReference,
                        onValueChange = onReferenceChanged,
                        label = { Text(referenceLabel(uiState.selectedMethod)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // "Tambah Pembayaran" button
            OutlinedButton(
                onClick = onAddStaged,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                enabled = uiState.canAddStaged && !uiState.isProcessing
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Pembayaran", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Staged payments list
            if (uiState.stagedPayments.isNotEmpty()) {
                StagedPaymentsList(uiState, onRemoveStaged)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Bottom action bar
        PaymentActionBar(
            uiState = uiState,
            onProcessPayment = onProcessPayment
        )
    }
}

// ============================================================
// Payment Complete: Receipt preview + action bar
// ============================================================

@Composable
private fun PaymentCompleteContent(
    uiState: PaymentUiState,
    onPrint: () -> Unit,
    onNewTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sale = uiState.sale ?: return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 420.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Pembayaran Berhasil!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (sale.changeDue().amount > BigDecimal.ZERO) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Kembalian",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    idrFormat.format(sale.changeDue().amount),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                ReceiptPreviewCard(
                    sale = sale,
                    outletName = uiState.outletSettings?.outletProfile?.name ?: "",
                    outletAddress = uiState.outletSettings?.outletProfile?.address,
                    outletPhone = uiState.outletSettings?.outletProfile?.phone,
                    cashierName = uiState.cashierName,
                    channelName = uiState.channelName
                )
            }
        }

        PaymentCompleteActionBar(
            uiState = uiState,
            onPrint = onPrint,
            onNewTransaction = onNewTransaction
        )
    }
}

@Composable
private fun PaymentCompleteActionBar(
    uiState: PaymentUiState,
    onPrint: () -> Unit,
    onNewTransaction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.printCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dicetak ${uiState.printCount}x", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPrint,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = uiState.hasPrinter && uiState.printStatus != PrintStatus.PRINTING
            ) {
                when (uiState.printStatus) {
                    PrintStatus.PRINTING -> {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mencetak...")
                    }
                    else -> {
                        Icon(
                            if (uiState.hasPrinter) Icons.Default.Print else Icons.Default.PrintDisabled,
                            contentDescription = null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.hasPrinter) "Cetak Struk" else "Printer Belum Diatur")
                    }
                }
            }

            Button(
                onClick = onNewTransaction,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transaksi Baru", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================
// Receipt Preview Card
// ============================================================

@Composable
private fun ReceiptPreviewCard(
    sale: Sale,
    outletName: String,
    outletAddress: String?,
    outletPhone: String?,
    cashierName: String?,
    channelName: String?
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (outletName.isNotBlank()) {
                    Text(outletName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                outletAddress?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                outletPhone?.let {
                    Text("Telp: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }

            ReceiptDivider(char = '=')

            sale.receiptNumber?.let { ReceiptRow("No.", it) }
            ReceiptRow("Tanggal", dateFormat.format(Date(sale.createdAtMillis)))
            cashierName?.let { ReceiptRow("Kasir", it) }
            channelName?.let { ReceiptRow("Channel", it) }

            ReceiptDivider()

            sale.lines.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.productRef.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (line.selectedModifiers.isNotEmpty()) {
                            Text(line.selectedModifiers.joinToString(", ") { it.optionName }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${line.quantity} x ${idrFormat.format(line.effectiveUnitPrice().amount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!line.notes.isNullOrBlank()) {
                            Text("* ${line.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Text(idrFormat.format(line.lineTotal().amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }

            ReceiptDivider()

            ReceiptRow("Subtotal", idrFormat.format(sale.subtotal().amount))
            sale.taxLines.forEach { tax ->
                val label = if (tax.isIncludedInPrice) "${tax.taxName} (inkl.)" else tax.taxName
                ReceiptRow(label, idrFormat.format(tax.taxAmount.amount))
            }
            sale.serviceCharge?.let { sc ->
                val label = if (sc.isIncludedInPrice) "SC (inkl.)" else "Service Charge"
                ReceiptRow(label, idrFormat.format(sc.chargeAmount.amount))
            }
            sale.tip?.let { tip -> ReceiptRow("Tip", idrFormat.format(tip.amount.amount)) }

            ReceiptDivider(char = '=')

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(idrFormat.format(sale.totalAmount().amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            ReceiptDivider()

            sale.payments.forEach { payment ->
                ReceiptRow(paymentMethodLabel(payment.method), idrFormat.format(payment.amount.amount))
                payment.reference?.let { ref ->
                    if (ref.isNotBlank()) {
                        Text("  Ref: $ref", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (sale.changeDue().isPositive()) {
                ReceiptRow("Kembali", idrFormat.format(sale.changeDue().amount), bold = true)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Terima kasih atas kunjungan Anda", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ReceiptDivider(char: Char = '-') {
    Text(char.toString().repeat(40), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outlineVariant, maxLines = 1, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}

// ============================================================
// Order Summary Panel (tablet only — left side)
// ============================================================

@Composable
private fun OrderSummaryPanel(
    uiState: PaymentUiState,
    modifier: Modifier = Modifier
) {
    val sale = uiState.sale ?: return

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Text("Ringkasan Pesanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(sale.lines, key = { it.id.value }) { line ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.productRef.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (line.selectedModifiers.isNotEmpty()) {
                            Text(line.selectedModifiers.joinToString(", ") { it.optionName }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${idrFormat.format(line.effectiveUnitPrice().amount)} x ${line.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(idrFormat.format(line.lineTotal().amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }

        HorizontalDivider()
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SummaryRow("Subtotal", sale.subtotal())
            sale.taxLines.forEach { tax ->
                val label = if (tax.isIncludedInPrice) "${tax.taxName} (inkl.)" else tax.taxName
                SummaryRow(label, tax.taxAmount)
            }
            sale.serviceCharge?.let { sc ->
                val label = if (sc.isIncludedInPrice) "Service Charge (inkl.)" else "Service Charge"
                SummaryRow(label, sc.chargeAmount)
            }
            sale.tip?.let { tip -> SummaryRow("Tip", tip.amount) }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(idrFormat.format(sale.totalAmount().amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // Staged payments summary (tablet)
            if (uiState.stagedPayments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                StagedPaymentsList(uiState, onRemove = {})
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: id.stargan.intikasirfnb.domain.shared.Money) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(idrFormat.format(amount.amount), style = MaterialTheme.typography.bodyMedium)
    }
}

// ============================================================
// Payment Input Panel (tablet: right side)
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentInputPanel(
    uiState: PaymentUiState,
    onMethodSelected: (PaymentMethod) -> Unit,
    onAmountInputChanged: (String) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onQuickCash: (Long) -> Unit,
    onAddStaged: () -> Unit,
    onRemoveStaged: (Int) -> Unit,
    onProcessPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Metode Pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            PaymentMethodRow(uiState.selectedMethod, onMethodSelected)

            Spacer(modifier = Modifier.height(20.dp))

            // Amount input
            AmountInputSection(uiState, onAmountInputChanged, onQuickCash)
            Spacer(modifier = Modifier.height(16.dp))

            // Non-cash reference
            AnimatedVisibility(visible = uiState.selectedMethod != PaymentMethod.CASH) {
                Column {
                    OutlinedTextField(
                        value = uiState.paymentReference,
                        onValueChange = onReferenceChanged,
                        label = { Text(referenceLabel(uiState.selectedMethod)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // "Tambah Pembayaran" button
            OutlinedButton(
                onClick = onAddStaged,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = uiState.canAddStaged && !uiState.isProcessing
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Pembayaran", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Staged payments list
            if (uiState.stagedPayments.isNotEmpty()) {
                StagedPaymentsList(uiState, onRemoveStaged)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        PaymentActionBar(
            uiState = uiState,
            onProcessPayment = onProcessPayment
        )
    }
}

// ============================================================
// Shared components
// ============================================================

@Composable
private fun TotalBillCard(uiState: PaymentUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Tagihan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                idrFormat.format(uiState.totalAmount.amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentMethodRow(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PaymentMethodChip("Tunai", Icons.Default.Money, selectedMethod == PaymentMethod.CASH) { onMethodSelected(PaymentMethod.CASH) }
        PaymentMethodChip("Kartu", Icons.Default.CreditCard, selectedMethod == PaymentMethod.CARD) { onMethodSelected(PaymentMethod.CARD) }
        PaymentMethodChip("E-Wallet", Icons.Default.PhoneAndroid, selectedMethod == PaymentMethod.E_WALLET) { onMethodSelected(PaymentMethod.E_WALLET) }
        PaymentMethodChip("Transfer", Icons.Default.AccountBalance, selectedMethod == PaymentMethod.TRANSFER) { onMethodSelected(PaymentMethod.TRANSFER) }
        PaymentMethodChip("Lainnya", Icons.Default.Payments, selectedMethod == PaymentMethod.OTHER) { onMethodSelected(PaymentMethod.OTHER) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountInputSection(
    uiState: PaymentUiState,
    onAmountInputChanged: (String) -> Unit,
    onQuickCash: (Long) -> Unit
) {
    val remainingLong = uiState.remainingAmount.amount.toLong()

    Text(
        if (uiState.selectedMethod == PaymentMethod.CASH) "Jumlah Uang Diterima" else "Jumlah Pembayaran",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = formatCashDisplay(uiState.amountInput),
        onValueChange = { onAmountInputChanged(it) },
        label = { Text("Jumlah (Rp)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        prefix = { Text("Rp ") },
        supportingText = {
            Text("Sisa: ${idrFormat.format(uiState.remainingAmount.amount)}")
        },
        modifier = Modifier.fillMaxWidth()
    )

    AnimatedVisibility(visible = uiState.selectedMethod == PaymentMethod.CASH) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onQuickCash(remainingLong) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("Uang Pas", style = MaterialTheme.typography.labelMedium) }

                buildQuickCashAmounts(remainingLong).forEach { amount ->
                    OutlinedButton(
                        onClick = { onQuickCash(amount) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) { Text(formatQuickCash(amount), style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

@Composable
private fun ChangeDueCard(changeDue: id.stargan.intikasirfnb.domain.shared.Money) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kembalian", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(idrFormat.format(changeDue.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun StagedPaymentsList(
    uiState: PaymentUiState,
    onRemove: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                "Daftar Pembayaran",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            uiState.stagedPayments.forEachIndexed { index, staged ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(paymentMethodLabel(staged.method), style = MaterialTheme.typography.bodySmall)
                        staged.reference?.let { ref ->
                            Text("Ref: $ref", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        idrFormat.format(staged.amount.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (!uiState.isProcessing) {
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (index < uiState.stagedPayments.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Staged total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Dibayar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    idrFormat.format(uiState.stagedTotal.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Remaining
            if (uiState.remainingAmount.amount > BigDecimal.ZERO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sisa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Text(idrFormat.format(uiState.remainingAmount.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }

            // Change due (split mode)
            if (uiState.changeDue.amount > BigDecimal.ZERO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kembalian", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    Text(idrFormat.format(uiState.changeDue.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

/** Bottom action bar: BAYAR button */
@Composable
private fun PaymentActionBar(
    uiState: PaymentUiState,
    onProcessPayment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = onProcessPayment,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = uiState.canPay && !uiState.isProcessing,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Memproses...", style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                if (uiState.isFullyStaged) {
                    Text("BAYAR ${idrFormat.format(uiState.totalAmount.amount)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                } else {
                    Text("BAYAR (belum lengkap)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

// ============================================================
// Utility functions
// ============================================================

private fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Tunai"
    PaymentMethod.CARD -> "Kartu"
    PaymentMethod.E_WALLET -> "E-Wallet"
    PaymentMethod.TRANSFER -> "Transfer"
    PaymentMethod.OTHER -> "Lainnya"
}

private fun referenceLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CARD -> "No. Approval / Last 4 digit"
    PaymentMethod.E_WALLET -> "No. Referensi"
    PaymentMethod.TRANSFER -> "No. Referensi Transfer"
    else -> "Keterangan"
}

private fun formatCashDisplay(input: String): String {
    if (input.isBlank()) return ""
    val number = input.toLongOrNull() ?: return input
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(number)
}

private fun formatQuickCash(amount: Long): String {
    return when {
        amount >= 1_000_000 -> "${amount / 1_000_000}jt"
        amount >= 1_000 -> "${amount / 1_000}rb"
        else -> amount.toString()
    }
}

private fun buildQuickCashAmounts(total: Long): List<Long> {
    if (total <= 0) return emptyList()
    val amounts = mutableListOf<Long>()
    val roundups = listOf(1_000L, 5_000L, 10_000L, 20_000L, 50_000L, 100_000L)
    for (r in roundups) {
        val rounded = ((total + r - 1) / r) * r
        if (rounded > total && rounded !in amounts) amounts.add(rounded)
        if (amounts.size >= 4) break
    }
    val commonBills = listOf(50_000L, 100_000L, 200_000L, 500_000L)
    for (bill in commonBills) {
        if (bill > total && bill !in amounts) amounts.add(bill)
        if (amounts.size >= 6) break
    }
    return amounts.sorted().take(6)
}
