package id.stargan.intikasirfnb.feature.identity.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.stargan.intikasirfnb.feature.identity.ui.components.NumPad
import id.stargan.intikasirfnb.feature.identity.ui.components.PinDots

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is OnboardingUiState.Success -> onOnboardingComplete()
            is OnboardingUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as OnboardingUiState.Error).message)
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Step indicator
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Langkah $currentStep dari 3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Step content
            AnimatedContent(
                targetState = currentStep,
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    1 -> Step1BusinessInfo(viewModel)
                    2 -> Step2OutletInfo(viewModel)
                    3 -> Step3OwnerSetup(viewModel)
                }
            }
        }
    }
}

@Composable
private fun Step1BusinessInfo(viewModel: OnboardingViewModel) {
    val businessName by viewModel.businessName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.Store,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Selamat Datang!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mari setup bisnis Anda",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = businessName,
            onValueChange = viewModel::onBusinessNameChanged,
            label = { Text("Nama Bisnis") },
            placeholder = { Text("Contoh: Warung Makan Bahagia") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = viewModel::onNextStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = viewModel.isStep1Valid()
        ) {
            Text("Lanjut")
        }
    }
}

@Composable
private fun Step2OutletInfo(viewModel: OnboardingViewModel) {
    val outletName by viewModel.outletName.collectAsState()
    val outletAddress by viewModel.outletAddress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.Storefront,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Outlet Pertama",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Informasi outlet utama Anda",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = outletName,
            onValueChange = viewModel::onOutletNameChanged,
            label = { Text("Nama Outlet") },
            placeholder = { Text("Contoh: Cabang Pusat") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = outletAddress,
            onValueChange = viewModel::onOutletAddressChanged,
            label = { Text("Alamat (opsional)") },
            placeholder = { Text("Contoh: Jl. Raya No. 1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = viewModel::onPreviousStep,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Kembali")
            }
            Button(
                onClick = viewModel::onNextStep,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = viewModel.isStep2Valid()
            ) {
                Text("Lanjut")
            }
        }
    }
}

@Composable
private fun Step3OwnerSetup(viewModel: OnboardingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val ownerName by viewModel.ownerName.collectAsState()
    val ownerEmail by viewModel.ownerEmail.collectAsState()
    val pin by viewModel.pin.collectAsState()
    val confirmPin by viewModel.confirmPin.collectAsState()
    val isEnteringConfirmPin by viewModel.isEnteringConfirmPin.collectAsState()
    val isLoading = uiState is OnboardingUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Buat Akun Owner",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = ownerName,
            onValueChange = viewModel::onOwnerNameChanged,
            label = { Text("Nama") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = ownerEmail,
            onValueChange = viewModel::onOwnerEmailChanged,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN Section
        if (!isEnteringConfirmPin) {
            Text(
                text = "Buat PIN (4-6 digit)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            PinDots(pinLength = pin.length, maxLength = 6)
            Spacer(modifier = Modifier.height(16.dp))
            NumPad(
                onDigit = { digit -> viewModel.onPinChanged(pin + digit) },
                onDelete = { viewModel.onPinDelete() },
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (pin.length >= 4) {
                Button(
                    onClick = viewModel::switchToConfirmPin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Konfirmasi PIN")
                }
            }
        } else {
            Text(
                text = "Konfirmasi PIN",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            PinDots(pinLength = confirmPin.length, maxLength = 6)
            Spacer(modifier = Modifier.height(16.dp))
            NumPad(
                onDigit = { digit -> viewModel.onPinChanged(confirmPin + digit) },
                onDelete = { viewModel.onPinDelete() },
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = viewModel::switchToPin) {
                Text("Ubah PIN")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = viewModel::onPreviousStep,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = !isLoading
            ) {
                Text("Kembali")
            }
            Button(
                onClick = viewModel::onComplete,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = viewModel.isStep3Valid() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Selesai & Mulai")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
