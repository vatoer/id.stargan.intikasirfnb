# Implementation Plan — IntiKasir F&B

> Living document. Update setiap kali ada perubahan scope, prioritas, atau timeline.
>
> **Last updated**: 2026-03-15
> **Version**: 1.9.0
>
> **Gap Analysis**: [GAP_ANALYSIS_2026-03-07.md](GAP_ANALYSIS_2026-03-07.md) — snapshot kondisi code vs plan.

---

## Table of Contents

- [1. Overview](#1-overview)
- [2. Guiding Principles](#2-guiding-principles)
- [3. Phase Overview](#3-phase-overview)
- [4. Phase 1 — Foundation & Standalone MVP](#4-phase-1--foundation--standalone-mvp)
- [5. Phase 2 — Full PoS Features](#5-phase-2--full-pos-features)
- [6. Phase 3 — Cloud Sync Foundation](#6-phase-3--cloud-sync-foundation)
- [7. Phase 4 — Multi-Terminal](#7-phase-4--multi-terminal)
- [8. Phase 5 — Multi-Outlet & Multi-Tenant](#8-phase-5--multi-outlet--multi-tenant)
- [9. Cross-Cutting Concerns](#9-cross-cutting-concerns)
- [10. Risk Register](#10-risk-register)
- [11. Definition of Done](#11-definition-of-done)
- [12. Document References](#12-document-references)

---

## 1. Overview

**Product**: IntiKasir F&B — Android PoS untuk restoran/kafe
**Stack**: Kotlin, MVVM, Clean Architecture, Room, Hilt, Jetpack Compose
**Goal**: Offline-first single PoS yang cloud-ready untuk multi-device/outlet/tenant

### Milestone Summary


| Phase | Milestone                   | Target | Status      |
| ----- | --------------------------- | ------ | ----------- |
| 1     | Foundation & Standalone MVP | -      | DONE        |
| 2     | Full PoS Features           | -      | DONE        |
| 3     | Cloud Sync Foundation       | -      | NOT_STARTED |
| 4     | Multi-Terminal              | -      | NOT_STARTED |
| 5     | Multi-Outlet & Multi-Tenant | -      | NOT_STARTED |


> Status: `NOT_STARTED` | `IN_PROGRESS` | `BLOCKED` | `DONE`

---

## 2. Guiding Principles

1. **Offline-first always** — Setiap fitur HARUS berfungsi tanpa internet. Cloud sync adalah enhancement, bukan requirement.
2. **Sync-ready from day one** — Semua entity menyertakan sync metadata (ULID, syncStatus, syncVersion, terminalId) meskipun sync belum aktif.
3. **Vertical slices** — Bangun fitur end-to-end (domain → data → UI) satu per satu, bukan layer per layer.
4. **Test as you go** — Unit test domain logic, integration test Room DAO, UI test critical flows.
5. **Ship incrementally** — Setiap phase menghasilkan aplikasi yang bisa dipakai (releasable).

---

## 3. Phase Overview

```
Phase 1: Foundation & Standalone MVP
  ├── Project setup, module structure, shared kernel
  ├── Identity & Access (Tenant, Outlet, User, Terminal)
  ├── Settings (Tax, Service Charge, Tip, Numbering, Receipt, Printer)
  ├── Catalog (MenuItem, Category, Modifier, Add-on)
  └── Basic Transaction (Sale, LineItem, Payment, CashierSession)

Phase 2: Full PoS Features
  ├── F&B specifics (OrderChannel, Table, Recipe, KitchenTicket)
  ├── Customer
  ├── Pricing & Promotion (basic discount)
  ├── Inventory (stock deduction, stock adjustment)
  ├── Accounting (basic journal)
  └── Reporting (local reports)

Phase 3: Cloud Sync Foundation
  ├── Cloud API server setup
  ├── SyncEngine implementation (CloudSyncEngine)
  ├── Push/Pull sync
  ├── Conflict resolution
  └── SyncSettings UI

Phase 4: Multi-Terminal
  ├── Multi-kasir support
  ├── Waiter terminal mode
  ├── Kitchen Display mode
  └── Real-time sync (SSE)

Phase 5: Multi-Outlet & Multi-Tenant
  ├── Multi-outlet data scoping
  ├── Cross-outlet reporting
  ├── Multi-tenant admin
  └── Tenant-wide vs per-outlet catalog
```

---

## 4. Phase 1 — Foundation & Standalone MVP

> Goal: Aplikasi bisa diinstall dan digunakan sebagai kasir sederhana di 1 device.

### 4.1 Project Setup & Infrastructure


| #      | Task                                                                                                                                         | Layer        | Depends On | Status      | Notes                                                                                |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------------- | ------------ | ---------- | ----------- | ------------------------------------------------------------------------------------ |
| 1.1.1  | Setup Android project (Kotlin, Gradle KTS)                                                                                                   | infra        | —          | DONE        | Kotlin 2.3.10, Compose BOM 2026.02.00, Material 3                                    |
| 1.1.2  | Setup module structure: `:core:domain`, `:core:data`, `:app`                                                                                 | infra        | 1.1.1      | DONE        | + `:feature:identity` module                                                         |
| 1.1.3  | Configure Hilt DI                                                                                                                            | infra        | 1.1.1      | DONE        | DatabaseModule, RepositoryModule, AppModule                                          |
| 1.1.4  | Configure Room database + migrations strategy                                                                                                | data         | 1.1.2      | PARTIAL     | Room v10, 20 tables, pakai `fallbackToDestructiveMigration` — perlu proper migration |
| 1.1.5  | Setup Jetpack Navigation (single activity)                                                                                                   | presentation | 1.1.2      | DONE        | Navigation Compose, 15+ routes (auth, landing, settings/*, catalog/*)                |
| 1.1.6  | Setup ULID generator library                                                                                                                 | domain       | 1.1.2      | DONE        | ulid-creator 5.2.3, all 15 ID classes migrated from UUID                             |
| 1.1.7  | Define shared kernel: `Syncable` interface, base value objects (`Money`, `TenantId`, `OutletId`, `TerminalId`, `SyncStatus`, `SyncMetadata`) | domain       | 1.1.2      | DONE        | Money, Syncable, SyncMetadata, SyncStatus, all ID VOs with ULID                      |
| 1.1.8  | Define `SyncEngine` interface + `NoOpSyncEngine`                                                                                             | domain+data  | 1.1.7      | DONE        | SyncEngine in domain/sync, NoOpSyncEngine in data/sync                               |
| 1.1.9  | Setup test infrastructure (JUnit5, MockK, Turbine for Flow)                                                                                  | infra        | 1.1.2      | DONE        | JUnit 4.13.2, MockK 1.14.2, Turbine 1.2.0, kotlinx-coroutines-test                   |
| 1.1.10 | Setup CI: lint, compile, unit tests                                                                                                          | infra        | 1.1.9      | NOT_STARTED |                                                                                      |


### 4.2 Identity & Access Context


| #      | Task                                                                                                           | Layer        | Depends On   | Status      | Notes                                                                                                           |
| ------ | -------------------------------------------------------------------------------------------------------------- | ------------ | ------------ | ----------- | --------------------------------------------------------------------------------------------------------------- |
| 1.2.1  | Domain: `Tenant` entity + `TenantId` VO                                                                        | domain       | 1.1.7        | DONE        | data class Tenant(id, name, isActive)                                                                           |
| 1.2.2  | Domain: `Outlet` entity + `OutletId` VO                                                                        | domain       | 1.2.1        | DONE        | data class Outlet(id, tenantId, name, address, isActive)                                                        |
| 1.2.3  | Domain: `User` entity + `Role` + `Permission` VOs                                                              | domain       | 1.2.1        | DONE        | RBAC: User.hasAccessToOutlet(), Role/Permission as value class                                                  |
| 1.2.4  | Domain: `Terminal` entity + `TerminalType` + `TerminalStatus`                                                  | domain       | 1.2.2        | DONE        | Terminal + TerminalType(CASHIER,WAITER,KITCHEN_DISPLAY,MANAGER) + TerminalStatus(ACTIVE,SUSPENDED,DEREGISTERED) |
| 1.2.5  | Domain: Repository interfaces (`TenantRepository`, `OutletRepository`, `UserRepository`, `TerminalRepository`) | domain       | 1.2.1-4      | DONE        | 4/4 done                                                                                                        |
| 1.2.6  | Domain: Events (`UserLoggedIn`, `TerminalRegistered`)                                                          | domain       | 1.2.3-4      | DROPPED     | Domain Events adalah pattern DDD, bukan UI arch. Tidak dibutuhkan di MVVM maupun MVI — side effects ditangani langsung di use cases. Cloud sync cukup pakai syncStatus column (sudah ada di semua entity). Hanya relevan jika butuh event sourcing atau audit log |
| 1.2.7  | Data: Room entities + DAOs for Tenant, Outlet, User, Terminal                                                  | data         | 1.2.5, 1.1.4 | DONE        | All 4 entities + sync metadata + DAOs + FK indices                                                              |
| 1.2.8  | Data: Repository implementations                                                                               | data         | 1.2.7        | DONE        | 4/4: TenantRepoImpl, OutletRepoImpl, UserRepoImpl, TerminalRepoImpl                                             |
| 1.2.9  | Data: Mappers (Room entity <-> Domain model)                                                                   | data         | 1.2.7        | DONE        | IdentityMappers.kt: Tenant, Outlet, User, Terminal mappers                                                      |
| 1.2.10 | Domain: Use cases (`GetTenantUseCase`, `GetUserByEmailUseCase`, `GetTerminalInfoUseCase`)                      | domain       | 1.2.5        | DONE        | 7 use cases                                                                                                     |
| 1.2.11 | UI: Initial setup screen (create Tenant, Outlet, first User, auto-create Terminal)                             | presentation | 1.2.10       | DONE        | 3-step onboarding wizard (business info, outlet info, owner+PIN). Terminal auto-create NOT implemented          |
| 1.2.12 | UI: Login screen (PIN or email)                                                                                | presentation | 1.2.10       | DONE        | PIN-based login + outlet picker. Custom NumPad + PinDots components                                             |
| 1.2.13 | Unit tests: domain entities, invariants                                                                        | test         | 1.2.1-6      | DONE        | 9 tests: Terminal(6), User(3)                                                                                   |
| 1.2.14 | Integration tests: Room DAOs                                                                                   | test         | 1.2.7-9      | DONE        | 20 androidTest: IdentityDaoIntegrationTest — TenantDao(4), OutletDao(3), UserDao(6), TerminalDao(5), Cross-DAO hierarchy(1). Tenant isolation, sort ordering, email lookup, active status filter |


### 4.3 Settings Context


| #      | Task                                                                                                                                                                        | Layer        | Depends On   | Status      | Notes                                                                                                                                                                                                                                                                                                                                     |
| ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ | ------------ | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1.3.1  | Domain: `TenantSettings`, `OutletSettings` aggregates                                                                                                                       | domain       | 1.2.1        | DONE        | TenantSettings: currency, numbering, syncEnabled. OutletSettings: timezone, SC, Tip, ReceiptConfig                                                                                                                                                                                                                                        |
| 1.3.2  | Domain: `TerminalSettings` aggregate + `PrinterConfig`                                                                                                                      | domain       | 1.2.4        | DONE        | PrinterConfig: connectionType(NONE/BT/USB/NET), address, name, autoCut, density, autoPrint, copies, cashDrawer                                                                                                                                                                                                                            |
| 1.3.3  | Domain: `SyncSettings` VO (embedded in TenantSettings)                                                                                                                      | domain       | 1.1.7        | DONE        | `syncEnabled` field in TenantSettings                                                                                                                                                                                                                                                                                                     |
| 1.3.4  | Domain: VOs (`NumberingSequenceConfig`)                                                                                                                                     | domain       | 1.3.1        | DONE        | NumberingSequenceConfig: prefix, paddingLength, nextNumber                                                                                                                                                                                                                                                                                |
| 1.3.4b | Domain: `TaxConfig` entity (taxId, name, rate, isIncludedInPrice, TaxScope, isActive)                                                                                       | domain       | 1.3.1        | DONE        | TaxConfigId(ULID), TaxScope(ALL_ITEMS, SPECIFIC_CATEGORIES, SPECIFIC_ITEMS), supports >1 active tax (PPN, PB1)                                                                                                                                                                                                                            |
| 1.3.4c | Domain: `ServiceChargeConfig` VO (isEnabled, rate, isIncludedInPrice)                                                                                                       | domain       | 1.3.1        | DONE        | Embedded in OutletSettings                                                                                                                                                                                                                                                                                                                |
| 1.3.4d | Domain: `TipConfig` VO (isEnabled, suggestedPercentages, allowCustomAmount)                                                                                                 | domain       | 1.3.1        | DONE        | Embedded in OutletSettings                                                                                                                                                                                                                                                                                                                |
| 1.3.4e | Domain: `ReceiptConfig` — header (logo, businessName, address, phone, NPWP, customLines), body (show toggles), footer (text, barcode/QR, thankYou, socialMedia), paperWidth | domain       | 1.3.1        | DONE        | ReceiptHeaderConfig, ReceiptBodyConfig, ReceiptFooterConfig, PaperWidth(58mm/80mm), ReceiptBarcodeType(NONE/CODE128/QR_CODE)                                                                                                                                                                                                              |
| 1.3.5  | Domain: Repository interfaces                                                                                                                                               | domain       | 1.3.1-2      | DONE        | TenantSettingsRepo, OutletSettingsRepo, TaxConfigRepo, TerminalSettingsRepo                                                                                                                                                                                                                                                               |
| 1.3.6  | Domain: Use cases                                                                                                                                                           | domain       | 1.3.5        | DONE        | GetTenantSettings, GetOutletSettings, GetActiveTaxConfigs, SaveTaxConfig, SaveOutletSettings, SaveTenantSettings                                                                                                                                                                                                                          |
| 1.3.7  | Data: Room entities + DAOs + repository impl                                                                                                                                | data         | 1.3.5, 1.1.4 | DONE        | TenantSettingsEntity, OutletSettingsEntity (full receipt columns), TaxConfigEntity, TerminalSettingsEntity (no FK, indices only) + DAOs + repos + mappers. DB v8                                                                                                                                                                          |
| 1.3.8  | UI: Settings screen (tax, service charge, tip, receipt, printer)                                                                                                            | presentation | 1.3.6        | DONE        | SettingsMainScreen, OutletProfileScreen, ReceiptSettingsScreen, PrinterSettingsScreen, TaxSettingsScreen, ServiceChargeSettingsScreen, TipSettingsScreen. Shared components: TestPrintSection, StickyBottomSaveBar, SettingsCard, SettingsSwitchItem, SettingsTextFieldItem. BT discovery, optimistic UI update, collapsible device lists |
| 1.3.9  | Domain: `NumberingSequence` logic (per terminal per day)                                                                                                                    | domain       | 1.3.4        | NOT_STARTED | {OutletCode}-{TerminalCode}-{YYYYMMDD}-{Seq}                                                                                                                                                                                                                                                                                              |
| 1.3.10 | Unit tests: settings, tax/SC/tip, receipt, printer config                                                                                                                   | test         | 1.3.1-6      | DONE        | 21 tests: TaxConfig(5), TaxConfigId(1), SC(1), Tip(1), OutletSettings(2), TenantSettings(2), ReceiptConfig(5), PrinterConfig(2), TerminalSettings(1), Numbering(1)                                                                                                                                                                        |


### 4.4 Catalog Context


| #      | Task                                                                                                       | Layer        | Depends On   | Status      | Notes                                                                                                                                                                                                                                     |
| ------ | ---------------------------------------------------------------------------------------------------------- | ------------ | ------------ | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1.4.1  | Domain: `Category` aggregate                                                                               | domain       | 1.1.7        | DONE        | Hierarchical (parentId), sortOrder, isActive                                                                                                                                                                                              |
| 1.4.2  | Domain: `MenuItem` aggregate (F&B specialization of Product)                                               | domain       | 1.4.1        | DONE        | id, tenantId, categoryId, name, description, imageUri, basePrice, taxCode, modifierLinks, recipe                                                                                                                                          |
| 1.4.3  | Domain: `ModifierGroup` + `ModifierOption` entities (separate, reusable) + `MenuItemModifierLink` junction | domain       | 1.4.2        | DONE        | ModifierGroup(id, tenantId, name, options, isRequired, minSelection, maxSelection, sortOrder, isActive). Selection rules on Group level (not per-link). ModifierOption(id, groupId, name, priceDelta, sortOrder, isActive). Junction: MenuItemModifierLink(menuItemId, modifierGroupId, sortOrder) — simple toggle, no per-item overrides |
| 1.4.4  | Domain: VOs (`ProductId`, `CategoryId`, `ModifierGroupId`, `ModifierOptionId`, `UnitOfMeasure`)            | domain       | 1.1.7        | DONE        | + IngredientId, UoM enum (PCS, KG, GRAM, LITER, ML, PORTION, PACK, HOUR)                                                                                                                                                                  |
| 1.4.5  | Domain: Events (`ProductCreated`, `ProductUpdated`, `ProductDeactivated`)                                  | domain       | 1.4.2        | DROPPED     | Tidak dibutuhkan. Sync menggunakan syncStatus column, bukan event-driven. Cross-module side effects ditangani di use cases                                                                                                                |
| 1.4.6  | Domain: Repository interfaces (`MenuItemRepository`, `CategoryRepository`, `ModifierGroupRepository`)      | domain       | 1.4.2        | DONE        | + delete, searchByName, modifier link CRUD                                                                                                                                                                                                |
| 1.4.7  | Domain: Use cases (CRUD menu item, CRUD category, CRUD modifier, search, delete)                           | domain       | 1.4.6        | DONE        | 11 use cases: GetCategories, SaveCategory, DeleteCategory, GetMenuItems, GetMenuItemById, SaveMenuItem, DeleteMenuItem, GetMenuItemsByCategory, SearchMenuItems, SaveModifierGroup, GetModifierGroups, DeleteModifierGroup                |
| 1.4.8  | Data: Room entities + DAOs + mappers                                                                       | data         | 1.4.6, 1.1.4 | DONE        | CategoryEntity, MenuItemEntity (imageUri, no modifierGroupsJson), ModifierGroupEntity, ModifierOptionEntity, MenuItemModifierGroupEntity (junction). 3 new DAOs + updated existing. DB v9, 19 tables                                      |
| 1.4.9  | Data: Repository implementations                                                                           | data         | 1.4.8        | DONE        | CategoryRepositoryImpl, MenuItemRepositoryImpl (with modifier links), ModifierGroupRepositoryImpl (group + options + links)                                                                                                               |
| 1.4.10 | UI: Category management screen                                                                             | presentation | 1.4.7        | DONE        | CategoryManagementScreen: CRUD with dialog, hierarchical display (parent→children indent), item count per category, active toggle, sortOrder, parent selector. CatalogMainScreen hub (4 tabs: Categories, Menu Items, Modifiers, Add-on). Navigation: Landing→Catalog→Categories |
| 1.4.11 | UI: Menu item management screen                                                                            | presentation | 1.4.7        | DONE        | MenuItemManagementScreen (list + category filter chips + search + active toggle + delete confirm) + MenuItemFormScreen (add/edit, image picker, category dropdown, price, modifier group checklist with per-item config, add-on group checklist). Navigation with itemId navArgument |
| 1.4.12 | UI: Modifier group management                                                                              | presentation | 1.4.7        | DONE        | ModifierGroupManagementScreen (list with inline options, price deltas, active toggle, delete confirm) + ModifierGroupFormScreen (dynamic options via mutableStateListOf). Navigation with groupId navArgument                             |
| 1.4.13 | Unit tests: domain, use cases                                                                              | test         | 1.4.1-7      | DONE        | 58 tests: CatalogModelsTest(17), CatalogUseCaseTest(11) with fake repos, EscPosBuilderTest(30) incl ReceiptConfig + rasterImage                                                                                                           |
| 1.4.14 | Integration tests: Room DAOs, repository                                                                   | test         | 1.4.8-9      | DONE        | 27 androidTest: RoomDaoIntegrationTest — SaleDao(6), OrderLineDao(4), PaymentDao(2), CategoryDao(3), TableDao(8), Cross-DAO(2). In-memory DB, FK cascade, soft delete, filtering, JSON snapshots. Tested on Android 14+15 |
| 1.4.15 | Domain: `AddOnGroup` + `AddOnItem` entities (separate, reusable) + `MenuItemAddOnLink` junction            | domain       | 1.4.2        | DONE | AddOnGroup(id, tenantId, name, items, sortOrder, isActive). AddOnItem(id, groupId, name, price, maxQty, inventoryItemId?, sortOrder, isActive). Junction: MenuItemAddOnLink(menuItemId, addOnGroupId, sortOrder). Qty-based, selalu ada harga |
| 1.4.16 | Domain: VOs (`AddOnGroupId`, `AddOnItemId`)                                                                | domain       | 1.1.7        | DONE | ULID-based value classes                                                                                                                                                                                                                  |
| 1.4.17 | Domain: Repository interface `AddOnGroupRepository`                                                        | domain       | 1.4.15       | DONE | CRUD group+items, link CRUD, listByTenant, getLinksForItem                                                                                                                                                                                |
| 1.4.18 | Domain: Use cases (SaveAddOnGroup, GetAddOnGroups, DeleteAddOnGroup)                                       | domain       | 1.4.17       | DONE | 3 use cases: CRUD add-on group with items                                                                                                                                                                                                 |
| 1.4.19 | Data: Room entities + DAOs + mappers (AddOnGroup, AddOnItem, MenuItemAddOnGroup)                           | data         | 1.4.17,1.1.4 | DONE | addon_groups, addon_items, menu_item_addon_groups tables. Sync metadata on all. FK CASCADE on items. DB v22                                                                                                                               |
| 1.4.20 | Data: `AddOnGroupRepositoryImpl`                                                                           | data         | 1.4.19       | DONE | group + items + links CRUD, same pattern as ModifierGroupRepositoryImpl                                                                                                                                                                   |
| 1.4.21 | UI: Add-on group management screen                                                                         | presentation | 1.4.18       | DONE | AddOnGroupManagementScreen (list with inline items, price, maxQty) + AddOnGroupFormScreen (dynamic items list). CatalogMainScreen: "Add-on" nav tab                                                                                       |
| 1.4.22 | Domain: `SelectedAddOn` VO + OrderLine.addOns + addOnTotal() + updated lineTotal()                         | domain       | 1.4.15,1.5.2 | DONE | SelectedAddOn(addOnName, quantity, unitPrice, totalPrice). lineTotal = (effectiveUnitPrice * qty) + addOnTotal - discount. addOnTotal NOT multiplied by OrderLine qty                                                                      |
| 1.4.23 | Data: OrderLineEntity.addOnSnapshot (JSON) + serialization                                                 | data         | 1.4.22       | DONE | Same snapshot pattern as modifierSnapshot. JSON: `[{n:"Extra Cheese",q:2,u:5000,t:10000}]`                                                                                                                                               |
| 1.4.24 | UI: Add-on selection in POS (during add-to-cart)                                                           | presentation | 1.4.22       | DONE | Combined with modifier in ModifierAndAddOnSelectionContent bottom sheet. Qty stepper, max qty, running total                                                                                                                              |
| 1.4.25 | Unit tests: AddOnGroup domain, use cases, SelectedAddOn + lineTotal                                        | test         | 1.4.15-22    | DONE        | 21 tests: AddOnModelTest — ID uniqueness, AddOnItem defaults/price/maxQty/inventoryId, AddOnGroup defaults/items/copy, MenuItemAddOnLink, SelectedAddOn totalPrice/override, OrderLine addOnTotal/lineTotal with addons+modifiers+discount, MenuItem with addOnLinks |


### 4.5 Transaction Context (Basic)


| #      | Task                                                                                                                                                         | Layer        | Depends On      | Status      | Notes                                                                                                                                                                                                                                                                                                                                                                                   |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------ | --------------- | ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1.5.1  | Domain: `Sale` aggregate root + state machine (DRAFT -> CONFIRMED -> PAID -> COMPLETED / VOIDED)                                                             | domain       | 1.1.7           | DONE        | SaleStatus enum, methods: addLine, updateLine, removeLine, addPayment, confirm, complete, void. totalAmount, totalPaid, isFullyPaid, subtotal, changeDue. Sale uses channelId(SalesChannelId) instead of OrderChannel enum                                                                                                                                                              |
| 1.5.2  | Domain: `OrderLine` entity (productRef, qty, priceSnapshot, discount)                                                                                        | domain       | 1.5.1           | DONE        | OrderLineId(ULID), SelectedModifier(groupName, optionName, priceDelta), effectiveUnitPrice(), modifierTotal(), lineTotal() = (unitPrice + modifiers) * qty - discount                                                                                                                                                                                                                   |
| 1.5.3  | Domain: `Payment` entity/VO (method, amount, reference)                                                                                                      | domain       | 1.5.1           | DONE        | PaymentId(ULID), PaymentMethod enum: CASH, CARD, E_WALLET, TRANSFER, OTHER                                                                                                                                                                                                                                                                                                              |
| 1.5.4  | Domain: `CashierSession` aggregate (open, close, float, reconciliation)                                                                                      | domain       | 1.2.4           | DONE        | CashierSessionId(ULID) as PK, terminalId as indexed column. closingCash, expectedCash, cashDifference() for reconciliation. OPEN/CLOSED status                                                                                                                                                                                                                                          |
| 1.5.5  | Domain: `SalesChannel` aggregate + `ChannelType` enum (DINE_IN, TAKE_AWAY, DELIVERY_PLATFORM, OWN_DELIVERY) + `PlatformConfig` VO + `SalesChannelRepository` | domain       | 1.1.7           | DONE        | SalesChannel entity with resolvePrice(), factory methods dineIn()/takeAway(), PlatformConfig, TableMode(REQUIRED/OPTIONAL/NONE). Pre-seeded during onboarding. 3 use cases: GetSalesChannels, SaveSalesChannel, DeactivateSalesChannel                                                                                                                                                 |
| 1.5.5b | Domain: VOs (`SaleId`, `SalesChannelId`, `PaymentMethod`, `PriceAdjustmentType`)                                                                             | domain       | 1.1.7           | DONE        | All ULID-based: SaleId, SalesChannelId, OrderLineId, PaymentId, CashierSessionId, TableId. PriceAdjustmentType(MARKUP_PERCENT, MARKUP_FIXED, DISCOUNT_PERCENT, DISCOUNT_FIXED)                                                                                                                                                                                                          |
| 1.5.5c | Domain: Channel pricing logic — base price + markup/discount per channel                                                                                     | domain       | 1.5.5, 1.4.2    | DONE        | resolvePrice(basePrice) on SalesChannel entity. 4 adjustment types. CreateSaleUseCase validates channel rules dynamically                                                                                                                                                                                                                                                               |
| 1.5.5d | Data: Room entity + DAO + repository impl for SalesChannel                                                                                                   | data         | 1.5.5, 1.1.4    | DONE        | SalesChannelEntity with flattened PlatformConfig columns. SalesChannelDao (CRUD + countByTenant). SalesChannelRepositoryImpl + mapper. Pre-seed Dine In + Take Away via CompleteOnboardingUseCase. DB v10, 20 tables                                                                                                                                                                    |
| 1.5.5e | UI: Sales channel management screen (CRUD channels, set pricing, platform config)                                                                            | presentation | 1.5.5c          | DONE        | SalesChannelSettingsScreen: list with type icon, name, code, adjustment info, platform config. Add/edit dialog: ChannelType selector (FilterChips), name, code, sortOrder, price adjustment toggle (4 types), PlatformConfig section (delivery only). SalesChannelViewModel (HiltViewModel). Navigation: Settings → Penjualan → Channel Penjualan. 2 new DI providers (Save/Deactivate) |
| 1.5.6  | Domain: `ProductSnapshot` VO (ACL from Catalog, **price = channel-adjusted price**)                                                                          | domain       | 1.4.2, 1.5.5c   | DONE        | ProductRef(productId, name, price, taxCode) with ProductRef.from(menuItem) factory                                                                                                                                                                                                                                                                                                      |
| 1.5.6b | Domain: `TaxLine`, `ServiceChargeLine`, `TipLine` VOs on Sale                                                                                                | domain       | 1.5.1, 1.3.4b-d | DONE        | TaxLine.compute() (inclusive/exclusive), ServiceChargeLine.compute(), TipLine VO. Sale.applyTotals(), taxTotal(), inclusiveTaxTotal(), serviceChargeAmount(), totalAmount() includes all                                                                                                                                                                                                |
| 1.5.6c | Domain: `CalculateSaleTotalsUseCase` — hitung subtotal, tax lines, SC, grandTotal                                                                            | domain       | 1.5.6b, 1.3.6   | DONE        | Resolves active TaxConfig + OutletSettings SC at confirmation. ConfirmSaleUseCase computes tax+SC snapshot. CalculateSaleTotalsUseCase for preview                                                                                                                                                                                                                                      |
| 1.5.6d | Domain: `AddTipUseCase` — tambah tip ke Sale                                                                                                                 | domain       | 1.5.6b          | DONE        | Sale.addTip()/removeTip(). AddTipUseCase. Displayed in Payment + Receipt screens                                                                                                                                                                                                                                                                                                        |
| 1.5.7  | Domain: Events (`SaleCreated`, `LineItemAdded`, `PaymentReceived`, `SaleCompleted`, `SaleVoided`)                                                            | domain       | 1.5.1           | DROPPED     | Tidak dibutuhkan. CompleteSaleUseCase sudah handle side effects langsung (release table, create settlement). Sync pakai syncStatus column. Jika butuh audit trail di masa depan, bisa ditambahkan sebagai fitur terpisah tanpa event infrastructure |
| 1.5.8  | Domain: Invariants (payment >= grandTotal incl tax/SC/tip, valid transitions, price snapshot)                                                                | domain       | 1.5.1-3, 1.5.6b | DONE        | State machine transitions enforced. Tax/SC/tip computed at confirmation. Payment auto-transitions to PAID via isFullyPaidAfter(). Channel validation dynamic via SalesChannel entity                                                                                                                                                                                                    |
| 1.5.9  | Domain: Repository interfaces (`SaleRepository`, `CashierSessionRepository`)                                                                                 | domain       | 1.5.1, 1.5.4    | DONE        | + TableRepository                                                                                                                                                                                                                                                                                                                                                                       |
| 1.5.10 | Domain: Use cases (CreateSale, AddLineItem, AddPayment, CompleteSale, VoidSale, OpenSession, CloseSession)                                                   | domain       | 1.5.9           | DONE        | 17 use cases: CreateSale, AddLineItem, UpdateLineItem, RemoveLineItem, AddPayment, ConfirmSale, CompleteSale, VoidSale, GetSaleById, GetSalesByOutlet, OpenCashierSession, CloseCashierSession, GetCurrentCashierSession, GetTablesByOutlet, GetSalesChannels, SaveSalesChannel, DeactivateSalesChannel                                                                                 |
| 1.5.11 | Data: Room entities + DAOs for Sale, OrderLine, Payment, CashierSession, SalesChannel                                                                        | data         | 1.5.9, 1.1.4    | DONE        | SaleEntity (channelId, receiptNumber, notes), OrderLineEntity (notes, modifiers JSON), PaymentEntity (PaymentId), CashierSessionEntity (CashierSessionId PK, closing fields), SalesChannelEntity (flattened PlatformConfig), TableEntity + DAOs. Sync metadata on all. DB v10                                                                                                           |
| 1.5.12 | Data: Repository implementations + mappers                                                                                                                   | data         | 1.5.11          | DONE        | SaleRepositoryImpl (atomic withTransaction: 3 DAOs), CashierSessionRepoImpl (getById, listByOutlet), SalesChannelRepoImpl, TableRepoImpl + TransactionMappers (SelectedModifier JSON via org.json) + SalesChannelMapper                                                                                                                                                                 |
| 1.5.13 | UI: PoS main screen (menu grid + cart)                                                                                                                       | presentation | 1.5.10, 1.4.7   | DONE        | Responsive layout: phone (BottomSheetScaffold cart, FAB) vs tablet (60/40 split panel). Category filter, search, SalesChannel selector. Auto-creates DRAFT Sale on first item tap                                                                                                                                                                                                       |
| 1.5.14 | UI: Payment screen (method selection, amount, change)                                                                                                        | presentation | 1.5.10          | DONE        | Responsive layout. 5 payment methods. Cash: quick denomination buttons + kembalian. Non-cash: autofill remaining amount. 2-step split payment (stage→review→BAYAR). ConfirmSale→AddPayment→CompleteSale flow                                                                                                                                                                            |
| 1.5.15 | UI: Receipt view (on-screen preview)                                                                                                                         | presentation | 1.5.10, 1.3.6   | DONE        | Merged into Payment success screen. Receipt card: outlet header, line items, tax/SC/tip breakdown, payment info, kembalian. Print button + auto-print                                                                                                                                                                                                                                   |
| 1.5.16 | UI: Open/close cashier session flow                                                                                                                          | presentation | 1.5.10          | DONE        | CashierSessionScreen: open dialog (float input), close dialog (expected vs actual cash, selisih, notes). Navigation gate before POS                                                                                                                                                                                                                                                     |
| 1.5.17 | UI: Transaction history list                                                                                                                                 | presentation | 1.5.10          | DONE        | TransactionHistoryScreen: filterable list by status/date, resume draft, resume payment. Accessible from Landing + POS TopBar (ReceiptLong icon). TransactionHistoryViewModel                                                                                                                                                                                                            |
| 1.5.18 | Receipt printing (Bluetooth thermal printer)                                                                                                                 | infra        | 1.5.15, 1.3.2   | DONE        | ESC/POS builder with raster bitmap (GS v 0). BluetoothPrinterService (SPP, 1.5s flush delay). buildSaleReceipt(): full receipt with header/items/tax/SC/tip/payment/footer. Modifier+add-on price detail: paid modifiers per-line with price, free modifiers inline, add-on with unit price breakdown. Auto-print. Multi-copy. LogoBitmapProcessor with Floyd-Steinberg dithering        |
| 1.5.19 | Unit tests: Sale aggregate, state machine, invariants, SalesChannel                                                                                          | test         | 1.5.1-8         | DONE        | 50 tests: SaleTest(34) — state machine, ID uniqueness, modifiers, updateLine/removeLine, subtotal/changeDue, multi-payment, discount, CashierSession reconciliation. SalesChannelTest(16) — factories, validation, price resolution (4 types), enum completeness                                                                                                                        |
| 1.5.20 | Integration tests: transaction flow end-to-end                                                                                                               | test         | 1.5.11-12       | DONE        | 14 tests: TransactionFlowIntegrationTest — full take-away flow, dine-in with table occupy/release, modifiers+add-ons price calc, update/remove lines, tax+SC computation, multi-payment, cash overpayment change, void+table release, inactive channel validation, modifier replacement. Fake repos pattern |


### 4.6 App Shell & Navigation


| #     | Task                                                       | Layer        | Depends On    | Status      | Notes                                                                                                                                          |
| ----- | ---------------------------------------------------------- | ------------ | ------------- | ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| 1.6.1 | Landing page / home screen (navigation hub)                | presentation | 1.1.5         | DONE        | 6-item grid: PoS, Katalog, Pelanggan, Laporan, Meja, Pengaturan. Shows user/outlet name, logout                                                |
| 1.6.2 | Navigation graph (all Phase 1 screens)                     | presentation | 1.6.1         | DONE        | All Phase 1 routes: auth (splash→onboarding→login→outlet→landing), settings/*, catalog/*, PoS (session→pos→payment/{saleId}). 22+ routes total |
| 1.6.3 | App theme & design system (colors, typography, components) | presentation | 1.1.1         | DONE        | Material 3, light/dark mode, green palette (#1B5E20)                                                                                           |
| 1.6.4 | Splash screen + first-run detection                        | presentation | 1.2.11        | DONE        | SplashScreen → CheckOnboardingNeededUseCase → route to onboarding or login                                                                     |
| 1.6.5 | Integrate license check into startup flow                  | presentation | 1.7.13, 1.6.4 | DONE        | SplashViewModel: license check → NeedActivation → ActivationScreen, or continue to onboarding/login. Background revalidation fire-and-forget   |


### 4.7 License & Activation (AppReg Integration)

> Ref: [docs/external-integration/android-integration.md](external-integration/android-integration.md)
>
> App membutuhkan lisensi aktif sebelum bisa digunakan. Integrasi dengan AppReg License Server
> menggunakan challenge-response, Play Integrity, Ed25519 signed license, offline verification.


| #      | Task                                                                                                                   | Layer        | Depends On    | Status      | Notes                                                                      |
| ------ | ---------------------------------------------------------------------------------------------------------------------- | ------------ | ------------- | ----------- | -------------------------------------------------------------------------- |
| 1.7.1  | Add dependencies: Retrofit, OkHttp, Gson, BouncyCastle, Play Integrity, security-crypto, coroutines-play-services      | infra        | 1.1.1         | DONE | Retrofit 2.11, OkHttp 4.12, BouncyCastle 1.78.1, Play Integrity 1.4.0, security-crypto 1.1.0-alpha06 |
| 1.7.2  | Setup build flavors (`dev` / `prod`) di `:app` module                                                                  | infra        | 1.7.1         | DONE | dev: dummy integrity, no cert pinning, .dev suffix. prod: real integrity + cert pinning |
| 1.7.3  | `AppConfig` — PUBLIC_KEY_HEX, CLOUD_PROJECT_NUMBER, CERTIFICATE_PINS, CERT_PIN_HOSTNAME                                | infra        | 1.7.2         | DONE | Semua dari custom.properties → BuildConfig → AppConfig. Tidak ada hardcode |
| 1.7.4  | `DeviceIdProvider` — Widevine ID with ANDROID_ID fallback                                                              | data         | 1.7.1         | DONE | Widevine DRM ID (MediaDrm) with Settings.Secure.ANDROID_ID fallback        |
| 1.7.5  | `PlayIntegrityHelper` — dev (dummy) + prod (real) via source sets                                                      | data         | 1.7.2         | DONE | src/dev/ returns dummy token, src/prod/ uses real Play Integrity API        |
| 1.7.6  | Network layer: `AppRegApi` (Retrofit interface) + `LicenseModule` (OkHttp + cert pinning)                              | data         | 1.7.1         | DONE | 4 endpoints. OkHttp with cert pinning from custom.properties. Hilt DI      |
| 1.7.7  | DTO & domain models: `ChallengeRequest/Response`, `ActivateRequest/Response`, `SignedLicenseDto`, `ValidationResponse` | data         | 1.7.6         | DONE | All DTOs with @SerializedName for Gson                                     |
| 1.7.8  | `LicenseStorage` — EncryptedSharedPreferences for signed license                                                       | data         | 1.7.1         | DONE | AES256_SIV key + AES256_GCM value. save/load/clear + lastOnlineCheck       |
| 1.7.9  | `LicenseVerifier` — offline Ed25519 signature verification + device binding + expiry check                             | domain       | 1.7.3         | DONE | BouncyCastle Ed25519Signer. verify() + isValid() convenience               |
| 1.7.10 | `ActivationRepository` — orchestrate challenge→integrity→activate→verify→save                                          | data         | 1.7.5-9       | DONE | Full flow with auto-reactivate on "already bound" error                    |
| 1.7.11 | `LicenseRevalidator` — periodic online check + 7-day offline grace period                                              | data         | 1.7.6, 1.7.8  | DONE | checkOnline() with grace period. Fire-and-forget on app launch             |
| 1.7.12 | UI: Activation screen (SN input → activate → success/error)                                                            | presentation | 1.7.10        | DONE | ActivationScreen + ActivationViewModel. Material 3 UI                      |
| 1.7.13 | App startup flow: license check → activation or main app                                                               | presentation | 1.7.9, 1.7.11 | DONE | SplashViewModel checks license → NeedActivation or continue. Navigation wired |
| 1.7.14 | Unit tests: LicenseVerifier, LicenseRevalidator grace period logic                                                     | test         | 1.7.9, 1.7.11 | DONE | 23 tests: LicenseVerifierTest(14) — Ed25519 sign/verify, tampered payload, wrong key, device binding, expiry, hex encoding, full flow. LicenseRevalidatorTest(9) — online valid/revoked, offline grace period (1d/6d/8d/30d), no license, never checked, constant |


### Phase 1 Acceptance Criteria

- Aplikasi bisa diinstall di Android device/emulator
- First-run wizard: setup tenant, outlet, user
- Login via PIN
- License activation via serial number (AppReg)
- Offline license verification (Ed25519) at startup
- CRUD kategori dan menu item
- Buka sesi kasir dengan modal awal
- Buat transaksi: pilih item, bayar cash, cetak receipt (termasuk tax, service charge)
- Tax, service charge, dan tip terhitung otomatis berdasarkan Settings
- Lihat history transaksi
- Tutup sesi kasir dengan rekapitulasi
- Semua data persist di Room (survive app restart)
- Semua entity mempunyai sync metadata (ULID, syncStatus, terminalId)
- Unit test coverage: domain logic >= 80%

---

## 5. Phase 2 — Full PoS Features

> Goal: Fitur PoS lengkap untuk restoran F&B, masih standalone.
>
> **Note**: Beberapa domain model Phase 2 sudah ter-scaffold (models + repositories tanpa use cases/UI). Lihat "early" notes.

### 5.1 F&B Transaction Enhancements


| #      | Task                                                                                                                                    | Layer        | Depends On | Status | Notes                                                                                                                                                                                                                                       |
| ------ | --------------------------------------------------------------------------------------------------------------------------------------- | ------------ | ---------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2.1.1  | Domain: `Table` entity + `TableStatus` (AVAILABLE, OCCUPIED, RESERVED)                                                                  | domain       | Phase 1    | DONE   | Table(id, outletId, name, capacity, currentSaleId, isActive) + TableRepository + Room entity + DAO. TableStatus derived from currentSaleId (AVAILABLE/OCCUPIED/RESERVED)                                                                    |
| 2.1.2  | Domain: 3rd party delivery platform support — `PlatformConfig` (commission%, settlement flow), `PlatformPayment` VO, `SettlementStatus` | domain       | 1.5.5      | DONE   | PlatformConfig VO on SalesChannel. PlatformPayment + PlatformSettlement models with full calculate(). 7 settlement use cases                                                                                                                |
| 2.1.2b | Domain: `PriceList` aggregate + `PriceListEntry` — harga per item per channel (full override, bukan hanya markup)                       | domain       | 1.5.5c     | DONE   | PriceList + PriceListEntry domain models + PriceListRepository. Full data layer: PriceListEntity, PriceListEntryEntity, DAO, mapper, repo impl, DI                                                                                          |
| 2.1.2c | UI: Platform channel setup wizard (nama platform, komisi%, markup%, payment method)                                                     | presentation | 2.1.2      | DONE   | SalesChannelSettingsScreen: 2-step wizard with platform presets (GoFood/GrabFood/ShopeeFood), commission%, payment method selector, pricing adjustments                                                                                     |
| 2.1.2d | Domain: Platform settlement tracking (AR/piutang platform, mark as settled, rekonsiliasi)                                               | domain       | 2.1.2      | DONE   | PlatformSettlementRepository with 12 methods incl. analytics. 7 use cases: Create, GetPending, GetSummary, MarkSettled, MarkDisputed, Cancel, BatchSettle                                                                                   |
| 2.1.2e | UI: Settlement reconciliation screen (list pending settlements, mark settled)                                                           | presentation | 2.1.2d     | DONE   | SettlementScreen with tabs (PENDING/SETTLED/ALL), channel filter, batch settle, dispute/cancel dialogs. SettlementViewModel with full state management                                                                                      |
| 2.1.3  | Domain: Dine-in flow (table selection, tableId on Sale)                                                                                 | domain       | 2.1.1      | DONE   | Sale.tableId + AssignTableUseCase + table release. TableMode enum (REQUIRED/OPTIONAL/NONE) on SalesChannel — independent from OrderFlow. Default: DINE_IN→REQUIRED, others→NONE. Configurable per channel in SalesChannelSettingsScreen. Table setup prompt when REQUIRED but no tables exist. POS navigates to Settings→Kelola Meja |
| 2.1.4  | Domain: Split bill support                                                                                                              | domain       | 1.5.1      | DONE   | SplitBill + SplitBillEntry models. 3 strategies: EQUAL, BY_ITEM, BY_AMOUNT. InitSplitBillUseCase (3 methods) + CancelSplitBillUseCase. Payment tracking per payerIndex. JSON serialization for Room                                         |
| 2.1.5  | Domain: Multiple payment methods per sale                                                                                               | domain       | 1.5.3      | DONE   | PaymentBreakdown + PaymentBreakdownEntry. Sale helpers: remainingAmount, paymentsByMethod, totalByMethod, cashTotal, nonCashTotal, isMixedPayment. Non-cash validation (cannot exceed remaining)                                            |
| 2.1.6  | Data: Table Room entity + DAO + repository                                                                                              | data         | 2.1.1      | DONE   | TableEntity + TableDao + TableRepositoryImpl already in Phase 1 data layer                                                                                                                                                                  |
| 2.1.7  | UI: Table map / grid (visual table layout)                                                                                              | presentation | 2.1.6      | DONE   | TableManagementScreen: grid with section filter, color-coded status, add/edit/delete dialogs, capacity display. TablePickerContent for POS bottom sheet. TableManagementViewModel. Settings → Kelola Meja navigation                        |
| 2.1.8  | UI: Channel selection on new sale (from configured SalesChannels)                                                                       | presentation | 1.5.5      | DONE   | ChannelSelectorBar + ChannelChip in PosTopBar. Scrollable LazyRow with icons per type, channel name, order flow subtitle. Auto-switch channel with cart preservation logic                                                                  |
| 2.1.9  | UI: Modifier selection during add-to-cart                                                                                               | presentation | 1.4.3      | DONE        | ModifierSelectionContent bottom sheet: groups with required/optional badges, single/multi-select per maxSelection, price delta display, running total, validation. PosViewModel loads links on-demand, items with modifiers always new line |
| 2.1.10 | UI: Add-on selection during add-to-cart                                                                                                 | presentation | 1.4.24     | DONE | AddOnGroupSection + AddOnItemRow with qty stepper, max qty enforcement, running total. Shown after modifier selection in ModifierAndAddOnSelectionContent. Items with add-ons always new line |
| 2.1.11 | UI: Add-on display in cart, receipt, kitchen ticket                                                                                     | presentation | 2.1.10     | DONE | Cart: add-on per line (+ name xqty @price) in tertiary color. Receipt UI: add-on lines under modifiers. SaleReceiptBuilder: add-on lines with total. KitchenOrderTicketBuilder: add-on lines per item |


### 5.2 Workflow / Kitchen Queue


| #     | Task                                                                                | Layer        | Depends On    | Status      | Notes                                                                                                               |
| ----- | ----------------------------------------------------------------------------------- | ------------ | ------------- | ----------- | ------------------------------------------------------------------------------------------------------------------- |
| 2.2.1 | Domain: `KitchenTicket` aggregate (status: PENDING -> PREPARING -> READY -> SERVED) | domain       | Phase 1       | DONE        | KitchenTicket + KitchenTicketItem + 4-state status enum + KitchenStationType (KITCHEN/BAR/GENERAL). Methods: startPreparing(), markReady(), markServed(), elapsedMillis(), prepTimeMillis() |
| 2.2.2 | Domain: Events (`WorkOrderCreated`, `WorkOrderStarted`, `WorkOrderCompleted`)       | domain       | 2.2.1         | DROPPED     | Tidak dibutuhkan. Kitchen queue pakai status field + Room Flow query untuk real-time update                          |
| 2.2.3 | Domain: Use cases (SendToKitchen, UpdateTicketStatus, GetActiveTickets)              | domain       | 2.2.1         | DONE        | SendToKitchenUseCase (delta items, auto ticket number), UpdateKitchenTicketStatusUseCase (all transitions + event publish), GetActiveKitchenTicketsUseCase (stream + one-shot) |
| 2.2.4 | Data: Room entities + DAOs + repository                                             | data         | 2.2.1         | DONE        | KitchenTicketEntity + KitchenTicketItemEntity (FK CASCADE), KitchenTicketDao (status queries, streamActive, nextTicketNumber), KitchenTicketRepositoryImpl, WorkflowMappers |
| 2.2.5 | UI: Kitchen Display Screen (KDS)                                                    | presentation | 2.2.4         | DONE        | KitchenDisplayScreen: real-time ticket grid (adaptive 280dp), color-coded status (red/orange/green), elapsed time ticker, action buttons per status, station filter chips (Semua/Dapur/Bar), empty state. KitchenDisplayViewModel with Flow observation. Accessible from Landing ("Dapur") |
| 2.2.6 | Printing: Kitchen ticket builder (KOT)                                              | infra        | 2.2.3, 1.5.18 | DONE        | KitchenOrderTicketBuilder: ESC/POS for 58mm, delta items only, ticket number, table/channel/cashier info, modifier+add-on+notes. Auto-print from POS sendToKitchen flow |


### 5.3 Customer Context


| #     | Task                                        | Layer  | Depends On   | Status  | Notes                                                                                                       |
| ----- | ------------------------------------------- | ------ | ------------ | ------- | ----------------------------------------------------------------------------------------------------------- |
| 2.3.1 | Domain: `Customer` aggregate + VOs          | domain | 1.1.7        | DONE    | Customer(id, tenantId, name, phone, email, address, loyaltyPoints) + Address VO                             |
| 2.3.2 | Domain: Link customer to Sale (optional)    | domain | 2.3.1, 1.5.1 | DONE    | Sale.customerId field exists                                                                                |
| 2.3.3 | Data + UI: Customer CRUD + selection in PoS | all    | 2.3.1-2      | DONE    | Data: CustomerEntity + DAO (search + delete) + Repo + Mappers + 3 use cases. UI: CustomerManagementScreen (list + search + card + form dialog + delete confirm) + CustomerViewModel. Landing "Pelanggan" card. Navigation: PosRoutes.CUSTOMER_MANAGEMENT |


### 5.4 Pricing & Promotion (Basic)


| #     | Task                                                                | Layer        | Depends On | Status      | Notes                                                                              |
| ----- | ------------------------------------------------------------------- | ------------ | ---------- | ----------- | ---------------------------------------------------------------------------------- |
| 2.4.1 | Domain: `DiscountType` enum + `DiscountInfo` VO + OrderLine methods | domain       | Phase 1    | DONE        | DiscountType(PERCENT, FIXED_AMOUNT), DiscountInfo(type, value, reason). OrderLine.applyDiscount(), clearDiscount(), subtotalBeforeDiscount(). Discount capped at subtotal |
| 2.4.2 | Domain: Apply discount via UpdateLineItemUseCase                    | domain       | 2.4.1      | DONE        | UpdateLineItemUseCase accepts DiscountInfo param, uses applyDiscount() to compute amount. lineTotal() includes discount |
| 2.4.3 | UI: Discount button in PoS (per line item)                          | presentation | 2.4.2      | DONE        | DiscountDialog: type selector (% / Rp), value input, reason, preview. Percent icon per cart line. Discount display: lineTotal + -amount red. Clear discount with X button |
| 2.4.4 | Domain: `PriceList` for happy hour / member price                   | domain       | 2.4.1      | DONE        | PriceList aggregate + PriceListEntry + data layer (entity, DAO, repo). Channel-level via SalesChannel.priceListId |


### 5.5 Inventory (Basic)


| #     | Task                                                        | Layer        | Depends On     | Status      | Notes                                                                              |
| ----- | ----------------------------------------------------------- | ------------ | -------------- | ----------- | ---------------------------------------------------------------------------------- |
| 2.5.1 | Domain: `StockLevel` aggregate + `StockMovement`            | domain       | 1.1.7          | DONE        | StockLevel (id, productId, outletId, productName, quantity, UoM, lowStockThreshold, isLowStock). StockMovement (id, 5 types: RECEIVE/SALE/ADJUSTMENT/WASTE/TRANSFER, notes, reference, timestamp). ULID IDs |
| 2.5.2 | Domain: `Recipe` + `RecipeLine` (ingredient, qty per porsi) | domain       | 1.4.2          | DONE        | Recipe + RecipeLine in CatalogModels (optional on MenuItem)                        |
| 2.5.3 | Domain: Auto-deduct stock on SaleCompleted (via recipe)     | domain       | 2.5.1-2, 1.5.7 | DEFERRED    | Deferred — requires Recipe CRUD UI. Manual adjustment sufficient for MVP           |
| 2.5.4 | Domain: Use cases (AdjustStock, ReceiveStock, GetStockLevels) | domain     | 2.5.1          | DONE        | AdjustStockUseCase (new qty + movement audit), ReceiveStockUseCase (add incoming), GetStockLevelsUseCase |
| 2.5.5 | Data + UI: Stock management screens                         | all          | 2.5.1-4        | DONE        | Room: StockLevelEntity + StockMovementEntity + DAOs + Repos + Mappers. DB v23. UI: StockManagementScreen (list + search + adjust/receive/waste dialog + add item). StockViewModel. Landing "Stok" card |
| 2.5.6 | UI: Low stock alert                                         | presentation | 2.5.1          | DONE        | StockLevel.isLowStock computed. Badge count on TopBar. Card highlight red for low stock items. Warning icon |


### 5.6 Accounting (Basic)


| #     | Task                                              | Layer        | Depends On   | Status      | Notes                                                                                                                    |
| ----- | ------------------------------------------------- | ------------ | ------------ | ----------- | ------------------------------------------------------------------------------------------------------------------------ |
| 2.6.1 | Domain: `Journal` + `JournalEntry` (double-entry) + `DailySummary` | domain | Phase 1 | DONE | Journal (outletId, entries, totalDebit/Credit, isBalanced). JournalEntry (accountId, accountName, debit/credit). DailySummary (date, totalSales, tax, SC, discount, netSales, count). Account + AccountType(5) |
| 2.6.2 | Domain: RecordSaleJournalUseCase                  | domain       | 2.6.1        | DONE        | Double-entry: Debit Cash/Receivable, Credit Revenue + Tax Payable + SC Revenue. Auto handles platform settlement split |
| 2.6.3 | Domain: COGS journal from recipe cost             | domain       | 2.6.1, 2.5.2 | DEFERRED    | Requires Recipe CRUD UI. Manual for MVP                                                                                  |
| 2.6.4 | Data + UI: Room entities + daily summary screen    | all          | 2.6.1-2      | DONE        | JournalEntity (entries as JSON) + AccountEntity + DAOs + Repos + Mappers. DB v24. SalesSummaryScreen (daily cards: gross/discount/net/tax/SC/count). GetDailySummaryUseCase. Landing "Laporan" card |


### 5.7 Reporting (Local)


| #     | Task                                                                                           | Layer        | Depends On | Status      | Notes                             |
| ----- | ---------------------------------------------------------------------------------------------- | ------------ | ---------- | ----------- | --------------------------------- |
| 2.7.1 | Daily sales summary report **per channel** (Dine In vs Take Away vs GoFood vs GrabFood vs ...) | presentation | Phase 1    | NOT_STARTED | Room query, group by channelId    |
| 2.7.2 | Product mix / best seller report (filterable per channel)                                      | presentation | Phase 1    | NOT_STARTED |                                   |
| 2.7.3 | Cashier session recap report                                                                   | presentation | 1.5.4      | NOT_STARTED |                                   |
| 2.7.4 | Platform commission report (total komisi per platform per periode)                             | presentation | 2.1.2      | NOT_STARTED | Gross vs Net revenue per platform |
| 2.7.5 | Platform settlement status report (pending vs settled per platform)                            | presentation | 2.1.2d     | NOT_STARTED | AR reconciliation                 |
| 2.7.6 | Export report to PDF / share                                                                   | infra        | 2.7.1-5    | NOT_STARTED |                                   |


### Phase 2 Acceptance Criteria

- Dine-in flow dengan table map
- Take away channel dengan harga berbeda dari dine-in (configurable)
- 3rd party delivery platform support (GoFood, GrabFood, ShopeeFood — masing-masing bisa beda harga)
- Platform commission tracking & settlement reconciliation
- Kitchen ticket auto-print
- Customer linked to transaction
- Manual discount (item & cart level)
- Stock auto-deduction via recipe
- Basic P&L report (per channel breakdown)
- Daily sales & product mix report (filterable per channel)
- Platform commission & settlement report

---

## 6. Phase 3 — Cloud Sync Foundation

> Goal: Bisa connect ke self-hosted cloud, sync data, migrasi standalone -> cloud.

### 6.1 Cloud API Server


| #     | Task                                                    | Layer   | Depends On | Status      | Notes                                     |
| ----- | ------------------------------------------------------- | ------- | ---------- | ----------- | ----------------------------------------- |
| 3.1.1 | Choose cloud stack (Ktor / Go / Node)                   | backend | —          | NOT_STARTED | ADR needed                                |
| 3.1.2 | Setup project structure, DB (PostgreSQL), migrations    | backend | 3.1.1      | NOT_STARTED |                                           |
| 3.1.3 | Auth endpoints (login, JWT, refresh, API key)           | backend | 3.1.2      | NOT_STARTED |                                           |
| 3.1.4 | Terminal registration endpoint                          | backend | 3.1.3      | NOT_STARTED | POST /api/terminals/register              |
| 3.1.5 | Sync push endpoint (POST /api/sync/push)                | backend | 3.1.3      | NOT_STARTED | Validate, apply, return accepted/conflict |
| 3.1.6 | Sync pull endpoint (GET /api/sync/pull)                 | backend | 3.1.3      | NOT_STARTED | Return changes since version              |
| 3.1.7 | Health & sync status endpoints                          | backend | 3.1.2      | NOT_STARTED |                                           |
| 3.1.8 | Tenant/outlet data isolation (tenant_id in all queries) | backend | 3.1.2      | NOT_STARTED |                                           |


### 6.2 Android Sync Implementation


| #      | Task                                             | Layer | Depends On     | Status      | Notes                                |
| ------ | ------------------------------------------------ | ----- | -------------- | ----------- | ------------------------------------ |
| 3.2.1  | `CloudSyncEngine` implementation                 | data  | 3.1.5-6, 1.1.8 | NOT_STARTED | Replaces NoOpSyncEngine when enabled |
| 3.2.2  | `SyncWorker` (WorkManager periodic + on-change)  | data  | 3.2.1          | NOT_STARTED |                                      |
| 3.2.3  | `SyncQueue` Room entity + DAO                    | data  | 1.1.4          | NOT_STARTED |                                      |
| 3.2.4  | `ChangeTracker` (detect PENDING_UPLOAD entities) | data  | 3.2.3          | NOT_STARTED |                                      |
| 3.2.5  | Push logic (batch changes, handle response)      | data  | 3.2.1-4        | NOT_STARTED |                                      |
| 3.2.6  | Pull logic (apply remote changes to Room)        | data  | 3.2.1          | NOT_STARTED |                                      |
| 3.2.7  | Conflict detection & LWW resolution              | data  | 3.2.5-6        | NOT_STARTED |                                      |
| 3.2.8  | `ConflictRecord` Room entity + DAO               | data  | 3.2.7          | NOT_STARTED |                                      |
| 3.2.9  | HTTP client setup (OkHttp/Ktor Client + JWT)     | data  | 3.1.3          | NOT_STARTED |                                      |
| 3.2.10 | Retry logic with exponential backoff             | data  | 3.2.2          | NOT_STARTED |                                      |
| 3.2.11 | Initial sync flow (first-time full download)     | data  | 3.2.6          | NOT_STARTED |                                      |
| 3.2.12 | Network connectivity monitor                     | data  | —              | NOT_STARTED | Online/offline state                 |


### 6.3 Android Sync UI


| #     | Task                                                                                                              | Layer        | Depends On | Status      | Notes                           |
| ----- | ----------------------------------------------------------------------------------------------------------------- | ------------ | ---------- | ----------- | ------------------------------- |
| 3.3.1 | UI: Cloud sync settings screen                                                                                    | presentation | 3.2.1      | NOT_STARTED | Enable/disable, URL, token      |
| 3.3.2 | UI: Sync status indicator (toolbar/statusbar)                                                                     | presentation | 3.2.1      | NOT_STARTED | Synced/pending/offline icon     |
| 3.3.3 | UI: Conflict resolution screen                                                                                    | presentation | 3.2.8      | NOT_STARTED | Side-by-side, choose resolution |
| 3.3.4 | UI: Migration wizard (standalone -> cloud)                                                                        | presentation | 3.2.11     | NOT_STARTED | Step-by-step flow               |
| 3.3.5 | Use cases: `EnableCloudSyncUseCase`, `DisableCloudSyncUseCase`, `TriggerSyncNowUseCase`, `ResolveConflictUseCase` | domain       | 3.2.1      | NOT_STARTED |                                 |


### Phase 3 Acceptance Criteria

- Cloud API server berjalan (health check OK)
- Terminal bisa register ke cloud
- Data ter-push ke cloud setelah transaksi
- Data dari cloud ter-pull ke device
- Conflict detected dan resolved (LWW)
- Sync status visible di UI
- Standalone -> cloud migration works
- Cloud -> standalone reversion works
- Offline operation unaffected saat cloud unreachable

---

## 7. Phase 4 — Multi-Terminal

> Goal: Multiple device bisa terkoneksi ke cloud dan bekerja bersamaan di 1 outlet.

### 7.1 Multi-Kasir


| #     | Task                                                       | Layer       | Depends On | Status      | Notes                  |
| ----- | ---------------------------------------------------------- | ----------- | ---------- | ----------- | ---------------------- |
| 4.1.1 | Terminal-scoped CashierSession (each terminal own session) | domain+data | Phase 3    | NOT_STARTED |                        |
| 4.1.2 | Transaction numbering with terminal code                   | domain      | 1.3.9      | NOT_STARTED | KMG-K1-..., KMG-K2-... |
| 4.1.3 | Shared product catalog real-time update                    | data        | 3.2.6      | NOT_STARTED |                        |
| 4.1.4 | Shared customer list real-time update                      | data        | 3.2.6      | NOT_STARTED |                        |


### 7.2 Waiter Terminal


| #     | Task                                                                | Layer        | Depends On     | Status      | Notes |
| ----- | ------------------------------------------------------------------- | ------------ | -------------- | ----------- | ----- |
| 4.2.1 | WAITER terminal type (canCreateOrder=true, canProcessPayment=false) | domain       | 1.2.4          | NOT_STARTED |       |
| 4.2.2 | Order creation from waiter -> sync -> cashier sees it               | data         | 4.2.1, Phase 3 | NOT_STARTED |       |
| 4.2.3 | Table assignment by waiter                                          | domain       | 2.1.1, 4.2.1   | NOT_STARTED |       |
| 4.2.4 | Real-time table status sync across terminals                        | data         | 4.2.3          | NOT_STARTED |       |
| 4.2.5 | UI: Waiter-optimized PoS layout (simplified)                        | presentation | 4.2.1          | NOT_STARTED |       |


### 7.3 Kitchen Display System (KDS)


| #     | Task                                                      | Layer        | Depends On     | Status      | Notes |
| ----- | --------------------------------------------------------- | ------------ | -------------- | ----------- | ----- |
| 4.3.1 | KITCHEN_DISPLAY terminal type (read-only tickets)         | domain       | 1.2.4          | NOT_STARTED |       |
| 4.3.2 | Real-time ticket sync (order -> kitchen display)          | data         | 4.3.1, Phase 3 | NOT_STARTED |       |
| 4.3.3 | Kitchen status update sync back (PREPARING -> READY)      | data         | 4.3.2          | NOT_STARTED |       |
| 4.3.4 | UI: Kitchen display screen (ticket cards, tap to advance) | presentation | 4.3.2          | NOT_STARTED |       |


### 7.4 Real-Time (SSE)


| #     | Task                                                         | Layer   | Depends On   | Status      | Notes          |
| ----- | ------------------------------------------------------------ | ------- | ------------ | ----------- | -------------- |
| 4.4.1 | Backend: SSE endpoint (GET /api/sync/stream)                 | backend | Phase 3      | NOT_STARTED |                |
| 4.4.2 | Android: SSE client integration                              | data    | 4.4.1        | NOT_STARTED | Auto-reconnect |
| 4.4.3 | Real-time notification: new order, order ready, table status | data    | 4.4.2        | NOT_STARTED |                |
| 4.4.4 | Fallback: periodic pull when SSE disconnected                | data    | 4.4.2, 3.2.2 | NOT_STARTED |                |


### Phase 4 Acceptance Criteria

- 2 kasir berjalan simultan, transaksi terpisah, no conflict
- Pelayan buat order dari tablet, muncul di kasir & kitchen
- Kitchen display tampilkan ticket, update status sync ke semua
- Table status real-time sync
- SSE real-time dengan graceful fallback

---

## 8. Phase 5 — Multi-Outlet & Multi-Tenant

> Goal: Support multiple cabang dan multiple bisnis pada 1 cloud instance.


| #   | Task                                                     | Layer          | Depends On | Status      | Notes                       |
| --- | -------------------------------------------------------- | -------------- | ---------- | ----------- | --------------------------- |
| 5.1 | Multi-outlet data scoping (strict outlet isolation)      | backend+data   | Phase 4    | NOT_STARTED |                             |
| 5.2 | Tenant-wide vs per-outlet product catalog (configurable) | domain+backend | 5.1        | NOT_STARTED |                             |
| 5.3 | Per-outlet pricing / price list                          | domain         | 5.2        | NOT_STARTED |                             |
| 5.4 | Cross-outlet reporting (cloud aggregation)               | backend        | 5.1        | NOT_STARTED |                             |
| 5.5 | Multi-tenant admin API + UI                              | backend        | Phase 3    | NOT_STARTED | Super-admin manages tenants |
| 5.6 | User assignment to multiple outlets                      | domain+backend | 5.1        | NOT_STARTED |                             |
| 5.7 | Per-outlet stock (terpisah per cabang)                   | domain+data    | 5.1        | NOT_STARTED |                             |
| 5.8 | Outlet selector in app (switch between outlets)          | presentation   | 5.1        | NOT_STARTED |                             |


### Phase 5 Acceptance Criteria

- 2 outlet terdaftar di 1 tenant, data terpisah
- Product catalog bisa tenant-wide atau per-outlet
- Reporting agregasi lintas outlet di cloud
- User bisa akses multiple outlet (with proper permissions)
- Stock terpisah per outlet

---

## 9. Cross-Cutting Concerns

Hal-hal yang harus diperhatikan di sepanjang semua phase:

### 9.1 Testing Strategy


| Level                   | Tool                           | Coverage Target                            | When              |
| ----------------------- | ------------------------------ | ------------------------------------------ | ----------------- |
| Unit test (domain)      | JUnit5 + MockK                 | >= 80% domain logic                        | Every PR          |
| Integration test (data) | AndroidX Test + Room in-memory | All DAOs, critical repo flows              | Every PR          |
| UI test (presentation)  | Compose Testing                | Critical user flows (create sale, payment) | Per milestone     |
| E2E test                | Manual / Maestro               | Full transaction flow                      | Per phase release |


### 9.2 Security

- PIN-based offline auth (SHA-256 hash stored in Room) — **Note**: Consider upgrading to bcrypt/argon2
- **License activation via AppReg** (challenge-response + Play Integrity + Ed25519 signed license) — See [4.7](#47-license--activation-appreg-integration)
- **Ed25519 offline license verification** at every startup (BouncyCastle)
- **Certificate pinning** for AppReg server (production builds only)
- **EncryptedSharedPreferences** for license storage (Android Keystore-backed)
- **Periodic online revalidation** with 7-day offline grace period
- **Build flavors** (dev/prod) — bypass security in dev, enforce in prod
- JWT token management for cloud sync (secure storage: EncryptedSharedPreferences)
- TLS/HTTPS for all cloud API communication
- SQL injection prevention (Room parameterized queries)
- Input validation at domain boundary
- Sensitive data encryption at rest (Room + SQLCipher) — evaluate necessity

### 9.3 Performance

- Room query optimization (indices on tenantId, outletId, status, categoryId, saleId)
- Pagination for large lists (transaction history, product catalog)
- Image optimization (menu item photos: compress, cache)
- Sync batch size tuning (maxBatchSize setting)
- Background sync must not affect UI responsiveness

### 9.4 Accessibility & UX

- Support landscape mode (tablet kasir)
- Large touch targets (PoS environment: wet/greasy hands)
- High contrast mode option
- Sound/vibration feedback for transaction events

---

## 10. Risk Register


| #   | Risk                                                                                        | Impact | Probability | Mitigation                                                                                                            | Status   |
| --- | ------------------------------------------------------------------------------------------- | ------ | ----------- | --------------------------------------------------------------------------------------------------------------------- | -------- |
| R1  | Room DB corruption on device                                                                | HIGH   | LOW         | Regular backup, export capability, Room integrity check                                                               | OPEN     |
| R2  | Sync conflicts cause data loss                                                              | HIGH   | MEDIUM      | Conservative conflict resolution (LWW), conflict log, manual review UI                                                | OPEN     |
| R3  | Bluetooth printer compatibility issues                                                      | MEDIUM | HIGH        | Abstract printer interface, test with multiple brands, USB fallback                                                   | OPEN     |
| R4  | Large dataset sync slow on first connect                                                    | MEDIUM | MEDIUM      | Paginated initial sync, progress indicator, background download                                                       | OPEN     |
| R5  | Scope creep in Phase 2 features                                                             | MEDIUM | HIGH        | Strict phase boundaries, defer non-essential features                                                                 | OPEN     |
| R6  | Cloud server downtime affects operations                                                    | HIGH   | LOW         | Offline-first design (mitigated by architecture), local queue                                                         | OPEN     |
| R7  | Device storage full (photos, transaction data)                                              | MEDIUM | MEDIUM      | Data retention policy, archive old transactions, image compression                                                    | OPEN     |
| R8  | **Sync metadata retrofit** — Adding sync fields to existing entities causes large migration | HIGH   | HIGH        | Should be done ASAP while data layer is small. Destructive migration OK during dev                                    | RESOLVED |
| R9  | **OrderChannel→SalesChannel migration** — Hardcoded enum deeply embedded in domain+data     | MEDIUM | HIGH        | Refactor early before more code depends on OrderChannel                                                               | RESOLVED |
| R10 | **AppReg server dependency** — License activation requires network access to AppReg server  | MEDIUM | LOW         | Offline grace period (7 days), signed license stored locally. Only affects first activation and periodic revalidation | OPEN     |
| R11 | **Play Integrity API changes** — Google may change API requirements                         | LOW    | LOW         | Abstract via PlayIntegrityHelper, dev flavor bypasses entirely                                                        | OPEN     |
| R12 | **Certificate pinning rotation** — Let's Encrypt cert renewal may break pinning             | MEDIUM | MEDIUM      | Use `reuse_key=True` in certbot, dual-pin migration strategy. See android-integration.md Section 6.5                  | OPEN     |


---

## 11. Definition of Done

Sebuah task dianggap `DONE` jika:

1. **Code** — Kode ditulis, di-review, dan di-merge ke branch utama
2. **Tests** — Unit test untuk domain logic, integration test untuk data layer (jika applicable)
3. **Docs** — Jika task mengubah arsitektur/API, update dokumen terkait
4. **CHANGELOG** — Entry ditambahkan di [CHANGELOG.md](../CHANGELOG.md)
5. **No regression** — Existing tests tetap pass
6. **Works offline** — Fitur berfungsi tanpa internet (offline-first)

---

## 12. Document References


| Document                                                                                   | Purpose                                     |
| ------------------------------------------------------------------------------------------ | ------------------------------------------- |
| [DDD_Core_Support_Architecture.md](DDD_Core_Support_Architecture.md)                       | Bounded contexts, aggregates, domain events |
| [DDD_FnB_Detail.md](DDD_FnB_Detail.md)                                                     | F&B specialization (channels, recipe, COGS) |
| [DDD_Architecture.md](DDD_Architecture.md)                                                 | Foundational DDD & ubiquitous language      |
| [Domain_Layer_Implementation_Guide.md](Domain_Layer_Implementation_Guide.md)               | Implementation checklist per context        |
| [Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md)     | Tech stack, layers, Kotlin conventions      |
| [Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md)       | Sync engine, conflict resolution, cloud API |
| [UseCases_Reference.md](UseCases_Reference.md)                                             | Use case signatures                         |
| [Module_Structure_Options.md](Module_Structure_Options.md)                                 | Module structure alternatives               |
| [GAP_ANALYSIS_2026-03-07.md](GAP_ANALYSIS_2026-03-07.md)                                   | Gap analysis: plan vs actual code           |
| [CHANGELOG.md](../CHANGELOG.md)                                                            | Implementation history                      |
| [external-integration/android-integration.md](external-integration/android-integration.md) | AppReg License Server integration guide     |
| [ADR/](adr/)                                                                               | Architecture Decision Records               |


---

> **How to update this document:**
>
> - Saat mulai task: ubah status `NOT_STARTED` -> `IN_PROGRESS`
> - Saat selesai: ubah status -> `DONE` dan checklist acceptance criteria
> - Saat blocked: ubah status -> `BLOCKED` dan tambah notes
> - Saat scope berubah: tambah/hapus task, update version dan date
> - Setiap update: ubah "Last updated" date dan version di header

