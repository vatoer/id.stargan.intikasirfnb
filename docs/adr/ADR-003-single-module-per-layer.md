# ADR-003: Single Module per Layer (Domain + Data)

**Date**: 2026-03-07
**Status**: ACCEPTED

## Context

DDD mengidentifikasi 12 bounded contexts. Module structure options:
1. **Per-domain modules**: `core:domain:identity`, `core:domain:catalog`, `core:domain:transaction`, dst. (24+ modules)
2. **Single module per layer**: `core:domain` (semua context), `core:data` (semua context) — pemisahan via package (2 core modules)
3. **Hybrid**: Single domain+data, presentation split per feature

## Decision

Menggunakan **single module per layer** dengan pemisahan via **package**:

```
core/
  domain/     # Semua bounded contexts via packages
    identity/ catalog/ transaction/ customer/ settings/ sync/ ...
  data/       # Semua repository impl, Room, mappers
    identity/ catalog/ transaction/ ...
app/          # Presentation + navigation
feature/      # Optional: presentation split per context (later)
```

## Consequences

### Positive
- Sederhana: hanya 2 core modules + 1 app module
- Mudah referensi cross-context (Transaction -> Catalog) tanpa circular module dependency
- Single Room database — no multi-database complexity
- Cocok untuk tim kecil (1-3 developer)
- Build time cepat (less module overhead)

### Negative
- Tidak ada compiler-enforced boundary antar context (hanya konvensi package)
- Semua context recompile saat 1 berubah
- Path ke microservices lebih sulit (tapi not a goal for Phase 1-3)

### Neutral
- Bisa migrasi ke per-domain modules di masa depan jika tim/codebase membesar
- Presentation bisa dipecah ke feature modules tanpa mengubah domain/data structure
- Detail opsi dan kapan migrasi: lihat `Module_Structure_Options.md`
