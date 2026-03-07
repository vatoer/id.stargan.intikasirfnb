# Implementation Status — IntiKasir F&B

> Dashboard status implementasi. Update setiap akhir sesi kerja.
>
> **Last updated**: 2026-03-08
> **Current Phase**: Phase 1 — Foundation & Standalone MVP (IN_PROGRESS)
> **Active Sprint**: Session open/close complete — next: Transaction history, License & Activation

---

## Quick Status

```
Phase 1: Foundation & Standalone MVP    [################....] 79%   IN_PROGRESS
Phase 2: Full PoS Features             [#...................] 5%    NOT_STARTED (early models scaffolded)
Phase 3: Cloud Sync Foundation          [....................] 0%    NOT_STARTED
Phase 4: Multi-Terminal                 [....................] 0%    NOT_STARTED
Phase 5: Multi-Outlet & Multi-Tenant    [....................] 0%    NOT_STARTED
```

> Progress bar: `#` = done, `.` = remaining.

---

## Current Focus

| Item | Detail |
|------|--------|
| **Working on** | Session open/close DONE. Next: Transaction history |
| **Blocked by** | — |
| **Next up** | 1. Transaction history  2. License & Activation  3. Phase 2 features |
| **Decisions needed** | — |

---

## Phase 1 Progress

### 1.1 Project Setup & Infrastructure

| Task | Status | Notes |
|------|--------|-------|
| Android project setup | DONE | Kotlin 2.3.10, Compose BOM 2026.02.00 |
| Module structure (:core:domain, :core:data, :app) | DONE | + :feature:identity |
| Hilt DI config | DONE | DatabaseModule, RepositoryModule, AppModule (identity + 9 transaction use cases) |
| Room database setup | DONE | Room v9, exportSchema=true, sync metadata on all entities, 20 tables. Destructive migration OK during dev |
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
| Tests | DONE | 9 tests: Terminal(6), User(3) |

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
| Domain (MenuItem, Category, Modifier) | DONE | MenuItem (imageUri added), Category, ModifierGroup + ModifierOption (separate entities, reusable across items), MenuItemModifierLink (junction with per-item overrides: isRequired, min/maxSelection) |
| Data layer | DONE | 5 entities (Category, MenuItem, ModifierGroup, ModifierOption, MenuItemModifierGroup), 5 DAOs, mappers, 3 repos. MenuItem: removed modifierGroupsJson, added imageUri. DB v9 |
| Use cases | DONE | 11 use cases: GetCategories, SaveCategory, DeleteCategory, GetMenuItems, GetMenuItemById, SaveMenuItem, DeleteMenuItem, GetMenuItemsByCategory, SearchMenuItems, SaveModifierGroup, GetModifierGroups, DeleteModifierGroup |
| UI: Category management | DONE | CategoryManagementScreen: CRUD dialog, hierarchical display, item count, active toggle, sortOrder, parent selector. CatalogMainScreen hub (Categories, Menu Items, Modifiers nav) |
| UI: Menu item management | DONE | MenuItemManagementScreen (list + category filter chips + search + active toggle + delete confirm) + MenuItemFormScreen (add/edit with image picker, category dropdown, price field). AsyncImage for photos, NumberFormat IDR. Navigation with itemId argument |
| UI: Modifier group management | DONE | ModifierGroupManagementScreen (list with inline options, price deltas, active toggle, delete confirm with FK warning) + ModifierGroupFormScreen (dynamic options list with mutableStateListOf, add/remove options, price delta per option). Navigation with groupId argument |
| Tests | DONE | 58 tests: CatalogModelsTest(17) — ID uniqueness, defaults, relationships. CatalogUseCaseTest(11) — all use cases with fake repos, tenant filtering, search, delete with link cleanup. EscPosBuilderTest(30) — ESC/POS commands, rasterImage, ReceiptConfig integration (header/footer/logo/NPWP/socialMedia) |

### 1.5 Transaction (Basic)

