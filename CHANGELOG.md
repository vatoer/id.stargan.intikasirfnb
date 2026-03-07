# Changelog

All notable changes to IntiKasir F&B will be documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> Living document. Update setiap kali ada perubahan yang di-merge.

---

## [Unreleased]

### Added
- **AppReg License integration plan** — 14 tasks added to Phase 1.7
  - Challenge-response activation with Play Integrity
  - Ed25519 signed license with offline verification
  - Device binding (Widevine ID / ANDROID_ID)
  - Periodic online revalidation with 7-day offline grace period
  - Build flavors (dev/prod) for security bypass in development
  - EncryptedSharedPreferences for license storage
  - Certificate pinning (production only)
  - Integration with app startup flow (license check before onboarding/login)
- Licensing as 13th bounded context (Support domain)
- 3 new risks: R10 (AppReg server dependency), R11 (Play Integrity API changes), R12 (cert pinning rotation)
- `SalesChannel` as configurable entity (replaces hardcoded OrderChannel enum)
  - `ChannelType`: DINE_IN, TAKE_AWAY, DELIVERY_PLATFORM, OWN_DELIVERY
  - Per-channel pricing: PriceList override OR markup/discount per channel
  - Dine In dan Take Away bisa beda harga (configurable)
  - 3rd party platform support: GoFood, GrabFood, ShopeeFood, dll. (unlimited)
  - `PlatformConfig`: commission%, settlement flow, auto-confirm
  - `PlatformPayment`: gross/net/commission tracking, settlement status
  - `ResolvePriceUseCase`: hitung harga efektif per channel + modifier
- Sales Channel use cases: CRUD, resolve price, deactivate
- Platform settlement & reconciliation model
- Per-channel reporting: sales per channel, commission report, settlement status
- Tax, Service Charge & Tip model (configurable di Settings)
  - `TaxConfig`: multiple taxes (PPN, PB1), per-scope (ALL_ITEMS/SPECIFIC_CATEGORIES/SPECIFIC_ITEMS), isIncludedInPrice
  - `ServiceChargeConfig`: rate, applicable channel types, isIncludedInPrice
  - `TipConfig`: suggested percentages, allowCustomAmount, applicable channel types
  - `TaxLine`, `ServiceChargeLine`, `TipLine` VOs on Sale/Order
  - Calculation order: subtotal → discount → service charge → tax → tip → grandTotal
  - Settings hierarchy: Tenant default with Outlet override
- Tax/SC/Tip use cases: `GetActiveTaxConfigsUseCase`, `SaveTaxConfigUseCase`, `CalculateSaleTotalsUseCase`, `AddTipUseCase`, etc.
- Gap Analysis document (`GAP_ANALYSIS_2026-03-07.md`)
  - Full source code review: domain, data, presentation layers
  - 78 Phase 1 tasks mapped: 31 DONE, 11 PARTIAL, 36 MISSING
  - Critical gaps identified: sync metadata, SalesChannel, Terminal entity, ULID, UI screens
  - Recommended action plan with priority ranking

### Changed
- `IMPLEMENTATION_PLAN.md` v1.0.0 → v1.1.0: updated all task statuses based on gap analysis
  - Phase 1 status: NOT_STARTED → IN_PROGRESS
  - 31 tasks marked DONE, 11 PARTIAL, remainder NOT_STARTED
  - Phase 1 acceptance criteria: 3/12 checked
  - Added 2 new risks: R8 (sync metadata retrofit), R9 (OrderChannel migration)
  - Added link to GAP_ANALYSIS_2026-03-07.md
  - Phase 2 early-implemented items noted (Table, Customer, Inventory, Accounting models)
  - Cross-cutting security/performance items checked where applicable
- `OrderChannel` enum replaced by `SalesChannel` entity (configurable, extensible)
- `CreateSaleUseCase` now takes `SalesChannelId` instead of `OrderChannel`
- `OrderLine.priceSnapshot` now includes channel-adjusted price (not just base price)
- Updated all architecture docs to reflect SalesChannel model
- Updated `fnb-02-transaction-channels.mmd` diagram with new channel architecture

---

## [0.0.0] - 2026-03-07

### Added
- Initial project setup with `.gitignore`
- DDD Architecture documentation suite:
  - `DDD_Architecture.md` — Foundational DDD & ubiquitous language
  - `DDD_Core_Support_Architecture.md` — Core vs Support domains, 12 bounded contexts
  - `DDD_FnB_Detail.md` — F&B specialization (channels, recipe, COGS)
  - `Domain_Layer_Implementation_Guide.md` — Implementation checklist per context
  - `Android_Kotlin_MVVM_Clean_Architecture.md` — Tech stack, layers, Kotlin conventions
  - `UseCases_Reference.md` — Use case signatures
  - `Module_Structure_Options.md` — Module structure alternatives
- Offline-First & Cloud-Sync Architecture documentation:
  - `Offline_First_Cloud_Sync_Architecture.md` — Sync engine, conflict resolution, cloud API design
  - Terminal entity design (CASHIER, WAITER, KITCHEN_DISPLAY, MANAGER)
  - Sync metadata specification (Syncable interface, ULID, SyncStatus)
  - Push-Pull sync strategy with conflict resolution matrix
  - Self-hosted cloud API endpoint design
  - Multi-terminal, multi-outlet, multi-tenant support design
- 19 Mermaid architecture diagrams:
  - 7 base DDD diagrams (domain classification, context map, transactions, catalog, identity, integration, line mapping)
  - 3 F&B diagrams (context overview, channels, recipe/COGS)
  - 3 Android architecture diagrams (clean architecture, MVVM flow, feature modules)
  - 6 Sync diagrams (engine architecture, push/pull, conflict resolution, topology, initial sync, offline/online state)
- Implementation planning documentation:
  - `IMPLEMENTATION_PLAN.md` — 5-phase roadmap with detailed tasks
  - `CHANGELOG.md` — This file
  - `docs/adr/` — Architecture Decision Records

---

<!-- Link references -->
[Unreleased]: https://github.com/user/intikasir-fnb/compare/v0.0.0...HEAD
[0.0.0]: https://github.com/user/intikasir-fnb/releases/tag/v0.0.0

<!--
## Template for new releases:

## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes in existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Vulnerability fixes

### Architecture
- Architecture changes, ADR references

### Infrastructure
- Build, CI/CD, tooling changes
-->
