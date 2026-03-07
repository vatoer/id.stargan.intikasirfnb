# Offline-First & Cloud-Sync Architecture

> IntiKasir F&B - Arsitektur Offline-First dengan Cloud-Ready Sync

## 1. Prinsip Dasar

### 1.1 Offline-First Philosophy

```
LOCAL DB (Room) = Source of Truth (saat offline)
CLOUD DB        = Source of Truth (saat online, untuk aggregasi multi-device)
```

- Aplikasi HARUS berfungsi 100% tanpa koneksi internet
- Semua operasi CRUD terjadi di local database terlebih dahulu
- Sync ke cloud adalah proses background yang TIDAK mengganggu UX
- Jika cloud tidak tersedia, aplikasi tetap beroperasi normal
- Data yang belum ter-sync ditandai dan di-queue untuk retry

### 1.2 Mode Operasi

| Mode | Deskripsi | Fitur |
|------|-----------|-------|
| **Standalone** | Single device, tanpa cloud | Semua fitur PoS, data hanya di device |
| **Cloud-Connected** | Single/multi device, sync ke self-hosted cloud | Multi PoS, multi pelayan, multi outlet, multi tenant |

Mode ditentukan oleh **setting** di level Tenant. Default: **Standalone**.

### 1.3 Deployment Topology

```
Mode Standalone:
  [Android Device] → [Room DB] → selesai

Mode Cloud-Connected:
  [Android Device 1 (Kasir)]     ─┐
  [Android Device 2 (Pelayan)]   ─┤── sync ──→ [Self-Hosted Cloud API] → [PostgreSQL]
  [Android Device 3 (Kasir 2)]   ─┘                    │
  [Android Device 4 (Outlet B)]  ── sync ──────────────→┘
```

---

## 2. Identitas & Registrasi Device

### 2.1 Terminal (Device) Entity

Setiap device yang menjalankan IntiKasir adalah sebuah **Terminal**. Terminal adalah first-class entity di Identity & Access context.

```kotlin
data class Terminal(
    val terminalId: TerminalId,       // UUID, generated saat pertama install
    val tenantId: TenantId,
    val outletId: OutletId,
    val deviceName: String,           // e.g. "Kasir 1", "Pelayan Andi"
    val terminalType: TerminalType,   // CASHIER, WAITER, KITCHEN_DISPLAY, MANAGER
    val status: TerminalStatus,       // ACTIVE, SUSPENDED, DEREGISTERED
    val lastSyncAt: Instant?,         // null jika belum pernah sync
    val registeredAt: Instant,
    val syncEnabled: Boolean,         // false = standalone mode di device ini
)

enum class TerminalType {
    CASHIER,           // Full PoS - bisa buat transaksi, terima pembayaran
    WAITER,            // Order-only - buat order dari meja, kirim ke kitchen
    KITCHEN_DISPLAY,   // Read-only - tampilkan kitchen ticket
    MANAGER            // Full access + reporting
}
```

### 2.2 Registrasi Flow

```
Mode Standalone:
  1. Install app → generate TerminalId (UUID)
  2. Setup Tenant & Outlet (local)
  3. Mulai operasi — tidak perlu cloud

Mode Cloud-Connected:
  1. Install app → generate TerminalId (UUID)
  2. Input Cloud URL + Registration Token
  3. POST /api/terminals/register { terminalId, deviceInfo }
  4. Cloud returns: tenantId, outletId, terminalConfig, initialSyncPayload
  5. Device melakukan Initial Sync (full download)
  6. Mulai operasi dengan background sync aktif
```

---

## 3. Sync Metadata pada Semua Entity

### 3.1 Syncable Base

Setiap entity yang perlu di-sync HARUS memiliki metadata berikut:

```kotlin
interface Syncable {
    val id: String                  // Primary key (UUID/ULID)
    val tenantId: TenantId          // Scope isolation
    val outletId: OutletId?         // null = tenant-wide (e.g. Product master)
    val createdAt: Instant
    val updatedAt: Instant
    val createdByTerminalId: TerminalId
    val updatedByTerminalId: TerminalId
    val syncStatus: SyncStatus
    val syncVersion: Long           // Monotonic version counter
    val deletedAt: Instant?         // Soft delete - TIDAK pernah hard delete
}

enum class SyncStatus {
    SYNCED,          // Data sudah sama dengan cloud
    PENDING_UPLOAD,  // Baru dibuat/diubah di local, belum di-push ke cloud
    PENDING_DOWNLOAD,// Cloud punya versi lebih baru (jarang, biasanya langsung apply)
    CONFLICT         // Versi local dan cloud berbeda, perlu resolusi
}
```

### 3.2 Room Entity Pattern

```kotlin
@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val outletId: String,
    // ... business fields ...
    // Sync metadata
    val createdAt: Long,            // epoch millis
    val updatedAt: Long,
    val createdByTerminalId: String,
    val updatedByTerminalId: String,
    val syncStatus: String,         // SYNCED | PENDING_UPLOAD | CONFLICT
    val syncVersion: Long,
    val deletedAt: Long?,           // null = active
)
```

### 3.3 ID Generation Strategy

```
Format: ULID (Universally Unique Lexicographically Sortable Identifier)

Alasan:
- Sortable by time → natural ordering untuk sync
- Unique across devices tanpa koordinasi → offline-safe
- 128-bit → sama dengan UUID tapi sortable
- Contoh: 01ARZ3NDEKTSV4RRFFQ69G5FAV
```

Semua entity ID menggunakan ULID, di-generate di device. Tidak ada auto-increment ID.

---

## 4. Sync Engine

### 4.1 Arsitektur Sync

```
┌─────────────────────────────────────────────────────┐
│                    Android Device                     │
│                                                       │
│  [Use Case] → [Repository] → [Room DB]               │
│                                    │                  │
│                              [SyncEngine]             │
│                              ┌─────┴─────┐           │
│                         [ChangeTracker] [SyncQueue]   │
│                                          │            │
│                                    [SyncWorker]       │
│                                   (WorkManager)       │
└──────────────────────────────────────┼────────────────┘
                                       │ HTTPS
                              ┌────────▼─────────┐
                              │  Self-Hosted      │
                              │  Cloud API        │
                              │  (REST + SSE)     │
                              └──────────────────┘
```

### 4.2 Sync Strategy: Push-Pull with Versioning

**Push (Device → Cloud):**
```
1. Setiap write operation di Room → set syncStatus = PENDING_UPLOAD
2. SyncWorker (periodic + on-change) queries semua PENDING_UPLOAD
3. POST /api/sync/push
   Body: { terminalId, changes: [{ entity, type, data, syncVersion }] }
4. Cloud validates, applies, returns accepted/rejected per item
5. Accepted → update syncStatus = SYNCED, syncVersion = cloud_version
6. Rejected (conflict) → syncStatus = CONFLICT, simpan cloud version untuk resolusi
```

**Pull (Cloud → Device):**
```
1. GET /api/sync/pull?since={lastSyncVersion}&terminalId={id}
2. Cloud returns semua changes sejak lastSyncVersion untuk tenant/outlet ini
3. Device applies changes ke Room DB
4. Update local lastSyncVersion
```

**Real-time (optional, saat online):**
```
SSE stream: GET /api/sync/stream?terminalId={id}
- Cloud push notifikasi saat ada perubahan dari terminal lain
- Device melakukan pull untuk mendapatkan data terbaru
- Fallback ke periodic pull jika SSE terputus
```

### 4.3 Conflict Resolution Strategy

```
Aturan Dasar:
1. Transaction data (Sale, Payment) → TIDAK BOLEH conflict
   - Sale dibuat di 1 terminal, payment di terminal itu juga
   - Jika multi-terminal: Sale di-assign ke terminal tertentu (locking)

2. Master data (Product, Category) → Last-Write-Wins (LWW) by updatedAt
   - Cloud timestamp sebagai tiebreaker
   - Perubahan master data biasanya dari 1 device (Manager)

3. Inventory (Stock) → Cloud sebagai arbiter
   - Stock movement di-push, cloud menghitung saldo final
   - Device TIDAK menghitung saldo sendiri saat online
   - Device menghitung saldo sendiri HANYA saat offline (local estimate)
   - Saat sync: cloud saldo menimpa local saldo

4. CashierSession → Terminal-scoped, tidak conflict
   - Setiap session terikat ke 1 terminal
```

