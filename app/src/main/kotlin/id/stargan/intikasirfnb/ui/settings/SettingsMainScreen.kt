package id.stargan.intikasirfnb.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsGroupHeader
import id.stargan.intikasirfnb.ui.settings.components.SettingsDivider
import id.stargan.intikasirfnb.ui.settings.components.SettingsNavigationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOutletProfile: () -> Unit,
    onNavigateToTax: () -> Unit,
    onNavigateToServiceCharge: () -> Unit,
    onNavigateToTip: () -> Unit,
    onNavigateToReceipt: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToSalesChannels: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Toko ---
            item {
                SettingsGroupHeader(
                    title = "Toko",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                SettingsCard {
                    SettingsNavigationItem(
                        icon = Icons.Default.Store,
                        title = "Informasi Toko",
                        subtitle = outletProfileSummary(uiState),
                        onClick = onNavigateToOutletProfile
                    )
                }
            }

            // --- Penjualan ---
            item {
                SettingsGroupHeader(title = "Penjualan")
            }
            item {
                SettingsCard {
                    SettingsNavigationItem(
                        icon = Icons.Default.Storefront,
                        title = "Channel Penjualan",
                        subtitle = "Dine In, Take Away, Platform Delivery",
                        onClick = onNavigateToSalesChannels
                    )
                }
            }

            // --- Pajak & Biaya ---
            item {
                SettingsGroupHeader(title = "Pajak & Biaya")
            }
            item {
                SettingsCard {
                    SettingsNavigationItem(
                        icon = Icons.Default.LocalOffer,
                        title = "Pajak",
                        subtitle = taxSummary(uiState),
                        onClick = onNavigateToTax
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Percent,
                        title = "Service Charge",
                        subtitle = serviceChargeSummary(uiState),
                        onClick = onNavigateToServiceCharge
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.VolunteerActivism,
                        title = "Tip",
                        subtitle = tipSummary(uiState),
                        onClick = onNavigateToTip
                    )
                }
            }

            // --- Struk & Cetak ---
            item {
                SettingsGroupHeader(title = "Struk & Cetak")
            }
            item {
                SettingsCard {
                    SettingsNavigationItem(
                        icon = Icons.Default.Receipt,
                        title = "Pengaturan Struk",
                        subtitle = "Tampilan header, footer, barcode",
                        onClick = onNavigateToReceipt
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Print,
                        title = "Printer",
                        subtitle = printerSummary(uiState),
                        onClick = onNavigateToPrinter
                    )
                }
            }
        }
    }
}

private fun outletProfileSummary(state: SettingsUiState): String {
    val profile = state.outletSettings?.outletProfile ?: return "Belum diatur"
    return profile.name.ifBlank { "Belum diatur" }
}

private fun taxSummary(state: SettingsUiState): String {
    val active = state.taxConfigs.filter { it.isActive }
    return if (active.isEmpty()) "Belum ada pajak" else "${active.size} pajak aktif"
}

private fun serviceChargeSummary(state: SettingsUiState): String {
    val sc = state.outletSettings?.serviceCharge ?: return "Nonaktif"
    return if (sc.isEnabled) "Aktif - ${sc.rate.toPlainString()}%" else "Nonaktif"
}

private fun tipSummary(state: SettingsUiState): String {
    val tip = state.outletSettings?.tip ?: return "Nonaktif"
    return if (tip.isEnabled) "Aktif" else "Nonaktif"
}

private fun printerSummary(state: SettingsUiState): String {
    val printer = state.terminalSettings?.printer ?: return "Belum diatur"
    return when (printer.connectionType) {
        id.stargan.intikasirfnb.domain.settings.PrinterConnectionType.NONE -> "Belum diatur"
        id.stargan.intikasirfnb.domain.settings.PrinterConnectionType.BLUETOOTH -> "Bluetooth - ${printer.name ?: printer.address ?: ""}"
        id.stargan.intikasirfnb.domain.settings.PrinterConnectionType.USB -> "USB - ${printer.name ?: ""}"
        id.stargan.intikasirfnb.domain.settings.PrinterConnectionType.NETWORK -> "Jaringan - ${printer.address ?: ""}"
    }
}
