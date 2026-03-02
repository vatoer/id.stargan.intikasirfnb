# Arsitektur DDD — F&B (Food & Beverage) — Dokumen Detil

Dokumen ini merupakan **branch/spesifikasi detil** dari arsitektur base PoS ([DDD_Core_Support_Architecture.md](DDD_Core_Support_Architecture.md)) untuk line bisnis **F&B** (restoran, kafe, warung, dll.). Semua domain base tetap dipakai; di sini didefinisikan perluasan dan aturan khusus F&B: **channel transaksi** (Dine In, Take Away, Ojol A, Ojol B), **resep opsional untuk COGS**, serta best practice PoS F&B.

Implementasi teknis (Android, Kotlin, MVVM, Clean Architecture) mengikuti [Android_Kotlin_MVVM_Clean_Architecture.md](Android_Kotlin_MVVM_Clean_Architecture.md).

---

## 1. Hubungan dengan Base Arsitektur

- **Base**: Transaction, Catalog, Inventory, Identity & Access, Customer, Accounting, Reporting, Settings, Pricing & Promotion, Workflow/Queue.
- **F&B menambah/ mengkhususkan**:
  - **Transaction**: *Order Channel* (dine in, take away, Ojol A, Ojol B); *Table* (untuk dine in); *Meja*, *Tiket Dapur*.
  - **Catalog**: *MenuItem*, *Modifier/Add-on*, *Resep* (opsional) untuk COGS.
  - **Workflow/Queue**: *Kitchen Display*, antrian dapur/bar.
  - **Inventory**: pengurangan stok berdasarkan *resep* (bahan per porsi) bila resep dipakai.

Diagram: [F&B Context dalam base](diagrams/fnb-01-fnb-context-overview.mmd).

---

## 2. Ubiquitous Language — F&B

| Istilah | Arti |
|--------|------|
| **Order / Pesanan** | Satu transaksi penjualan F&B; punya *channel* (dine in, take away, Ojol A, Ojol B) dan siklus hidup (draft → confirmed → paid → completed). |
| **Order Channel / Tipe Pesanan** | Asal pesanan: **Dine In**, **Take Away**, **Ojol A**, **Ojol B** (dan bisa ditambah channel lain). Memengaruhi alur (meja, receipt, integrasi eksternal). |
| **Dine In** | Makan di tempat; pesanan terikat **Meja**; bisa split bill per meja. |
| **Take Away** | Bungkus/bawa pulang; tidak ada meja; nomor order untuk pengambilan. |
| **Ojol A / Ojol B** | Pesanan dari platform delivery (contoh: GrabFood, GoFood); punya *order_id* eksternal, status kirim, dan mungkin fee/platform. |
| **Meja (Table)** | Representasi meja fisik; satu meja bisa punya satu Order aktif (dine in). |
| **MenuItem** | Item yang dijual (makanan/minuman); punya harga, kategori, modifier, dan opsional **Resep**. |
| **Modifier / Add-on** | Opsi pada MenuItem (level pedas, tambah telur, size); bisa berimbas harga. |
| **Order Line / Baris Pesanan** | Satu entri di Order: MenuItem + qty + modifier + price snapshot. |
| **Resep (Recipe)** | Opsional: daftar **Bahan (Ingredient)** + qty per porsi untuk satu MenuItem; dipakai untuk COGS dan pengurangan stok. |
| **Bahan (Ingredient)** | Item di Inventory yang dipakai di Resep (bahan baku). |
| **Tiket Dapur (Kitchen Ticket)** | Satu unit kerja ke dapur/bar; dibuat dari Order (atau subset order line) untuk disiapkan. |
| **COGS (Cost of Goods Sold)** | Harga pokok penjualan; bila Resep ada, dihitung dari pemakaian bahan (qty × cost) per order line. |
| **Sesi Kasir (Cashier Session)** | Periode kasir bertanggung jawab atas laci; open/close, float, rekonsiliasi. |

---

## 3. Order Channel (Pilihan Transaksi)

Setiap **Order** memiliki **Order Channel** yang menentukan alur dan aturan bisnis.

### 3.1 Daftar Channel

