# Arsitektur DDD Core & Support Domain — PoS Multi-Line

Dokumen ini mendefinisikan arsitektur Domain-Driven Design (DDD) untuk platform **Point of Sale (PoS)** yang dapat dipakai dan dikembangkan untuk berbagai line of business: **Retail**, **F&B**, **Laundry**, **Service** (Salon, Barber shop, spa, dll.), **Bengkel**, dan line lainnya. Pendekatan **Core vs Supporting Domain** memastikan domain inti bisnis (differentiator) terpisah dari domain pendukung yang dapat dipakai ulang di semua line.

**Stack implementasi**: Aplikasi PoS dikembangkan dengan **Android**, **Kotlin**, **MVVM**, dan **Clean Architecture**. Pemetaan DDD ke lapisan (domain, data, presentation), Use Case, serta konvensi Kotlin dijelaskan di **[Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md)**.

---

## 1. Tujuan & Prinsip

### 1.1 Tujuan

- **Reusability**: Domain pendukung (Support) dipakai konsisten di semua line (Retail, F&B, Laundry, Service, Bengkel).
- **Extensibility**: Domain inti (Core) dapat dikembangkan spesifik per line tanpa mengacaukan domain bersama.
- **Modern PoS**: Mengadopsi pola arsitektur PoS modern: multi-tenant, multi-outlet, headless/API-first, event-driven, offline-capable.

### 1.2 Strategic Design: Core vs Supporting

| Klasifikasi | Definisi | Contoh di PoS |
|-------------|----------|----------------|
| **Core Domain** | Differentiator bisnis; investasi utama; bisa berbeda per line | Transaction, Catalog (product vs service vs job), Workflow/Queue (antrian dapur/laundry/bengkel) |
| **Supporting Domain** | Penting tapi bukan differentiator; bisa dibeli/build sekali pakai ulang | Identity & Access, Customer, Supplier, Accounting, Reporting, Settings, Inventory (dasar) |

Diagram: [Klasifikasi Core vs Support](diagrams/01-domain-classification.mmd).

---

## 2. Daftar Domain & Bounded Contexts

### 2.1 Ringkasan Domain

| Domain | Tipe | Deskripsi Singkat |
|--------|------|-------------------|
| **Catalog** | Core | Produk, layanan, paket, kategori; berbeda per line (SKU vs menu vs layanan vs job type). |
| **Transaction** | Core | Penjualan, order, keranjang, pembayaran, void/refund; jantung PoS. |
| **Inventory** | Support (dapat diperkaya per line) | Stok barang, lot, expiry; opsional untuk pure service. |
| **Customer** | Support | Profil, alamat, riwayat, segmentasi, loyalty. |
| **Identity & Access** | Support | User, role, permission, tenant, outlet, session. |
| **Supplier** | Support | Vendor, pembelian, terms; untuk retail/F&B. |
| **Accounting** | Support | Jurnal, GL, AR/AP, rekonsiliasi, laporan keuangan. |
| **Reporting** | Support | Dashboard, report ad-hoc, export, analytics. |
| **Settings** | Support | Tenant, outlet, terminal, tax, currency, receipt, printer. |
| **Pricing & Promotion** | Support | Harga, diskon, bundle, voucher, loyalty points. |
| **Workflow / Queue** | Core (opsional per line) | Antrian order (dapur, laundry, bengkel); untuk Service: antrian & penugasan staf (siapa melayani siapa). |

Diagram: [Context Map lengkap](diagrams/02-context-map.mmd).

---

## 3. Bounded Context — Detail

### 3.1 Catalog (Core)

**Tanggung jawab**: Master data yang “dijual”: produk fisik (retail), item menu (F&B), layanan (laundry; salon/barber/spa), jenis pekerjaan/spare part (bengkel).

**Konsep utama**:
- **Product/Service/JobType**: entitas yang bisa dijual (polimorfik per line).
- **Category**: hierarki kategori (department, category, subcategory).
- **Variant**: ukuran, warna, add-on (F&B), paket (bundle).
- **Unit of Measure (UoM)**: pcs, kg, liter, paket, jam.

