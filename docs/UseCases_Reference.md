# Daftar Use Case — PoS F&B

Use case berada di modul **`:core:domain`** pada paket `id.stargan.intikasirfnb.domain.usecase`. ViewModel memanggil use case via `viewModelScope.launch { useCase(...) }`.

---

## Identity

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `GetTenantUseCase` | `TenantId` | `Tenant?` | Ambil tenant by ID |
| `GetOutletsByTenantUseCase` | `TenantId` | `List<Outlet>` | Daftar outlet per tenant |
| `GetUserByEmailUseCase` | `TenantId`, `String` (email) | `User?` | User by email (login) |

---

## Settings

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `GetTenantSettingsUseCase` | `TenantId` | `TenantSettings?` | Setting tenant |
| `GetOutletSettingsUseCase` | `OutletId` | `OutletSettings?` | Setting outlet |
| `GetActiveTaxConfigsUseCase` | `TenantId`, `OutletId` | `List<TaxConfig>` | Tax configs resolved (outlet override > tenant default) |
| `SaveTaxConfigUseCase` | `TaxConfig` | `Result<TaxConfig>` | Buat / update konfigurasi pajak |
| `GetServiceChargeConfigUseCase` | `TenantId`, `OutletId` | `ServiceChargeConfig` | SC config resolved |
| `SaveServiceChargeConfigUseCase` | `ServiceChargeConfig` | `Result<Unit>` | Update SC config |
| `GetTipConfigUseCase` | `TenantId`, `OutletId` | `TipConfig` | Tip config resolved |
| `SaveTipConfigUseCase` | `TipConfig` | `Result<Unit>` | Update tip config |

---

## Catalog

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `GetCategoriesUseCase` | `TenantId` | `List<Category>` | Semua kategori |
| `GetMenuItemsUseCase` | `TenantId` | `List<MenuItem>` | Semua menu item |
| `GetMenuItemsByCategoryUseCase` | `CategoryId` | `List<MenuItem>` | Menu per kategori |
| `GetMenuItemByIdUseCase` | `ProductId` | `MenuItem?` | Satu menu item |
| `SaveCategoryUseCase` | `Category` | — | Simpan kategori |
| `SaveMenuItemUseCase` | `MenuItem` | — | Simpan menu item |

---

## Sales Channel

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `GetSalesChannelsUseCase` | `TenantId` | `List<SalesChannel>` | Semua channel aktif |
| `GetSalesChannelByIdUseCase` | `SalesChannelId` | `SalesChannel?` | Satu channel |
| `SaveSalesChannelUseCase` | `SalesChannel` | `Result<SalesChannel>` | Buat / update channel |
| `DeactivateSalesChannelUseCase` | `SalesChannelId` | `Result<Unit>` | Nonaktifkan channel |
| `ResolvePriceUseCase` | `MenuItemId`, `SalesChannelId`, `List<Modifier>` | `Money` | Hitung harga efektif per channel + modifier |

---

## Transaction (Sales)

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `CreateSaleUseCase` | `OutletId`, `SalesChannelId`, `TableId?`, `externalOrderId?`, `UserId?` | `Result<Sale>` | Buat sale baru (channelId menentukan pricing & rules) |
| `GetSaleByIdUseCase` | `SaleId` | `Sale?` | Ambil sale |
| `GetSalesByOutletUseCase` | `OutletId`, `limit?`, `channelId?` | `List<Sale>` / `Flow<List<Sale>>` | Daftar/stream sale per outlet, optional filter per channel |
| `AddLineItemUseCase` | `SaleId`, `MenuItem`, `quantity`, `modifierSnapshot?` | `Result<Sale>` | Tambah line item (**priceSnapshot otomatis dihitung dari channel pricing**) |
| `CalculateSaleTotalsUseCase` | `SaleId` | `Result<Sale>` | Hitung subtotal, tax lines, service charge, tip → grandTotal. Dipanggil sebelum payment. |
| `AddTipUseCase` | `SaleId`, `Money`, `TipMethod` | `Result<Sale>` | Tambah tip ke Sale |
| `AddPaymentUseCase` | `SaleId`, `PaymentMethod`, `Money`, `reference?` | `Result<Sale>` | Tambah pembayaran |
| `ConfirmSaleUseCase` | `SaleId` | `Result<Sale>` | Konfirmasi (draft → confirmed) |
| `CompleteSaleUseCase` | `SaleId` | `Result<Sale>` | Selesaikan (paid → completed) |
| `VoidSaleUseCase` | `SaleId` | `Result<Sale>` | Batalkan sale |
| `GetTablesByOutletUseCase` | `OutletId` | `List<Table>` | Daftar meja (dine in) |
| `OpenCashierSessionUseCase` | `TerminalId`, `OutletId`, `UserId`, `Money` (float) | `Result<CashierSession>` | Buka sesi kasir |
| `CloseCashierSessionUseCase` | `OutletId`, `TerminalId` | `Result<CashierSession>` | Tutup sesi kasir |
| `GetCurrentCashierSessionUseCase` | `OutletId`, `TerminalId` | `CashierSession?` | Sesi kasir aktif |

