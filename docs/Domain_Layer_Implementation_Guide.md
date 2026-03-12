# Panduan Implementasi Domain Layer — PoS

Dokumen ini memandu implementasi **domain layer** (pure domain, tanpa infrastruktur) berdasarkan [DDD_Core_Support_Architecture.md](DDD_Core_Support_Architecture.md). Target implementasi: **aplikasi PoS Android** dengan **Kotlin**, **MVVM**, dan **Clean Architecture** — lihat [Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md) untuk pemetaan lapisan dan konvensi teknis.

Gunakan dokumen ini sebagai checklist agar implementasi konsisten dan tidak ada yang terlewat.

---

## 1. Prinsip Domain Layer

- **Tidak bergantung** pada framework, database, atau UI.
- **Entity & value object** hanya berisi logika bisnis dan invariant.
- **Repository** didefinisikan sebagai **interface** di domain; implementasi di lapisan **data** (Clean Architecture).
- **Domain events** didefinisikan di domain; publishing/handling di aplikasi/infrastruktur.
- Semua **ID** (OrderId, ProductId, TenantId, dll.) sebaiknya **value object** (type-safe). Di Kotlin: `value class` atau `data class`.
- **Kotlin**: Domain module tidak depend ke Android; hanya Kotlin stdlib. Interface repository tanpa prefix `I` (Kotlin idiom: `SaleRepository`, bukan `ISaleRepository`).

---

## 2. Urutan Implementasi yang Disarankan

Implementasi domain per bounded context bisa paralel setelah **Identity & Access** dan **Settings** (dasar multi-tenant). Urutan logis:

| Fase | Context | Alasan |
|------|---------|--------|
| 1 | **Identity & Access** | Tenant, Outlet, User, **Terminal** diperlukan untuk scope semua context lain. Terminal = device identity, diperlukan untuk sync metadata. |
| 2 | **Settings** | Tax, currency, numbering, **SyncSettings**, **TerminalSettings** dipakai Transaction & Catalog. |
| 3 | **Catalog** | Product/Category dipakai Transaction (ACL) dan Inventory. |
| 4 | **Transaction** | Inti PoS; bergantung Catalog (read), IAM, Settings. |
| 5 | **Customer** | Opsional di Transaction; bisa setelah Transaction. |
| 6 | **Pricing & Promotion** | Diperlukan Transaction untuk harga efektif; bisa sederhana dulu (base price dari Catalog). |
| 7 | **Inventory** | Dipakai setelah Transaction (event SaleCompleted). |
| 8 | **Supplier** | Dipakai Inventory (goods receipt) dan Accounting. |
| 9 | **Accounting** | Subscribe event dari Transaction, Inventory, Supplier. |
| 10 | **Workflow / Queue** | Opsional per line (F&B: KitchenTicket); bisa setelah Transaction. |
| 11 | **Reporting** | Read model / query; bisa setelah context tulis stabil. |
| 12 | **Sync** | Supporting domain untuk sync engine. Setelah semua context tulis stabil. SyncQueue, ConflictRecord, SyncSession. |

---

## 3. Checklist per Bounded Context

Untuk tiap context: definisikan **Aggregate Root**, **entity/value object di dalam boundary**, **domain events**, dan **repository interface**. Referensi lengkap ada di [DDD_Core_Support_Architecture.md](DDD_Core_Support_Architecture.md); F&B di [DDD_FnB_Detail.md](DDD_FnB_Detail.md).

### 3.1 Identity & Access

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `Tenant`, `Outlet`, `User`, `Terminal`. |
| **Value objects** | `TenantId`, `OutletId`, `UserId`, `TerminalId`, `Role`, `Permission`, `TerminalType` (CASHIER, WAITER, KITCHEN_DISPLAY, MANAGER), `TerminalStatus` (ACTIVE, SUSPENDED, DEREGISTERED). |
| **Entities (dalam boundary)** | Di `User`: referensi Role/Permission (bisa VO atau entity kecil). Di `Terminal`: device identity, type, sync status, last sync timestamp. |
| **Repository** | `TenantRepository`, `OutletRepository`, `UserRepository`, `TerminalRepository` (interface di domain). |
| **Events** | `UserLoggedIn`, `UserLoggedOut`, `RoleAssigned`, `TerminalRegistered`, `TerminalDeregistered` (jika perlu). |
| **Catatan offline-first** | Terminal auto-created saat install (standalone mode). Di cloud mode: Terminal diregistrasi ke cloud API. TerminalId = UUID, generated di device. Lihat [Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md) Section 2. |