| Channel | Kode (contoh) | Ciri | Meja | Receipt / Nomor | Integrasi |
|---------|----------------|------|------|------------------|-----------|
| **Dine In** | `DINE_IN` | Makan di tempat | Wajib pilih meja | Receipt untuk meja, bisa per bill | — |
| **Take Away** | `TAKE_AWAY` | Bungkus/bawa pulang | Tidak ada | Nomor order (antrian ambil) | — |
| **Ojol A** | `OJOL_A` | Pesanan dari platform A (mis. GrabFood) | Tidak ada | Nomor order + id eksternal | API/platform A |
| **Ojol B** | `OJOL_B` | Pesanan dari platform B (mis. GoFood) | Tidak ada | Nomor order + id eksternal | API/platform B |

Channel lain (e.g. Delivery sendiri, Drive Thru) dapat ditambah dengan pola yang sama.

### 3.2 Aturan per Channel

- **Dine In**
  - Order harus punya `tableId` (meja aktif).
  - Satu meja satu order aktif; order selesai/batal → meja kosong.
  - Split bill: tetap satu Order, beberapa Payment; atau kebijakan “bill per kursi” (implementasi spesifik).
- **Take Away**
  - Tidak ada meja; optional `customerReference` atau nomor antrian.
  - Nomor order unik untuk pengambilan (bisa dicetak di receipt/sticker).
- **Ojol A / Ojol B**
  - `externalOrderId` (dan optional `externalPlatform`) wajib untuk rekonsiliasi.
  - Status “ready for pickup” / “dispatched” bisa disimpan di Order atau di modul integrasi.
  - Fee/platform dan rounding bisa jadi line item terpisah atau adjustment (tergantung kebijakan).

### 3.3 Invariants (Transaction Context)

- `OrderChannel` adalah value object (enum atau sealed type); set sekali saat Order dibuat, tidak berubah.
- Dine In: `tableId` wajib; Take Away / Ojol: `tableId` null.
- Ojol: `externalOrderId` dan identifikasi platform (A/B) wajib.

Diagram: [Alur per channel](diagrams/fnb-02-transaction-channels.mmd).

---

## 4. Catalog F&B: MenuItem & Resep (Opsional) untuk COGS

### 4.1 MenuItem (dalam Catalog Context)

- **MenuItem**: Aggregate Root; nama, kategori, harga dasar, pajak, availability (aktif/non-aktif), urutan tampil.
- **Modifier Group & Modifier**: grup opsi (mis. “Level pedas”, “Size”) dan pilihan (Pedas, Extra pedas; Regular, Large) dengan delta harga opsional.
- **Category**: hierarki untuk menu (Makanan, Minuman, Snack, dll.).

### 4.2 Resep (Recipe) — Opsional

- **Resep** melekat pada **MenuItem** (one-to-one opsional).
- Resep = daftar **RecipeLine**: Ingredient (referensi ke Inventory/Catalog bahan) + quantity per porsi (UoM konsisten).
- Jika MenuItem **tidak** punya resep: tidak ada auto-deduct stok dari resep dan tidak ada COGS dari resep (COGS bisa manual atau nol).
- Jika MenuItem **punya** resep:
  - Saat **SaleCompleted**: Inventory bisa mengurangi stok bahan sesuai `quantity × orderLine.qty`.
  - **COGS** per order line = sum (bahan × qty × cost per unit); cost bisa dari moving average atau standard cost (Inventory/Accounting).

### 4.3 COGS dan Event

- **Accounting** (atau modul COGS) mendengarkan **SaleCompleted**.
- Untuk tiap Order Line yang punya MenuItem dengan Resep: hitung COGS dari resep + cost bahan; posting jurnal COGS (debit COGS, kredit Inventory).
- Jika tidak ada Resep: COGS = 0 atau diisi manual (kebijakan tenant).

Diagram: [Resep & COGS](diagrams/fnb-03-recipe-cogs-flow.mmd).

---

## 5. Bounded Context — Ringkasan F&B

### 5.1 Transaction (Sales) — F&B

- **Aggregates**: `Order` (root), `CashierSession` (root).
- **Order**: `channel` (OrderChannel), `tableId` (nullable), `externalOrderId` (nullable), `lines`, `payments`, status.
- **Value objects**: `OrderChannel`, `Money`, `OrderLine` (menuItemRef, qty, modifierSnapshot, priceSnapshot).
- **Events**: `OrderCreated`, `OrderChannelSet`, `LineItemAdded`, `PaymentReceived`, `OrderCompleted`, `OrderVoided`; untuk integrasi Ojol: `OrderReadyForPickup`, `OrderDispatched`.

### 5.2 Catalog — F&B

