package id.stargan.intikasirfnb.ui.catalog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Tune
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
import id.stargan.intikasirfnb.ui.settings.components.SettingsDivider
import id.stargan.intikasirfnb.ui.settings.components.SettingsNavigationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogMainScreen(
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToMenuItems: () -> Unit,
    onNavigateToModifiers: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Katalog Menu") },
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
            item {
                SettingsCard(modifier = Modifier.padding(top = 12.dp)) {
                    SettingsNavigationItem(
                        icon = Icons.Default.Folder,
                        title = "Kategori",
                        subtitle = categorySummary(uiState),
                        onClick = onNavigateToCategories
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.RestaurantMenu,
                        title = "Menu Item",
                        subtitle = menuItemSummary(uiState),
                        onClick = onNavigateToMenuItems
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Tune,
                        title = "Modifier Group",
                        subtitle = modifierSummary(uiState),
                        onClick = onNavigateToModifiers
                    )
                }
            }
        }
    }
}

private fun categorySummary(state: CatalogUiState): String {
    val active = state.categories.count { it.isActive }
    val total = state.categories.size
    return if (total == 0) "Belum ada kategori" else "$active aktif dari $total kategori"
}

private fun menuItemSummary(state: CatalogUiState): String {
    val active = state.menuItems.count { it.isActive }
    val total = state.menuItems.size
    return if (total == 0) "Belum ada menu" else "$active aktif dari $total item"
}

private fun modifierSummary(state: CatalogUiState): String {
    val total = state.modifierGroups.size
    return if (total == 0) "Belum ada modifier" else "$total group"
}
