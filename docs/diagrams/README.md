# Diagram Arsitektur DDD PoS

Diagram dalam folder ini menggunakan [Mermaid](https://mermaid.js.org/). Anda dapat:

- Membuka file `.mmd` di VS Code dengan ekstensi **Mermaid** dan preview
- Menggunakan [Mermaid Live Editor](https://mermaid.live/) untuk render dan export PNG/SVG
- Meng-embed di Markdown dengan blok ` ```mermaid ` (salin isi `.mmd` ke dalam blok)

## Daftar Diagram

| File | Deskripsi |
|------|-----------|
| `01-domain-classification.mmd` | Klasifikasi Core vs Supporting domain |
| `02-context-map.mmd` | Context map semua Bounded Context dan hubungan |
| `03-transaction-context.mmd` | Aggregate & event di Transaction context |
| `04-catalog-context.mmd` | Aggregate Catalog (Product, Category) |
| `05-identity-access-context.mmd` | Tenant, Outlet, User, Role |
| `06-integration-events.mmd` | Alur integrasi event antar context |
| `07-pos-lines-mapping.mmd` | Pemetaan domain per line (Retail, F&B, Laundry, Service=Salon/Barber/Spa, Bengkel) |
| **F&B (branch)** | |
| `fnb-01-fnb-context-overview.mmd` | F&B dalam base: Order+Channel, MenuItem+Recipe, Kitchen, Inventory |
| `fnb-02-transaction-channels.mmd` | Channel transaksi: Dine In, Take Away, Ojol A, Ojol B |
| `fnb-03-recipe-cogs-flow.mmd` | Resep opsional & alur COGS (Inventory + Accounting) |
| **Android / Clean Architecture** | |
| `android-01-clean-architecture-layers.mmd` | Lapisan Clean Architecture (Domain, Data, Application, Presentation) |
| `android-02-mvvm-flow.mmd` | Alur MVVM: View → ViewModel → Use Case → Repository |
| `android-03-presentation-per-context.mmd` | Presentation pisah per context (feature modules), app shell + core |
