package id.stargan.intikasirfnb.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.stargan.intikasirfnb.debug.DebugSeeder
import id.stargan.intikasirfnb.feature.identity.navigation.identityNavGraph
import kotlinx.coroutines.launch
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.ui.catalog.CatalogMainScreen
import id.stargan.intikasirfnb.ui.catalog.CatalogViewModel
import id.stargan.intikasirfnb.ui.catalog.CategoryManagementScreen
import id.stargan.intikasirfnb.ui.catalog.MenuItemFormScreen
import id.stargan.intikasirfnb.ui.catalog.MenuItemManagementScreen
import id.stargan.intikasirfnb.ui.catalog.ModifierGroupFormScreen
import id.stargan.intikasirfnb.ui.catalog.ModifierGroupManagementScreen
import id.stargan.intikasirfnb.ui.landing.LandingScreen
import id.stargan.intikasirfnb.ui.pos.CashierSessionScreen
import id.stargan.intikasirfnb.ui.pos.CashierSessionViewModel
import id.stargan.intikasirfnb.ui.pos.PaymentScreen
import id.stargan.intikasirfnb.ui.pos.PaymentViewModel
import id.stargan.intikasirfnb.ui.pos.PosScreen
import id.stargan.intikasirfnb.ui.pos.PosViewModel
import id.stargan.intikasirfnb.ui.settings.OutletProfileScreen
import id.stargan.intikasirfnb.ui.settings.PrinterSettingsScreen
import id.stargan.intikasirfnb.ui.settings.ReceiptSettingsScreen
import id.stargan.intikasirfnb.ui.settings.ServiceChargeSettingsScreen
import id.stargan.intikasirfnb.ui.settings.SettingsMainScreen
import id.stargan.intikasirfnb.ui.settings.SettingsViewModel
import id.stargan.intikasirfnb.ui.settings.TaxSettingsScreen
import id.stargan.intikasirfnb.ui.settings.SalesChannelSettingsScreen
import id.stargan.intikasirfnb.ui.settings.SalesChannelViewModel
import id.stargan.intikasirfnb.ui.history.TransactionHistoryScreen
import id.stargan.intikasirfnb.ui.history.TransactionHistoryViewModel
import id.stargan.intikasirfnb.ui.settlement.SettlementScreen
import id.stargan.intikasirfnb.ui.settlement.SettlementViewModel
import id.stargan.intikasirfnb.ui.settings.TipSettingsScreen
import id.stargan.intikasirfnb.ui.table.TableManagementScreen
import id.stargan.intikasirfnb.ui.table.TableManagementViewModel

object PosRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val OUTLET_PICKER = "outlet_picker"
    const val LANDING = "landing"
    const val SETTINGS = "settings"
    const val SETTINGS_OUTLET_PROFILE = "settings/outlet_profile"
    const val SETTINGS_TAX = "settings/tax"
    const val SETTINGS_SERVICE_CHARGE = "settings/service_charge"
    const val SETTINGS_TIP = "settings/tip"
    const val SETTINGS_RECEIPT = "settings/receipt"
    const val SETTINGS_PRINTER = "settings/printer"
    const val SETTINGS_SALES_CHANNELS = "settings/sales_channels"
    const val SETTINGS_SETTLEMENT = "settings/settlement"
    const val SETTINGS_TABLES = "settings/tables"
    const val CATALOG_GRAPH = "catalog_graph"
    const val CATALOG = "catalog"
    const val CATALOG_CATEGORIES = "catalog/categories"
    const val CATALOG_MENU_ITEMS = "catalog/menu_items"
    const val CATALOG_MENU_ITEM_ADD = "catalog/menu_items/add"
    const val CATALOG_MENU_ITEM_EDIT = "catalog/menu_items/edit/{itemId}"
    const val CATALOG_MODIFIERS = "catalog/modifiers"
    const val TRANSACTION_HISTORY = "transaction_history"
    const val CASHIER_SESSION = "cashier_session"
    const val POS = "pos?saleId={saleId}"
    const val POS_BASE = "pos"
    const val PAYMENT = "pos/payment/{saleId}"
    const val CATALOG_MODIFIER_ADD = "catalog/modifiers/add"
    const val CATALOG_MODIFIER_EDIT = "catalog/modifiers/edit/{groupId}"

    fun pos(saleId: String? = null) = if (saleId != null) "pos?saleId=$saleId" else "pos"
    fun payment(saleId: String) = "pos/payment/$saleId"
    fun menuItemEdit(itemId: String) = "catalog/menu_items/edit/$itemId"
    fun modifierGroupEdit(groupId: String) = "catalog/modifiers/edit/$groupId"
}

