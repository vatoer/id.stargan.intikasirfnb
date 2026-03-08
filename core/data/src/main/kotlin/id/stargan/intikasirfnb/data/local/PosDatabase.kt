package id.stargan.intikasirfnb.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import id.stargan.intikasirfnb.data.local.dao.CashierSessionDao
import id.stargan.intikasirfnb.data.local.dao.SalesChannelDao
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
import id.stargan.intikasirfnb.data.local.entity.CashierSessionEntity
import id.stargan.intikasirfnb.data.local.entity.SalesChannelEntity
import id.stargan.intikasirfnb.data.local.entity.CategoryEntity
import id.stargan.intikasirfnb.data.local.entity.CustomerEntity
import id.stargan.intikasirfnb.data.local.entity.MenuItemEntity
import id.stargan.intikasirfnb.data.local.entity.MenuItemModifierGroupEntity
import id.stargan.intikasirfnb.data.local.entity.ModifierGroupEntity
import id.stargan.intikasirfnb.data.local.entity.ModifierOptionEntity
import id.stargan.intikasirfnb.data.local.entity.OrderLineEntity
import id.stargan.intikasirfnb.data.local.entity.OutletEntity
import id.stargan.intikasirfnb.data.local.entity.OutletSettingsEntity
import id.stargan.intikasirfnb.data.local.entity.PaymentEntity
import id.stargan.intikasirfnb.data.local.entity.SaleEntity
import id.stargan.intikasirfnb.data.local.entity.TableEntity
import id.stargan.intikasirfnb.data.local.entity.TenantEntity
import id.stargan.intikasirfnb.data.local.entity.TenantSettingsEntity
import id.stargan.intikasirfnb.data.local.entity.TaxConfigEntity
import id.stargan.intikasirfnb.data.local.entity.TerminalEntity
import id.stargan.intikasirfnb.data.local.entity.TerminalSettingsEntity
import id.stargan.intikasirfnb.data.local.entity.UserEntity

@Database(
    entities = [
        TenantEntity::class,
        OutletEntity::class,
        UserEntity::class,
        TenantSettingsEntity::class,
        OutletSettingsEntity::class,
        CategoryEntity::class,
        MenuItemEntity::class,
        ModifierGroupEntity::class,
        ModifierOptionEntity::class,
        MenuItemModifierGroupEntity::class,
        SalesChannelEntity::class,
        SaleEntity::class,
        OrderLineEntity::class,
        PaymentEntity::class,
        TableEntity::class,
        CashierSessionEntity::class,
        CustomerEntity::class,
        TerminalEntity::class,
        TaxConfigEntity::class,
        TerminalSettingsEntity::class
    ],
    version = 14,
    exportSchema = true
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun outletDao(): OutletDao
    abstract fun userDao(): UserDao
    abstract fun tenantSettingsDao(): TenantSettingsDao
    abstract fun outletSettingsDao(): OutletSettingsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun modifierGroupDao(): ModifierGroupDao
    abstract fun modifierOptionDao(): ModifierOptionDao
    abstract fun menuItemModifierGroupDao(): MenuItemModifierGroupDao
    abstract fun salesChannelDao(): SalesChannelDao
    abstract fun saleDao(): SaleDao
    abstract fun orderLineDao(): OrderLineDao
    abstract fun paymentDao(): PaymentDao
    abstract fun tableDao(): TableDao
    abstract fun cashierSessionDao(): CashierSessionDao
    abstract fun customerDao(): CustomerDao
    abstract fun terminalDao(): TerminalDao
    abstract fun taxConfigDao(): TaxConfigDao
    abstract fun terminalSettingsDao(): TerminalSettingsDao
}
