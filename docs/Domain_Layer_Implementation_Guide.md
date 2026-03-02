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
| 1 | **Identity & Access** | Tenant, Outlet, User diperlukan untuk scope semua context lain. |
| 2 | **Settings** | Tax, currency, numbering dipakai Transaction & Catalog. |
| 3 | **Catalog** | Product/Category dipakai Transaction (ACL) dan Inventory. |
| 4 | **Transaction** | Inti PoS; bergantung Catalog (read), IAM, Settings. |
| 5 | **Customer** | Opsional di Transaction; bisa setelah Transaction. |
| 6 | **Pricing & Promotion** | Diperlukan Transaction untuk harga efektif; bisa sederhana dulu (base price dari Catalog). |
| 7 | **Inventory** | Dipakai setelah Transaction (event SaleCompleted). |
| 8 | **Supplier** | Dipakai Inventory (goods receipt) dan Accounting. |
| 9 | **Accounting** | Subscribe event dari Transaction, Inventory, Supplier. |
| 10 | **Workflow / Queue** | Opsional per line (F&B: KitchenTicket); bisa setelah Transaction. |
| 11 | **Reporting** | Read model / query; bisa setelah context tulis stabil. |

---

## 3. Checklist per Bounded Context

Untuk tiap context: definisikan **Aggregate Root**, **entity/value object di dalam boundary**, **domain events**, dan **repository interface**. Referensi lengkap ada di [DDD_Core_Support_Architecture.md](DDD_Core_Support_Architecture.md); F&B di [DDD_FnB_Detail.md](DDD_FnB_Detail.md).

### 3.1 Identity & Access

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `Tenant`, `Outlet`, `User`. |
| **Value objects** | `TenantId`, `OutletId`, `UserId`, `Role`, `Permission`, `TerminalId`. |
| **Entities (dalam boundary)** | Di `User`: referensi Role/Permission (bisa VO atau entity kecil). |
| **Repository** | `TenantRepository`, `OutletRepository`, `UserRepository` (interface di domain). |
| **Events** | `UserLoggedIn`, `UserLoggedOut`, `RoleAssigned` (jika perlu). |

### 3.2 Settings

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `TenantSettings`, `OutletSettings` (atau satu aggregate per scope). |
| **Value objects** | `TaxRate`, `TaxRule`, `Money` (currency + amount), `NumberingSequenceConfig`. |
| **Repository** | `TenantSettingsRepository`, `OutletSettingsRepository` (interface di domain). |
| **Events** | `SettingsUpdated` (opsional). |

### 3.3 Catalog

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `Product` (base), `Category`. Untuk F&B: `MenuItem` extends/specialisasi Product. |
| **Value objects** | `ProductId`, `CategoryId`, `Money`, `UnitOfMeasure`, `Variant` (size/option). |
| **Entities (dalam boundary)** | Di Product: variant list; di F&B MenuItem: ModifierGroup, Modifier; **Recipe** (opsional) → `RecipeLine` (ingredientId, quantity, uom). |
| **Repository** | `ProductRepository`, `CategoryRepository`. F&B: `MenuItemRepository` bila terpisah (interface di domain). |
| **Events** | `ProductCreated`, `ProductUpdated`, `ProductDeactivated`, `CategoryStructureChanged`, `RecipeUpdated`. |

### 3.4 Transaction

