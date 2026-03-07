# Arsitektur DDD Core & Support Domain — PoS Multi-Line

Dokumen ini mendefinisikan arsitektur Domain-Driven Design (DDD) untuk platform **Point of Sale (PoS)** yang dapat dipakai dan dikembangkan untuk berbagai line of business: **Retail**, **F&B**, **Laundry**, **Service** (Salon, Barber shop, spa, dll.), **Bengkel**, dan line lainnya. Pendekatan **Core vs Supporting Domain** memastikan domain inti bisnis (differentiator) terpisah dari domain pendukung yang dapat dipakai ulang di semua line.

**Stack implementasi**: Aplikasi PoS dikembangkan dengan **Android**, **Kotlin**, **MVVM**, dan **Clean Architecture**. Pemetaan DDD ke lapisan (domain, data, presentation), Use Case, serta konvensi Kotlin dijelaskan di **[Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md)**.

---

## 1. Tujuan & Prinsip

### 1.1 Tujuan

- **Reusability**: Domain pendukung (Support) dipakai konsisten di semua line (Retail, F&B, Laundry, Service, Bengkel).
- **Extensibility**: Domain inti (Core) dapat dikembangkan spesifik per line tanpa mengacaukan domain bersama.
- **Modern PoS**: Mengadopsi pola arsitektur PoS modern: multi-tenant, multi-outlet, headless/API-first, event-driven, offline-first.
- **Offline-First**: Aplikasi berfungsi 100% tanpa internet. Local DB (Room) sebagai source of truth saat offline. Sync ke cloud bersifat opsional dan non-blocking.
- **Cloud-Ready**: Arsitektur disiapkan untuk sync ke self-hosted cloud API. Saat cloud aktif: mendukung multi PoS point, multi terminal pelayan, multi-tenant, dan multi-outlet. Detail lengkap: **[Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md)**.

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
| **Licensing** | Support | Aktivasi lisensi via AppReg, Ed25519 signed license, offline verification, periodic revalidation. |

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

**Tanggung jawab**: Siklus hidup penjualan: keranjang → order → pembayaran → selesai/batal/refund. Mendukung tunai, non-tunai, split, tip, tax lines, service charge, dan integrasi payment gateway.

**Konsep utama**:
- **Sale / Order**: satu transaksi penjualan (bisa draft → confirmed → paid → completed); terikat ke satu **Sales Channel**.
- **Sales Channel**: entity configurable per tenant yang menentukan asal pesanan, aturan bisnis, dan **harga yang berlaku**. ChannelType: DINE_IN, TAKE_AWAY, DELIVERY_PLATFORM, OWN_DELIVERY. Jumlah channel tidak terbatas (GoFood, GrabFood, ShopeeFood, dll. masing-masing channel terpisah).
- **Line Item**: baris order (referensi Catalog, qty, **price snapshot sudah termasuk channel pricing**, discount).
- **Payment**: satu atau banyak pembayaran per order (method, amount, reference). Untuk DELIVERY_PLATFORM: `PlatformPayment` (gross, commission, net, settlement status).
- **Session**: sesi kasir (open/close, float, reconciliation).

**Aggregates**:
- `Sale` (atau `Order`): Aggregate Root; mengandung `LineItem`, `Payment`.
- `CashierSession`: Aggregate Root terpisah.
- `SalesChannel`: Aggregate Root; mengandung `PlatformConfig` (untuk DELIVERY_PLATFORM).

**Invariants**:
- Total payment >= total amount due (grandTotal = subtotal + tax + service charge + tip); status hanya berubah menurut state machine yang valid.
- Line item merekam harga pada saat transaksi (**price snapshot = harga efektif channel**, bukan base price).
- Setiap channel bisa punya harga berbeda: Dine In, Take Away, GoFood, GrabFood, ShopeeFood — semuanya configurable.
- Tax, service charge, dan tip di-snapshot di Sale saat kalkulasi (`TaxLine`, `ServiceChargeLine`, `TipLine`). Detail model: [DDD_FnB_Detail.md](DDD_FnB_Detail.md) Section 3.8.

**Events**: `SaleCreated`, `LineItemAdded`, `PaymentReceived`, `SaleCompleted`, `SaleVoided`, `RefundIssued`, `SalesChannelCreated`, `SalesChannelUpdated`, `PlatformSettlementReceived`.

Detail channel pricing untuk F&B: [DDD_FnB_Detail.md](DDD_FnB_Detail.md) Section 3.

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

**Tanggung jawab**: Autentikasi, otorisasi, multi-tenant, outlet, terminal, registrasi device, dan sesi kasir (dapat di-share dengan Transaction untuk konsep “session”).

**Konsep utama**:
- **Tenant / Organization**: pemilik bisnis (brand).
- **Outlet / Store / Branch**: lokasi fisik.
- **User**, **Role**, **Permission**: RBAC.
- **Terminal**: device/station per outlet. Setiap device Android yang menjalankan IntiKasir adalah Terminal dengan type: CASHIER, WAITER, KITCHEN_DISPLAY, atau MANAGER. Terminal adalah first-class entity untuk mendukung multi-device dan sync.
- **Session**: login session; bisa dikaitkan dengan CashierSession.

