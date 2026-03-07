# ADR-006: Terminal as First-Class Entity

**Date**: 2026-03-07
**Status**: ACCEPTED

## Context

Dalam PoS multi-device, perlu cara untuk:
- Mengidentifikasi setiap device secara unik
- Menentukan capability per device (kasir bisa terima bayar, pelayan tidak)
- Track asal perubahan data (untuk sync & audit)
- Scope data per device (CashierSession, transaction numbering)

Opsi:
1. **Device ID saja** (implicit): Simpan device UUID, tidak ada entity formal
2. **Terminal as first-class entity**: Aggregate root di Identity & Access, dengan type, status, capabilities

## Decision

Menjadikan **Terminal** sebagai **first-class aggregate root** di Identity & Access context:

```kotlin
data class Terminal(
    val terminalId: TerminalId,    // UUID, generated saat install
    val tenantId: TenantId,
    val outletId: OutletId,
    val deviceName: String,        // "Kasir 1", "Pelayan Andi"
    val terminalType: TerminalType,// CASHIER, WAITER, KITCHEN_DISPLAY, MANAGER
    val status: TerminalStatus,    // ACTIVE, SUSPENDED, DEREGISTERED
    val syncEnabled: Boolean,
    val lastSyncAt: Instant?,
)
```

- Setiap entity yang syncable memiliki `createdByTerminalId` dan `updatedByTerminalId`
- CashierSession di-scope ke Terminal
- Transaction numbering mengandung terminal code
- Terminal type menentukan UI mode dan capabilities

## Consequences

### Positive
- Audit trail jelas: siapa (user) + dari mana (terminal) setiap perubahan
- Multi-device ready dari hari pertama
- Terminal type enables different UI modes (kasir, pelayan, kitchen display)
- Transaction numbering conflict-free (per terminal sequence)
- Capabilities configurable per terminal (canProcessPayment, maxDiscount, etc.)

### Negative
- Tambahan entity dan tabel di database
- Perlu terminal management UI
- Setiap entity perlu 2 extra columns (createdByTerminalId, updatedByTerminalId)

### Neutral
- Di standalone mode: 1 terminal auto-created, user tidak perlu manage
- Di cloud mode: terminal register ke cloud, admin bisa manage via API
