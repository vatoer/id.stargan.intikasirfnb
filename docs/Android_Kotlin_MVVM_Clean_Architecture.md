# Arsitektur Aplikasi — Android, Kotlin, MVVM, Clean Architecture

Dokumen ini mendefinisikan **stack teknis** dan **pemetaan lapisan** untuk pengembangan aplikasi PoS Android berdasarkan [DDD_Core_Support_Architecture.md](DDD_Core_Support_Architecture.md). Kombinasi: **Kotlin**, **MVVM**, **Clean Architecture**, dengan **domain layer** mengikuti DDD (entities, value objects, aggregates, domain events, repository interfaces).

---

## 1. Tech Stack

| Aspek | Pilihan | Keterangan |
|-------|---------|------------|
| **Platform** | Android | Target: phone/tablet PoS |
| **Bahasa** | Kotlin | 100% Kotlin; null-safety, sealed class, coroutines |
| **Arsitektur** | Clean Architecture | Domain → Data → Presentation; dependency rule (inner tidak depend ke outer) |
| **Pola UI** | MVVM | ViewModel + View (Activity/Fragment/Compose); state via StateFlow/LiveData |
| **Async** | Kotlin Coroutines + Flow | Use case & repository return `Flow`/`suspend`; ViewModel `viewModelScope.launch` |
| **DI** | Hilt (atau Koin) | Inject repository, use case, ViewModel |
| **Persistence** | Room (+ optional remote sync) | Local-first; offline-capable |
| **Navigation** | Jetpack Navigation Component | Single Activity + fragments atau Compose |

---

## 2. Clean Architecture — Lapisan dan Dependency Rule

Dependency mengalir **ke dalam**: Presentation → Domain ← Data. **Domain** tidak bergantung pada Android, database, atau framework apa pun.

```
┌─────────────────────────────────────────────────────────────────┐
│  PRESENTATION (Android)                                          │
│  UI (Activity/Fragment/Compose), ViewModel, UI State/Events       │
│  → memanggil Use Case (Application layer)                        │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│  APPLICATION (Use Cases)                                         │
│  UseCase classes: orchestrate repository + domain logic           │
│  → memanggil Repository (interface dari Domain)                  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│  DOMAIN (DDD)                                                    │
│  Entity, Value Object, Aggregate, Domain Event, Repository IF     │
│  → tidak depend ke lapisan lain                                  │
└─────────────────────────────────────────────────────────────────┘
                                ▲
┌───────────────────────────────┴─────────────────────────────────┐
│  DATA                                                             │
│  Repository impl, Data Source (Room, Remote), Mapper (DTO ↔ Model) │
│  → implement Repository interface dari Domain                      │
└─────────────────────────────────────────────────────────────────┘
```

Diagram: [Lapisan Clean Architecture](diagrams/android-01-clean-architecture-layers.mmd).

---

## 3. Pemetaan DDD ke Lapisan Clean Architecture

| DDD (dokumen arsitektur) | Lapisan Clean Architecture | Lokasi di project (contoh) |
|--------------------------|----------------------------|-----------------------------|
| Entity, Value Object, Aggregate, Invariants | **Domain** | `:domain` atau `core/domain` |
| Domain Event (definisi) | **Domain** | `:domain` |
| Repository **interface** | **Domain** | `:domain` — interface only |
| Use case / application service | **Application** | `:domain` (use case) atau `:app` / `:core:use-case` |
| Repository **implementasi** | **Data** | `:data` |
| Data source (Room DAO, API), DTO, Mapper | **Data** | `:data` |
| ViewModel, UI State, Events | **Presentation** | `:app` (feature modules) |
| Activity, Fragment, Compose | **Presentation** | `:app` |

**Penting**: Repository **interface** hidup di **domain**; implementasinya di **data** mengimplement interface tersebut. Use case (di domain atau modul application) memanggil repository via interface, sehingga domain tidak tahu tentang Room/Retrofit.

---

## 4. MVVM dalam Presentation Layer

