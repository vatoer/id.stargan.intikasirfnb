package id.stargan.intikasirfnb.ui.pos

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PrintDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    OrderSummaryPanel(
                        sale = uiState.sale!!,
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    )

                    VerticalDivider()

                    PaymentInputPanel(
                        uiState = uiState,
                        onMethodSelected = viewModel::selectPaymentMethod,
                        onAmountInputChanged = viewModel::updateAmountInput,
                        onReferenceChanged = viewModel::updatePaymentReference,
                        onQuickCash = viewModel::selectQuickCash,
                        onAddPayment = viewModel::addPayment,
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    )
                }
            }
        }
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
        // Receipt preview (scrollable)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 420.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success header
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

                    // Show change due prominently
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

            // Receipt preview card
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

        // Action bar: Cetak Struk + Transaksi Baru
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Print status indicator
        if (uiState.printCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Dicetak ${uiState.printCount}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cetak Struk button
            OutlinedButton(
                onClick = onPrint,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = uiState.hasPrinter && uiState.printStatus != PrintStatus.PRINTING
            ) {
                when (uiState.printStatus) {
                    PrintStatus.PRINTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mencetak...")
                    }
                    else -> {
                        Icon(
                            if (uiState.hasPrinter) Icons.Default.Print else Icons.Default.PrintDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.hasPrinter) "Cetak Struk" else "Printer Belum Diatur")
                    }
                }
            }

            // Transaksi Baru button
            Button(
                onClick = onNewTransaction,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.PointOfSale,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Transaksi Baru",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================
// Receipt Preview Card (reused from ReceiptScreen)
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
    val dateFormat = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // --- Header ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (outletName.isNotBlank()) {
                    Text(
                        text = outletName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                outletAddress?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                outletPhone?.let {
                    Text(
                        text = "Telp: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            ReceiptDivider(char = '=')

            // --- Order info ---
            sale.receiptNumber?.let {
                ReceiptRow("No.", it)
            }
            ReceiptRow("Tanggal", dateFormat.format(Date(sale.createdAtMillis)))
            cashierName?.let { ReceiptRow("Kasir", it) }
            channelName?.let { ReceiptRow("Channel", it) }

            ReceiptDivider()

            // --- Line items ---
            sale.lines.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = line.productRef.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (line.selectedModifiers.isNotEmpty()) {
                            Text(
                                text = line.selectedModifiers.joinToString(", ") { it.optionName },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${line.quantity} x ${idrFormat.format(line.effectiveUnitPrice().amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!line.notes.isNullOrBlank()) {
                            Text(
                                text = "* ${line.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Text(
                        text = idrFormat.format(line.lineTotal().amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ReceiptDivider()

            // --- Totals ---
            ReceiptRow("Subtotal", idrFormat.format(sale.subtotal().amount))

            sale.taxLines.forEach { tax ->
                val label = if (tax.isIncludedInPrice) "${tax.taxName} (inkl.)" else tax.taxName
                ReceiptRow(label, idrFormat.format(tax.taxAmount.amount))
            }

            sale.serviceCharge?.let { sc ->
                val label = if (sc.isIncludedInPrice) "SC (inkl.)" else "Service Charge"
                ReceiptRow(label, idrFormat.format(sc.chargeAmount.amount))
            }

            sale.tip?.let { tip ->
                ReceiptRow("Tip", idrFormat.format(tip.amount.amount))
            }

            ReceiptDivider(char = '=')

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

            ReceiptDivider()

            // --- Payment ---
            sale.payments.forEach { payment ->
                val methodLabel = paymentMethodLabel(payment.method)
                ReceiptRow(methodLabel, idrFormat.format(payment.amount.amount))
                payment.reference?.let { ref ->
                    if (ref.isNotBlank()) {
                        Text(
                            text = "  Ref: $ref",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (sale.changeDue().isPositive()) {
                ReceiptRow("Kembali", idrFormat.format(sale.changeDue().amount), bold = true)
            }

            // --- Footer ---
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Terima kasih atas kunjungan Anda",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ReceiptDivider(char: Char = '-') {
    Text(
        text = char.toString().repeat(40),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outlineVariant,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

// ============================================================
// Order Summary Panel (payment input phase)
// ============================================================

@Composable
private fun OrderSummaryPanel(
    sale: Sale,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Text(
            "Ringkasan Pesanan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(sale.lines, key = { it.id.value }) { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = line.productRef.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (line.selectedModifiers.isNotEmpty()) {
                            Text(
                                text = line.selectedModifiers.joinToString(", ") { it.optionName },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${idrFormat.format(line.effectiveUnitPrice().amount)} x ${line.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = idrFormat.format(line.lineTotal().amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SummaryRow("Subtotal", sale.subtotal())

            sale.taxLines.forEach { tax ->
                val label = if (tax.isIncludedInPrice) "${tax.taxName} (inkl.)" else tax.taxName
                SummaryRow(label, tax.taxAmount)
            }

            sale.serviceCharge?.let { sc ->
                val label = if (sc.isIncludedInPrice) "Service Charge (inkl.)" else "Service Charge"
                SummaryRow(label, sc.chargeAmount)
            }

            sale.tip?.let { tip ->
                SummaryRow("Tip", tip.amount)
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TOTAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    idrFormat.format(sale.totalAmount().amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Show added payments
            if (sale.payments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Pembayaran",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                sale.payments.forEach { payment ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(paymentMethodLabel(payment.method), style = MaterialTheme.typography.bodySmall)
                            payment.reference?.let { ref ->
                                if (ref.isNotBlank()) {
                                    Text(
                                        "Ref: $ref",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Text(
                            idrFormat.format(payment.amount.amount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val remaining = sale.totalAmount() - sale.totalPaid()
                if (remaining.amount > BigDecimal.ZERO) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sisa Tagihan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(idrFormat.format(remaining.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }

                if (sale.changeDue().amount > BigDecimal.ZERO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kembalian", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Text(idrFormat.format(sale.changeDue().amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: id.stargan.intikasirfnb.domain.shared.Money) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(idrFormat.format(amount.amount), style = MaterialTheme.typography.bodyMedium)
    }
}

// ============================================================
// Payment Input Panel
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentInputPanel(
    uiState: PaymentUiState,
    onMethodSelected: (PaymentMethod) -> Unit,
    onAmountInputChanged: (String) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onQuickCash: (Long) -> Unit,
    onAddPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingLong = uiState.remainingAmount.amount.toLong()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Remaining amount indicator
        if (uiState.addedPayments.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sisa Tagihan", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(idrFormat.format(uiState.remainingAmount.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Metode Pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaymentMethodChip("Tunai", Icons.Default.Money, uiState.selectedMethod == PaymentMethod.CASH) { onMethodSelected(PaymentMethod.CASH) }
            PaymentMethodChip("Kartu", Icons.Default.CreditCard, uiState.selectedMethod == PaymentMethod.CARD) { onMethodSelected(PaymentMethod.CARD) }
            PaymentMethodChip("E-Wallet", Icons.Default.PhoneAndroid, uiState.selectedMethod == PaymentMethod.E_WALLET) { onMethodSelected(PaymentMethod.E_WALLET) }
            PaymentMethodChip("Transfer", Icons.Default.AccountBalance, uiState.selectedMethod == PaymentMethod.TRANSFER) { onMethodSelected(PaymentMethod.TRANSFER) }
            PaymentMethodChip("Lainnya", Icons.Default.Payments, uiState.selectedMethod == PaymentMethod.OTHER) { onMethodSelected(PaymentMethod.OTHER) }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
            supportingText = if (uiState.selectedMethod != PaymentMethod.CASH) {
                { Text("Maksimal: ${idrFormat.format(uiState.remainingAmount.amount)}") }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        // Cash: quick buttons + change
        AnimatedVisibility(visible = uiState.selectedMethod == PaymentMethod.CASH) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Uang Pas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                val quickAmounts = buildQuickCashAmounts(remainingLong)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onQuickCash(remainingLong) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) { Text("Uang Pas", style = MaterialTheme.typography.labelMedium) }

                    quickAmounts.forEach { amount ->
                        OutlinedButton(
                            onClick = { onQuickCash(amount) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text(formatQuickCash(amount), style = MaterialTheme.typography.labelMedium) }
                    }
                }

                if (uiState.changeDue.amount > BigDecimal.ZERO) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kembalian", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(idrFormat.format(uiState.changeDue.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Non-cash: reference input
        AnimatedVisibility(visible = uiState.selectedMethod != PaymentMethod.CASH) {
            Column {
                OutlinedTextField(
                    value = uiState.paymentReference,
                    onValueChange = onReferenceChanged,
                    label = {
                        Text(
                            when (uiState.selectedMethod) {
                                PaymentMethod.CARD -> "No. Approval / Last 4 digit"
                                PaymentMethod.E_WALLET -> "No. Referensi"
                                PaymentMethod.TRANSFER -> "No. Referensi Transfer"
                                else -> "Keterangan"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Total / Remaining card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Tagihan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(idrFormat.format(uiState.totalAmount.amount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                if (uiState.addedPayments.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sudah Dibayar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(idrFormat.format(uiState.paidAmount.amount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sisa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(idrFormat.format(uiState.remainingAmount.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Pay / Add payment button
        Button(
            onClick = onAddPayment,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState.canAddPayment && !uiState.isProcessing,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Memproses...")
            } else {
                val isFirstOrFull = uiState.addedPayments.isEmpty()
                if (isFirstOrFull) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("BAYAR ${idrFormat.format(uiState.amountInputValue)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Tambah Pembayaran ${idrFormat.format(uiState.amountInputValue)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
