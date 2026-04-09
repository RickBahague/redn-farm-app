# Plan — Complete Partially Implemented User Stories

**Canon:** [`USER_STORIES.md`](./USER_STORIES.md) (status lines are authoritative).  
**Created:** 2026-04-09  
**Purpose:** Ordered work to close gaps for stories marked **✅ (partial)**, **🔧**, or **📋** where code already exists but acceptance criteria are incomplete.

---

## How to use this document

1. Pick a **work stream** (below) in priority order for your milestone.
2. For each bullet, map to the **story ID** and **AC** in `USER_STORIES.md`.
3. After shipping, update the **Status** line in `USER_STORIES.md` and tick the matching tracker ([`EOD_EPIC_TRACKER.md`](./EOD_EPIC_TRACKER.md), [`PHASE6_TRACKER.md`](./PHASE6_TRACKER.md), [`EPIC3_PRODUCT_MANAGEMENT_TRACKER.md`](./EPIC3_PRODUCT_MANAGEMENT_TRACKER.md), [`INV_ACQUISITION_SRP_TRACKER.md`](./INV_ACQUISITION_SRP_TRACKER.md)) so docs stay aligned.

---

## Remaining AC checklist (agent-ready)

This section is the **current execution source** for follow-up agents.  
It is derived from `USER_STORIES.md` status lines. Any stream not listed here as complete still has actionable AC gaps.

### Stream implementation status (A-H)

| Stream | Current status | Notes |
|-------|----------------|-------|
| **A — Product catalog & price history** | ✅ Complete | `PRD-US-01`, `PRD-US-07` shipped |
| **B — Preset traceability** | ✅ Complete | `MGT-US-05` + `AUTH-US-04` AC6 shipped/verified |
| **C — Export truncate/clear** | ✅ Complete | `EXP-US-02` shipped (documented scope exclusions) |
| **D — Schema documentation** | ✅ Complete | `SYS-US-04` shipped |
| **E — End of day** | ⚠️ Not fully complete | Remaining partial stories: `EOD-US-04`, `EOD-US-09`, `EOD-US-10` |
| **F — Employee payments filters** | ✅ Complete | `EMP-US-06` AC2b shipped |
| **G — Pricing spec alignment** | ✅ Complete | `MGT-US-01`, `NFR-US-05` shipped |
| **H — Documentation consistency** | 🔄 Ongoing | Keep cross-doc sync after each shipment |

### Open items to implement

Only items below should be treated as **remaining implementation work**.

#### 1) `EOD-US-04` — Cash reconciliation (close remaining AC)

**Current status in canon:** ✅ (partial)

- [ ] **AC3 policy alignment:** Decide and implement single discrepancy rule for finalize guard:  
      require remarks for any non-zero discrepancy per AC text, or update AC text/status note to keep current counted-vs-drawer-only rule.
- [ ] **Finalize guard tests:** Add/extend ViewModel tests for all discrepancy paths:  
      zero discrepancy, expected-vs-remitted non-zero with no `cash_on_hand`, counted-vs-drawer mismatch with/without remarks.
- [ ] **UX copy consistency:** Ensure Day Close labels and validation message match final policy language (discrepancy definition + remarks requirement).
- [ ] **Doc sync:** Update `USER_STORIES.md` (`EOD-US-04` status line) and `EOD_EPIC_TRACKER.md` after policy is finalized.

#### 2) `EOD-US-10` — Outstanding inventory report (complete AC3 card fields)

**Current status in canon:** ✅ (partial)

- [ ] **AC3 per-product ledger fields:** Show missing fields on card (or equivalent clearly visible row content):  
      total acquired, total sold, previously posted spoilage, theoretical on hand, weighted average cost/unit, outstanding value, oldest unsold date, days on hand.
- [ ] **Units and labeling pass:** Ensure qty/unit labels are explicit and consistent across summary card, lot drill-down, and thermal output.
- [ ] **Sort/filter regression check:** Preserve current behaviors while adding AC3 fields: default sort by days-on-hand desc, category filter, search, at-risk toggle.
- [ ] **Doc sync:** Update `USER_STORIES.md` (`EOD-US-10` status line) and `EOD_EPIC_TRACKER.md` once AC3 display is complete.

