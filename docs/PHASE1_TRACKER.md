# Phase 1 — Restore CRUD after v4 schema (detailed tracker)

**Created:** 2026-04-02  
**Related plan:** `rebuild_plan.md` Phase 1 (this file does **not** modify that document).  
**Goal:** Every previously implemented flow still works after Room v4 + destructive migration; fix repository/UI gaps that would corrupt new columns.

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Code verified and/or fixed; automated build green |
| `[~]` | Partially done — see notes |
| `[ ]` | Requires **manual device/emulator smoke** (not automated here) |

---

## P1-1 — Auth (AUTH-US-01 / 02 / 03)

**Scope:** Login, logout, session with new DB (seed, init).

| Step | Status | Notes |
|------|--------|--------|
| Code review | `[x]` | No entity changes; `FarmDatabase` + `SessionManager` unchanged by Phase 1 work |
| `./gradlew assembleDebug` | `[x]` | BUILD SUCCESSFUL (2026-04-02) |
| `./gradlew testDebugUnitTest` | `[x]` | BUILD SUCCESSFUL |
| Manual: cold start → login → logout → re-login | `[ ]` | Use `./scripts/dev.sh install` or Studio run |

**Files touched this phase:** none (auth).

---

## P1-2 — Products CRUD (PRD-US-01 / 03 / 04 / 05)

**Scope:** New nullable columns `category`, `default_piece_count` must not be wiped on update.

| Step | Status | Notes |
|------|--------|--------|
| Domain + repository map v4 columns | `[x]` | `Product` gains `category`, `defaultPieceCount`; `ProductRepository` insert/update/`toProduct()`/`getFilteredProducts()` aligned |
| Manual: add/edit product, relaunch app | `[ ]` | Confirm data persists |

**Files changed**

- `app/src/main/java/com/redn/farm/data/model/Product.kt`
- `app/src/main/java/com/redn/farm/data/repository/ProductRepository.kt`

---

## P1-3 — Customers CRUD (CUS-US-01–04)

| Step | Status | Notes |
|------|--------|--------|
| Schema / code | `[x]` | No v4 customer changes; compile green |
| Manual smoke | `[ ]` | Create/edit customer |

**Files touched:** none.

---

## P1-4 — Employees + Payments (EMP-US-01–05)

**Scope:** Net pay display per EMP-US-04 AC#5: `amount − cash_advance_amount + liquidated_amount`.

| Step | Status | Notes |
|------|--------|--------|
| Net pay on payment card | `[x]` | `PaymentCard.kt` shows **Net pay** line using formula above (null advances/liquidated treated as 0) |
| Manual smoke | `[ ]` | Create payment with cash advance + liquidated; verify UI |

**Files changed**

- `app/src/main/java/com/redn/farm/ui/screens/manage/employees/payment/PaymentCard.kt`

---

## P1-5 — Farm Operations (FOP-US-01–04)

| Step | Status | Notes |
|------|--------|--------|
| | `[x]` | No schema changes; compile green |
| Manual smoke | `[ ]` | Add/edit operation |

**Files touched:** none.

---

## P1-6 — Remittances (REM-US-01–03)

| Step | Status | Notes |
|------|--------|--------|
| | `[x]` | No schema changes; compile green |
| Manual smoke | `[ ]` | Add/edit remittance |

**Files touched:** none.

---

## P1-7 — Orders + order items (ORD-US-03 / 04 / 05 / 06 / 07 / 09)

**Scope:** `channel` defaults to `"offline"`; all save paths must persist it.

| Step | Status | Notes |
|------|--------|--------|
| `Order` model default | `[x]` | `channel: String = "offline"` |
| `TakeOrderViewModel.placeOrder` | `[x]` | Builds `Order(...)` without overriding channel → **offline** |
| `OrderRepository` create/update | `[x]` | Passes `order.channel` into `OrderEntity` |
| Export/migration snapshot | `[x]` | `DatabaseMigrationViewModel` order mapping includes `channel` |
| Manual smoke | `[ ]` | Place order, edit order, mark paid/delivered |

**Files changed**

- `app/src/main/java/com/redn/farm/ui/screens/database/DatabaseMigrationViewModel.kt` (orders slice)

---

## P1-8 — Acquisitions (INV-US-01–04)

**Scope:** `created_at` set on insert, **unchanged** on update; full row mapping so SRP/snapshot columns (when added in Phase 3) are not nulled by edits.

| Step | Status | Notes |
|------|--------|--------|
| Domain model | `[x]` | `Acquisition` includes `created_at`, `piece_count`, preset snapshot + all SRP fields (nullable) |
| Repository | `[x]` | Full `AcquisitionEntity` ↔ `Acquisition` mapping; insert uses `System.currentTimeMillis()` for `created_at`; update preserves domain `created_at` |
| Edit dialog | `[x]` | `AcquireProduceScreen` uses `acquisitionToEdit.copy(...)` so pricing fields survive edits |
| Migration VM | `[x]` | Full acquisition field mapping for export consistency |
| Manual smoke | `[ ]` | Add acquisition, edit acquisition, verify sort/tiebreak by `created_at` after Phase 3 queries |

**Files changed**

- `app/src/main/java/com/redn/farm/data/model/Acquisition.kt`
- `app/src/main/java/com/redn/farm/data/repository/AcquisitionRepository.kt`
- `app/src/main/java/com/redn/farm/ui/screens/acquire/AcquireProduceScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/database/DatabaseMigrationViewModel.kt` (acquisitions + product prices slice)

**Additional fix (migration data integrity)**

- `DatabaseMigrationViewModel` **productPrices** now maps `discounted_per_kg_price` / `discounted_per_piece_price` for round-trip exports.

---

## Automated verification log

| Command | Result | When |
|---------|--------|------|
| `./gradlew assembleDebug` | SUCCESS | 2026-04-02 |
| `./gradlew testDebugUnitTest` | SUCCESS | 2026-04-02 |

---

## Manual smoke checklist (one pass on device)

Use fresh install or existing v4 DB after destructive migration.

1. Login / logout  
2. Product: create, edit, restart app, confirm row  
3. Customer: create, edit  
4. Employee payment: amount + cash advance + liquidated → **Net pay** looks correct  
5. Farm operation: add  
6. Remittance: add  
7. Order: take order → history → edit → paid/delivered as applicable  
8. Acquisition: add → edit → confirm list order stable  

---

## Open items / follow-ups

- **Instrumented tests** for Phase 1 flows are optional; consider Room `@Test` for `AcquisitionRepository` insert vs update `created_at` if regressions recur.  
- **rebuild_plan.md** remains the high-level checklist; update task checkboxes there separately when you want the plan file to reflect completion.

---

*End of Phase 1 tracker.*