**Conflict Resolution Matrix:**

| Entity Type | Strategy | Alasan |
|-------------|----------|--------|
| Sale/Order | No conflict (terminal-owned) | Dibuat & diproses di 1 terminal |
| Payment | No conflict (terminal-owned) | Payment terjadi di 1 terminal |
| CashierSession | No conflict (terminal-scoped) | 1 session = 1 terminal |
| Product/MenuItem | LWW by updatedAt | Master data, jarang concurrent edit |
| Category | LWW by updatedAt | Structural, jarang concurrent edit |
| Customer | LWW by updatedAt | Biasanya di-edit dari 1 tempat |
| StockMovement | Append-only, no conflict | Movement adalah event, bukan state |
| StockLevel | Cloud-computed | Dihitung dari movements |
| KitchenTicket | Terminal-owned + status merge | Status hanya maju (PENDING→PREPARING→DONE) |
| Settings | LWW by updatedAt | Biasanya di-edit oleh Manager |
| User/Role | Cloud-authoritative | Security-critical, cloud is source of truth |

### 4.4 Sync Queue & Retry

```kotlin
@Entity(tableName = "sync_queue")
data class SyncQueueEntry(
    @PrimaryKey val id: String,           // ULID
    val entityType: String,               // "sale", "product", etc.
    val entityId: String,                 // ID of the changed entity
    val operation: String,                // CREATE, UPDATE, DELETE
    val payload: String,                  // JSON serialized entity
    val createdAt: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 10,
    val nextRetryAt: Long,                // exponential backoff
    val status: String,                   // QUEUED, IN_PROGRESS, FAILED, COMPLETED
    val errorMessage: String? = null,
)
```

**Retry Policy:**
- Exponential backoff: 5s, 15s, 45s, 2m, 5m, 15m, 30m, 1h, 2h, 4h
- Max retries: 10 (lalu mark as FAILED, perlu manual intervention)
- Network error → retry otomatis
- Validation error (400) → mark FAILED, perlu manual fix
- Server error (500) → retry dengan backoff

### 4.5 Initial Sync (First-time Device Setup)

```
Saat device baru terdaftar di cloud mode:

1. Download master data:
   - Tenant & Outlet config
   - Users & Roles (untuk outlet ini)
   - Products & Categories
   - Customers
   - Settings (tax, receipt template, etc.)
   - Price lists & active promotions

2. TIDAK download:
   - Historical transactions (terlalu besar)
   - Old stock movements
   - Closed cashier sessions

3. Partial historical (optional, configurable):
   - Open/active orders (belum COMPLETED)
   - Today's transactions (untuk reporting shift)
   - Current stock levels (snapshot, bukan movements)
```

---

## 5. Cloud API Design

### 5.1 Self-Hosted Cloud Stack

```
┌──────────────────────────────────────┐
│          Self-Hosted Server           │
│                                       │
│  [Reverse Proxy: Nginx/Caddy]        │
│           │                           │
│  [API Server: Kotlin/Ktor atau       │
│   Go/Fiber atau Node/Fastify]        │
│           │                           │
│  [PostgreSQL]  [Redis (optional)]    │
│                                       │
│  Minimum: 1 VPS/server               │
│  Bisa di: rumah, kantor, VPS cloud   │
└──────────────────────────────────────┘
```

### 5.2 API Endpoints

