# Implementation Status — IntiKasir F&B

> Dashboard status implementasi. Update setiap akhir sesi kerja.
>
> **Last updated**: 2026-03-15
> **Current Phase**: Phase 1 — Foundation & Standalone MVP (IN_PROGRESS)
> **Active Sprint**: Core PoS flow complete. Add-on CRUD done. Transaction history done. Modifier/add-on price detail on receipt/payment screens. TableMode on SalesChannel. Next: License & Activation, Kitchen Queue

---

## Quick Status

```
Phase 1: Foundation & Standalone MVP    [####################] 100%  DONE
Phase 2: Full PoS Features             [####################] 100%  DONE (5.1-5.6 complete)
Phase 3: Cloud Sync Foundation          [....................] 0%    NOT_STARTED
Phase 4: Multi-Terminal                 [....................] 0%    NOT_STARTED
Phase 5: Multi-Outlet & Multi-Tenant    [....................] 0%    NOT_STARTED
```

> Phase 1: ALL tasks DONE (98/98, excl 4 DROPPED domain events). Phase 2: 5.1 fully complete (15/15), 5.2-5.6 mostly NOT_STARTED.

---

## Current Focus

| Item | Detail |
|------|--------|
| **Working on** | **Phase 1 + Phase 2 COMPLETE.** All sections done: F&B Transaction, Kitchen, Customer, Pricing, Inventory, Accounting |
| **Blocked by** | — |
| **Next up** | 1. Phase 3 Cloud Sync Foundation  2. Phase 4 Multi-Terminal  3. Phase 5 Multi-Outlet |
| **Decisions needed** | — |

---

## Phase 1 Progress

### 1.1 Project Setup & Infrastructure

| Task | Status | Notes |
|------|--------|-------|
| Android project setup | DONE | Kotlin 2.3.10, Compose BOM 2026.02.00 |
| Module structure (:core:domain, :core:data, :app) | DONE | + :feature:identity |
| Hilt DI config | DONE | DatabaseModule, RepositoryModule, AppModule (identity + 9 transaction use cases) |
| Room database setup | DONE | Room v9, exportSchema=true, sync metadata on all entities, 23 tables, DB v22. Destructive migration OK during dev |
| Jetpack Navigation | DONE | Navigation Compose, 18+ routes (auth + settings + catalog + PoS) |
| ULID generator | DONE | UlidGenerator utility + ulid-creator 5.2.3. All 15 ID classes migrated from UUID |
| Shared kernel (Syncable, Money, IDs) | DONE | Money, Syncable interface, SyncMetadata, SyncStatus, all ID VOs with ULID |
| SyncEngine interface + NoOpSyncEngine | DONE | SyncEngine in domain/sync, NoOpSyncEngine in data/sync, DI binding |
| Test infrastructure | DONE | JUnit 4.13.2, MockK 1.14.2, Turbine 1.2.0, kotlinx-coroutines-test |
| CI setup | NOT_STARTED | |

### 1.2 Identity & Access

| Task | Status | Notes |
|------|--------|-------|
| Domain entities (Tenant, Outlet, User) | DONE | All 3 entities + VOs |
| Domain entity: Terminal | DONE | Terminal + TerminalType + TerminalStatus. ULID ID |
| Room entities + DAOs | DONE | All 4 entities (Tenant, Outlet, User, Terminal) + sync metadata + DAOs |
| Repository interfaces + impls | DONE | 4/4: Tenant, Outlet, User, Terminal |
| Use cases | DONE | 7 use cases (incl Login, Onboarding) |
| UI: Setup wizard | DONE | 3-step onboarding |
| UI: Login screen | DONE | PIN login + outlet picker |
| Tests | DONE | 29 tests: Terminal(6), User(3), IdentityDaoIntegrationTest(20 androidTest — TenantDao, OutletDao, UserDao, TerminalDao, hierarchy) |

### 1.3 Settings

| Task | Status | Notes |
|------|--------|-------|
| Domain (TenantSettings, OutletSettings) | DONE | TenantSettings: currency, numbering, syncEnabled. OutletSettings: timezone, SC, Tip, ReceiptConfig |
| TerminalSettings + PrinterConfig | DONE | PrinterConfig: connectionType(NONE/BT/USB/NET), address, name, autoCut, density, autoPrint, copies, cashDrawer |
| TaxConfig entity | DONE | Separate entity: TaxConfigId(ULID), TaxScope, rate, inclusive flag. Supports >1 active tax |
| ServiceChargeConfig / TipConfig | DONE | Value objects in OutletSettings |
| ReceiptConfig | DONE | Header (logo, businessName, address, phone, NPWP, customLines), Body (9 show-toggles), Footer (text, barcode/QR, thankYou, socialMedia), PaperWidth (58mm/80mm) |
| Numbering sequence | DONE | NumberingSequenceConfig with prefix, padding, nextNumber |
| Data layer | DONE | OutletSettingsEntity (full receipt columns), TerminalSettingsEntity (no FK, indices only), TaxConfigEntity + DAOs + repos + mappers + DI. DB v8 |
| Use cases | DONE | SaveTaxConfig, GetActiveTaxConfigs, SaveOutletSettings, SaveTenantSettings |
| UI: Settings screen | DONE | 7 screens: SettingsMain, OutletProfile, Receipt, Printer, Tax, ServiceCharge, Tip. BT discovery, optimistic UI update, Coil 3.1.0 image handling, collapsible lists, FlowRow chips |
| Tests | DONE | 21 tests: TaxConfig(5), TaxConfigId(1), SC(1), Tip(1), OutletSettings(2), TenantSettings(2), ReceiptConfig(5), PrinterConfig(2), TerminalSettings(1), Numbering(1) |

### 1.4 Catalog

