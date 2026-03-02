package id.stargan.intikasirfnb.domain.usecase.identity

import id.stargan.intikasirfnb.domain.identity.Outlet
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.OutletRepository
import id.stargan.intikasirfnb.domain.identity.SessionManager

class SelectOutletUseCase(
    private val outletRepository: OutletRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(outletId: OutletId): Result<Outlet> {
        val currentUser = sessionManager.getCurrentUser()
            ?: return Result.failure(IllegalStateException("Belum login"))

        if (!currentUser.hasAccessToOutlet(outletId)) {
            return Result.failure(IllegalArgumentException("Tidak memiliki akses ke outlet ini"))
        }

        val outlet = outletRepository.getById(outletId)
            ?: return Result.failure(IllegalArgumentException("Outlet tidak ditemukan"))

        sessionManager.setCurrentOutlet(outlet)
        return Result.success(outlet)
    }
}
