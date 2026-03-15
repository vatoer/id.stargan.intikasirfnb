package id.stargan.intikasirfnb.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idrFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
    maximumFractionDigits = 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierSessionScreen(
    viewModel: CashierSessionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPos: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.sessionOpened) {
        if (uiState.sessionOpened) {
            snackbarHostState.showSnackbar("Sesi kasir berhasil dibuka")
            viewModel.resetSessionOpened()
        }
    }

    LaunchedEffect(uiState.sessionClosed) {
        if (uiState.sessionClosed) {
            snackbarHostState.showSnackbar("Sesi kasir berhasil ditutup")
            viewModel.resetSessionClosed()
        }
    }

    // Open session dialog
    if (uiState.showOpenDialog) {
        OpenSessionDialog(
            openingFloatInput = uiState.openingFloatInput,
            isProcessing = uiState.isProcessing,
            onOpeningFloatChange = viewModel::updateOpeningFloat,
            onConfirm = viewModel::openSession,
            onDismiss = viewModel::dismissOpenDialog
        )
    }

    // Close session dialog
    if (uiState.showCloseDialog) {
        CloseSessionDialog(
            session = uiState.currentSession,
            closingCashInput = uiState.closingCashInput,
            closeNotes = uiState.closeNotes,
            isProcessing = uiState.isProcessing,
            onClosingCashChange = viewModel::updateClosingCash,
            onNotesChange = viewModel::updateCloseNotes,
            onConfirm = viewModel::closeSession,
            onDismiss = viewModel::dismissCloseDialog
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sesi Kasir") },
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

            uiState.currentSession != null -> {
                // Active session view
                ActiveSessionContent(
                    session = uiState.currentSession!!,
                    onNavigateToPos = onNavigateToPos,
                    onCloseSession = viewModel::showCloseDialog,
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                // No active session
                NoSessionContent(
                    onOpenSession = viewModel::showOpenDialog,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ActiveSessionContent(
    session: CashierSession,
    onNavigateToPos: () -> Unit,
    onCloseSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id")) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status indicator
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Sesi Aktif",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Dibuka: ${dateFormat.format(Date(session.openAtMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Session details
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Detail Sesi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                SessionDetailRow("ID Sesi", session.id.value.takeLast(8).uppercase())
                SessionDetailRow("Waktu Buka", dateFormat.format(Date(session.openAtMillis)))
                SessionDetailRow("Modal Awal", idrFormat.format(session.openingFloat.amount))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Column(
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToPos,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Mulai Transaksi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onCloseSession,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Tutup Sesi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NoSessionContent(
    onOpenSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LockOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Belum Ada Sesi Aktif",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Buka sesi kasir terlebih dahulu\nsebelum memulai transaksi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onOpenSession,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Buka Sesi Kasir",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SessionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OpenSessionDialog(
    openingFloatInput: String,
    isProcessing: Boolean,
    onOpeningFloatChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val displayAmount = try {
        idrFormat.format(BigDecimal(openingFloatInput))
    } catch (_: Exception) {
        "Rp0"
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
        title = { Text("Buka Sesi Kasir") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Masukkan jumlah uang modal awal di laci kasir.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = openingFloatInput,
                    onValueChange = onOpeningFloatChange,
                    label = { Text("Modal Awal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    displayAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Buka Sesi")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun CloseSessionDialog(
    session: CashierSession?,
    closingCashInput: String,
    closeNotes: String,
    isProcessing: Boolean,
    onClosingCashChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val closingAmount = try {
        BigDecimal(closingCashInput)
    } catch (_: Exception) {
        BigDecimal.ZERO
    }

    val expectedAmount = session?.openingFloat?.amount ?: BigDecimal.ZERO
    val difference = closingAmount - expectedAmount

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text("Tutup Sesi Kasir") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Hitung uang tunai di laci kasir dan masukkan jumlahnya.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Expected cash
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kas Diharapkan", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            idrFormat.format(expectedAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = closingCashInput,
                    onValueChange = onClosingCashChange,
                    label = { Text("Kas Aktual (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Difference
                val diffColor = when {
                    difference > BigDecimal.ZERO -> MaterialTheme.colorScheme.primary
                    difference < BigDecimal.ZERO -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val diffLabel = when {
                    difference > BigDecimal.ZERO -> "Selisih: +${idrFormat.format(difference)} (lebih)"
                    difference < BigDecimal.ZERO -> "Selisih: ${idrFormat.format(difference)} (kurang)"
                    else -> "Selisih: Rp0 (sesuai)"
                }

                Text(
                    diffLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = diffColor
                )

                OutlinedTextField(
                    value = closeNotes,
                    onValueChange = onNotesChange,
                    label = { Text("Catatan (opsional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Tutup Sesi")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Batal")
            }
        }
    )
}