| Task | Status | Notes |
|------|--------|-------|
| Domain (MenuItem, Category, Modifier) | DONE | MenuItem (imageUri), Category, ModifierGroup (isRequired, minSelection, maxSelection on group level), ModifierOption (priceDelta), MenuItemModifierLink (simple junction, no per-item overrides) |
| Domain (AddOnGroup, AddOnItem) | DONE | AddOnGroup + AddOnItem (qty-based, own price, reusable). MenuItemAddOnLink junction. SelectedAddOn VO for OrderLine snapshot |
| Data layer (Modifier + Add-on) | DONE | 8 entities (Category, MenuItem, ModifierGroup, ModifierOption, MenuItemModifierGroup, AddOnGroup, AddOnItem, MenuItemAddOnGroup), DAOs, mappers, repos. DB v22 |
| Use cases | DONE | 14 use cases: GetCategories, SaveCategory, DeleteCategory, GetMenuItems, GetMenuItemById, SaveMenuItem, DeleteMenuItem, GetMenuItemsByCategory, SearchMenuItems, SaveModifierGroup, GetModifierGroups, DeleteModifierGroup, SaveAddOnGroup, GetAddOnGroups, DeleteAddOnGroup |
| UI: Category management | DONE | CategoryManagementScreen: CRUD dialog, hierarchical display, item count, active toggle, sortOrder, parent selector. CatalogMainScreen hub (Categories, Menu Items, Modifiers, Add-on nav) |
| UI: Menu item management | DONE | MenuItemManagementScreen (list + filter + search) + MenuItemFormScreen (image picker, category dropdown, modifier/add-on toggle checkboxes with rule info from group) |
| UI: Modifier group management | DONE | ModifierGroupManagementScreen (list with selection rules: "Pilih 1 · Wajib") + ModifierGroupFormScreen (options + selection type chips + required toggle + hint text) |
| UI: Add-on group management | DONE | AddOnGroupManagementScreen (list with inline items, price, maxQty) + AddOnGroupFormScreen (dynamic items list) |
| Tests | DONE | 106 tests: CatalogModelsTest(17), CatalogUseCaseTest(11), EscPosBuilderTest(30), AddOnModelTest(21), RoomDaoIntegrationTest(27 androidTest) |

### 1.5 Transaction (Basic)

| Task | Status | Notes |
|------|--------|-------|
| Domain (Sale, OrderLine, Payment, CashierSession) | DONE | Sale aggregate root with state machine (DRAFT→CONFIRMED→PAID→COMPLETED/VOIDED). OrderLine with OrderLineId(ULID), SelectedModifier, effectiveUnitPrice(), modifierTotal(). Payment with PaymentId(ULID). CashierSession with CashierSessionId(ULID) PK, terminalId column, closing reconciliation (closingCash, expectedCash, cashDifference). Sale: addLine, updateLine, removeLine, confirm, addPayment, complete, void, subtotal, totalAmount, changeDue. 17 use cases |
| SalesChannel aggregate | DONE | SalesChannel entity with ChannelType(4), OrderFlowType(3), TableMode(REQUIRED/OPTIONAL/NONE), PriceAdjustmentType(4), PlatformConfig VO, resolvePrice(). Factory: dineIn(), takeAway(). 3 use cases. **UI: SalesChannelSettingsScreen** with table mode selector for DINE_IN channels |
| TaxLine / ServiceChargeLine / TipLine | DONE | TaxLine.compute() (inclusive/exclusive), ServiceChargeLine.compute(), TipLine VO. Sale.applyTotals(), Sale.addTip()/removeTip(). Sale.taxTotal(), inclusiveTaxTotal(), serviceChargeAmount(), totalAmount() includes all. ConfirmSaleUseCase computes tax+SC from active TaxConfig + OutletSettings at confirmation time. CalculateSaleTotalsUseCase for preview. AddTipUseCase. All displayed in Payment + Receipt screens |
| State machine + invariants | DONE | Full state machine: DRAFT→CONFIRMED→PAID→COMPLETED, DRAFT/CONFIRMED→VOIDED. Channel validation dynamic via SalesChannel entity. Tax/SC computed at confirmation. Payment auto-transitions to PAID via isFullyPaidAfter(). Invariants: no mutations on non-DRAFT (except tip/payment on CONFIRMED), non-empty lines required for confirm |
| Data layer | DONE | Entities + DAOs + mappers + repos (incl. Table, SalesChannel). SaleRepositoryImpl uses withTransaction for atomic save. DB v10, 20 tables |
| UI: PoS main screen | DONE | Responsive layout: phone (BottomSheetScaffold cart with FAB badge, 100dp cards) vs tablet (60/40 split). Category filter + search + SalesChannel selector. Auto-creates DRAFT on first item tap. navigationBarsPadding() |
| UI: Payment screen | DONE | Responsive (phone: column + fixed bar, tablet: side-by-side). 5 methods. Cash: quick denominations + kembalian. Non-cash: autofill remaining. Multi-payment always on (no split toggle) — stage→review→BAYAR. RemovePaymentUseCase. Receipt + print merged into success screen |
| UI: Receipt | DONE | Merged into Payment success screen. Receipt card: outlet header, items with modifier/add-on price detail (paid modifiers per-line, free inline, add-on with unit price breakdown), tax/SC/tip, payment + kembalian, footer. buildSaleReceipt() ESC/POS. Auto-print. Multi-copy |
| UI: Session open/close | DONE | CashierSessionScreen: active session view (details + "Mulai Transaksi" + "Tutup Sesi") + no-session view ("Buka Sesi Kasir"). Open dialog: opening float input with IDR preview. Close dialog: expected cash display, actual cash input, selisih calculation (lebih/kurang/sesuai), notes field. CashierSessionViewModel with 3 use cases (Open/Close/GetCurrent). Navigation: Landing "POS" → CashierSession → POS. DI providers in AppModule |
| Receipt printing | DONE | ESC/POS builder + raster bitmap (GS v 0). BluetoothPrinterService (SPP, 1.5s flush). buildSaleReceipt(): full receipt header/items/tax/payment/footer. Auto-print on payment complete. Multi-copy. LogoBitmapProcessor (Floyd-Steinberg dithering) |
| Tests | DONE | 64 tests: SaleTest(34), SalesChannelTest(16), TransactionFlowIntegrationTest(14) — full e2e flow with fake repos: create→add→confirm(tax+SC)→pay→complete, dine-in table occupy/release, modifiers+add-ons, multi-payment, void |

### 1.7 License & Activation (AppReg)