| Item | Keterangan |
|------|------------|
| **Aggregate roots** | `Sale` (atau `Order`), `CashierSession`. |
| **Value objects** | `SaleId`, `OrderChannel` (DINE_IN, TAKE_AWAY, OJOL_A, OJOL_B), `Money`, `OrderLine` (productRef, qty, priceSnapshot, discount), `Payment` (method, amount, reference). |
| **Entities (dalam boundary)** | Di Sale: `OrderLine`, `Payment` (sebagai entity atau VO). |
| **Invariants** | Total payment ≥ total due; status hanya transisi valid; line item price snapshot; Dine In → tableId wajib; Ojol → externalOrderId wajib. |
| **Repository** | `SaleRepository`, `CashierSessionRepository`. Untuk F&B: `TableRepository` (meja). Interface di domain. |
| **Events** | `SaleCreated`, `LineItemAdded`, `PaymentReceived`, `SaleCompleted`, `SaleVoided`, `RefundIssued`; F&B: `OrderReadyForPickup`, `OrderDispatched`. |

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
| **Aggregate roots** | `PriceList`, `PriceRule`; atau `Discount`, `Coupon` (tergantung granularitas). |
| **Value objects** | `PriceRule` (product/category/customer segment, amount/percent). |
| **Repository** | `PriceListRepository`, `CouponRepository` (jika ada; interface di domain). |
| **Catatan** | Bisa mulai sederhana: harga efektif = base price dari Catalog; rules tambahan iterasi berikutnya. |

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

---

## 4. Shared Value Objects (Cross-Context)

Definisikan sekali, pakai di banyak context:

| Value object | Field | Dipakai di |
|--------------|-------|------------|
| **Money** | currency (code), amount (decimal) | Transaction, Catalog, Accounting, Pricing |
| **ProductRef** (read-only snapshot) | productId, name, price, taxCode | Transaction (dari Catalog via ACL) |
| **TenantId / OutletId** | (value) | Semua context (scope) |

---

## 5. Repository Interface (Ringkas) — Konvensi Kotlin

Daftar **interface** yang didefinisikan di **domain layer** (implementasi di modul **data**). Di Kotlin tidak memakai prefix `I`:

- `TenantRepository`, `OutletRepository`, `UserRepository`
- `TenantSettingsRepository`, `OutletSettingsRepository`
- `ProductRepository`, `CategoryRepository`
- `SaleRepository`, `CashierSessionRepository`, `TableRepository` (F&B)
- `CustomerRepository`
- `PriceListRepository`, `CouponRepository`
- `StockLevelRepository`, `StockMovementRepository`
- `SupplierRepository`, `PurchaseOrderRepository`
- `JournalRepository`, `AccountRepository`
- `WorkOrderRepository`

**Signature contoh (Kotlin)**: `suspend fun getById(id: SaleId): Sale?`, `suspend fun save(sale: Sale)`, `fun streamByOutlet(outletId: OutletId): Flow<List<Sale>>`. Hindari leak query kompleks ke domain; spesifikasi di application layer/use case bila perlu.

---

## 6. Kotlin & Clean Architecture — Ringkasan

- **Domain** module: entity, value object, aggregate, repository **interface**, domain event. Tanpa dependency Android/Room/Retrofit.
- **Use case** (application layer): class yang menerima repository via constructor; `suspend fun invoke(...)` atau `Flow`; dipanggil dari **ViewModel**.
- **Data** module: implement repository interface; Room DAO, DTO, mapper (DTO ↔ domain model).
- **Presentation** (MVVM): ViewModel memanggil use case di `viewModelScope.launch`; state ke View via `StateFlow`/`LiveData`. Lihat [Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md).

---

## 7. Kesimpulan

- **Dokumen arsitektur (DDD_Core_Support_Architecture + DDD_FnB_Detail)** sudah **cukup untuk mulai** implementasi domain layer.
- **Stack**: Android, Kotlin, MVVM, Clean Architecture — detail di [Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md).
- Gunakan **panduan ini** sebagai checklist: aggregate, value object, events, repository (interface) per context; konvensi Kotlin (tanpa prefix `I`, `value class` untuk ID, `sealed class` untuk state).
- Tetapkan **state machine** (status Order/Sale, CashierSession) dan **payload event** saat menulis kode; bisa didokumentasikan di code atau doc terpisah.
- Mulai dari **Identity & Access** dan **Settings**, lalu **Catalog** dan **Transaction**; context lain mengikuti sesuai kebutuhan fitur.