#### 3) `EOD-US-09` — Employee day summary at close (close partial status)

**Current status in canon:** ✅ (partial)

- [ ] **AC2 presentation audit:** Confirm row fields exactly match canonical labels (employee, gross wage, cash advance, net pay) and formula note references `BUG-EMP-01`.
- [ ] **AC4 notes UX audit:** Confirm notes affordance clearly communicates “wages due but not yet paid” intent and remains non-blocking for finalize.
- [ ] **AC6 print wording parity:** Validate thermal line text/format is aligned with story wording (“Employee wages paid today: PHP X,XXX.00”).
- [ ] **Doc sync:** Update `USER_STORIES.md` (`EOD-US-09` status line) and `EOD_EPIC_TRACKER.md` after final UX/print parity checks.

### Exit criteria for this checklist

- [ ] `EOD-US-04`, `EOD-US-09`, `EOD-US-10` move from **✅ (partial)** to **✅** in `USER_STORIES.md`
- [ ] Epic 12 tracker reflects identical completion state and notes
- [ ] This file keeps historical record sections below but the **Remaining AC checklist** remains the active handoff section

### Agent task cards (assignable)

Use one card per agent. Keep each implementation branch scoped to one story.

#### Agent Card A — `EOD-US-04` Cash reconciliation

**Goal:** Close remaining AC policy gap and align UI + finalize behavior.

**Primary scope/files:**
- `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseViewModel.kt`
- `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseScreen.kt`
- `app/src/main/java/com/redn/farm/data/repository/DayCloseRepository.kt` *(only if query/policy data wiring changes)*
- `docs/USER_STORIES.md`
- `docs/EOD_EPIC_TRACKER.md`

**Implementation checklist:**
- [ ] Decide canonical discrepancy policy for AC3 (strict "any non-zero discrepancy requires remarks" vs current counted-vs-drawer-only guard).
- [ ] Implement finalize guard to match chosen policy.
- [ ] Align on-screen labels/help text/snackbar with chosen policy.
- [ ] Add or update unit tests for finalize guard permutations.
- [ ] Update story status text + tracker note after behavior is finalized.

**Acceptance verification (manual + automated):**
- [ ] `./gradlew :app:testDebugUnitTest --tests "*DayCloseViewModel*"`
- [ ] Manual: expected-remitted non-zero with no cash-on-hand -> behavior matches canonical policy.
- [ ] Manual: counted cash mismatch with empty remarks -> finalize blocked with clear guidance.
- [ ] Manual: discrepancy resolved or remarks provided -> finalize allowed.

**Out of scope:** redesigning cash section layout beyond wording/validation needed for AC parity.

---

#### Agent Card B — `EOD-US-10` Outstanding inventory report

**Goal:** Complete AC3 per-product ledger display while preserving existing filters/print behavior.

**Primary scope/files:**
- `app/src/main/java/com/redn/farm/ui/screens/eod/OutstandingInventoryScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/eod/OutstandingInventoryViewModel.kt` *(if UI model needs additional fields/plumbing)*
- `app/src/main/java/com/redn/farm/data/repository/DayCloseRepository.kt` *(if missing card fields are not exposed yet)*
- `app/src/main/java/com/redn/farm/utils/ThermalPrintBuilders.kt` *(only if label/unit parity updates are needed)*
- `docs/USER_STORIES.md`
- `docs/EOD_EPIC_TRACKER.md`

**Implementation checklist:**
- [ ] Surface all AC3 summary fields on each product card (or equivalent always-visible summary row).
- [ ] Keep unit labels explicit and consistent for qty, cost/unit, and value.
- [ ] Preserve sort/filter behavior (days-on-hand desc default, search, category, at-risk).
- [ ] Validate no regression in lot drill-down and print output.
- [ ] Update story status text + tracker note after AC3 parity is complete.

**Acceptance verification (manual + automated):**
- [ ] `./gradlew assembleDebug`
- [ ] Manual: product card visibly includes total acquired/sold/spoilage/theoretical/cost/value/oldest/date-age.
- [ ] Manual: category, search, and at-risk filters still work together.
- [ ] Manual: print output remains readable and unit-consistent.