| Task | Status | Notes |
|------|--------|-------|
| Dependencies | DONE | Retrofit 2.11, OkHttp 4.12, BouncyCastle 1.78.1, Play Integrity 1.4.0, security-crypto |
| Build flavors (dev/prod) | DONE | dev: dummy integrity, .dev suffix. prod: real integrity + cert pinning |
| AppConfig | DONE | All from custom.properties → BuildConfig: PUBLIC_KEY_HEX, CLOUD_PROJECT_NUMBER, CERT_PIN_HOSTNAME, CERTIFICATE_PINS |
| DeviceIdProvider | DONE | Widevine DRM ID with ANDROID_ID fallback |
| PlayIntegrityHelper | DONE | src/dev/ dummy token, src/prod/ real Play Integrity API |
| Network layer (AppRegApi + LicenseModule) | DONE | 4 endpoints. OkHttp cert pinning from custom.properties. Hilt DI |
| DTO & models | DONE | ChallengeRequest/Response, ActivateRequest/Response, SignedLicenseDto, ValidationResponse |
| LicenseStorage | DONE | EncryptedSharedPreferences (AES256_SIV key + AES256_GCM value) |
| LicenseVerifier | DONE | Ed25519 offline verification via BouncyCastle. verify() + isValid() |
| ActivationRepository | DONE | Full flow: challenge→integrity→activate→verify→save. Auto-reactivate fallback |
| LicenseRevalidator | DONE | Online check + 7-day offline grace period. Fire-and-forget on launch |
| UI: Activation screen | DONE | ActivationScreen + ActivationViewModel. SN input → loading → success/error |
| App startup license check | DONE | SplashViewModel: license check → NeedActivation or continue. Background revalidation |
| Tests | DONE | 23 tests: LicenseVerifierTest(14), LicenseRevalidatorTest(9) |

### 1.6 App Shell

| Task | Status | Notes |
|------|--------|-------|
| Landing page | DONE | 6-item grid with icons |
| Navigation graph | DONE | All Phase 1 routes: auth + settings + catalog + PoS + payment + session. 22+ routes total |
| Theme & design system | DONE | Material 3, light/dark, green palette |
| Splash + first-run | DONE | CheckOnboardingNeededUseCase |

---

## Phase 2 Progress

### 5.1 F&B Transaction Enhancements

| Task | Status | Notes |
|------|--------|-------|
| 2.1.1 Table entity + TableStatus | DONE | Table with derived status (AVAILABLE/OCCUPIED/RESERVED via currentSaleId) |
| 2.1.2 Platform delivery support | DONE | PlatformConfig, PlatformPayment, PlatformSettlement models + 7 use cases |
| 2.1.2b PriceList aggregate | DONE | Domain + full data layer (entity, DAO, repo, mapper, DI) |
| 2.1.2c Platform channel setup wizard | DONE | SalesChannelSettingsScreen with platform presets, commission%, payment method |
| 2.1.2d Platform settlement tracking | DONE | PlatformSettlementRepository (12 methods) + 7 use cases |
| 2.1.2e Settlement reconciliation UI | DONE | SettlementScreen with tabs, batch settle, dispute/cancel. SettlementViewModel |
| 2.1.3 Dine-in flow | DONE | AssignTableUseCase + table release on complete/void + TablePickerContent |
| 2.1.4 Split bill | DONE | SplitBill model (EQUAL/BY_ITEM/BY_AMOUNT) + Init/Cancel use cases + payerIndex tracking |
| 2.1.5 Multiple payment methods | DONE | PaymentBreakdown, remainingAmount, cashTotal, nonCashTotal, validation |
| 2.1.6 Table data layer | DONE | TableEntity + TableDao + TableRepositoryImpl |
| 2.1.7 Table map / grid UI | DONE | TableManagementScreen + TablePickerContent + Settings integration |
| 2.1.8 Channel selection UI | DONE | ChannelSelectorBar + ChannelChip with icons, order flow subtitle |
| 2.1.9 Modifier selection UI | DONE | ModifierSelectionContent bottom sheet, required/optional, price delta, validation |
| 2.1.10 Add-on selection UI | DONE | Combined with modifier in ModifierAndAddOnSelectionContent bottom sheet. Qty stepper, max qty, running total |
| 2.1.11 Add-on display in cart/receipt/kitchen | DONE | Cart: add-on per line with price. Receipt/Payment/ReceiptScreen: paid modifiers with price, free inline, add-on with unit price breakdown. Kitchen ticket: add-on per item |

> **5.1: 15/15 tasks DONE**

### 5.2 Workflow / Kitchen Queue

| Task | Status | Notes |
|------|--------|-------|
| 2.2.1 KitchenTicket domain | DONE | 4-state lifecycle (PENDING→PREPARING→READY→SERVED), KitchenStationType (KITCHEN/BAR/GENERAL), elapsed/prep time metrics |
| 2.2.2 Domain Events | DROPPED | Tidak dibutuhkan, pakai status field + Room Flow |
| 2.2.3 Use cases | DONE | SendToKitchenUseCase (delta items), UpdateKitchenTicketStatusUseCase (transitions + events), GetActiveKitchenTicketsUseCase (stream) |
| 2.2.4 Room data layer | DONE | KitchenTicketEntity + ItemEntity, DAO (streamActive, nextTicketNumber, status updates), Repo, Mappers |
| 2.2.5 Kitchen Display Screen | DONE | KDS: real-time grid, color-coded (red/orange/green), elapsed ticker, action buttons, station filter, empty state. Landing "Dapur" card |
| 2.2.6 Kitchen ticket printing | DONE | KitchenOrderTicketBuilder: ESC/POS 58mm, delta items, ticket number, modifiers+add-ons+notes |

> **5.2: 5/5 tasks DONE (1 DROPPED)**

### 5.3 Customer Context

| Task | Status | Notes |
|------|--------|-------|
| 2.3.1 Customer domain | DONE | Customer aggregate + Address VO + CustomerId. 3 use cases |
| 2.3.2 Link to Sale | DONE | Sale.customerId + Sale.customerName fields |
| 2.3.3 Data + UI: CRUD | DONE | CustomerEntity + DAO (search + delete) + Repo. CustomerManagementScreen (list + search + form dialog + delete). Landing "Pelanggan" card |

> **5.3: 3/3 tasks DONE**

### 5.4 Pricing & Promotion (Basic)

| Task | Status | Notes |
|------|--------|-------|
| 2.4.1 DiscountType + DiscountInfo | DONE | DiscountType(PERCENT, FIXED_AMOUNT), DiscountInfo(type, value, reason). OrderLine.applyDiscount(), clearDiscount(), subtotalBeforeDiscount() |
| 2.4.2 Apply via use case | DONE | UpdateLineItemUseCase accepts DiscountInfo, computes amount, capped at subtotal |
| 2.4.3 Discount UI in POS | DONE | DiscountDialog (type selector, value, reason, preview). Percent icon per cart line. -amount display in red |
| 2.4.4 PriceList | DONE | PriceList aggregate + data layer. Channel-level via SalesChannel.priceListId |

> **5.4: 4/4 tasks DONE**

### 5.5 Inventory (Basic)

