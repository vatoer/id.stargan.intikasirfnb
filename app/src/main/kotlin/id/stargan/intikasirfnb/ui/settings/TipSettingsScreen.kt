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
import id.stargan.intikasirfnb.domain.settings.TipConfig
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsGroupHeader
import id.stargan.intikasirfnb.ui.settings.components.SettingsSwitchItem
import id.stargan.intikasirfnb.ui.settings.components.SettingsTextFieldItem
import id.stargan.intikasirfnb.ui.settings.components.StickyBottomSaveBar
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val outletSettings = uiState.outletSettings ?: return
    val tip = outletSettings.tip

    val savedSuggestedText = remember(tip) {
        tip.suggestedPercentages.joinToString(", ") { it.toPlainString() }
    }
    var suggestedText by remember(tip) { mutableStateOf(savedSuggestedText) }
    val hasUnsavedChanges by remember(tip) {
        derivedStateOf { suggestedText != savedSuggestedText }
    }

    fun saveTextFields() {
        val percentages = suggestedText
            .split(",")
            .mapNotNull { it.trim().toBigDecimalOrNull() }
            .ifEmpty { listOf(BigDecimal("5"), BigDecimal("10"), BigDecimal("15")) }
        viewModel.updateOutletSettings {
            it.copy(tip = it.tip.copy(suggestedPercentages = percentages))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Tip") },
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
                    title = "Aktifkan Tip",
                    subtitle = "Tampilkan opsi tip saat pembayaran",
                    checked = tip.isEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateOutletSettings {
                            it.copy(tip = it.tip.copy(isEnabled = enabled))
                        }
                    }
                )
            }

            if (tip.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsGroupHeader(title = "Opsi Tip")
                SettingsCard {
                    SettingsTextFieldItem(
                        label = "Saran persentase",
                        value = suggestedText,
                        onValueChange = { suggestedText = it },
                        placeholder = "contoh: 5, 10, 15"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSwitchItem(
                        title = "Izinkan jumlah custom",
                        subtitle = "Pelanggan bisa memasukkan jumlah tip sendiri",
                        checked = tip.allowCustomAmount,
                        onCheckedChange = { allow ->
                            viewModel.updateOutletSettings {
                                it.copy(tip = it.tip.copy(allowCustomAmount = allow))
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
