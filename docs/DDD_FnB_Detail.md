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
| **Sales Channel** | Entity configurable per tenant: menentukan asal pesanan, aturan bisnis, dan **harga yang berlaku**. Setiap channel punya ChannelType dan optional pricing override (PriceList atau markup/discount). Contoh: "Dine In", "Take Away", "GoFood", "GrabFood", "ShopeeFood", "Delivery Sendiri". |
| **Channel Type** | Kategori umum channel (enum built-in): DINE_IN, TAKE_AWAY, DELIVERY_PLATFORM, OWN_DELIVERY. |
| **Dine In** | Makan di tempat; pesanan terikat **Meja**; bisa split bill per meja. Harga bisa sama atau beda dari Take Away. |
| **Take Away** | Bungkus/bawa pulang; tidak ada meja; nomor order untuk pengambilan. Harga bisa beda dari Dine In (configurable). |
| **Delivery Platform** | Pesanan dari platform 3rd party (GoFood, GrabFood, ShopeeFood, dll.); punya *external order id*, komisi platform, settlement flow, dan **harga per channel** (biasanya markup dari base price). Jumlah platform tidak terbatas — ditambah sesuai kebutuhan. |
| **Platform Config** | Konfigurasi per delivery platform: nama, komisi%, tipe komisi, metode pembayaran (settlement/COD), auto-confirm. |
| **Settlement** | Proses pembayaran dari platform ke merchant. Status: PENDING → SETTLED. Perlu rekonsiliasi. |
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

Setiap **Order** memiliki **Order Channel** yang menentukan alur, aturan bisnis, dan **harga yang berlaku**.

### 3.1 Channel Type & Platform Configuration

Order channel terdiri dari 2 konsep:

1. **ChannelType** — Kategori umum channel (built-in, tidak bisa dihapus):

```kotlin
enum class ChannelType {
    DINE_IN,        // Makan di tempat
    TAKE_AWAY,      // Bungkus / bawa pulang
    DELIVERY_PLATFORM,  // 3rd party delivery (GoFood, GrabFood, ShopeeFood, dll.)
    OWN_DELIVERY,   // Delivery sendiri (driver sendiri)
}
```

2. **SalesChannel** — Entity configurable per tenant. Setiap SalesChannel merujuk ke ChannelType dan bisa punya konfigurasi harga & platform sendiri:

```kotlin
data class SalesChannel(
    val channelId: SalesChannelId,
    val tenantId: TenantId,
    val channelType: ChannelType,
    val name: String,                     // “Dine In”, “Take Away”, “GoFood”, “GrabFood”, “ShopeeFood”
    val code: String,                     // “DI”, “TA”, “GF”, “GB”, “SF” — untuk nomor transaksi
    val isActive: Boolean,
    val sortOrder: Int,

    // --- Pricing ---
    val priceListId: PriceListId?,        // null = pakai harga base dari Catalog
                                          // non-null = pakai harga dari PriceList khusus channel ini

    val priceAdjustmentType: PriceAdjustmentType?,  // MARKUP_PERCENT, MARKUP_FIXED, DISCOUNT_PERCENT, DISCOUNT_FIXED, null
    val priceAdjustmentValue: BigDecimal?,           // e.g. 20 (untuk markup 20%)
    // Harga efektif = base price + adjustment, ATAU harga dari PriceList (jika di-set)

    // --- Platform 3rd Party (hanya untuk DELIVERY_PLATFORM) ---
    val platformConfig: PlatformConfig?,  // null untuk DINE_IN, TAKE_AWAY, OWN_DELIVERY
)

data class PlatformConfig(
    val platformName: String,             // “GoFood”, “GrabFood”, “ShopeeFood”
    val commissionPercent: BigDecimal,    // e.g. 20% (potongan platform dari penjualan)
    val commissionType: CommissionType,   // FROM_MENU_PRICE / FROM_SELLING_PRICE
    val requiresExternalOrderId: Boolean, // true — wajib isi external order ID
    val paymentMethod: PlatformPaymentMethod, // PLATFORM_SETTLEMENT / CASH_ON_DELIVERY
    val autoConfirmOrder: Boolean,        // true = langsung confirmed saat dibuat (platform sudah confirm)
    val platformNotes: String?,           // Catatan tambahan
)

enum class CommissionType {
    FROM_MENU_PRICE,    // Komisi dihitung dari harga menu (sebelum markup)
    FROM_SELLING_PRICE, // Komisi dihitung dari harga jual (setelah markup)
}

enum class PlatformPaymentMethod {
    PLATFORM_SETTLEMENT,  // Platform bayar ke merchant via transfer (AR/receivable)
    CASH_ON_DELIVERY,     // Driver bayar cash ke merchant saat pickup
}
```