| Task | Status | Notes |
|------|--------|-------|
| 2.5.1 Domain models | DONE | StockLevel (ULID, lowStockThreshold, isLowStock) + StockMovement (5 types, notes, reference, timestamp) |
| 2.5.2 Recipe | DONE | Recipe + RecipeLine in CatalogModels |
| 2.5.3 Auto-deduct on sale | DEFERRED | Requires Recipe CRUD UI. Manual adjustment sufficient for MVP |
| 2.5.4 Use cases | DONE | AdjustStockUseCase, ReceiveStockUseCase, GetStockLevelsUseCase |
| 2.5.5 Data + UI | DONE | StockLevelEntity + StockMovementEntity + DAOs + Repos + Mappers. DB v23. StockManagementScreen (list + search + adjust/receive/waste + add item). Landing "Stok" card |
| 2.5.6 Low stock alert | DONE | isLowStock computed, badge count, card highlight, warning icon |

> **5.5: 5/5 tasks DONE (1 DEFERRED)**

### 5.6 Accounting (Basic)

| Task | Status | Notes |
|------|--------|-------|
| 2.6.1 Domain models | DONE | Journal (outletId, isBalanced) + JournalEntry (debit XOR credit) + DailySummary + Account(5 types) |
| 2.6.2 RecordSaleJournal | DONE | Double-entry: Debit Cash/Receivable, Credit Revenue + Tax + SC. Handles platform split |
| 2.6.3 COGS journal | DEFERRED | Requires Recipe CRUD UI |
| 2.6.4 Data + UI | DONE | JournalEntity + AccountEntity + DAOs + Repos + Mappers. DB v24. SalesSummaryScreen (daily cards). Landing "Laporan" card |

> **5.6: 3/3 tasks DONE (1 DEFERRED)**

---

## Blockers & Issues

| # | Issue | Impact | Reported | Resolved | Notes |
|---|-------|--------|----------|----------|-------|
| B1 | Sync metadata missing on all entities | HIGH — bigger refactor the longer delayed | 2026-03-07 | 2026-03-07 | RESOLVED: All 13 entities now have sync metadata columns |
| B2 | OrderChannel enum vs SalesChannel entity | HIGH — architecture mismatch with docs | 2026-03-07 | 2026-03-08 | RESOLVED: SalesChannel aggregate replaces OrderChannel. ChannelType enum, PlatformConfig, price resolution, pre-seeding. DB v10 |
| B3 | Room destructive migration | MEDIUM — data loss on schema change | 2026-03-07 | — | OK during dev, must fix before beta |
| B4 | No ULID library | MEDIUM — all IDs are UUID, docs require ULID | 2026-03-07 | 2026-03-07 | RESOLVED: ulid-creator 5.2.3 integrated, all 15 ID classes use ULID |
| B5 | TerminalSettingsEntity FK to TerminalEntity | HIGH — all terminal settings saves silently fail | 2026-03-07 | 2026-03-07 | RESOLVED: FK removed, DB v8. Terminal record never created during onboarding |

---

## Key Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Phase 1 tasks DONE | 98/98 (100%) | 98/98 (100%) |
| Phase 1 tasks DONE (excl License) | 86/86 (100%) | 86/86 (100%) |
| Phase 1 License tasks DONE | 14/14 (100%) | 14/14 (100%) |
| Phase 1 tasks PARTIAL | 0/98 (0%) | 0 |
| Phase 2 Section 5.1 | 15/15 (100%) | 15/15 |
| Phase 2 Section 5.2 | 5/5 (100%) + 1 DROPPED | 6/6 |
| Phase 2 Section 5.3 | 3/3 (100%) | 3/3 |
| Phase 2 Section 5.4 | 4/4 (100%) | 4/4 |
| Phase 2 Section 5.5 | 5/5 (100%) + 1 DEFERRED | 6/6 |
| Phase 2 Section 5.6 | 3/3 (100%) + 1 DEFERRED | 4/4 |
| Phase 2 overall | 43/43 (100%) | 43/43 |
| Domain unit tests | 192 (all passing) | >= 80% coverage |
| App unit tests | 23 (all passing) | License tests |
| Data integration tests | 47 androidTest (all passing on Android 14+15) | >= 60% coverage |
| Open blockers | 1 (B3: destructive migration) | 0 |
| ADRs documented | 7 | — |
| Bounded contexts (domain models) | 9/13 | 13 |
| Use cases implemented | 55+ | — |
| UI screens implemented | 32 (auth + settings + catalog + PoS + Payment/Receipt + Session + Table + Settlement + History + Add-on + KDS + Customer + Stock + SalesSummary) | 32 |
| Room tables | 27 | — |
| Room DB version | 24 | — |

---

## Completed Milestones

