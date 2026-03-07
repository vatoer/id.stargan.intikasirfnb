# ADR-007: Room as Local Database

**Date**: 2026-03-07
**Status**: ACCEPTED

## Context

Offline-first PoS membutuhkan local database yang reliable. Opsi untuk Android:
1. **Room (SQLite wrapper)**: Jetpack standard, compile-time query validation, Flow/coroutine support
2. **Realm**: Object database, built-in sync (Realm Sync) — proprietary, MongoDB dependency
3. **ObjectBox**: Fast object database — less community, proprietary
4. **SQLDelight**: Cross-platform SQL — less Android-specific features
5. **Raw SQLite**: Maximum control — no compile-time safety

## Decision

Menggunakan **Room** sebagai local database:

- Jetpack standard — long-term Google support
- Compile-time SQL query validation via annotation processor
- Native Kotlin coroutines + Flow support
- Migration framework built-in (schema versioning)
- Extensive community dan dokumentasi
- Single database untuk semua bounded contexts

Schema strategy:
- Export schema JSON for migration testing
- Sync metadata columns di semua entity tables
- Indices pada: `tenant_id`, `outlet_id`, `sync_status`, `updated_at`

## Consequences

### Positive
- Compile-time query validation — catch SQL errors early
- First-class coroutine/Flow support — reactive queries
- Schema migration framework — structured DB evolution
- Huge community, extensive docs, Google backed
- Works perfectly with Hilt DI

### Negative
- SQLite limitations (no concurrent writers, 1 writer thread)
- Relational model — need manual mapping to/from domain objects (mappers)
- Schema migrations bisa jadi complex seiring waktu

### Neutral
- Tidak include sync — sync di-handle oleh SyncEngine kita sendiri
- Single database file — semua context dalam 1 DB
- Bisa pakai SQLCipher untuk encryption at rest (evaluasi di Phase 2)
