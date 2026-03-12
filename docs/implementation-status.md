# Implementation Status — IntiKasir F&B

> Dashboard status implementasi · Update setiap akhir sesi kerja.
>
> **Last updated**: 2026-03-12
> **Version**: 3.0.0
> **Current Phase**: Phase 1 (78%) + Phase 2 (50%)
> **Roadmap**: [implementation-plan.md](implementation-plan.md)

---

## Health Dashboard

| Indicator | Status | Detail |
|-----------|--------|--------|
| **Overall Progress** | 🟡 ON_TRACK | 46% (82/180 tasks done) |
| **Phase 1** | 🟡 IN_PROGRESS | 78% — blocked by License (1.7, 14 tasks) |
| **Phase 2** | 🟡 IN_PROGRESS | 50% — 2.1 complete, 2.2-2.7 in progress |
| **Open Blockers** | 🟢 LOW | 1 open (B3 — destructive migration, dev-only) |
| **Tech Debt** | 🟡 MEDIUM | 7 items tracked — 1 HIGH severity |
| **Test Coverage** | 🟡 NEEDS_ATTENTION | 157 unit tests, 0 integration, 0 UI |
| **CI/CD** | 🔴 MISSING | No automated pipeline |

---

## Current Focus

| Item | Detail |
|------|--------|
| **Terakhir selesai** | Phase 2.1 F&B Transaction Enhancements — 13/13 DONE |
| **Sedang dikerjakan** | Dokumentasi ulang (best practice format) |
| **Blocked by** | — |
| **Next up** | 1. Kitchen Queue (2.2) · 2. Customer UI (2.3) · 3. License & Activation (1.7) |
| **Decisions needed** | — |

---

## Quick Status

```
Phase 1: Foundation & Standalone MVP    ████████████████░░░░  78%  🟡 IN_PROGRESS
  ├── 1.1 Project Setup                ██████████████████░░  90%  🟡
  ├── 1.2 Identity & Access            ████████████████░░░░  79%  🟡
  ├── 1.3 Settings                     ██████████████████░░  91%  🟡
  ├── 1.4 Catalog                      ████████████████░░░░  79%  🟡
  ├── 1.5 Transaction                  █████████████████░░░  84%  🟡
  ├── 1.6 App Shell                    ████████████████████ 100%  🟢
  └── 1.7 License & Activation        ░░░░░░░░░░░░░░░░░░░░   0%  🔴

Phase 2: Full PoS Features             ██████████░░░░░░░░░░  50%  🟡 IN_PROGRESS
  ├── 2.1 F&B Transaction Enh.         ████████████████████ 100%  🟢
  ├── 2.2 Kitchen Workflow             ███████░░░░░░░░░░░░░  36%  🟡
  ├── 2.3 Customer                     ███████████████░░░░░  75%  🟡
  ├── 2.4 Pricing & Promotion          █████░░░░░░░░░░░░░░░  25%  🔴
  ├── 2.5 Inventory                    ██████░░░░░░░░░░░░░░  33%  🔴
  ├── 2.6 Accounting                   █████░░░░░░░░░░░░░░░  25%  🔴
  └── 2.7 Reporting                    ░░░░░░░░░░░░░░░░░░░░   0%  🔴

Phase 3: Cloud Sync Foundation          ░░░░░░░░░░░░░░░░░░░░   0%  ⚪ NOT_STARTED
Phase 4: Multi-Terminal                 ░░░░░░░░░░░░░░░░░░░░   0%  ⚪ NOT_STARTED
Phase 5: Multi-Outlet & Tenant          ░░░░░░░░░░░░░░░░░░░░   0%  ⚪ NOT_STARTED
```

---

## Phase 1 — Detailed Progress

### 1.1 Project Setup & Infrastructure — 90%

