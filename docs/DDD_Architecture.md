# Arsitektur Domain-Driven Design (DDD) - IntiKasir F&B

Dokumen ini menguraikan arsitektur domain untuk aplikasi Point of Sale (PoS) yang dikhususkan untuk industri Makanan & Minuman (F&B). Pendekatan yang digunakan adalah Domain-Driven Design (DDD) untuk memastikan perangkat lunak secara akurat merefleksikan model mental dari para ahli domain (pemilik restoran, kasir, koki).

## 1. Bahasa Ubiquitous (Ubiquitous Language)

Bahasa ini adalah fondasi dari DDD, digunakan secara konsisten oleh tim developer dan para ahli domain.

-   **Pesanan (Order):** Kumpulan item menu yang dipesan oleh pelanggan dalam satu transaksi. Pesanan memiliki siklus hidup (dibuka, dikonfirmasi, dibayar, selesai, dibatalkan).
-   **Meja (Table):** Representasi meja fisik di restoran. Sebuah meja dapat memiliki satu pesanan aktif.
-   **Item Menu (Menu Item):** Produk yang dijual (mis: "Nasi Goreng", "Es Teh Manis"). Memiliki harga, deskripsi, dan mungkin resep.
-   **Katalog (Catalog):** Kumpulan dari semua Item Menu yang tersedia untuk dijual.
-   **Sales Channel (Kanal Penjualan):** Asal/jenis pesanan yang menentukan alur bisnis dan **harga yang berlaku**. Configurable per tenant — contoh: "Dine In", "Take Away", "GoFood", "GrabFood", "ShopeeFood", "Delivery Sendiri". Setiap channel bisa punya harga berbeda (markup, discount, atau PriceList terpisah). Jumlah platform 3rd party tidak terbatas.
-   **Baris Pesanan (Order Line):** Satu entri dalam sebuah pesanan, yang menunjuk ke satu Item Menu beserta jumlah dan harga saat itu. Harga sudah memperhitungkan channel pricing (misal: harga GoFood bisa beda dari harga Dine In).
-   **Pembayaran (Payment):** Transaksi finansial untuk menyelesaikan sebuah Pesanan. Bisa tunai, kartu, QRIS, atau settlement dari platform 3rd party (GoFood/GrabFood/ShopeeFood).
-   **Tiket Dapur (Kitchen Ticket):** Representasi dari pesanan yang dikirim ke dapur untuk disiapkan.
-   **Stok (Stock):** Jumlah bahan baku yang tersedia di inventaris.
-   **Bahan Baku (Ingredient):** Komponen dasar yang membentuk Item Menu (mis: "Nasi", "Telur", "Kecap").
-   **Sesi Kasir (Cashier Session):** Periode waktu dimana seorang kasir bertanggung jawab atas laci kas, dimulai dengan modal awal dan diakhiri dengan rekapitulasi.
-   **Terminal:** Device Android yang menjalankan IntiKasir. Setiap terminal memiliki identitas unik (TerminalId) dan tipe (Kasir, Pelayan, Kitchen Display, Manager). Terminal adalah unit dasar untuk sync data.
-   **Sync:** Proses sinkronisasi data antara terminal (local) dan cloud server. Bersifat opsional — aplikasi berfungsi 100% offline (Standalone mode). Saat cloud aktif, mendukung multi terminal dan multi outlet.

## 2. Bounded Contexts

Sistem PoS F&B dapat dipecah menjadi beberapa sub-domain yang kohesif dan independen.

1.  **Sales Context:**
    -   **Tanggung Jawab:** Mengelola seluruh alur penjualan, mulai dari membuka pesanan, menambah item, hingga menerima pembayaran. Ini adalah jantung dari aplikasi.
    -   **Aggregates Utama:** `Order`, `Table`.

2.  **Kitchen Context:**
    -   **Tanggung Jawab:** Mengelola pesanan yang perlu disiapkan. Menerima permintaan dari Sales, menampilkan dalam antrian, dan menandai saat sudah siap.
    -   **Aggregates Utama:** `KitchenTicket`.

3.  **Catalog Context:**
    -   **Tanggung Jawab:** Manajemen menu. Menambah, mengubah, menghapus item menu dan kategori.
    -   **Aggregates Utama:** `MenuItem`, `Category`.

4.  **Inventory Context:**
    -   **Tanggung Jawab:** Mengelola stok bahan baku. Mengurangi stok saat penjualan terjadi dan memungkinkan penambahan stok baru.
    -   **Aggregates Utama:** `Ingredient`.

5.  **Identity & Access Context:**
    -   **Tanggung Jawab:** Mengelola pengguna (staf), hak akses mereka (mis: Kasir, Manajer, Koki), dan registrasi terminal (device).
    -   **Aggregates Utama:** `User`, `Role`, `Terminal`.

6.  **Sync Context:**
    -   **Tanggung Jawab:** Mengelola sinkronisasi data antara local DB dan self-hosted cloud API. Mengelola antrian sync, retry, dan resolusi konflik.
    -   **Aggregates Utama:** `SyncSession`, `SyncQueueEntry`, `ConflictRecord`.
    -   **Catatan:** Hanya aktif saat cloud mode diaktifkan. Di standalone mode, menggunakan NoOpSyncEngine. Detail: [Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md).