**Aggregates**:
- `Tenant`, `Outlet`, `User`: masing-masing Aggregate Root.
- `Terminal`: Aggregate Root — menyimpan terminalId (UUID), type, status, dan sync metadata. Di mode standalone: 1 terminal auto-created. Di mode cloud: terminal diregistrasi ke cloud API.
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

**Tanggung jawab**: Konfigurasi tenant/outlet/terminal: pajak, mata uang, format receipt, printer, numbering (invoice, receipt), business hours, **tax/service charge/tip configuration**, **cloud sync configuration**.

**Konsep utama**:
- **TaxConfig**: Konfigurasi pajak per tenant (PPN, PB1, dll.) — rate, isIncludedInPrice, scope (ALL_ITEMS / SPECIFIC_CATEGORIES / SPECIFIC_ITEMS). Bisa >1 pajak aktif bersamaan. Detail model: [DDD_FnB_Detail.md](DDD_FnB_Detail.md) Section 3.8.
- **ServiceChargeConfig**: Biaya layanan per tenant — rate (%), applicable channel types (e.g. hanya DINE_IN), isIncludedInPrice. Master toggle `isEnabled`.
- **TipConfig**: Konfigurasi tip — suggested percentages, allowCustomAmount, applicable channel types. Master toggle `isEnabled`.
- **ReceiptTemplate**, **PrinterConfig**, **NumberingSequence**.
- **SyncSettings**: cloud API URL, sync interval, real-time toggle, conflict strategy, bandwidth management. Master toggle `cloudSyncEnabled` menentukan mode Standalone vs Cloud-Connected.
- **TerminalSettings**: capabilities per device (canProcessPayment, canModifyProduct, maxDiscountPercent, dll.) berdasarkan TerminalType + override.

**Aggregates**:
- `TenantSettings`, `OutletSettings`, `TerminalSettings`: konfigurasi per scope dengan hierarchy: Terminal > Outlet > Tenant Default.
- `SyncSettings`: embedded di TenantSettings (tenant-wide toggle) dengan override di OutletSettings.
- Tax, Service Charge, dan Tip dikonfigurasi di level Tenant (default) dengan override di level Outlet. Contoh: Outlet A service charge 5%, Outlet B 10%.

---

### 3.10 Pricing & Promotion (Support)

**Tanggung jawab**: Harga dasar, **harga per sales channel**, harga tier (customer/segment), diskon (item/cart), voucher, bundle. Catalog memegang “base price”; Pricing memegang PriceList dan rules override.

**Konsep utama**:
- **PriceList**: Daftar harga per product. Digunakan untuk **channel-specific pricing** — setiap SalesChannel bisa merujuk ke PriceList tertentu (contoh: PriceList “GoFood”, PriceList “Take Away”). Jika tidak ada PriceList, pakai base price + adjustment.
- **PriceRule**: Rule harga per product, customer segment, channel, waktu (happy hour).
- **Discount**, **Coupon**, **Bundle**: applied di Transaction setelah channel pricing.

**Channel-specific pricing flow**:
1. Base price dari Catalog
2. Channel override: PriceList channel ATAU markup/discount dari SalesChannel config
3. Modifier delta
4. Discount/promotion (jika ada)
5. = Price snapshot (frozen di OrderLine)

**Contoh**: Nasi Goreng base Rp25.000 → Dine In: Rp25.000 (base), Take Away: Rp27.500 (markup 10%), GoFood: Rp30.000 (markup 20%), ShopeeFood: Rp28.000 (PriceList khusus).

**Integrasi**: Transaction meminta “harga efektif” ke Pricing berdasarkan SalesChannel + MenuItem; harga di-snapshot di OrderLine. Detail F&B: [DDD_FnB_Detail.md](DDD_FnB_Detail.md) Section 3.3.

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

### 3.12 Licensing (Support)

**Tanggung jawab**: Aktivasi dan validasi lisensi aplikasi via AppReg License Server. Gatekeeper sebelum app bisa digunakan.

**Konsep utama**:
- **Serial Number (SN)**: Kode lisensi yang diinput user untuk mengaktivasi app.
- **Signed License**: License payload yang ditandatangani server dengan Ed25519. Disimpan lokal, diverifikasi offline.
- **Challenge-Response**: Nonce dari server (5 menit TTL, single-use) untuk mencegah replay attack.
- **Play Integrity**: Google Play Integrity API untuk verifikasi bahwa app berjalan di device resmi (production only).
- **Device Binding**: License terikat ke device ID (Widevine ID / ANDROID_ID).
- **Offline Verification**: Ed25519 signature check tanpa network, dilakukan setiap app startup.
- **Periodic Revalidation**: Online check ke server, 7-hari grace period jika offline.