| Date | Milestone | Notes |
|------|-----------|-------|
| 2026-03-07 | Documentation complete | All architecture docs, diagrams, ADRs, implementation plan |
| 2026-03-07 | Auth flow complete | Splash → Onboarding → Login → Outlet Picker → Landing |
| 2026-03-07 | Domain layer scaffolded | 9 contexts: Identity, Settings, Catalog, Transaction, Customer, Inventory, Supplier, Accounting, Workflow |
| 2026-03-07 | Data layer scaffolded | 13 tables, 13 DAOs, 11 repo impls, 5 mapper files |
| 2026-03-07 | Gap analysis v1.0 | Full code review vs implementation plan — see GAP_ANALYSIS_2026-03-07.md |
| 2026-03-07 | Phase 1.1 infra complete | ULID, Syncable, SyncEngine, sync metadata on all entities, 26 unit tests |
| 2026-03-07 | Phase 1.2 Identity complete | Terminal entity + TerminalType/Status + DAO + repo + mapper + DI + 9 tests |
| 2026-03-07 | Phase 1.3 Settings complete | TaxConfig, SC/Tip VOs, ReceiptConfig (header/body/footer/paper), PrinterConfig (BT/USB/NET), TerminalSettings, 6 use cases, DB v6, 21 tests |
| 2026-03-07 | Settings UI complete | 7 screens: SettingsMain, OutletProfile, Receipt, Printer, Tax, ServiceCharge, Tip. Shared components (TestPrintSection, StickyBottomSaveBar, SettingsCard). BT discovery + test print working. DB v8 (FK removed from TerminalSettingsEntity) |
| 2026-03-07 | Receipt printing (partial) | ESC/POS builder in domain, BluetoothPrinterService with flush delay fix, PrinterServiceFactory with BT discovery (BroadcastReceiver + StateFlow). Test print verified on physical printer |
| 2026-03-07 | Catalog modifier refactor | ModifierGroup/ModifierOption refactored from embedded JSON to separate entities (Option B). Reusable across items via junction table with per-item overrides. MenuItem: added imageUri, removed modifierGroupsJson. 5 new use cases (delete, search, modifier CRUD). DB v9, 19 tables |
| 2026-03-07 | Catalog UI complete | 4 new screens: MenuItemManagementScreen (list + filter + search), MenuItemFormScreen (add/edit + image picker + category dropdown), ModifierGroupManagementScreen (list with inline options), ModifierGroupFormScreen (dynamic options). 58 new tests (CatalogModels 17, CatalogUseCases 11, EscPosBuilder 30). Receipt printing enhanced: ReceiptConfig-driven buildTestReceipt, raster bitmap (GS v 0), logo caching with Floyd-Steinberg dithering |
| 2026-03-08 | Transaction domain improved | Sale aggregate: OrderLineId(ULID), PaymentId(ULID), SelectedModifier + modifier price calc, updateLine/removeLine, subtotal/changeDue. CashierSession: CashierSessionId(ULID) as PK (enables session history), closingCash/expectedCash/cashDifference reconciliation. SaleRepositoryImpl atomic save via withTransaction. 5 new use cases (UpdateLineItem, RemoveLineItem, GetSalesChannels, SaveSalesChannel, DeactivateSalesChannel). 34 SaleTests + 16 SalesChannelTests |
| 2026-03-08 | SalesChannel aggregate (B2 resolved) | Replaced hardcoded OrderChannel enum with configurable SalesChannel entity. ChannelType(4), PriceAdjustmentType(4), PlatformConfig VO, resolvePrice(). Factory: dineIn(), takeAway(). Pre-seeded during onboarding. SalesChannelEntity + DAO + repo + mapper. DB v10, 20 tables. Sale.channel→Sale.channelId(SalesChannelId). CreateSaleUseCase validates dynamically via SalesChannel entity |
| 2026-03-08 | PoS main screen complete | Split-panel layout: menu grid (left 60%, category filter chips + search + LazyVerticalGrid with image/initials) + cart panel (right 40%, qty +/-, remove, subtotal, BAYAR button). SalesChannel selector in TopBar (FilterChip). Auto-creates DRAFT Sale on first item tap, increments qty on re-tap. PosViewModel with 5 injected use cases (CreateSale, AddLineItem, UpdateLineItem, RemoveLineItem, GetSalesChannels). AppModule DI providers added. Navigation wired from LandingScreen "POS / Kasir" card. Coil AsyncImage for menu item photos |
| 2026-03-08 | Receipt UI + print integration complete | Receipt preview screen (card layout with outlet header, order lines, tax/SC/tip breakdown, payment info, kembalian). buildSaleReceipt() in domain/printer: full ESC/POS receipt builder taking Sale + OutletSettings + PrinterConfig + ReceiptConfig + logo. ReceiptViewModel: loads settings, auto-prints if autoPrintReceipt=true, multi-copy support, print status tracking. Navigation: Payment → Receipt/{saleId} → fresh POS. Receipt printing now fully integrated end-to-end (was only test print from Settings) |
| 2026-03-08 | First transaction end-to-end complete | Full flow working: Landing → POS (browse menu, add to cart) → Payment (confirm sale with tax/SC, select method, pay) → Receipt (preview + print) → new transaction. All domain use cases wired through UI |
| 2026-03-08 | Session open/close UI complete | CashierSessionScreen with open/close dialogs, CashierSessionViewModel with 3 use cases, navigation gate before POS (Landing → Session → POS). Open dialog: opening float input. Close dialog: expected vs actual cash with selisih calculation + notes. 3 new DI providers in AppModule |
| 2026-03-08 | Payment screen + confirmation complete | Split layout: order summary (left, line items + tax/SC breakdown + total) + payment input (right, method selector + cash/reference input + pay button). Full payment flow: ConfirmSale (tax+SC computation) → AddPayment (auto PAID) → CompleteSale (COMPLETED). Cash: quick denomination buttons with smart roundups, kembalian display. Non-cash: reference input per method. Success screen with check icon, payment details, kembalian, "Transaksi Baru" button. PaymentViewModel with SavedStateHandle for saleId navigation arg. 4 new use case DI providers (GetSaleById, ConfirmSale, AddPayment, CompleteSale). Navigation: POS → Payment/{saleId} → back to fresh POS on complete |
| 2026-03-08 | Responsive POS layout (phone/tablet) | BoxWithConstraints with 600dp breakpoint. Phone: BottomSheetScaffold (peek bar cart summary, FAB with cart badge, 100dp menu cards). Tablet: 60/40 split panel (140dp cards). navigationBarsPadding() on all bottom-anchored elements (snackbar, cart summary, FAB, payment bar). Cart summary shrunk fonts for 8-digit amounts |
| 2026-03-08 | 2-step split payment | Modern F&B PoS pattern: StagedPayment (local, not persisted) → stage multiple entries → review with remove → BAYAR batch commit. Non-cash autofills remaining amount. RemovePaymentUseCase + Sale.removePayment() added for domain completeness. Receipt preview merged into payment success screen |
| 2026-03-08 | Dine In channel fix | SalesChannel.requiresTable deferred to Phase 2 (always returns false). Table management not yet implemented — prevents "Table is required for Dine In" error |
| 2026-03-08 | Phase 2.1 F&B Transaction Enhancements complete | All 13 tasks DONE: split bill (3 strategies), multi-payment (breakdown+validation), table management (grid+picker+release), channel selection (scrollable bar+chips), modifier selection (bottom sheet+groups+required/optional+price delta), platform settlement (tracking+reconciliation UI+batch settle), PriceList data layer, platform channel wizard |
| 2026-03-15 | Modifier selection rules moved to group level | isRequired/minSelection/maxSelection moved from MenuItemModifierLink to ModifierGroup. Attach to menu item is now simple checkbox toggle. ModifierGroupFormScreen: selection type chips + required toggle |
| 2026-03-15 | TableMode on SalesChannel | TableMode enum (REQUIRED/OPTIONAL/NONE) independent from OrderFlow. DINE_IN default REQUIRED, configurable in SalesChannelSettingsScreen. Table setup prompt when REQUIRED but no tables exist, navigates to Settings→Kelola Meja |
| 2026-03-15 | Catalog Add-on complete (1.4.15-24) | AddOnGroup/AddOnItem entities, Room entities+DAOs+repos, 3 use cases, AddOnGroupManagementScreen+FormScreen, Add-on selection in POS combined with modifier in bottom sheet |
| 2026-03-15 | Transaction history accessible from POS | TransactionHistoryScreen accessible from Landing + POS TopBar (ReceiptLong icon). Resume draft/payment from history |
| 2026-03-15 | Receipt modifier/add-on price detail | All screens (SaleReceiptBuilder, PaymentScreen, ReceiptScreen): paid modifiers shown per-line with price, free modifiers inline, add-on with unit price breakdown. Consistent across print + UI |
| 2026-03-15 | Gradle configuration cache + parallel enabled | org.gradle.configuration-cache=true, org.gradle.parallel=true. Build time reduced significantly |
| 2026-03-15 | Phase 2.1 expanded to 15/15 tasks DONE | Add-on selection UI + Add-on display in cart/receipt/kitchen completed |
| 2026-03-15 | Transaction flow integration tests (1.5.20) | 14 e2e tests with fake repos: full take-away+dine-in flows, modifiers+add-ons, tax+SC, multi-payment, void+table release, error cases |
| 2026-03-15 | Room DAO integration tests (1.4.14) | 27 androidTest: SaleDao, OrderLineDao, PaymentDao, CategoryDao, TableDao, cross-DAO. In-memory DB. FK cascade verified. Tested on SM-X200 (Android 14) + Moto G45 (Android 15) |
| 2026-03-15 | Identity DAO integration tests (1.2.14) | 20 androidTest: TenantDao(4), OutletDao(3), UserDao(6), TerminalDao(5), hierarchy(1). Tested on Android 14+15 |
| 2026-03-15 | Phase 1 excl License 100% complete | All non-license Phase 1 tasks DONE (86/86) |
| 2026-03-15 | License & Activation verified + tests | All 14 licensing tasks verified DONE (code existed, plan not updated). AppConfig migrated to custom.properties (no hardcode). CERT_PIN_HOSTNAME + CERTIFICATE_PINS added to BuildConfig. 23 unit tests: LicenseVerifierTest(14) + LicenseRevalidatorTest(9) |
| 2026-03-15 | **PHASE 1 COMPLETE** | All 98 tasks DONE (4 domain event tasks DROPPED). Foundation & Standalone MVP milestone achieved |
| 2026-03-15 | Phase 2.2 Kitchen Queue complete | KitchenDisplayScreen (KDS): real-time ticket grid, color-coded status, elapsed time, station filter, action buttons. 5/5 done (1 DROPPED) |
| 2026-03-15 | Phase 2.3 Customer Context complete | CustomerManagementScreen: CRUD + search by name/phone + form dialog. CustomerDao: search + delete added. Landing "Pelanggan" card. 3/3 done |
| 2026-03-15 | Phase 2.4 Pricing & Promotion complete | DiscountType(PERCENT/FIXED) + DiscountInfo VO. OrderLine.applyDiscount()/clearDiscount(). DiscountDialog in POS: type selector, value, reason, preview. Percent icon per cart line. 4/4 done |