### 3.2 Settings

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `TenantSettings`, `OutletSettings`, `TerminalSettings` (per scope). |
| **Value objects** | `TaxConfig` (taxId, name, rate, isIncludedInPrice, applicableTo: TaxScope, isActive), `ServiceChargeConfig` (isEnabled, rate, type, applicableChannelTypes, isIncludedInPrice), `TipConfig` (isEnabled, suggestedPercentages, allowCustomAmount, applicableChannelTypes), `TaxScope` (ALL_ITEMS, SPECIFIC_CATEGORIES, SPECIFIC_ITEMS), `ChargeType` (PERCENTAGE, FIXED_AMOUNT), `Money` (currency + amount), `NumberingSequenceConfig`, `SyncSettings` (cloudSyncEnabled, cloudApiUrl, syncInterval, conflictStrategy, etc.), `PrinterType`. |
| **Repository** | `TenantSettingsRepository`, `OutletSettingsRepository`, `TerminalSettingsRepository` (interface di domain). |
| **Events** | `SettingsUpdated`, `SyncSettingsChanged` (trigger sync engine restart), `TaxConfigUpdated`, `ServiceChargeConfigUpdated`. |
| **Catatan tax/SC/tip** | Tax, ServiceCharge, dan Tip dikonfigurasi di level Tenant (default) dengan override di Outlet. Bisa >1 tax aktif. Service charge applicable per channel type (e.g. hanya DINE_IN). Detail model: [DDD_FnB_Detail.md](DDD_FnB_Detail.md) Section 3.8. |
| **Catatan offline-first** | Settings hierarchy: Terminal > Outlet > Tenant Default. SyncSettings menentukan mode Standalone vs Cloud-Connected. Lihat [Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md) Section 7. |

### 3.3 Catalog

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `Product` (base), `Category`. Untuk F&B: `MenuItem` extends/specialisasi Product. |
| **Value objects** | `ProductId`, `CategoryId`, `Money`, `UnitOfMeasure`, `Variant` (size/option). |
| **Entities (dalam boundary)** | Di Product: variant list; di F&B MenuItem: ModifierGroup, ModifierOption (selection-based customization), **AddOnGroup, AddOnItem** (qty-based extras); **Recipe** (opsional) → `RecipeLine` (ingredientId, quantity, uom). |
| **Repository** | `ProductRepository`, `CategoryRepository`, `ModifierGroupRepository`, `AddOnGroupRepository`. F&B: `MenuItemRepository` bila terpisah (interface di domain). |
| **Events** | `ProductCreated`, `ProductUpdated`, `ProductDeactivated`, `CategoryStructureChanged`, `RecipeUpdated`. |