**Aggregates**:
- `Product` (atau `Service`, `JobType` sebagai specialisasi): Aggregate Root.
- `Category`: Aggregate Root terpisah (referensi oleh Product).

**Events**: `ProductCreated`, `ProductUpdated`, `ProductDeactivated`, `CategoryStructureChanged`.

Diagram: [Catalog Context](diagrams/04-catalog-context.mmd).

---

### 3.2 Transaction (Core)

**Tanggung jawab**: Siklus hidup penjualan: keranjang → order → pembayaran → selesai/batal/refund. Mendukung tunai, non-tunai, split, tip, dan integrasi payment gateway.

**Konsep utama**:
- **Sale / Order**: satu transaksi penjualan (bisa draft → confirmed → paid → completed).
- **Line Item**: baris order (referensi Catalog, qty, price snapshot, discount).
- **Payment**: satu atau banyak pembayaran per order (method, amount, reference).
- **Session**: sesi kasir (open/close, float, reconciliation).

**Aggregates**:
- `Sale` (atau `Order`): Aggregate Root; mengandung `LineItem`, `Payment`.
- `CashierSession`: Aggregate Root terpisah.

**Invariants**:
- Total payment ≥ total amount due; status hanya berubah menurut state machine yang valid.
- Line item merekam harga pada saat transaksi (price snapshot).

**Events**: `SaleCreated`, `LineItemAdded`, `PaymentReceived`, `SaleCompleted`, `SaleVoided`, `RefundIssued`.

Diagram: [Transaction Context](diagrams/03-transaction-context.mmd).

---

### 3.3 Inventory (Support)

**Tanggung jawab**: Stok barang (quantity, lot, expiry, location). Terkait Catalog (product) dan Transaction (pengurangan saat penjualan) serta Supplier (penerimaan barang).

**Konsep utama**:
- **Stock**: quantity per product per location (outlet/warehouse).
- **Movement**: in/out/adjustment; referensi ke Sale atau Purchase.
- **Lot/Batch** (opsional): untuk F&B, farmasi.

**Aggregates**:
- `StockLevel`: Aggregate Root (product + location).
- `StockMovement`: event-sourcing style atau append-only log.

**Events**: `StockReceived`, `StockDeducted`, `StockAdjusted`.

---

### 3.4 Customer (Support)

**Tanggung jawab**: Data pelanggan untuk layanan, loyalty, dan marketing. Dipakai oleh Transaction (opsional) dan Reporting.

**Konsep utama**:
- **Customer**: profil, kontak, alamat, segment.
- **CustomerAccount**: poin, tier, riwayat transaksi ringkas (ID saja).

**Aggregates**:
- `Customer`: Aggregate Root.

**Events**: `CustomerRegistered`, `CustomerUpdated`, `LoyaltyPointsEarned`.

---

### 3.5 Identity & Access (Support)

**Tanggung jawab**: Autentikasi, otorisasi, multi-tenant, outlet, terminal, dan sesi kasir (dapat di-share dengan Transaction untuk konsep “session”).

**Konsep utama**:
- **Tenant / Organization**: pemilik bisnis (brand).
- **Outlet / Store / Branch**: lokasi fisik.
- **User**, **Role**, **Permission**: RBAC.
- **Terminal**: device/station per outlet.
- **Session**: login session; bisa dikaitkan dengan CashierSession.

**Aggregates**:
- `Tenant`, `Outlet`, `User`: masing-masing bisa Aggregate Root.
- `Role`, `Permission`: sering value object atau entity di dalam User/tenant.

Diagram: [Identity & Access Context](diagrams/05-identity-access-context.mmd).

---

### 3.6 Supplier (Support)

**Tanggung jawab**: Vendor, purchase order, penerimaan barang. Dipakai oleh Inventory dan Accounting.

**Konsep utama**:
- **Supplier**: data vendor, terms.
- **PurchaseOrder**, **GoodsReceipt**: dokumen pembelian.

**Aggregates**:
- `Supplier`: Aggregate Root.
- `PurchaseOrder`: Aggregate Root (dengan line items).

---

### 3.7 Accounting (Support)

**Tanggung jawab**: Buku besar, jurnal dari transaksi penjualan/pembelian, AR/AP, rekonsiliasi kas.