**Out of scope:** changing FIFO logic or aging thresholds unless required by AC defect.

---

#### Agent Card C — `EOD-US-09` Employee day summary at close

**Goal:** Remove partial status by closing display/wording parity checks for section + print line.

**Primary scope/files:**
- `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseViewModel.kt` *(only if formatting data changes are needed)*
- `app/src/main/java/com/redn/farm/utils/ThermalPrintBuilders.kt`
- `docs/USER_STORIES.md`
- `docs/EOD_EPIC_TRACKER.md`

**Implementation checklist:**
- [ ] Ensure row presentation matches AC2 labels (employee, gross wage, cash advance, net pay).
- [ ] Ensure notes affordance clearly covers "wages due but not yet paid" and remains non-blocking.
- [ ] Ensure printed wages line wording/format matches AC6 intent.
- [ ] Confirm formula note remains aligned with `BUG-EMP-01`.
- [ ] Update story status text + tracker note after parity confirmation.

**Acceptance verification (manual + automated):**
- [ ] `./gradlew assembleDebug`
- [ ] Manual: with payments today, rows show all required fields and total wages line.
- [ ] Manual: with no payments today, empty-state copy is shown and close remains allowed.
- [ ] Manual: EOD slip includes wages line with expected wording/value.

**Out of scope:** payroll domain rule changes outside EOD summary representation.

### Parallel execution plan (3 agents)

Run these cards in parallel, but with doc-update sequencing to avoid merge churn.

#### Agent allocation

| Agent | Card | Primary code focus | Risk level |
|------|------|--------------------|------------|
| **Agent 1** | Card A (`EOD-US-04`) | `DayCloseViewModel`, `DayCloseScreen`, tests | Medium |
| **Agent 2** | Card B (`EOD-US-10`) | `OutstandingInventoryScreen` (+ repo/viewmodel if needed) | Medium |
| **Agent 3** | Card C (`EOD-US-09`) | `DayCloseScreen`, `ThermalPrintBuilders` | Low/Medium |

#### Branching strategy

- Create one branch per card:
  - `agent/eod-us-04-cash-recon`
  - `agent/eod-us-10-outstanding-ac3`
  - `agent/eod-us-09-employee-summary`
- Keep each PR scoped to one story only (no opportunistic cleanup).
- Defer shared doc edits (`USER_STORIES.md`, `EOD_EPIC_TRACKER.md`) until code is validated to reduce rebases.

#### File overlap and conflict controls

- **Potential overlap:** `DayCloseScreen.kt` (Agent 1 + Agent 3).
  - Agent 1 owns **Cash reconciliation** section and finalize warnings/copy only.
  - Agent 3 owns **Employee payments + notes** section and wages print wording only.
  - No cross-edit outside owned section boundaries.
- **No expected overlap:** Agent 2 work is mainly in `OutstandingInventory*` files.
- Use small commits grouped by concern (UI, logic, tests, docs).

#### Integration sequence (recommended)

1. **Merge Agent 2 first** (`EOD-US-10`)  
   Rationale: mostly isolated files; lowest blast radius on EOD screen.
2. **Merge Agent 1 second** (`EOD-US-04`)  
   Rationale: finalize guard behavior can affect close flow; should stabilize before final doc closure.
3. **Rebase Agent 3**, then merge (`EOD-US-09`)  
   Rationale: shared `DayCloseScreen.kt`; resolve any UI text/section adjacency conflicts last.
4. **Final doc sync pass** in a short follow-up PR:
   - update status lines in `USER_STORIES.md`
   - update `EOD_EPIC_TRACKER.md`
   - tick this checklist exit criteria

#### Validation matrix before merge

- **Agent 1 (`EOD-US-04`)**
  - `./gradlew :app:testDebugUnitTest --tests "*DayCloseViewModel*"`
  - manual finalize guard scenarios (remarks/discrepancy combinations)
- **Agent 2 (`EOD-US-10`)**
  - `./gradlew assembleDebug`
  - manual AC3 field visibility + filter/sort/print checks