### 3.4 Transaction

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `Sale` (atau `Order`), `CashierSession`, `SalesChannel`. |
| **Value objects** | `SaleId`, `SalesChannelId`, `ChannelType` (DINE_IN, TAKE_AWAY, DELIVERY_PLATFORM, OWN_DELIVERY), `Money`, `OrderLine` (productRef, qty, **priceSnapshot sudah termasuk channel pricing**, modifierSnapshot, discount), `Payment` (method, amount, reference), `PlatformPayment` (grossAmount, commission, netAmount, settlementStatus), `PlatformConfig` (embedded di SalesChannel), `TaxLine` (taxId, taxRate, taxableAmount, taxAmount, isIncluded), `ServiceChargeLine` (name, rate, baseAmount, amount, isIncluded), `TipLine` (amount, method: ADDED_TO_BILL / SEPARATE_CASH). |
| **Entities (dalam boundary)** | Di Sale: `OrderLine`, `Payment` (sebagai entity atau VO), `TaxLine` (list), `ServiceChargeLine` (nullable), `TipLine` (nullable). Di SalesChannel: `PlatformConfig` (VO untuk DELIVERY_PLATFORM). |
| **Invariants** | Total payment >= grandTotal (subtotal + tax + serviceCharge + tip); status hanya transisi valid; **priceSnapshot di OrderLine harus sudah memperhitungkan channel pricing** (markup/PriceList); DINE_IN → tableId wajib; DELIVERY_PLATFORM → externalOrderId wajib jika platform requires it. Tax/SC/tip di-snapshot saat kalkulasi. |
| **Repository** | `SaleRepository`, `CashierSessionRepository`, `SalesChannelRepository`. Untuk F&B: `TableRepository` (meja). Interface di domain. |
| **Events** | `SaleCreated`, `LineItemAdded`, `PaymentReceived`, `SaleCompleted`, `SaleVoided`, `RefundIssued`; F&B: `OrderReadyForPickup`, `OrderDispatched`, `PlatformSettlementReceived`; Channel: `SalesChannelCreated`, `SalesChannelUpdated`. |
| **Catatan channel pricing** | Setiap SalesChannel bisa punya harga sendiri: (a) base price (default), (b) markup/discount dari channel config, (c) PriceList terpisah per item per channel. Harga Dine In, Take Away, GoFood, GrabFood, ShopeeFood semuanya bisa berbeda. Detail: [DDD_FnB_Detail.md](DDD_FnB_Detail.md) Section 3. |

### 3.5 Customer

| Item | Keterangan |
|------|------------|
| **Aggregate root** | `Customer`. |
| **Value objects** | `CustomerId`, `Address`, `Segment`. |
| **Repository** | `CustomerRepository` (interface di domain). |
| **Events** | `CustomerRegistered`, `CustomerUpdated`, `LoyaltyPointsEarned`. |

### 3.6 Pricing & Promotion

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `PriceList` (daftar harga per product — dipakai untuk channel-specific pricing), `PriceRule`; atau `Discount`, `Coupon` (tergantung granularitas). |
| **Value objects** | `PriceListId`, `PriceListEntry` (productId, price per item per PriceList), `PriceRule` (product/category/customer segment/channel, amount/percent), `PriceAdjustmentType` (MARKUP_PERCENT, MARKUP_FIXED, DISCOUNT_PERCENT, DISCOUNT_FIXED). |
| **Repository** | `PriceListRepository`, `CouponRepository` (jika ada; interface di domain). |
| **Integrasi dengan SalesChannel** | SalesChannel merujuk ke `PriceListId` (harga manual per item per channel) ATAU `priceAdjustmentType` + `value` (markup/discount otomatis). Resolusi: PriceList > adjustment > base price. |
| **Catatan** | Phase 1: harga efektif = base price dari Catalog (semua channel sama). Phase 2: channel pricing (SalesChannel + PriceList/adjustment) + basic discount. Detail: [DDD_FnB_Detail.md](DDD_FnB_Detail.md) Section 3.3. |

### 3.7 Inventory

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `StockLevel` (product + location), optional `StockMovement` (append-only). |
| **Value objects** | `ProductId`, `OutletId`/`LocationId`, `Quantity`, `Lot/Batch` (opsional). |
| **Repository** | `IStockLevelRepository`, `IStockMovementRepository` (jika dipakai). |
| **Events** | `StockReceived`, `StockDeducted`, `StockAdjusted`. |

### 3.8 Supplier

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `Supplier`, `PurchaseOrder`. |
| **Value objects** | `SupplierId`, `PurchaseOrderId`, line item (productId, qty, price). |
| **Repository** | `SupplierRepository`, `PurchaseOrderRepository` (interface di domain). |
| **Events** | `PurchaseOrderCreated`, `GoodsReceiptCreated`. |

