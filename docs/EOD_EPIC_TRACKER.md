# Epic 12 — End of Day Operations (EOD-US-*) Implementation Tracker

**Canon:** [`USER_STORIES.md`](./USER_STORIES.md) Epic 12 (EOD-US-01 onward, including EOD-US-11 to EOD-US-15 follow-ups)  
**Maintenance:** For cross-doc status sync, use the **Canonical status table** in [`PARTIAL_IMPLEMENTATION_PLAN.md`](./PARTIAL_IMPLEMENTATION_PLAN.md).  
**Readiness report:** See `docs/bugs.md` and inline notes below  
**Created:** 2026-04-07 · **Snapshot updated:** 2026-04-09 (Stream E + closure verification pass)  
**DB version (current app):** `FarmDatabase` **v10** (day close entities landed in v9+; verify in `FarmDatabase.kt`).

---

## Implementation snapshot (2026-04-09)

Substantial code exists under `app/src/main/java/com/redn/farm/ui/screens/eod/` and `data/repository/DayCloseRepository.kt`. Stream E and its follow-up slice are shipped; **USER_STORIES.md** is the source of truth for final acceptance status. The phase checklists below are retained as historical implementation records.

**Cross-reference:** Stream E follow-up work is now formalized in [`USER_STORIES.md`](./USER_STORIES.md) as **EOD-US-11** to **EOD-US-15**.

### Stream E follow-up execution slice (completed)

This order was executed and mirrored in [`PARTIAL_IMPLEMENTATION_PLAN.md`](./PARTIAL_IMPLEMENTATION_PLAN.md):

1. **EOD-US-12** — add explicit daily paid/unpaid/delivered rows in Day Close sales summary.
2. **EOD-US-13** — complete last-acquisition snippet (date + qty + unit cost) in inventory row.
3. **EOD-US-15** — finalize EOD thermal footer metadata (`closed_by` / `closed_at`; draft `printed_by` / `printed_at`).
4. **EOD-US-14** — enrich Day Close History row metadata (margin, closed by, closed at).
5. **EOD-US-11** — implement explicit Review → Confirm finalize flow.

**Execution note:** This section is historical (completed); the phase checklists below are retained as implementation history/reference.

| Story | Primary files | Validation steps | Status |
|-------|---------------|------------------|--------|
| **EOD-US-12** | `data/local/dao/OrderDao.kt`, `data/repository/DayCloseRepository.kt`, `ui/screens/eod/DayCloseScreen.kt`, `ui/screens/eod/DayCloseViewModel.kt` | `./gradlew assembleDebug` ✅; verify paid/unpaid/delivered rows on Day Close match Order History for same business date | `[x]` |
| **EOD-US-13** | `data/local/dao/AcquisitionDao.kt`, `data/repository/DayCloseRepository.kt`, `ui/screens/eod/DayCloseScreen.kt` | `./gradlew assembleDebug` ✅; verify last-acquisition snippet includes date + qty + unit cost and fallback for missing data | `[x]` |
| **EOD-US-15** | `utils/ThermalPrintBuilders.kt`, `ui/screens/eod/DayCloseViewModel.kt`, `ui/screens/eod/DayCloseScreen.kt` | `./gradlew assembleDebug` ✅; print draft + finalized EOD slips and verify footer metadata fields | `[x]` |
| **EOD-US-14** | `ui/screens/eod/DayCloseHistoryScreen.kt`, `ui/screens/eod/DayCloseHistoryViewModel.kt`, `data/repository/DayCloseRepository.kt` | `./gradlew assembleDebug` ✅; verify history rows show margin/closed by/closed at and date filters still work | `[x]` |
| **EOD-US-11** | `ui/screens/eod/DayCloseScreen.kt`, `ui/screens/eod/DayCloseViewModel.kt`, `data/repository/DayCloseRepository.kt` | `./gradlew assembleDebug` ✅; verify explicit Review → Confirm flow and existing finalize guards remain intact | `[x]` |

### Closure slice for remaining partials (completed)

Manual verification is complete for the former partials in canon: **EOD-US-04**, **EOD-US-09**, **EOD-US-10**.