---

## Upcoming Milestones

| Target | Milestone | Dependencies |
|--------|-----------|--------------|
| DONE | Architecture gaps closed (SalesChannel migration) | B2 resolved |
| DONE | Catalog UI working (CRUD categories + menu items + modifiers) | — |
| DONE | PoS main screen (responsive phone/tablet) | Catalog UI |
| DONE | Payment screen + 2-step split payment | PoS screen |
| DONE | Receipt + print merged into payment success | Payment screen |
| DONE | First transaction end-to-end (PoS → payment → receipt print) | All done |
| DONE | Session open/close UI | Transaction domain |
| DONE | Responsive layout (phone BottomSheet + tablet split) | — |
| DONE | Phase 2.1 F&B Transaction Enhancements (15/15) | Phase 1 PoS |
| DONE | Transaction history (1.5.17) | Transaction domain |
| DONE | Catalog Add-on complete (1.4.15-24) | Catalog domain |
| DONE | Phase 2.2 Workflow / Kitchen Queue (5/5 + 1 DROPPED) | KDS screen, use cases, domain, data, printer |
| DONE | Phase 2.3 Customer Context (3/3) | CRUD UI + search + Landing |
| DONE | Phase 2.4 Pricing & Promotion (4/4) | Discount type + UI + PriceList |
| DONE | Phase 2.5 Inventory (5/5 + 1 DEFERRED) | StockLevel + StockMovement + Room + use cases + StockManagementScreen + low stock alert |
| 2026-03-15 | Phase 2.6 Accounting complete | Journal (double-entry) + DailySummary + RecordSaleJournalUseCase + GetDailySummaryUseCase + SalesSummaryScreen. DB v24. Landing "Laporan" card |
| 2026-03-15 | **PHASE 2 COMPLETE** | All 43 tasks DONE (3 DEFERRED: auto-deduct stock, COGS journal, domain events). Full PoS Features milestone achieved |
| — | Numbering sequence (1.3.9) | Settings domain |
| — | License & Activation (1.7.*) | External: AppReg server |
| — | Phase 1: Standalone MVP release | All 1.x tasks |

---

## Decision Log (Quick Reference)

Decisions made during implementation. For full rationale, see [ADR/](adr/).

| Date | Decision | ADR | Context |
|------|----------|-----|---------|
| 2026-03-07 | Offline-first with optional cloud sync | [ADR-001](adr/ADR-001-offline-first-architecture.md) | Architecture foundation |
| 2026-03-07 | ULID for all entity IDs | [ADR-002](adr/ADR-002-ulid-for-entity-ids.md) | Offline-safe, sortable |
| 2026-03-07 | Single module per layer | [ADR-003](adr/ADR-003-single-module-per-layer.md) | Simplicity for small team |
| 2026-03-07 | Self-hosted cloud API | [ADR-004](adr/ADR-004-self-hosted-cloud-api.md) | No vendor lock-in |
| 2026-03-07 | Push-Pull sync with versioning | [ADR-005](adr/ADR-005-sync-push-pull-with-versioning.md) | Simple, proven pattern |
| 2026-03-07 | Terminal as first-class entity | [ADR-006](adr/ADR-006-terminal-as-first-class-entity.md) | Multi-device ready |
| 2026-03-07 | Room as local database | [ADR-007](adr/ADR-007-room-as-local-database.md) | Jetpack standard |

---

## Notes & Observations