| Task | Status | Notes |
|------|--------|-------|
| Android project setup | ✅ DONE | Kotlin 2.3.10, Compose BOM 2026.02.00 |
| Module structure (:core:domain, :core:data, :app) | ✅ DONE | + :feature:identity |
| Hilt DI config | ✅ DONE | DatabaseModule, RepositoryModule, AppModule (60+ use case providers) |
| Room database setup | ✅ DONE | Room v19, exportSchema=true, 25 tables. **Destructive migration [B3]** |
| Jetpack Navigation | ✅ DONE | Navigation Compose, 22+ routes |
| ULID generator | ✅ DONE | ulid-creator 5.2.3, all 15 ID classes |
| Shared kernel (Syncable, Money, IDs) | ✅ DONE | Money, Syncable interface, SyncMetadata, SyncStatus, all VOs |
| SyncEngine interface + NoOpSyncEngine | ✅ DONE | domain/sync + data/sync + DI binding |
| Test infrastructure | ✅ DONE | JUnit 4.13.2, MockK 1.14.2, Turbine 1.2.0, coroutines-test |
| CI setup | ⬚ NOT_STARTED | [TD-05] |

### 1.2 Identity & Access — 79%

| Task | Status | Notes |
|------|--------|-------|
| Domain entities (Tenant, Outlet, User, Terminal) | ✅ DONE | All 4 entities + VOs + sync metadata |
| Terminal types & status | ✅ DONE | CASHIER, WAITER, KITCHEN_DISPLAY, MANAGER |
| Room entities + DAOs (4) | ✅ DONE | FK indices, sync columns |
| Repository interfaces + impls (4) | ✅ DONE | Tenant, Outlet, User, Terminal |
| Mappers | ✅ DONE | IdentityMappers.kt |
| Use cases (7) | ✅ DONE | GetTenant, GetUserByEmail, GetOutletsByTenant, LoginWithPin, SelectOutlet, CheckOnboarding, CompleteOnboarding |
| UI: Setup wizard (3-step) | ✅ DONE | Business info → outlet → owner+PIN |
| UI: Login screen | ✅ DONE | PIN entry + outlet picker + custom NumPad/PinDots |
| Domain events | ⬚ NOT_STARTED | Deferred to Phase 4 — use case orchestration sufficient for standalone |
| Tests | ✅ DONE | 9 tests: Terminal(6), User(3) |
| Integration tests: Room DAOs | ⬚ NOT_STARTED | [TD-04] |

### 1.3 Settings — 91%

| Task | Status | Notes |
|------|--------|-------|
| TenantSettings, OutletSettings | ✅ DONE | Currency, numbering, syncEnabled, timezone, SC, Tip, ReceiptConfig |
| TerminalSettings + PrinterConfig | ✅ DONE | NONE/BT/USB/NET, address, autoCut, density, autoPrint, copies, cashDrawer |
| TaxConfig entity | ✅ DONE | TaxConfigId(ULID), TaxScope, >1 active tax (PPN, PB1) |
| ServiceChargeConfig / TipConfig | ✅ DONE | VOs in OutletSettings, applicableChannelTypes |
| ReceiptConfig | ✅ DONE | Header (logo, NPWP, customLines), Body (9 toggles), Footer (QR, social), PaperWidth |
| NumberingSequenceConfig | ✅ DONE | prefix, paddingLength, nextNumber |
| Data layer | ✅ DONE | OutletSettingsEntity (full receipt columns), TerminalSettingsEntity (no FK), TaxConfigEntity |
| Use cases (6) | ✅ DONE | Get/Save TenantSettings, Get/Save OutletSettings, GetActiveTaxConfigs, SaveTaxConfig |
| UI: 7 settings screens | ✅ DONE | SettingsMain, OutletProfile, Receipt, Printer, Tax, SC, Tip. BT discovery, optimistic UI |
| NumberingSequence logic | ⬚ NOT_STARTED | Format: {OutletCode}-{TerminalCode}-{YYYYMMDD}-{Seq} |
| Tests | ✅ DONE | 21 tests |

### 1.4 Catalog — 79%

| Task | Status | Notes |
|------|--------|-------|
| MenuItem, Category, ModifierGroup/Option | ✅ DONE | Separate entities, reusable modifiers via junction table |
| MenuItemModifierLink (per-item overrides) | ✅ DONE | isRequired, min/maxSelection |
| Data layer (5 entities, 5 DAOs, 3 repos) | ✅ DONE | DB v9, 19 tables. imageUri, no modifierGroupsJson |
| Use cases (11) | ✅ DONE | Full CRUD + search + delete with link cleanup |
| UI: Category management | ✅ DONE | CRUD dialog, hierarchical, item count, sort, parent selector |
| UI: Menu item management + form | ✅ DONE | List + filter + search + add/edit (image picker, category dropdown) |
| UI: Modifier group management + form | ✅ DONE | Inline options + dynamic form (mutableStateListOf) |
| Domain events | ⬚ NOT_STARTED | Deferred to Phase 4 — not needed for standalone |
| Tests | ✅ DONE | 58 tests: Models(17), UseCases(11), EscPosBuilder(30) |
| Integration tests | ⬚ NOT_STARTED | [TD-04] |