```
Authentication:
  POST   /api/auth/login              # Login user → JWT token
  POST   /api/auth/refresh            # Refresh token
  POST   /api/auth/logout             # Invalidate token

Terminal Management:
  POST   /api/terminals/register      # Register new terminal
  GET    /api/terminals               # List terminals for outlet
  PUT    /api/terminals/:id           # Update terminal config
  DELETE /api/terminals/:id           # Deregister terminal

Sync:
  POST   /api/sync/push              # Push local changes to cloud
  GET    /api/sync/pull              # Pull changes since version
  GET    /api/sync/stream            # SSE real-time change stream
  POST   /api/sync/resolve-conflict  # Submit conflict resolution
  GET    /api/sync/status            # Sync health & pending count

Master Data (CRUD, cloud-authoritative saat online):
  GET    /api/products               # List products
  POST   /api/products               # Create product
  PUT    /api/products/:id           # Update product
  DELETE /api/products/:id           # Soft delete
  # ... sama untuk categories, customers, users, settings, etc.

Reporting (cloud-only, aggregasi multi-terminal):
  GET    /api/reports/sales-summary   # Aggregated sales
  GET    /api/reports/product-mix     # Product sales breakdown
  GET    /api/reports/cashier-sessions # Session summaries
  GET    /api/reports/stock-levels    # Current stock across outlets
  GET    /api/reports/dashboard       # Real-time dashboard data

Multi-Tenant Admin (super-admin only):
  GET    /api/admin/tenants           # List tenants
  POST   /api/admin/tenants           # Create tenant
  GET    /api/admin/tenants/:id/outlets # List outlets
  POST   /api/admin/tenants/:id/outlets # Create outlet
```

### 5.3 Sync Push/Pull Payload

```json
// POST /api/sync/push
{
  "terminalId": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
  "batch": [
    {
      "entityType": "sale",
      "entityId": "01BX5ZZKBKACTAV9WEVGEMMVRY",
      "operation": "CREATE",
      "syncVersion": 42,
      "timestamp": "2026-03-07T10:30:00Z",
      "data": { /* full entity JSON */ }
    },
    {
      "entityType": "product",
      "entityId": "01BX5ZZKBKACTAV9WEVGEMMVRZ",
      "operation": "UPDATE",
      "syncVersion": 15,
      "timestamp": "2026-03-07T10:31:00Z",
      "data": { /* full entity JSON */ }
    }
  ]
}

// Response
{
  "results": [
    { "entityId": "...VRY", "status": "ACCEPTED", "cloudVersion": 43 },
    { "entityId": "...VRZ", "status": "CONFLICT", "cloudVersion": 18,
      "cloudData": { /* cloud version of entity */ } }
  ],
  "serverTimestamp": "2026-03-07T10:31:05Z"
}
```

```json
// GET /api/sync/pull?since=42&terminalId=01ARZ...
{
  "changes": [
    {
      "entityType": "product",
      "entityId": "01BX5ZZKBKACTAV9WEVGEMM000",
      "operation": "UPDATE",
      "syncVersion": 44,
      "timestamp": "2026-03-07T10:32:00Z",
      "sourceTerminalId": "01ARZ3NDEKTSV4RRFFQ69G5FBB",
      "data": { /* full entity JSON */ }
    }
  ],
  "latestVersion": 44,
  "hasMore": false
}
```

### 5.4 Authentication & Security

```
- JWT token dengan short expiry (15 menit)
- Refresh token dengan longer expiry (7 hari)
- Terminal-specific token (terminalId embedded in JWT claims)
- TLS/HTTPS wajib (self-signed OK untuk local network)
- API key per tenant sebagai tambahan autentikasi
- Rate limiting per terminal
- Tenant isolation di database level (tenant_id di setiap query)
```

---

## 6. Multi-Device Support (Cloud Mode)

### 6.1 Multi PoS Point (Multi Kasir)

```
Skenario: Restoran dengan 2 kasir

Terminal 1 (CASHIER) ──┐
                       ├── Cloud API ── PostgreSQL
Terminal 2 (CASHIER) ──┘

Aturan:
- Setiap kasir punya CashierSession sendiri
- Sale dibuat di terminal yang menerima order
- Payment diproses di terminal yang sama dengan Sale
- Stock level di-sync via cloud (near real-time)
- Nomor transaksi: {OutletCode}-{TerminalCode}-{Sequence}
  e.g. JKT01-K1-0001, JKT01-K2-0001
```