**Konsep utama**:
- **Journal**, **JournalEntry**: double-entry.
- **Account** (Chart of Accounts): GL, AR, AP, Cash, Sales.
- **Reconciliation**: bank/cash reconciliation.

**Aggregates**:
- `Journal`: Aggregate Root (satu dokumen = satu atau banyak entry).
- `Account`: biasanya reference data; bisa aggregate terpisah.

**Integrasi**: Mendengarkan events dari Transaction (SaleCompleted → jurnal penjualan), Supplier (PO/GR → jurnal pembelian), Inventory (adjustment).

---

### 3.8 Reporting (Support)

**Tanggung jawab**: Dashboard, report standar (penjualan, stok, kasir), export, dan analytics. Membaca dari context lain via query/read model atau event-sourced projection.

**Dashboard** (ringkasan angka, grafik) masuk ke context **Reporting**. **Landing page** (halaman depan berisi daftar icon menu navigasi, seperti di aplikasi keuangan) bukan bagian satu context — ia layar shell di **app** yang mengarahkan ke berbagai feature (POS, Katalog, Laporan, Pengaturan, dll.).

**Konsep utama**:
- **Report Definition**, **Dashboard**, **Export** (PDF/Excel).
- **Read models**: denormalisasi dari Transaction, Inventory, Accounting.

**Pola**: CQRS — write di context asal, read via Reporting context (API query atau materialized view).

---

### 3.9 Settings (Support)

**Tanggung jawab**: Konfigurasi tenant/outlet/terminal: pajak, mata uang, format receipt, printer, numbering (invoice, receipt), business hours.

**Konsep utama**:
- **TaxRate**, **TaxRule**: per product/category/region.
- **ReceiptTemplate**, **PrinterConfig**, **NumberingSequence**.

**Aggregates**:
- `TenantSettings`, `OutletSettings`: konfigurasi per scope.

---

### 3.10 Pricing & Promotion (Support)

**Tanggung jawab**: Harga dasar, harga tier (customer/location), diskon (item/cart), voucher, bundle. Catalog memegang “base price”; Pricing memegang rules dan override.

**Konsep utama**:
- **PriceList**, **PriceRule**: per product, customer segment, channel.
- **Discount**, **Coupon**, **Bundle**: applied di Transaction.

**Integrasi**: Transaction meminta “harga efektif” ke Pricing; atau Pricing mempublikasikan event dan Transaction memakai snapshot.

---

### 3.11 Workflow / Queue (Core — opsional per line)

**Tanggung jawab**: Antrian kerja: order dapur (F&B), antrian laundry, antrian bengkel; untuk **Service (Salon, Barber, spa)** antrian pelanggan dan **penugasan staf** — siapa melayani siapa (stylist/barber/therapist assigned to customer).

**Konsep utama**:
- **WorkOrder** / **KitchenTicket** / **JobTicket**: satu unit kerja terkait Sale/Order.
- **Queue**, **Station**: antrian per stasiun (dapur, cuci, setrika, bengkel); untuk Service: antrian per staff atau per layanan.
- **Staff assignment** (Service): Order/Sale memiliki *serviced_by* — User/staff yang melayani (stylist, barber, terapis).

**Aggregates**:
- `WorkOrder`: Aggregate Root; status dan assignee (untuk Service, assignee = staff yang melayani).

**Events**: `WorkOrderCreated`, `WorkOrderStarted`, `WorkOrderCompleted`; untuk Service juga `StaffAssignedToOrder`.

Diagram alur integrasi: [Integration & Events](diagrams/06-integration-events.mmd).

---

## 4. Context Map & Integrasi

### 4.1 Hubungan Antar Context

