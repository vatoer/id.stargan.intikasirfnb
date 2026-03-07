# ADR-005: Sync Strategy: Push-Pull with Versioning

**Date**: 2026-03-07
**Status**: ACCEPTED

## Context

Perlu strategi sinkronisasi antara local Room DB dan cloud PostgreSQL. Opsi:
1. **Event sourcing + CQRS**: Setiap perubahan adalah event yang di-replay — powerful tapi complex
2. **CRDTs (Conflict-free Replicated Data Types)**: Automatic merge — limited data types, complex
3. **Push-Pull with versioning**: Kirim perubahan lokal, tarik perubahan remote, version counter untuk ordering — simple, proven
4. **Operational Transform**: Google Docs-style — overkill untuk PoS data

## Decision

Menggunakan **Push-Pull with monotonic version counter**:

- Setiap entity punya `syncVersion` (Long, monotonic, cloud-assigned)
- **Push**: Device kirim batch entity dengan `syncStatus=PENDING_UPLOAD` ke cloud
- **Pull**: Device request semua perubahan sejak `lastSyncVersion`
- Cloud menjadi arbiter version numbering
- Conflict detection: cloud menolak push jika `syncVersion` tidak match (optimistic locking)

Conflict resolution per entity type:
- **Transaction/Payment**: Terminal-owned, no conflict possible
- **Master data (Product, Category, Customer)**: Last-Write-Wins by `updatedAt`
- **StockLevel**: Cloud-computed from movements
- **User/Role**: Cloud-authoritative

Optional SSE (Server-Sent Events) untuk real-time notification.

## Consequences

### Positive
- Simple mental model: push local changes, pull remote changes
- Proven pattern (used by CouchDB, PouchDB, Realm Sync)
- Works well with offline-first (queue-and-retry)
- Conflict resolution per entity type — pragmatic, not one-size-fits-all
- Version counter eliminates need for vector clocks

### Negative
- Full entity sent per change (not field-level diff) — more bandwidth
- Last-Write-Wins bisa kehilangan perubahan (acceptable for master data)
- Cloud server required for version arbitration (tapi device tetap bisa beroperasi offline)

### Neutral
- Batch size tunable (maxBatchSize setting)
- SSE optional — fallback ke periodic pull
- Bisa di-upgrade ke field-level sync di masa depan jika needed