### 3.9 Accounting

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `Journal` (dengan `JournalEntry`), optional `Account` (chart of accounts). |
| **Value objects** | `JournalId`, `AccountId`, `JournalEntry` (account, debit, credit, reference). |
| **Repository** | `IJournalRepository`, `IAccountRepository`. |
| **Events** | `JournalPosted` (jika perlu). |

### 3.10 Workflow / Queue

| Item | Keterangan |
|------|------------|
| **Aggregate root** | `WorkOrder` (atau `KitchenTicket` untuk F&B). |
| **Value objects** | `WorkOrderId`, `Station`, status enum (PENDING, IN_PROGRESS, COMPLETED). |
| **Repository** | `WorkOrderRepository` (interface di domain). |
| **Events** | `WorkOrderCreated`, `WorkOrderStarted`, `WorkOrderCompleted`; F&B: `StaffAssignedToOrder`. |

### 3.11 Reporting

| Item | Keterangan |
|------|------------|
| **Catatan** | Domain layer tipis; fokus pada **read model** dan **query** di aplikasi/infrastruktur. Bisa definisikan **report type** atau **dashboard definition** sebagai value object jika disimpan. |
| **Catatan offline-first** | Local reports berjalan offline (Room queries). Cloud-only aggregate reports (multi-terminal, multi-outlet) tersedia saat cloud mode aktif. |

### 3.12 Sync (Supporting Domain — Baru)

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `SyncSession` (represents a sync batch operation), `SyncQueueEntry` (individual entity change waiting to sync), `ConflictRecord` (unresolved conflicts). |
| **Value objects** | `SyncStatus` (SYNCED, PENDING_UPLOAD, CONFLICT), `EntityType`, `SyncVersion`. |
| **Entities (dalam boundary)** | `SyncQueueEntry`: entityType, entityId, operation (CREATE/UPDATE/DELETE), payload (JSON), retryCount, nextRetryAt, status (QUEUED/IN_PROGRESS/FAILED/COMPLETED). |
| **Repository** | `SyncQueueRepository`, `ConflictRepository`, `SyncLogRepository` (interface di domain). |
| **Events** | `SyncCompleted(terminalId, pushCount, pullCount)`, `SyncFailed(terminalId, reason)`, `ConflictDetected(entityType, entityId)`, `ConflictResolved(entityType, entityId, resolution)`. |
| **Interface kunci** | `SyncEngine` (notifyChange, startPeriodicSync, syncNow, observeSyncStatus). Di standalone: `NoOpSyncEngine`. Di cloud mode: `CloudSyncEngine`. |
| **Catatan** | Semua syncable entity harus implement `Syncable` interface: id, tenantId, outletId, createdAt, updatedAt, createdByTerminalId, updatedByTerminalId, syncStatus, syncVersion, deletedAt (soft delete). Detail: [Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md) Section 3-4. |

### 3.13 Licensing (Support)

| Item | Keterangan |
|------|------------|
| **Komponen utama** | `LicenseVerifier` (Ed25519 offline verification), `ActivationRepository` (orchestrate activation flow), `LicenseRevalidator` (periodic online check + grace period). |
| **Value objects** | `SignedLicense` (sn, applicationId, deviceId, licenseType, maxDevices, expiry, signature, payloadBase64), `ActivationState` (Idle, Loading, Success, Error). |
| **External API** | `AppRegApi` — 4 endpoints: challenge, activate, reactivate, validate. Retrofit interface. |
| **Storage** | `LicenseStorage` — EncryptedSharedPreferences (Android Keystore-backed). |
| **Security** | Ed25519 signature verification (BouncyCastle), certificate pinning (production), Play Integrity API (production), device binding (Widevine ID). |
| **Build variants** | `dev` source set: dummy PlayIntegrityHelper. `prod` source set: real Play Integrity. |
| **App flow** | Startup: load license → Ed25519 verify → device check → expiry check → app active. Background: online revalidation (7-day grace period). No license → Activation screen. |
| **Catatan** | Licensing gates app access. Must be checked before onboarding/login flow. Ref: [android-integration.md](external-integration/android-integration.md) |

---

## 4. Shared Value Objects (Cross-Context)

