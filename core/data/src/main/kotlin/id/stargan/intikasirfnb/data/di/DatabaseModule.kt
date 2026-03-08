package id.stargan.intikasirfnb.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.stargan.intikasirfnb.data.local.PosDatabase
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
import id.stargan.intikasirfnb.data.local.dao.PlatformSettlementDao
import id.stargan.intikasirfnb.data.local.dao.PriceListDao
import id.stargan.intikasirfnb.data.local.dao.SaleDao
import id.stargan.intikasirfnb.data.local.dao.SalesChannelDao
import id.stargan.intikasirfnb.data.local.dao.TableDao
import id.stargan.intikasirfnb.data.local.dao.TenantDao
import id.stargan.intikasirfnb.data.local.dao.TenantSettingsDao
import id.stargan.intikasirfnb.data.local.dao.TaxConfigDao
import id.stargan.intikasirfnb.data.local.dao.TerminalDao
import id.stargan.intikasirfnb.data.local.dao.TerminalSettingsDao
import id.stargan.intikasirfnb.data.local.dao.UserDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PosDatabase =
        Room.databaseBuilder(context, PosDatabase::class.java, "pos_database")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTenantDao(db: PosDatabase): TenantDao = db.tenantDao()
    @Provides fun provideOutletDao(db: PosDatabase): OutletDao = db.outletDao()
    @Provides fun provideUserDao(db: PosDatabase): UserDao = db.userDao()
    @Provides fun provideTenantSettingsDao(db: PosDatabase): TenantSettingsDao = db.tenantSettingsDao()
    @Provides fun provideOutletSettingsDao(db: PosDatabase): OutletSettingsDao = db.outletSettingsDao()
    @Provides fun provideCategoryDao(db: PosDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideMenuItemDao(db: PosDatabase): MenuItemDao = db.menuItemDao()
    @Provides fun provideModifierGroupDao(db: PosDatabase): ModifierGroupDao = db.modifierGroupDao()
    @Provides fun provideModifierOptionDao(db: PosDatabase): ModifierOptionDao = db.modifierOptionDao()
    @Provides fun provideMenuItemModifierGroupDao(db: PosDatabase): MenuItemModifierGroupDao = db.menuItemModifierGroupDao()
    @Provides fun provideSalesChannelDao(db: PosDatabase): SalesChannelDao = db.salesChannelDao()
    @Provides fun provideSaleDao(db: PosDatabase): SaleDao = db.saleDao()
    @Provides fun provideOrderLineDao(db: PosDatabase): OrderLineDao = db.orderLineDao()
    @Provides fun providePaymentDao(db: PosDatabase): PaymentDao = db.paymentDao()
    @Provides fun provideTableDao(db: PosDatabase): TableDao = db.tableDao()
    @Provides fun provideCashierSessionDao(db: PosDatabase): CashierSessionDao = db.cashierSessionDao()
    @Provides fun provideCustomerDao(db: PosDatabase): CustomerDao = db.customerDao()
    @Provides fun provideTerminalDao(db: PosDatabase): TerminalDao = db.terminalDao()
    @Provides fun provideTaxConfigDao(db: PosDatabase): TaxConfigDao = db.taxConfigDao()
    @Provides fun provideTerminalSettingsDao(db: PosDatabase): TerminalSettingsDao = db.terminalSettingsDao()
    @Provides fun providePlatformSettlementDao(db: PosDatabase): PlatformSettlementDao = db.platformSettlementDao()
    @Provides fun providePriceListDao(db: PosDatabase): PriceListDao = db.priceListDao()
}