### 1.5 Transaction — 84%

| Task | Status | Notes |
|------|--------|-------|
| Sale aggregate + state machine | ✅ DONE | DRAFT→CONFIRMED→PAID→COMPLETED/VOIDED |
| OrderLine + modifiers + price snapshot | ✅ DONE | OrderLineId(ULID), SelectedModifier, effectiveUnitPrice(), modifierTotal() |
| Payment (multi-method) | ✅ DONE | PaymentId(ULID), CASH/CARD/E_WALLET/TRANSFER/OTHER |
| TaxLine / ServiceChargeLine / TipLine | ✅ DONE | compute() methods, Sale.applyTotals(), frozen at confirmation |
| SalesChannel entity (configurable) | ✅ DONE | 4 ChannelTypes, 4 PriceAdjustmentTypes, resolvePrice(), factory methods, pre-seeded |
| CashierSession (open/close/reconciliation) | ✅ DONE | CashierSessionId(ULID) PK, closingCash, expectedCash, cashDifference() |
| Data layer (6 entities, 6 DAOs) | ✅ DONE | SaleRepositoryImpl with withTransaction{} atomic save |
| Use cases (36 total) | ✅ DONE | 17 core + 3 channel + 7 settlement + 7 table + 2 split |
| UI: PoS main screen (responsive) | ✅ DONE | Phone: BottomSheet + FAB. Tablet: 60/40 split. Auto-create draft, same-item increment |
| UI: Payment (2-step split) | ✅ DONE | StagedPayment → review → batch commit. Quick cash denominations, non-cash autofill |
| UI: Receipt + print | ✅ DONE | Merged into payment success. Auto-print, multi-copy, print count badge |
| UI: Cashier session open/close | ✅ DONE | Float input, reconciliation (selisih), notes |
| UI: Transaction history | ✅ DONE | List past sales |
| Receipt printing (ESC/POS + BT) | ✅ DONE | EscPosBuilder, BluetoothPrinterService (SPP, 1.5s flush), Floyd-Steinberg dithering |
| Domain events | ⬚ NOT_STARTED | Deferred to Phase 4 — side effects via use case orchestration |
| Tests | ✅ DONE | 50 tests: Sale(34), SalesChannel(16) |
| Integration tests | ⬚ NOT_STARTED | [TD-04] |

### 1.6 App Shell — 100% ✅

| Task | Status | Notes |
|------|--------|-------|
| Landing page (6-item grid) | ✅ DONE | POS, Catalog, Laporan, Pengaturan, Pelanggan, Sesi |
| Navigation graph (22+ routes) | ✅ DONE | All Phase 1 + 2.1 screens connected |
| Theme & design system | ✅ DONE | Material 3, light/dark, green palette |
| Splash + first-run detection | ✅ DONE | CheckOnboardingNeededUseCase |

### 1.7 License & Activation (AppReg) — 0%

| Task | Status | Notes |
|------|--------|-------|
| Dependencies (Retrofit, OkHttp, BouncyCastle, Play Integrity) | ⬚ NOT_STARTED | |
| Build flavors (dev/prod) | ⬚ NOT_STARTED | dev: dummy integrity. prod: real + cert pinning |
| AppConfig constants | ⬚ NOT_STARTED | |
| DeviceIdProvider | ⬚ NOT_STARTED | Widevine + ANDROID_ID fallback |
| PlayIntegrityHelper | ⬚ NOT_STARTED | src/dev/ vs src/prod/ |
| AppRegApi + NetworkModule + cert pinning | ⬚ NOT_STARTED | 4 endpoints |
| DTO & models | ⬚ NOT_STARTED | |
| LicenseStorage (EncryptedSharedPreferences) | ⬚ NOT_STARTED | |
| LicenseVerifier (Ed25519) | ⬚ NOT_STARTED | |
| ActivationRepository | ⬚ NOT_STARTED | |
| LicenseRevalidator (periodic + 7-day grace) | ⬚ NOT_STARTED | |
| UI: Activation screen | ⬚ NOT_STARTED | |
| App startup license check | ⬚ NOT_STARTED | |
| Tests | ⬚ NOT_STARTED | |

