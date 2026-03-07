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
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository
import id.stargan.intikasirfnb.domain.usecase.transaction.AddLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.AddPaymentUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CloseCashierSessionUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CompleteSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.ConfirmSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CreateSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetCurrentCashierSessionUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSaleByIdUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSalesChannelsUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.OpenCashierSessionUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.RemoveLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.UpdateLineItemUseCase
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
        sessionManager: SessionManager,
        salesChannelRepository: SalesChannelRepository
    ): CompleteOnboardingUseCase = CompleteOnboardingUseCase(
        tenantRepository, outletRepository, userRepository, pinHasher, sessionManager, salesChannelRepository
    )

    // --- Transaction use cases ---

    @Provides
    @Singleton
    fun provideGetSalesChannelsUseCase(
        salesChannelRepository: SalesChannelRepository
    ): GetSalesChannelsUseCase = GetSalesChannelsUseCase(salesChannelRepository)

    @Provides
    @Singleton
    fun provideCreateSaleUseCase(
        saleRepository: SaleRepository,
        salesChannelRepository: SalesChannelRepository
    ): CreateSaleUseCase = CreateSaleUseCase(saleRepository, salesChannelRepository)

    @Provides
    @Singleton
    fun provideAddLineItemUseCase(
        saleRepository: SaleRepository
    ): AddLineItemUseCase = AddLineItemUseCase(saleRepository)

    @Provides
    @Singleton
    fun provideUpdateLineItemUseCase(
        saleRepository: SaleRepository
    ): UpdateLineItemUseCase = UpdateLineItemUseCase(saleRepository)

    @Provides
    @Singleton
    fun provideRemoveLineItemUseCase(
        saleRepository: SaleRepository
    ): RemoveLineItemUseCase = RemoveLineItemUseCase(saleRepository)

    // --- Payment use cases ---

    @Provides
    @Singleton
    fun provideGetSaleByIdUseCase(
        saleRepository: SaleRepository
    ): GetSaleByIdUseCase = GetSaleByIdUseCase(saleRepository)

    @Provides
    @Singleton
    fun provideConfirmSaleUseCase(
        saleRepository: SaleRepository,
        taxConfigRepository: TaxConfigRepository,
        outletSettingsRepository: OutletSettingsRepository
    ): ConfirmSaleUseCase = ConfirmSaleUseCase(saleRepository, taxConfigRepository, outletSettingsRepository)

    @Provides
    @Singleton
    fun provideAddPaymentUseCase(
        saleRepository: SaleRepository
    ): AddPaymentUseCase = AddPaymentUseCase(saleRepository)

    @Provides
    @Singleton
    fun provideCompleteSaleUseCase(
        saleRepository: SaleRepository
    ): CompleteSaleUseCase = CompleteSaleUseCase(saleRepository)

    // --- Cashier session use cases ---

    @Provides
    @Singleton
    fun provideOpenCashierSessionUseCase(
        cashierSessionRepository: CashierSessionRepository
    ): OpenCashierSessionUseCase = OpenCashierSessionUseCase(cashierSessionRepository)

    @Provides
    @Singleton
    fun provideCloseCashierSessionUseCase(
        cashierSessionRepository: CashierSessionRepository
    ): CloseCashierSessionUseCase = CloseCashierSessionUseCase(cashierSessionRepository)

    @Provides
    @Singleton
    fun provideGetCurrentCashierSessionUseCase(
        cashierSessionRepository: CashierSessionRepository
    ): GetCurrentCashierSessionUseCase = GetCurrentCashierSessionUseCase(cashierSessionRepository)
}
