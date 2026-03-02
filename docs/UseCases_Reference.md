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

## Transaction (Sales)

| Use Case | Parameter | Return | Keterangan |
|----------|-----------|--------|------------|
| `CreateSaleUseCase` | `OutletId`, `OrderChannel`, `TableId?`, `externalOrderId?`, `UserId?` | `Result<Sale>` | Buat sale baru |
| `GetSaleByIdUseCase` | `SaleId` | `Sale?` | Ambil sale |
| `GetSalesByOutletUseCase` | `OutletId`, `limit?` | `List<Sale>` / `Flow<List<Sale>>` | Daftar/stream sale per outlet |
| `AddLineItemUseCase` | `SaleId`, `MenuItem`, `quantity`, `modifierSnapshot?` | `Result<Sale>` | Tambah line item |
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

## DI (Hilt/Koin)

Provide tiap use case dengan inject repository yang sesuai. Contoh (Hilt):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides fun provideCreateSaleUseCase(repo: SaleRepository) = CreateSaleUseCase(repo)
    @Provides fun provideAddLineItemUseCase(repo: SaleRepository) = AddLineItemUseCase(repo)
    // ...
}
```

ViewModel inject use case dan memanggil `createSaleUseCase(outletId, channel, tableId, ...)` di dalam `viewModelScope.launch`.