## 3. Detail Bounded Context: Sales

Mari kita bedah `Sales Context` sebagai contoh.

### Aggregates

-   **`Order` (Aggregate Root):**
    -   Ini adalah entitas utama yang memastikan konsistensi transaksional. Semua perubahan pada `OrderLine` harus melalui `Order`.
    -   **Entities di dalamnya:** `OrderLine`.
    -   **Value Objects di dalamnya:** `Money` (untuk harga), `CustomerInfo`.
    -   **Invariants (Aturan yang dijaga):**
        -   Total harga pesanan harus selalu sama dengan jumlah harga semua `OrderLine`.
        -   Item tidak dapat ditambahkan ke pesanan yang sudah dibayar atau dibatalkan.
        -   Pembayaran tidak dapat melebihi total tagihan.

    ```kotlin
    // Contoh representasi Aggregate Root Order di Kotlin
    class Order(
        val id: OrderId,               // ULID, generated di device
        val tableId: TableId,
        val terminalId: TerminalId,     // Terminal yang membuat order ini
        private val lines: MutableList<OrderLine> = mutableListOf(),
        var status: OrderStatus = OrderStatus.OPEN
    ) {
        fun addItem(menuItem: MenuItem, quantity: Int) {
            if (status != OrderStatus.OPEN) {
                throw IllegalStateException("Cannot add item to a non-open order.")
            }
            // Logika untuk menambah atau menggabungkan item
            lines.add(OrderLine(menuItem.id, quantity, menuItem.price))
        }

        fun applyPayment(amount: Money) {
            // Logika untuk memproses pembayaran
            // ...
            if (isFullyPaid()) {
                status = OrderStatus.PAID
                // Terbitkan Domain Event: OrderPaid
            }
        }
        
        fun calculateTotal(): Money {
            // Hitung total dari semua 'lines'
        }
    }
    ```

### Domain Events

-   `OrderPlaced`: Saat pelanggan mengkonfirmasi pesanan mereka.
-   `OrderPaid`: Saat pesanan telah lunas dibayar.
-   `ItemAddedToOrder`: Saat item baru ditambahkan ke pesanan.
-   `TableOccupied`: Saat meja mulai digunakan untuk pesanan baru.

### Repositories

-   `OrderRepository`: Antarmuka untuk menyimpan dan mengambil `Order` aggregate.
-   `TableRepository`: Antarmuka untuk menyimpan dan mengambil `Table` aggregate.

## 4. Context Map

Peta ini menjelaskan bagaimana para Bounded Contexts berinteraksi.

-   **Sales -> Kitchen (Upstream/Downstream & Pub/Sub):**
    -   Ketika `OrderPlaced` event terjadi di **Sales Context**, **Kitchen Context** akan mendengarkan event ini.
    -   Kitchen Context kemudian akan membuat `KitchenTicket` berdasarkan data dari event tersebut.
    -   Hubungan: **Sales** (Upstream) tidak perlu tahu tentang **Kitchen** (Downstream). Komunikasi terjadi via event.

-   **Sales -> Inventory (Upstream/Downstream & Pub/Sub):**
    -   Ketika `OrderPaid` event terjadi di **Sales Context**, **Inventory Context** akan mendengarkan.
    -   **Inventory Context** kemudian mengurangi stok `Ingredient` yang relevan berdasarkan item yang terjual.

-   **Sales -> Catalog (Anti-Corruption Layer - ACL):**
    -   **Sales Context** perlu informasi harga dan nama dari `MenuItem` yang ada di **Catalog Context**.
    -   Sales akan memiliki representasi `MenuItem` sendiri yang lebih sederhana (mis: hanya `id`, `name`, `price`). Sebuah `ACL` (Anti-Corruption Layer) di sisi Sales bertanggung jawab untuk menerjemahkan data dari Catalog. Ini melindungi Sales dari perubahan internal di Catalog.

-   **Sales -> Identity & Access (Generic Subdomain):**
    -   **Sales Context** perlu mengetahui `User` (kasir) mana yang sedang login untuk diatribusikan ke sebuah `Order` atau `CashierSession`. Ini adalah hubungan permintaan/respons sederhana.

---

## 5. Offline-First & Cloud-Ready

Arsitektur ini dirancang **offline-first**:
- Semua operasi CRUD terjadi di local database (Room) terlebih dahulu
- Aplikasi berfungsi 100% tanpa koneksi internet (Standalone mode)
- Cloud sync bersifat opsional, diaktifkan via SyncSettings
- Saat cloud aktif: mendukung multi kasir, multi pelayan, multi outlet

Semua entity menyertakan **sync metadata** dari hari pertama:
- `syncStatus`: SYNCED / PENDING_UPLOAD / CONFLICT
- `syncVersion`: monotonic version counter
- `createdByTerminalId` / `updatedByTerminalId`: tracking asal perubahan
- `deletedAt`: soft delete (tidak pernah hard delete)

Detail lengkap: **[Offline_First_Cloud_Sync_Architecture.md](Offline_First_Cloud_Sync_Architecture.md)**

---
Dokumen ini adalah titik awal. Seiring pemahaman domain yang semakin dalam, model ini akan berevolusi.
