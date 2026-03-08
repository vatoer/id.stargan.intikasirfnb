package id.stargan.intikasirfnb.ui.landing

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.stargan.intikasirfnb.domain.identity.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class LandingUiState(
    val userName: String = "",
    val outletName: String = "",
    val menuItems: List<LandingMenuItem> = LandingMenuItem.defaults()
)

data class LandingMenuItem(
    val id: String,
    val label: String,
    val iconName: String
) {
    companion object {
        fun defaults() = listOf(
            LandingMenuItem("pos", "POS / Kasir", "point_of_sale"),
            LandingMenuItem("catalog", "Katalog Menu", "restaurant_menu"),
            LandingMenuItem("customer", "Pelanggan", "people"),
            LandingMenuItem("report", "Riwayat Transaksi", "receipt_long"),
            LandingMenuItem("table", "Meja", "table_restaurant"),
            LandingMenuItem("settings", "Pengaturan", "settings")
        )
    }
}

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LandingUiState(
            userName = sessionManager.getCurrentUser()?.displayName ?: "",
            outletName = sessionManager.getCurrentOutlet()?.name ?: ""
        )
    )
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    fun logout() {
        sessionManager.logout()
    }
}