---

## Phase 2 — Detailed Progress

### 2.1 F&B Transaction Enhancements — 100% ✅

| Task | Status | Notes |
|------|--------|-------|
| Table entity + derived status | ✅ DONE | AVAILABLE/OCCUPIED via currentSaleId |
| Platform delivery + PlatformConfig | ✅ DONE | Commission%, settlement flow |
| PriceList (per-channel pricing) | ✅ DONE | Full data layer |
| Platform channel wizard | ✅ DONE | Presets GoFood/GrabFood/ShopeeFood |
| Platform settlement tracking (7 use cases) | ✅ DONE | Create, GetPending, Settle, BatchSettle, Dispute, Cancel, Summary |
| Settlement reconciliation UI | ✅ DONE | Tabs, batch settle, dispute/cancel |
| Dine-in flow (assign/release table) | ✅ DONE | AssignTableUseCase + auto-release |
| Split bill (3 strategies) | ✅ DONE | EQUAL, BY_ITEM, BY_AMOUNT + payerIndex |
| Multi-payment (breakdown + validation) | ✅ DONE | remainingAmount, isMixedPayment |
| Table data layer | ✅ DONE | Entity + DAO + repo |
| Table map / grid UI | ✅ DONE | Adaptive grid, color-coded, section filter |
| Channel selection UI | ✅ DONE | ChannelSelectorBar + ChannelChip, scrollable LazyRow |
| Modifier selection UI | ✅ DONE | Bottom sheet, required/optional, validation, price delta |

> **13/13 tasks DONE**

### 2.2 Workflow / Kitchen Queue — 8%

| Task | Status | Notes |
|------|--------|-------|
| KitchenTicket domain model + repo | 🔶 PARTIAL | Model exists. Status: PENDING, IN_PROGRESS, COMPLETED |
| Workflow event classes | ✅ DONE | `WorkflowEvents.kt` — 4 events defined. Subscribers deferred to Phase 4 |
| SendToKitchenUseCase | ✅ DONE | Use case orchestration — creates ticket on send-to-kitchen action |
| Room entity + DAO | ⬚ NOT_STARTED | |
| Kitchen display UI | ⬚ NOT_STARTED | |
| Kitchen ticket printing | ⬚ NOT_STARTED | |
| Sound/notification | ⬚ NOT_STARTED | |

### 2.3 Customer — 75%

| Task | Status | Notes |
|------|--------|-------|
| Customer domain model | ✅ DONE | Customer + Address VO |
| Sale.customerId | ✅ DONE | Optional link |
| Data layer (entity + DAO + repo + 3 use cases) | ✅ DONE | |
| Customer CRUD + selection UI | ⬚ NOT_STARTED | |

### 2.4 Pricing & Promotion — 25%

| Task | Status | Notes |
|------|--------|-------|
| Discount VO (item-level) | 🔶 PARTIAL | OrderLine.discountAmount exists, no Discount type |
| Cart-level discount | ⬚ NOT_STARTED | |
| Discount UI in PoS | ⬚ NOT_STARTED | |
| Coupon/voucher | ⬚ NOT_STARTED | Can defer |

### 2.5 Inventory — 33%

| Task | Status | Notes |
|------|--------|-------|
| StockLevel + StockMovement models | ✅ DONE | Domain models + repos |
| Recipe + RecipeLine | ✅ DONE | Optional on MenuItem |
| Auto-deduct on sale complete | ⬚ NOT_STARTED | Use case orchestration in `CompleteSaleUseCase` |
| Manual stock adjustment | ⬚ NOT_STARTED | |
| Room entities + UI | ⬚ NOT_STARTED | |
| Low stock alert | ⬚ NOT_STARTED | |

### 2.6 Accounting — 25%

| Task | Status | Notes |
|------|--------|-------|
| Journal + JournalEntry models | ✅ DONE | Double-entry, debit XOR credit |
| Auto-journal on sale complete | ⬚ NOT_STARTED | Use case orchestration in `CompleteSaleUseCase` |
| COGS from recipe | ⬚ NOT_STARTED | |
| P&L view | ⬚ NOT_STARTED | |