| Story | Primary files | Validation steps | Status |
|-------|---------------|------------------|--------|
| **EOD-US-04** | `ui/screens/eod/DayCloseViewModel.kt`, `ui/screens/eod/DayCloseScreen.kt`, `app/src/test/java/com/redn/farm/ui/screens/eod/DayCloseViewModelCashGuardTest.kt` | `./gradlew :app:testDebugUnitTest --tests "*DayCloseViewModel*"` ✅; manual finalize-guard discrepancy/remarks scenarios ✅ | `[x]` |
| **EOD-US-10** | `data/repository/DayCloseRepository.kt`, `ui/screens/eod/OutstandingInventoryScreen.kt`, `ui/screens/eod/OutstandingInventoryViewModel.kt`, `utils/ThermalPrintBuilders.kt` | `./gradlew assembleDebug` ✅; manual AC3 ledger fields + filter/sort/print checks ✅ | `[x]` |
| **EOD-US-09** | `ui/screens/eod/DayCloseScreen.kt`, `utils/ThermalPrintBuilders.kt` | `./gradlew assembleDebug` ✅; manual employee summary labels/notes UX/thermal wages line checks ✅ | `[x]` |

| Area | In codebase (high level) | Typical gaps vs USER_STORIES |
|------|-------------------------|------------------------------|
| Schema / DAOs / repository | `DayCloseEntity`, inventory, audit, `DayCloseRepository`, FIFO helper, date windowing | Fine-tune aggregates, finalize persistence for all fields |
| Navigation / RBAC | `DayClose`, `DayCloseHistory`, `OutstandingInventory` routes; dashboard tiles; `Rbac` day-close roles | Optional dedicated read-only **day close detail** route |
| **DayCloseScreen** | Warnings, channel + top products, cumulative + outflows, full cash card, inventory (prior variance, toggle, persist, finalize bulk write), outstanding orders, employees, notes, print, admin un-finalize, explicit Review → Confirm finalize | Remaining gaps tracked outside this tracker (if any) |
| **DayCloseHistoryScreen** | List + All/30d/90d filter + open by date → **DayCloseScreen**; richer row metadata (sales/orders/margin/closed by/at) | Remaining gaps tracked outside this tracker (if any) |
| **OutstandingInventoryScreen** | Search, category, at-risk, total value, print, FIFO + **AC11** actual override, per-row ledger columns | Device QA on print formatting |
| Thermal EOD / outstanding print | **`buildEodSummary` (PRN-09)**, **`buildOutstandingInventoryReport` (PRN-10)** | Device QA on Sunmi |

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Implemented and verified |
| `[~]` | Partially done — see notes |
| `[ ]` | Not started |
| `[!]` | Blocked or needs decision |

---

## Key design decisions (resolve before coding)