- **2026-03-07**: Gap analysis menunjukkan domain+data layer sudah ~40% done, tapi ada gap arsitektur kritis (sync metadata, SalesChannel, Terminal). UI baru auth flow saja. Prioritas: fix arsitektur gaps sebelum bangun UI fitur baru.
- **2026-03-07**: Beberapa domain model Phase 2 sudah ter-scaffold lebih awal (Customer, Inventory, Accounting, Workflow, Table) — models + repositories ada, tapi belum ada use cases, Room entities, atau UI.
- **2026-03-07**: PIN hashing pakai SHA-256 — consider upgrading ke bcrypt/argon2 sebelum production.
- **2026-03-07**: Phase 1.1 infra gaps closed: ULID (15 ID classes migrated), Syncable interface + SyncMetadata + SyncStatus, SyncEngine + NoOpSyncEngine, sync metadata columns on all 13 Room entities, Room DB bumped to v3 with exportSchema=true, 26 domain unit tests created and passing. Blockers B1 and B4 resolved.
- **2026-03-07**: Phase 1.3 Settings expanded with full Receipt & Printer config following modern FnB PoS best practices. ReceiptConfig (per outlet): header (logo, businessName, address, phone, NPWP, customLines), body (9 show-toggles), footer (text, barcode/QR, thankYou, socialMedia), PaperWidth (58mm/80mm). PrinterConfig (per terminal): connectionType (NONE/BT/USB/NET), autoCut, density, autoPrint, copies, cashDrawer. Room DB bumped to v6.
- **2026-03-07**: Settings UI fully implemented (7 screens). Key architectural decisions: (1) TerminalSettingsEntity FK constraints to TerminalEntity/OutletEntity removed — caused silent save failures since Terminal records were never created during onboarding. DB bumped to v8. (2) Optimistic UI update pattern in SettingsViewModel — update state immediately, persist in background, revert on failure. (3) Bluetooth discovery via BroadcastReceiver in PrinterServiceFactory (Singleton), exposed as StateFlow, collected in ViewModel. (4) OutletProfile separated from ReceiptConfig for cleaner UI. (5) Coil 3.1.0 for image handling (logo). (6) FlowRow for FilterChips to handle wrapping. (7) CollapsibleLists with AnimatedVisibility + rememberSaveable.
- **2026-03-07**: BT printing fix — `OutputStream.flush()` only flushes Java buffer, not BT hardware transport buffer. Added 1.5s delay before socket close in BluetoothPrinterService. Test print confirmed working on physical thermal printer.
- **2026-03-07**: BT permissions: BLUETOOTH_SCAN with neverForLocation (API 31+), ACCESS_FINE_LOCATION maxSdkVersion=30 (pre-API 31), BLUETOOTH_CONNECT (API 31+).
- **2026-03-07**: Catalog UI complete — 4 new screens (MenuItemManagement, MenuItemForm, ModifierGroupManagement, ModifierGroupForm). Key patterns: (1) Async form init for edit screens via `LaunchedEffect(existingItem)` + `formInitialized` flag. (2) `mutableStateListOf` for dynamic modifier options in form. (3) `ExposedDropdownMenuBox` with `MenuAnchorType.PrimaryNotEditable` for category selection. (4) Navigation with `navArgument` for edit routes (itemId, groupId). (5) Hand-written fake repositories for domain tests (not mocks). (6) Receipt printing now ReceiptConfig-driven with raster bitmap support (GS v 0 command), logo pre-caching via Floyd-Steinberg dithering in LogoBitmapProcessor. Total domain tests: 114.
- **2026-03-08**: Transaction domain significantly improved. Key changes: (1) OrderLine now has OrderLineId(ULID) — was index-based, enabling cart update/remove by ID. (2) SelectedModifier data class with priceDelta, lineTotal() now = (unitPrice + modifierTotal) * qty - discount. (3) CashierSession PK changed from TerminalId to CashierSessionId(ULID) — was 1-session-per-terminal-forever, now supports session history + reconciliation (closingCash, expectedCash, cashDifference). (4) SaleRepositoryImpl.save() wrapped in `database.withTransaction{}` for atomic delete+insert (was unprotected). (5) Sale gains updateLine(), removeLine(), subtotal(), changeDue(). (6) PaymentId(ULID) added.
- **2026-03-08**: SalesChannel replaces OrderChannel (blocker B2). Design: configurable entity with ChannelType enum (4 types), PriceAdjustmentType (MARKUP_PERCENT/FIXED, DISCOUNT_PERCENT/FIXED), PlatformConfig VO (for GoFood/GrabFood etc — platformName, commissionPercent, requiresExternalOrderId, autoConfirmOrder). resolvePrice(basePrice) computes channel-adjusted price. Factory methods dineIn()/takeAway() pre-seeded during onboarding via CompleteOnboardingUseCase. CreateSaleUseCase now loads SalesChannel entity and validates rules dynamically (requiresTable, requiresExternalOrderId) instead of hardcoded enum checks. DB v10, 20 tables.
- **2026-03-08**: Receipt UI + print integration implemented. Key design decisions: (1) Separate ReceiptScreen (not embedded in PaymentSuccess) — allows clean navigation (Payment → Receipt → fresh POS), receipt can be re-visited or printed multiple times. (2) `buildSaleReceipt()` function in domain/printer — pure Kotlin, mirrors TestReceiptBuilder pattern but takes real Sale object. Renders: outlet header (logo/name/address/phone/NPWP), order info (receipt number/date/cashier/channel/table), line items (name, qty x unitPrice, modifiers, notes, discount), subtotals (subtotal/tax lines/SC/tip), grand total, payment info (method/amount/reference/kembalian), footer (thank you, social media, custom text), auto-cut + cash drawer. (3) ReceiptViewModel loads OutletSettings + TerminalSettings separately from PaymentViewModel — decoupled concerns (payment flow vs print flow). Uses same PrinterServiceFactory + LogoBitmapProcessor pattern from SettingsViewModel. (4) Auto-print on screen load if `printer.autoPrintReceipt == true` — reduces cashier taps. (5) Multi-copy via `printer.receiptCopies` config. (6) Print status tracking (IDLE/PRINTING/SUCCESS/ERROR) with print count badge ("Dicetak 2x"). (7) Receipt preview as centered Card with 400dp max width — looks like a real receipt on screen. (8) Print button disabled with "Printer Belum Diatur" message if no printer configured. (9) Navigation: Payment.onPaymentComplete → Receipt/{saleId} (replaces Payment in backstack), Receipt.onDone → pop to POS inclusive + navigate fresh POS.
- **2026-03-08**: Payment screen + confirmation implemented. Key design decisions: (1) Split-panel layout (45% order summary / 55% payment input) — order summary shows line items with unit price x qty, tax/SC breakdown (from ConfirmSaleUseCase), grand total. (2) ConfirmSaleUseCase called on screen load — transitions DRAFT→CONFIRMED with computed TaxLine/ServiceChargeLine snapshots. This ensures tax+SC are always current even if settings changed after cart was built. (3) Cash payment: smart quick-cash denomination buttons via `buildQuickCashAmounts()` — rounds up total to nearest 1K/5K/10K/20K/50K/100K + adds common larger bills. "Uang Pas" button for exact amount. Kembalian (change due) displayed in tertiary container. (4) Non-cash methods: reference/approval input field with per-method placeholder label (no amount input needed — full amount assumed). (5) Payment flow is atomic: AddPaymentUseCase auto-transitions to PAID via `Sale.isFullyPaidAfter()`, then CompleteSaleUseCase transitions PAID→COMPLETED. (6) Success screen uses full-screen centered layout with CheckCircle icon, payment summary card, and "Transaksi Baru" button that pops back to fresh POS screen via `popBackStack(POS, inclusive=true) + navigate(POS)`. (7) PaymentViewModel uses `SavedStateHandle` to receive saleId from navigation argument — follows Android best practice for nav args in ViewModels. (8) `formatCashDisplay()` uses Indonesian number format for thousand separators in cash input. (9) Back navigation disabled on success screen to prevent returning to completed sale.
- **2026-03-08**: PoS main screen implemented. Key design decisions: (1) Split-panel layout with Row weights (0.6 menu / 0.4 cart) — designed for tablet landscape, works on phone portrait too. (2) Auto-create DRAFT Sale on first item tap — no explicit "new order" step, reduces friction. (3) Same-item re-tap increments qty (matched by productId + empty modifiers) instead of adding duplicate line. (4) SalesChannel selector as FilterChip in TopBar — quick switching between Dine In / Take Away / Delivery. (5) Cart panel with qty +/- controls, per-line remove, clear all — uses domain use cases (UpdateLineItem, RemoveLineItem) that persist to Room on every change. (6) Transaction use cases injected via AppModule @Provides — use cases don't have @Inject constructors, following existing pattern. (7) IDR currency formatting via NumberFormat.getCurrencyInstance(Locale("id", "ID")). (8) Menu item cards show AsyncImage (Coil) or 2-letter initials as fallback. (9) "BAYAR" button callback ready for Payment screen (onNavigateToPayment with saleId).
- **2026-03-08**: Responsive POS layout implemented. Phone uses BottomSheetScaffold with peek bar (80dp) cart summary + FAB with cart badge. Tablet keeps 60/40 split panel. BoxWithConstraints with 600dp breakpoint. Phone menu cards shrunk to 100dp/50dp (vs 140dp/80dp on tablet). Cart summary font sizes reduced for 8-digit IDR amounts. navigationBarsPadding() added to all bottom-anchored elements (snackbar hosts, cart summary pay button, payment action bars, FAB) to prevent Android nav bar overlap.
- **2026-03-08**: Payment screen redesigned with 2-step split payment flow following modern F&B PoS best practice. StagedPayment data class (local, not persisted to DB) allows cashier to stage multiple payment entries (Tunai + Kartu, etc), review them, remove mistakes, then commit all at once via BAYAR button. Non-cash methods now autofill remaining amount (editable). RemovePaymentUseCase + Sale.removePayment() added to domain for completeness. Receipt preview merged into payment success screen (was separate ReceiptScreen). PrintStatus enum: IDLE/PRINTING/SUCCESS/ERROR.
- **2026-03-08**: SalesChannel.requiresTable deferred to Phase 2. Was hardcoded `channelType == DINE_IN` which caused "Table is required for Dine In" error since table management doesn't exist yet. Now always returns false — table selection will be required when table management is implemented in Phase 2.
- **2026-03-08**: Phase 2.1 F&B Transaction Enhancements fully completed (13/13 tasks). Key implementations: (1) Split bill: SplitBill model with 3 strategies (EQUAL, BY_ITEM, BY_AMOUNT), SplitBillEntry with payerIndex tracking, JSON serialization for Room, InitSplitBillUseCase (3 methods) + CancelSplitBillUseCase. (2) Multi-payment: PaymentBreakdown + PaymentBreakdownEntry VOs, Sale helpers (remainingAmount, cashTotal, nonCashTotal, isMixedPayment, paymentsByMethod), non-cash validation (cannot exceed remaining). (3) Table management: TableManagementScreen with adaptive grid, section filter, color-coded status (green=available, red=occupied), add/edit/delete dialogs. TablePickerContent reusable bottom sheet for POS. Settings integration. (4) Channel selection: ChannelSelectorBar + ChannelChip in PosTopBar, scrollable LazyRow with per-type icons, order flow subtitle, channel switch with cart preservation. (5) Modifier selection: ModifierSelectionContent bottom sheet, modifier groups with required/optional badges, single/multi-select per maxSelection, price delta display, running total, validation (disabled confirm until required groups satisfied), PosViewModel on-demand link loading. (6) Platform settlement: Full reconciliation UI with tabs, batch settle, dispute/cancel. PriceList full data layer. DB bumped to v18.
- **2026-03-07**: Catalog modifier architecture refactored from embedded JSON (Option A) to separate entities (Option B). Rationale: FnB modifiers (Size, Sugar Level, Ice Level) are reused across many items — embedded JSON causes duplication nightmare and makes "rename modifier" require updating 20+ items. New schema: modifier_groups (tenant-scoped, reusable), modifier_options (FK CASCADE from group), menu_item_modifier_groups (junction with per-item overrides: isRequired, minSelection, maxSelection). OrderLine.modifierSnapshot remains JSON (transaction snapshot pattern unchanged). MenuItem gained imageUri field. DB v9.

---

> **How to update this document:**
> - Update "Last updated" date setiap kali edit
> - Update "Current Focus" section di awal setiap sesi kerja
> - Update task status saat mulai (IN_PROGRESS) dan selesai (DONE)
> - Progress bar: hitung persentase dari task yang DONE per phase
> - Tambah ke "Blockers & Issues" jika ada impediment
> - Tambah ke "Notes & Observations" untuk insight selama development
> - Catat ke "Completed Milestones" saat milestone tercapai
