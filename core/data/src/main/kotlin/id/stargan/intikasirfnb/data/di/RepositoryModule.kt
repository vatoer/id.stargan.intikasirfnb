package id.stargan.intikasirfnb.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.stargan.intikasirfnb.data.local.dao.CashierSessionDao
import id.stargan.intikasirfnb.data.local.dao.CategoryDao
import id.stargan.intikasirfnb.data.local.dao.CustomerDao
import id.stargan.intikasirfnb.data.local.dao.MenuItemDao
import id.stargan.intikasirfnb.data.local.dao.MenuItemModifierGroupDao
import id.stargan.intikasirfnb.data.local.dao.ModifierGroupDao
import id.stargan.intikasirfnb.data.local.dao.ModifierOptionDao
import id.stargan.intikasirfnb.data.local.dao.OrderLineDao
import id.stargan.intikasirfnb.data.local.dao.OutletDao
import id.stargan.intikasirfnb.data.local.dao.OutletSettingsDao
import id.stargan.intikasirfnb.data.local.dao.PaymentDao
import id.stargan.intikasirfnb.data.local.dao.SaleDao
import id.stargan.intikasirfnb.data.local.dao.TableDao
import id.stargan.intikasirfnb.data.local.dao.TenantDao
import id.stargan.intikasirfnb.data.local.dao.TenantSettingsDao
import id.stargan.intikasirfnb.data.local.dao.TaxConfigDao
import id.stargan.intikasirfnb.data.local.dao.TerminalDao
import id.stargan.intikasirfnb.data.local.dao.TerminalSettingsDao
import id.stargan.intikasirfnb.data.local.dao.UserDao
import id.stargan.intikasirfnb.data.repository.CashierSessionRepositoryImpl
import id.stargan.intikasirfnb.data.repository.CategoryRepositoryImpl
import id.stargan.intikasirfnb.data.repository.CustomerRepositoryImpl
import id.stargan.intikasirfnb.data.repository.MenuItemRepositoryImpl
import id.stargan.intikasirfnb.data.repository.ModifierGroupRepositoryImpl
import id.stargan.intikasirfnb.data.repository.OutletRepositoryImpl
import id.stargan.intikasirfnb.data.repository.OutletSettingsRepositoryImpl
import id.stargan.intikasirfnb.data.local.PosDatabase
import id.stargan.intikasirfnb.data.local.dao.PlatformSettlementDao
import id.stargan.intikasirfnb.data.local.dao.PriceListDao
import id.stargan.intikasirfnb.data.local.dao.SalesChannelDao
import id.stargan.intikasirfnb.data.repository.PlatformSettlementRepositoryImpl
import id.stargan.intikasirfnb.data.repository.PriceListRepositoryImpl
import id.stargan.intikasirfnb.data.repository.SaleRepositoryImpl
import id.stargan.intikasirfnb.data.repository.SalesChannelRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TableRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TenantRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TenantSettingsRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TaxConfigRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TerminalRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TerminalSettingsRepositoryImpl
import id.stargan.intikasirfnb.data.repository.UserRepositoryImpl
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository
import id.stargan.intikasirfnb.domain.customer.CustomerRepository
import id.stargan.intikasirfnb.domain.identity.OutletRepository
import id.stargan.intikasirfnb.domain.identity.TenantRepository
import id.stargan.intikasirfnb.domain.identity.TerminalRepository
import id.stargan.intikasirfnb.domain.identity.UserRepository
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import id.stargan.intikasirfnb.domain.settings.TenantSettingsRepository
import id.stargan.intikasirfnb.domain.settings.TerminalSettingsRepository
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.catalog.PriceListRepository
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository
import id.stargan.intikasirfnb.domain.transaction.TableRepository
import id.stargan.intikasirfnb.domain.sync.SyncEngine
import id.stargan.intikasirfnb.data.sync.NoOpSyncEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTenantRepository(dao: TenantDao): TenantRepository = TenantRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideOutletRepository(dao: OutletDao): OutletRepository = OutletRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideUserRepository(dao: UserDao): UserRepository = UserRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCategoryRepository(dao: CategoryDao): CategoryRepository = CategoryRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideMenuItemRepository(
        dao: MenuItemDao,
        linkDao: MenuItemModifierGroupDao
    ): MenuItemRepository = MenuItemRepositoryImpl(dao, linkDao)

    @Provides
    @Singleton
    fun provideModifierGroupRepository(
        groupDao: ModifierGroupDao,
        optionDao: ModifierOptionDao,
        linkDao: MenuItemModifierGroupDao
    ): ModifierGroupRepository = ModifierGroupRepositoryImpl(groupDao, optionDao, linkDao)

    @Provides
    @Singleton
    fun provideCustomerRepository(dao: CustomerDao): CustomerRepository = CustomerRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideSalesChannelRepository(dao: SalesChannelDao): SalesChannelRepository =
        SalesChannelRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideSaleRepository(
        database: PosDatabase,
        saleDao: SaleDao,
        orderLineDao: OrderLineDao,
        paymentDao: PaymentDao
    ): SaleRepository = SaleRepositoryImpl(database, saleDao, orderLineDao, paymentDao)

    @Provides
    @Singleton
    fun provideTableRepository(dao: TableDao): TableRepository = TableRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCashierSessionRepository(dao: CashierSessionDao): CashierSessionRepository =
        CashierSessionRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTenantSettingsRepository(dao: TenantSettingsDao): TenantSettingsRepository =
        TenantSettingsRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideOutletSettingsRepository(dao: OutletSettingsDao): OutletSettingsRepository =
        OutletSettingsRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTerminalRepository(dao: TerminalDao): TerminalRepository = TerminalRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTaxConfigRepository(dao: TaxConfigDao): TaxConfigRepository =
        TaxConfigRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTerminalSettingsRepository(dao: TerminalSettingsDao): TerminalSettingsRepository =
        TerminalSettingsRepositoryImpl(dao)

    @Provides
    @Singleton
    fun providePriceListRepository(dao: PriceListDao): PriceListRepository =
        PriceListRepositoryImpl(dao)

    @Provides
    @Singleton
    fun providePlatformSettlementRepository(dao: PlatformSettlementDao): PlatformSettlementRepository =
        PlatformSettlementRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideSyncEngine(): SyncEngine = NoOpSyncEngine()
}
