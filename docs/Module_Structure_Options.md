# Opsi Struktur Modul — Satu Modul vs Per Domain

Saat ini implementasi memakai **satu modul domain** (`:core:domain`) dan **satu modul data** (`:core:data`). Dokumen ini menjelaskan alasannya dan kapan opsi **pecah per bounded context** lebih masuk akal.

---

## 1. Yang Dipakai Sekarang: Satu Modul per Lapisan

```
core/
  domain/     ← Semua bounded context: identity, catalog, transaction, customer, settings, ...
  data/       ← Semua repository impl, Room, DAO, mapper
app/
```

- **Domain**: Semua entity, value object, aggregate, repository interface, use case dalam satu modul; pemisahan hanya lewat **paket** (e.g. `domain.identity`, `domain.catalog`, `domain.transaction`).
- **Data**: Semua implementasi repository, entity Room, DAO, mapper dalam satu modul; pemisahan lewat paket (e.g. `data.local.entity`, `data.repository`).

**Bukan** “data dan domain jadi satu modul”. Domain dan data **tetap dua modul terpisah** (`:core:domain` dan `:core:data`). Yang “jadi satu” adalah **isi** masing-masing modul: semua context (identity, catalog, transaction, dll.) digabung dalam satu modul domain dan satu modul data.

---

## 2. Alasan Pakai Satu Modul Domain & Satu Modul Data

| Alasan | Penjelasan |
|--------|------------|
| **Kesederhanaan** | Hanya 2 modul core (+ app). Gradle dan dependency mudah; build cepat. |
| **Referensi antar context** | Transaction memakai `ProductRef` (Catalog), `CustomerId` (Customer), `OutletId` (Identity). Dalam satu modul domain, ini sekadar import; tidak perlu atur dependency antar modul context. |
| **Menghindari circular dependency** | Kalau pecah per context, modul `domain:transaction` butuh `domain:catalog` (ProductRef) dan mungkin `domain:customer` (CustomerId). Kalau nanti Catalog butuh sesuatu dari Transaction, muncul circular. Satu modul domain = tidak ada circular di level modul. |
| **Cocok untuk tim kecil/sedang** | Satu codebase domain/data, boundary tetap jelas lewat paket dan DDD. |
| **Room + satu database** | Satu `PosDatabase` dan satu modul data yang implement semua repository cocok untuk aplikasi monolitik PoS. |

Jadi: **domain dan data tetap dua lapisan terpisah**; yang “satu” adalah **gabungan semua bounded context dalam satu modul domain dan satu modul data**.

---

## 3. Alternatif: Pecah per Bounded Context (Per Domain)

Struktur alternatif: **satu modul per context** per lapisan.

```
core/
  domain/
    identity/      ← Tenant, Outlet, User, TenantRepository, ...
    catalog/       ← Category, MenuItem, CategoryRepository, MenuItemRepository, ...
    transaction/   ← Sale, SaleRepository, TableRepository, ...
    customer/      ← Customer, CustomerRepository
    settings/      ← TenantSettings, OutletSettings, ...
  data/
    identity/      ← TenantRepositoryImpl, Room entities/DAOs untuk identity
    catalog/       ← CategoryRepositoryImpl, MenuItemRepositoryImpl, ...
    transaction/   ← SaleRepositoryImpl, ...
    ...
app/
```

**Keuntungan:**

- **Batas context ketat**: Compiler memaksa agar `data:catalog` tidak bergantung ke `domain:transaction`.
- **Kepemilikan per tim**: Satu tim bisa punya `domain:transaction` + `data:transaction`.
- **Scale ke microservice**: Nanti bisa pecah jadi service per context.
- **Build inkremental**: Ubah hanya di satu context → hanya modul itu (dan dependen) yang di-rebuild.

**Kerugian / tantangan:**

- **Dependency antar context**: Transaction butuh Catalog (ProductRef) dan Customer (CustomerId). Perlu modul **shared** atau aturan ketat:
  - Mis. `domain:transaction` depend ke `domain:catalog` dan `domain:customer`.
  - Hindari `domain:catalog` depend ke `domain:transaction` agar tidak circular.
- **Shared types**: `Money`, `TenantId`, `OutletId` dipakai banyak context. Perlu modul `domain:shared` yang di-depend semua context.
- **Lebih banyak modul**: Banyak `build.gradle`, konfigurasi, dan kemungkinan siklus dependency.
- **Room**: Bisa satu database tetap di satu modul `data:local` yang depend ke semua `domain:*`, atau satu modul data per context dengan schema terpisah (lebih rumit).

---

## 4. Kapan Pakai Yang Mana?

| Situasi | Rekomendasi |
|---------|-------------|
| Tim kecil/sedang, satu produk PoS, mau cepat dan maintainable | **Satu modul domain + satu modul data** (seperti sekarang). Boundary lewat paket. |
| Banyak tim, masing-masing punya bounded context jelas, mau enforce dependency ketat | **Pecah per domain** + modul shared; siapkan aturan dependency (e.g. no cycle, shared hanya di `domain:shared`). |
| Rencana ke microservice / multi-service | Pertimbangkan **pecah per domain** dari awal agar batas context sudah jelas di level modul. |

---

## 5. Kesimpulan

- **Saat ini**: Domain dan data **bukan** satu modul; ada **dua** modul: `:core:domain` dan `:core:data`. Yang “satu” adalah isi masing-masing: **semua bounded context digabung** dalam satu modul domain dan satu modul data.
- **Alasan**: Sederhana, referensi antar context (Transaction → Catalog, Customer, Identity) mudah, tidak ada circular dependency modul, cocok untuk tim kecil/sedang dan satu database Room.
- **Pecah per domain**: Bisa dipilih jika tim besar, kepemilikan per context ketat, atau rencana ke multi-service; dengan konsekuensi atur dependency (shared, arah dependensi) dan lebih banyak modul.