### 3.2 Daftar Channel Default (Pre-configured)

Saat Tenant pertama kali dibuat, channel default:

| Channel | ChannelType | Code | PriceList | Adjustment | Platform |
|---------|-------------|------|-----------|------------|----------|
| **Dine In** | DINE_IN | DI | null (base price) | null | — |
| **Take Away** | TAKE_AWAY | TA | null (base price) | null | — |

**Semua channel bisa dikonfigurasi harganya** — termasuk Dine In dan Take Away. Contoh skenario:
- Dine In dan Take Away beda harga → set PriceList atau adjustment per channel
- Take Away markup 5% dari Dine In → set `MARKUP_PERCENT 5` di channel Take Away
- Dine In pakai harga base, Take Away pakai PriceList terpisah

Channel 3rd party dan channel lain ditambahkan oleh user sesuai kebutuhan:

| Channel | ChannelType | Code | PriceList | Adjustment | Platform |
|---------|-------------|------|-----------|------------|----------|
| **GoFood** | DELIVERY_PLATFORM | GF | null | MARKUP_PERCENT 20% | GoFood, 20% commission, PLATFORM_SETTLEMENT |
| **GrabFood** | DELIVERY_PLATFORM | GB | null | MARKUP_PERCENT 25% | GrabFood, 25% commission, PLATFORM_SETTLEMENT |
| **ShopeeFood** | DELIVERY_PLATFORM | SF | PriceList “Shopee” | null | ShopeeFood, 18% commission, PLATFORM_SETTLEMENT |
| **Delivery Sendiri** | OWN_DELIVERY | DS | null | MARKUP_FIXED 5000 | — |

**Contoh skenario harga per channel (Nasi Goreng base price Rp25.000):**

| Channel | Mekanisme | Harga Jual | Keterangan |
|---------|-----------|------------|------------|
| **Dine In** | Base price (default) | Rp25.000 | Harga standar |
| **Take Away** | MARKUP_PERCENT 10% | Rp27.500 | Lebih mahal karena packaging |
| **GoFood** | MARKUP_PERCENT 20% | Rp30.000 | Cover komisi platform |
| **GrabFood** | MARKUP_PERCENT 25% | Rp31.250 | Komisi GrabFood lebih tinggi |
| **ShopeeFood** | PriceList “Shopee” | Rp28.000 | Harga manual per item |
| **Delivery Sendiri** | MARKUP_FIXED 5000 | Rp30.000 | Flat markup |

Atau dengan PriceList terpisah per channel (full control):

| Channel | Mekanisme | Keterangan |
|---------|-----------|------------|
| **Dine In** | PriceList “Dine In” | Harga terpisah per item untuk dine-in |
| **Take Away** | PriceList “Take Away” | Harga terpisah per item untuk take-away |
| **GoFood** | PriceList “GoFood” | Harga terpisah per item untuk GoFood |

> **Prinsip**: Setiap SalesChannel bisa punya harga sendiri. Pemilik restoran bebas memilih: (a) semua channel harga sama (default), (b) markup/discount per channel, atau (c) harga manual per item per channel via PriceList.

### 3.3 Channel-Specific Pricing Flow

```
Harga efektif per OrderLine:

1. Ambil base price dari Catalog (MenuItem.basePrice)
2. Cek SalesChannel dari Order:
   a. Jika channel punya PriceListId → ambil harga dari PriceList untuk item ini
      - Jika item tidak ada di PriceList → fallback ke base price + adjustment
   b. Jika channel punya priceAdjustmentType → hitung:
      - MARKUP_PERCENT:  basePrice × (1 + adjustmentValue/100)
      - MARKUP_FIXED:    basePrice + adjustmentValue
      - DISCOUNT_PERCENT: basePrice × (1 - adjustmentValue/100)
      - DISCOUNT_FIXED:   basePrice - adjustmentValue
   c. Jika keduanya null → pakai base price (Dine In, Take Away default)
3. Modifier price delta ditambahkan di atas harga efektif channel
4. Simpan sebagai priceSnapshot di OrderLine (frozen at transaction time)
```