### 6.2 Multi PoS Pelayan (Waiter Terminals)

```
Skenario: Pelayan dengan tablet mengambil order dari meja

Terminal W1 (WAITER) ──┐
Terminal W2 (WAITER) ──┤── Cloud API ── PostgreSQL
Terminal K1 (CASHIER) ─┤
Terminal KD (KITCHEN)──┘

Flow:
1. Pelayan W1 buka meja 5 → buat Order (status: OPEN)
2. Order di-sync ke cloud → cloud broadcast ke semua terminal
3. Kitchen Display (KD) terima order → tampilkan ticket
4. Pelayan W2 bisa tambah item ke Order meja 5 (jika W1 sudah sync)
5. Kasir K1 terima pembayaran untuk Order meja 5
6. Sale COMPLETED → sync → stock deducted

Locking:
- Order dimiliki oleh terminal yang membuat
- Terminal lain bisa ADD item (append-only, no conflict)
- MODIFY/CANCEL item hanya oleh terminal pemilik atau MANAGER
- Payment hanya di terminal CASHIER
```

### 6.3 Table Management (Multi-Device)

```kotlin
data class TableSession(
    val tableId: TableId,
    val outletId: OutletId,
    val status: TableStatus,          // AVAILABLE, OCCUPIED, RESERVED, NEEDS_CLEANING
    val currentOrderId: OrderId?,     // Active order di meja ini
    val assignedTerminalId: TerminalId?, // Pelayan yang handle meja ini
    val guestCount: Int?,
    val occupiedSince: Instant?,
    val syncVersion: Long,
)

// Meja status di-sync real-time via SSE
// Semua terminal melihat status meja yang sama
// Conflict: jika 2 pelayan assign meja yang sama → first-write-wins
```

### 6.4 Multi-Tenant & Multi-Outlet

```
Tenant: "Warung Makan Pak Budi"
├── Outlet: "Cabang Kemang"
│   ├── Terminal K1 (CASHIER)
│   ├── Terminal W1 (WAITER)
│   └── Terminal KD1 (KITCHEN_DISPLAY)
├── Outlet: "Cabang Menteng"
│   ├── Terminal K1 (CASHIER)
│   └── Terminal KD1 (KITCHEN_DISPLAY)
└── Outlet: "Cabang Depok"
    └── Terminal K1 (CASHIER)

Data Scope:
- Product/Category    → bisa tenant-wide ATAU per-outlet (configurable)
- Price List          → per outlet (harga bisa beda antar cabang)
- Sale/Order          → per outlet, per terminal
- Stock               → per outlet (inventory terpisah per cabang)
- Customer            → tenant-wide (pelanggan bisa ke semua cabang)
- User                → tenant-wide, assigned ke outlet(s)
- Settings            → tenant-level defaults + outlet-level overrides
- Reporting           → per outlet + aggregasi tenant-wide (cloud only)
```

---

## 7. Settings & Configuration

### 7.1 Cloud Sync Settings

```kotlin
data class SyncSettings(
    // Master toggle
    val cloudSyncEnabled: Boolean = false,

    // Cloud connection
    val cloudApiUrl: String? = null,          // e.g. "https://pos.pakbudi.com/api"
    val cloudApiKey: String? = null,          // Tenant API key
    val registrationToken: String? = null,    // One-time registration token

    // Sync behavior
    val syncIntervalSeconds: Int = 30,        // Periodic sync interval
    val realtimeEnabled: Boolean = true,      // SSE real-time updates
    val syncOnTransaction: Boolean = true,    // Immediate sync after sale completed
    val syncMasterDataOnly: Boolean = false,  // Hanya sync master data (no transactions)

    // Data scope
    val syncProducts: Boolean = true,
    val syncCustomers: Boolean = true,
    val syncTransactions: Boolean = true,
    val syncInventory: Boolean = true,
    val syncReporting: Boolean = false,       // Reporting biasanya cloud-side only

    // Conflict handling
    val conflictStrategy: ConflictStrategy = ConflictStrategy.LAST_WRITE_WINS,
    val notifyOnConflict: Boolean = true,

    // Bandwidth management
    val syncOverWifiOnly: Boolean = false,
    val maxBatchSize: Int = 50,               // Max entities per push
    val compressPayload: Boolean = true,      // gzip sync payloads
)

enum class ConflictStrategy {
    LAST_WRITE_WINS,        // Default: timestamp terbaru menang
    CLOUD_WINS,             // Cloud version selalu menang
    LOCAL_WINS,             // Local version selalu menang
    MANUAL_RESOLVE,         // Tampilkan dialog ke user
}
```

