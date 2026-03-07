package id.stargan.intikasirfnb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.data.printer.DiscoveredBluetoothDevice
import id.stargan.intikasirfnb.data.printer.LogoBitmapProcessor
import id.stargan.intikasirfnb.data.printer.PairedBluetoothDevice
import id.stargan.intikasirfnb.data.printer.PrinterServiceFactory
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.printer.buildTestReceipt
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.TaxConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfigId
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import id.stargan.intikasirfnb.domain.settings.TerminalSettings
import id.stargan.intikasirfnb.domain.settings.TerminalSettingsRepository
import id.stargan.intikasirfnb.domain.settings.PaperWidth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

enum class PrintStatus { IDLE, PRINTING, SUCCESS, ERROR }

data class SettingsUiState(
    val outletSettings: OutletSettings? = null,
    val terminalSettings: TerminalSettings? = null,
    val taxConfigs: List<TaxConfig> = emptyList(),
    val isLoading: Boolean = true,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val printStatus: PrintStatus = PrintStatus.IDLE,
    val pairedBluetoothDevices: List<PairedBluetoothDevice> = emptyList(),
    val discoveredDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val isBtScanning: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val outletSettingsRepository: OutletSettingsRepository,
    private val terminalSettingsRepository: TerminalSettingsRepository,
    private val taxConfigRepository: TaxConfigRepository,
    private val printerServiceFactory: PrinterServiceFactory,
    private val logoBitmapProcessor: LogoBitmapProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        // Collect discovery state from PrinterServiceFactory
        viewModelScope.launch {
            printerServiceFactory.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(discoveredDevices = devices) }
            }
        }
        viewModelScope.launch {
            printerServiceFactory.isScanning.collect { scanning ->
                _uiState.update { it.copy(isBtScanning = scanning) }
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outlet = sessionManager.getCurrentOutlet()
                val outletId = outlet?.id ?: return@launch
                val tenantId = outlet.tenantId

                val outletSettings = outletSettingsRepository.getByOutletId(outletId)
                    ?: OutletSettings(outletId = outletId, tenantId = tenantId)
                val taxConfigs = taxConfigRepository.getAllByTenant(tenantId)
                val terminalSettings = terminalSettingsRepository.getByTerminalId(
                    id.stargan.intikasirfnb.domain.identity.TerminalId(outletId.value)
                ) ?: TerminalSettings(
                    terminalId = id.stargan.intikasirfnb.domain.identity.TerminalId(outletId.value),
                    outletId = outletId
                )

                _uiState.update {
                    it.copy(
                        outletSettings = outletSettings,
                        terminalSettings = terminalSettings,
                        taxConfigs = taxConfigs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun saveOutletSettings(settings: OutletSettings) {
        viewModelScope.launch {
            try {
                outletSettingsRepository.save(settings)
                _uiState.update { it.copy(outletSettings = settings, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun saveTerminalSettings(settings: TerminalSettings) {
        viewModelScope.launch {
            try {
                terminalSettingsRepository.save(settings)
                _uiState.update { it.copy(terminalSettings = settings, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun saveTaxConfig(taxConfig: TaxConfig) {
        viewModelScope.launch {
            try {
                require(taxConfig.name.isNotBlank()) { "Nama pajak harus diisi" }
                require(taxConfig.rate > BigDecimal.ZERO) { "Tarif pajak harus lebih dari 0" }
                require(taxConfig.rate <= BigDecimal("100")) { "Tarif pajak maksimal 100%" }
                taxConfigRepository.save(taxConfig)
                val updated = taxConfigRepository.getAllByTenant(taxConfig.tenantId)
                _uiState.update { it.copy(taxConfigs = updated, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteTaxConfig(id: TaxConfigId) {
        viewModelScope.launch {
            try {
                taxConfigRepository.delete(id)
                val tenantId = sessionManager.getCurrentOutlet()?.tenantId ?: return@launch
                val updated = taxConfigRepository.getAllByTenant(tenantId)
                _uiState.update { it.copy(taxConfigs = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun updateOutletSettings(transform: (OutletSettings) -> OutletSettings) {
        val current = _uiState.value.outletSettings ?: return
        val updated = transform(current)
        // Optimistic UI update
        _uiState.update { it.copy(outletSettings = updated) }
        viewModelScope.launch {
            try {
                outletSettingsRepository.save(updated)
            } catch (e: Exception) {
                // Revert on failure
                _uiState.update { it.copy(outletSettings = current, errorMessage = "Gagal menyimpan: ${e.message}") }
            }
        }
    }

    fun updateTerminalSettings(transform: (TerminalSettings) -> TerminalSettings) {
        val current = _uiState.value.terminalSettings ?: return
        val updated = transform(current)
        // Optimistic UI update
        _uiState.update { it.copy(terminalSettings = updated) }
        viewModelScope.launch {
            try {
                terminalSettingsRepository.save(updated)
            } catch (e: Exception) {
                // Revert on failure
                _uiState.update { it.copy(terminalSettings = current, errorMessage = "Gagal menyimpan: ${e.message}") }
            }
        }
    }

    fun loadPairedBluetoothDevices() {
        val devices = printerServiceFactory.getPairedBluetoothDevices()
        _uiState.update { it.copy(pairedBluetoothDevices = devices) }
    }

    fun startBluetoothDiscovery() {
        printerServiceFactory.startDiscovery()
    }

    fun stopBluetoothDiscovery() {
        printerServiceFactory.stopDiscovery()
    }

    fun isBluetoothEnabled(): Boolean {
        return printerServiceFactory.isBluetoothEnabled()
    }

    fun testPrint() {
        viewModelScope.launch {
            val terminal = _uiState.value.terminalSettings ?: return@launch
            val outlet = _uiState.value.outletSettings ?: return@launch

            _uiState.update { it.copy(printStatus = PrintStatus.PRINTING, errorMessage = null) }

            val service = printerServiceFactory.create(terminal.printer)
            if (service == null) {
                _uiState.update {
                    it.copy(
                        printStatus = PrintStatus.ERROR,
                        errorMessage = "Printer tidak dikonfigurasi atau alamat belum diisi"
                    )
                }
                return@launch
            }

            // Load cached logo raster if logo exists and header.showLogo is on
            val logoRaster = if (outlet.receipt.header.showLogo) {
                outlet.outletProfile.logoImagePath?.let { path ->
                    logoBitmapProcessor.loadCachedRaster(path)
                }
            } else null

            val receipt = buildTestReceipt(
                outletProfile = outlet.outletProfile,
                printerConfig = terminal.printer,
                receiptConfig = outlet.receipt,
                logoRaster = logoRaster
            )

            service.print(receipt)
                .onSuccess {
                    _uiState.update { it.copy(printStatus = PrintStatus.SUCCESS) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            printStatus = PrintStatus.ERROR,
                            errorMessage = "Gagal cetak: ${e.message}"
                        )
                    }
                }
        }
    }

    fun clearPrintStatus() {
        _uiState.update { it.copy(printStatus = PrintStatus.IDLE) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Pre-convert logo to monochrome raster bitmap for thermal printing.
     * Call this when the user uploads/changes a logo.
     */
    fun cacheLogoBitmap(imagePath: String) {
        viewModelScope.launch {
            val outlet = _uiState.value.outletSettings ?: return@launch
            val paperWidthDots = when (outlet.receipt.paperWidth) {
                PaperWidth.THERMAL_58MM -> 384
                PaperWidth.THERMAL_80MM -> 576
            }
            logoBitmapProcessor.processAndCache(
                imagePath = imagePath,
                logoSize = outlet.receipt.header.logoSize,
                paperWidthDots = paperWidthDots
            )
        }
    }

    /**
     * Delete cached logo raster when logo is removed.
     */
    fun deleteCachedLogoBitmap(imagePath: String) {
        logoBitmapProcessor.deleteCachedRaster(imagePath)
    }
}