@Composable
fun PosNavGraph(debugSeeder: DebugSeeder? = null) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = PosRoutes.SPLASH
    ) {
        identityNavGraph(
            navController = navController,
            onLoginSuccess = {
                navController.navigate(PosRoutes.OUTLET_PICKER) {
                    popUpTo(PosRoutes.LOGIN) { inclusive = true }
                }
            },
            onOutletSelected = {
                navController.navigate(PosRoutes.LANDING) {
                    popUpTo(PosRoutes.OUTLET_PICKER) { inclusive = true }
                }
            },
            onOnboardingComplete = {
                navController.navigate(PosRoutes.LANDING) {
                    popUpTo(PosRoutes.ONBOARDING) { inclusive = true }
                }
            },
            onDebugSeed = if (debugSeeder != null) {
                {
                    scope.launch {
                        debugSeeder.seed()
                        navController.navigate(PosRoutes.LANDING) {
                            popUpTo(PosRoutes.SPLASH) { inclusive = true }
                        }
                    }
                }
            } else null
        )

        composable(PosRoutes.LANDING) {
            LandingScreen(
                onNavigateToPos = {
                    navController.navigate(PosRoutes.CASHIER_SESSION)
                },
                onNavigateToSettings = {
                    navController.navigate(PosRoutes.SETTINGS)
                },
                onNavigateToCatalog = {
                    navController.navigate(PosRoutes.CATALOG_GRAPH)
                },
                onNavigateToHistory = {
                    navController.navigate(PosRoutes.TRANSACTION_HISTORY)
                },
                onLogout = {
                    navController.navigate(PosRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Transaction History ---
        composable(PosRoutes.TRANSACTION_HISTORY) {
            val historyViewModel = hiltViewModel<TransactionHistoryViewModel>()
            TransactionHistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() },
                onResumeDraft = { saleId ->
                    navController.navigate(PosRoutes.pos(saleId))
                },
                onResumePayment = { saleId ->
                    navController.navigate(PosRoutes.payment(saleId))
                }
            )
        }

        // --- Cashier Session ---
        composable(PosRoutes.CASHIER_SESSION) {
            val sessionViewModel = hiltViewModel<CashierSessionViewModel>()
            CashierSessionScreen(
                viewModel = sessionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPos = {
                    navController.navigate(PosRoutes.POS_BASE)
                }
            )
        }

        // --- POS ---
        composable(
            PosRoutes.POS,
            arguments = listOf(navArgument("saleId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            val posViewModel = hiltViewModel<PosViewModel>()
            PosScreen(
                viewModel = posViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPayment = { saleId ->
                    navController.navigate(PosRoutes.payment(saleId))
                }
            )
        }

        composable(
            PosRoutes.PAYMENT,
            arguments = listOf(navArgument("saleId") { type = NavType.StringType })
        ) {
            val paymentViewModel = hiltViewModel<PaymentViewModel>()
            PaymentScreen(
                viewModel = paymentViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNewTransaction = {
                    // Pop back to POS and start fresh
                    navController.popBackStack(PosRoutes.POS, inclusive = true)
                    navController.navigate(PosRoutes.POS_BASE)
                }
            )
        }

        composable(PosRoutes.SETTINGS) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            SettingsMainScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOutletProfile = { navController.navigate(PosRoutes.SETTINGS_OUTLET_PROFILE) },
                onNavigateToTax = { navController.navigate(PosRoutes.SETTINGS_TAX) },
                onNavigateToServiceCharge = { navController.navigate(PosRoutes.SETTINGS_SERVICE_CHARGE) },
                onNavigateToTip = { navController.navigate(PosRoutes.SETTINGS_TIP) },
                onNavigateToReceipt = { navController.navigate(PosRoutes.SETTINGS_RECEIPT) },
                onNavigateToPrinter = { navController.navigate(PosRoutes.SETTINGS_PRINTER) },
                onNavigateToSalesChannels = { navController.navigate(PosRoutes.SETTINGS_SALES_CHANNELS) },
                onNavigateToSettlement = { navController.navigate(PosRoutes.SETTINGS_SETTLEMENT) },
                onNavigateToTables = { navController.navigate(PosRoutes.SETTINGS_TABLES) }
            )
        }

        composable(PosRoutes.SETTINGS_OUTLET_PROFILE) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            OutletProfileScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosRoutes.SETTINGS_TAX) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            TaxSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosRoutes.SETTINGS_SERVICE_CHARGE) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            ServiceChargeSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosRoutes.SETTINGS_TIP) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            TipSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosRoutes.SETTINGS_RECEIPT) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            ReceiptSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosRoutes.SETTINGS_PRINTER) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            PrinterSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosRoutes.SETTINGS_SALES_CHANNELS) {
            val salesChannelViewModel = hiltViewModel<SalesChannelViewModel>()
            SalesChannelSettingsScreen(
                viewModel = salesChannelViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosRoutes.SETTINGS_SETTLEMENT) {
            val settlementViewModel = hiltViewModel<SettlementViewModel>()
            SettlementScreen(
                viewModel = settlementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosRoutes.SETTINGS_TABLES) {
            val tableViewModel = hiltViewModel<TableManagementViewModel>()
            TableManagementScreen(
                viewModel = tableViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- Catalog (nested nav graph — shared ViewModel) ---
        navigation(
            startDestination = PosRoutes.CATALOG,
            route = PosRoutes.CATALOG_GRAPH
        ) {
            composable(PosRoutes.CATALOG) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(PosRoutes.CATALOG_GRAPH) }
                val catalogViewModel = hiltViewModel<CatalogViewModel>(parentEntry)
                CatalogMainScreen(
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategories = { navController.navigate(PosRoutes.CATALOG_CATEGORIES) },
                    onNavigateToMenuItems = { navController.navigate(PosRoutes.CATALOG_MENU_ITEMS) },
                    onNavigateToModifiers = { navController.navigate(PosRoutes.CATALOG_MODIFIERS) }
                )
            }

            composable(PosRoutes.CATALOG_CATEGORIES) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(PosRoutes.CATALOG_GRAPH) }
                val catalogViewModel = hiltViewModel<CatalogViewModel>(parentEntry)
                CategoryManagementScreen(
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(PosRoutes.CATALOG_MENU_ITEMS) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(PosRoutes.CATALOG_GRAPH) }
                val catalogViewModel = hiltViewModel<CatalogViewModel>(parentEntry)
                MenuItemManagementScreen(
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddItem = { navController.navigate(PosRoutes.CATALOG_MENU_ITEM_ADD) },
                    onNavigateToEditItem = { productId ->
                        navController.navigate(PosRoutes.menuItemEdit(productId.value))
                    }
                )
            }

            composable(PosRoutes.CATALOG_MENU_ITEM_ADD) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(PosRoutes.CATALOG_GRAPH) }
                val catalogViewModel = hiltViewModel<CatalogViewModel>(parentEntry)
                MenuItemFormScreen(
                    viewModel = catalogViewModel,
                    editItemId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                PosRoutes.CATALOG_MENU_ITEM_EDIT,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(PosRoutes.CATALOG_GRAPH) }
                val catalogViewModel = hiltViewModel<CatalogViewModel>(parentEntry)
                MenuItemFormScreen(
                    viewModel = catalogViewModel,
                    editItemId = ProductId(itemId),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // --- Modifier Groups ---
            composable(PosRoutes.CATALOG_MODIFIERS) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(PosRoutes.CATALOG_GRAPH) }
                val catalogViewModel = hiltViewModel<CatalogViewModel>(parentEntry)
                ModifierGroupManagementScreen(
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddGroup = { navController.navigate(PosRoutes.CATALOG_MODIFIER_ADD) },
                    onNavigateToEditGroup = { groupId ->
                        navController.navigate(PosRoutes.modifierGroupEdit(groupId.value))
                    }
                )
            }

            composable(PosRoutes.CATALOG_MODIFIER_ADD) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(PosRoutes.CATALOG_GRAPH) }
                val catalogViewModel = hiltViewModel<CatalogViewModel>(parentEntry)
                ModifierGroupFormScreen(
                    viewModel = catalogViewModel,
                    editGroupId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                PosRoutes.CATALOG_MODIFIER_EDIT,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(PosRoutes.CATALOG_GRAPH) }
                val catalogViewModel = hiltViewModel<CatalogViewModel>(parentEntry)
                ModifierGroupFormScreen(
                    viewModel = catalogViewModel,
                    editGroupId = ModifierGroupId(groupId),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
