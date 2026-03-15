package id.stargan.intikasirfnb.ui.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.stargan.intikasirfnb.domain.catalog.AddOnGroup
import id.stargan.intikasirfnb.domain.catalog.AddOnItem
import id.stargan.intikasirfnb.domain.catalog.AddOnItemId
import id.stargan.intikasirfnb.domain.catalog.MenuItemAddOnLink
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierOption
import id.stargan.intikasirfnb.domain.catalog.ModifierOptionId
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.ChannelType
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.SelectedAddOn
import id.stargan.intikasirfnb.domain.transaction.SelectedModifier
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import id.stargan.intikasirfnb.domain.customer.Customer
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.ui.table.TablePickerContent
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idrFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
    maximumFractionDigits = 0
}

// Breakpoint: screens wider than 600dp use split-panel (tablet), narrower use bottom sheet (phone)
private val TABLET_BREAKPOINT = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (saleId: String) -> Unit = {},
    onNavigateToTableSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Refresh tables when returning from other screens (e.g., table settings)
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTables()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Show snackbar when kitchen ticket is sent
    LaunchedEffect(uiState.kitchenTicketResult) {
        uiState.kitchenTicketResult?.let { result ->
            val itemCount = result.newLines.sumOf { it.quantity }
            snackbarHostState.showSnackbar("$itemCount item dikirim ke dapur")
            viewModel.clearKitchenTicketResult()
        }
    }

    // Open orders bottom sheet
    if (uiState.showOpenOrders) {
        OpenOrdersSheet(
            openOrders = uiState.sortedOpenOrders,
            salesChannels = uiState.salesChannels,
            tables = uiState.tables,
            sortMode = uiState.orderSortMode,
            onSortModeChanged = { viewModel.setOrderSortMode(it) },
            onDismiss = { viewModel.toggleOpenOrders() },
            onResumeOrder = { saleId -> viewModel.resumeOpenOrder(saleId) },
            onNewOrder = { viewModel.newOrder() }
        )
    }

    // Table picker bottom sheet
    if (uiState.showTablePicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideTablePicker() }
        ) {
            TablePickerContent(
                tables = uiState.tables,
                sections = uiState.tableSections,
                selectedSection = uiState.selectedTableSection,
                onSectionSelected = { viewModel.selectTableSection(it) },
                onTableSelected = { table -> viewModel.selectTable(table) },
                onDismiss = { viewModel.hideTablePicker() }
            )
        }
    }

    // Table setup prompt — DINE_IN + PAY_LAST but no tables configured
    if (uiState.showTableSetupPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTableSetupPrompt() },
            icon = {
                Icon(
                    Icons.Default.TableRestaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Belum Ada Meja") },
            text = {
                Text(
                    "Channel ini dikonfigurasi \"Wajib Meja\" tapi belum ada meja yang dibuat.\n\n" +
                            "Silakan buat daftar meja di Pengaturan, atau ubah pengaturan meja channel menjadi \"Tanpa Meja\" atau \"Opsional\"."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissTableSetupPrompt()
                    onNavigateToTableSettings()
                }) {
                    Text("Kelola Meja")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTableSetupPrompt() }) {
                    Text("Nanti")
                }
            }
        )
    }

    // Discount dialog
    var discountLineId by remember { mutableStateOf<id.stargan.intikasirfnb.domain.transaction.OrderLineId?>(null) }
    discountLineId?.let { lineId ->
        val line = uiState.currentSale?.lines?.find { it.id == lineId }
        if (line != null) {
            DiscountDialog(
                productName = line.productRef.name,
                subtotal = line.subtotalBeforeDiscount(),
                existingInfo = line.discountInfo,
                onDismiss = { discountLineId = null },
                onApply = { info ->
                    viewModel.applyDiscount(lineId, info)
                    discountLineId = null
                },
                onClear = {
                    viewModel.clearDiscount(lineId)
                    discountLineId = null
                }
            )
        }
    }

    // Modifier + Add-on selection bottom sheet (add new or edit existing)
    val pendingItem = uiState.pendingMenuItem
    if (uiState.showModifierDialog && pendingItem != null) {
        val isEditing = uiState.editingLineId != null
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissModifierDialog() }
        ) {
            ModifierAndAddOnSelectionContent(
                menuItem = pendingItem,
                modifierGroups = uiState.modifierGroups,
                addOnGroups = uiState.addOnGroups,
                existingModifierSelections = if (isEditing) uiState.editingModifiers else emptyList(),
                existingAddOnSelections = if (isEditing) uiState.editingAddOns else emptyList(),
                confirmLabel = if (isEditing) "Simpan Perubahan" else "Tambahkan",
                onConfirm = { selectedModifiers, selectedAddOns ->
                    viewModel.confirmModifierAndAddOnSelection(selectedModifiers, selectedAddOns)
                },
                onDismiss = { viewModel.dismissModifierDialog() }
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= TABLET_BREAKPOINT

        if (isTablet) {
            TabletPosLayout(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onNavigateToPayment = onNavigateToPayment,
                onNavigateToHistory = onNavigateToHistory,
                onDiscountLine = { discountLineId = it }
            )
        } else {
            PhonePosLayout(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onNavigateToPayment = onNavigateToPayment,
                onNavigateToHistory = onNavigateToHistory,
                onDiscountLine = { discountLineId = it }
            )
        }
    }
}