```kotlin
// Di domain layer — Pricing service / use case
fun resolvePrice(
    menuItem: MenuItem,
    channel: SalesChannel,
    modifiers: List<Modifier>,
    priceListRepository: PriceListRepository,
): Money {
    // 1. Base price
    val basePrice = menuItem.basePrice

    // 2. Channel price
    val channelPrice = when {
        channel.priceListId != null -> {
            val listPrice = priceListRepository.getPrice(channel.priceListId, menuItem.id)
            listPrice ?: applyAdjustment(basePrice, channel) // fallback
        }
        else -> applyAdjustment(basePrice, channel)
    }

    // 3. Modifier delta
    val modifierDelta = modifiers.sumOf { it.priceDelta.amount }

    return channelPrice + Money(modifierDelta, channelPrice.currency)
}
```

### 3.4 Aturan per Channel Type

- **DINE_IN**
  - Order harus punya `tableId` (meja aktif).
  - Satu meja satu order aktif; order selesai/batal → meja kosong.
  - Split bill: tetap satu Order, beberapa Payment; atau kebijakan “bill per kursi” (implementasi spesifik).
  - Harga: base price (default) atau sesuai channel config.

- **TAKE_AWAY**
  - Tidak ada meja; optional `customerReference` atau nomor antrian.
  - Nomor order unik untuk pengambilan (bisa dicetak di receipt/sticker).
  - Harga: base price (default) atau sesuai channel config.

- **DELIVERY_PLATFORM** (GoFood, GrabFood, ShopeeFood, dll.)
  - `externalOrderId` wajib (dari platform) untuk rekonsiliasi.
  - `channelId` menentukan platform mana (GoFood vs GrabFood vs ShopeeFood).
  - Harga: **per-channel pricing** — bisa markup% atau PriceList terpisah.
  - Payment: biasanya `PLATFORM_SETTLEMENT` (bukan cash langsung).
  - Platform commission: dicatat untuk reporting dan rekonsiliasi.
  - Status “ready for pickup” / “dispatched” disimpan di Order.
  - Auto-confirm: jika `autoConfirmOrder=true`, order langsung CONFIRMED (skip DRAFT).
  - Fee/platform dan rounding bisa jadi line item terpisah atau adjustment (tergantung kebijakan).

- **OWN_DELIVERY**
  - Delivery menggunakan kurir sendiri.
  - Optional: ongkos kirim sebagai line item atau fee terpisah.
  - Harga: sesuai channel config (bisa sama dengan dine-in atau markup).

### 3.5 Platform Settlement & Accounting

```
Flow pembayaran 3rd party platform:

1. Customer bayar ke platform (GoFood/GrabFood/ShopeeFood)
2. Order masuk ke PoS → Payment method = “GoFood Settlement”
3. Accounting: Debit AR/Piutang Platform, Credit Revenue
4. Saat platform transfer ke rekening merchant (settlement):
   - Debit Cash/Bank
   - Credit AR/Piutang Platform
   - Selisih (komisi) = Debit Expense/Biaya Komisi Platform
5. Rekonsiliasi: cocokkan settlement platform dengan AR di sistem
```

```kotlin
// Payment untuk 3rd party platform order
data class PlatformPayment(
    val paymentMethod: PaymentMethod,        // GOFOOD_SETTLEMENT, GRABFOOD_SETTLEMENT, etc.
    val grossAmount: Money,                  // Total harga jual ke customer
    val platformCommission: Money,           // Potongan platform (computed from commissionPercent)
    val netAmount: Money,                    // Yang akan diterima merchant (grossAmount - commission)
    val expectedSettlementDate: LocalDate?,  // Estimasi kapan platform transfer
    val settlementStatus: SettlementStatus,  // PENDING, SETTLED, DISPUTED
    val settlementReference: String?,        // Nomor transfer / batch dari platform
)

enum class SettlementStatus {
    PENDING,    // Belum di-settle oleh platform
    SETTLED,    // Sudah masuk ke rekening merchant
    DISPUTED,   // Ada selisih / dispute
}
```

