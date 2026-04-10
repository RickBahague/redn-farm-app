# Epic 3 — Product Management (Completion Tracker)

**Created:** 2026-04-02  
**Source review:** `docs/user_review_product_management.md`  
**Goal:** Convert current v3-era product management UI/data handling into v4-aligned behavior per PRD-US-01 through PRD-US-08.

**Stream A (2026-04-09):** **PRD-US-01** / **PRD-US-07** closed in app — catalog SRP summary + unified price/SRP history on **ProductFormScreen**. See [`USER_STORIES.md`](./USER_STORIES.md).

**Stream B (2026-04-10):** **Steps 7–9** completed in codebase — list card SRP badges aligned with `observeAllActiveSrps`; **`ProductActiveStatusFilter`** replaces `showOutOfStock`; full-screen **`ProductPriceHistoryScreen`** + shared **`ProductHistoryUi`** (history also remains summarized on **ProductFormScreen** for convenience).

**Stream C (2026-04-10):** **Step 9** deliverable polish — **`UnifiedHistoryRowContent`** shows disabled source **`AssistChip`**s **Manual** vs **Computed** (PRD-US-07 AC#2; aligns with Step 9 “source chip” wording) on both **`ProductPriceHistoryScreen`** and embedded **ProductFormScreen** history.

---

## Epic closure (2026-04-10)

**Verdict:** **Closed** — product catalog, CRUD, filters, SRP-aware list lines, manual fallback sheet, and unified price/SRP history (**PRD-US-01** … **PRD-US-07**) are implemented and traceable in code. **`./gradlew assembleDebug`** is green.

**Exception (documented):** **Step 5 / PRD-US-08 (in-app “reload seed catalog”)** is **not** shipped as a Manage Products action. **BUG-PRD-02** removed the top-bar **Refresh** / reload-default UI (confusion with per-product delete); **BUG-SYS-01** removed **`DatabaseInitializer.reinitializeDatabase()`** (destructive full DB wipe). **What remains:** first-time DB creation still runs **`DatabaseInitializer.populateDatabase()`** from **`assets/data/products.json`**, **`assets/data/product_prices.json`**, and **`assets/data/customers.json`** via **`RoomDatabase.Callback.onCreate`**. Restoring a **safe**, scoped “re-import products from JSON” (without wiping the whole DB) is **out of scope** for this epic closure — track as a follow-up if **PRD-US-08** must be strictly literal in-app.

**Manual QA:** Smoke items in **Build / tests / manual QA** (below) remain recommended before a release train; they are not automated here.

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Implemented in codebase |
| `[~]` | Partial / simplified — see notes |
| `[ ]` | Manual QA / follow-up |

---

## Step-by-step implementation activities (Priority Order)

### Step 1 — Fix screen architecture (unblocks everything else)
Activities:
1. Refactor `ManageProductsScreen` to use the ViewModel’s StateFlows (`products`, `productPrices`) instead of instantiating `ProductRepository` locally.
2. Convert `ManageProductsViewModel` to use Hilt properly (`@HiltViewModel` + `@Inject constructor`).
3. Ensure the screen reads from the ViewModel flows so list updates after add/edit/delete.
4. Remove unreachable/dead UI state in the screen (e.g., unused `showDeleteDialog`).

Deliverables:
- `ManageProductsScreen.kt` uses `viewModel.products.collectAsState()` and `viewModel.productPrices.collectAsState()`
- `ManageProductsViewModel.kt` is Hilt-powered and owns repository lifecycle

---

### Step 2 — Fix Add Product dialog
Activities:
1. Remove all price inputs from the Add dialog.
2. Add required fields:
   - unit type picker: `kg` / `piece` / `both`
   - category dropdown (optional)
   - default piece count numeric field (shown only when unit type includes piece)
3. Remove price-gated save condition (product must be savable with no price yet).
4. Save only product attributes (unit type, category, defaults), leaving price-setting as a separate action (Step 6).

Deliverables:
- Add dialog matches PRD-US-02 / AC#1 constraints

---

### Step 3 — Fix Edit Product dialog
Activities:
1. Remove all price inputs from the Edit dialog.
2. Add:
   - unit type picker
   - category
   - default piece count
   - active/inactive toggle
3. Decouple product edit from price insertion:
   - `updateProduct()` only
   - do not call `updateProductPrice()` / insert new historical price rows during product-only edits

Deliverables:
- Editing the name/category/unit type does not pollute `product_prices` history

---

### Step 4 — Wire Delete and Deactivate
Activities:
1. Implement delete entry point:
   - set `showDeleteDialog` from swipe action or long-press on product card
   - confirm calls `viewModel.deleteProduct(productId)`
2. Handle FK constraint failures gracefully:
   - show user message if delete fails due to linked orders/acquisitions
3. Implement deactivate/reactivate:
   - add toggle (card or edit screen)
   - update `is_active` in `viewModel`
4. Ensure list reflects active/inactive changes immediately.

Deliverables:
- Delete + deactivate flows work and update the displayed list

---

### Step 5 — Wire Reinitialize
Activities:
1. Add `Icons.Default.Refresh` to the `TopAppBar` in `ManageProductsScreen`.
2. Implement confirmation dialog → call `viewModel.reinitializeDatabase()`.
3. Fix `DatabaseInitializer.populateDatabase()` to load:
   - `assets/data/products.json`
   - `assets/data/product_prices.json`
   using the Gson parser already imported.
4. Remove/make safe the callback’s v1-era manual table creation schema (it no longer matches Room entities).

Deliverables:
- Reinitialize reloads correct data from JSON assets

**Reality vs original Step 5 text (closure audit):**
- **`populateDatabase()`** ✅ Loads **`data/products.json`**, **`data/product_prices.json`**, **`data/customers.json`** on **`onCreate`**; no v1 manual **CREATE TABLE** in callback (**BUG-SYS-01**).
- **Manage Products Refresh + `reinitializeDatabase()`** ❌ **Removed** on purpose — **BUG-PRD-02** (reload vs delete confusion), **BUG-SYS-01** (orphan full DB wipe API). See **[Epic closure](#epic-closure-2026-04-10)**.

---

### Step 6 — Set Manual Fallback Price (separate action — PRD-US-06)
Activities:
1. Add a dedicated “Set Fallback Price” action accessible from the product detail/edit flow.
2. Implement a bottom sheet or full-screen form with fields:
   - per-kg price (optional)
   - per-piece price (optional)
3. No “discounted” fields in this flow (manual fallback only).
4. Save by calling `repository.insertProductPrice()`.
5. Clearly label in UI: “Manual fallback — used when no acquisition SRP exists.”

Deliverables:
- Admin can set fallback prices independently of product attribute edits

---

### Step 7 — Fix product card price display (after SRP pipeline)
Activities:
1. Update `ProductCard` to read active SRPs by mapping:
   - `AcquisitionRepository.observeAllActiveSrps()` (by product ID)
2. Apply badge logic:
   - Active SRP exists → show “from ₱X/kg”, no badge
   - Only manual fallback → show fallback price + amber “Manual Price” chip
   - No price → show grey “No Price” chip
3. Ensure per-unit labels are correct (do not show “Per kg” label for piece-only products).

Deliverables:
- Product list matches PRD-US-01 AC#2..AC#4 pricing behavior

**Implementation notes (2026-04-10):** `ManageProductsScreen` `ProductCard` uses `activeAcquisitionByProductId` + `OrderPricingResolver.catalogSrpSummaryAmounts`; chips **Acquisition SRP** / **Manual price** / **No price**; manual peso amounts not on list (PRD-US-05).

---

### Step 8 — Fix filters
Activities:
1. Extend `ProductFilters` with:
   - `unitType: String?`
   - `category: String?`
   - `activeStatus: ActiveStatus` (enum: `ALL` / `ACTIVE_ONLY` / `INACTIVE_ONLY`)
2. Update `ProductRepository.getFilteredProducts()` to apply:
   - search predicate includes `product_id`
   - unit type filter
   - category filter
   - active status filter
3. Update FilterDialog UI to expose the three required filters.
4. Remove or deprecate confusing inverted semantics of `showOutOfStock`.

Deliverables:
- Filters work exactly as PRD-US-01 AC#7 expects

**Implementation notes (2026-04-10):** `ProductActiveStatusFilter` enum; `ProductFilters` fields `unitTypeFilter`, `categoryFilter`, `activeStatus` (default **ACTIVE_ONLY** — same default behavior as old “hide inactive”); **`showOutOfStock` removed**. Filter dialog: **All / Active / Inactive** chips.

---

### Step 9 — Price history screen (PRD-US-07)
Activities:
1. Add repository method:
   - `getProductPriceHistory(productId)`
   - wire through `ProductPriceDao.getPriceHistory(productId)`
2. Create a new screen `ProductPriceHistoryScreen.kt` (full-screen, not a card-in-dialog).
3. For each entry:
   - show source chip: `Computed` vs `Manual`
   - show date
   - show per-channel SRP values OR fallback values
   - include preset reference for computed entries
4. Ensure navigation entry point exists from the product card or edit flow.

Deliverables:
- Price history screen renders complete history entries and is reachable from the UI

**Implementation notes (2026-04-10):** `ProductRepository.getPriceHistory` (existing) + **`ProductPriceHistoryScreen`** route `product_price_history/{productId}`; **`ProductHistoryUi.kt`** holds `mergeProductPriceHistory` + `UnifiedHistoryRowContent` shared with **ProductFormScreen**. Entry: product card **History** icon; edit form **Price history** app bar action. Each row: date row + **Manual** or **Computed** source chip (read-only **`AssistChip`**); acquisition rows keep **Acquisition (preset/custom SRP)** title, per-channel SRP lines, **`preset_ref`**, and **Current SRP** chip when applicable (**Stream C**).

---

## Completion tracker

| Step | Completion | Notes |
|------|------------|-------|
| Step 1 — Fix screen architecture | `[x]` | Screen now consumes ViewModel StateFlows (no local repository), and ViewModel is Hilt-backed + injects repository |
| Step 2 — Fix Add dialog | `[x]` | Remove price fields; add unit/category/default_piece_count; save no longer requires price |
| Step 3 — Fix Edit dialog | `[x]` | Removed price fields; added unit/category/default_piece_count + active toggle; save updates product only |
| Step 4 — Wire Delete + Deactivate | `[x]` | Delete icon + confirm dialog + FK failure snackbar; active toggle already in edit dialog; inactive badge shown on list |
| Step 5 — Wire Reinitialize | `[~]` | **JSON seed on `onCreate` only** (`populateDatabase()`); **no** in-app Refresh / full DB reinitialize (**BUG-PRD-02**, **BUG-SYS-01**) — see **Epic closure** |
| Step 6 — Set manual fallback price | `[x]` | Separate “Set fallback price” bottom sheet; saves per-kg/per-piece via `insertProductPrice()`; clearly labeled as manual fallback |
| Step 7 — Fix product card price display | `[x]` | `ProductCard` + `observeAllActiveSrps` map + `catalogSrpSummaryAmounts` + list chips (**2026-04-10** verified in code) |
| Step 8 — Fix filters | `[x]` | `ProductActiveStatusFilter`; repo applies search + unit + category + active; dialog chips (**2026-04-10**) |
| Step 9 — Price history screen | `[x]` | `ProductPriceHistoryScreen` + `ProductHistoryUi`; nav from list + edit; **Manual** / **Computed** chips (**Stream C**, **2026-04-10**) |

---

## Build / tests / manual QA (minimum)
- `./gradlew assembleDebug` succeeds after each milestone *(verified **2026-04-10** at epic closure)*.
- Manual smoke after Steps 1–4:
  - Add product (no price) -> appears in list
  - Edit product attribute-only -> no new price history row is created
  - Deactivate/reactivate -> list respects active filter
  - Delete with FK constraint -> user-facing error appears
- Manual smoke after Steps 5–7:
  - Reinitialize reloads JSON seed set
  - Product card displays correct active SRP / manual fallback badges
- Manual smoke after Steps 8–9:
  - Filters include unit type, category, active status + product_id search
  - Price history screen shows computed/manual entries correctly