Jika nanti memutuskan pecah per domain, langkah berikut: buat `domain:shared`, lalu `domain:identity`, `domain:catalog`, `domain:transaction`, dll., dan atur `data:*` agar hanya depend ke domain yang relevan.

---

## 6. Presentation: Pisah per Context (Feature Modules)

**Ya — dengan arsitektur sekarang, presentation bisa dipisah per context** tanpa mengubah domain/data. Domain dan data tetap satu modul masing-masing; yang dipecah hanya **lapisan presentation** menjadi **feature module** per bounded context.

### Struktur

```
app/                    ← Shell: MainActivity, NavHost, DI, navigation graph,
                         + Landing page (halaman berisi daftar icon menu navigasi)
core/
  domain/               ← Tetap satu modul (semua context)
  data/                 ← Tetap satu modul (semua repository impl)
feature/
  identity/             ← Login, pilih outlet, session
  catalog/             ← Kelola kategori & menu item
  transaction/         ← POS: sale, keranjang, pembayaran, meja
  customer/             ← Daftar pelanggan, form customer
  settings/            ← Setting tenant/outlet (opsional digabung ke identity)
  reporting/            ← Dashboard (ringkasan/grafik) & laporan
```

### Dependency

- **`:app`** — depend ke `:core:domain`, `:core:data`, dan **semua** `:feature:*`. Tugas app: inisialisasi DI (provide repository dari data, provide use case), NavHost, dan routing ke tiap feature.
- **`:feature:catalog`** — depend **hanya** ke `:core:domain`. Tidak depend ke `:core:data` atau feature lain. ViewModel di feature ini menerima **use case** via constructor (di-inject dari app). UI hanya kenal domain model (Category, MenuItem) dan use case.
- **`:feature:transaction`** — depend hanya ke `:core:domain`. ViewModel menerima CreateSaleUseCase, AddLineItemUseCase, GetMenuItemByIdUseCase, dll.
- **`:feature:identity`**, **`:feature:customer`**, **`:feature:settings`** — sama: depend hanya ke `:core:domain`, terima use case lewat DI.

Dengan begitu, **setiap context punya modul presentation sendiri** (ViewModel + UI + state), sementara domain dan data tetap satu modul.

### Isi per feature module

| Modul | Isi (contoh) |
|-------|----------------|
| `feature:identity` | LoginScreen, OutletPickerScreen, ViewModel(s), UiState/Event |
| `feature:catalog` | CategoryListScreen, MenuItemListScreen, Form Category/MenuItem, ViewModel(s) |
| `feature:transaction` | PosScreen (keranjang + daftar menu), PaymentScreen, TablePickerScreen, ViewModel(s) |
| `feature:customer` | CustomerListScreen, CustomerFormScreen, ViewModel(s) |
| `feature:settings` | SettingsScreen(s), ViewModel(s) (bisa digabung ke identity) |
| `feature:reporting` | **Dashboard** (ringkasan angka, grafik) & layar laporan. Baca dari Transaction, Inventory, dll. via use case/query. |

**Landing page** (halaman depan berisi daftar icon menu untuk navigasi — seperti di aplikasi keuangan) **bukan** satu bounded context; ia pintu masuk ke berbagai context. Tempatkan di **`:app`**: satu layar (LandingScreen) yang menampilkan grid/daftar menu (POS, Katalog, Pelanggan, Laporan, Pengaturan, dll.) dan mengarahkan ke feature yang sesuai lewat navigation. Tidak perlu feature module terpisah.

### DI

- Repository (impl) di-provide di **`:app`** (atau modul `:core:data` jika pakai Hilt subcomponent), karena impl ada di `:core:data`.
- Use case di-provide di **`:app`** (atau modul `:core:domain`), inject repository ke use case.
- ViewModel tiap feature di-provide di **`:app`** (atau di dalam feature module dengan `@ViewModelInject` yang menerima use case). ViewModel hanya tergantung use case, tidak langsung ke repository.

### Circular dependency?

**Dengan aturan di atas, tidak ada circular dependency.** Grafik dependency berbentuk DAG (directed acyclic graph):

- **app** → feature:*, core:domain, core:data  
- **feature:*** → **hanya** core:domain  
- **core:data** → core:domain  
- **core:domain** → tidak depend ke modul lain  

Feature tidak depend ke feature lain, tidak depend ke app atau data. Jadi tidak ada siklus.

**Kapan circular bisa muncul (hindari):**

| Jangan | Alasannya |
|--------|-----------|
| Feature A depend ke Feature B | Jika nanti B butuh sesuatu dari A → siklus. |
| Feature depend ke `:core:data` | Tidak perlu; cukup terima use case dari app. |
| Modul `feature:common` yang di-depend feature lain, lalu common depend balik ke suatu feature | Siklus. |

**Aturan aman:** Setiap feature module **hanya** depend ke **`:core:domain`**. Navigasi/koordinasi antar layar (mis. dari Transaction ke Catalog) lewat **app** (navigation graph, deep link, atau event ke app), bukan lewat dependency modul feature → feature.

### Ringkasan

- **Domain & data**: tetap satu modul masing-masing (arsitektur sekarang).
- **Presentation**: bisa dipisah per context dengan **feature module** yang hanya depend ke `:core:domain`.
- **Circular**: Tidak ada, selama feature hanya depend ke domain dan tidak saling depend.
- **Manfaat**: batas context jelas di UI, tim bisa kerja per feature, build bisa lebih inkremental; domain/data tidak berubah.

Diagram: [Presentation per context](diagrams/android-03-presentation-per-context.mmd).