### 7.2 Terminal Settings

```kotlin
data class TerminalSettings(
    val terminalId: TerminalId,
    val terminalName: String,                 // "Kasir 1"
    val terminalType: TerminalType,           // CASHIER, WAITER, etc.

    // Capabilities (berdasarkan type + override)
    val canCreateOrder: Boolean = true,
    val canProcessPayment: Boolean = true,    // false untuk WAITER
    val canModifyProduct: Boolean = false,    // true hanya untuk MANAGER
    val canViewReports: Boolean = false,      // true untuk MANAGER
    val canManageStock: Boolean = false,      // true untuk MANAGER
    val canVoidSale: Boolean = false,         // true untuk CASHIER + MANAGER
    val canGiveDiscount: Boolean = false,     // configurable per terminal
    val maxDiscountPercent: Double = 0.0,     // batas diskon yang bisa diberikan

    // Printing
    val printerType: PrinterType = PrinterType.NONE,
    val printerAddress: String? = null,       // Bluetooth MAC atau IP
    val autoPrintReceipt: Boolean = true,
    val autoPrintKitchenTicket: Boolean = false,

    // UI
    val showTableMap: Boolean = true,         // false untuk takeaway-only outlet
    val defaultSalesChannelId: SalesChannelId? = null, // null = user pilih manual; non-null = auto-select channel
)
```

### 7.3 Settings Hierarchy

```
                ┌─────────────────┐
                │  Tenant Default  │  ← berlaku untuk semua outlet
                └────────┬────────┘
                         │ override
                ┌────────▼────────┐
                │ Outlet Override  │  ← per cabang
                └────────┬────────┘
                         │ override
                ┌────────▼────────┐
                │ Terminal Override │  ← per device
                └─────────────────┘

Resolusi: Terminal > Outlet > Tenant Default
Contoh: Tax rate default 11% (tenant) → Outlet Depok override 0% (tax-free zone)
```

---

## 8. Data Layer Changes

### 8.1 Repository Pattern dengan Sync Support

```kotlin
// Domain layer - repository interface (unchanged)
interface SaleRepository {
    suspend fun getById(id: SaleId): Sale?
    fun observeByOutlet(outletId: OutletId): Flow<List<Sale>>
    suspend fun save(sale: Sale): Result<Sale>
}

// Data layer - implementation dengan sync awareness
class SaleRepositoryImpl(
    private val saleDao: SaleDao,
    private val syncEngine: SyncEngine,       // Injected, nullable-safe
    private val clock: Clock,
    private val terminalId: TerminalId,
) : SaleRepository {

    override suspend fun save(sale: Sale): Result<Sale> {
        val entity = sale.toEntity(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            syncVersion = saleDao.getNextVersion(sale.id),
            updatedByTerminalId = terminalId.value,
            updatedAt = clock.now(),
        )
        saleDao.upsert(entity)

        // Trigger sync jika enabled (fire-and-forget)
        syncEngine.notifyChange(EntityType.SALE, sale.id.value)

        return Result.success(sale)
    }
}
```

### 8.2 SyncEngine Interface

