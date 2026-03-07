# Gap Analysis: Implementation Plan vs Existing Application

> **Date**: 2026-03-07
> **Version**: 1.0.0
> **Scope**: Phase 1 — Foundation & Standalone MVP
> **Method**: Full source code review (domain, data, presentation layers) dibandingkan dengan IMPLEMENTATION_PLAN.md

---

## 1. Executive Summary

Phase 1 secara keseluruhan **~40% implemented**. Domain layer dan data layer untuk Identity, Catalog, dan Transaction sudah cukup lengkap, namun ada **gap arsitektur kritis** antara dokumentasi dan kode:

- **SalesChannel**: Docs mengharuskan entity configurable, code masih hardcoded `OrderChannel` enum.
- **Sync metadata**: Docs mengharuskan "sync-ready from day one", code **tidak punya** sync fields di satupun entity.
- **Terminal entity**: Docs mendefinisikan Terminal sebagai aggregate root, code hanya punya `TerminalId` value class.
- **Tax/SC/Tip**: Docs punya model lengkap, code hanya punya `TaxRate` VO minimal yang belum di-parse.
- **UI**: Hanya flow auth (splash, onboarding, login, outlet picker, landing) yang ter-implementasi. **Tidak ada PoS screen, Catalog UI, Settings UI, atau screen fitur lainnya.**

---

## 2. Existing Application Inventory

### 2.1 Module Structure

```
app/                          -- Main app (MainActivity, navigation, theme, landing)
feature/identity/             -- Auth & onboarding feature module
core/domain/                  -- Pure domain layer (entities, VOs, repos, use cases)
core/data/                    -- Data layer (Room entities, DAOs, mappers, repo impl, DI)
```

### 2.2 Tech Stack (Confirmed)

| Component | Implementation |
|-----------|---------------|
| Language | Kotlin 2.3.10 |
| UI | Jetpack Compose (BOM 2026.02.00), Material 3 |
| DI | Hilt |
| Navigation | Navigation Compose |
| Database | Room (version 2, destructive migration) |
| State | StateFlow / MutableStateFlow |
| Architecture | MVVM + Clean Architecture |
| Auth | PIN-based (SHA-256 hash) |

### 2.3 Database Schema (13 Tables)

| Table | Sync Metadata | Notes |
|-------|--------------|-------|
| `tenants` | NONE | id, name, isActive |
| `outlets` | NONE | id, tenantId, name, address, isActive |
| `users` | NONE | id, tenantId, email, displayName, pinHash, outletIdsCsv, rolesCsv |
| `tenant_settings` | NONE | tenantId, currencyCode, taxRatesJson, numbering fields |
| `outlet_settings` | NONE | outletId, tenantId, timeZoneId, receiptHeader/Footer |
| `categories` | NONE | id, tenantId, name, parentId, sortOrder, isActive |
| `menu_items` | NONE | id, tenantId, categoryId, name, basePrice, taxCode, modifierGroupsJson, recipeJson |
| `sales` | NONE | id, outletId, **channel** (enum string), tableId, status, timestamps |
| `order_lines` | NONE | id, saleId, productId, productName, qty, unitPrice, discount, modifier |
| `payments` | NONE | id, saleId, method, amount, reference |
| `tables` | NONE | id, outletId, name, capacity, currentSaleId, isActive |
| `cashier_sessions` | NONE | terminalId, outletId, userId, openAt, closeAt, float, status |
| `customers` | NONE | id, tenantId, name, phone, email, address fields, loyaltyPoints |

### 2.4 Domain Models Implemented

