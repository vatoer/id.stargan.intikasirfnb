package id.stargan.intikasirfnb.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import id.stargan.intikasirfnb.domain.printer.PrinterService
import id.stargan.intikasirfnb.domain.settings.PrinterConfig
import id.stargan.intikasirfnb.domain.settings.PrinterConnectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PairedBluetoothDevice(
    val name: String,
    val address: String
)

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean
)

@Singleton
class PrinterServiceFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _discoveredDevices.asStateFlow()

    private var receiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    @Suppress("DEPRECATION")
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return
                    val deviceName = try { device.name } catch (_: SecurityException) { null }
                    val deviceAddress = device.address ?: return

                    val pairedAddresses = getPairedBluetoothDevices().map { it.address }.toSet()
                    val discovered = DiscoveredBluetoothDevice(
                        name = deviceName ?: "Tidak diketahui",
                        address = deviceAddress,
                        isPaired = deviceAddress in pairedAddresses
                    )

                    _discoveredDevices.value = (_discoveredDevices.value + discovered)
                        .distinctBy { it.address }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    unregisterReceiver()
                }
            }
        }
    }

    fun create(config: PrinterConfig): PrinterService? {
        return when (config.connectionType) {
            PrinterConnectionType.BLUETOOTH -> {
                val adapter = getBluetoothAdapter()
                config.address?.let { BluetoothPrinterService(adapter, it) }
            }
            PrinterConnectionType.NETWORK -> {
                config.address?.let { addr ->
                    val parts = addr.split(":")
                    val host = parts[0]
                    val port = parts.getOrNull(1)?.toIntOrNull() ?: 9100
                    NetworkPrinterService(host, port)
                }
            }
            PrinterConnectionType.USB -> null
            PrinterConnectionType.NONE -> null
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<PairedBluetoothDevice> {
        val adapter = getBluetoothAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return try {
            adapter.bondedDevices
                ?.map { PairedBluetoothDevice(it.name ?: "Tidak diketahui", it.address) }
                ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val adapter = getBluetoothAdapter() ?: return
        if (!adapter.isEnabled) return

        // Stop any existing discovery
        try { adapter.cancelDiscovery() } catch (_: SecurityException) {}

        // Clear previous results
        _discoveredDevices.value = emptyList()

        // Register receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        if (!receiverRegistered) {
            context.registerReceiver(discoveryReceiver, filter)
            receiverRegistered = true
        }

        // Start discovery
        try {
            adapter.startDiscovery()
        } catch (_: SecurityException) {
            _isScanning.value = false
            unregisterReceiver()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            getBluetoothAdapter()?.cancelDiscovery()
        } catch (_: SecurityException) {}
        _isScanning.value = false
        unregisterReceiver()
    }

    fun isBluetoothEnabled(): Boolean {
        return getBluetoothAdapter()?.isEnabled == true
    }

    private fun unregisterReceiver() {
        if (receiverRegistered) {
            try { context.unregisterReceiver(discoveryReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    private fun getBluetoothAdapter() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
}