- **Agent 3 (`EOD-US-09`)**
  - `./gradlew assembleDebug`
  - manual employee summary rows, empty state, wages line in thermal slip
- **Post-merge smoke**
  - open Day Close for today, walk Review -> Confirm finalize path
  - print draft + finalized EOD once

#### Definition of done for parallel batch

- All 3 card PRs merged without unresolved behavior conflicts.
- `EOD-US-04`, `EOD-US-09`, `EOD-US-10` status lines moved to **✅** in `USER_STORIES.md`.
- `EOD_EPIC_TRACKER.md` and this plan reflect same final state.

---

## Stream A — Product catalog & price history (Epic 3)

**✅ shipped (2026-04-09):** **PRD-US-01** and **PRD-US-07** implemented — `ManageProductsViewModel` + `observeAllActiveSrps`, `ProductRepository.getPriceHistory`, merged history on **ProductFormScreen**, **OrderPricingResolver.catalogSrpSummaryAmounts** / **minPerPieceSrpAcrossChannels**, preset deep link from history for pricing-capable roles.

| Story | Status |
|-------|--------|
| **PRD-US-01** | Shipped — see **USER_STORIES.md** status note |
| **PRD-US-07** | Shipped — see **USER_STORIES.md** status note |

---

## Stream B — Preset traceability from acquisitions (Epic 10)

**✅ shipped (2026-04-09):** **`AcquisitionFormScreen`** shows **Pricing preset** when **`preset_ref`** is set; **ADMIN** navigates to **`PresetDetail`** via **`NavGraph`**; **Purchasing** sees read-only preset ID (same rule as **ProductForm**). **`PresetDetailScreen`** distinguishes loading vs **preset not found**. **AUTH-US-04** AC6 audited: save path sets **`saved_by`**; activation uses **`SessionManager`** username; repository KDoc updated.

| Story | Status |
|-------|--------|
| **MGT-US-05** | Shipped — see **USER_STORIES.md** |
| **AUTH-US-04** (AC6) | Verified — see **USER_STORIES.md** |

---

## Stream C — Export: truncate / clear (Epic 9, EXP-US-02)

**✅ shipped (2026-04-09):** **`ClearableTable`** + **`ExportViewModel.clearSelectedTables`**; **`ExportScreen`** batch clear UI; **`UserDao.deleteNonSeedUsers`**; **`PricingPresetDao.truncatePresetsAndActivationLog`**; **`PricingPresetRepository.truncatePresetsAndActivationLog`**; **`OrderRepository.truncateOrderItemsOnly`**; **`truncateProductPrices`** no longer calls full product truncate; [**BUG-02**](./BACKLOG.md) closed.

| Story | Status |
|-------|--------|
| **EXP-US-02** | Shipped — see **USER_STORIES.md** (narrow exclusions: day-close tables, full user wipe) |

---

## Stream D — Schema documentation (SYS-US-04)

**✅ shipped (2026-04-09):** **`FarmDatabase.kt`** KDoc + **`version = 10`** as source of truth; **`docs/schema_evolution.sql`** header points at KSP **`FarmDatabase_Impl`**; **VERSION 10** block = full DDL from **`createAllTables`** (incl. EOD + `remittances.entry_type`); **`USER_STORIES.md`** SYS-US-04 rewritten; **`DESIGN.md`** §6 (converters, table details, migrations); **`PHASE6_TRACKER.md`** P6-4 checked.

| Story | Status |
|-------|--------|
| **SYS-US-04** | Shipped — see **USER_STORIES.md** |

---

## Stream E — End of day (Epic 12) — highest surface area

**✅ shipped (2026-04-09):** Day close **E1–E5** shipped in code — sales/cash/cumulative/outstanding orders/employees, inventory seed + persist + finalize write, history range chips, outstanding inventory filters + print + finalized-close qty override, thermal **`buildEodSummary`** / **`buildOutstandingInventoryReport`**; **USER_STORIES.md** Epic 12 statuses and **`EOD_EPIC_TRACKER.md`** snapshot refreshed.

