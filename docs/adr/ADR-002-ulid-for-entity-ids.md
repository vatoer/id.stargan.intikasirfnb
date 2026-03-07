# ADR-002: ULID for All Entity IDs

**Date**: 2026-03-07
**Status**: ACCEPTED

## Context

Dalam arsitektur offline-first dengan multi-device sync, ID generation harus:
- Unique tanpa koordinasi antar device (no central sequence)
- Tidak ada auto-increment (akan conflict antar device)
- Sortable by creation time (untuk sync ordering dan UI display)

Opsi yang dipertimbangkan:
1. **UUID v4**: Random, unique, tapi tidak sortable
2. **UUID v7**: Time-ordered UUID, tapi belum widely adopted
3. **ULID**: Universally Unique Lexicographically Sortable Identifier — 128-bit, time-based prefix + random suffix
4. **Snowflake ID**: Requires machine-id coordination

## Decision

Menggunakan **ULID** untuk semua entity ID di seluruh bounded context.

Format: `01ARZ3NDEKTSV4RRFFQ69G5FAV` (26 karakter, Crockford Base32)
- 48-bit timestamp (millisecond precision, good until year 10889)
- 80-bit randomness (collision-resistant)

Stored di Room sebagai `TEXT` column.
Kotlin: `@JvmInline value class SaleId(val value: String)`.

## Consequences

### Positive
- Sortable by creation time — natural ordering untuk sync dan UI
- Unique across devices tanpa koordinasi — offline-safe
- 128-bit — collision probability negligible
- Human-readable (Base32, uppercase)
- Bisa generate ID di domain layer (tidak butuh database)

### Negative
- String storage di SQLite lebih besar dari INTEGER auto-increment
- Index lookup pada TEXT column sedikit lebih lambat dari INTEGER
- Perlu library tambahan (ulid-creator atau custom generator)

### Neutral
- Tidak bisa infer creation order just from ID (tapi bisa dari prefix)
- Timestamp di ULID adalah wall-clock — bukan pengganti `createdAt` field