- **View**: Activity, Fragment, atau Composable. Hanya menampilkan state dan mengirim event (user action) ke ViewModel.
- **ViewModel**: Memegang **UI State** (data class / sealed interface) dan **UI Event** (user intent). Memanggil **Use Case** (suspend atau Flow); mengexpose state ke View lewat **StateFlow** (atau LiveData).
- **State**: Satu data class per screen (e.g. `OrderUiState`: loading, list order, error). Menggunakan **sealed class** atau **sealed interface** jika ada beberapa bentuk state (Loading / Success / Error).
- **Event**: One-off (snackbar, navigation). Bisa `Channel` atau `SharedFlow` / `StateFlow`; atau single LiveData.

Alur: **User action** → ViewModel **event handler** → panggil **Use Case** → Use Case panggil **Repository** → **domain logic** (entity/aggregate) → Repository persist → Use Case return → ViewModel update **state** → View re-render.

Diagram: [MVVM + Use Case](diagrams/android-02-mvvm-flow.mmd).

---

## 5. Struktur Modul (Contoh)

Struktur berikut mengikuti Clean Architecture dan memudahkan pengembangan per bounded context (domain).

```
app/                          # Presentation: Activity, NavHost, ViewModel (bisa per feature)
core/
  domain/                     # Domain layer (shared)
    entity/
    valueobject/
    repository/               # Interface saja
    usecase/                  # Use case per fitur/context
    event/
  data/                       # Data layer (shared)
    local/                    # Room DAO, Database
    remote/                   # API client (jika ada)
    repository/               # Implementasi Repository
    mapper/                  # DTO <-> Domain model
feature/
  catalog/                    # Optional: feature module Catalog (domain + data + UI)
  transaction/
  ...
```

**Implementasi saat ini**: Satu modul **domain** dan satu modul **data** (bukan “domain dan data jadi satu modul”). Semua bounded context (identity, catalog, transaction, customer, settings, …) digabung **di dalam** masing-masing modul; pemisahan hanya lewat **paket** (e.g. `domain.identity`, `domain.transaction`). Alasan: kesederhanaan, referensi antar context (Transaction → Catalog, Customer) tanpa circular dependency modul, cocok untuk tim kecil/sedang. Opsi **pecah per domain** (satu modul per bounded context) dan kapan memakainya dijelaskan di **[Module_Structure_Options.md](Module_Structure_Options.md)**.

- `:core:domain` — entity, value object, aggregate, repository interface, use case, domain event (semua context dalam satu modul).
- `:core:data` — repository impl, Room, API, DTO, mapper (semua context dalam satu modul).
- `:app` — Activity/Fragment/Compose, ViewModel, navigation; depend ke `:core:domain` dan `:core:data` (via DI).

**Presentation pisah per context**: Anda bisa memecah **hanya** lapisan presentation menjadi feature module per bounded context (e.g. `:feature:catalog`, `:feature:transaction`, `:feature:identity`). Tiap feature module hanya depend ke `:core:domain`; ViewModel menerima use case via DI dari app. Domain dan data tetap satu modul. Detail dan diagram di **[Module_Structure_Options.md](Module_Structure_Options.md)** §6.

**Konvensi paket (Kotlin)** dalam `domain`:

- `pos.domain.catalog` — Product, Category, ProductRepository (interface).
- `pos.domain.transaction` — Sale, OrderLine, Payment, SaleRepository, CashierSessionRepository.
- `pos.domain.identity` — User, Tenant, Outlet, UserRepository.
- Dan seterusnya per bounded context.

---

## 6. Konvensi Kotlin untuk Domain Layer