### 2.7 Reporting — 0%

| Task | Status | Notes |
|------|--------|-------|
| Daily sales summary per channel | ⬚ NOT_STARTED | |
| Product mix / best seller | ⬚ NOT_STARTED | |
| Cashier session recap | ⬚ NOT_STARTED | |
| Platform commission report | ⬚ NOT_STARTED | |
| Settlement status report | ⬚ NOT_STARTED | |
| Export to PDF / share | ⬚ NOT_STARTED | |

---

## Summary Tables

### Phase 1 by Section

| Section | Total | Done | Partial | Not Started | % | Health |
|---------|-------|------|---------|-------------|---|--------|
| 1.1 Project Setup | 10 | 9 | 0 | 1 | 90% | 🟢 |
| 1.2 Identity & Access | 14 | 11 | 0 | 3 | 79% | 🟡 |
| 1.3 Settings | 11 | 10 | 0 | 1 | 91% | 🟢 |
| 1.4 Catalog | 14 | 11 | 0 | 3 | 79% | 🟡 |
| 1.5 Transaction | 19 | 16 | 0 | 3 | 84% | 🟡 |
| 1.6 App Shell | 4 | 4 | 0 | 0 | 100% | 🟢 |
| 1.7 License & Activation | 14 | 0 | 0 | 14 | 0% | 🔴 |
| **Phase 1 Total** | **86** | **61** | **0** | **25** | **71%** | **🟡** |
| **Phase 1 excl. License** | **72** | **61** | **0** | **11** | **85%** | **🟢** |

### Phase 2 by Section

| Section | Total | Done | Partial | Not Started | % | Health |
|---------|-------|------|---------|-------------|---|--------|
| 2.1 F&B Transaction | 13 | 13 | 0 | 0 | 100% | 🟢 |
| 2.2 Kitchen Workflow | 7 | 2 | 1 | 4 | 36% | 🟡 |
| 2.3 Customer | 4 | 3 | 0 | 1 | 75% | 🟡 |
| 2.4 Pricing & Promotion | 4 | 0 | 2 | 2 | 25% | 🔴 |
| 2.5 Inventory | 6 | 2 | 0 | 4 | 33% | 🔴 |
| 2.6 Accounting | 4 | 1 | 0 | 3 | 25% | 🔴 |
| 2.7 Reporting | 6 | 0 | 0 | 6 | 0% | 🔴 |
| **Phase 2 Total** | **44** | **21** | **3** | **20** | **50%** | **🟡** |

### Overall Progress

| Phase | Total Tasks | Done | % | Health |
|-------|------------|------|---|--------|
| Phase 1 | 86 | 61 | 71% | 🟡 |
| Phase 2 | 44 | 21 | 50% | 🟡 |
| Phase 3 | 25 | 0 | 0% | ⚪ |
| Phase 4 | 17 | 0 | 0% | ⚪ |
| Phase 5 | 8 | 0 | 0% | ⚪ |
| **Grand Total** | **180** | **82** | **46%** | **🟡** |

---

## Key Metrics

| Metric | Current | Target | Trend | Health |
|--------|---------|--------|-------|--------|
| Domain unit tests | 157 | >= 200 | ↗ | 🟡 |
| Data layer tests | 0 | >= 60% coverage | → | 🔴 |
| UI tests | 0 | Critical flows | → | 🔴 |
| UI screens implemented | 26+ | ~30 (Phase 1+2 complete) | ↗ | 🟢 |
| Room tables | 25 | — | — | 🟢 |
| Room DB version | 19 | — | — | 🟢 |
| Use cases implemented | 60+ | — | — | 🟢 |
| Bounded contexts (domain models) | 9/12 | 12 | ↗ | 🟡 |
| ADRs documented | 7 | — | — | 🟢 |
| Documentation docs | 16 (numbered) | — | — | 🟢 |
| Mermaid diagrams | 46 (27 new + 19 legacy) | — | — | 🟢 |
| Open blockers | 1 (B3) | 0 | ↘ | 🟡 |
| Tech debt items | 7 (1 HIGH) | 0 HIGH | ↘ | 🟡 |

---

## Blockers

### Open