**Follow-up status:** Stream E follow-up stories in **`USER_STORIES.md`** (**EOD-US-11** through **EOD-US-15**) are shipped (2026-04-09).

The checklist below is retained as an **implementation record** for E1–E5.

### E1 — Day close screen completeness (**EOD-US-01**, **EOD-US-02**, **EOD-US-04**, **EOD-US-07**)

| Item | Work |
|------|------|
| **EOD-US-01 AC4** | Top-of-screen warnings: unpaid orders **today**; optional “acquisitions today with no sales” heuristic (query-driven). |
| **EOD-US-01 AC3** | Past-date close: only from **DayCloseHistory** or date picker for **ADMIN**; block future dates. |
| **EOD-US-01 AC6** | **Un-finalize** button (admin) on finalized **DayCloseScreen** → `DayCloseRepository.unfinalize` + audit row. |
| **EOD-US-02** | **Sales summary:** channel breakdown (reuse queries sketched in [`EOD_EPIC_TRACKER.md`](./EOD_EPIC_TRACKER.md) Phase 2b); top N products by revenue for the business day. |
| **EOD-US-04** | Replace simplified card with: expected cash (paid offline+reseller today), remittances sum (**REMITTANCE** only), disbursements line, digital collections line, difference coloring; **block finalize** if discrepancy ≠ 0 and remarks empty. |
| **EOD-US-07** | **Cumulative position** subsection: total acquisition spend all-time, total collected paid orders, outstanding inventory value, net recovered; color indicators. **Other outflows** list: wages today, remittances, disbursements (informational). |

### E2 — Inventory close (**EOD-US-03**)

| Item | Work |
|------|------|
| Per-row fields | Ensure list matches AC2: total acquired all time, total sold through close date, prior posted spoilage, adjusted theoretical, sold today, **last acquisition** snippet. |
| UX | Toggle “show zero-theoretical products”; **not counted** state excluding row from spoilage total; aging highlight (>3 days) per AC6–7. |
| Persistence | Verify `enterActualCount` persists to DB before finalize; on finalize, bulk-write `day_close_inventory` and lock edits. |

### E3 — Outstanding orders & employees (**EOD-US-08**, **EOD-US-09**)

| Item | Work |
|------|------|
| **EOD-US-08** | New section on **DayCloseScreen**: `OrderDao.getAllUnpaid()`-style query, sort ASC by date, total header, tap → `OrderDetail`; cap display + “N more” for print. |
| **EOD-US-09** | Section: payments with `date_paid` in business window; list + total; link `notes` on `day_closes` for “wages due” free text. |

### E4 — History & outstanding inventory polish (**EOD-US-06**, **EOD-US-10**)

| Item | Work |
|------|------|
| **EOD-US-06** | Date range filter on history; optional read-only **detail** route vs reusing **DayClose** in read-only mode; **Re-print**; **Un-finalize** from detail. |
| **EOD-US-10** | Search, category filter, **At-risk only** toggle; **Print** via thermal builder; **AC11** — if today’s close finalized, blend `actual_remaining` into outstanding calculation (see tracker D6). |

### E5 — Thermal print (**EOD-US-05**, ties to E4)

| Item | Work |
|------|------|
| **EOD-US-05** | Implement `buildEodSummary` (and wire buttons) per [`EOD_EPIC_TRACKER.md`](./EOD_EPIC_TRACKER.md) Phase 5; register **PRN-09** in [`printing.md`](./printing.md). |
| **EOD-US-10** | `buildOutstandingInventoryReport` + **PRN-10** + button on **OutstandingInventoryScreen**. |

**Exit criteria:** Update Epic 12 status lines in `USER_STORIES.md` from **🔧/📋** to **✅** or **✅ (partial)** with narrow notes; refresh [`EOD_EPIC_TRACKER.md`](./EOD_EPIC_TRACKER.md) checkboxes to match reality.

---

## Stream E-followup — Epic 12 closure record (completed)

This section is retained as the completed execution record after Stream E shipment.

**Bug cross-reference:** Track mixed-unit labeling defect in Day Close / EOD print as [`BUG-EOD-01`](./bugs.md).