- **Entity**: class (bisa `data class` jika immutable); ID sebagai value object (e.g. `SaleId`, `ProductId`).
- **Value object**: `data class` immutable; equals/hashCode by field (default di Kotlin); tanpa ID.
- **ID**: `value class` atau `data class` (e.g. `@JvmInline value class SaleId(val value: String)`).
- **State / status**: `enum class` atau `sealed class` (e.g. `SaleStatus { DRAFT, CONFIRMED, PAID, COMPLETED, VOIDED }`).
- **Domain event**: `sealed class` atau `data class` (e.g. `data class SaleCompleted(val saleId: SaleId, val outletId: OutletId, ...)`).
- **Repository interface**: penamaan tanpa prefix `I` (Kotlin idiom): `interface SaleRepository`, `interface ProductRepository`. Metode: `suspend fun getById(id: SaleId): Sale?`, `suspend fun save(sale: Sale)`, `fun streamAll(): Flow<List<Sale>>` jika perlu reactive.
- **Invariant**: enforce di dalam aggregate (method yang mengubah state); throw `IllegalStateException` atau domain exception jika dilanggar.

---

## 7. Use Case (Application Layer)

- Satu use case per aksi user / workflow (e.g. `CreateSaleUseCase`, `AddLineItemUseCase`, `CompleteSaleUseCase`).
- Use case **menerima** repository (interface) via constructor (DI); **tidak** menerima ViewModel atau Android API.
- Use case memanggil repository, memuat/mengubah aggregate, memvalidasi invariant; mengembalikan hasil (domain model atau DTO sederhana) atau throw.
- Signature: `suspend fun invoke(...): Result<...>` atau `fun invoke(...): Flow<...>`; parameter hanya domain types atau primitif.

Contoh (pseudocode):

```kotlin
// Di domain/transaction
class AddLineItemUseCase(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository  // ACL: baca product untuk snapshot
) {
    suspend operator fun invoke(saleId: SaleId, productId: ProductId, qty: Int): Result<Sale> {
        val sale = saleRepository.getById(saleId) ?: return Result.failure(...)
        val product = productRepository.getById(productId) ?: return Result.failure(...)
        sale.addLineItem(ProductSnapshot.from(product), qty)  // domain logic
        saleRepository.save(sale)
        return Result.success(sale)
    }
}
```

ViewModel memanggil use case dari `viewModelScope.launch` atau `flow { ... }.collect`.

---

## 8. Dependency Rule dan Android

- **Domain** module tidak boleh depend ke `android.*`, Room, Retrofit, atau `:data`. Hanya Kotlin stdlib (dan optional dependency seperti kotlinx.datetime jika dipakai).
- **Data** module depend ke **domain** (implement `SaleRepository`, dll.); boleh depend ke Android (Room, WorkManager untuk sync).
- **App** (presentation) depend ke **domain** (use case, model untuk UI) dan **data** (DI provide repository impl); ViewModel di-inject dengan use case, bukan repository langsung (boleh, tapi use case lebih khas).

---

## 9. Ringkasan Checklist Implementasi

| Lapisan | Isi | Referensi |
|---------|-----|-----------|
| **Domain** | Entity, VO, Aggregate, Repository interface, Domain event, Use case | [DDD_Core_Support_Architecture](DDD_Core_Support_Architecture.md), [Domain_Layer_Implementation_Guide](Domain_Layer_Implementation_Guide.md) |
| **Data** | Repository impl, Room DAO/Entity, DTO, Mapper (DTO ↔ Domain) | Interface dari domain |
| **Presentation** | ViewModel, UiState (sealed/ data class), Event, View | MVVM; state flow ke View |
| **DI** | Provide Repository impl, Use case, ViewModel | Hilt/Koin modules |

---

## 10. Diagram yang Tersedia

| Diagram | File | Keterangan |
|---------|------|------------|
| Lapisan Clean Architecture | [android-01-clean-architecture-layers.mmd](diagrams/android-01-clean-architecture-layers.mmd) | Domain, Data, Application, Presentation + dependency |
| Alur MVVM + Use Case | [android-02-mvvm-flow.mmd](diagrams/android-02-mvvm-flow.mmd) | View → ViewModel → Use Case → Repository → Domain |

---

Dokumen ini menjadi acuan teknis untuk implementasi PoS Android dengan Kotlin, MVVM, dan Clean Architecture, selaras dengan DDD di [DDD_Core_Support_Architecture.md](DDD_Core_Support_Architecture.md) dan [DDD_FnB_Detail.md](DDD_FnB_Detail.md).
