package id.stargan.intikasirfnb.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.stargan.intikasirfnb.data.local.dao.CashierSessionDao
import id.stargan.intikasirfnb.data.local.dao.CategoryDao
import id.stargan.intikasirfnb.data.local.dao.CustomerDao
import id.stargan.intikasirfnb.data.local.dao.MenuItemDao
import id.stargan.intikasirfnb.data.local.dao.OrderLineDao
import id.stargan.intikasirfnb.data.local.dao.OutletDao
import id.stargan.intikasirfnb.data.local.dao.OutletSettingsDao
import id.stargan.intikasirfnb.data.local.dao.PaymentDao
import id.stargan.intikasirfnb.data.local.dao.SaleDao
import id.stargan.intikasirfnb.data.local.dao.TableDao
import id.stargan.intikasirfnb.data.local.dao.TenantDao
import id.stargan.intikasirfnb.data.local.dao.TenantSettingsDao
import id.stargan.intikasirfnb.data.local.dao.UserDao
import id.stargan.intikasirfnb.data.repository.CashierSessionRepositoryImpl
import id.stargan.intikasirfnb.data.repository.CategoryRepositoryImpl
import id.stargan.intikasirfnb.data.repository.CustomerRepositoryImpl
import id.stargan.intikasirfnb.data.repository.MenuItemRepositoryImpl
import id.stargan.intikasirfnb.data.repository.OutletRepositoryImpl
import id.stargan.intikasirfnb.data.repository.OutletSettingsRepositoryImpl
import id.stargan.intikasirfnb.data.repository.SaleRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TableRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TenantRepositoryImpl
import id.stargan.intikasirfnb.data.repository.TenantSettingsRepositoryImpl
import id.stargan.intikasirfnb.data.repository.UserRepositoryImpl
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.customer.CustomerRepository
import id.stargan.intikasirfnb.domain.identity.OutletRepository
import id.stargan.intikasirfnb.domain.identity.TenantRepository
import id.stargan.intikasirfnb.domain.identity.UserRepository
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import id.stargan.intikasirfnb.domain.settings.TenantSettingsRepository
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.TableRepository
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
    fun provideMenuItemRepository(dao: MenuItemDao): MenuItemRepository = MenuItemRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCustomerRepository(dao: CustomerDao): CustomerRepository = CustomerRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideSaleRepository(
        saleDao: SaleDao,
        orderLineDao: OrderLineDao,
        paymentDao: PaymentDao
    ): SaleRepository = SaleRepositoryImpl(saleDao, orderLineDao, paymentDao)

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
}