| # | Decision | Resolution |
|---|----------|------------|
| D1 | **WAC unit basis**: products acquired both per-kg and per-piece → incoherent WAC. | v1 rule: always compute WAC in kg. For per-piece acquisitions, convert: `effective_kg = quantity / piece_count`. Document in `DayCloseRepository` comments. |
| D2 | **STORE_ASSISTANT inventory access**: EOD-US-01 allows STORE_ASSISTANT to open Day Close; EOD-US-03 says only Admin + Purchasing edit counts. | STORE_ASSISTANT sees inventory section read-only. `DayCloseViewModel` exposes `canEditInventoryCounts: Boolean` = `role in {ADMIN, PURCHASING}`. |
| D3 | **Surplus variance (negative spoilage)**: actual remaining > theoretical. | Non-blocking — same pattern as negative margin (warning chip on row, allow finalize). Log in `notes` field. |
| D4 | **Finalize with negative COGS margin**: EOD-US-07 AC4 requires a confirmation dialog. | Pre-finalize check in `DayCloseViewModel.requestFinalize()`: if `grossMarginAmount < 0` emit `ConfirmNegativeMargin` event; UI shows dialog. User may proceed. |
| D5 | **Date windowing**: all EOD queries scope to a "business day" window. | Use `DateWindowHelper.startOfDay(millis)` / `endOfDay(millis)` (create if it doesn't exist). All queries use `BETWEEN startOfDay AND endOfDay`. |
| D6 | **Outstanding Inventory integration in Day Close**: EOD-US-10 says it appears as a section inside the Day Close screen. | Add as a read-only collapsible `Card` section between Inventory Close and Cash Reconciliation. Navigates to `OutstandingInventoryScreen` via "View full report" button. |
| D7 | **FIFO vs WAC for per-lot drill-down (EOD-US-10)**: lot remaining uses FIFO; WAC is for EOD-US-03/07 pricing. | FIFO only in `InventoryFifoAllocator`; WAC computed separately in `DayCloseRepository`. Both can coexist — different purposes. |

---

## Phase 1 — Entities and schema (DB v9)

**Target files (all new):**
- `data/local/entity/DayCloseEntity.kt`
- `data/local/entity/DayCloseInventoryEntity.kt`
- `data/local/entity/DayCloseAuditEntity.kt`
- `data/local/FarmDatabase.kt` — add entities, bump version

| Task | Status | Notes |
|------|--------|-------|
| Create `DayCloseEntity` with all fields from USER_STORIES.md Epic 12 entity spec | `[ ]` | Fields: `close_id`, `business_date` UNIQUE, `closed_by`, `closed_at?`, `is_finalized`, `total_orders`, `total_sales_amount`, `total_collected`, `snapshot_all_unpaid_count?`, `snapshot_all_unpaid_amount?`, `notes?`, `cash_on_hand?`, `cash_reconciliation_remarks?`, `gross_revenue_today?`, `collected_revenue_today?`, `total_cogs_today?`, `gross_margin_amount?`, `gross_margin_percent?` |
| Create `DayCloseInventoryEntity` | `[ ]` | Fields: `id`, `close_id` FK→day_closes CASCADE, `product_id`, `product_name`, `total_acquired_all_time`, `total_sold_through_close_date`, `prior_posted_variance`, `adjusted_theoretical_remaining`, `sold_this_close_date`, `actual_remaining?`, `variance_qty?`, `weighted_avg_cost_per_unit`, `variance_cost?`, `is_counted` Boolean default true |
| Create `DayCloseAuditEntity` | `[ ]` | Fields: `audit_id`, `close_id` FK→day_closes, `action` TEXT, `username`, `at_millis`, `note?` |
| Add `@Index` on `day_closes.business_date` (UNIQUE) | `[ ]` | Also index `(is_finalized, business_date DESC)` for history query |
| Add `@Index` on `day_close_inventory.(close_id, product_id)` | `[ ]` | |
| Add all three entities to `@Database` annotation in `FarmDatabase.kt` | `[ ]` | |
| Bump `FarmDatabase.version` from 8 to 9 | `[ ]` | `fallbackToDestructiveMigration()` already enabled — no migration code needed |
| Record v9 schema in `docs/schema_evolution.sql` (new VERSION 9 section) | `[ ]` | Copy CREATE TABLE DDL from generated `FarmDatabase_Impl.java` after build |
| `./gradlew assembleDebug` passes after entity additions | `[ ]` | |

---

## Phase 2 — DAOs and repository

**Target files (all new):**
- `data/local/dao/DayCloseDao.kt`
- `data/local/dao/DayCloseInventoryDao.kt`
- `data/local/dao/DayCloseAuditDao.kt`
- `data/repository/DayCloseRepository.kt`
- `data/util/InventoryFifoAllocator.kt`
- `data/util/DateWindowHelper.kt` (or verify existing)
- `data/model/DayClose.kt`, `DayCloseInventoryLine.kt`, snapshot types

### Phase 2a — DAOs

| Task | Status | Notes |
|------|--------|-------|
| `DayCloseDao`: `insert`, `update`, `getByDate(businessDate)`, `getAllDesc(): Flow<List<DayCloseEntity>>`, `getById(closeId)` | `[ ]` | |
| `DayCloseInventoryDao`: `insertAll(rows)`, `getByCloseId(closeId)`, `deleteByCloseId(closeId)` | `[ ]` | Delete used when un-finalizing and re-entering counts |
| `DayCloseAuditDao`: `insert`, `getByCloseId(closeId)` | `[ ]` | |
| Register all three DAOs in `DatabaseModule` or `FarmDatabase` | `[ ]` | |
| `./gradlew assembleDebug` passes | `[ ]` | |

### Phase 2b — Aggregation queries (extend existing DAOs)

These methods are added to existing DAOs but consumed only by `DayCloseRepository`.

| Task | Status | Notes |
|------|--------|-------|
| `OrderDao.getSalesOnDate(start, end)`: total count, total amount, paid count/amount, unpaid count/amount | `[ ]` | |
| `OrderDao.getSalesByChannel(start, end)`: group by channel → count + amount per channel | `[ ]` | |
| `OrderDao.getTopProductsByRevenue(start, end, limit)`: join order_items → sum total_price group by product_id | `[ ]` | |
| `OrderDao.getAllUnpaid()`: all orders `is_paid = false`, sorted `order_date ASC` | `[ ]` | |
| `OrderItemDao` (or `OrderDao`): `getQtySoldByProduct(start, end)`: sum `quantity` group by `product_id` | `[ ]` | Used for COGS |
| `AcquisitionDao.getTotalAcquiredByProduct()`: `SUM(quantity)` and `SUM(total_amount)` group by `product_id` (all time) | `[ ]` | Used for WAC and theoretical stock |
| `AcquisitionDao.getTotalSoldByProductUpTo(endMillis)`: delegate to order_items — or compute in repository via join | `[ ]` | |
| `AcquisitionDao.getLastAcquisitionByProduct()`: `MAX(date_acquired)` per product | `[ ]` | Used for "last acquisition" display in EOD-US-03 |
| `RemittanceDao.getSumRemittancesOnDate` / `getSumDisbursementsOnDate` (Epic 8) | `[x]` | EOD UI can wire when ready |
| `EmployeePaymentDao.getPaymentsOnDate(start, end)`: list payments where `date_paid BETWEEN start AND end` | `[ ]` | |
| Verify `RemittanceEntity.date` is epoch millis (not seconds) | `[ ]` | If seconds, add conversion in query or fix entity |

### Phase 2c — `DayCloseRepository`

| Task | Status | Notes |
|------|--------|-------|
| `getSalesSummary(businessDate)` → `SalesSummary` domain object | `[ ]` | Calls sales DAO methods; packages into snapshot |
| `getCogs(businessDate)` → `CogsSummary`: per-product WAC×qty_sold, total COGS, revenue, margin | `[ ]` | WAC = total_amount_all_time / total_qty_all_time per product; apply D1 unit rule |
| `getInventoryPosition(businessDate)` → `List<InventoryLine>` | `[ ]` | total_acquired − total_sold − prior_posted_variance per product; query `day_close_inventory` for prior variance sum |
| `getCumulativePosition()` → total investment, total collected, outstanding inventory value | `[ ]` | Used by EOD-US-07 cumulative subsection |
| `getCashPosition(businessDate)` → `CashSummary` | `[ ]` | Expected cash (paid offline+reseller), remitted, difference |
| `getOutstandingOrders()` → `List<OrderWithDetails>` | `[ ]` | All `is_paid = false`, sorted `order_date ASC` |
| `getEmployeePaymentsOnDate(businessDate)` → `List<EmployeePaymentWithEmployee>` | `[ ]` | |
| `getDayCloseForDate(businessDate)` → `DayClose?` | `[ ]` | |
| `upsertDayClose(draft)` → save draft (non-finalized) | `[ ]` | |
| `finalizeDayClose(closeId, inventoryLines, snapshots)` | `[ ]` | `@Transaction`: set `is_finalized = true`, write `closed_at`/`closed_by`, bulk-insert `DayCloseInventoryEntity` rows, snapshot margin fields |
| `unfinalizeClose(closeId, adminUser, note)` | `[ ]` | `@Transaction`: set `is_finalized = false`, clear `closed_at`, insert `DayCloseAuditEntity` row action=`"UNFINALIZE"` |
| `getAllCloses()` → `Flow<List<DayClose>>` | `[ ]` | |

### Phase 2d — Utilities

| Task | Status | Notes |
|------|--------|-------|
| `DateWindowHelper.startOfDay(millis): Long` | `[ ]` | Truncate to 00:00:00 local time; use `java.time.LocalDate` + `ZoneId.systemDefault()` |
| `DateWindowHelper.endOfDay(millis): Long` | `[ ]` | 23:59:59.999 local time |
| `InventoryFifoAllocator.allocate(lots, totalSold)` | `[ ]` | Input: sorted `List<AcquisitionLot>` (date, id, qty, cost); output: per-lot remaining, oldest unsold date, days on hand |
| Unit tests for `InventoryFifoAllocator` | `[ ]` | Cases: single lot fully unsold, single lot partially sold, multiple lots, fully sold, oversold (negative remaining) |
| Unit tests for `DateWindowHelper` | `[ ]` | Start/end of day, DST boundary |
| Unit tests for WAC calculation logic | `[ ]` | Mixed kg/piece (D1 rule), zero acquisitions, single acquisition |
| `./gradlew testDebugUnitTest` passes | `[ ]` | |

---

## Phase 3 — RBAC and navigation

**Target files:**
- `security/Rbac.kt` — additions only
- `navigation/NavGraph.kt` — additions only
- `ui/screens/main/MainScreen.kt` — tile additions

| Task | Status | Notes |
|------|--------|-------|
| Add `ROLES_DAY_CLOSE = setOf(ADMIN, STORE_ASSISTANT)` to `Rbac.kt` | `[ ]` | |
| Add `ROLES_DAY_CLOSE_HISTORY = setOf(ADMIN)` | `[ ]` | |
| Add `ROLES_OUTSTANDING_INVENTORY = setOf(ADMIN, PURCHASING)` | `[ ]` | |
| Add `fun canAccessDayClose(role)`, `canAccessDayCloseHistory(role)`, `canAccessOutstandingInventory(role)` | `[ ]` | |
| Add `fun canEditInventoryCounts(role) = normalizeRole(role) in setOf(ADMIN, PURCHASING)` | `[ ]` | |
| Add `Screen.DayClose("day_close")` to sealed class in `NavGraph.kt` | `[ ]` | No route params needed for today's close; optional `businessDate` param for admin re-open |
| Add `Screen.DayCloseHistory("day_close_history")` | `[ ]` | |
| Add `Screen.DayCloseDetail("day_close_detail/{closeId}")` with `createRoute(closeId)` | `[ ]` | |
| Add `Screen.OutstandingInventory("outstanding_inventory")` | `[ ]` | |
| Add composable stubs for all four routes in `NavHost` | `[ ]` | Stubs show placeholder `Text("Coming soon")` initially |
| Add "Close Day" dashboard tile to `MainScreen` — visible to STORE_ASSISTANT + ADMIN | `[ ]` | |
| Add "Day Close History" dashboard tile — visible to ADMIN only | `[ ]` | |
| Add "Outstanding Inventory" dashboard tile — visible to ADMIN + PURCHASING | `[ ]` | |
| `./gradlew assembleDebug` passes | `[ ]` | |

---

## Phase 4 — Screens and ViewModels

### P4-1 — `DayCloseViewModel` + `DayCloseScreen`

`DayCloseScreen` is a multi-section scrollable screen. Each section is a collapsible `Card`. A sticky bottom bar holds the Confirm/Print buttons.

**Files:** `ui/screens/dayclose/DayCloseScreen.kt`, `ui/screens/dayclose/DayCloseViewModel.kt`

| Task | Status | Notes |
|------|--------|-------|
| ViewModel: load or create draft `DayClose` for today's business date on `init` | `[ ]` | If finalized close exists for today, load read-only; if draft exists, load editable; else create new |
| ViewModel: expose `uiState: StateFlow<DayCloseUiState>` with all section data | `[ ]` | `DayCloseUiState` holds: sales summary, inventory lines, cash position, cogs summary, outstanding orders, employee payments, `canEditInventoryCounts`, `isFinalized` |
| ViewModel: `updateActualRemaining(productId, qty)` — update inventory line, recompute variance | `[ ]` | Only allowed when `canEditInventoryCounts = true` |
| ViewModel: `updateCashOnHand(amount)`, `updateReconciliationRemarks(text)` | `[ ]` | |
| ViewModel: `requestFinalize()` — validates: cash discrepancy without remarks → emit error; negative margin → emit `ConfirmNegativeMargin` event | `[ ]` | D4 |
| ViewModel: `confirmFinalize()` — calls `DayCloseRepository.finalizeDayClose(...)` | `[ ]` | |
| Screen: Sales Summary section — orders count, amounts, channel breakdown, top 5 products | `[ ]` | EOD-US-02 |
| Screen: Inventory Close section — product list with `adjusted_theoretical_remaining`, `actual_remaining` editable field, variance chip | `[ ]` | EOD-US-03; editable only if `canEditInventoryCounts`; sold-today shown as subtitle |
| Screen: Outstanding Inventory snapshot — read-only summary rows; "View full report" button → `Screen.OutstandingInventory` | `[ ]` | EOD-US-10 / D6 |
| Screen: Cash Reconciliation section — expected cash, remitted today, difference; `cash_on_hand` field; remarks field; digital collections line | `[ ]` | EOD-US-04 |
| Screen: Revenue vs COGS section — today's revenue, COGS, margin; cumulative position subsection; other outflows | `[ ]` | EOD-US-07 |
| Screen: Outstanding Orders section — list all unpaid (capped at 10 rows; "and N more" if over); tap row → `Screen.OrderDetail` | `[ ]` | EOD-US-08 |
| Screen: Employee Day Summary section — today's payments list; total wages line; notes field | `[ ]` | EOD-US-09 |
| Screen: Warnings at top — unpaid orders today badge, acquisitions with no sales badge | `[ ]` | EOD-US-01 AC4 |
| Screen: Sticky bottom bar — "Print Draft" + "Confirm & Finalize" buttons; disabled when `isFinalized`; "Print Final" + "Un-finalize" (admin only) when finalized | `[ ]` | EOD-US-01 AC5, EOD-US-05 |
| Negative margin confirmation dialog (`AlertDialog`) | `[ ]` | EOD-US-07 AC4 / D4 |

### P4-2 — `DayCloseHistoryScreen` + `DayCloseDetailScreen`

**Files:** `ui/screens/dayclose/DayCloseHistoryScreen.kt`, `ui/screens/dayclose/DayCloseHistoryViewModel.kt`, `ui/screens/dayclose/DayCloseDetailScreen.kt`

| Task | Status | Notes |
|------|--------|-------|
| History list: one row per close — date, total sales, order count, margin %, closed by | `[ ]` | EOD-US-06 AC2 |
| History list: date range filter (from/to date pickers — follow `AcquisitionFormScreen` pattern) | `[ ]` | EOD-US-06 AC5 |
| History list: RBAC gate — admin only; show "Access restricted" if non-admin reaches route | `[ ]` | EOD-US-06 / Phase 3 |
| Detail screen: read-only view of all sections (same sections as `DayCloseScreen` but non-editable) | `[ ]` | EOD-US-06 AC3 |
| Detail screen: "Re-print" button → triggers EOD thermal print | `[ ]` | EOD-US-06 AC4 |
| Detail screen: "Un-finalize" button (admin only) → `DayCloseRepository.unfinalizeClose()` → navigate back to `DayCloseScreen` for editing | `[ ]` | EOD-US-06 AC6 |

### P4-3 — `OutstandingInventoryScreen`

**Files:** `ui/screens/inventory/OutstandingInventoryScreen.kt`, `ui/screens/inventory/OutstandingInventoryViewModel.kt`

| Task | Status | Notes |
|------|--------|-------|
| ViewModel: load `List<OutstandingInventoryLine>` on init — computed from `DayCloseRepository.getInventoryPosition()` + `InventoryFifoAllocator` | `[ ]` | EOD-US-10 AC3 |
| ViewModel: expose total outstanding value, sorted by days-on-hand DESC | `[ ]` | EOD-US-10 AC4/5 |
| Screen: header showing total outstanding value | `[ ]` | EOD-US-10 AC5 |
| Screen: product rows — name, qty on hand, days on hand (oldest lot), outstanding value; aging color (amber ≥3 days, red ≥7 days) | `[ ]` | EOD-US-10 AC3/7 |
| Screen: expandable per-acquisition lot rows (FIFO allocated) — date, qty acquired, qty attributed sold, qty remaining in lot, cost/unit, lot value, age | `[ ]` | EOD-US-10 AC6 |
| Screen: search by product name | `[ ]` | EOD-US-10 AC8 |
| Screen: category filter chip row | `[ ]` | EOD-US-10 AC8 |
| Screen: "At-risk only" toggle — filter to products with oldest lot ≥ 3 days | `[ ]` | EOD-US-10 AC9 |
| Screen: "Print Outstanding Inventory" button | `[ ]` | EOD-US-10 AC10 |
| If today's close is finalized: use `actual_remaining` values instead of theoretical for that product | `[ ]` | EOD-US-10 AC11 |

---

## Phase 5 — Thermal printing

**Target file:** `utils/ThermalPrintBuilders.kt` (additions only)

| Task | Status | Notes |
|------|--------|-------|
| `buildEodSummary(...): String` | `[x]` | **`ThermalPrintBuilders.kt`** — PRN-09; draft banner, channels, top 5, inventory, cash, COGS/margin, unpaid cap 10, wages |
| `buildEodSummary` header: print "DRAFT — NOT FINAL" if draft | `[x]` | EOD-US-05 AC3 |
| `buildEodSummary` outstanding orders section: if `> 10`, tail as count + note | `[x]` | EOD-US-08 AC6 |
| `buildOutstandingInventoryReport(...): String` | `[x]` | PRN-10 |
| Wire print in `DayCloseScreen` | `[x]` | EOD-US-05 AC4 |
| Wire print in `OutstandingInventoryScreen` | `[x]` | EOD-US-10 AC10 |
| `docs/printing.md` PRN-09 / PRN-10 | `[x]` | |

---

## Phase 6 — QA and acceptance verification

Run after all phases complete. Each row maps to a specific acceptance criterion.

### EOD-US-01 — Initiate day close

| AC | Status | Notes |
|----|--------|-------|
| AC1 "Close Day" visible to STORE_ASSISTANT + ADMIN only | `[ ]` | |
| AC2 Opens Day Close screen for current date; existing draft loads if present | `[ ]` | |
| AC3 Cannot close for a future date; admin can close past date | `[ ]` | |
| AC4 Warning shown if unpaid orders today OR all stock appears unsold | `[ ]` | |
| AC5 Two-step Review → Confirm; finalize sets `is_finalized=true`, `closed_at`, `closed_by` | `[ ]` | |
| AC6 Finalized close is read-only; admin un-finalize only | `[ ]` | |

### EOD-US-02 — Daily sales summary

| AC | Status | Notes |
|----|--------|-------|
| AC1 Total orders, total sales, paid/unpaid counts and amounts | `[ ]` | |
| AC2 Breakdown by channel (online / reseller / offline) — count and amount each | `[ ]` | |
| AC3 Top products by revenue — name, qty sold, total revenue | `[ ]` | |
| AC4 All amounts in peso format | `[ ]` | |
| AC5 Figures match Order History filtered to today | `[ ]` | Cross-check manually |

### EOD-US-03 — Post closing inventory

| AC | Status | Notes |
|----|--------|-------|
| AC1 Lists every product with non-zero adjusted theoretical remaining | `[ ]` | |
| AC2 Shows: total acquired all time, total sold through today, prior posted variance, adjusted theoretical, sold today, last acquisition date/cost | `[ ]` | |
| AC3 Actual remaining field defaults to adjusted theoretical; user can override | `[ ]` | |
| AC4 Variance computed live; positive = spoilage (normal); negative = surplus discrepancy (warn) | `[ ]` | |
| AC5 Variance cost = variance × WAC; shown per product and as section total | `[ ]` | |
| AC6 Product with no recent acquisition but with prior remaining shown with `sold_today = 0` | `[ ]` | |
| AC7 Aging flag on products with stock older than 3 days since last acquisition | `[ ]` | |
| AC7 "Not counted" rows excluded from spoilage total | `[ ]` | |
| AC8 Finalized close writes inventory rows to `day_close_inventory` | `[ ]` | |

### EOD-US-04 — Cash reconciliation

| AC | Status | Notes |
|----|--------|-------|
| AC1 Expected cash = paid offline+reseller orders today; **remitted** = today's **`remittances`** with **`entry_type = REMITTANCE`** (exclude **DISBURSEMENT** — **Epic 8**); optional **disbursements** line; difference colored | `[ ]` | |
| AC2 Optional cash-on-hand entry; shows vs expected; shortage/surplus | `[ ]` | |
| AC3 Non-zero discrepancy requires remarks before finalize | `[ ]` | |
| AC4 Digital collections (online channel) shown separately, not in cash math | `[ ]` | |

### EOD-US-05 — Print EOD summary

| AC | Status | Notes |
|----|--------|-------|
| AC1 Print button available in both draft and finalized states | `[ ]` | |
| AC2 Slip includes all required sections (sales, channels, top 5, inventory, cash, COGS/margin, outstanding orders, employee wages, footer) | `[ ]` | |
| AC3 Draft slip prints "DRAFT — NOT FINAL" in header | `[ ]` | |
| AC4 Print via `PrinterUtils.printMessage()` on Sunmi | `[ ]` | Device test required |

### EOD-US-06 — Day close history

| AC | Status | Notes |
|----|--------|-------|
| AC1 Screen accessible from dashboard — admin only | `[ ]` | |
| AC2 List: date, total sales, order count, gross margin, closed by, closed at | `[ ]` | |
| AC3 Tap row → full detail read-only | `[ ]` | |
| AC4 Re-print from detail view | `[ ]` | |
| AC5 Sorted descending; date range filter | `[ ]` | |
| AC6 Admin can un-finalize; logged with username + timestamp | `[ ]` | |

### EOD-US-07 — Cost of goods sold vs. revenue

| AC | Status | Notes |
|----|--------|-------|
| AC1 Today's COGS per product (WAC × qty sold), total COGS, revenue figures, margin amount and % | `[ ]` | |
| AC2 Cumulative position: total investment, total collected, outstanding inventory value, net recovered | `[ ]` | |
| AC3 Color indicator on positive/negative margin | `[ ]` | |
| AC4 Negative margin shows confirmation dialog before finalize; user can proceed | `[ ]` | |
| AC5 Other outflows (wages, **sales remittances**, **disbursements**) listed separately; not deducted from margin | `[ ]` | **Epic 8** split |

### EOD-US-08 — Outstanding orders at close

| AC | Status | Notes |
|----|--------|-------|
| AC1 All unpaid orders across all dates, sorted oldest first | `[ ]` | |
| AC2 Each row: order ID, customer name, channel, order date (age in days), amount | `[ ]` | |
| AC3 Total outstanding amount shown at top | `[ ]` | |
| AC4 Sorted by order date ASC | `[ ]` | |
| AC5 Tap row → navigate to Order Detail; mark paid; return to close | `[ ]` | Test back-nav |
| AC6 Thermal slip: up to 10 rows; if more, shows count + total only | `[ ]` | |

### EOD-US-09 — Employee day summary

| AC | Status | Notes |
|----|--------|-------|
| AC1 Section shows employees with `date_paid` = today | `[ ]` | |
| AC2 Per employee: name, gross wage, cash advance, net pay | `[ ]` | |
| AC3 "No employee payments recorded today" when section is empty — not a finalize blocker | `[ ]` | |
| AC4 Wages-due notes field on close record | `[ ]` | |
| AC5 Total wages = SUM of `amount` field (not net pay) | `[ ]` | |
| AC6 Thermal slip: single total line "Employee wages paid today: PHP X,XXX.00" | `[ ]` | |

### EOD-US-10 — Outstanding inventory report

| AC | Status | Notes |
|----|--------|-------|
| AC1 Accessible standalone from dashboard (Admin + Purchasing) | `[ ]` | |
| AC2 Also shown as read-only section inside Day Close screen | `[ ]` | |
| AC3 Per-product: total acquired, total sold, posted spoilage, theoretical on hand, WAC, value, oldest lot date, days on hand | `[ ]` | |
| AC4 Sorted by days on hand DESC | `[ ]` | |
| AC5 Total outstanding value at top | `[ ]` | |
| AC6 Expandable per-acquisition lots with FIFO attribution (date, qty, sold, remaining, cost, lot value, age) | `[ ]` | |
| AC7 Aging flags: amber ≥ 3 days, red ≥ 7 days | `[ ]` | |
| AC8 Search by product name; filter by category | `[ ]` | |
| AC9 "At-risk only" toggle | `[ ]` | |
| AC10 Print thermal slip (header, per-product lines, total, footer) | `[ ]` | |
| AC11 Finalized today's close: use `actual_remaining` instead of theoretical | `[ ]` | |

---

## Build checkpoints

| Checkpoint | Status | Notes |
|------------|--------|-------|
| Phase 1 complete: `assembleDebug` after v9 entities | `[ ]` | |
| Phase 2 complete: `testDebugUnitTest` (FIFO + WAC + DateWindow unit tests) | `[ ]` | |
| Phase 3 complete: `assembleDebug` with new routes (stubs) | `[ ]` | |
| Phase 4 complete: `assembleDebug` with all screens | `[ ]` | |
| Phase 5 complete: `assembleDebug` with print functions | `[ ]` | |
| Phase 6: full manual device test on Sunmi V2 Pro | `[ ]` | Use `./scripts/dev.sh fresh` for clean install |

---

## Known risks and edge cases to test

| Risk | Mitigation |
|------|-----------|
| First-ever close (no prior `day_close_inventory` rows) — `prior_posted_variance = 0` for all products | Unit test in `DayCloseRepository` tests; verify UI shows correct zero-baseline |
| Day close on a day with no sales — all quantities sold = 0; COGS = 0; margin undefined | Show margin as "—"; test that no division-by-zero occurs |
| Day close on a day with no acquisitions and no prior close — theoretical = 0 for all; inventory section empty | Verify section shows "No stock on hand" state gracefully |
| Product acquired but sold in a different unit (e.g., bought per-kg, sold per-piece) | Covered by D1 unit rule; verify WAC conversion is consistent |
| Very large outstanding order list (>100 unpaid orders) — thermal slip and list performance | Cap list at 10 for print; lazy list in UI; verify scroll performance |
| Un-finalize a close that has been synced to server | Log audit row; mark resync reason as `CORRECTION` on next sync |
| Back navigation from Order Detail (EOD-US-08 AC5) — must return to Day Close, not order history | Test back stack explicitly |

---

*Last updated: 2026-04-09 — Stream E + EOD-US-11..15 follow-up shipped; tracker headings synced to completed status.*