| Context | Entities/VOs | Repository | Use Cases | Events |
|---------|-------------|------------|-----------|--------|
| Identity | Tenant, Outlet, User, Role, Permission, TerminalId, SessionManager, PinHasher | TenantRepo, OutletRepo, UserRepo | GetTenant, GetUserByEmail, GetOutletsByTenant, LoginWithPin, SelectOutlet, CheckOnboarding, CompleteOnboarding | NONE |
| Settings | TenantSettings, OutletSettings, TaxRate, NumberingSequenceConfig | TenantSettingsRepo, OutletSettingsRepo | GetTenantSettings, GetOutletSettings | NONE |
| Catalog | MenuItem, Category, ModifierGroup, ModifierOption, Recipe, RecipeLine, ProductRef, UoM | CategoryRepo, MenuItemRepo | GetCategories, SaveCategory, GetMenuItems, GetMenuItemById, SaveMenuItem, GetMenuItemsByCategory | NONE |
| Transaction | Sale, OrderLine, Payment, CashierSession, Table, **OrderChannel** (enum), SaleStatus, PaymentMethod | SaleRepo, CashierSessionRepo, TableRepo | CreateSale, AddLineItem, AddPayment, ConfirmSale, CompleteSale, VoidSale, GetSaleById, GetSalesByOutlet, OpenSession, CloseSession, GetCurrentSession, GetTables | NONE |
| Customer | Customer, Address | CustomerRepo | GetCustomers, GetCustomerById, SaveCustomer | NONE |
| Inventory | StockLevel, StockMovement | StockLevelRepo, StockMovementRepo | NONE | NONE |
| Supplier | Supplier, PurchaseOrder, PurchaseOrderLine | SupplierRepo, PurchaseOrderRepo | NONE | NONE |
| Accounting | Journal, JournalEntry, Account | JournalRepo, AccountRepo | NONE | NONE |
| Workflow | KitchenTicket | KitchenTicketRepo | NONE | NONE |

### 2.5 UI Screens Implemented

| Screen | Module | Status |
|--------|--------|--------|
| Splash | feature:identity | Functional — routes to onboarding or login |
| Onboarding (3 steps) | feature:identity | Functional — creates tenant, outlet, user |
| Login (PIN) | feature:identity | Functional — PIN auth against local DB |
| Outlet Picker | feature:identity | Functional — grid, access check |
| Landing | app | Partial — 6-item grid, navigation STUB (no target screens) |

### 2.6 Reusable Components

- `PinDots` — Visual PIN entry feedback
- `NumPad` — Custom numeric keypad
- `PosTheme` — Material 3, light/dark, green palette

---

## 3. Gap Analysis per Task

### 3.1 Project Setup & Infrastructure (Section 4.1)

| # | Task | Plan | Actual | Gap |
|---|------|------|--------|-----|
| 1.1.1 | Android project setup | NOT_STARTED | DONE | |
| 1.1.2 | Module structure | NOT_STARTED | DONE | +:feature:identity |
| 1.1.3 | Hilt DI | NOT_STARTED | DONE | |
| 1.1.4 | Room + migrations | NOT_STARTED | PARTIAL | Room v2 OK tapi `fallbackToDestructiveMigration` — data hilang saat schema change |
| 1.1.5 | Navigation Compose | NOT_STARTED | DONE | |
| 1.1.6 | ULID generator | NOT_STARTED | MISSING | Pakai UUID.randomUUID().toString() |
| 1.1.7 | Shared kernel | NOT_STARTED | PARTIAL | Money VO + ID VOs ada. MISSING: Syncable, SyncMetadata, SyncStatus |
| 1.1.8 | SyncEngine + NoOpSyncEngine | NOT_STARTED | MISSING | |
| 1.1.9 | Test infrastructure | NOT_STARTED | STUB | ExampleUnitTest + ExampleInstrumentedTest saja |
| 1.1.10 | CI | NOT_STARTED | MISSING | |

### 3.2 Identity & Access (Section 4.2)

| # | Task | Plan | Actual | Gap |
|---|------|------|--------|-----|
| 1.2.1 | Tenant + TenantId | NOT_STARTED | DONE | |
| 1.2.2 | Outlet + OutletId | NOT_STARTED | DONE | |
| 1.2.3 | User + Role + Permission | NOT_STARTED | DONE | |
| 1.2.4 | Terminal entity + type + status | NOT_STARTED | MISSING | Hanya TerminalId VO, tidak ada Terminal entity, TerminalType, TerminalStatus |
| 1.2.5 | Repository interfaces | NOT_STARTED | PARTIAL | 3/4 (missing TerminalRepository) |
| 1.2.6 | Events | NOT_STARTED | MISSING | Zero event infrastructure |
| 1.2.7 | Room entities + DAOs | NOT_STARTED | PARTIAL | 3/4 (missing Terminal table) |
| 1.2.8 | Repo implementations | NOT_STARTED | PARTIAL | 3/4 |
| 1.2.9 | Mappers | NOT_STARTED | PARTIAL | 3/4 |
| 1.2.10 | Use cases | NOT_STARTED | DONE+ | 7 use cases (lebih dari plan: +LoginWithPin, +SelectOutlet, +CheckOnboarding, +CompleteOnboarding) |
| 1.2.11 | Setup screen / wizard | NOT_STARTED | DONE | 3-step onboarding |
| 1.2.12 | Login screen | NOT_STARTED | DONE | PIN login + outlet picker |
| 1.2.13 | Unit tests | NOT_STARTED | MISSING | |
| 1.2.14 | Integration tests | NOT_STARTED | MISSING | |