```kotlin
interface SyncEngine {
    /** Notify that a local entity has changed */
    fun notifyChange(entityType: EntityType, entityId: String)

    /** Start background sync worker */
    fun startPeriodicSync()

    /** Stop background sync */
    fun stopSync()

    /** Force immediate sync */
    suspend fun syncNow(): SyncResult

    /** Observe sync status */
    fun observeSyncStatus(): Flow<SyncStatus>

    /** Get pending sync count */
    suspend fun getPendingCount(): Int

    /** Observe real-time changes from other terminals */
    fun observeRemoteChanges(): Flow<RemoteChange>
}

// No-op implementation untuk standalone mode
class NoOpSyncEngine : SyncEngine {
    override fun notifyChange(entityType: EntityType, entityId: String) { /* no-op */ }
    override fun startPeriodicSync() { /* no-op */ }
    override fun stopSync() { /* no-op */ }
    override suspend fun syncNow() = SyncResult.Disabled
    override fun observeSyncStatus() = flowOf(SyncStatus.DISABLED)
    override suspend fun getPendingCount() = 0
    override fun observeRemoteChanges() = emptyFlow()
}
```

### 8.3 DI Configuration

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncEngine(
        syncSettings: SyncSettings,
        syncApi: SyncApi,
        database: AppDatabase,
        workManager: WorkManager,
    ): SyncEngine {
        return if (syncSettings.cloudSyncEnabled) {
            CloudSyncEngine(syncApi, database, workManager, syncSettings)
        } else {
            NoOpSyncEngine()
        }
    }
}
```

---

## 9. Transaction Numbering (Multi-Terminal Safe)

### 9.1 Format

```
{OutletCode}-{TerminalCode}-{YYYYMMDD}-{Sequence}

Contoh:
  KMG-K1-20260307-0001   → Kasir 1 di outlet Kemang
  KMG-K2-20260307-0001   → Kasir 2 di outlet Kemang
  MTG-K1-20260307-0001   → Kasir 1 di outlet Menteng

Standalone mode:
  001-K1-20260307-0001   → Default outlet, default terminal
```

### 9.2 Sequence Generation

```kotlin
// Sequence di-generate per terminal per hari, di local DB
// Tidak perlu koordinasi dengan cloud → offline-safe
// Cloud mengetahui format dan bisa de-duplikasi berdasarkan full number

@Entity(tableName = "numbering_sequences")
data class NumberingSequence(
    @PrimaryKey val key: String,        // "{outletCode}-{terminalCode}-{date}"
    val currentValue: Int,
    val updatedAt: Long,
)
```

---

## 10. Migrasi Standalone → Cloud

### 10.1 Flow Migrasi

```
1. User aktifkan cloud sync di Settings
2. Input Cloud API URL + Registration Token
3. App validasi koneksi ke cloud
4. Register terminal di cloud
5. Upload existing data:
   a. Tenant & Outlet config
   b. Sales Channels (Dine In, Take Away, GoFood, GrabFood, etc. + pricing config)
   c. Products & Categories
   d. Price Lists (channel-specific pricing)
   e. Customers
   f. Active/recent transactions (configurable: last N days)
   g. Current stock levels
6. Cloud confirm receipt
7. Cloud sync aktif → mode berubah ke Cloud-Connected
8. Jika ada terminal lain sudah terdaftar → pull their data
```

### 10.2 Reversibility

```
Cloud → Standalone:
1. User nonaktifkan cloud sync di Settings
2. Ensure semua PENDING_UPLOAD sudah ter-sync (warning jika belum)
3. App stop SyncWorker dan SSE connection
4. Data lokal tetap ada dan bisa digunakan
5. Mode kembali ke Standalone
6. Data di cloud tetap ada (tidak dihapus)
```

---

## 11. Kitchen Display Protocol (Multi-Device)

### 11.1 Kitchen Ticket Flow

```
[Waiter] → Create Order → sync → [Cloud] → broadcast → [Kitchen Display]
                                                      → [Cashier] (update order list)

Kitchen Display lifecycle:
1. Receive new KitchenTicket (status: PENDING)
2. Tap "Mulai" → status: PREPARING → sync → Waiter sees "Sedang Dimasak"
3. Tap "Selesai" → status: READY → sync → Waiter/Cashier notified
4. Kasir/Pelayan tap "Diambil" → status: SERVED → sync

