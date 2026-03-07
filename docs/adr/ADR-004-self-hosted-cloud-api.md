# ADR-004: Self-Hosted Cloud API (No Firebase/BaaS)

**Date**: 2026-03-07
**Status**: ACCEPTED

## Context

Untuk cloud sync, dibutuhkan backend server. Opsi:
1. **Firebase / Supabase / Appwrite**: BaaS managed — cepat setup, tapi vendor lock-in, recurring cost, limited custom logic
2. **Self-hosted BaaS** (e.g. Supabase self-host): Kurang vendor lock-in, tapi still BaaS constraints
3. **Custom self-hosted API**: Full control, bisa deploy di VPS/on-premise, no recurring cloud cost per user

Target pengguna IntiKasir:
- Pemilik restoran kecil-menengah di Indonesia
- Cost-sensitive — menghindari recurring SaaS fees
- Beberapa ingin server di kantor sendiri (data sovereignty)
- Butuh custom business logic (sync, conflict resolution, multi-tenant)

## Decision

Menggunakan **custom self-hosted cloud API**:

- API server ditulis sendiri (Kotlin/Ktor, Go, atau Node — ADR terpisah saat Phase 3)
- Database: PostgreSQL
- Deploy target: VPS (DigitalOcean, Hetzner), on-premise server, atau cloud VM
- Reverse proxy: Nginx atau Caddy (auto-TLS)
- Minimum requirement: 1 VPS (2GB RAM, 1 vCPU)

## Consequences

### Positive
- Zero vendor lock-in
- Full control atas business logic (sync, multi-tenant, conflict resolution)
- No per-user recurring cost — fixed server cost
- Data sovereignty (bisa di-host di Indonesia)
- Bisa di-host di kantor pemilik restoran (local network)

### Negative
- Perlu develop & maintain backend sendiri
- Perlu setup server, TLS, monitoring, backup
- Update/patching server jadi tanggung jawab pengguna/admin
- Lebih lambat development vs BaaS di awal

### Neutral
- Bisa menyediakan one-click deploy script atau Docker Compose
- Bisa menawarkan managed hosting sebagai service (di masa depan)
