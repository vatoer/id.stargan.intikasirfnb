package id.stargan.intikasirfnb.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.settings.LogoSize
import id.stargan.intikasirfnb.domain.settings.PaperWidth
import id.stargan.intikasirfnb.domain.settings.PrinterConnectionType
import id.stargan.intikasirfnb.domain.settings.ReceiptBarcodeType
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsDivider
import id.stargan.intikasirfnb.ui.settings.components.SettingsGroupHeader
import id.stargan.intikasirfnb.ui.settings.components.SettingsSwitchItem
import id.stargan.intikasirfnb.ui.settings.components.SettingsTextFieldItem
import id.stargan.intikasirfnb.ui.settings.components.StickyBottomSaveBar
import id.stargan.intikasirfnb.ui.settings.components.TestPrintSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val outletSettings = uiState.outletSettings ?: return
    val receipt = outletSettings.receipt
    val header = receipt.header
    val footer = receipt.footer
    val terminalSettings = uiState.terminalSettings
    val printer = terminalSettings?.printer
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar for print result
    LaunchedEffect(uiState.printStatus) {
        when (uiState.printStatus) {
            PrintStatus.SUCCESS -> {
                snackbarHostState.showSnackbar("Tes cetak berhasil!")
                viewModel.clearPrintStatus()
            }
            PrintStatus.ERROR -> {
                snackbarHostState.showSnackbar(uiState.errorMessage ?: "Gagal mencetak")
                viewModel.clearPrintStatus()
            }
            else -> {}
        }
    }

    // Text field local state (needs explicit save)
    var customFooterText by remember(footer) { mutableStateOf(footer.customFooterText ?: "") }
    var thankYouMessage by remember(footer) { mutableStateOf(footer.thankYouMessage) }
    var socialMediaText by remember(footer) { mutableStateOf(footer.socialMediaText ?: "") }

    val hasUnsavedChanges by remember(footer) {
        derivedStateOf {
            customFooterText != (footer.customFooterText ?: "") ||
                thankYouMessage != footer.thankYouMessage ||
                socialMediaText != (footer.socialMediaText ?: "")
        }
    }

    fun saveTextFields() {
        viewModel.updateOutletSettings {
            it.copy(
                receipt = it.receipt.copy(
                    footer = it.receipt.footer.copy(
                        customFooterText = customFooterText.ifBlank { null },
                        thankYouMessage = thankYouMessage,
                        socialMediaText = socialMediaText.ifBlank { null }
                    )
                )
            )
        }
    }

    fun autoSaveHeader(transform: (id.stargan.intikasirfnb.domain.settings.ReceiptHeaderConfig) -> id.stargan.intikasirfnb.domain.settings.ReceiptHeaderConfig) {
        viewModel.updateOutletSettings {
            it.copy(receipt = it.receipt.copy(header = transform(it.receipt.header)))
        }
    }

    fun autoSaveBody(transform: (id.stargan.intikasirfnb.domain.settings.ReceiptBodyConfig) -> id.stargan.intikasirfnb.domain.settings.ReceiptBodyConfig) {
        viewModel.updateOutletSettings {
            it.copy(receipt = it.receipt.copy(body = transform(it.receipt.body)))
        }
    }

    fun autoSaveFooter(transform: (id.stargan.intikasirfnb.domain.settings.ReceiptFooterConfig) -> id.stargan.intikasirfnb.domain.settings.ReceiptFooterConfig) {
        viewModel.updateOutletSettings {
            it.copy(receipt = it.receipt.copy(footer = transform(it.receipt.footer)))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Struk") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            StickyBottomSaveBar(visible = hasUnsavedChanges, onSave = ::saveTextFields)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Ukuran Kertas ---
            SettingsGroupHeader(title = "Ukuran Kertas", modifier = Modifier.padding(top = 8.dp))
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    PaperWidth.entries.forEach { pw ->
                        FilterChip(
                            selected = receipt.paperWidth == pw,
                            onClick = {
                                viewModel.updateOutletSettings {
                                    it.copy(receipt = it.receipt.copy(paperWidth = pw))
                                }
                            },
                            label = { Text("${pw.mm}mm") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            // --- Header (toggles only — data from Informasi Toko) ---
            SettingsGroupHeader(title = "Header Struk")
            SettingsCard {
                SettingsSwitchItem(
                    title = "Tampilkan Logo",
                    subtitle = "Logo diatur di Informasi Toko",
                    checked = header.showLogo,
                    onCheckedChange = { autoSaveHeader { h -> h.copy(showLogo = it) } }
                )
                if (header.showLogo) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Ukuran Logo", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            LogoSize.entries.forEach { size ->
                                FilterChip(
                                    selected = header.logoSize == size,
                                    onClick = { autoSaveHeader { h -> h.copy(logoSize = size) } },
                                    label = {
                                        Text(
                                            when (size) {
                                                LogoSize.SMALL -> "Kecil"
                                                LogoSize.MEDIUM -> "Sedang"
                                                LogoSize.LARGE -> "Besar"
                                            }
                                        )
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
                SettingsDivider()
                SettingsSwitchItem(
                    title = "Tampilkan nama toko",
                    checked = header.showBusinessName,
                    onCheckedChange = { autoSaveHeader { h -> h.copy(showBusinessName = it) } }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    title = "Tampilkan alamat",
                    checked = header.showAddress,
                    onCheckedChange = { autoSaveHeader { h -> h.copy(showAddress = it) } }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    title = "Tampilkan telepon",
                    checked = header.showPhone,
                    onCheckedChange = { autoSaveHeader { h -> h.copy(showPhone = it) } }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    title = "Tampilkan NPWP",
                    checked = header.showNpwp,
                    onCheckedChange = { autoSaveHeader { h -> h.copy(showNpwp = it) } }
                )
            }

            // --- Isi Struk ---
            SettingsGroupHeader(title = "Tampilan Isi Struk")
            SettingsCard {
                SettingsSwitchItem(title = "Nomor order", checked = receipt.body.showOrderNumber, onCheckedChange = { autoSaveBody { b -> b.copy(showOrderNumber = it) } })
                SettingsDivider()
                SettingsSwitchItem(title = "Nomor meja", checked = receipt.body.showTableNumber, onCheckedChange = { autoSaveBody { b -> b.copy(showTableNumber = it) } })
                SettingsDivider()
                SettingsSwitchItem(title = "Nama kasir", checked = receipt.body.showCashierName, onCheckedChange = { autoSaveBody { b -> b.copy(showCashierName = it) } })
                SettingsDivider()
                SettingsSwitchItem(title = "Nama pelanggan", checked = receipt.body.showCustomerName, onCheckedChange = { autoSaveBody { b -> b.copy(showCustomerName = it) } })
                SettingsDivider()
                SettingsSwitchItem(title = "Catatan item", checked = receipt.body.showItemNotes, onCheckedChange = { autoSaveBody { b -> b.copy(showItemNotes = it) } })
                SettingsDivider()
                SettingsSwitchItem(title = "Rincian diskon", checked = receipt.body.showDiscountBreakdown, onCheckedChange = { autoSaveBody { b -> b.copy(showDiscountBreakdown = it) } })
                SettingsDivider()
                SettingsSwitchItem(title = "Rincian pajak", checked = receipt.body.showTaxDetail, onCheckedChange = { autoSaveBody { b -> b.copy(showTaxDetail = it) } })
                SettingsDivider()
                SettingsSwitchItem(title = "Rincian service charge", checked = receipt.body.showServiceChargeDetail, onCheckedChange = { autoSaveBody { b -> b.copy(showServiceChargeDetail = it) } })
                SettingsDivider()
                SettingsSwitchItem(title = "Metode pembayaran", checked = receipt.body.showPaymentMethod, onCheckedChange = { autoSaveBody { b -> b.copy(showPaymentMethod = it) } })
            }

            // --- Footer ---
            SettingsGroupHeader(title = "Footer Struk")
            SettingsCard {
                SettingsSwitchItem(
                    title = "Pesan terima kasih",
                    checked = footer.showThankYouMessage,
                    onCheckedChange = { autoSaveFooter { f -> f.copy(showThankYouMessage = it) } }
                )
                if (footer.showThankYouMessage) {
                    SettingsTextFieldItem(
                        label = "Pesan",
                        value = thankYouMessage,
                        onValueChange = { thankYouMessage = it }
                    )
                }
                SettingsDivider()
                SettingsTextFieldItem(
                    label = "Teks footer custom",
                    value = customFooterText,
                    onValueChange = { customFooterText = it },
                    singleLine = false,
                    placeholder = "contoh: Promo, info, dll"
                )
                SettingsDivider()
                SettingsSwitchItem(
                    title = "Tampilkan sosial media",
                    checked = footer.showSocialMedia,
                    onCheckedChange = { autoSaveFooter { f -> f.copy(showSocialMedia = it) } }
                )
                if (footer.showSocialMedia) {
                    SettingsTextFieldItem(
                        label = "Info sosial media",
                        value = socialMediaText,
                        onValueChange = { socialMediaText = it },
                        placeholder = "contoh: IG: @warungpadang"
                    )
                }
                SettingsDivider()
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Barcode / QR Code", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        ReceiptBarcodeType.entries.forEach { type ->
                            FilterChip(
                                selected = footer.barcodeType == type,
                                onClick = {
                                    autoSaveFooter { f -> f.copy(barcodeType = type) }
                                },
                                label = {
                                    Text(
                                        when (type) {
                                            ReceiptBarcodeType.NONE -> "Tidak ada"
                                            ReceiptBarcodeType.BARCODE_CODE128 -> "Barcode"
                                            ReceiptBarcodeType.QR_CODE -> "QR Code"
                                        }
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
            }

            // ==================== TES CETAK ====================
            if (printer != null && printer.connectionType != PrinterConnectionType.NONE) {
                SettingsGroupHeader(title = "Tes Cetak")
                TestPrintSection(
                    printStatus = uiState.printStatus,
                    connectionType = printer.connectionType,
                    printerName = printer.name,
                    printerAddress = printer.address,
                    onTestPrint = { viewModel.testPrint() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