Definisikan sekali, pakai di banyak context:

| Value object | Field | Dipakai di |
|--------------|-------|------------|
| **Money** | currency (code), amount (decimal) | Transaction, Catalog, Accounting, Pricing |
| **ProductRef** (read-only snapshot) | productId, name, price, taxCode | Transaction (dari Catalog via ACL) |
| **TenantId / OutletId / TerminalId** | (value) | Semua context (scope & sync tracking) |
| **SyncMetadata** | syncStatus, syncVersion, createdByTerminalId, updatedByTerminalId, deletedAt | Semua syncable entity (embedded, bukan inheritance) |

---

## 5. Repository Interface (Ringkas) — Konvensi Kotlin

Daftar **interface** yang didefinisikan di **domain layer** (implementasi di modul **data**). Di Kotlin tidak memakai prefix `I`:

- `TenantRepository`, `OutletRepository`, `UserRepository`
- `TenantSettingsRepository`, `OutletSettingsRepository`
- `ProductRepository`, `CategoryRepository`
- `SaleRepository`, `CashierSessionRepository`, `TableRepository` (F&B), `SalesChannelRepository`
- `CustomerRepository`
- `PriceListRepository`, `CouponRepository`
- `StockLevelRepository`, `StockMovementRepository`
- `SupplierRepository`, `PurchaseOrderRepository`
- `JournalRepository`, `AccountRepository`
- `WorkOrderRepository`
- `TerminalRepository` (Identity & Access)
- `TerminalSettingsRepository` (Settings)
- `SyncQueueRepository`, `ConflictRepository`, `SyncLogRepository` (Sync)

**Signature contoh (Kotlin)**: `suspend fun getById(id: SaleId): Sale?`, `suspend fun save(sale: Sale)`, `fun streamByOutlet(outletId: OutletId): Flow<List<Sale>>`. Hindari leak query kompleks ke domain; spesifikasi di application layer/use case bila perlu.

**ID Generation**: Semua entity ID menggunakan **ULID** (Universally Unique Lexicographically Sortable Identifier) — sortable by time, unique across devices tanpa koordinasi, offline-safe. Tidak ada auto-increment ID.

---

## 6. Kotlin & Clean Architecture — Ringkasan

- **Domain** module: entity, value object, aggregate, repository **interface**, domain event. Tanpa dependency Android/Room/Retrofit.
- **Use case** (application layer): class yang menerima repository via constructor; `suspend fun invoke(...)` atau `Flow`; dipanggil dari **ViewModel**.
- **Data** module: implement repository interface; Room DAO, DTO, mapper (DTO ↔ domain model).
- **Presentation** (MVVM): ViewModel memanggil use case di `viewModelScope.launch`; state ke View via `StateFlow`/`LiveData`. Lihat [Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md).

---

## 7. Kesimpulan

- **Dokumen arsitektur (DDD_Core_Support_Architecture + DDD_FnB_Detail + Offline_First_Cloud_Sync_Architecture)** sudah **cukup untuk mulai** implementasi domain layer.
- **Stack**: Android, Kotlin, MVVM, Clean Architecture — detail di [Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md).
- Gunakan **panduan ini** sebagai checklist: aggregate, value object, events, repository (interface) per context; konvensi Kotlin (tanpa prefix `I`, `value class` untuk ID, `sealed class` untuk state).
- Tetapkan **state machine** (status Order/Sale, CashierSession) dan **payload event** saat menulis kode; bisa didokumentasikan di code atau doc terpisah.
- Mulai dari **Identity & Access** (termasuk Terminal) dan **Settings** (termasuk SyncSettings), lalu **Catalog** dan **Transaction**; context lain mengikuti sesuai kebutuhan fitur.
- **Offline-first dari hari pertama**: Semua entity sudah menyertakan sync metadata (syncStatus, syncVersion, terminalId) meski sync belum aktif. SyncEngine menggunakan NoOpSyncEngine di Phase 1 (Standalone). Lihat [Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md) untuk detail arsitektur sync dan implementation phases.
