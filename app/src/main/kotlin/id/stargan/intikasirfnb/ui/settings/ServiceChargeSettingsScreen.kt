package id.stargan.intikasirfnb.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.stargan.intikasirfnb.domain.settings.ServiceChargeConfig
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsSwitchItem
import id.stargan.intikasirfnb.ui.settings.components.SettingsTextFieldItem
import id.stargan.intikasirfnb.ui.settings.components.StickyBottomSaveBar
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceChargeSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val outletSettings = uiState.outletSettings ?: return
    val sc = outletSettings.serviceCharge

    var rate by remember(sc) { mutableStateOf(sc.rate.toPlainString()) }
    val hasUnsavedChanges by remember(sc) {
        derivedStateOf { rate != sc.rate.toPlainString() }
    }

    fun saveTextFields() {
        val parsedRate = rate.toBigDecimalOrNull() ?: BigDecimal.ZERO
        viewModel.updateOutletSettings {
            it.copy(serviceCharge = it.serviceCharge.copy(rate = parsedRate))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Charge") },
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
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsSwitchItem(
                    title = "Aktifkan Service Charge",
                    subtitle = "Tambahkan biaya layanan ke setiap transaksi",
                    checked = sc.isEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateOutletSettings {
                            it.copy(serviceCharge = it.serviceCharge.copy(isEnabled = enabled))
                        }
                    }
                )
            }

            if (sc.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsTextFieldItem(
                        label = "Tarif (%)",
                        value = rate,
                        onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = "contoh: 5"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSwitchItem(
                        title = "Sudah termasuk harga",
                        subtitle = "Service charge sudah termasuk dalam harga menu",
                        checked = sc.isIncludedInPrice,
                        onCheckedChange = { included ->
                            viewModel.updateOutletSettings {
                                it.copy(serviceCharge = it.serviceCharge.copy(isIncludedInPrice = included))
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
