# ADR-001: Offline-First Architecture with Optional Cloud Sync

**Date**: 2026-03-07
**Status**: ACCEPTED

## Context

IntiKasir F&B adalah PoS untuk restoran/kafe. Lingkungan operasi:
- Banyak restoran di Indonesia tidak memiliki koneksi internet stabil
- PoS harus bisa beroperasi saat listrik mati (pakai UPS) atau WiFi down
- Pemilik dengan multiple outlet ingin data terpusat di cloud
- Beberapa restoran hanya punya 1 kasir, tidak butuh cloud

Opsi yang dipertimbangkan:
1. **Cloud-first** (Firebase/Supabase): Semua data di cloud, cache lokal untuk offline → terlalu tergantung pada internet
2. **Offline-only**: Tidak ada cloud → tidak bisa multi-device/outlet
3. **Offline-first with optional cloud sync**: Local DB sebagai primary, cloud sebagai optional sync target

## Decision

Menggunakan arsitektur **offline-first** dengan **optional cloud sync**:

- Room (SQLite) sebagai source of truth di setiap device
- Semua operasi CRUD terjadi di local database terlebih dahulu
- Cloud sync bersifat **background**, **non-blocking**, dan **optional**
- Toggle `cloudSyncEnabled` di Settings menentukan mode:
  - `false` (default): Standalone mode, zero cloud dependency
  - `true`: Cloud-Connected mode, sync aktif
- Semua entity memiliki sync metadata dari hari pertama (ULID, syncStatus, syncVersion, terminalId) meskipun sync belum aktif

## Consequences

### Positive
- Aplikasi 100% berfungsi tanpa internet
- UX tidak terpengaruh oleh latency jaringan
- Pengguna bisa mulai standalone, upgrade ke cloud kapan saja
- Tidak ada vendor lock-in (self-hosted cloud)

### Negative
- Perlu conflict resolution untuk multi-device edits
- Data di device bisa out-of-date saat offline (eventual consistency)
- Sync metadata menambah storage overhead (~50 bytes per entity)
- Implementasi lebih kompleks vs pure cloud atau pure offline

### Neutral
- Perlu 2 implementasi SyncEngine (NoOp + Cloud)
- Perlu migration path standalone -> cloud (dan sebaliknya)