| # | Issue | Severity | Reported | Impact | Ref |
|---|-------|----------|----------|--------|-----|
| B3 | Destructive DB migration | 🟡 MEDIUM | 2026-03-07 | Data loss on schema change. OK during dev, **must fix before beta** | [TD-01] |

### Resolved

| # | Issue | Resolved | Resolution |
|---|-------|----------|------------|
| B1 | Sync metadata missing on all entities | 2026-03-07 | All 25 entities now have sync columns |
| B2 | OrderChannel enum vs SalesChannel entity | 2026-03-08 | Migrated to configurable SalesChannel aggregate |
| B4 | No ULID library (all IDs were UUID) | 2026-03-07 | ulid-creator 5.2.3, all 15 ID classes migrated |
| B5 | TerminalSettingsEntity FK to TerminalEntity | 2026-03-07 | FK removed (Terminal never created during onboarding). DB v8 |

---

## Technical Debt Summary

> Full register: [implementation-plan.md § Technical Debt Register](implementation-plan.md#12-technical-debt-register)

| ID | Description | Severity | Target Fix |
|----|-------------|----------|------------|
| TD-01 | Destructive DB migration | 🔴 HIGH | Before beta |
| TD-02 | PIN hashing SHA-256 (not bcrypt/argon2) | 🟡 MEDIUM | Before production |
| TD-03 | Domain event subscribers not wired (infrastructure exists, not needed until multi-terminal) | 🟢 LOW | Phase 4 |
| TD-04 | Zero integration tests | 🟡 MEDIUM | Phase 2 |
| TD-05 | No CI pipeline | 🟡 MEDIUM | Phase 2 |
| TD-06 | BT print 1.5s sleep workaround | 🟢 LOW | Phase 3 |
| TD-07 | No pagination on large lists | 🟢 LOW | Phase 2.7 |

---

## Upcoming Milestones

| Priority | Milestone | Dependencies | Est. Effort | Target |
|----------|-----------|--------------|-------------|--------|
| P1 | Phase 2.2 Kitchen Queue (data layer + basic UI) | 2.1 done | Medium | — |
| P1 | Phase 2.3 Customer UI (CRUD + selection in PoS) | Customer data layer done | Small | — |
| P1 | Phase 2.7 Basic Reporting (sales summary, session recap) | Transaction data | Medium | — |
| P2 | Phase 1.7 License & Activation (14 tasks) | AppReg server ready (external) | Large | — |
| P2 | [TD-01] Proper Room migrations | — | Small | Before beta |
| P2 | Phase 2.4 Discount UI | — | Small | — |
| P3 | Phase 2.5 Inventory data layer + UI | — | Medium | — |
| P3 | Phase 2.6 Accounting (auto-journal) | 2.5 Inventory | Medium | — |
| — | **Phase 1 MVP release** | All 1.x tasks complete | — | Q2 2026 |

---

## Completed Milestones

| Date | Milestone | Impact |
|------|-----------|--------|
| 2026-03-12 | Documentation restructured — best practice format (16 numbered docs, 27 new diagrams) | High |
| 2026-03-08 | Phase 2.1 F&B Transaction Enhancements complete (13/13 tasks) | High |
| 2026-03-08 | Responsive POS layout (phone BottomSheet + tablet 60/40 split, 600dp breakpoint) | Medium |
| 2026-03-08 | Session open/close UI complete (float, reconciliation, selisih, notes) | Medium |
| 2026-03-08 | First transaction end-to-end (menu → cart → pay → print → new transaction) | High |
| 2026-03-08 | Payment screen + 2-step split payment (staged → review → commit batch) | Medium |
| 2026-03-08 | Receipt + print merged into payment success (auto-print, multi-copy, print count badge) | Medium |
| 2026-03-08 | PoS main screen complete (responsive phone/tablet, auto-create DRAFT, same-item increment) | High |
| 2026-03-08 | Transaction domain improved (OrderLineId ULID, SelectedModifier, CashierSessionId PK, atomic save) | Medium |
| 2026-03-08 | SalesChannel aggregate replaces hardcoded OrderChannel enum (B2 resolved) | High |
| 2026-03-07 | Receipt printing enhanced (raster bitmap GS v 0, Floyd-Steinberg dithering, logo caching) | Medium |
| 2026-03-07 | BT printing working (ESC/POS + SPP, test print on physical thermal printer) | High |
| 2026-03-07 | Catalog UI complete (4 screens: category, menu item, modifier group + forms) | High |
| 2026-03-07 | Settings UI complete (7 screens: outlet, receipt, printer, tax, SC, tip) | High |
| 2026-03-07 | Architecture gaps closed (ULID, sync metadata, Terminal entity, SyncEngine) | High |
| 2026-03-07 | Auth flow complete (splash → onboarding → login → outlet picker → landing) | High |
| 2026-03-07 | Documentation suite complete (11 docs + 19 diagrams + 7 ADRs) | Medium |

---

## Decision Log

| Date | Decision | ADR | Context |
|------|----------|-----|---------|
| 2026-03-07 | Offline-first with optional cloud sync | [ADR-001](adr/ADR-001-offline-first-architecture.md) | Arsitektur dasar |
| 2026-03-07 | ULID for all entity IDs | [ADR-002](adr/ADR-002-ulid-for-entity-ids.md) | Offline-safe, sortable, no coordination |
| 2026-03-07 | Single module per layer | [ADR-003](adr/ADR-003-single-module-per-layer.md) | Simplicity untuk tim kecil |
| 2026-03-07 | Self-hosted cloud API (no Firebase) | [ADR-004](adr/ADR-004-self-hosted-cloud-api.md) | No vendor lock-in, kontrol penuh |
| 2026-03-07 | Push-Pull sync with versioning | [ADR-005](adr/ADR-005-sync-push-pull-with-versioning.md) | Simple, proven pattern |
| 2026-03-07 | Terminal as first-class entity | [ADR-006](adr/ADR-006-terminal-as-first-class-entity.md) | Multi-device ready from day 1 |
| 2026-03-07 | Room as local database | [ADR-007](adr/ADR-007-room-as-local-database.md) | Android standard, good Kotlin support |

---

## Notes & Observations

- **2026-03-12**: Dokumentasi distruktur ulang mengikuti best practice — 16 numbered docs (00-13 + implementation), 27 diagram Mermaid baru (inline + file terpisah). Implementation plan diperkaya dengan phase gates, dependency map, tech debt register.
- **2026-03-08**: CashierSession PK changed dari TerminalId ke CashierSessionId(ULID) — sebelumnya 1-session-per-terminal-forever, sekarang support session history + reconciliation.
- **2026-03-08**: SalesChannel.requiresTable deferred ke Phase 2 — hardcoded `channelType == DINE_IN` menyebabkan error karena table management belum ada. Sekarang selalu false.
- **2026-03-08**: Payment screen redesigned dengan 2-step split pattern — StagedPayment (local, bukan persisted) → stage multiple → review → batch commit. Modern F&B PoS best practice.
- **2026-03-07**: Gap analysis menunjukkan domain+data layer ~40% done, ada gap arsitektur kritis (sync metadata, SalesChannel, Terminal). Prioritas: fix arsitektur gaps sebelum UI fitur baru.
- **2026-03-07**: Beberapa domain model Phase 2 sudah ter-scaffold lebih awal (Customer, Inventory, Accounting, Workflow, Table) — models + repos ada, tapi belum ada use cases, Room entities, atau UI.
- **2026-03-07**: PIN hashing pakai SHA-256 — consider bcrypt/argon2 sebelum production. [TD-02]
- **2026-03-07**: BT printing fix — `OutputStream.flush()` hanya flush Java buffer, bukan BT hardware transport. Solusi: 1.5s delay sebelum socket close. [TD-06]
- **2026-03-07**: Catalog modifier direfactor dari embedded JSON ke separate entities (reusable). Rationale: FnB modifiers (Size, Sugar Level) di-reuse across items — embedded JSON = duplication nightmare.

---

> **Cara update dokumen ini:**
> 1. Update "Last updated" date + "Current Phase" di header
> 2. Update "Health Dashboard" — review setiap indikator
> 3. Update "Current Focus" di awal sesi kerja
> 4. Update per-section task tables saat status berubah
> 5. Recalculate summary tables (totals + health indicators)
> 6. Tambah ke "Completed Milestones" saat milestone tercapai
> 7. Update "Open Blockers" dan "Technical Debt Summary" jika ada perubahan
> 8. Tambah ke "Notes & Observations" untuk insight selama dev
