package id.stargan.intikasirfnb.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import id.stargan.intikasirfnb.domain.settings.PrinterConnectionType
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsDivider
import id.stargan.intikasirfnb.ui.settings.components.SettingsGroupHeader
import id.stargan.intikasirfnb.ui.settings.components.SettingsSwitchItem
import id.stargan.intikasirfnb.ui.settings.components.SettingsTextFieldItem
import id.stargan.intikasirfnb.ui.settings.components.StickyBottomSaveBar
import id.stargan.intikasirfnb.ui.settings.components.TestPrintSection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrinterSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val terminalSettings = uiState.terminalSettings ?: return
    val printer = terminalSettings.printer
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Text field local state
    var address by remember(printer) { mutableStateOf(printer.address ?: "") }
    var name by remember(printer) { mutableStateOf(printer.name ?: "") }
    var printDensity by remember(printer) { mutableStateOf(printer.printDensity.toString()) }
    var receiptCopies by remember(printer) { mutableStateOf(printer.receiptCopies.toString()) }
    var kitchenTicketCopies by remember(printer) { mutableStateOf(printer.kitchenTicketCopies.toString()) }

    // Toggle visibility for device lists
    var showDiscoveredDevices by rememberSaveable { mutableStateOf(true) }
    var showPairedDevices by rememberSaveable { mutableStateOf(true) }

    val hasUnsavedChanges by remember(printer) {
        derivedStateOf {
            address != (printer.address ?: "") ||
                name != (printer.name ?: "") ||
                printDensity != printer.printDensity.toString() ||
                receiptCopies != printer.receiptCopies.toString() ||
                kitchenTicketCopies != printer.kitchenTicketCopies.toString()
        }
    }

    fun saveTextFields() {
        viewModel.updateTerminalSettings {
            it.copy(
                printer = it.printer.copy(
                    address = address.ifBlank { null },
                    name = name.ifBlank { null },
                    printDensity = (printDensity.toIntOrNull() ?: 5).coerceIn(1, 8),
                    receiptCopies = (receiptCopies.toIntOrNull() ?: 1).coerceAtLeast(1),
                    kitchenTicketCopies = (kitchenTicketCopies.toIntOrNull() ?: 1).coerceAtLeast(1)
                )
            )
        }
    }

    fun autoSavePrinter(transform: (id.stargan.intikasirfnb.domain.settings.PrinterConfig) -> id.stargan.intikasirfnb.domain.settings.PrinterConfig) {
        viewModel.updateTerminalSettings {
            it.copy(printer = transform(it.printer))
        }
    }

    // --- Permission launchers ---
    val btConnectPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadPairedBluetoothDevices()
        else scope.launch { snackbarHostState.showSnackbar("Izin Bluetooth diperlukan untuk melihat perangkat") }
    }

    val btScanPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startBluetoothDiscovery()
        else scope.launch { snackbarHostState.showSnackbar("Izin diperlukan untuk memindai perangkat") }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startBluetoothDiscovery()
        else scope.launch { snackbarHostState.showSnackbar("Izin lokasi diperlukan untuk memindai Bluetooth") }
    }

    fun hasBtConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun loadBtDevices() {
        if (!viewModel.isBluetoothEnabled()) {
            scope.launch { snackbarHostState.showSnackbar("Bluetooth belum diaktifkan") }
            return
        }
        if (!hasBtConnectPermission()) {
            btConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        viewModel.loadPairedBluetoothDevices()
    }

    fun startDiscovery() {
        if (!viewModel.isBluetoothEnabled()) {
            scope.launch { snackbarHostState.showSnackbar("Bluetooth belum diaktifkan") }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                btScanPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
                return
            }
            if (!hasBtConnectPermission()) {
                btConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return
            }
        }
        showDiscoveredDevices = true
        viewModel.startBluetoothDiscovery()
    }

    // Auto-load paired devices when BT selected
    LaunchedEffect(printer.connectionType) {
        if (printer.connectionType == PrinterConnectionType.BLUETOOTH) {
            if (hasBtConnectPermission()) {
                viewModel.loadPairedBluetoothDevices()
            }
        }
    }

    // Stop discovery when leaving the screen
    DisposableEffect(Unit) {
        onDispose { viewModel.stopBluetoothDiscovery() }
    }

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

    // Snackbar for general errors
    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage
        if (msg != null && uiState.printStatus != PrintStatus.ERROR) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Printer") },
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
            // ==================== 1. TIPE KONEKSI ====================
            SettingsGroupHeader(title = "Tipe Koneksi", modifier = Modifier.padding(top = 8.dp))
            SettingsCard {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrinterConnectionType.entries.forEach { type ->
                        FilterChip(
                            selected = printer.connectionType == type,
                            onClick = {
                                autoSavePrinter { p -> p.copy(connectionType = type) }
                            },
                            label = {
                                Text(
                                    when (type) {
                                        PrinterConnectionType.NONE -> "Tidak ada"
                                        PrinterConnectionType.BLUETOOTH -> "Bluetooth"
                                        PrinterConnectionType.USB -> "USB"
                                        PrinterConnectionType.NETWORK -> "Jaringan"
                                    },
                                    maxLines = 1
                                )
                            },
                            leadingIcon = if (printer.connectionType == type) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // ==================== BLUETOOTH ====================
            if (printer.connectionType == PrinterConnectionType.BLUETOOTH) {

                // --- BT Status Warning ---
                if (!viewModel.isBluetoothEnabled()) {
                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .clickable {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BluetoothDisabled,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Bluetooth tidak aktif",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Ketuk untuk membuka pengaturan Bluetooth",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // ==================== 2. PINDAI PERANGKAT ====================
                SettingsGroupHeader(title = "Pindai Perangkat")
                SettingsCard {
                    // Scan button / scanning state
                    if (uiState.isBtScanning) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sedang memindai...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${uiState.discoveredDevices.size} perangkat ditemukan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { viewModel.stopBluetoothDiscovery() }) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Berhenti")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (uiState.discoveredDevices.isEmpty()) "Cari perangkat Bluetooth terdekat"
                                else "${uiState.discoveredDevices.size} perangkat ditemukan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = ::startDiscovery) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pindai")
                            }
                        }
                    }

                    // Discovered device list (collapsible)
                    if (uiState.discoveredDevices.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        // Toggle header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDiscoveredDevices = !showDiscoveredDevices }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${uiState.discoveredDevices.size} perangkat ditemukan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (showDiscoveredDevices) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showDiscoveredDevices) "Sembunyikan" else "Tampilkan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AnimatedVisibility(visible = showDiscoveredDevices) {
                            Column {
                                uiState.discoveredDevices.forEachIndexed { index, device ->
                                    BluetoothDeviceRow(
                                        deviceName = device.name,
                                        deviceAddress = device.address,
                                        isSelected = address == device.address,
                                        icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                                        badge = if (device.isPaired) "Terpasang" else null,
                                        onClick = {
                                            address = device.address
                                            if (name.isBlank()) name = device.name
                                        }
                                    )
                                    if (index < uiState.discoveredDevices.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== 3. PERANGKAT TERPASANG ====================
                SettingsGroupHeader(title = "Perangkat Terpasang")
                SettingsCard {
                    if (uiState.pairedBluetoothDevices.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Belum ada perangkat terpasang",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = ::loadBtDevices) {
                                Text("Muat Ulang")
                            }
                        }
                    } else {
                        // Toggle header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPairedDevices = !showPairedDevices }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${uiState.pairedBluetoothDevices.size} perangkat terpasang",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = ::loadBtDevices) {
                                Text("Muat Ulang", style = MaterialTheme.typography.labelMedium)
                            }
                            Icon(
                                if (showPairedDevices) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showPairedDevices) "Sembunyikan" else "Tampilkan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AnimatedVisibility(visible = showPairedDevices) {
                            Column {
                                uiState.pairedBluetoothDevices.forEachIndexed { index, device ->
                                    BluetoothDeviceRow(
                                        deviceName = device.name,
                                        deviceAddress = device.address,
                                        isSelected = address == device.address,
                                        icon = Icons.Default.Bluetooth,
                                        onClick = {
                                            address = device.address
                                            if (name.isBlank()) name = device.name
                                        }
                                    )
                                    if (index < uiState.pairedBluetoothDevices.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== 4. INPUT MANUAL ====================
                SettingsGroupHeader(title = "Input Manual")
                SettingsCard {
                    SettingsTextFieldItem(
                        label = "Nama Printer",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "contoh: EPSON TM-T82X"
                    )
                    SettingsTextFieldItem(
                        label = "Alamat MAC",
                        value = address,
                        onValueChange = { address = it },
                        placeholder = "00:11:22:33:44:55"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ==================== NETWORK ====================
            if (printer.connectionType == PrinterConnectionType.NETWORK) {
                SettingsGroupHeader(title = "Koneksi Jaringan")
                SettingsCard {
                    SettingsTextFieldItem(
                        label = "Nama Printer",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "contoh: EPSON TM-T82X"
                    )
                    SettingsTextFieldItem(
                        label = "Alamat IP:Port",
                        value = address,
                        onValueChange = { address = it },
                        placeholder = "192.168.1.100:9100"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ==================== USB ====================
            if (printer.connectionType == PrinterConnectionType.USB) {
                SettingsGroupHeader(title = "Koneksi USB")
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Usb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "USB belum didukung",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Gunakan Bluetooth atau Jaringan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (printer.connectionType != PrinterConnectionType.NONE) {
                // ==================== TES CETAK ====================
                SettingsGroupHeader(title = "Tes Cetak")
                TestPrintSection(
                    printStatus = uiState.printStatus,
                    connectionType = printer.connectionType,
                    printerName = name.ifBlank { printer.name },
                    printerAddress = address.ifBlank { printer.address },
                    onTestPrint = {
                        saveTextFields()
                        viewModel.testPrint()
                    }
                )

                // ==================== PENCETAKAN OTOMATIS ====================
                SettingsGroupHeader(title = "Pencetakan Otomatis")
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Cetak struk otomatis",
                        subtitle = "Setelah pembayaran selesai",
                        checked = printer.autoPrintReceipt,
                        onCheckedChange = { autoSavePrinter { p -> p.copy(autoPrintReceipt = it) } }
                    )
                    SettingsDivider()
                    SettingsSwitchItem(
                        title = "Cetak tiket dapur otomatis",
                        subtitle = "Saat order dikonfirmasi",
                        checked = printer.autoPrintKitchenTicket,
                        onCheckedChange = { autoSavePrinter { p -> p.copy(autoPrintKitchenTicket = it) } }
                    )
                }

                // ==================== JUMLAH CETAK ====================
                SettingsGroupHeader(title = "Jumlah Cetak")
                SettingsCard {
                    SettingsTextFieldItem(
                        label = "Copy struk",
                        value = receiptCopies,
                        onValueChange = { receiptCopies = it.filter { c -> c.isDigit() } }
                    )
                    SettingsTextFieldItem(
                        label = "Copy tiket dapur",
                        value = kitchenTicketCopies,
                        onValueChange = { kitchenTicketCopies = it.filter { c -> c.isDigit() } }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ==================== HARDWARE ====================
                SettingsGroupHeader(title = "Hardware")
                SettingsCard {
                    SettingsSwitchItem(
                        title = "Auto-cut kertas",
                        subtitle = "Potong kertas setelah cetak",
                        checked = printer.autoCut,
                        onCheckedChange = { autoSavePrinter { p -> p.copy(autoCut = it) } }
                    )
                    SettingsDivider()
                    SettingsTextFieldItem(
                        label = "Kepadatan cetak (1-8)",
                        value = printDensity,
                        onValueChange = { printDensity = it.filter { c -> c.isDigit() } }
                    )
                    SettingsDivider()
                    SettingsSwitchItem(
                        title = "Buka cash drawer",
                        subtitle = "Setelah pembayaran tunai",
                        checked = printer.openCashDrawer,
                        onCheckedChange = { autoSavePrinter { p -> p.copy(openCashDrawer = it) } }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BluetoothDeviceRow(
    deviceName: String,
    deviceAddress: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = deviceAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Dipilih",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
