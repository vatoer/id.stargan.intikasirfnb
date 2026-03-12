# 09 — Use Case Reference

> Semua use case per bounded context

---

## 9.1 Overview

Total: **60+ use cases** implemented. Semua use case adalah `suspend` functions atau return `Flow`.

## 9.2 Identity & Access (7 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `GetTenantUseCase` | tenantId | Tenant | Ambil data tenant |
| `GetUserByEmailUseCase` | email | User? | Cari user by email |
| `GetOutletsByTenantUseCase` | tenantId | List\<Outlet\> | List outlet per tenant |
| `LoginWithPinUseCase` | email, pin | Result\<User\> | Autentikasi PIN |
| `SelectOutletUseCase` | outletId | Unit | Set active outlet di session |
| `CheckOnboardingNeededUseCase` | — | Boolean | Cek apakah perlu setup wizard |
| `CompleteOnboardingUseCase` | tenant, outlet, user | Unit | Create initial data + seed channels |

## 9.3 Settings (4 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `SaveTaxConfigUseCase` | TaxConfig | Unit | Simpan konfigurasi pajak |
| `GetActiveTaxConfigsUseCase` | tenantId | List\<TaxConfig\> | Ambil pajak aktif |
| `SaveOutletSettingsUseCase` | OutletSettings | Unit | Simpan settings outlet |
| `SaveTenantSettingsUseCase` | TenantSettings | Unit | Simpan settings tenant |

## 9.4 Catalog (15 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `GetCategoriesUseCase` | tenantId | Flow\<List\<Category\>\> | Stream kategori |
| `SaveCategoryUseCase` | Category | Unit | Create/update kategori |
| `DeleteCategoryUseCase` | categoryId | Unit | Hapus kategori (soft delete) |
| `GetMenuItemsUseCase` | tenantId | Flow\<List\<MenuItem\>\> | Stream menu items |
| `GetMenuItemByIdUseCase` | itemId | MenuItem? | Detail menu item |
| `SaveMenuItemUseCase` | MenuItem | Unit | Create/update menu item |
| `DeleteMenuItemUseCase` | itemId | Unit | Hapus menu item |
| `GetMenuItemsByCategoryUseCase` | categoryId | List\<MenuItem\> | Filter by kategori |
| `SearchMenuItemsUseCase` | query | List\<MenuItem\> | Cari menu item |
| `SaveModifierGroupUseCase` | ModifierGroup | Unit | Create/update modifier group |
| `GetModifierGroupsUseCase` | tenantId | Flow\<List\<ModifierGroup\>\> | Stream modifier groups |
| `DeleteModifierGroupUseCase` | groupId | Unit | Hapus modifier group |
| `SaveAddOnGroupUseCase` | AddOnGroup | Unit | Create/update add-on group + items |
| `GetAddOnGroupsUseCase` | tenantId | Flow\<List\<AddOnGroup\>\> | Stream add-on groups |
| `DeleteAddOnGroupUseCase` | groupId | Unit | Hapus add-on group (cascade items) |

## 9.5 Transaction — Core (17 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `CreateSaleUseCase` | channelId, outletId, cashierId | Sale | Buat draft sale |
| `AddLineItemUseCase` | saleId, productRef, qty, modifiers, addOns | Sale | Tambah item ke order (+ modifier & add-on) |
| `UpdateLineItemUseCase` | saleId, lineId, qty | Sale | Update qty item |
| `RemoveLineItemUseCase` | saleId, lineId | Sale | Hapus item dari order |
| `ConfirmSaleUseCase` | saleId | Sale | DRAFT→CONFIRMED + compute tax/SC |
| `AddPaymentUseCase` | saleId, method, amount, ref | Sale | Tambah pembayaran |
| `RemovePaymentUseCase` | saleId, paymentId | Sale | Hapus pembayaran |
| `CompleteSaleUseCase` | saleId | Sale | PAID→COMPLETED |
| `VoidSaleUseCase` | saleId, reason | Sale | Void transaksi |
| `GetSaleByIdUseCase` | saleId | Sale? | Detail transaksi |
| `GetSalesByOutletUseCase` | outletId | Flow\<List\<Sale\>\> | Riwayat per outlet |
| `CalculateSaleTotalsUseCase` | sale | TotalBreakdown | Preview totals (tax/SC/tip) |
| `AddTipUseCase` | saleId, amount | Sale | Tambah tip |
| `OpenSessionUseCase` | outletId, userId, float | CashierSession | Buka sesi kasir |
| `CloseSessionUseCase` | sessionId, closingCash | CashierSession | Tutup sesi kasir |
| `GetCurrentSessionUseCase` | terminalId | CashierSession? | Sesi aktif saat ini |
| `GetSalesChannelsUseCase` | tenantId | List\<SalesChannel\> | List channel tersedia |

## 9.6 Transaction — Sales Channel (3 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `SaveSalesChannelUseCase` | SalesChannel | Unit | Create/update channel |
| `DeactivateSalesChannelUseCase` | channelId | Unit | Nonaktifkan channel |
| `ResolvePriceUseCase` | menuItemId, channelId | Money | Hitung harga efektif |

## 9.7 Transaction — Platform Settlement (7 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `CreatePlatformSettlementUseCase` | saleId, gross, commission | Settlement | Buat settlement record |
| `GetPendingSettlementsUseCase` | outletId | List\<Settlement\> | Settlement belum selesai |
| `SettlePlatformPaymentUseCase` | settlementId | Settlement | Mark as settled |
| `BatchSettleUseCase` | settlementIds | List\<Settlement\> | Batch settle |
| `DisputeSettlementUseCase` | settlementId, reason | Settlement | Mark disputed |
| `CancelSettlementUseCase` | settlementId | Settlement | Cancel settlement |
| `GetSettlementSummaryUseCase` | outletId, dateRange | Summary | Ringkasan settlement |

## 9.8 Transaction — Table Management (7 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `GetTablesUseCase` | outletId | Flow\<List\<Table\>\> | Stream daftar meja |
| `SaveTableUseCase` | Table | Unit | Create/update meja |
| `DeleteTableUseCase` | tableId | Unit | Hapus meja |
| `AssignTableUseCase` | tableId, saleId | Table | Assign meja ke order |
| `ReleaseTableUseCase` | tableId | Table | Lepas meja |
| `GetAvailableTablesUseCase` | outletId | List\<Table\> | Meja tersedia saja |
| `GetTableByIdUseCase` | tableId | Table? | Detail meja |

## 9.9 Transaction — Split Bill (2 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `InitSplitBillUseCase` | saleId, strategy, count | SplitBill | Inisiasi split bill |
| `CancelSplitBillUseCase` | saleId | Sale | Batalkan split bill |

## 9.10 Workflow / Kitchen (2 Use Cases)

| Use Case | Input | Output | Deskripsi |
|----------|-------|--------|-----------|
| `GetActiveKitchenTicketsUseCase` | outletId | Flow\<List\<KitchenTicket\>\> | Tiket aktif |
| `UpdateKitchenTicketStatusUseCase` | ticketId, status | KitchenTicket | Update status tiket |

---

*Dokumen terkait: [03-Domain Model](03-domain-model.md) · [10-Testing Strategy](10-testing-strategy.md)*