### 3.3 Settings (Section 4.3)

| # | Task | Plan | Actual | Gap |
|---|------|------|--------|-----|
| 1.3.1 | TenantSettings, OutletSettings | NOT_STARTED | PARTIAL | Entities minimal — TenantSettings: currency, taxRates (empty), numbering. OutletSettings: timezone, receipt text. |
| 1.3.2 | TerminalSettings | NOT_STARTED | MISSING | |
| 1.3.3 | SyncSettings | NOT_STARTED | MISSING | |
| 1.3.4 | TaxRate, NumberingSequenceConfig VOs | NOT_STARTED | PARTIAL | VOs ada, tapi parseTaxRates() return empty list |
| 1.3.4b | TaxConfig entity | NOT_STARTED | MISSING | |
| 1.3.4c | ServiceChargeConfig | NOT_STARTED | MISSING | |
| 1.3.4d | TipConfig | NOT_STARTED | MISSING | |
| 1.3.5 | Repository interfaces | NOT_STARTED | DONE | TenantSettingsRepo, OutletSettingsRepo |
| 1.3.6 | Use cases | NOT_STARTED | PARTIAL | Get use cases ada. MISSING: tax/SC/tip use cases |
| 1.3.7 | Room entities + DAOs | NOT_STARTED | PARTIAL | Entities ada tapi field minimal |
| 1.3.8 | Settings UI | NOT_STARTED | MISSING | Landing punya icon, no screen |
| 1.3.9 | NumberingSequence logic | NOT_STARTED | MISSING | |
| 1.3.10 | Tests | NOT_STARTED | MISSING | |

### 3.4 Catalog (Section 4.4)

| # | Task | Plan | Actual | Gap |
|---|------|------|--------|-----|
| 1.4.1 | Category aggregate | NOT_STARTED | DONE | |
| 1.4.2 | MenuItem aggregate | NOT_STARTED | DONE | |
| 1.4.3 | Variant, ModifierGroup, Modifier | NOT_STARTED | DONE | JSON storage |
| 1.4.4 | VOs | NOT_STARTED | DONE | |
| 1.4.5 | Events | NOT_STARTED | MISSING | |
| 1.4.6 | Repository interfaces | NOT_STARTED | DONE | |
| 1.4.7 | Use cases | NOT_STARTED | DONE | 6 use cases |
| 1.4.8 | Room entities + DAOs | NOT_STARTED | DONE | |
| 1.4.9 | Repo implementations | NOT_STARTED | DONE | |
| 1.4.10 | Category management UI | NOT_STARTED | MISSING | |
| 1.4.11 | Menu item management UI | NOT_STARTED | MISSING | |
| 1.4.12 | Modifier group management UI | NOT_STARTED | MISSING | |
| 1.4.13 | Unit tests | NOT_STARTED | MISSING | |
| 1.4.14 | Integration tests | NOT_STARTED | MISSING | |

### 3.5 Transaction (Section 4.5)