| Story | Gap | Suggested implementation |
|-------|-----|---------------------------|
| **EOD-US-12** | ✅ shipped (2026-04-09) | Added daily paid/unpaid counts+amounts and delivered count to Day Close sales summary via DAO aggregate + repository snapshot |
| **EOD-US-13** | ✅ shipped (2026-04-09) | Added latest-acquisition date + qty + unit-cost snippet to Day Close inventory rows via repository snapshot + UI render; unit labeling aligned with [`BUG-EOD-01`](./bugs.md) |
| **EOD-US-15** | ✅ shipped (2026-04-09) | `buildEodSummary` footer now uses draft **printed by/printed at** and finalized **closed by/closed at** metadata; inventory print units remain aligned with [`BUG-EOD-01`](./bugs.md) |
| **EOD-US-14** | ✅ shipped (2026-04-09) | History now lists finalized closes with sales/orders/margin and closed_by/closed_at metadata; date filters retained |
| **EOD-US-11** | ✅ shipped (2026-04-09) | Added explicit Review → Confirm finalize flow in Day Close UI; finalize requests gated to review state in ViewModel |

### Execution table (pickup-ready)

| Story | Primary files | Validation steps | Status |
|-------|---------------|------------------|--------|
| **EOD-US-12** | `app/src/main/java/com/redn/farm/data/local/dao/OrderDao.kt`, `app/src/main/java/com/redn/farm/data/repository/DayCloseRepository.kt`, `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseScreen.kt`, `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseViewModel.kt` | `./gradlew assembleDebug` ✅; manual parity check vs Order History date filter | `[x]` |
| **EOD-US-13** | `app/src/main/java/com/redn/farm/data/local/dao/AcquisitionDao.kt`, `app/src/main/java/com/redn/farm/data/repository/DayCloseRepository.kt`, `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseScreen.kt` | `./gradlew assembleDebug` ✅; manually verify inventory row shows last acquisition date + qty + unit cost; verify fallback text for products with no acquisition history | `[x]` |
| **EOD-US-15** | `app/src/main/java/com/redn/farm/utils/ThermalPrintBuilders.kt`, `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseViewModel.kt`, `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseScreen.kt` | `./gradlew assembleDebug` ✅; print draft + finalized EOD slip and confirm footer fields (`printed_by/printed_at` for draft, `closed_by/closed_at` for finalized) | `[x]` |
| **EOD-US-14** | `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseHistoryScreen.kt`, `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseHistoryViewModel.kt`, `app/src/main/java/com/redn/farm/data/repository/DayCloseRepository.kt` | `./gradlew assembleDebug` ✅; manually verify history row metadata (sales/orders/margin/closed by/closed at) and filters (All/30/90 days) still work | `[x]` |
| **EOD-US-11** | `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseScreen.kt`, `app/src/main/java/com/redn/farm/ui/screens/eod/DayCloseViewModel.kt`, `app/src/main/java/com/redn/farm/data/repository/DayCloseRepository.kt` | `./gradlew assembleDebug` ✅; manual flow test: Review state visible, explicit Confirm action required, finalize only after confirmation, negative-margin + cash-remarks guards still enforced | `[x]` |

---

## Stream F — Employee payments filters (**EMP-US-06**)

| Story | Gap | Suggested implementation |
|-------|-----|---------------------------|
| **EMP-US-06** AC2b | ~~Extra date presets (last month, custom range, etc.)~~ **✅ shipped (2026-04-09)** | Added presets: Last month, Last 3 months, Last 6 months, and Custom date range in `EmployeePaymentScreen`; custom range uses explicit start/end date pickers and feed-through to print summary period label. |

---

## Stream G — Pricing spec alignment (**MGT-US-01**, **NFR-US-05**)

| Story | Gap | Suggested implementation |
|-------|-----|---------------------------|
| **MGT-US-01** | ~~Spoilage: rate vs absolute kg (**BUG-PRC-04** / **pricing_clarif.md**)~~ **✅ shipped (2026-04-09)** | Existing implementation uses preset spoilage rate with optional per-line absolute `spoilage_kg` override in acquisition flow; reflected in `SrpCalculator` + `AcquisitionFormScreen` + [`INV_ACQUISITION_SRP_TRACKER.md`](./INV_ACQUISITION_SRP_TRACKER.md). |
| **NFR-US-05** | ~~200 ms debounced SRP recompute on acquisition form~~ **✅ shipped (2026-04-09)** | `AcquisitionFormScreen` preview effect now debounces at **200ms** and runs preview computation in `Dispatchers.Default` before updating UI state. |

