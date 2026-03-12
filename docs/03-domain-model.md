# 03 — Domain Model

> Bounded Contexts, Entities, Value Objects, dan Aggregates

---

## 3.1 Domain Classification

IntiKasir memiliki 12 bounded context yang dikategorikan menjadi Core dan Support domain.

```mermaid
graph TB
    subgraph "Core Domains — Differentiator"
        TX[Transaction<br/>Sale, Payment, Session]
        CT[Catalog<br/>MenuItem, Category, Modifier]
        WF[Workflow / Kitchen<br/>KitchenTicket, Queue]
    end

    subgraph "Support Domains"
        IA[Identity & Access<br/>Tenant, Outlet, User, Terminal]
        ST[Settings<br/>Tax, SC, Tip, Receipt, Printer]
        PP[Pricing & Promotion<br/>PriceList, Discount, Coupon]
        INV[Inventory<br/>Stock, Movement, Lot]
        CU[Customer<br/>Profile, Loyalty]
        SU[Supplier<br/>Vendor, PO, GR]
        AC[Accounting<br/>Journal, GL, AR/AP]
        RP[Reporting<br/>Dashboard, Export]
        LI[Licensing<br/>Activation, Verification]
    end

    IA -->|auth context| TX
    CT -->|product ref| TX
    ST -->|tax/SC config| TX
    PP -->|price resolution| TX
    TX -->|tickets| WF
    TX -->|stock deduction| INV
    TX -->|journal posting| AC
    TX -->|loyalty points| CU
    AC -->|reports| RP
```

> Diagram file: [`diagrams/domain-01-classification.mmd`](diagrams/domain-01-classification.mmd)

## 3.2 Context Map

```mermaid
graph LR
    subgraph "Upstream"
        IA2[Identity & Access]
        CT2[Catalog]
        ST2[Settings]
    end

    subgraph "Core"
        TX2[Transaction]
    end

    subgraph "Downstream"
        WF2[Workflow]
        INV2[Inventory]
        AC2[Accounting]
        RP2[Reporting]
    end

    IA2 -->|Shared Kernel| TX2
    CT2 -->|ACL: ProductRef| TX2
    ST2 -->|Conformist| TX2
    TX2 -->|Domain Event| WF2
    TX2 -->|Domain Event| INV2
    TX2 -->|Domain Event| AC2
    TX2 -->|Query| RP2
```

> Diagram file: [`diagrams/domain-02-context-map.mmd`](diagrams/domain-02-context-map.mmd)

### Relasi Antar Context

| Upstream | Downstream | Pattern | Deskripsi |
|----------|------------|---------|-----------|
| Identity | Transaction | Shared Kernel | TenantId, OutletId, UserId shared |
| Catalog | Transaction | ACL (ProductRef) | OrderLine menyimpan snapshot, bukan reference langsung |
| Settings | Transaction | Conformist | Tax/SC config di-apply saat confirm |
| Transaction | Workflow | Domain Event | `OrderConfirmed` → create KitchenTicket |
| Transaction | Inventory | Domain Event | `SaleCompleted` → deduct stock |
| Transaction | Accounting | Domain Event | `PaymentReceived` → post journal |

## 3.3 Transaction Context (Core)

### Aggregate: Sale

```mermaid
classDiagram
    class Sale {
        +SaleId id
        +TenantId tenantId
        +OutletId outletId
        +SalesChannelId channelId
        +TableId? tableId
        +UserId cashierId
        +SaleStatus status
        +List~OrderLine~ lines
        +List~Payment~ payments
        +List~TaxLine~ taxLines
        +ServiceChargeLine? serviceCharge
        +TipLine? tip
        +SyncMetadata syncMetadata
        +addLine()
        +updateLine()
        +removeLine()
        +confirm()
        +addPayment()
        +complete()
        +void()
        +subtotal() Money
        +totalAmount() Money
        +changeDue() Money
    }

    class OrderLine {
        +OrderLineId id
        +ProductId productId
        +String productName
        +Int quantity
        +Money unitPrice
        +Money effectiveUnitPrice
        +List~SelectedModifier~ modifiers
        +Money discountAmount
        +lineTotal() Money
    }

    class Payment {
        +PaymentId id
        +PaymentMethod method
        +Money amount
        +String? reference
    }

    class TaxLine {
        +TaxConfigId taxConfigId
        +String taxName
        +BigDecimal rate
        +Boolean isInclusive
        +Money taxableAmount
        +Money taxAmount
    }

    Sale "1" *-- "*" OrderLine
    Sale "1" *-- "*" Payment
    Sale "1" *-- "*" TaxLine
```

> Diagram file: [`diagrams/domain-03-sale-aggregate.mmd`](diagrams/domain-03-sale-aggregate.mmd)

### State Machine: Sale

```mermaid
stateDiagram-v2
    [*] --> DRAFT: createSale()
    DRAFT --> CONFIRMED: confirm()
    CONFIRMED --> PAID: addPayment() [fully paid]
    PAID --> COMPLETED: complete()
    DRAFT --> VOIDED: void()
    CONFIRMED --> VOIDED: void()
```

> Diagram file: [`diagrams/domain-04-sale-state-machine.mmd`](diagrams/domain-04-sale-state-machine.mmd)

### Value Objects