**Komponen utama**:
- `LicenseVerifier`: Verifikasi Ed25519 signature + device binding + expiry (domain/pure logic).
- `ActivationRepository`: Orchestrate challenge→integrity→activate→verify→save.
- `LicenseRevalidator`: Periodic online check + grace period logic.
- `LicenseStorage`: EncryptedSharedPreferences (Android Keystore-backed).
- `AppRegApi`: Retrofit interface ke AppReg server (challenge, activate, reactivate, validate).

**Integrasi**: Licensing → Identity & Access (gating app access). Startup flow: License check → Activation screen atau lanjut ke Login/Onboarding.

Referensi: [docs/external-integration/android-integration.md](external-integration/android-integration.md)

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

- **Sale / Order**: Satu transaksi penjualan dari draft sampai selesai; terikat ke satu **Sales Channel**.
- **Sales Channel**: Kanal penjualan configurable per tenant (Dine In, Take Away, GoFood, GrabFood, ShopeeFood, dll.); menentukan aturan bisnis dan **harga yang berlaku**. Setiap channel bisa punya harga berbeda.
- **Line Item**: Satu baris di Sale (product/service + qty + **price snapshot sudah termasuk channel pricing**).
- **Payment**: Satu pembayaran (method + amount); untuk delivery platform: `PlatformPayment` (gross, commission, net, settlement status).
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
- **Offline-first**: Semua operasi CRUD terjadi di local DB terlebih dahulu. Sync ke cloud bersifat background dan non-blocking. Aplikasi HARUS berfungsi 100% tanpa koneksi internet.
- **Cloud-ready**: Self-hosted cloud API (bukan Firebase/third-party BaaS). Saat aktif: mendukung multi PoS, multi pelayan, multi tenant, multi outlet. Migrasi Standalone → Cloud reversible.
- **Sync strategy**: Push-Pull with versioning. Semua entity memiliki sync metadata (syncStatus, syncVersion, createdByTerminalId, updatedByTerminalId). ID menggunakan ULID (sortable, offline-safe).
- **Conflict resolution**: Transaction/Payment = terminal-owned (no conflict). Master data = Last-Write-Wins. Stock = cloud-computed. User/Role = cloud-authoritative.
- **Event-driven**: Domain events untuk integrasi antar context; mengurangi coupling.
- **CQRS di Reporting**: Write di context asal; read model terpisah untuk report/dashboard. Cloud-only aggregate reports saat multi-outlet.
- **Idempotency**: Payment dan mutation penting memakai idempotency key.
- **Audit**: User, terminal, timestamp, dan reason untuk void/refund/adjustment.
- **Transaction numbering**: Format `{OutletCode}-{TerminalCode}-{YYYYMMDD}-{Sequence}` — multi-terminal safe, offline-safe.
- **Versioning API**: Cloud API di-version untuk kompatibilitas.

Detail arsitektur offline-first dan cloud sync: **[Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md)**.

---

## 8. Diagram yang Tersedia

| Diagram | File | Keterangan |
|---------|------|------------|
| Klasifikasi Core vs Support | [01-domain-classification.mmd](diagrams/01-domain-classification.mmd) | Bubble: Core vs Supporting domain |
| Context Map | [02-context-map.mmd](diagrams/02-context-map.mmd) | Semua Bounded Context dan hubungan |
| Transaction Context | [03-transaction-context.mmd](diagrams/03-transaction-context.mmd) | Aggregate & event Transaction |
| Catalog Context | [04-catalog-context.mmd](diagrams/04-catalog-context.mmd) | Aggregate Catalog |
| Identity & Access | [05-identity-access-context.mmd](diagrams/05-identity-access-context.mmd) | Tenant, Outlet, User, Role, Terminal |
| Integrasi & Events | [06-integration-events.mmd](diagrams/06-integration-events.mmd) | Alur event antar context |
| Mapping per Line | [07-pos-lines-mapping.mmd](diagrams/07-pos-lines-mapping.mmd) | Domain vs Retail/F&B/Laundry/Service/Bengkel |

### Diagram Sync / Offline-First

| Diagram | File | Keterangan |
|---------|------|------------|
| Sync Engine Architecture | [sync-01-sync-engine-architecture.mmd](diagrams/sync-01-sync-engine-architecture.mmd) | Komponen SyncEngine dan flow data |
| Push-Pull Sequence | [sync-02-push-pull-sequence.mmd](diagrams/sync-02-push-pull-sequence.mmd) | Sequence diagram push/pull |
| Conflict Resolution | [sync-03-conflict-resolution-flow.mmd](diagrams/sync-03-conflict-resolution-flow.mmd) | Decision tree conflict resolution |
| Multi-Terminal Topology | [sync-04-multi-terminal-topology.mmd](diagrams/sync-04-multi-terminal-topology.mmd) | Deployment topology multi-device |
| Initial Sync Sequence | [sync-05-initial-sync-sequence.mmd](diagrams/sync-05-initial-sync-sequence.mmd) | First-time device registration & sync |
| Offline/Online Transition | [sync-06-offline-to-online-transition.mmd](diagrams/sync-06-offline-to-online-transition.mmd) | State machine: offline ↔ online |

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
