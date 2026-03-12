# Diagram Arsitektur — IntiKasir F&B

Diagram menggunakan [Mermaid](https://mermaid.js.org/). Render via:
- VS Code + ekstensi Mermaid (preview)
- [Mermaid Live Editor](https://mermaid.live/) (export PNG/SVG)
- Embed di Markdown: `` ```mermaid `` block

---

## Daftar Diagram

### Product

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `product-01-sales-channels.mmd` | Sales channel architecture | [01-Product Overview](../01-product-overview.md) |

### Architecture

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `arch-01-clean-architecture.mmd` | Clean Architecture layers | [02-Architecture](../02-architecture-overview.md) |
| `arch-02-mvvm-flow.mmd` | MVVM sequence flow | [02-Architecture](../02-architecture-overview.md) |
| `arch-03-module-architecture.mmd` | Gradle module dependencies | [02-Architecture](../02-architecture-overview.md) |

### Domain Model

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `domain-01-classification.mmd` | Core vs Support domain classification | [03-Domain Model](../03-domain-model.md) |
| `domain-02-context-map.mmd` | Context map & relationships | [03-Domain Model](../03-domain-model.md) |
| `domain-03-sale-aggregate.mmd` | Sale aggregate class diagram | [03-Domain Model](../03-domain-model.md) |
| `domain-04-sale-state-machine.mmd` | Sale status state machine | [03-Domain Model](../03-domain-model.md) |
| `domain-05-identity-context.mmd` | Identity & Access class diagram | [03-Domain Model](../03-domain-model.md) |

### F&B Specialization

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `fnb-01-channel-architecture.mmd` | Channel type & SalesChannel entity | [04-F&B Domain](../04-fnb-domain-specialization.md) |
| `fnb-02-modifier-system.mmd` | Modifier group/option architecture | [04-F&B Domain](../04-fnb-domain-specialization.md) |
| `fnb-03-calculation-order.mmd` | Tax/SC/Tip calculation order | [04-F&B Domain](../04-fnb-domain-specialization.md) |
| `fnb-04-table-state.mmd` | Table state (available/occupied) | [04-F&B Domain](../04-fnb-domain-specialization.md) |
| `fnb-05-platform-settlement.mmd` | Platform settlement flow | [04-F&B Domain](../04-fnb-domain-specialization.md) |
| `fnb-06-recipe-cogs.mmd` | Recipe & COGS flow | [04-F&B Domain](../04-fnb-domain-specialization.md) |

### Data Architecture

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `data-01-er-diagram.mmd` | Entity-Relationship diagram (25 tables) | [05-Data Architecture](../05-data-architecture.md) |
| `data-02-sync-metadata-flow.mmd` | Sync status lifecycle | [05-Data Architecture](../05-data-architecture.md) |

### Sync & Cloud

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `sync-01-engine-architecture.mmd` | SyncEngine components | [06-Sync Architecture](../06-sync-and-cloud-architecture.md) |
| `sync-02-push-pull-sequence.mmd` | Push/pull sync sequence | [06-Sync Architecture](../06-sync-and-cloud-architecture.md) |
| `sync-03-conflict-resolution.mmd` | Conflict resolution strategy | [06-Sync Architecture](../06-sync-and-cloud-architecture.md) |
| `sync-04-multi-terminal-topology.mmd` | Multi-device topology | [06-Sync Architecture](../06-sync-and-cloud-architecture.md) |
| `sync-05-offline-online-transition.mmd` | Offline/online state machine | [06-Sync Architecture](../06-sync-and-cloud-architecture.md) |

### UI & Navigation

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `ui-01-navigation-graph.mmd` | Full navigation graph | [07-UI & Navigation](../07-ui-and-navigation.md) |
| `ui-02-responsive-layout.mmd` | Phone vs tablet layout | [07-UI & Navigation](../07-ui-and-navigation.md) |

### Module Structure

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `module-01-hierarchy.mmd` | Gradle module hierarchy | [08-Module Structure](../08-module-and-project-structure.md) |

### Testing

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `test-01-pyramid.mmd` | Testing pyramid | [10-Testing Strategy](../10-testing-strategy.md) |

### Security & Licensing

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `security-01-rbac.mmd` | Role-based access control | [11-Security](../11-security-and-licensing.md) |
| `security-02-license-activation.mmd` | AppReg license activation flow | [11-Security](../11-security-and-licensing.md) |

### Printing

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `print-01-architecture.mmd` | Printing architecture | [12-Printing](../12-printing-and-peripherals.md) |
| `print-02-bluetooth-flow.mmd` | Bluetooth printing sequence | [12-Printing](../12-printing-and-peripherals.md) |

### Deployment

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `deploy-01-build-flavors.mmd` | Build flavor configuration | [13-Deployment](../13-deployment-and-release.md) |

### Implementation

| File | Deskripsi | Dokumen |
|------|-----------|---------|
| `impl-01-roadmap.mmd` | 5-phase Gantt roadmap | [Implementasi Plan](../implementasi-plan.md) |

### Legacy (Original Diagrams)

| File | Deskripsi |
|------|-----------|
| `01-domain-classification.mmd` | Original domain classification |
| `02-context-map.mmd` | Original context map |
| `03-transaction-context.mmd` | Original transaction context |
| `04-catalog-context.mmd` | Original catalog context |
| `05-identity-access-context.mmd` | Original identity context |
| `06-integration-events.mmd` | Original integration events |
| `07-pos-lines-mapping.mmd` | Original PoS lines mapping |
| `fnb-01-fnb-context-overview.mmd` | Original F&B overview |
| `fnb-02-transaction-channels.mmd` | Original transaction channels |
| `fnb-03-recipe-cogs-flow.mmd` | Original recipe/COGS |
| `android-01-clean-architecture-layers.mmd` | Original clean architecture |
| `android-02-mvvm-flow.mmd` | Original MVVM flow |
| `android-03-presentation-per-context.mmd` | Original presentation per context |
| `sync-01-sync-engine-architecture.mmd` | Original sync engine |
| `sync-02-push-pull-sequence.mmd` | Original push/pull |
| `sync-03-conflict-resolution-flow.mmd` | Original conflict resolution |
| `sync-04-multi-terminal-topology.mmd` | Original topology |
| `sync-05-initial-sync-sequence.mmd` | Original initial sync |
| `sync-06-offline-to-online-transition.mmd` | Original offline/online |

---

**Total**: 30+ diagrams (new) + 19 legacy diagrams