---

## Stream H — Documentation consistency (ongoing)

| Doc | Action |
|-----|--------|
| [`EOD_EPIC_TRACKER.md`](./EOD_EPIC_TRACKER.md) | Treat checklist as spec; sync **Implementation snapshot** at top when phases complete (see that file). |
| [`PHASE6_TRACKER.md`](./PHASE6_TRACKER.md) | DB version **10**; P6-4 synced (Stream D). |
| [`DESIGN.md`](./DESIGN.md) | Navigation table + `FarmDatabase` version match `NavGraph.kt` / `FarmDatabase.kt`. |
| [`schema_evolution.sql`](./schema_evolution.sql) | ~~Header + VERSION 10~~ — done Stream D; re-verify when bumping Room. |

### Canonical status table (quick sync)

Use this table as the single copy/paste reference when updating trackers; source-of-truth status text lives in [`USER_STORIES.md`](./USER_STORIES.md).

| Story | Canonical status | Cross-docs that should match |
|-------|------------------|------------------------------|
| `EOD-US-11` | ✅ shipped (Review → Confirm finalize) | `EOD_EPIC_TRACKER.md`, `USER_STORIES.md` |
| `EOD-US-12` | ✅ shipped (daily paid/unpaid/delivered rows) | `EOD_EPIC_TRACKER.md`, `USER_STORIES.md` |
| `EOD-US-13` | ✅ shipped (last acquisition qty/cost snippet) | `EOD_EPIC_TRACKER.md`, `USER_STORIES.md` |
| `EOD-US-14` | ✅ shipped (history row metadata completeness) | `EOD_EPIC_TRACKER.md`, `USER_STORIES.md` |
| `EOD-US-15` | ✅ shipped (EOD slip footer close metadata) | `EOD_EPIC_TRACKER.md`, `USER_STORIES.md` |
| `EMP-US-06` AC2b | ✅ shipped (extra presets + custom range) | `EMP_EPIC_TRACKER.md`, `DESIGN.md`, `USER_STORIES.md` |
| `MGT-US-01` | ✅ shipped (rate + optional per-line `spoilage_kg` override policy) | `INV_ACQUISITION_SRP_TRACKER.md`, `USER_STORIES.md` |
| `NFR-US-05` | ✅ shipped (200ms debounce + background SRP preview compute) | `USER_STORIES.md` |

---

## Suggested milestone ordering (historical execution order)

1. **Epic 12 closure (quick wins):** `EOD-US-12` + `EOD-US-13` + `EOD-US-15`.  
2. **History/readability:** `EOD-US-14`.  
3. **Flow hardening:** `EOD-US-11` (Review → Confirm behavior change).  
4. **Documentation hygiene:** Stream H (ongoing sync after each shipped slice).

---

## Reference — partial / incomplete story IDs (snapshot)

| ID | Tag in USER_STORIES |
|----|---------------------|
| PRD-US-01, PRD-US-07 | ✅ (Stream A complete 2026-04-09) |
| MGT-US-01 | ✅ (spec caveat) |
| MGT-US-05 | ✅ (Stream B complete 2026-04-09) |
| AUTH-US-04 | ✅ (AC6 verify) |
| EXP-US-02 | ✅ (Stream C complete 2026-04-09) |
| SYS-US-04 | ✅ (Stream D complete 2026-04-09) |
| EOD-US-01 – EOD-US-10 | ✅ or ✅ (partial) — see **USER_STORIES.md** (Stream E 2026-04-09) |
| EOD-US-11 – EOD-US-15 | ✅ (Stream E follow-up shipped 2026-04-09) |
| EMP-US-06 | ✅ (AC2b shipped 2026-04-09) |
| NFR-US-05 | ✅ (shipped 2026-04-09) |
