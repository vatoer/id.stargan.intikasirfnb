package id.stargan.intikasirfnb.feature.identity.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.usecase.identity.CheckOnboardingNeededUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashUiState {
    data object Loading : SplashUiState
    data object NeedOnboarding : SplashUiState
    data object HasData : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkOnboardingNeededUseCase: CheckOnboardingNeededUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkOnboarding()
    }

    private fun checkOnboarding() {
        viewModelScope.launch {
            val needsOnboarding = checkOnboardingNeededUseCase()
            _uiState.value = if (needsOnboarding) {
                SplashUiState.NeedOnboarding
            } else {
                SplashUiState.HasData
            }
        }
    }
}
