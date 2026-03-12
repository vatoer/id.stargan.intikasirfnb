# IntiKasir F&B — Dokumentasi Pengembangan

> **Versi**: 2.0.0
> **Terakhir diperbarui**: 2026-03-12
> **Produk**: IntiKasir F&B — Android Point of Sale untuk Restoran & Kafe
> **Stack**: Kotlin · Jetpack Compose · MVVM · Clean Architecture · DDD · Room · Hilt

---

## Daftar Dokumen

| No. | Dokumen | Deskripsi |
|-----|---------|-----------|
| 01 | [Product Overview](01-product-overview.md) | Visi produk, target pasar, fitur utama, roadmap |
| 02 | [Architecture Overview](02-architecture-overview.md) | Clean Architecture, DDD, MVVM — gambaran besar |
| 03 | [Domain Model](03-domain-model.md) | Bounded contexts, entities, value objects, aggregates |
| 04 | [F&B Domain Specialization](04-fnb-domain-specialization.md) | Sales channel, recipe/COGS, tax/SC/tip, platform delivery |
| 05 | [Data Architecture](05-data-architecture.md) | Room database, schema, migrations, sync metadata |
| 06 | [Sync & Cloud Architecture](06-sync-and-cloud-architecture.md) | Offline-first, push-pull sync, conflict resolution |
| 07 | [UI & Navigation](07-ui-and-navigation.md) | Screens, navigation graph, responsive layout, Compose |
| 08 | [Module & Project Structure](08-module-and-project-structure.md) | Gradle modules, dependencies, build configuration |
| 09 | [Use Case Reference](09-use-case-reference.md) | Semua use case per bounded context |
| 10 | [Testing Strategy](10-testing-strategy.md) | Strategi testing, coverage targets, tools |
| 11 | [Security & Licensing](11-security-and-licensing.md) | Autentikasi, PIN, AppReg licensing, encryption |
| 12 | [Printing & Peripherals](12-printing-and-peripherals.md) | ESC/POS, Bluetooth printer, receipt formatting |
| 13 | [Deployment & Release](13-deployment-and-release.md) | Build flavors, CI/CD, release process |

---

## Dokumen Pendukung

| Dokumen | Deskripsi |
|---------|-----------|
| [Architecture Decision Records](adr/README.md) | 7 ADR: offline-first, ULID, modules, cloud API, sync, terminal, Room |
| [Diagrams](diagrams/README.md) | Semua diagram Mermaid (.mmd) |
| [Implementation Plan](implementation-plan.md) | Roadmap 5 fase dengan detail task per layer + dependencies |
| [Implementation Status](implementation-status.md) | Dashboard progress real-time + metrics + milestones |
| [Changelog](../CHANGELOG.md) | Riwayat perubahan (Keep a Changelog) |

---

## Quick Reference

### Tech Stack

| Layer | Teknologi |
|-------|-----------|
| Language | Kotlin 2.3.10 |
| UI | Jetpack Compose (BOM 2026.02.00), Material 3 |
| Architecture | Clean Architecture + DDD + MVVM |
| DI | Hilt |
| Database | Room (SQLite) |
| Navigation | Navigation Compose |
| State | StateFlow / Coroutines / Flow |
| IDs | ULID (ulid-creator 5.2.3) |
| Printing | ESC/POS via Bluetooth SPP |
| Testing | JUnit 4, MockK, Turbine |

### Bounded Contexts

| Context | Tipe | Status |
|---------|------|--------|
| Transaction | Core | DONE |
| Catalog | Core | DONE |
| Workflow/Kitchen | Core | PARTIAL |
| Identity & Access | Support | DONE |
| Settings | Support | DONE |
| Pricing & Promotion | Support | PARTIAL |
| Inventory | Support | PARTIAL |
| Customer | Support | PARTIAL |
| Supplier | Support | PARTIAL |
| Accounting | Support | PARTIAL |
| Reporting | Support | NOT_STARTED |
| Licensing | Support | NOT_STARTED |

### Phase Overview

```
Phase 1: Foundation & Standalone MVP    ████████████████░░░░ 73%
Phase 2: Full PoS Features             ██████░░░░░░░░░░░░░░ 30%
Phase 3: Cloud Sync Foundation          ░░░░░░░░░░░░░░░░░░░░  0%
Phase 4: Multi-Terminal                 ░░░░░░░░░░░░░░░░░░░░  0%
Phase 5: Multi-Outlet & Multi-Tenant    ░░░░░░░░░░░░░░░░░░░░  0%
```

---

## Konvensi Dokumentasi

- **Bahasa**: Campuran Indonesia (narasi) dan Inggris (teknis/kode)
- **Diagram**: Inline Mermaid + file terpisah di `docs/diagrams/*.mmd`
- **ADR**: Format Michael Nygard di `docs/adr/`
- **Status**: `DONE` | `IN_PROGRESS` | `PARTIAL` | `NOT_STARTED`
- **ID Format**: ULID (26 karakter, sortable, offline-safe)
- **Tanggal**: ISO 8601 (YYYY-MM-DD)