// ============================================================
// TABLET LAYOUT — Split panel (menu 60% + cart 40%)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletPosLayout(
    uiState: PosUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (saleId: String) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onDiscountLine: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit = {}
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            PosTopBar(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onChannelSelected = viewModel::selectChannel,
                onOrderFlowOverride = viewModel::overrideOrderFlow,
                onOpenOrdersClicked = viewModel::toggleOpenOrders,
                openOrdersCount = uiState.openOrders.size,
                onHistoryClicked = onNavigateToHistory
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                MenuPanel(
                    uiState = uiState,
                    cartQuantities = cartQuantities(uiState.currentSale),
                    onCategorySelected = viewModel::selectCategory,
                    onSearchChanged = viewModel::updateSearch,
                    onItemClicked = viewModel::addItemToCart,
                    gridMinSize = 130.dp,
                    cardHeight = 140.dp,
                    imageHeight = 70.dp,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )

                VerticalDivider()

                CartPanel(
                    sale = uiState.currentSale,
                    orderFlow = uiState.effectiveOrderFlow,
                    channelType = uiState.selectedChannel?.channelType,
                    isSendingToKitchen = uiState.isSendingToKitchen,
                    openOrders = uiState.sortedOpenOrders,
                    salesChannels = uiState.salesChannels,
                    onIncrement = viewModel::incrementLine,
                    onDecrement = viewModel::decrementLine,
                    onRemove = viewModel::removeLine,
                    onEditModifiers = viewModel::editLineModifiers,
                    onApplyDiscount = { lineId -> onDiscountLine(lineId) },
                    onClearDiscount = { lineId -> viewModel.clearDiscount(lineId) },
                    onClearCart = viewModel::clearCart,
                    onPay = { sale -> onNavigateToPayment(sale.id.value) },
                    onSendToKitchen = viewModel::sendToKitchen,
                    onResumeOrder = viewModel::resumeOpenOrder,
                    onNewOrder = viewModel::newOrder,
                    onTableChanged = viewModel::setTableNumber,
                    onCustomerNameChanged = viewModel::setCustomerName,
                    customerSearchResults = uiState.customerSearchResults,
                    onCustomerSearch = viewModel::searchCustomers,
                    onCustomerSelected = viewModel::selectCustomer,
                    onCustomerCleared = viewModel::clearCustomer,
                    hasTables = uiState.tables.isNotEmpty(),
                    tables = uiState.tables,
                    onShowTablePicker = viewModel::showTablePicker,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

// ============================================================
// PHONE LAYOUT — Full-width menu + bottom sheet cart
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhonePosLayout(
    uiState: PosUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (saleId: String) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onDiscountLine: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    val cartItemCount = uiState.currentSale?.lines?.sumOf { it.quantity } ?: 0

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            PosTopBar(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onChannelSelected = viewModel::selectChannel,
                onOrderFlowOverride = viewModel::overrideOrderFlow,
                onOpenOrdersClicked = viewModel::toggleOpenOrders,
                openOrdersCount = uiState.openOrders.size,
                onHistoryClicked = onNavigateToHistory
            )
        },
        sheetPeekHeight = if (cartItemCount > 0) 80.dp else 0.dp,
        sheetContent = {
            if (cartItemCount > 0) {
                CartPanel(
                    sale = uiState.currentSale,
                    orderFlow = uiState.effectiveOrderFlow,
                    channelType = uiState.selectedChannel?.channelType,
                    isSendingToKitchen = uiState.isSendingToKitchen,
                    openOrders = uiState.sortedOpenOrders,
                    salesChannels = uiState.salesChannels,
                    onIncrement = viewModel::incrementLine,
                    onDecrement = viewModel::decrementLine,
                    onRemove = viewModel::removeLine,
                    onEditModifiers = viewModel::editLineModifiers,
                    onApplyDiscount = { lineId -> onDiscountLine(lineId) },
                    onClearDiscount = { lineId -> viewModel.clearDiscount(lineId) },
                    onClearCart = viewModel::clearCart,
                    onPay = { sale -> onNavigateToPayment(sale.id.value) },
                    onSendToKitchen = viewModel::sendToKitchen,
                    onResumeOrder = viewModel::resumeOpenOrder,
                    onNewOrder = viewModel::newOrder,
                    onTableChanged = viewModel::setTableNumber,
                    onCustomerNameChanged = viewModel::setCustomerName,
                    customerSearchResults = uiState.customerSearchResults,
                    onCustomerSearch = viewModel::searchCustomers,
                    onCustomerSelected = viewModel::selectCustomer,
                    onCustomerCleared = viewModel::clearCustomer,
                    hasTables = uiState.tables.isNotEmpty(),
                    tables = uiState.tables,
                    onShowTablePicker = viewModel::showTablePicker,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                MenuPanel(
                    uiState = uiState,
                    cartQuantities = cartQuantities(uiState.currentSale),
                    onCategorySelected = viewModel::selectCategory,
                    onSearchChanged = viewModel::updateSearch,
                    onItemClicked = { item ->
                        viewModel.addItemToCart(item)
                    },
                    gridMinSize = 100.dp,
                    cardHeight = 110.dp,
                    imageHeight = 50.dp,
                    gridBottomPadding = if (cartItemCount > 0) 148.dp else 64.dp,
                    modifier = Modifier.fillMaxSize()
                )

                // FAB to expand cart
                if (cartItemCount > 0 && sheetState.currentValue == SheetValue.PartiallyExpanded) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch { sheetState.expand() }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 12.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text("$cartItemCount")
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Keranjang")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosTopBar(
    uiState: PosUiState,
    onNavigateBack: () -> Unit,
    onChannelSelected: (id.stargan.intikasirfnb.domain.transaction.SalesChannel) -> Unit,
    onOrderFlowOverride: (OrderFlowType?) -> Unit,
    onOpenOrdersClicked: () -> Unit,
    openOrdersCount: Int,
    onHistoryClicked: () -> Unit = {}
) {
    Column {
        TopAppBar(
            title = { Text("POS Kasir") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
            },
            actions = {
                // Transaction history button
                IconButton(onClick = onHistoryClicked) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Riwayat Transaksi")
                }
                // Open orders button
                if (openOrdersCount > 0) {
                    IconButton(onClick = onOpenOrdersClicked) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                    Text("$openOrdersCount")
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Pesanan Aktif")
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Channel selector bar
        if (uiState.salesChannels.isNotEmpty()) {
            ChannelSelectorBar(
                channels = uiState.salesChannels,
                selectedChannel = uiState.selectedChannel,
                onChannelSelected = onChannelSelected
            )
        }

        // Order flow override chips — show when channel is PAY_FLEXIBLE and no active sale
        val channel = uiState.selectedChannel
        val canOverride = channel?.defaultOrderFlow == OrderFlowType.PAY_FLEXIBLE
            && uiState.currentSale == null
        if (canOverride) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Alur:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OrderFlowType.entries.forEach { flow ->
                    val isSelected = uiState.effectiveOrderFlow == flow
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onOrderFlowOverride(if (flow == channel.defaultOrderFlow) null else flow)
                        },
                        label = {
                            Text(
                                when (flow) {
                                    OrderFlowType.PAY_FIRST -> "Bayar Dulu"
                                    OrderFlowType.PAY_LAST -> "Bayar Akhir"
                                    OrderFlowType.PAY_FLEXIBLE -> "Fleksibel"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// CHANNEL SELECTOR BAR
// ============================================================

@Composable
private fun ChannelSelectorBar(
    channels: List<id.stargan.intikasirfnb.domain.transaction.SalesChannel>,
    selectedChannel: id.stargan.intikasirfnb.domain.transaction.SalesChannel?,
    onChannelSelected: (id.stargan.intikasirfnb.domain.transaction.SalesChannel) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(channels) { channel ->
                val isSelected = selectedChannel?.id == channel.id
                ChannelChip(
                    channel = channel,
                    isSelected = isSelected,
                    onClick = { onChannelSelected(channel) }
                )
            }
        }
    }
}

@Composable
private fun ChannelChip(
    channel: id.stargan.intikasirfnb.domain.transaction.SalesChannel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = channelTypeIcon(channel.channelType)
    val flowLabel = when (channel.defaultOrderFlow) {
        OrderFlowType.PAY_FIRST -> "Bayar Dulu"
        OrderFlowType.PAY_LAST -> "Bayar Akhir"
        OrderFlowType.PAY_FLEXIBLE -> "Fleksibel"
    }
    val subtitle = when (channel.channelType) {
        ChannelType.DELIVERY_PLATFORM -> channel.platformConfig?.platformName ?: flowLabel
        else -> flowLabel
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        ),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                channel.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun channelTypeIcon(type: ChannelType): androidx.compose.ui.graphics.vector.ImageVector =
    when (type) {
        ChannelType.DINE_IN -> Icons.Default.Restaurant
        ChannelType.TAKE_AWAY -> Icons.Default.ShoppingCart
        ChannelType.DELIVERY_PLATFORM -> Icons.Default.DeliveryDining
        ChannelType.OWN_DELIVERY -> Icons.Default.DeliveryDining
    }

// ============================================================
// OPEN ORDERS SHEET
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenOrdersSheet(
    openOrders: List<Sale>,
    salesChannels: List<id.stargan.intikasirfnb.domain.transaction.SalesChannel>,
    tables: List<Table> = emptyList(),
    sortMode: OrderSortMode,
    onSortModeChanged: (OrderSortMode) -> Unit,
    onDismiss: () -> Unit,
    onResumeOrder: (SaleId) -> Unit,
    onNewOrder: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("id")) }
    val channelMap = remember(salesChannels) { salesChannels.associateBy { it.id } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Pesanan Aktif",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${openOrders.size} pesanan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sort chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OrderSortMode.entries.forEach { mode ->
                    val label = when (mode) {
                        OrderSortMode.TIME -> "Waktu"
                        OrderSortMode.TABLE -> "Meja"
                        OrderSortMode.NAME -> "Nama"
                    }
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { onSortModeChanged(mode) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(openOrders, key = { it.id.value }) { sale ->
                    val channelName = channelMap[sale.channelId]?.name ?: ""
                    val itemCount = sale.lines.sumOf { it.quantity }
                    val sentCount = sale.lines.filter { it.isSentToKitchen }.sumOf { it.quantity }
                    val unsentCount = sale.lines.filter { !it.isSentToKitchen }.sumOf { it.quantity }

                    Card(
                        onClick = { onResumeOrder(sale.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sale.status == SaleStatus.OPEN)
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Status indicator
                                        val statusColor = if (sale.status == SaleStatus.OPEN)
                                            MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.outline
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    statusColor,
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            sale.receiptNumber ?: sale.id.value.takeLast(6),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    val subtitle = buildString {
                                        append(channelName)
                                        resolveTableName(sale.tableId, tables)?.let { append(" · Meja $it") }
                                        sale.customerName?.let { append(" · $it") }
                                        sale.queueNumber?.let { append(" · #$it") }
                                        append(" · ${timeFormat.format(Date(sale.updatedAtMillis))}")
                                    }
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    idrFormat.format(sale.subtotal().amount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Items summary
                            val itemNames = sale.lines.take(3).joinToString(", ") {
                                "${it.quantity}x ${it.productRef.name}"
                            }
                            val more = if (sale.lines.size > 3) " +${sale.lines.size - 3} lainnya" else ""
                            Text(
                                itemNames + more,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Kitchen status
                            if (sale.status == SaleStatus.OPEN) {
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Terkirim: $sentCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    if (unsentCount > 0) {
                                        Text(
                                            "Belum: $unsentCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNewOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pesanan Baru")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============================================================
// MENU PANEL
// ============================================================

@Composable
private fun MenuPanel(
    uiState: PosUiState,
    cartQuantities: Map<ProductId, Int> = emptyMap(),
    onCategorySelected: (id.stargan.intikasirfnb.domain.catalog.CategoryId?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onItemClicked: (MenuItem) -> Unit,
    gridMinSize: androidx.compose.ui.unit.Dp = 130.dp,
    cardHeight: androidx.compose.ui.unit.Dp = 140.dp,
    imageHeight: androidx.compose.ui.unit.Dp = 70.dp,
    gridBottomPadding: androidx.compose.ui.unit.Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari menu...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // Category filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("Semua") }
                )
            }
            items(uiState.categories) { category ->
                FilterChip(
                    selected = uiState.selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Menu item grid
        if (uiState.filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tidak ada menu item",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = gridMinSize),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = gridBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredItems, key = { it.id.value }) { item ->
                    MenuItemCard(
                        item = item,
                        cartQty = cartQuantities[item.id] ?: 0,
                        onClick = { onItemClicked(item) },
                        cardHeight = cardHeight,
                        imageHeight = imageHeight
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
    cartQty: Int = 0,
    onClick: () -> Unit,
    cardHeight: androidx.compose.ui.unit.Dp = 140.dp,
    imageHeight: androidx.compose.ui.unit.Dp = 70.dp
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        colors = CardDefaults.cardColors(
            containerColor = if (cartQty > 0)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (item.imageUri != null) {
                AsyncImage(
                    model = item.imageUri,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = idrFormat.format(item.basePrice.amount),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

            // Quantity badge
            if (cartQty > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "$cartQty",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ============================================================
// CART PANEL (with dine-in support)
// ============================================================

@Composable
private fun CartPanel(
    sale: Sale?,
    orderFlow: OrderFlowType = OrderFlowType.PAY_FIRST,
    channelType: ChannelType? = null,
    isSendingToKitchen: Boolean = false,
    openOrders: List<Sale> = emptyList(),
    salesChannels: List<id.stargan.intikasirfnb.domain.transaction.SalesChannel> = emptyList(),
    onIncrement: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit,
    onDecrement: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit,
    onRemove: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit,
    onEditModifiers: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit = {},
    onApplyDiscount: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit = {},
    onClearDiscount: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit = {},
    onClearCart: () -> Unit,
    onPay: (Sale) -> Unit,
    onSendToKitchen: () -> Unit = {},
    onResumeOrder: (SaleId) -> Unit = {},
    onNewOrder: () -> Unit = {},
    onTableChanged: (String?) -> Unit = {},
    onCustomerNameChanged: (String?) -> Unit = {},
    customerSearchResults: List<Customer> = emptyList(),
    onCustomerSearch: (String) -> Unit = {},
    onCustomerSelected: (Customer) -> Unit = {},
    onCustomerCleared: () -> Unit = {},
    hasTables: Boolean = false,
    tables: List<Table> = emptyList(),
    onShowTablePicker: () -> Unit = {},
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val showKitchen = orderFlow == OrderFlowType.PAY_LAST || orderFlow == OrderFlowType.PAY_FLEXIBLE
    val channelMap = remember(salesChannels) { salesChannels.associateBy { it.id } }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        // Order tabs strip — shows active orders as switchable tabs
        // Merge current sale into sorted open orders list (order follows sort mode)
        val allOrders = remember(openOrders, sale) {
            if (sale != null && sale.lines.isNotEmpty() && openOrders.none { it.id == sale.id }) {
                openOrders + sale // already sorted from sortedOpenOrders; new sale appended at end
            } else if (sale != null && sale.lines.isNotEmpty()) {
                // Replace stale entry with fresh currentSale
                openOrders.map { if (it.id == sale.id) sale else it }
            } else {
                openOrders
            }
        }
        if (allOrders.isNotEmpty()) {
            OrderTabStrip(
                orders = allOrders,
                activeSaleId = sale?.id,
                channelMap = channelMap,
                tables = tables,
                onResumeOrder = onResumeOrder,
                onNewOrder = onNewOrder,
                compact = compact
            )
        }

        // Cart header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Show channel + status label
            val channelName = sale?.let { channelMap[it.channelId]?.name }
            Text(
                channelName ?: "Pesanan Baru",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Show sale status badge for OPEN orders
            if (sale?.status == SaleStatus.OPEN) {
                Text(
                    "AKTIF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (sale != null && sale.lines.isNotEmpty() && sale.status == SaleStatus.DRAFT) {
                TextButton(onClick = onClearCart) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Order info bar — table number, customer name, queue number
        if (sale != null && sale.lines.isNotEmpty()) {
            OrderInfoBar(
                sale = sale,
                channelType = channelType,
                orderFlow = orderFlow,
                onTableChanged = onTableChanged,
                onCustomerNameChanged = onCustomerNameChanged,
                customerSearchResults = customerSearchResults,
                onCustomerSearch = onCustomerSearch,
                onCustomerSelected = onCustomerSelected,
                onCustomerCleared = onCustomerCleared,
                hasTables = hasTables,
                tables = tables,
                onShowTablePicker = onShowTablePicker,
                compact = compact
            )
        }

        HorizontalDivider()

        if (sale == null || sale.lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Keranjang kosong",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Ketuk menu untuk menambahkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // Cart items - grouped by kitchen status for dine-in
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // Sent items (dine-in only)
                val sentLines = sale.lines.filter { it.isSentToKitchen }
                val unsentLines = sale.lines.filter { !it.isSentToKitchen }

                if (showKitchen && sentLines.isNotEmpty() && unsentLines.isNotEmpty()) {
                    // Show section headers only when there are both sent and unsent
                    item {
                        Text(
                            "Sudah dikirim ke dapur",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                if (showKitchen && sentLines.isNotEmpty()) {
                    items(sentLines, key = { it.id.value }) { line ->
                        CartLineItem(
                            line = line,
                            isSent = true,
                            onIncrement = { onIncrement(line.id) },
                            onDecrement = { onDecrement(line.id) },
                            onRemove = { onRemove(line.id) },
                            onEditModifiers = { onEditModifiers(line.id) },
                            onApplyDiscount = { onApplyDiscount(line.id) },
                            onClearDiscount = { onClearDiscount(line.id) }
                        )
                    }
                }

                if (showKitchen && sentLines.isNotEmpty() && unsentLines.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Belum dikirim",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                val displayLines = if (showKitchen && sentLines.isNotEmpty()) unsentLines else sale.lines
                if (!(showKitchen && sentLines.isNotEmpty())) {
                    // All lines (no grouping needed)
                    items(displayLines, key = { it.id.value }) { line ->
                        CartLineItem(
                            line = line,
                            isSent = line.isSentToKitchen,
                            onIncrement = { onIncrement(line.id) },
                            onDecrement = { onDecrement(line.id) },
                            onRemove = { onRemove(line.id) },
                            onEditModifiers = { onEditModifiers(line.id) },
                            onApplyDiscount = { onApplyDiscount(line.id) },
                            onClearDiscount = { onClearDiscount(line.id) }
                        )
                    }
                } else {
                    // Unsent lines
                    items(unsentLines, key = { it.id.value }) { line ->
                        CartLineItem(
                            line = line,
                            isSent = false,
                            onIncrement = { onIncrement(line.id) },
                            onDecrement = { onDecrement(line.id) },
                            onRemove = { onRemove(line.id) },
                            onEditModifiers = { onEditModifiers(line.id) },
                            onApplyDiscount = { onApplyDiscount(line.id) },
                            onClearDiscount = { onClearDiscount(line.id) }
                        )
                    }
                }
            }

            // Totals & action buttons
            CartSummary(
                sale = sale,
                orderFlow = orderFlow,
                isSendingToKitchen = isSendingToKitchen,
                hasUnsentItems = sale.unsentLines().isNotEmpty(),
                onPay = { onPay(sale) },
                onSendToKitchen = onSendToKitchen
            )
        }
    }
}

// ============================================================
// ORDER INFO BAR — table, customer name, queue number
// ============================================================

@Composable
private fun OrderInfoBar(
    sale: Sale,
    channelType: ChannelType?,
    orderFlow: OrderFlowType,
    onTableChanged: (String?) -> Unit,
    onCustomerNameChanged: (String?) -> Unit,
    customerSearchResults: List<Customer> = emptyList(),
    onCustomerSearch: (String) -> Unit = {},
    onCustomerSelected: (Customer) -> Unit = {},
    onCustomerCleared: () -> Unit = {},
    hasTables: Boolean = false,
    tables: List<Table> = emptyList(),
    onShowTablePicker: () -> Unit = {},
    compact: Boolean = false
) {
    val canEdit = sale.status == SaleStatus.DRAFT || sale.status == SaleStatus.OPEN
    val isDineIn = channelType == ChannelType.DINE_IN

    // Determine if we have saved values to show as labels
    val hasTable = sale.tableId != null
    val hasName = sale.customerName != null
    val hasInfo = hasTable || hasName

    // Editing mode: start in edit if no info yet, otherwise show label
    var isEditing by remember(sale.id) {
        mutableStateOf(!hasInfo)
    }
    var tableText by remember(sale.id) {
        mutableStateOf(sale.tableId?.value ?: "")
    }
    var nameText by remember(sale.id) {
        mutableStateOf(sale.customerName ?: "")
    }
    var showNameField by remember(sale.id) {
        mutableStateOf(sale.customerName != null)
    }
    // Customer picker: true when linked to a Customer record (not just free-text)
    val hasLinkedCustomer = sale.customerId != null
    var showCustomerDropdown by remember { mutableStateOf(false) }

    // Sync from sale when saved values change externally
    LaunchedEffect(sale.tableId?.value) { tableText = sale.tableId?.value ?: "" }
    LaunchedEffect(sale.customerName) {
        nameText = sale.customerName ?: ""
        if (sale.customerName != null) showNameField = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Queue number display (always visible if assigned)
        val queueNum = sale.queueNumber
        if (queueNum != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    queueNum,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if (!canEdit) {
            // Read-only: show label only
            val tableName = resolveTableName(sale.tableId, tables)
            val label = tableName?.let { "Meja $it" } ?: sale.queueNumber ?: sale.customerName
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isEditing) {
            // --- EDIT MODE ---

            // Table selection (dine-in only)
            if (isDineIn) {
                if (hasTables) {
                    // Visual table picker button
                    OutlinedButton(
                        onClick = onShowTablePicker,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.TableBar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (sale.tableId != null) "Ganti Meja" else "Pilih Meja",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    // Fallback: free text input when no tables configured
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TableBar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = tableText,
                            onValueChange = { tableText = it },
                            placeholder = { Text("No. Meja", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Customer picker (toggle)
            if (showNameField) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = nameText,
                                onValueChange = { text ->
                                    nameText = text
                                    if (text.length >= 2) {
                                        onCustomerSearch(text)
                                        showCustomerDropdown = true
                                    } else {
                                        showCustomerDropdown = false
                                    }
                                },
                                placeholder = { Text("Cari / ketik nama pelanggan", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        IconButton(
                            onClick = {
                                nameText = ""
                                showNameField = false
                                showCustomerDropdown = false
                                onCustomerCleared()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Hapus pelanggan",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    // Dropdown results
                    if (showCustomerDropdown && customerSearchResults.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                customerSearchResults.take(5).forEach { customer ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onCustomerSelected(customer)
                                                nameText = customer.name
                                                showCustomerDropdown = false
                                                isEditing = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                customer.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            customer.phone?.let { phone ->
                                                Text(
                                                    phone,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action row: +Pelanggan toggle & Simpan button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!showNameField) {
                    TextButton(
                        onClick = {
                            showNameField = true
                            onCustomerSearch("") // preload customer list
                        },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pelanggan", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                // Save & close edit mode (for free-text name or table)
                val hasChanges = (isDineIn && !hasTables && tableText.isNotBlank()) || (showNameField && nameText.isNotBlank() && !hasLinkedCustomer)
                if (hasChanges) {
                    TextButton(
                        onClick = {
                            if (isDineIn && !hasTables) onTableChanged(tableText.takeIf { it.isNotBlank() })
                            if (showNameField && !hasLinkedCustomer) onCustomerNameChanged(nameText.takeIf { it.isNotBlank() })
                            showCustomerDropdown = false
                            isEditing = false
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Simpan", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            // --- DISPLAY LABEL MODE --- clickable to re-enter edit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isEditing = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasTable) {
                    Icon(
                        Icons.Default.TableBar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Meja ${resolveTableName(sale.tableId, tables) ?: ""}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (hasName) {
                    if (hasTable) {
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        sale.customerName!!,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Edit hint icon
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit info",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ============================================================
// ORDER TAB STRIP — switchable tabs for active orders
// ============================================================

@Composable
private fun OrderTabStrip(
    orders: List<Sale>,
    activeSaleId: SaleId?,
    channelMap: Map<id.stargan.intikasirfnb.domain.transaction.SalesChannelId, id.stargan.intikasirfnb.domain.transaction.SalesChannel>,
    tables: List<Table> = emptyList(),
    onResumeOrder: (SaleId) -> Unit,
    onNewOrder: () -> Unit,
    compact: Boolean = false
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp)
    ) {
        // All orders in original creation order
        items(orders, key = { "tab_${it.id.value}" }) { order ->
            val isActive = order.id == activeSaleId
            OrderTab(
                sale = order,
                channelName = channelMap[order.channelId]?.code ?: "?",
                tables = tables,
                isActive = isActive,
                onClick = { if (!isActive) onResumeOrder(order.id) }
            )
        }

        // "+" button for new order
        item(key = "new_order") {
            Surface(
                onClick = onNewOrder,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                modifier = Modifier.height(if (compact) 32.dp else 36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Pesanan Baru",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Baru",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderTab(
    sale: Sale,
    channelName: String,
    tables: List<Table> = emptyList(),
    isActive: Boolean,
    onClick: () -> Unit
) {
    val itemCount = sale.lines.sumOf { it.quantity }
    val bgColor = if (isActive)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerLow
    val textColor = if (isActive)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // Channel code + order label (resolve table name)
            val tableName = resolveTableName(sale.tableId, tables)
            val orderLabel = tableName?.let { "Meja $it" } ?: sale.queueNumber ?: sale.customerName
            val tabLabel = if (orderLabel != null) "$channelName $orderLabel" else channelName
            Text(
                tabLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            // Item count
            Text(
                "(${itemCount})",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
            // Status dot for OPEN (sent to kitchen)
            if (sale.status == SaleStatus.OPEN) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(3.dp))
                )
            }
            // Subtotal
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                idrFormat.format(sale.subtotal().amount),
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun CartLineItem(
    line: OrderLine,
    isSent: Boolean = false,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onEditModifiers: () -> Unit = {},
    onApplyDiscount: () -> Unit = {},
    onClearDiscount: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kitchen status indicator for sent items
        if (isSent) {
            Icon(
                Icons.Default.Kitchen,
                contentDescription = "Terkirim",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Item info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.productRef.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSent)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )
            val hasCustomizations = line.selectedModifiers.isNotEmpty() || line.selectedAddOns.isNotEmpty()
            if (hasCustomizations) {
                Column(
                    modifier = if (!isSent) Modifier.clickable { onEditModifiers() } else Modifier
                ) {
                    if (line.selectedModifiers.isNotEmpty()) {
                        Text(
                            text = line.selectedModifiers.joinToString(", ") { it.optionName } +
                                    if (!isSent && line.selectedAddOns.isEmpty()) "  \u270E" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isSent) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    line.selectedAddOns.forEach { addOn ->
                        Text(
                            text = "+ ${addOn.addOnName} x${addOn.quantity} @${idrFormat.format(addOn.unitPrice.amount)}" +
                                    if (!isSent && addOn == line.selectedAddOns.last()) "  \u270E" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isSent) MaterialTheme.colorScheme.tertiary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (line.discountAmount.isPositive()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = idrFormat.format(line.lineTotal().amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "-${idrFormat.format(line.discountAmount.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (!isSent) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hapus diskon",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onClearDiscount() },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    text = idrFormat.format(line.lineTotal().amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Qty controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isSent) {
                // Only show full controls for unsent items
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Kurangi", modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = "${line.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )
            if (!isSent) {
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onApplyDiscount, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Percent, contentDescription = "Diskon", modifier = Modifier.size(16.dp),
                        tint = if (line.discountAmount.isPositive()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun CartSummary(
    sale: Sale,
    orderFlow: OrderFlowType = OrderFlowType.PAY_FIRST,
    isSendingToKitchen: Boolean = false,
    hasUnsentItems: Boolean = false,
    onPay: () -> Unit,
    onSendToKitchen: () -> Unit = {}
) {
    val showKitchen = orderFlow == OrderFlowType.PAY_LAST || orderFlow == OrderFlowType.PAY_FLEXIBLE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp)
            .navigationBarsPadding()
    ) {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))

        val totalItems = sale.lines.sumOf { it.quantity }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$totalItems item",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                idrFormat.format(sale.subtotal().amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showKitchen) {
            // Kitchen flow: Two buttons - "Kirim ke Dapur" + "Bayar"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Send to Kitchen button
                Button(
                    onClick = onSendToKitchen,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = hasUnsentItems && !isSendingToKitchen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    if (isSendingToKitchen) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    } else {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Dapur",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pay button
                Button(
                    onClick = onPay,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = sale.lines.isNotEmpty()
                ) {
                    Text(
                        "BAYAR",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // PAY_FIRST: Single "Bayar" button
            Button(
                onClick = onPay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = sale.lines.isNotEmpty()
            ) {
                Text(
                    "BAYAR",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================
// MODIFIER + ADD-ON SELECTION CONTENT (combined bottom sheet)
// ============================================================

@Composable
private fun ModifierAndAddOnSelectionContent(
    menuItem: MenuItem,
    modifierGroups: List<ModifierGroup>,
    addOnGroups: List<AddOnGroup> = emptyList(),
    existingModifierSelections: List<SelectedModifier> = emptyList(),
    existingAddOnSelections: List<SelectedAddOn> = emptyList(),
    confirmLabel: String = "Tambahkan",
    onConfirm: (List<SelectedModifier>, List<SelectedAddOn>) -> Unit,
    onDismiss: () -> Unit
) {
    // Modifier state: track selected option IDs per group
    val selections = remember(modifierGroups, existingModifierSelections) {
        mutableMapOf<String, MutableList<ModifierOptionId>>().apply {
            modifierGroups.forEach { group ->
                val preselected = mutableListOf<ModifierOptionId>()
                existingModifierSelections.forEach { sel ->
                    if (sel.groupName == group.name) {
                        val option = group.options.find { it.name == sel.optionName }
                        if (option != null) preselected.add(option.id)
                    }
                }
                put(group.id.value, preselected)
            }
        }
    }

    // Add-on state: track qty per add-on item
    val addOnQuantities = remember(addOnGroups, existingAddOnSelections) {
        mutableMapOf<String, Int>().apply {
            // Pre-populate from existing selections
            existingAddOnSelections.forEach { sel ->
                addOnGroups.forEach { group ->
                    group.items.find { it.name == sel.addOnName }?.let { item ->
                        put(item.id.value, sel.quantity)
                    }
                }
            }
        }
    }

    val hasPreselection = existingModifierSelections.isNotEmpty() || existingAddOnSelections.isNotEmpty()
    var selectionVersion by remember { mutableStateOf(if (hasPreselection) 1 else 0) }

    // Validation: check all required modifier groups have minimum selections
    val isValid = remember(selectionVersion) {
        modifierGroups.all { group ->
            val selected = selections[group.id.value]?.size ?: 0
            if (group.isRequired) {
                selected >= group.minSelection.coerceAtLeast(1)
            } else {
                selected >= group.minSelection
            }
        }
    }

    // Price calculations
    val modifierTotal = remember(selectionVersion) {
        var total = Money.zero()
        selections.forEach { (groupIdValue, optionIds) ->
            val group = modifierGroups.find { it.id.value == groupIdValue }
            optionIds.forEach { optionId ->
                val option = group?.options?.find { it.id == optionId }
                if (option != null) total = total + option.priceDelta
            }
        }
        total
    }

    val addOnTotal = remember(selectionVersion) {
        var total = Money.zero()
        addOnQuantities.forEach { (itemId, qty) ->
            if (qty > 0) {
                addOnGroups.forEach { group ->
                    val item = group.items.find { it.id.value == itemId }
                    if (item != null) total = total + (item.price * qty)
                }
            }
        }
        total
    }

    val combinedExtra = modifierTotal + addOnTotal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        // Header: menu item name + price
        Text(
            menuItem.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            idrFormat.format(menuItem.basePrice.amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable modifier + add-on groups
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Modifier groups
            items(modifierGroups, key = { "mod_${it.id.value}" }) { group ->
                val selectedIds = selections[group.id.value] ?: mutableListOf()

                ModifierGroupSection(
                    group = group,
                    isRequired = group.isRequired,
                    minSelection = group.minSelection,
                    maxSelection = group.maxSelection,
                    selectedOptionIds = selectedIds,
                    selectionVersion = selectionVersion,
                    onToggleOption = { optionId ->
                        val list = selections.getOrPut(group.id.value) { mutableListOf() }
                        if (list.contains(optionId)) {
                            list.remove(optionId)
                        } else {
                            if (group.maxSelection == 1) {
                                list.clear()
                                list.add(optionId)
                            } else if (list.size < group.maxSelection) {
                                list.add(optionId)
                            }
                        }
                        selectionVersion++
                    }
                )
            }

            // Add-on groups
            if (addOnGroups.isNotEmpty()) {
                item(key = "addon_divider") {
                    if (modifierGroups.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                items(addOnGroups, key = { "ao_${it.id.value}" }) { group ->
                    AddOnGroupSection(
                        group = group,
                        quantities = addOnQuantities,
                        selectionVersion = selectionVersion,
                        onQuantityChange = { itemId, qty ->
                            addOnQuantities[itemId.value] = qty
                            selectionVersion++
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Price summary
        if (combinedExtra.amount > java.math.BigDecimal.ZERO) {
            if (modifierTotal.amount > java.math.BigDecimal.ZERO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Modifier",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "+${idrFormat.format(modifierTotal.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (addOnTotal.amount > java.math.BigDecimal.ZERO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Add-on",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "+${idrFormat.format(addOnTotal.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    idrFormat.format((menuItem.basePrice + combinedExtra).amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("Batal")
            }
            Button(
                onClick = {
                    // Build SelectedModifier list
                    val modResult = mutableListOf<SelectedModifier>()
                    selections.forEach { (groupIdValue, optionIds) ->
                        val group = modifierGroups.find { it.id.value == groupIdValue } ?: return@forEach
                        optionIds.forEach { optionId ->
                            val option = group.options.find { it.id == optionId } ?: return@forEach
                            modResult.add(
                                SelectedModifier(
                                    groupName = group.name,
                                    optionName = option.name,
                                    priceDelta = option.priceDelta
                                )
                            )
                        }
                    }
                    // Build SelectedAddOn list
                    val aoResult = mutableListOf<SelectedAddOn>()
                    addOnQuantities.forEach { (itemId, qty) ->
                        if (qty > 0) {
                            addOnGroups.forEach { group ->
                                val item = group.items.find { it.id.value == itemId }
                                if (item != null) {
                                    aoResult.add(
                                        SelectedAddOn(
                                            addOnName = item.name,
                                            quantity = qty,
                                            unitPrice = item.price,
                                            totalPrice = item.price * qty
                                        )
                                    )
                                }
                            }
                        }
                    }
                    onConfirm(modResult, aoResult)
                },
                enabled = isValid,
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ============================================================
// ADD-ON SECTION COMPOSABLES
// ============================================================

@Composable
private fun AddOnGroupSection(
    group: AddOnGroup,
    quantities: Map<String, Int>,
    selectionVersion: Int,
    onQuantityChange: (AddOnItemId, Int) -> Unit
) {
    Column {
        // Group header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Add-on",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                "Opsional",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Items with qty stepper
        val activeItems = group.items.filter { it.isActive }.sortedBy { it.sortOrder }
        activeItems.forEach { item ->
            @Suppress("UNUSED_EXPRESSION")
            selectionVersion // read to trigger recomposition
            val qty = quantities[item.id.value] ?: 0
            AddOnItemRow(
                item = item,
                quantity = qty,
                onQuantityChange = { newQty -> onQuantityChange(item.id, newQty) }
            )
        }
    }
}

@Composable
private fun AddOnItemRow(
    item: AddOnItem,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    val isSelected = quantity > 0
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item name
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // Price
            Text(
                idrFormat.format(item.price.amount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Qty stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { if (quantity > 0) onQuantityChange(quantity - 1) },
                    enabled = quantity > 0,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Kurangi",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    "$quantity",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { if (quantity < item.maxQty) onQuantityChange(quantity + 1) },
                    enabled = quantity < item.maxQty,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Tambah",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModifierGroupSection(
    group: ModifierGroup,
    isRequired: Boolean,
    minSelection: Int,
    maxSelection: Int,
    selectedOptionIds: List<ModifierOptionId>,
    selectionVersion: Int,
    onToggleOption: (ModifierOptionId) -> Unit
) {
    Column {
        // Group header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isRequired) {
                    Text(
                        "Wajib",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            // Selection hint
            val hint = when {
                maxSelection == 1 -> "Pilih 1"
                minSelection > 0 -> "Pilih min $minSelection, maks $maxSelection"
                else -> "Pilih maks $maxSelection"
            }
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Options
        val isSingleSelect = maxSelection == 1
        val activeOptions = group.options.filter { it.isActive }.sortedBy { it.sortOrder }
        activeOptions.forEach { option ->
            @Suppress("UNUSED_EXPRESSION")
            selectionVersion // read to trigger recomposition
            val isSelected = selectedOptionIds.contains(option.id)
            ModifierOptionRow(
                option = option,
                isSelected = isSelected,
                isSingleSelect = isSingleSelect,
                onClick = { onToggleOption(option.id) }
            )
        }

        // Show selection count for multi-select
        if (!isSingleSelect) {
            val count = selectedOptionIds.size
            Text(
                "$count / $maxSelection dipilih",
                style = MaterialTheme.typography.labelSmall,
                color = if (count > maxSelection) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ModifierOptionRow(
    option: ModifierOption,
    isSelected: Boolean,
    isSingleSelect: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator: radio for single-select, checkbox for multi-select
            Icon(
                imageVector = if (isSingleSelect) {
                    if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked
                } else {
                    if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                option.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (option.priceDelta.amount > java.math.BigDecimal.ZERO) {
                Text(
                    "+${idrFormat.format(option.priceDelta.amount)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun cartQuantities(sale: Sale?): Map<ProductId, Int> {
    if (sale == null) return emptyMap()
    return sale.lines.groupBy { it.productRef.productId }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
}

/** Resolve tableId to table name from table list; fallback to tableId value */
private fun resolveTableName(
    tableId: id.stargan.intikasirfnb.domain.transaction.TableId?,
    tables: List<Table>
): String? {
    if (tableId == null) return null
    return tables.find { it.id == tableId }?.name ?: tableId.value
}

// ============================================================
// DISCOUNT DIALOG
// ============================================================

@Composable
private fun DiscountDialog(
    productName: String,
    subtotal: id.stargan.intikasirfnb.domain.shared.Money,
    existingInfo: id.stargan.intikasirfnb.domain.transaction.DiscountInfo?,
    onDismiss: () -> Unit,
    onApply: (id.stargan.intikasirfnb.domain.transaction.DiscountInfo) -> Unit,
    onClear: () -> Unit
) {
    var discountType by remember {
        mutableStateOf(existingInfo?.type ?: id.stargan.intikasirfnb.domain.transaction.DiscountType.PERCENT)
    }
    var valueText by remember {
        mutableStateOf(existingInfo?.value?.stripTrailingZeros()?.toPlainString() ?: "")
    }
    var reason by remember { mutableStateOf(existingInfo?.reason ?: "") }

    val valueNum = valueText.toBigDecimalOrNull()
    val isValid = valueNum != null && valueNum > java.math.BigDecimal.ZERO &&
            (discountType != id.stargan.intikasirfnb.domain.transaction.DiscountType.PERCENT || valueNum <= java.math.BigDecimal(100))

    // Preview discount amount
    val previewAmount = if (isValid && valueNum != null) {
        when (discountType) {
            id.stargan.intikasirfnb.domain.transaction.DiscountType.PERCENT ->
                subtotal.amount.multiply(valueNum).divide(java.math.BigDecimal(100), 0, java.math.RoundingMode.HALF_UP)
            id.stargan.intikasirfnb.domain.transaction.DiscountType.FIXED_AMOUNT -> valueNum
        }
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diskon: $productName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Type selector
                Text("Tipe Diskon", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = discountType == id.stargan.intikasirfnb.domain.transaction.DiscountType.PERCENT,
                        onClick = { discountType = id.stargan.intikasirfnb.domain.transaction.DiscountType.PERCENT },
                        label = { Text("Persen (%)") }
                    )
                    FilterChip(
                        selected = discountType == id.stargan.intikasirfnb.domain.transaction.DiscountType.FIXED_AMOUNT,
                        onClick = { discountType = id.stargan.intikasirfnb.domain.transaction.DiscountType.FIXED_AMOUNT },
                        label = { Text("Nominal (Rp)") }
                    )
                }

                // Value input
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { v -> valueText = v.filter { it.isDigit() || it == '.' } },
                    label = {
                        Text(if (discountType == id.stargan.intikasirfnb.domain.transaction.DiscountType.PERCENT) "Persen (%)" else "Nominal (Rp)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = {
                        Text(if (discountType == id.stargan.intikasirfnb.domain.transaction.DiscountType.PERCENT) "%" else "Rp ")
                    }
                )

                // Reason
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan (opsional)") },
                    placeholder = { Text("contoh: Promo, Karyawan, Komplain") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Preview
                if (previewAmount != null) {
                    Text(
                        "Diskon: -Rp ${idrFormat.format(previewAmount).replace(Regex("[^0-9.,]"), "")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid && valueNum != null) {
                        onApply(
                            id.stargan.intikasirfnb.domain.transaction.DiscountInfo(
                                type = discountType,
                                value = valueNum,
                                reason = reason.trim().takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                enabled = isValid
            ) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            Row {
                if (existingInfo != null) {
                    TextButton(onClick = onClear) {
                        Text("Hapus Diskon", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    )
}