### 3.6 Invariants (Transaction Context)

- `SalesChannel` direferensi via `channelId` di Order; set sekali saat Order dibuat, tidak berubah.
- DINE_IN: `tableId` wajib; TAKE_AWAY / DELIVERY_PLATFORM / OWN_DELIVERY: `tableId` null.
- DELIVERY_PLATFORM: `externalOrderId` wajib jika `platformConfig.requiresExternalOrderId = true`.
- Price snapshot di OrderLine HARUS sudah memperhitungkan channel pricing (markup/pricelist).
- Platform commission dihitung saat Order completed, bukan saat dibuat (harga bisa berubah jika ada void line).

### 3.7 Reporting per Channel

Reporting harus bisa memfilter dan mengelompokkan berdasarkan:
- **Per channel**: Penjualan Dine In vs Take Away vs GoFood vs GrabFood vs ShopeeFood
- **Per channel type**: Semua DELIVERY_PLATFORM digabung vs per platform
- **Revenue vs Net**: Gross revenue (harga jual ke customer) vs Net revenue (setelah komisi platform)
- **Commission tracking**: Total komisi per platform per periode
- **Settlement reconciliation**: AR platform vs actual settlement

Diagram: [Alur per channel](diagrams/fnb-02-transaction-channels.mmd).

---

## 3.8 Tax, Service Charge & Tip

### 3.8.1 Model Overview

Setiap transaksi (Sale/Order) bisa memiliki 3 jenis tambahan biaya yang dikonfigurasi di Settings:

```kotlin
// === Tax (Pajak) ===

data class TaxConfig(
    val taxId: TaxId,
    val tenantId: TenantId,
    val name: String,                       // "PPN", "PB1", "Pajak Restoran"
    val code: String,                       // "PPN", "PB1"
    val rate: BigDecimal,                   // 0.11 (11%), 0.10 (10%)
    val isIncludedInPrice: Boolean,         // true = harga sudah termasuk pajak; false = ditambahkan di atas
    val applicableTo: TaxScope,             // ALL_ITEMS, SPECIFIC_CATEGORIES, SPECIFIC_ITEMS
    val applicableCategoryIds: List<CategoryId>?,  // jika SPECIFIC_CATEGORIES
    val applicableProductIds: List<ProductId>?,    // jika SPECIFIC_ITEMS
    val isActive: Boolean,
    val sortOrder: Int,                     // urutan penerapan (jika ada pajak bertingkat)
)

enum class TaxScope {
    ALL_ITEMS,             // Berlaku untuk semua item
    SPECIFIC_CATEGORIES,   // Hanya kategori tertentu (e.g. makanan kena PB1, minuman tidak)
    SPECIFIC_ITEMS,        // Hanya item tertentu
}

// === Service Charge (Biaya Layanan) ===

data class ServiceChargeConfig(
    val tenantId: TenantId,
    val isEnabled: Boolean,                   // Master toggle
    val rate: BigDecimal,                     // 0.05 (5%), 0.10 (10%)
    val type: ChargeType,                     // PERCENTAGE, FIXED_AMOUNT
    val fixedAmount: Money?,                  // Jika FIXED_AMOUNT
    val applicableChannelTypes: Set<ChannelType>,  // e.g. {DINE_IN} — hanya dine-in kena service charge
    val isIncludedInPrice: Boolean,           // true = sudah included; false = ditambahkan
    val name: String,                         // "Service Charge", "Biaya Layanan"
)

enum class ChargeType {
    PERCENTAGE,     // % dari subtotal
    FIXED_AMOUNT,   // Nominal tetap per transaksi
}

// === Tip (Gratifikasi Pelanggan) ===

data class TipConfig(
    val tenantId: TenantId,
    val isEnabled: Boolean,                   // Tampilkan opsi tip di payment screen
    val suggestedPercentages: List<Int>,       // e.g. [5, 10, 15] — pilihan cepat di UI
    val allowCustomAmount: Boolean,           // User bisa input nominal bebas
    val applicableChannelTypes: Set<ChannelType>,  // e.g. {DINE_IN, TAKE_AWAY} — biasanya tidak untuk Ojol
)
```