| Value Object | Fields | Deskripsi |
|-------------|--------|-----------|
| `Money` | `currency: String, amount: BigDecimal` | Aritmetika aman untuk uang |
| `SaleId` | `value: String` (ULID) | Identity Sale |
| `OrderLineId` | `value: String` (ULID) | Identity OrderLine |
| `PaymentId` | `value: String` (ULID) | Identity Payment |
| `SelectedModifier` | `name, priceDelta` | Snapshot modifier di order |
| `ProductRef` | `productId, name, price` | ACL snapshot dari Catalog |
| `PaymentBreakdown` | `entries: List<Entry>` | Summary pembayaran multi-method |

### Enums

| Enum | Values |
|------|--------|
| `SaleStatus` | `DRAFT, CONFIRMED, PAID, COMPLETED, VOIDED` |
| `PaymentMethod` | `CASH, CARD, E_WALLET, TRANSFER, OTHER` |

## 3.4 Catalog Context (Core)

| Entity/VO | Tipe | Fields Utama |
|-----------|------|-------------|
| `MenuItem` | Entity | id, tenantId, categoryId, name, basePrice, imageUri, isActive |
| `Category` | Entity | id, tenantId, name, parentId, sortOrder, isActive |
| `ModifierGroup` | Entity | id, tenantId, name, isActive |
| `ModifierOption` | Entity | id, groupId, name, priceDelta, isActive |
| `MenuItemModifierLink` | Junction | menuItemId, modifierGroupId, isRequired, min/maxSelection |
| `PriceList` | Entity | id, tenantId, name, channelId |
| `PriceListEntry` | Entity | id, priceListId, menuItemId, overridePrice |

## 3.5 Identity & Access Context (Support)

```mermaid
classDiagram
    class Tenant {
        +TenantId id
        +String name
        +Boolean isActive
    }

    class Outlet {
        +OutletId id
        +TenantId tenantId
        +String name
        +String address
        +Boolean isActive
    }

    class User {
        +UserId id
        +TenantId tenantId
        +String displayName
        +String pinHash
        +Set~Role~ roles
        +Set~OutletId~ outletIds
    }

    class Terminal {
        +TerminalId id
        +TenantId tenantId
        +OutletId outletId
        +TerminalType type
        +TerminalStatus status
        +Instant lastSyncAt
    }

    Tenant "1" *-- "*" Outlet
    Tenant "1" *-- "*" User
    Outlet "1" *-- "*" Terminal
```

> Diagram file: [`diagrams/domain-05-identity-context.mmd`](diagrams/domain-05-identity-context.mmd)

### Terminal Types

| Type | Fungsi | Fitur |
|------|--------|-------|
| `CASHIER` | Kasir utama | PoS, payment, receipt print |
| `WAITER` | Pelayan | Order taking, table assignment |
| `KITCHEN_DISPLAY` | Dapur | Kitchen ticket display |
| `MANAGER` | Manajer | Reports, settings, override |

## 3.6 Settings Context (Support)

| Entity/VO | Scope | Fields Utama |
|-----------|-------|-------------|
| `TenantSettings` | Per Tenant | currencyCode, numberingConfig, syncEnabled |
| `OutletSettings` | Per Outlet | timezone, serviceCharge, tip, receiptConfig |
| `TerminalSettings` | Per Terminal | printerConfig |
| `TaxConfig` | Per Tenant | name, rate, isInclusive, scope, isActive |
| `ReceiptConfig` | Per Outlet | header (logo, name, NPWP), body (toggles), footer |
| `PrinterConfig` | Per Terminal | connectionType, address, autoCut, autoPrint |

## 3.7 Workflow Context (Core)

| Entity | Fields Utama | Status |
|--------|-------------|--------|
| `KitchenTicket` | id, saleId, items, status, createdAt | Domain DONE, UI NOT_STARTED |
| `KitchenTicketItem` | menuItemName, quantity, modifiers, notes | Domain DONE |

### Kitchen Ticket Status Flow

```
PENDING → IN_PROGRESS → READY → SERVED
                      → CANCELLED
```

## 3.8 Other Contexts (Support)

| Context | Entities | Data Layer | UI | Overall |
|---------|----------|------------|-----|---------|
| **Customer** | Customer, Address | DONE | NOT_STARTED | PARTIAL |
| **Inventory** | StockLevel, StockMovement | NOT_STARTED | NOT_STARTED | PARTIAL |
| **Supplier** | Supplier, PurchaseOrder | NOT_STARTED | NOT_STARTED | PARTIAL |
| **Accounting** | Journal, JournalEntry, Account | NOT_STARTED | NOT_STARTED | PARTIAL |
| **Reporting** | — | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| **Licensing** | License, Activation | NOT_STARTED | NOT_STARTED | NOT_STARTED |

## 3.9 Shared Kernel

Value objects dan interfaces yang digunakan lintas bounded context:

| Item | Package | Deskripsi |
|------|---------|-----------|
| `Money` | `domain.shared` | Currency + BigDecimal |
| `SyncMetadata` | `domain.shared` | syncStatus, syncVersion, timestamps, terminalIds |
| `SyncStatus` | `domain.shared` | PENDING, SYNCED, CONFLICT |
| `Syncable` | `domain.shared` | Interface marker untuk entity syncable |
| `SyncEngine` | `domain.sync` | Interface: notifyChange, syncNow, observeStatus |
| ID Value Classes | `domain.*.XxxId` | Type-safe ULID wrappers |
| `UlidGenerator` | `domain.shared` | ULID generation utility |

---

*Dokumen terkait: [04-F&B Specialization](04-fnb-domain-specialization.md) · [05-Data Architecture](05-data-architecture.md) · [09-Use Case Reference](09-use-case-reference.md)*
