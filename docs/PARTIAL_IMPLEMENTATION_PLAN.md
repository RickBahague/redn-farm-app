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

## Stream A — Product catalog & price history (Epic 3)

**Done (2026-04-09):** **PRD-US-01** and **PRD-US-07** implemented — `ManageProductsViewModel` + `observeAllActiveSrps`, `ProductRepository.getPriceHistory`, merged history on **ProductFormScreen**, **OrderPricingResolver.catalogSrpSummaryAmounts** / **minPerPieceSrpAcrossChannels**, preset deep link from history for pricing-capable roles.

| Story | Status |
|-------|--------|
| **PRD-US-01** | Shipped — see **USER_STORIES.md** status note |
| **PRD-US-07** | Shipped — see **USER_STORIES.md** status note |

---

## Stream B — Preset traceability from acquisitions (Epic 10)

| Story | Gap | Suggested implementation |
|-------|-----|---------------------------|
| **MGT-US-05** (partial, AC5) | No navigation from acquisition detail to preset record | On `AcquisitionFormScreen` (view/edit) or acquisition card: if `preset_ref` non-null, show tappable link → `navController.navigate(Screen.PresetDetail.createRoute(presetRef))`. Handle missing preset (deleted DB) with a clear message. |
| **AUTH-US-04** (AC6) | Confirm `saved_by` on preset save | Audit `PricingPresetRepository` / editor save paths; ensure username from `SessionManager` is persisted on preset rows and activation log. If already set, remove the verification note from `USER_STORIES.md`. |

---

## Stream C — Export: truncate / clear (Epic 9, EXP-US-02)

| Story | Gap | Suggested implementation |
|-------|-----|---------------------------|
| **EXP-US-02** | Only single-table clears; no batch checklist, FK warnings, presets+activation log, users | **Phase C1:** Add `ExportViewModel.clearSelectedTables(Set<ClearableTable>)` with ordered deletes (same order as FK-safe truncate in repositories). **C2:** UI multi-select + “Select all/none” + confirmation summary. **C3:** Dependency hints (e.g. Products without Acquisitions → dialog to add acquisitions). **C4:** Add `truncate` for `users` (non-seed rows only or full reset — product decision). **C5:** `pricing_presets` + `preset_activation_log` cleared together in one transaction. **Fix:** [`BACKLOG.md`](./BACKLOG.md) **BUG-02** — separate `truncateOrderItems()` from full order delete. |

**Exit criteria:** EXP-US-02 → **✅** or **✅ (partial)** with explicit remaining bullets.

---

## Stream D — Schema documentation (SYS-US-04)

| Story | Gap | Suggested implementation |
|-------|-----|---------------------------|
| **SYS-US-04** | Narrative and trackers cite old DB versions; `schema_evolution.sql` header out of date | **D1:** Set `FarmDatabase.kt` version as source of truth (currently **10**). **D2:** Append a new **VERSION 10** block to `docs/schema_evolution.sql` from generated `FarmDatabase_Impl` (KSP output). **D3:** Rewrite SYS-US-04 acceptance criteria in `USER_STORIES.md` to “current version + destructive migration + evolution file” without pinning to v4. **D4:** Update [`PHASE6_TRACKER.md`](./PHASE6_TRACKER.md) P6-4 and [`DESIGN.md`](./DESIGN.md) §6 to match. |

---

## Stream E — End of day (Epic 12) — highest surface area

Implement in this order to unlock printing and history parity.

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
| **EOD-US-05** | Implement `buildEodSummary` (and wire buttons) per [`EOD_EPIC_TRACKER.md`](./EOD_EPIC_TRACKER.md) Phase 5; register **PRN-08** in [`printing.md`](./printing.md). |
| **EOD-US-10** | `buildOutstandingInventoryReport` + **PRN-09** + button on **OutstandingInventoryScreen**. |

**Exit criteria:** Update Epic 12 status lines in `USER_STORIES.md` from **🔧/📋** to **✅** or **✅ (partial)** with narrow notes; refresh [`EOD_EPIC_TRACKER.md`](./EOD_EPIC_TRACKER.md) checkboxes to match reality.

---

## Stream F — Employee payments filters (**EMP-US-06**)

| Story | Gap | Suggested implementation |
|-------|-----|---------------------------|
| **EMP-US-06** AC2b | Extra date presets (last month, custom range, etc.) | Follow patterns from order history / acquisition date filters; persist last-used range if useful. |

---

## Stream G — Pricing spec alignment (**MGT-US-01**, **NFR-US-05**)

| Story | Gap | Suggested implementation |
|-------|-----|---------------------------|
| **MGT-US-01** | Spoilage: rate vs absolute kg (**BUG-PRC-04** / **pricing_clarif.md**) | Product decision + `SrpCalculator` / preset editor fields; update [`INV_ACQUISITION_SRP_TRACKER.md`](./INV_ACQUISITION_SRP_TRACKER.md). |
| **NFR-US-05** | 200 ms debounced SRP recompute on acquisition form | Measure on device; move heavy work off main thread; debounce in `AcquisitionFormViewModel`. |

---

## Stream H — Documentation consistency (ongoing)

| Doc | Action |
|-----|--------|
| [`EOD_EPIC_TRACKER.md`](./EOD_EPIC_TRACKER.md) | Treat checklist as spec; sync **Implementation snapshot** at top when phases complete (see that file). |
| [`PHASE6_TRACKER.md`](./PHASE6_TRACKER.md) | Distinguish **EXP-US-01** batch export (done) vs **USER_STORIES** **EXP-US-02** truncate (partial); update DB version to **10**. |
| [`DESIGN.md`](./DESIGN.md) | Navigation table + `FarmDatabase` version match `NavGraph.kt` / `FarmDatabase.kt`. |
| [`schema_evolution.sql`](./schema_evolution.sql) | Header comment: current Room version. |

---

## Suggested milestone ordering

1. **Quick wins:** Stream H + BUG-02 (unblocks honest EXP-US-02).  
2. **User-visible correctness:** Stream E1 (sales/cash/margin) + E3 (outstanding orders).  
3. **Operational completeness:** E5 printing, E4 history/unfinalize, E2 inventory AC parity.  
4. **Catalog clarity:** Stream A + B.  
5. **Polish:** Stream C, F, G.

---

## Reference — partial / incomplete story IDs (snapshot)

| ID | Tag in USER_STORIES |
|----|---------------------|
| PRD-US-01, PRD-US-07 | ✅ (Stream A complete 2026-04-09) |
| MGT-US-01 | ✅ (spec caveat) |
| MGT-US-05 | ✅ partial |
| AUTH-US-04 | ✅ (AC6 verify) |
| EXP-US-02 | 🔧 |
| SYS-US-04 | 🔧 |
| EOD-US-01 – EOD-US-04, EOD-US-06 – EOD-US-07, EOD-US-10 | 🔧 |
| EOD-US-05, EOD-US-08, EOD-US-09 | 📋 |
| EMP-US-06 | ✅ (AC2b backlog) |
| NFR-US-05 | 📋 |
