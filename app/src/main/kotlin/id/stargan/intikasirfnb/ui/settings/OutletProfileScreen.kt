package id.stargan.intikasirfnb.ui.settings

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import id.stargan.intikasirfnb.ui.settings.components.SettingsCard
import id.stargan.intikasirfnb.ui.settings.components.SettingsGroupHeader
import id.stargan.intikasirfnb.ui.settings.components.SettingsTextFieldItem
import id.stargan.intikasirfnb.ui.settings.components.StickyBottomSaveBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletProfileScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val outletSettings = uiState.outletSettings ?: return
    val profile = outletSettings.outletProfile
    val context = LocalContext.current

    // Text field local state
    var name by remember(profile) { mutableStateOf(profile.name) }
    var address by remember(profile) { mutableStateOf(profile.address ?: "") }
    var phone by remember(profile) { mutableStateOf(profile.phone ?: "") }
    var npwp by remember(profile) { mutableStateOf(profile.npwp ?: "") }
    var logoPath by remember(profile) { mutableStateOf(profile.logoImagePath) }

    var showImageSourceDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges by remember(profile) {
        derivedStateOf {
            name != profile.name ||
                address != (profile.address ?: "") ||
                phone != (profile.phone ?: "") ||
                npwp != (profile.npwp ?: "") ||
                logoPath != profile.logoImagePath
        }
    }

    fun saveProfile() {
        viewModel.updateOutletSettings {
            it.copy(
                outletProfile = it.outletProfile.copy(
                    name = name,
                    address = address.ifBlank { null },
                    phone = phone.ifBlank { null },
                    npwp = npwp.ifBlank { null },
                    logoImagePath = logoPath
                )
            )
        }
    }

    // Copy picked image to app-internal storage and return local path
    fun copyImageToInternal(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val dir = File(context.filesDir, "logos")
            dir.mkdirs()
            val file = File(dir, "outlet_logo_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> inputStream.copyTo(out) }
            inputStream.close()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = copyImageToInternal(it)
            logoPath = path
            path?.let { p -> viewModel.cacheLogoBitmap(p) }
        }
    }

    // Camera capture
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let {
                val path = copyImageToInternal(it)
                logoPath = path
                path?.let { p -> viewModel.cacheLogoBitmap(p) }
            }
        }
    }

    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "camera_temp")
            dir.mkdirs()
            val file = File(dir, "logo_capture_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            galleryLauncher.launch("image/*")
        }
    }

    fun launchGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun launchCamera() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informasi Toko") },
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
            StickyBottomSaveBar(visible = hasUnsavedChanges, onSave = ::saveProfile)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Logo ---
            SettingsGroupHeader(title = "Logo Toko", modifier = Modifier.padding(top = 8.dp))
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (logoPath != null) {
                        Box {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(logoPath!!))
                                    .build(),
                                contentDescription = "Logo toko",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showImageSourceDialog = true },
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    logoPath?.let { viewModel.deleteCachedLogoBitmap(it) }
                                    logoPath = null
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus logo",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showImageSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tambah Logo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ketuk untuk memilih logo dari galeri atau kamera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Info Toko ---
            SettingsGroupHeader(title = "Identitas Toko")
            SettingsCard {
                SettingsTextFieldItem(
                    label = "Nama Toko",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "contoh: Warung Nasi Padang"
                )
                SettingsTextFieldItem(
                    label = "Alamat",
                    value = address,
                    onValueChange = { address = it },
                    singleLine = false,
                    placeholder = "contoh: Jl. Merdeka No. 1, Jakarta"
                )
                SettingsTextFieldItem(
                    label = "No. Telepon",
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "contoh: 021-1234567"
                )
                SettingsTextFieldItem(
                    label = "NPWP",
                    value = npwp,
                    onValueChange = { npwp = it },
                    placeholder = "contoh: 12.345.678.9-012.345"
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Image source picker dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Pilih Sumber Gambar") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false
                                launchGallery()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Text("Galeri")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false
                                launchCamera()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Text("Kamera")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
