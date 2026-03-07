package id.stargan.intikasirfnb.ui.pos

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PrintDisabled
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
fun ReceiptScreen(
    viewModel: ReceiptViewModel,
    onDone: () -> Unit
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
                title = { Text("Struk Pembayaran") },
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

            uiState.sale != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Receipt preview (scrollable)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 400.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            ReceiptPreviewCard(
                                sale = uiState.sale!!,
                                outletName = uiState.outletSettings?.outletProfile?.name ?: "",
                                outletAddress = uiState.outletSettings?.outletProfile?.address,
                                outletPhone = uiState.outletSettings?.outletProfile?.phone,
                                cashierName = uiState.cashierName,
                                channelName = uiState.channelName
                            )
                        }
                    }

                    // Action buttons
                    ReceiptActionBar(
                        uiState = uiState,
                        onPrint = viewModel::printReceipt,
                        onDone = onDone
                    )
                }
            }
        }
    }
}

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
                val methodLabel = when (payment.method) {
                    id.stargan.intikasirfnb.domain.transaction.PaymentMethod.CASH -> "Tunai"
                    id.stargan.intikasirfnb.domain.transaction.PaymentMethod.CARD -> "Kartu"
                    id.stargan.intikasirfnb.domain.transaction.PaymentMethod.E_WALLET -> "E-Wallet"
                    id.stargan.intikasirfnb.domain.transaction.PaymentMethod.TRANSFER -> "Transfer"
                    id.stargan.intikasirfnb.domain.transaction.PaymentMethod.OTHER -> "Lainnya"
                }
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

@Composable
private fun ReceiptActionBar(
    uiState: ReceiptUiState,
    onPrint: () -> Unit,
    onDone: () -> Unit
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
            // Print button
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

            // Done / New transaction button
            Button(
                onClick = onDone,
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
