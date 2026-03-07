# Architecture Decision Records (ADR)

Dokumen ini mencatat keputusan arsitektur penting yang dibuat selama pengembangan IntiKasir F&B. Mengikuti format [Michael Nygard's ADR](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

## Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [ADR-001](ADR-001-offline-first-architecture.md) | Offline-First Architecture with Optional Cloud Sync | ACCEPTED | 2026-03-07 |
| [ADR-002](ADR-002-ulid-for-entity-ids.md) | ULID for All Entity IDs | ACCEPTED | 2026-03-07 |
| [ADR-003](ADR-003-single-module-per-layer.md) | Single Module per Layer (Domain + Data) | ACCEPTED | 2026-03-07 |
| [ADR-004](ADR-004-self-hosted-cloud-api.md) | Self-Hosted Cloud API (No Firebase/BaaS) | ACCEPTED | 2026-03-07 |
| [ADR-005](ADR-005-sync-push-pull-with-versioning.md) | Sync Strategy: Push-Pull with Versioning | ACCEPTED | 2026-03-07 |
| [ADR-006](ADR-006-terminal-as-first-class-entity.md) | Terminal as First-Class Entity | ACCEPTED | 2026-03-07 |
| [ADR-007](ADR-007-room-as-local-database.md) | Room as Local Database | ACCEPTED | 2026-03-07 |

## Status Definitions

- **PROPOSED** — Under discussion, not yet decided
- **ACCEPTED** — Decision made and active
- **DEPRECATED** — Was accepted, now replaced by newer ADR
- **SUPERSEDED** — Replaced by another ADR (link to successor)

## How to Add New ADR

1. Copy `_template.md` to `ADR-NNN-short-title.md`
2. Fill in all sections
3. Add entry to Index table above
4. Log in [CHANGELOG.md](../../CHANGELOG.md) under `### Architecture`
