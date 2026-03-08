package id.stargan.intikasirfnb.ui.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableBar
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.transaction.ChannelType
import id.stargan.intikasirfnb.domain.transaction.OrderFlowType
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idrFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
}

// Breakpoint: screens wider than 600dp use split-panel (tablet), narrower use bottom sheet (phone)
private val TABLET_BREAKPOINT = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (saleId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
            openOrders = uiState.openOrders,
            salesChannels = uiState.salesChannels,
            onDismiss = { viewModel.toggleOpenOrders() },
            onResumeOrder = { saleId -> viewModel.resumeOpenOrder(saleId) },
            onNewOrder = { viewModel.newOrder() }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= TABLET_BREAKPOINT

        if (isTablet) {
            TabletPosLayout(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onNavigateToPayment = onNavigateToPayment
            )
        } else {
            PhonePosLayout(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onNavigateToPayment = onNavigateToPayment
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
    onNavigateToPayment: (saleId: String) -> Unit
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
                openOrdersCount = uiState.openOrders.size
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
                    openOrders = uiState.openOrders,
                    salesChannels = uiState.salesChannels,
                    onIncrement = viewModel::incrementLine,
                    onDecrement = viewModel::decrementLine,
                    onRemove = viewModel::removeLine,
                    onClearCart = viewModel::clearCart,
                    onPay = { sale -> onNavigateToPayment(sale.id.value) },
                    onSendToKitchen = viewModel::sendToKitchen,
                    onResumeOrder = viewModel::resumeOpenOrder,
                    onNewOrder = viewModel::newOrder,
                    onTableChanged = viewModel::setTableNumber,
                    onCustomerNameChanged = viewModel::setCustomerName,
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
    onNavigateToPayment: (saleId: String) -> Unit
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
                openOrdersCount = uiState.openOrders.size
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
                    openOrders = uiState.openOrders,
                    salesChannels = uiState.salesChannels,
                    onIncrement = viewModel::incrementLine,
                    onDecrement = viewModel::decrementLine,
                    onRemove = viewModel::removeLine,
                    onClearCart = viewModel::clearCart,
                    onPay = { sale -> onNavigateToPayment(sale.id.value) },
                    onSendToKitchen = viewModel::sendToKitchen,
                    onResumeOrder = viewModel::resumeOpenOrder,
                    onNewOrder = viewModel::newOrder,
                    onTableChanged = viewModel::setTableNumber,
                    onCustomerNameChanged = viewModel::setCustomerName,
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
    openOrdersCount: Int
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
                uiState.salesChannels.forEach { channel ->
                    val selected = uiState.selectedChannel?.id == channel.id
                    FilterChip(
                        selected = selected,
                        onClick = { onChannelSelected(channel) },
                        label = { Text(channel.name, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
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
                            Icon(Icons.Default.ListAlt, contentDescription = "Pesanan Aktif")
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Order flow override chips — show when channel is PAY_FLEXIBLE and no active sale
        val channel = uiState.selectedChannel
        val canOverride = channel?.defaultOrderFlow == OrderFlowType.PAY_FLEXIBLE
            && uiState.currentSale == null
        if (canOverride) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
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
// OPEN ORDERS SHEET
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenOrdersSheet(
    openOrders: List<Sale>,
    salesChannels: List<id.stargan.intikasirfnb.domain.transaction.SalesChannel>,
    onDismiss: () -> Unit,
    onResumeOrder: (SaleId) -> Unit,
    onNewOrder: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale("id")) }
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

            Spacer(modifier = Modifier.height(12.dp))

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
                                    val orderLabel = sale.orderLabel()
                                    val subtitle = buildString {
                                        append(channelName)
                                        if (orderLabel != null) append(" - $orderLabel")
                                        append(" - ${timeFormat.format(Date(sale.updatedAtMillis))}")
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
    onClearCart: () -> Unit,
    onPay: (Sale) -> Unit,
    onSendToKitchen: () -> Unit = {},
    onResumeOrder: (SaleId) -> Unit = {},
    onNewOrder: () -> Unit = {},
    onTableChanged: (String?) -> Unit = {},
    onCustomerNameChanged: (String?) -> Unit = {},
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val showKitchen = orderFlow == OrderFlowType.PAY_LAST || orderFlow == OrderFlowType.PAY_FLEXIBLE
    val channelMap = remember(salesChannels) { salesChannels.associateBy { it.id } }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        // Order tabs strip — shows active orders as switchable tabs
        val otherOrders = openOrders.filter { it.id != sale?.id }
        if (otherOrders.isNotEmpty() || (sale != null && sale.lines.isNotEmpty())) {
            OrderTabStrip(
                currentSale = sale,
                otherOrders = otherOrders,
                channelMap = channelMap,
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
                            onRemove = { onRemove(line.id) }
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
                            onRemove = { onRemove(line.id) }
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
                            onRemove = { onRemove(line.id) }
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
    compact: Boolean = false
) {
    val canEdit = sale.status == SaleStatus.DRAFT || sale.status == SaleStatus.OPEN
    val isDineIn = channelType == ChannelType.DINE_IN
    var tableText by remember(sale.id) {
        mutableStateOf(sale.tableId?.value ?: "")
    }
    var nameText by remember(sale.id) {
        mutableStateOf(sale.customerName ?: "")
    }
    var showNameField by remember(sale.id) {
        mutableStateOf(sale.customerName != null)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Table number (dine-in) or Queue number (counter/takeaway)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDineIn) {
                // Table number input
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
                    enabled = canEdit,
                    textStyle = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                // Save button for table
                if (canEdit && tableText != (sale.tableId?.value ?: "")) {
                    TextButton(
                        onClick = {
                            onTableChanged(tableText.takeIf { it.isNotBlank() })
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("OK", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Queue number display (if assigned)
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

            // If not dine-in and no fields shown, show "Meja bebas" hint
            if (!isDineIn && queueNum == null) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Toggle customer name field
            if (!showNameField && canEdit) {
                TextButton(
                    onClick = { showNameField = true },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nama", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Row 2: Customer name (expandable)
        if (showNameField) {
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
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    placeholder = { Text("Nama pelanggan", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    enabled = canEdit,
                    textStyle = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                if (canEdit && nameText != (sale.customerName ?: "")) {
                    TextButton(
                        onClick = {
                            onCustomerNameChanged(nameText.takeIf { it.isNotBlank() })
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("OK", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (canEdit) {
                    IconButton(
                        onClick = {
                            nameText = ""
                            showNameField = false
                            onCustomerNameChanged(null)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hapus nama",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Read-only display when not editable
        if (!canEdit) {
            val label = sale.orderLabel()
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    currentSale: Sale?,
    otherOrders: List<Sale>,
    channelMap: Map<id.stargan.intikasirfnb.domain.transaction.SalesChannelId, id.stargan.intikasirfnb.domain.transaction.SalesChannel>,
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
        // Current sale tab (highlighted)
        if (currentSale != null && currentSale.lines.isNotEmpty()) {
            item(key = "current_${currentSale.id.value}") {
                OrderTab(
                    sale = currentSale,
                    channelName = channelMap[currentSale.channelId]?.code ?: "?",
                    isActive = true,
                    onClick = { /* already active */ }
                )
            }
        }

        // Other open orders as tabs
        items(otherOrders, key = { "tab_${it.id.value}" }) { order ->
            OrderTab(
                sale = order,
                channelName = channelMap[order.channelId]?.code ?: "?",
                isActive = false,
                onClick = { onResumeOrder(order.id) }
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
            // Channel code + order label
            val orderLabel = sale.orderLabel()
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
    onRemove: () -> Unit
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
            if (line.selectedModifiers.isNotEmpty()) {
                Text(
                    text = line.selectedModifiers.joinToString(", ") { it.optionName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = idrFormat.format(line.lineTotal().amount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
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

private fun cartQuantities(sale: Sale?): Map<ProductId, Int> {
    if (sale == null) return emptyMap()
    return sale.lines.groupBy { it.productRef.productId }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
}