| Task | Status | Notes |
|------|--------|-------|
| Domain (Sale, OrderLine, Payment, CashierSession) | DONE | Sale aggregate root with state machine (DRAFT→CONFIRMED→PAID→COMPLETED/VOIDED). OrderLine with OrderLineId(ULID), SelectedModifier, effectiveUnitPrice(), modifierTotal(). Payment with PaymentId(ULID). CashierSession with CashierSessionId(ULID) PK, terminalId column, closing reconciliation (closingCash, expectedCash, cashDifference). Sale: addLine, updateLine, removeLine, confirm, addPayment, complete, void, subtotal, totalAmount, changeDue. 17 use cases |
| SalesChannel aggregate | DONE | SalesChannel entity replaces hardcoded OrderChannel enum. ChannelType(DINE_IN, TAKE_AWAY, DELIVERY_PLATFORM, OWN_DELIVERY), PriceAdjustmentType (4 types), PlatformConfig VO, resolvePrice(). Factory methods: dineIn(), takeAway(). Pre-seeded during onboarding. 3 use cases: Get/Save/Deactivate |
| TaxLine / ServiceChargeLine / TipLine | DONE | TaxLine.compute() (inclusive/exclusive), ServiceChargeLine.compute(), TipLine VO. Sale.applyTotals(), Sale.addTip()/removeTip(). Sale.taxTotal(), inclusiveTaxTotal(), serviceChargeAmount(), totalAmount() includes all. ConfirmSaleUseCase computes tax+SC from active TaxConfig + OutletSettings at confirmation time. CalculateSaleTotalsUseCase for preview. AddTipUseCase. All displayed in Payment + Receipt screens |
| State machine + invariants | DONE | Full state machine: DRAFT→CONFIRMED→PAID→COMPLETED, DRAFT/CONFIRMED→VOIDED. Channel validation dynamic via SalesChannel entity. Tax/SC computed at confirmation. Payment auto-transitions to PAID via isFullyPaidAfter(). Invariants: no mutations on non-DRAFT (except tip/payment on CONFIRMED), non-empty lines required for confirm |
| Data layer | DONE | Entities + DAOs + mappers + repos (incl. Table, SalesChannel). SaleRepositoryImpl uses withTransaction for atomic save. DB v10, 20 tables |
| UI: PoS main screen | DONE | Split layout: menu grid (left, category filter + search + LazyVerticalGrid) + cart panel (right, qty +/-, remove, subtotal, BAYAR button). SalesChannel selector in TopBar. Auto-creates draft Sale on first item tap, increments qty if same item tapped again. PosViewModel with 5 transaction use cases. Navigation wired from LandingScreen |
| UI: Payment screen | DONE | Split layout: order summary (left 45%) + payment input (right 55%). 5 payment methods (Tunai/Kartu/E-Wallet/Transfer/Lainnya) via FilterChip. Cash: quick denomination buttons (Uang Pas + smart roundups), kembalian card. Non-cash: reference input field. Flow: ConfirmSale (computes tax+SC) → AddPayment (auto PAID) → CompleteSale. Success screen with payment details + "Transaksi Baru" button. PaymentViewModel with SavedStateHandle for saleId nav arg. 4 new use case DI providers |
| UI: Receipt | DONE | Receipt preview screen (card layout: header/outlet info, line items with modifiers/notes/discount, tax/SC/tip breakdown, grand total, payment details + kembalian, footer). Print integration via ReceiptViewModel: loads OutletSettings + TerminalSettings, builds ESC/POS bytes via buildSaleReceipt(), auto-prints if autoPrintReceipt=true. Print button + print count indicator. "Transaksi Baru" button. Navigation: Payment → Receipt/{saleId} → fresh POS |
| UI: Session open/close | DONE | CashierSessionScreen: active session view (details + "Mulai Transaksi" + "Tutup Sesi") + no-session view ("Buka Sesi Kasir"). Open dialog: opening float input with IDR preview. Close dialog: expected cash display, actual cash input, selisih calculation (lebih/kurang/sesuai), notes field. CashierSessionViewModel with 3 use cases (Open/Close/GetCurrent). Navigation: Landing "POS" → CashierSession → POS. DI providers in AppModule |
| Receipt printing | DONE | ESC/POS builder (domain) with raster bitmap (GS v 0), BluetoothPrinterService (SPP, 1.5s flush delay), PrinterServiceFactory (BT discovery, BroadcastReceiver, StateFlow). ReceiptConfig-driven test print + sale receipt print. buildSaleReceipt() in domain/printer: full receipt with header (logo/name/address/NPWP), order info, line items (modifiers/notes/discount), tax/SC/tip, payment, kembalian, footer. Auto-print on receipt screen load. Multi-copy support |
| Tests | DONE | 50 tests: SaleTest(34) — state machine, ID uniqueness, modifiers, updateLine/removeLine, subtotal/changeDue, multi-payment, discount, CashierSession reconciliation. SalesChannelTest(16) — factories, validation, price resolution (4 types), enum completeness |

