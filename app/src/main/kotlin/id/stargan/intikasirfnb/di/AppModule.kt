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
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.transaction.TableRepository
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository
import id.stargan.intikasirfnb.domain.usecase.transaction.AddLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.AddPaymentUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.BatchSettleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CancelSettlementUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.RemovePaymentUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CloseCashierSessionUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CompleteSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.ConfirmSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CreatePlatformSettlementUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CreateSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetCurrentCashierSessionUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetPendingSettlementsUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSaleByIdUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSalesChannelsUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSettlementSummaryUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.MarkSettlementDisputedUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.MarkSettlementSettledUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.SaveSalesChannelUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.DeactivateSalesChannelUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.OpenCashierSessionUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetSalesByOutletUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.RemoveLineItemUseCase
import id.stargan.intikasirfnb.domain.shared.DomainEventBus
import id.stargan.intikasirfnb.domain.usecase.transaction.SendToKitchenUseCase
import id.stargan.intikasirfnb.domain.usecase.workflow.GetActiveKitchenTicketsUseCase
import id.stargan.intikasirfnb.domain.usecase.workflow.UpdateKitchenTicketStatusUseCase
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketRepository
import id.stargan.intikasirfnb.domain.usecase.transaction.GetOpenSalesUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GenerateQueueNumberUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.AssignTableUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.DeleteTableUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.GetTablesByOutletUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.ReleaseTableUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.SaveTableUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.TransferTableUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.UpdateLineItemUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.VoidSaleUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.InitSplitBillUseCase
import id.stargan.intikasirfnb.domain.usecase.transaction.CancelSplitBillUseCase
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
    fun provideSaveSalesChannelUseCase(
        salesChannelRepository: SalesChannelRepository
    ): SaveSalesChannelUseCase = SaveSalesChannelUseCase(salesChannelRepository)

    @Provides
    @Singleton
    fun provideDeactivateSalesChannelUseCase(
        salesChannelRepository: SalesChannelRepository
    ): DeactivateSalesChannelUseCase = DeactivateSalesChannelUseCase(salesChannelRepository)

    @Provides
    @Singleton
    fun provideCreateSaleUseCase(
        saleRepository: SaleRepository,
        salesChannelRepository: SalesChannelRepository,
        tableRepository: TableRepository
    ): CreateSaleUseCase = CreateSaleUseCase(saleRepository, salesChannelRepository, tableRepository)

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
    fun provideRemovePaymentUseCase(
        saleRepository: SaleRepository
    ): RemovePaymentUseCase = RemovePaymentUseCase(saleRepository)

    @Provides
    @Singleton
    fun provideCompleteSaleUseCase(
        saleRepository: SaleRepository,
        settlementRepository: PlatformSettlementRepository,
        tableRepository: TableRepository
    ): CompleteSaleUseCase = CompleteSaleUseCase(saleRepository, settlementRepository, tableRepository)

    @Provides
    @Singleton
    fun provideGetSalesByOutletUseCase(
        saleRepository: SaleRepository
    ): GetSalesByOutletUseCase = GetSalesByOutletUseCase(saleRepository)

    // --- Kitchen / Open orders use cases ---

    @Provides
    @Singleton
    fun provideSendToKitchenUseCase(
        saleRepository: SaleRepository,
        kitchenTicketRepository: KitchenTicketRepository,
        eventBus: DomainEventBus
    ): SendToKitchenUseCase = SendToKitchenUseCase(saleRepository, kitchenTicketRepository, eventBus)

    @Provides
    @Singleton
    fun provideUpdateKitchenTicketStatusUseCase(
        kitchenTicketRepository: KitchenTicketRepository,
        eventBus: DomainEventBus
    ): UpdateKitchenTicketStatusUseCase = UpdateKitchenTicketStatusUseCase(kitchenTicketRepository, eventBus)

    @Provides
    @Singleton
    fun provideGetActiveKitchenTicketsUseCase(
        kitchenTicketRepository: KitchenTicketRepository
    ): GetActiveKitchenTicketsUseCase = GetActiveKitchenTicketsUseCase(kitchenTicketRepository)

    @Provides
    @Singleton
    fun provideGetOpenSalesUseCase(
        saleRepository: SaleRepository
    ): GetOpenSalesUseCase = GetOpenSalesUseCase(saleRepository)

    @Provides
    @Singleton
    fun provideGenerateQueueNumberUseCase(
        saleRepository: SaleRepository
    ): GenerateQueueNumberUseCase = GenerateQueueNumberUseCase(saleRepository)

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

    // --- Platform settlement use cases ---

    @Provides
    @Singleton
    fun provideCreatePlatformSettlementUseCase(
        settlementRepository: PlatformSettlementRepository
    ): CreatePlatformSettlementUseCase = CreatePlatformSettlementUseCase(settlementRepository)

    @Provides
    @Singleton
    fun provideMarkSettlementSettledUseCase(
        settlementRepository: PlatformSettlementRepository
    ): MarkSettlementSettledUseCase = MarkSettlementSettledUseCase(settlementRepository)

    @Provides
    @Singleton
    fun provideMarkSettlementDisputedUseCase(
        settlementRepository: PlatformSettlementRepository
    ): MarkSettlementDisputedUseCase = MarkSettlementDisputedUseCase(settlementRepository)

    @Provides
    @Singleton
    fun provideCancelSettlementUseCase(
        settlementRepository: PlatformSettlementRepository
    ): CancelSettlementUseCase = CancelSettlementUseCase(settlementRepository)

    @Provides
    @Singleton
    fun provideGetPendingSettlementsUseCase(
        settlementRepository: PlatformSettlementRepository
    ): GetPendingSettlementsUseCase = GetPendingSettlementsUseCase(settlementRepository)

    @Provides
    @Singleton
    fun provideGetSettlementSummaryUseCase(
        settlementRepository: PlatformSettlementRepository
    ): GetSettlementSummaryUseCase = GetSettlementSummaryUseCase(settlementRepository)

    @Provides
    @Singleton
    fun provideBatchSettleUseCase(
        settlementRepository: PlatformSettlementRepository
    ): BatchSettleUseCase = BatchSettleUseCase(settlementRepository)

    // --- Table / Dine-in use cases ---

    @Provides
    @Singleton
    fun provideGetTablesByOutletUseCase(
        tableRepository: TableRepository
    ): GetTablesByOutletUseCase = GetTablesByOutletUseCase(tableRepository)

    @Provides
    @Singleton
    fun provideAssignTableUseCase(
        saleRepository: SaleRepository,
        tableRepository: TableRepository
    ): AssignTableUseCase = AssignTableUseCase(saleRepository, tableRepository)

    @Provides
    @Singleton
    fun provideReleaseTableUseCase(
        saleRepository: SaleRepository,
        tableRepository: TableRepository
    ): ReleaseTableUseCase = ReleaseTableUseCase(saleRepository, tableRepository)

    @Provides
    @Singleton
    fun provideTransferTableUseCase(
        saleRepository: SaleRepository,
        tableRepository: TableRepository
    ): TransferTableUseCase = TransferTableUseCase(saleRepository, tableRepository)

    @Provides
    @Singleton
    fun provideSaveTableUseCase(
        tableRepository: TableRepository
    ): SaveTableUseCase = SaveTableUseCase(tableRepository)

    @Provides
    @Singleton
    fun provideDeleteTableUseCase(
        tableRepository: TableRepository
    ): DeleteTableUseCase = DeleteTableUseCase(tableRepository)

    // --- Split bill use cases ---

    @Provides
    @Singleton
    fun provideInitSplitBillUseCase(
        saleRepository: SaleRepository
    ): InitSplitBillUseCase = InitSplitBillUseCase(saleRepository)

    @Provides
    @Singleton
    fun provideCancelSplitBillUseCase(
        saleRepository: SaleRepository
    ): CancelSplitBillUseCase = CancelSplitBillUseCase(saleRepository)

    @Provides
    @Singleton
    fun provideVoidSaleUseCase(
        saleRepository: SaleRepository,
        tableRepository: TableRepository
    ): VoidSaleUseCase = VoidSaleUseCase(saleRepository, tableRepository)
}