- **Transaction → Catalog**: ACL; Transaction hanya baca Product (id, name, price snapshot, tax). Catalog adalah upstream.
- **Transaction → Customer**: Opsional; pilih customer untuk order (downstream memakai Customer).
- **Transaction → Identity & Access**: User/session untuk kasir dan outlet (downstream).
- **Transaction → Pricing & Promotion**: Hitung harga akhir dan diskon; bisa RPC atau event.
- **Transaction → Accounting**: Event `SaleCompleted` → jurnal penjualan (Transaction upstream, Accounting downstream).
- **Transaction → Inventory**: Event `SaleCompleted` → kurangi stok (Transaction upstream).
- **Transaction → Workflow/Queue**: Event `OrderConfirmed` → buat WorkOrder (F&B/Laundry/Bengkel); untuk Service (Salon/Barber) sekaligus penugasan staf (siapa melayani).
- **Inventory → Catalog**: Referensi Product; Inventory downstream.
- **Inventory → Supplier**: Goods receipt dari Supplier; Inventory consume.
- **Reporting**: Downstream ke semua; baca via read model/API.

Diagram: [Context Map](diagrams/02-context-map.mmd), [Integration Flow](diagrams/06-integration-events.mmd).

### 4.2 Pola Integrasi

- **ACL (Anti-Corruption Layer)**: Di Consumer context untuk mengonsumsi model dari Context lain (misalnya Transaction mengonsumsi Catalog tanpa ketergantungan model dalam).
- **Event-driven**: SaleCompleted, OrderConfirmed, StockDeducted — context lain subscribe.
- **RPC/API**: Untuk sinkron (misalnya “harga efektif” dari Pricing ke Transaction) bila diperlukan.

---

## 5. Pemetaan ke Line of Business

**Catatan**: **Service** di sini = bisnis berbasis layanan dengan *siapa melayani siapa*, misalnya **Salon**, **Barber shop**, **Spa** — ada order, ada staf yang melayani (stylist, barber, terapis), dan optional appointment/antrian.

| Domain | Retail | F&B | Laundry | Service (Salon/Barber/Spa) | Bengkel |
|--------|--------|-----|---------|----------------------------|--------|
| Catalog | Product, SKU, category | Menu item, variant, add-on | Layanan (cuci, setrika), paket | Layanan (potong, treatment, paket), add-on | Job type, spare part, jasa |
| Transaction | Sale, cart, payment | Order, meja, split bill | Order per item/paket | Order + **serviced_by** (staf yang melayani), deposit/tip | Job order, estimasi |
| Inventory | Wajib (stok toko) | Wajib (bahan + menu) | Sedikit (konsumen) | Opsional (produk salon) | Spare part, stok |
| Workflow/Queue | — | Dapur, bar | Cuci → setrika → selesai | **Antrian + penugasan staf** (siapa melayani siapa) | Antrian mekanik, job card |
| Customer | Loyalty, member | Reservasi, loyalty | Nomor order, member | Riwayat layanan, preferensi stylist, member | Riwayat kendaraan, pelanggan |
| Supplier | Purchase, GR | Bahan baku | — | Opsional (produk) | Spare part |
| Lainnya | Same | Same | Same | Same | Same |

Diagram: [Mapping per Line](diagrams/07-pos-lines-mapping.mmd).

---

## 6. Ubiquitous Language (Ringkas)

- **Sale / Order**: Satu transaksi penjualan dari draft sampai selesai.
- **Line Item**: Satu baris di Sale (product/service + qty + price snapshot).
- **Payment**: Satu pembayaran (method + amount).
- **Cashier Session**: Sesi kasir (open/close, float).
- **Product / Service / Job Type**: Item yang dijual (dari Catalog).
- **Category**: Kategori/hierarki untuk Catalog.
- **Stock / Movement**: Stok dan gerakan stok (Inventory).
- **Customer**: Profil pelanggan (Support).
- **Tenant, Outlet, User, Role**: Identity & Access.
- **Supplier, Purchase Order, Goods Receipt**: Pembelian.
- **Journal, Account**: Accounting.
- **Work Order / Kitchen Ticket / Job Ticket**: Satu unit antrian kerja (Workflow). Untuk Service (Salon/Barber): **serviced_by** = staf yang melayani pelanggan.

---

## 7. Best Practice PoS Modern

- **Multi-tenant**: Semua data di-scope Tenant dan (bila ada) Outlet.
- **Offline-first**: Transaction dan Catalog dapat di-sync (eventual consistency) saat offline.
- **Event-driven**: Domain events untuk integrasi antar context; mengurangi coupling.
- **CQRS di Reporting**: Write di context asal; read model terpisah untuk report/dashboard.
- **Idempotency**: Payment dan mutation penting memakai idempotency key.
- **Audit**: User, timestamp, dan reason untuk void/refund/adjustment.
- **Versioning API**: API publik (bila headless) di-version untuk kompatibilitas.