### 1.7 License & Activation (AppReg)

| Task | Status | Notes |
|------|--------|-------|
| Dependencies (Retrofit, OkHttp, Gson, BouncyCastle, Play Integrity) | NOT_STARTED | |
| Build flavors (dev/prod) | NOT_STARTED | dev: dummy integrity. prod: real + cert pinning |
| AppConfig (PUBLIC_KEY_HEX, CLOUD_PROJECT_NUMBER, CERTIFICATE_PINS) | NOT_STARTED | |
| DeviceIdProvider (Widevine + ANDROID_ID fallback) | NOT_STARTED | |
| PlayIntegrityHelper (dev dummy + prod real) | NOT_STARTED | |
| Network layer (AppRegApi + NetworkModule + cert pinning) | NOT_STARTED | |
| DTO & models | NOT_STARTED | |
| LicenseStorage (EncryptedSharedPreferences) | NOT_STARTED | |
| LicenseVerifier (Ed25519 offline verification) | NOT_STARTED | |
| ActivationRepository (full activation flow) | NOT_STARTED | |
| LicenseRevalidator (periodic online + 7-day grace) | NOT_STARTED | |
| UI: Activation screen | NOT_STARTED | |
| App startup license check integration | NOT_STARTED | |
| Tests | NOT_STARTED | |

### 1.6 App Shell

| Task | Status | Notes |
|------|--------|-------|
| Landing page | DONE | 6-item grid with icons |
| Navigation graph | DONE | Auth + Settings + Catalog + PoS + Payment + Receipt + Session routes. 22 routes total |
| Theme & design system | DONE | Material 3, light/dark, green palette |
| Splash + first-run | DONE | CheckOnboardingNeededUseCase |

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
| Phase 1 tasks DONE | 73/92 (79%) | 92/92 (100%) |
| Phase 1 tasks PARTIAL | 0/92 (0%) | 0 |
| Domain unit tests | 157 (all passing) | >= 80% coverage |
| Data test coverage | 0% | >= 60% |
| Open blockers | 1 (B3: destructive migration) | 0 |
| ADRs documented | 7 | — |
| Bounded contexts (domain models) | 9/13 | 13 |
| Use cases implemented | 42 | — |
| UI screens implemented | 23 (auth + settings + catalog + PoS + Payment + Receipt + Session) | ~25 (Phase 1 complete) |
| Room tables | 20 | — |
| Room DB version | 10 | — |

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

---

## Upcoming Milestones

| Target | Milestone | Dependencies |
|--------|-----------|--------------|
| DONE | Architecture gaps closed (SalesChannel migration) | B2 resolved |
| DONE | Catalog UI working (CRUD categories + menu items + modifiers) | — |
| DONE | PoS main screen (menu grid + cart) | Catalog UI |
| DONE | Payment screen + confirmation | PoS screen |
| DONE | Receipt UI + print integration | Payment screen |
| DONE | First transaction end-to-end (PoS → payment → receipt print) | All done |
| DONE | Session open/close UI | Transaction domain |
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