| # | Task | Plan | Actual | Gap |
|---|------|------|--------|-----|
| 1.5.1 | Sale aggregate + state machine | NOT_STARTED | DONE | DRAFT/CONFIRMED/PAID/COMPLETED/VOIDED |
| 1.5.2 | OrderLine | NOT_STARTED | DONE | |
| 1.5.3 | Payment | NOT_STARTED | DONE | CASH, CARD, E_WALLET, TRANSFER, OTHER |
| 1.5.4 | CashierSession | NOT_STARTED | DONE | |
| 1.5.5 | SalesChannel aggregate | NOT_STARTED | **CRITICAL MISSING** | Masih `OrderChannel` enum (DINE_IN, TAKE_AWAY, OJOL_A, OJOL_B) — hardcoded, bukan configurable entity |
| 1.5.5b | SalesChannelId, PriceAdjustmentType | NOT_STARTED | MISSING | |
| 1.5.5c | Channel pricing (ResolvePriceUseCase) | NOT_STARTED | MISSING | |
| 1.5.5d | SalesChannel Room + DAO | NOT_STARTED | MISSING | |
| 1.5.5e | Sales channel management UI | NOT_STARTED | MISSING | |
| 1.5.6 | ProductSnapshot VO | NOT_STARTED | DONE | ProductRef with ACL |
| 1.5.6b | TaxLine, ServiceChargeLine, TipLine | NOT_STARTED | MISSING | |
| 1.5.6c | CalculateSaleTotalsUseCase | NOT_STARTED | MISSING | |
| 1.5.6d | AddTipUseCase | NOT_STARTED | MISSING | |
| 1.5.7 | Events | NOT_STARTED | MISSING | |
| 1.5.8 | Invariants | NOT_STARTED | PARTIAL | State machine OK, tapi no tax/SC/tip in total |
| 1.5.9 | Repository interfaces | NOT_STARTED | DONE | |
| 1.5.10 | Use cases | NOT_STARTED | DONE | 12 use cases |
| 1.5.11 | Room entities + DAOs | NOT_STARTED | DONE | Sale, OrderLine, Payment, CashierSession |
| 1.5.12 | Repo impl + mappers | NOT_STARTED | DONE | |
| 1.5.13 | PoS main screen | NOT_STARTED | MISSING | |
| 1.5.14 | Payment screen | NOT_STARTED | MISSING | |
| 1.5.15 | Receipt view | NOT_STARTED | MISSING | |
| 1.5.16 | Cashier session UI | NOT_STARTED | MISSING | |
| 1.5.17 | Transaction history | NOT_STARTED | MISSING | |
| 1.5.18 | Receipt printing | NOT_STARTED | MISSING | |
| 1.5.19 | Unit tests | NOT_STARTED | MISSING | |
| 1.5.20 | Integration tests | NOT_STARTED | MISSING | |

### 3.6 App Shell & Navigation (Section 4.6)

| # | Task | Plan | Actual | Gap |
|---|------|------|--------|-----|
| 1.6.1 | Landing page | NOT_STARTED | DONE | 6-item grid |
| 1.6.2 | Navigation graph | NOT_STARTED | PARTIAL | Auth flow complete, feature screens MISSING |
| 1.6.3 | Theme & design system | NOT_STARTED | DONE | Material 3, light/dark |
| 1.6.4 | Splash + first-run | NOT_STARTED | DONE | |

---

## 4. Critical Gaps (Ranked by Priority)

### Priority 1 — Arsitektur (harus diperbaiki sebelum fitur baru)

| # | Gap | Impact | Detail |
|---|-----|--------|--------|
| G1 | **Sync metadata tidak ada di semua entity** | Semakin ditunda, semakin besar refactor. Docs: "sync-ready from day one". | Tambah: syncStatus, syncVersion, createdAt, updatedAt, createdByTerminalId, updatedByTerminalId, deletedAt ke semua Room entities. |
| G2 | **OrderChannel enum vs SalesChannel entity** | Arsitektur docs, diagrams, plan — semuanya based on configurable SalesChannel. Code masih hardcoded 4 channel. | Migrasi ke SalesChannel entity + SalesChannelRepository + ChannelType enum. |
| G3 | **Terminal entity tidak ada** | Dibutuhkan untuk sync metadata (createdByTerminalId), multi-device, cashier session scoping. | Buat Terminal entity, TerminalType, TerminalStatus, TerminalRepository, Terminal table. |
| G4 | **ULID belum dipakai** | Docs: semua entity ID pakai ULID (sortable, offline-safe). Code: UUID.randomUUID(). | Integrasikan ULID library, ganti ID generation. |
| G5 | **Room destructive migration** | Data hilang setiap schema change. Tidak acceptable untuk production. | Ganti ke proper migration strategy dengan Migration objects. |

### Priority 2 — Fitur Core (MVP blocking)

| # | Gap | Impact | Detail |
|---|-----|--------|--------|
| G6 | **PoS main screen tidak ada** | Core value proposition app. Domain layer ready, UI missing. | Buat PoS screen: menu grid + cart + channel selection. |
| G7 | **Catalog management UI tidak ada** | User tidak bisa input menu items tanpa UI. | Buat CRUD screens untuk category dan menu item. |
| G8 | **Settings UI tidak ada** | User tidak bisa configure tax, receipt, printer. | Buat Settings screen. |
| G9 | **Payment screen tidak ada** | Tidak bisa complete transaction tanpa payment UI. | Buat payment screen: method, amount, change calculation. |

