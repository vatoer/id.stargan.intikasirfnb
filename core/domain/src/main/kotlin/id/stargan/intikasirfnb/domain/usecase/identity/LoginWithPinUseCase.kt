package id.stargan.intikasirfnb.domain.usecase.identity

import id.stargan.intikasirfnb.domain.identity.PinHasher
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.User
import id.stargan.intikasirfnb.domain.identity.UserRepository

class LoginWithPinUseCase(
    private val userRepository: UserRepository,
    private val pinHasher: PinHasher,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(tenantId: TenantId, pin: String): Result<User> {
        val users = userRepository.listByTenant(tenantId)
        val matchedUser = users.firstOrNull { user ->
            user.isActive && user.pinHash.isNotBlank() && pinHasher.verify(pin, user.pinHash)
        } ?: return Result.failure(IllegalArgumentException("PIN tidak valid"))

        sessionManager.setCurrentUser(matchedUser)
        return Result.success(matchedUser)
    }
}