Offline handling:
- Kitchen Display bisa beroperasi dari local DB jika koneksi ke cloud putus
- Orders yang masuk sebelum putus tetap tampil
- Orders baru dari terminal lain TIDAK tampil sampai koneksi pulih
- Status update di-queue dan di-sync saat koneksi pulih
```

---

## 12. Dampak pada Bounded Contexts

### 12.1 Context Changes Summary

| Bounded Context | Perubahan untuk Sync Support |
|----------------|------------------------------|
| **Identity & Access** | + Terminal entity, + TerminalType, + device registration flow |
| **Settings** | + SyncSettings, + TerminalSettings, + settings hierarchy (tenant>outlet>terminal) |
| **Transaction** | + syncStatus metadata, + terminal-scoped numbering, + order locking by terminal, + SalesChannel entity (configurable per tenant, per-channel pricing) |
| **Catalog** | + syncStatus metadata, + tenant-wide vs per-outlet scope flag |
| **Inventory** | + cloud-computed stock levels, + local estimate saat offline |
| **Customer** | + syncStatus metadata, + tenant-wide scope |
| **Workflow/Queue** | + real-time status sync, + kitchen display terminal support |
| **Pricing & Promotion** | + syncStatus metadata, + sync price list per outlet, + PriceList per SalesChannel (channel-specific pricing) |
| **Accounting** | + journal sync for cloud aggregation |
| **Reporting** | + cloud-only aggregate reports, + local reports tetap bisa offline |
| **Supplier** | + syncStatus metadata |

### 12.2 New Bounded Context: Sync

```
Context: Sync (Supporting Domain)

Aggregates:
  - SyncSession: represents a sync batch operation
  - SyncQueueEntry: individual entity change waiting to sync
  - ConflictRecord: unresolved conflicts

Events:
  - SyncCompleted(terminalId, pushCount, pullCount, timestamp)
  - SyncFailed(terminalId, reason, retryAt)
  - ConflictDetected(entityType, entityId, localVersion, cloudVersion)
  - ConflictResolved(entityType, entityId, resolution)

Repository:
  - SyncQueueRepository: manage pending sync items
  - ConflictRepository: manage unresolved conflicts
  - SyncLogRepository: track sync history
```

---

## 13. Diagram Baru yang Diperlukan

```
sync-01-sync-engine-architecture.mmd     → Komponen SyncEngine dan flow data
sync-02-push-pull-sequence.mmd           → Sequence diagram push/pull
sync-03-conflict-resolution-flow.mmd     → Decision tree untuk conflict resolution
sync-04-multi-terminal-topology.mmd      → Deployment topology multi-device
sync-05-initial-sync-sequence.mmd        → First-time device registration & sync
sync-06-offline-to-online-transition.mmd → State machine: offline ↔ online
```

---

## 14. Implementation Priority

### Phase 1: Standalone (MVP)
```
- [ ] Semua entity menggunakan ULID
- [ ] Syncable metadata di semua Room entities (prepopulate, tapi sync belum aktif)
- [ ] SyncEngine interface dengan NoOpSyncEngine
- [ ] Terminal entity (single terminal, auto-created)
- [ ] Transaction numbering dengan terminal code
- [ ] Semua fitur PoS berjalan fully offline
```

### Phase 2: Cloud Foundation
```
- [ ] Cloud API server setup (Ktor/Go/Node)
- [ ] Authentication (JWT + API key)
- [ ] Terminal registration endpoint
- [ ] Sync push/pull endpoints
- [ ] SyncSettings UI di app
- [ ] CloudSyncEngine implementation
- [ ] SyncWorker (WorkManager)
- [ ] Initial sync flow
```

### Phase 3: Multi-Terminal
```
- [ ] Multi-kasir support
- [ ] Waiter terminal mode
- [ ] Kitchen Display terminal mode
- [ ] Table management real-time sync
- [ ] Order locking per terminal
- [ ] SSE real-time stream
```

### Phase 4: Multi-Outlet & Multi-Tenant
```
- [ ] Multi-outlet data scoping
- [ ] Cross-outlet reporting (cloud)
- [ ] Tenant-wide vs per-outlet product catalog
- [ ] Multi-tenant admin API
- [ ] Outlet-specific pricing
```