### 3.8.2 Penerapan di Order / Sale

```kotlin
data class Sale(
    // ... existing fields (channelId, tableId, lines, payments, status) ...

    // === Tax, Service Charge, Tip ===
    val taxLines: List<TaxLine>,             // Rincian pajak yang berlaku
    val serviceCharge: ServiceChargeLine?,   // null jika tidak applicable
    val tip: TipLine?,                       // null jika tidak ada tip

    // === Calculated Totals ===
    // subtotal       = sum of all OrderLine.lineTotal (channel-adjusted price × qty)
    // totalTax       = sum of taxLines.amount
    // totalServiceCharge = serviceCharge?.amount ?: 0
    // totalTip       = tip?.amount ?: 0
    // grandTotal     = subtotal + totalTax + totalServiceCharge + totalTip
    //                  (jika tax/sc included in price, maka tidak ditambahkan lagi)
)

data class TaxLine(
    val taxId: TaxId,
    val taxName: String,                     // "PPN 11%", "PB1 10%"
    val taxRate: BigDecimal,                 // 0.11
    val taxableAmount: Money,                // Subtotal yang kena pajak ini
    val taxAmount: Money,                    // Jumlah pajak
    val isIncluded: Boolean,                 // Jika true: sudah termasuk di harga, tidak menambah grand total
)

data class ServiceChargeLine(
    val name: String,                        // "Service Charge 5%"
    val rate: BigDecimal,                    // 0.05
    val baseAmount: Money,                   // Subtotal yang jadi basis
    val amount: Money,                       // Jumlah service charge
    val isIncluded: Boolean,                 // Jika true: sudah termasuk di harga
)

data class TipLine(
    val amount: Money,                       // Jumlah tip yang diberikan customer
    val method: TipMethod,                   // Bagaimana tip dikumpulkan
)

enum class TipMethod {
    ADDED_TO_BILL,     // Tip ditambahkan ke total tagihan (customer bayar sekaligus)
    SEPARATE_CASH,     // Tip cash terpisah (tidak masuk tagihan, dicatat saja)
}
```

### 3.8.3 Kalkulasi Urutan

```
1. subtotal = sum(OrderLine.lineTotal)                 → harga channel × qty per line
2. discount = applied discounts (item-level + cart-level)
3. subtotalAfterDiscount = subtotal - discount
4. serviceCharge = subtotalAfterDiscount × serviceChargeRate  (jika enabled & channel applicable)
5. taxableAmount = subtotalAfterDiscount + serviceCharge      (atau tanpa SC, tergantung aturan pajak)
6. tax = taxableAmount × taxRate                              (per TaxConfig, bisa >1 pajak)
   - Jika isIncludedInPrice: tax sudah di dalam harga, tidak menambah total
   - Jika NOT included: tax ditambahkan ke total
7. tip = customer-entered amount (opsional)
8. grandTotal = subtotalAfterDiscount
               + serviceCharge (jika NOT included)
               + tax (jika NOT included)
               + tip (jika ADDED_TO_BILL)

Contoh (Dine In, Nasi Goreng Rp25.000 × 2 + Es Teh Rp8.000 × 2):
  subtotal             = Rp66.000
  discount             = Rp0
  service charge (5%)  = Rp3.300
  PB1 (10%)           = Rp6.930   (dari Rp66.000 + Rp3.300 = Rp69.300)
  tip                  = Rp5.000
  grandTotal           = Rp66.000 + Rp3.300 + Rp6.930 + Rp5.000 = Rp81.230

Contoh (Take Away, same items, no service charge):
  subtotal             = Rp66.000
  service charge       = Rp0 (SC hanya untuk DINE_IN)
  PB1 (10%)           = Rp6.600
  tip                  = Rp0
  grandTotal           = Rp72.600
```

### 3.8.4 Konfigurasi di Settings

Tax, Service Charge, dan Tip dikonfigurasi di level **Tenant** (default) dengan override di level **Outlet**:

```
TenantSettings:
  taxes: [
    { name: "PB1", rate: 10%, included: false, scope: ALL_ITEMS },
  ]
  serviceCharge: { enabled: true, rate: 5%, channels: [DINE_IN] }
  tip: { enabled: true, suggested: [5, 10, 15], channels: [DINE_IN] }

OutletSettings (Cabang Kemang — override):
  taxes: [
    { name: "PB1", rate: 10%, included: false, scope: ALL_ITEMS },
    { name: "PPN", rate: 11%, included: true, scope: SPECIFIC_CATEGORIES, categories: ["Minuman Beralkohol"] },
  ]
  serviceCharge: { enabled: true, rate: 10% }  → override rate jadi 10%
  tip: inherit from tenant

OutletSettings (Cabang Depok — tax-free zone):
  taxes: []  → no tax
  serviceCharge: { enabled: false }
```

### 3.8.5 Receipt Display

```
Receipt contoh (Dine In, Cabang Kemang):
────────────────────────────────
  Nasi Goreng      2 × Rp25.000
  Es Teh Manis     2 ×  Rp8.000
────────────────────────────────
  Subtotal              Rp66.000
  Service Charge (10%)   Rp6.600
  PB1 (10%)              Rp7.260
  Tip                    Rp5.000
────────────────────────────────
  TOTAL                 Rp84.860
────────────────────────────────

Receipt contoh (GoFood, harga markup 20%):
────────────────────────────────
  Nasi Goreng      2 × Rp30.000
  Es Teh Manis     2 ×  Rp9.600
────────────────────────────────
  Subtotal              Rp79.200
  PB1 (10%)              Rp7.920
  (Service Charge: N/A - delivery)
────────────────────────────────
  TOTAL                 Rp87.120
────────────────────────────────
```

### 3.8.6 Accounting Treatment

| Item | Debit | Credit | Keterangan |
|------|-------|--------|------------|
| **Tax (PB1/PPN)** | — | Utang Pajak | Pajak dikumpulkan dari customer, disetor ke pemerintah |
| **Service Charge** | — | Pendapatan Service Charge (atau Utang SC ke karyawan) | Tergantung kebijakan: revenue atau dibagi ke staff |
| **Tip** | — | Utang Tip ke Karyawan | Tip bukan revenue restoran; dibagikan ke staff |

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
- **Order**: `channelId` (SalesChannelId), `tableId` (nullable), `externalOrderId` (nullable), `lines`, `payments`, `taxLines` (list), `serviceCharge` (nullable), `tip` (nullable), status.
- **Value objects**: `SalesChannelId`, `Money`, `OrderLine` (menuItemRef, qty, modifierSnapshot, **priceSnapshot sudah termasuk channel pricing**), `PlatformPayment` (untuk delivery platform), `TaxLine` (taxId, taxRate, taxableAmount, taxAmount, isIncluded), `ServiceChargeLine` (name, rate, baseAmount, amount, isIncluded), `TipLine` (amount, method).
- **Kalkulasi**: subtotal → discount → serviceCharge → tax → tip → grandTotal. Detail urutan dan contoh: Section 3.8.3.
- **Events**: `OrderCreated`, `LineItemAdded`, `PaymentReceived`, `OrderCompleted`, `OrderVoided`; untuk delivery platform: `OrderReadyForPickup`, `OrderDispatched`, `PlatformSettlementReceived`.

### 5.1b Sales Channel & Pricing — F&B

- **Aggregates**: `SalesChannel` (root, configurable per tenant).
- **Entities di dalamnya**: `PlatformConfig` (VO embedded di SalesChannel untuk DELIVERY_PLATFORM type).
- **Value objects**: `ChannelType` (DINE_IN, TAKE_AWAY, DELIVERY_PLATFORM, OWN_DELIVERY), `PriceAdjustmentType`, `CommissionType`, `PlatformPaymentMethod`, `SettlementStatus`.
- **Integrasi dengan Pricing**: SalesChannel bisa merujuk ke `PriceListId` (harga per-item per-channel) ATAU `priceAdjustmentType` + `priceAdjustmentValue` (markup/discount global).
- **Events**: `SalesChannelCreated`, `SalesChannelUpdated`, `SalesChannelDeactivated`.

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