- **Aggregates**: `MenuItem` (root), `Category` (root).
- **MenuItem**: optional `Recipe` (aggregate atau entity di dalam MenuItem).
- **Recipe**: list of `RecipeLine` (ingredientId, quantity, uom).
- **Events**: `MenuItemCreated`, `MenuItemUpdated`, `RecipeUpdated`, `CategoryStructureChanged`.

### 5.3 Workflow / Kitchen — F&B

- **Aggregates**: `KitchenTicket` (root) — satu tiket bisa untuk satu Order atau per station (makanan vs minuman).
- **Events**: `KitchenTicketCreated` (dari OrderConfirmed), `KitchenTicketStarted`, `KitchenTicketCompleted`.
- Best practice: Kitchen Display System (KDS) baca dari antrian KitchenTicket dan update status.

### 5.4 Inventory — F&B (dengan Resep)

- Saat **SaleCompleted**: untuk setiap order line yang punya MenuItem dengan Resep, emit **StockDeductionRequest** (atau event) berisi (ingredientId, quantity × line.qty).
- Inventory memproses pengurangan stok; optional: lot/expiry (FIFO/FEFO) untuk bahan.

---

## 6. Context Map — F&B (Ringkas)

- **Transaction → Catalog**: ACL; baca MenuItem (id, name, price, tax, modifier) + snapshot saat tambah line.
- **Transaction → Workflow**: Event `OrderConfirmed` → buat KitchenTicket (untuk channel yang butuh produksi).
- **Transaction → Inventory**: Event `OrderCompleted`/`SaleCompleted` → deduct by recipe (jika resep ada).
- **Transaction → Accounting**: Event `OrderCompleted` → jurnal penjualan; COGS dari resep → jurnal COGS (bila ada).
- **Transaction → Customer**: Opsional; Order bisa link ke Customer (loyalty, riwayat).
- **Ojol A/B**: Adapter/ACL ke API eksternal; Order simpan external id dan status; sync status “ready”/“dispatched” ke platform.

---

## 7. Best Practice PoS F&B

- **Channel jelas di UI**: Pilih channel (Dine In / Take Away / Ojol A / Ojol B) sebelum atau di awal order; tampilkan di header order dan receipt.
- **Meja hanya untuk Dine In**: Pilih meja saat buat order dine in; validasi meja kosong; lepaskan meja saat order selesai/batal.
- **Nomor order unik per channel**: Format nomor bisa beda per channel (mis. DI-001, TA-001, OA-001, OB-001) untuk memudahkan operasional dan laporan.
- **Kitchen ticket per station**: Pisahkan tiket dapur (makanan) dan bar (minuman) bila ada; status per station.
- **Modifier dan price snapshot**: Setiap order line simpan modifier yang dipilih + harga per line (setelah modifier) agar receipt dan laporan konsisten.
- **Resep opsional**: Tenant bisa menyalakan “pakai resep” per MenuItem; tanpa resep, tidak ada auto-deduct dan COGS dari resep; dengan resep, deduct + COGS otomatis.
- **Offline & sync**: Order bisa dibuat offline (terutama di device kasir); sync saat online; conflict resolution by “last write” atau sequence.
- **Idempotency**: Payment dan submit order (terutama dari Ojol) pakai idempotency key agar tidak double charge / double order.
- **Audit**: Void, refund, dan ubah order dicatat dengan user, waktu, dan alasan.
- **Receipt & cetak**: Template receipt bisa beda per channel (dine in vs take away vs Ojol); nomor order dan barcode/QR untuk pengambilan.
- **Laporan per channel**: Reporting bisa slice penjualan per Order Channel (dine in vs take away vs Ojol A/B) untuk analisis.

---

## 8. Diagram yang Tersedia (F&B)

| Diagram | File | Keterangan |
|---------|------|------------|
| F&B dalam base | [fnb-01-fnb-context-overview.mmd](diagrams/fnb-01-fnb-context-overview.mmd) | Posisi F&B relatif terhadap base context |
| Channel transaksi | [fnb-02-transaction-channels.mmd](diagrams/fnb-02-transaction-channels.mmd) | Dine In, Take Away, Ojol A, Ojol B — alur & aturan |
| Resep & COGS | [fnb-03-recipe-cogs-flow.mmd](diagrams/fnb-03-recipe-cogs-flow.mmd) | Resep opsional, deduct stok, perhitungan COGS |

---

Dokumen ini menjadi acuan implementasi **branch F&B** dari base PoS: channel transaksi (Dine In, Take Away, Ojol A, Ojol B), resep opsional untuk COGS, dan praktik PoS F&B yang solid.
