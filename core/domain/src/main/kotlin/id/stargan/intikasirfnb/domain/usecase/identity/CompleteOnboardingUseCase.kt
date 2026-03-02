package id.stargan.intikasirfnb.domain.usecase.identity

import id.stargan.intikasirfnb.domain.identity.Outlet
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.OutletRepository
import id.stargan.intikasirfnb.domain.identity.PinHasher
import id.stargan.intikasirfnb.domain.identity.Role
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.Tenant
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TenantRepository
import id.stargan.intikasirfnb.domain.identity.User
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.identity.UserRepository

class CompleteOnboardingUseCase(
    private val tenantRepository: TenantRepository,
    private val outletRepository: OutletRepository,
    private val userRepository: UserRepository,
    private val pinHasher: PinHasher,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        businessName: String,
        outletName: String,
        outletAddress: String?,
        ownerName: String,
        ownerEmail: String,
        pin: String
    ): Result<User> {
        return try {
            val tenantId = TenantId("default")
            val outletId = OutletId.generate()
            val userId = UserId.generate()

            val tenant = Tenant(
                id = tenantId,
                name = businessName
            )
            tenantRepository.save(tenant)

            val outlet = Outlet(
                id = outletId,
                tenantId = tenantId,
                name = outletName,
                address = outletAddress?.takeIf { it.isNotBlank() }
            )
            outletRepository.save(outlet)

            val user = User(
                id = userId,
                tenantId = tenantId,
                email = ownerEmail,
                displayName = ownerName,
                pinHash = pinHasher.hash(pin),
                outletIds = listOf(outletId),
                roles = listOf(Role("owner"))
            )
            userRepository.save(user)

            sessionManager.setCurrentUser(user)
            sessionManager.setCurrentOutlet(outlet)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
