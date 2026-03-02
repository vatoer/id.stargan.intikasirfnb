package id.stargan.intikasirfnb.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.stargan.intikasirfnb.data.PinHasherImpl
import id.stargan.intikasirfnb.domain.identity.Outlet
import id.stargan.intikasirfnb.domain.identity.OutletRepository
import id.stargan.intikasirfnb.domain.identity.PinHasher
import id.stargan.intikasirfnb.domain.identity.SessionManager
import id.stargan.intikasirfnb.domain.identity.TenantRepository
import id.stargan.intikasirfnb.domain.identity.User
import id.stargan.intikasirfnb.domain.identity.UserRepository
import id.stargan.intikasirfnb.domain.usecase.identity.CheckOnboardingNeededUseCase
import id.stargan.intikasirfnb.domain.usecase.identity.CompleteOnboardingUseCase
import id.stargan.intikasirfnb.domain.usecase.identity.LoginWithPinUseCase
import id.stargan.intikasirfnb.domain.usecase.identity.SelectOutletUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePinHasher(): PinHasher = PinHasherImpl()

    @Provides
    @Singleton
    fun provideSessionManager(): SessionManager = object : SessionManager {
        private var currentUser: User? = null
        private var currentOutlet: Outlet? = null

        override fun setCurrentUser(user: User) { currentUser = user }
        override fun setCurrentOutlet(outlet: Outlet) { currentOutlet = outlet }
        override fun getCurrentUser(): User? = currentUser
        override fun getCurrentOutlet(): Outlet? = currentOutlet
        override fun logout() { currentUser = null; currentOutlet = null }
    }

    @Provides
    @Singleton
    fun provideLoginWithPinUseCase(
        userRepository: UserRepository,
        pinHasher: PinHasher,
        sessionManager: SessionManager
    ): LoginWithPinUseCase = LoginWithPinUseCase(userRepository, pinHasher, sessionManager)

    @Provides
    @Singleton
    fun provideSelectOutletUseCase(
        outletRepository: OutletRepository,
        sessionManager: SessionManager
    ): SelectOutletUseCase = SelectOutletUseCase(outletRepository, sessionManager)

    @Provides
    @Singleton
    fun provideCheckOnboardingNeededUseCase(
        tenantRepository: TenantRepository
    ): CheckOnboardingNeededUseCase = CheckOnboardingNeededUseCase(tenantRepository)

    @Provides
    @Singleton
    fun provideCompleteOnboardingUseCase(
        tenantRepository: TenantRepository,
        outletRepository: OutletRepository,
        userRepository: UserRepository,
        pinHasher: PinHasher,
        sessionManager: SessionManager
    ): CompleteOnboardingUseCase = CompleteOnboardingUseCase(
        tenantRepository, outletRepository, userRepository, pinHasher, sessionManager
    )
}
