package id.stargan.intikasirfnb.domain.usecase.identity

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.User
import id.stargan.intikasirfnb.domain.identity.UserRepository

class GetUserByEmailUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(tenantId: TenantId, email: String): User? =
        userRepository.getByEmail(tenantId, email)
}
