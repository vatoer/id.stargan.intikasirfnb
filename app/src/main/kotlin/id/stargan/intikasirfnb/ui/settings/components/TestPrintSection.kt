package id.stargan.intikasirfnb.ui.settings.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import id.stargan.intikasirfnb.domain.settings.PrinterConnectionType
import id.stargan.intikasirfnb.ui.settings.PrintStatus

@Composable
fun TestPrintSection(
    printStatus: PrintStatus,
    connectionType: PrinterConnectionType,
    printerName: String?,
    printerAddress: String?,
    onTestPrint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onTestPrint()
    }

    fun handlePrint() {
        if (connectionType == PrinterConnectionType.BLUETOOTH
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }
        onTestPrint()
    }

    SettingsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Connection info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = when (connectionType) {
                        PrinterConnectionType.BLUETOOTH -> Icons.Default.Bluetooth
                        PrinterConnectionType.NETWORK -> Icons.Default.Wifi
                        else -> Icons.Default.Print
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (connectionType != PrinterConnectionType.NONE)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = printerName ?: "Printer belum dikonfigurasi",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (printerAddress != null && connectionType != PrinterConnectionType.NONE) {
                        Text(
                            text = printerAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            // Test print button
            Button(
                onClick = ::handlePrint,
                enabled = printStatus != PrintStatus.PRINTING
                    && connectionType != PrinterConnectionType.NONE
                    && printerAddress != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (printStatus) {
                    PrintStatus.PRINTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mencetak...")
                    }
                    else -> {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tes Cetak")
                    }
                }
            }

            // Status feedback
            when (printStatus) {
                PrintStatus.SUCCESS -> {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Tes cetak berhasil!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                PrintStatus.ERROR -> {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Gagal mencetak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {}
            }

            if (connectionType == PrinterConnectionType.NONE) {
                Text(
                    text = "Atur koneksi printer di Pengaturan > Printer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
