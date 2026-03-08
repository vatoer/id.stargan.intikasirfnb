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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.Sale
import kotlinx.coroutines.launch
import java.text.NumberFormat
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
                onChannelSelected = viewModel::selectChannel
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
                    onIncrement = viewModel::incrementLine,
                    onDecrement = viewModel::decrementLine,
                    onRemove = viewModel::removeLine,
                    onClearCart = viewModel::clearCart,
                    onPay = { sale -> onNavigateToPayment(sale.id.value) },
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
    val cartSubtotal = uiState.currentSale?.subtotal()

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
                onChannelSelected = viewModel::selectChannel
            )
        },
        sheetPeekHeight = if (cartItemCount > 0) 80.dp else 0.dp,
        sheetContent = {
            // Peek bar: summary + tap to expand
            if (cartItemCount > 0) {
                CartPanel(
                    sale = uiState.currentSale,
                    onIncrement = viewModel::incrementLine,
                    onDecrement = viewModel::decrementLine,
                    onRemove = viewModel::removeLine,
                    onClearCart = viewModel::clearCart,
                    onPay = { sale -> onNavigateToPayment(sale.id.value) },
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
    onChannelSelected: (id.stargan.intikasirfnb.domain.transaction.SalesChannel) -> Unit
) {
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

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

@Composable
private fun CartPanel(
    sale: Sale?,
    onIncrement: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit,
    onDecrement: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit,
    onRemove: (id.stargan.intikasirfnb.domain.transaction.OrderLineId) -> Unit,
    onClearCart: () -> Unit,
    onPay: (Sale) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        // Cart header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Keranjang",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (sale != null && sale.lines.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            }
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
            // Cart items
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(sale.lines, key = { it.id.value }) { line ->
                    CartLineItem(
                        line = line,
                        onIncrement = { onIncrement(line.id) },
                        onDecrement = { onDecrement(line.id) },
                        onRemove = { onRemove(line.id) }
                    )
                }
            }

            // Totals & pay button
            CartSummary(sale = sale, onPay = { onPay(sale) })
        }
    }
}

@Composable
private fun CartLineItem(
    line: OrderLine,
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
        // Item info
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
            IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Kurangi", modifier = Modifier.size(16.dp))
            }
            Text(
                text = "${line.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )
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
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun CartSummary(
    sale: Sale,
    onPay: () -> Unit
) {
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

private fun cartQuantities(sale: Sale?): Map<ProductId, Int> {
    if (sale == null) return emptyMap()
    return sale.lines.groupBy { it.productRef.productId }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
}