---

## Customer

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `GetCustomersUseCase` | `TenantId` | `List<Customer>` | Daftar pelanggan |
| `GetCustomerByIdUseCase` | `CustomerId` | `Customer?` | Satu pelanggan |
| `SaveCustomerUseCase` | `Customer` | — | Simpan pelanggan |

---

## Sync

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `GetSyncStatusUseCase` | — | `Flow<SyncStatus>` | Observe sync status (SYNCED, PENDING, CONFLICT, DISABLED) |
| `GetPendingSyncCountUseCase` | — | `Int` | Jumlah entity yang belum ter-sync |
| `TriggerSyncNowUseCase` | — | `Result<SyncResult>` | Force immediate sync |
| `GetConflictsUseCase` | — | `List<ConflictRecord>` | Daftar conflict yang belum resolved |
| `ResolveConflictUseCase` | `ConflictRecord`, `Resolution` (KEEP_LOCAL/KEEP_CLOUD/MERGE) | `Result<Unit>` | Resolve satu conflict |
| `EnableCloudSyncUseCase` | `cloudApiUrl`, `registrationToken` | `Result<Unit>` | Aktifkan cloud sync, register terminal |
| `DisableCloudSyncUseCase` | — | `Result<Unit>` | Nonaktifkan cloud sync, kembali ke standalone |
| `GetTerminalInfoUseCase` | — | `Terminal` | Info terminal ini (id, type, status, lastSync) |

---

## Terminal & Device

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `RegisterTerminalUseCase` | `cloudApiUrl`, `registrationToken` | `Result<Terminal>` | Register device ke cloud |
| `UpdateTerminalSettingsUseCase` | `TerminalSettings` | `Result<Unit>` | Update terminal name, type, capabilities |
| `GetTerminalSettingsUseCase` | `TerminalId` | `TerminalSettings?` | Ambil settings terminal (resolved hierarchy) |

---

## Licensing (AppReg)

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `ActivateLicenseUseCase` | `serialNumber: String` | `Result<SignedLicenseDto>` | Full flow: challenge → integrity → activate → verify → save |
| `ReactivateLicenseUseCase` | `serialNumber: String` | `Result<SignedLicenseDto>` | Re-issue license for previously bound device (after reinstall) |
| `VerifyLicenseOfflineUseCase` | — | `Boolean` | Load stored license, Ed25519 verify, device check, expiry check |
| `RevalidateLicenseOnlineUseCase` | — | `Boolean` | Online check + grace period logic. Returns false if must deactivate |
| `GetLicenseStatusUseCase` | — | `LicenseStatus` | Current status: ACTIVE, EXPIRED, REVOKED, NOT_ACTIVATED |
| `ClearLicenseUseCase` | — | `Unit` | Remove stored license (deactivation / revoke) |

> Ref: [docs/external-integration/android-integration.md](external-integration/android-integration.md)

---

## DI (Hilt/Koin)

Provide tiap use case dengan inject repository yang sesuai. Contoh (Hilt):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides fun provideCreateSaleUseCase(repo: SaleRepository) = CreateSaleUseCase(repo)
    @Provides fun provideAddLineItemUseCase(repo: SaleRepository) = AddLineItemUseCase(repo)
    @Provides fun provideGetSyncStatusUseCase(syncEngine: SyncEngine) = GetSyncStatusUseCase(syncEngine)
    @Provides fun provideTriggerSyncNowUseCase(syncEngine: SyncEngine) = TriggerSyncNowUseCase(syncEngine)
    // ...
}
```

ViewModel inject use case dan memanggil `createSaleUseCase(outletId, channel, tableId, ...)` di dalam `viewModelScope.launch`.