---

## 8. Diagram yang Tersedia

| Diagram | File | Keterangan |
|---------|------|------------|
| Klasifikasi Core vs Support | [01-domain-classification.mmd](diagrams/01-domain-classification.mmd) | Bubble: Core vs Supporting domain |
| Context Map | [02-context-map.mmd](diagrams/02-context-map.mmd) | Semua Bounded Context dan hubungan |
| Transaction Context | [03-transaction-context.mmd](diagrams/03-transaction-context.mmd) | Aggregate & event Transaction |
| Catalog Context | [04-catalog-context.mmd](diagrams/04-catalog-context.mmd) | Aggregate Catalog |
| Identity & Access | [05-identity-access-context.mmd](diagrams/05-identity-access-context.mmd) | Tenant, Outlet, User, Role |
| Integrasi & Events | [06-integration-events.mmd](diagrams/06-integration-events.mmd) | Alur event antar context |
| Mapping per Line | [07-pos-lines-mapping.mmd](diagrams/07-pos-lines-mapping.mmd) | Domain vs Retail/F&B/Laundry/Service/Bengkel |

---

## 9. Kesiapan Implementasi Domain Layer

**Apakah dokumen ini cukup untuk mulai implementasi domain layer?**

**Ya, cukup untuk memulai** — dengan catatan berikut.

### 9.1 Yang Sudah Cukup Jelas

- **Bounded context** dan tanggung jawab tiap domain.
- **Aggregate root** per context (Sale/Order, Product, Category, CashierSession, StockLevel, Customer, Tenant, Outlet, User, Supplier, PurchaseOrder, Journal, WorkOrder).
- **Konsep utama** (entitas, value object) dan **domain events** per context.
- **Invariants** untuk Transaction (payment ≥ due, price snapshot, state machine).
- **Context map** dan pola integrasi (ACL, event-driven).
- Untuk **F&B**: detail di [DDD_FnB_Detail.md](DDD_FnB_Detail.md) (OrderChannel, Table, Recipe, COGS).

Dari sini tim bisa mulai menulis **entity, value object, dan aggregate** di domain layer (pure domain, tanpa infrastruktur).

### 9.2 Yang Perlu Ditetapkan Saat Implementasi

- **Struktur dalam aggregate**: entity/value object di dalam boundary (mis. `OrderLine`, `Payment` sebagai entity/VO di dalam `Sale`); bisa mengikuti uraian di dokumen dan menyempurnakan saat coding.
- **State machine eksplisit**: state dan transisi untuk `Sale` (e.g. DRAFT → CONFIRMED → PAID → COMPLETED) dan `CashierSession` (OPEN → CLOSED); tetapkan enum/sealed type saat implementasi.
- **ID types**: `OrderId`, `ProductId`, `TenantId`, dll. sebagai value object; konvensi penamaan dan format (UUID vs ULID vs sequence) pilih di level aplikasi.
- **Payload domain event**: field yang dibawa tiap event (e.g. `SaleCompleted`: orderId, outletId, totalAmount, lineItems[]) — bisa ditulis saat define interface event.
- **Repository interface**: hanya disebut konseptual; daftar eksplisit (Kotlin: `SaleRepository`, `ProductRepository`, dll.) ada di [Domain_Layer_Implementation_Guide.md](Domain_Layer_Implementation_Guide.md).

### 9.3 Panduan Implementasi

- **Checklist domain layer** (aggregate, value object, repository, urutan pengerjaan): **[Domain_Layer_Implementation_Guide.md](Domain_Layer_Implementation_Guide.md)**.
- **Android, Kotlin, MVVM, Clean Architecture** (lapisan, Use Case, ViewModel, konvensi Kotlin): **[Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md)**.

---

Dokumen ini menjadi acuan untuk mengembangkan dan memperluas PoS berbagai line dengan tetap mempertahankan batas domain yang jelas dan pola integrasi yang konsisten.