### Priority 3 — Quality & Completeness

| # | Gap | Impact | Detail |
|---|-----|--------|--------|
| G10 | **Tax/ServiceCharge/Tip model belum di-implement** | Docs punya model lengkap (TaxConfig, ServiceChargeConfig, TipConfig). Code hanya punya TaxRate VO yang belum diparsing. | Implement setelah PoS screen jalan. |
| G11 | **Zero tests** | Target >= 80% domain coverage. Saat ini 0%. | Mulai dengan domain entity tests, lalu use case tests. |
| G12 | **Zero domain events** | Docs define events per context. Bisa ditambah nanti tapi penting untuk integrasi antar context. | Low priority — bisa pakai direct calls dulu di Phase 1. |
| G13 | **parseTaxRates() returns empty list** | Tax rates disimpan sebagai JSON tapi SettingsMappers.kt tidak parse. | Fix mapper. |
| G14 | **NumberingSequence logic belum ada** | Transaction numbering format belum implemented. | Implement {OutletCode}-{TerminalCode}-{YYYYMMDD}-{Seq}. |

---

## 5. Positif Findings

Hal-hal yang sudah **baik dan sesuai** dengan arsitektur docs:

1. **Clean Architecture** terjaga — domain layer pure Kotlin, tidak depend ke Android/Room.
2. **Module structure** benar — `:core:domain`, `:core:data`, `:app`, `feature:identity`.
3. **MVVM pattern** konsisten — StateFlow, ViewModel, Compose.
4. **Hilt DI** proper — DatabaseModule, RepositoryModule, AppModule.
5. **Money VO** dengan BigDecimal dan arithmetic operations.
6. **Value class IDs** (TenantId, OutletId, UserId, ProductId, dll.) — type-safe.
7. **Repository pattern** — domain interface, data implementation.
8. **Sale aggregate** dengan proper state machine (sealed/enum status, transition methods).
9. **Multi-tenant scoping** — semua entity punya tenantId.
10. **ACL pattern** — ProductRef sebagai read-only snapshot dari Catalog ke Transaction.
11. **Auth flow lengkap** — splash, onboarding, PIN login, outlet picker.
12. **ModifierGroup/Modifier** sudah ada di Catalog.
13. **Recipe model** sudah ada di MenuItem (opsional).

---

## 6. Recommended Action Plan

Urutan pengerjaan yang disarankan berdasarkan dependency dan impact:

```
Step 1: Arsitektur Foundation (fix gaps G1-G5)
  1a. ULID library integration (G4)
  1b. Sync metadata ke semua entities (G1)
  1c. Terminal entity (G3)
  1d. SalesChannel entity, migrasi dari OrderChannel (G2)
  1e. Proper Room migration strategy (G5)

Step 2: Core UI (fix gaps G6-G9)
  2a. Catalog management screens (G7) — butuh data untuk PoS
  2b. PoS main screen (G6) — menu grid + cart
  2c. Payment screen (G9)
  2d. Settings UI (G8)

Step 3: Business Logic Completion (fix gaps G10, G13, G14)
  3a. Fix parseTaxRates (G13)
  3b. TaxConfig/SC/Tip implementation (G10)
  3c. NumberingSequence logic (G14)

Step 4: Quality (fix gaps G11, G12)
  4a. Domain unit tests (G11)
  4b. Integration tests
  4c. Domain events infrastructure (G12)
```

---

## 7. Statistics

| Metric | Value |
|--------|-------|
| Total Phase 1 tasks | 78 |
| DONE | 31 |
| PARTIAL | 11 |
| MISSING | 36 |
| Completion (DONE) | 40% |
| Completion (DONE + PARTIAL) | 54% |
| Domain models implemented | 9/12 contexts (missing dedicated Sync, full Settings, Terminal) |
| Room tables | 13 |
| DAOs | 13 |
| Repository interfaces | 14 |
| Repository implementations | 11 |
| Use cases | 28 |
| UI screens | 5 (auth flow only) |
| Unit tests | 0 (stub only) |

---

*Dokumen ini adalah snapshot analisis pada tanggal pembuatan. Update sesuai progress implementasi.*
