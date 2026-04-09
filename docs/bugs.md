# Bugs & Fix Tracker

**Created:** 2026-04-02  
**Purpose:** Consolidate user-reported issues + implementation fixes across UI improvements and Epic work.

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Fixed in codebase |
| `[~]` | Mitigated / partial |
| `[ ]` | Not fixed yet |

---

## BUG-ACQ-01 — Acquire: tapping numeric inputs shows no keyboard

### Report
- **Screen:** Acquire Produce → Add/Edit Acquisition dialog
- **Symptom:** Tapping numeric input boxes does not bring up the keyboard (and appears “dead”).

### Root cause
- Quantity / Price / Total fields are `readOnly` (by design for UI-04 numeric pad), and relied on `Modifier.clickable` to open the numeric pad.
- `OutlinedTextField` often consumes pointer input internally, so the outer `clickable` can fail to trigger.
- Add/Edit Acquisition UI is hosted inside an `AlertDialog` (separate dialog window). A `ModalBottomSheet` numeric pad is hosted in the activity window, so it can render **behind** the `AlertDialog` (looks like “nothing opened” / keyboard appears under the dialog).

### Fix
- Open the numeric pad **on press** using the field’s `interactionSource.collectIsPressedAsState()`, and immediately clear focus via `LocalFocusManager.clearFocus()`.
- Add an explicit trailing icon button (dialpad) as a guaranteed tap target to open the numeric pad.
- Render the numeric pad as a bottom-aligned **`Dialog`** (instead of `ModalBottomSheet`) so it appears above `AlertDialog` content, with `Modifier.imePadding()` applied.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/acquire/AcquireProduceScreen.kt`

### Verification
- `./gradlew assembleDebug` ✅
- Manual QA: **verified on device** ✅ (numeric pad appears above the Add/Edit Acquisition dialog)

---

## BUG-PRD-01 — Take Order: adding a product (quantity / search) — IME or numeric pad under dialog

*(Originally misfiled as Manage Products → Add Product; the observed issue is on **Take Order**.)*

### Report
- **Screen:** Take Order → **Select Product** / **Add Product** (`ProductSelectionDialog`)
- **Symptom:** Search keyboard or quantity **numeric pad** appears to do nothing, or looks **under** the dialog.

### Root cause
- **`AlertDialog`** is a separate window: without `imePadding()` on the dialog body, IME insets don’t lift content, so the keyboard can sit visually under the dialog.
- **Quantity** used a **`readOnly` `OutlinedTextField` + outer `Modifier.clickable`**: clicks are often consumed by the text field, so the numeric pad may not open (same class of issue as `BUG-ACQ-01`).
- **`NumericPadBottomSheet`** is implemented as a full-screen **`Dialog`** overlay (not `ModalBottomSheet`), so it stacks above `AlertDialog` once the pad is shown.

### Fix (implemented — mark `[x]` after your review)
- **Take Order** `ProductSelectionDialog`: `imePadding()` on the dialog body column; quantity field uses **press `interactionSource` + dialpad icon** (no outer `clickable`).
- **Edit Order** (same UX): `EditOrderScreen` private `ProductSelectionDialog` + `EditOrderItemDialog` updated the same way.
- **Also** (related hardening): Manage Products dialogs / filter / fallback sheet still use scroll + `imePadding()` where applied earlier.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/order/ProductSelectionDialog.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/EditOrderScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/manage/products/ManageProductsScreen.kt` (related)
- `app/src/main/java/com/redn/farm/ui/components/NumericPadBottomSheet.kt`

### Verification
- `./gradlew assembleDebug` ✅
- Manual on device: pending (you mark `[x]` in the tracker when satisfied)

---

## BUG-ORD-01 — Take Order: Place order not fully visible; move to app bar

### Report
- **Screen:** Take Order
- **Symptom:** After adding line items, the **Place order** control (previously in the bottom bar) is not fully visible on screen / easy to reach.
- **Desired UX:** **Place order** on the **top bar**, same row as **Active SRPs** and **Order history**.

### Root cause
- **Bottom bar** + scrollable cart + **Order summary card** competed for vertical space; on smaller screens or with many items the primary action was clipped or pushed off‑screen.

### Fix (implemented — mark `[x]` after your review)
- **Submit** in **`TopAppBar` actions**: compact **`FilledTonalButton`** label **“Order”** (full action = place order) with a11y **`contentDescription = "Place order"`**, before Active SRP and History — avoids truncation on narrow screens vs long “Place order” text.
- **Thin `bottomBar`**: when the cart is non-empty, a single row shows **Total** + amount only (no second button) so the running total stays visible while scrolling; line breakdowns stay in **`OrderSummaryCard`**.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/order/TakeOrderScreen.kt`

### Verification
- `./gradlew assembleDebug` ✅
- Manual on device: pending

---

## BUG-ORD-02 — Order edit: finalize only when paid **and** delivered

### Report
- **Screen:** Order detail / edit order
- **Symptom / requirement:** Orders could be treated as **read-only** (or **View** vs **Edit**) as soon as **paid**, even when **delivered** was not set — so staff could not finish tagging delivery while lines were locked, or the UI blocked **Delivered** after payment.
- **Rule:** Do **not** treat an order as finalized / read-only for line items until **both** **paid** and **delivered** are true. Until then, keep **channel**, **line items**, **add/remove**, and **delivery** editable as appropriate.

### Root cause
- Logic was **`is_paid` only**: title “View” vs “Edit”, save, channel chips, cart editing, and dialogs gated on `!is_paid`. **Delivered** switch used `enabled = !is_paid`, which turned off delivery updates after marking paid.

### Fix (implemented — mark `[x]` after your review)
- Added **`Order.isOrderFinalized`** (`is_paid && is_delivered`) in `data/model/Order.kt`.
- **Edit order:** “View” title, **Save**, date edit (when not finalized), **channel** chips, **add/remove/edit** lines, and item dialogs use **`!isOrderFinalized`** (not `!is_paid`). **Delete order** remains **unpaid only**.
- **Payment & delivery card:** always shows **Paid** and **Delivered** switches; delivery is no longer disabled after payment.
- **Order detail:** **Edit** (app bar + bottom button) available until **`isOrderFinalized`**; **Delivered** switch no longer tied to paid-only enablement.
- Payment confirm copy updated (removed misleading “cannot be undone” for paid-only lock-in).

### Files
- `app/src/main/java/com/redn/farm/data/model/Order.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/EditOrderScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/OrderDetailScreen.kt`

### Verification
- `./gradlew assembleDebug` ✅
- Manual: unpaid → edit lines; paid, not delivered → still edit lines + toggle delivered; both paid and delivered → **View** + lines locked; clear **Delivered** or **Paid** → editing unlocks again.

---

## BUG-PRD-02 — Manage Products: “Reload default data” vs per-product delete (resolved by removal)

### Report
- **Screen:** Manage Products
- **Symptom:** Top-bar **reload default / full DB reset** was easy to confuse with **per-product delete**.

### Fix
- **Removed** the reload-default UI from Manage Products (**Refresh** icon, confirmation dialog, blocking progress).
- **Removed** `reinitializeDatabase()` / `isReinitializing` / `DatabaseInitializer` usage from **`ManageProductsViewModel`**.
- **Per-product delete** (trash on each card + confirm dialog) is unchanged.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/manage/products/ManageProductsScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/manage/products/ManageProductsViewModel.kt`

### Verification
- `./gradlew assembleDebug` ✅

---

## BUG-SYS-01 — `DatabaseInitializer.reinitializeDatabase()` (orphaned full DB wipe)

### Report
- **`DatabaseInitializer`** exposed **`reinitializeDatabase()`**: close DB, **`clearInstance()`**, **`deleteDatabase("farm_database")`**, then **`populateDatabase()`** — a full destructive reset with no in-app caller after Manage Products reload UI was removed (**BUG-PRD-02**).
- Risk if ever wired again without guardrails: data loss on production devices; prior notes flagged race/un safety (**TD-06** in backlog).

### Fix
- **Removed** `reinitializeDatabase()` from **`DatabaseInitializer`**. First-run / post-install seeding remains **`RoomDatabase.Callback.onCreate`** → **`populateDatabase()`** only.

### Files
- `app/src/main/java/com/redn/farm/data/local/DatabaseInitializer.kt`

### Verification
- `./gradlew assembleDebug` ✅

---

## BUG-PRC-01 — Preset history: allow delete for inactive presets only

### Report
- **Screen:** Pricing → **Preset history** / **Preset detail**.
- **Requirement:** Delete inactive presets only; **active** preset cannot be deleted (activate another first).

### Fix (implemented)
- **`PricingPresetDao.deleteById`**, **`PricingPresetRepository.deleteInactivePreset`** (`check(!is_active)` + clear error messages).
- **Preset history:** trash icon on inactive rows only → confirm dialog → snackbar on success/error.
- **Preset detail:** **Delete preset** outlined button (inactive only) → confirm → pop back on success; snackbar on error.

### Files
- `app/src/main/java/com/redn/farm/data/local/dao/PricingPresetDao.kt`
- `app/src/main/java/com/redn/farm/data/repository/PricingPresetRepository.kt`
- `app/src/main/java/com/redn/farm/ui/screens/pricing/PresetHistoryScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/pricing/PresetHistoryViewModel.kt`
- `app/src/main/java/com/redn/farm/ui/screens/pricing/PresetDetailScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/pricing/PresetDetailViewModel.kt`

### Verification
- `./gradlew assembleDebug` ✅

---

## BUG-EMP-01 — Employee payment: net pay should be gross + cash advance; liquidated recording-only

### Report
- **Screens:** Add / edit employee payment (`PaymentFormScreen` / full-screen payment form route).
- **Required behavior:**
  - **Net pay** must be computed as **gross wage (`amount`) + cash advance (`cash_advance_amount`)** on **both** add and edit flows (same formula everywhere that net is shown for the form).
  - **`liquidated_amount`** is **for recording only** — it must **not** be included in the net pay calculation on this screen (still stored and shown elsewhere, e.g. history aggregates / outstanding advance logic, as applicable).

### Doc realignment *(2026-04-02)*

Canonical product text is now **gross + cash advance** with **liquidated excluded** from net pay in:

- [`USER_STORIES.md`](./USER_STORIES.md) (EMP-US-05, EMP-US-06)
- [`EMP_EPIC_TRACKER.md`](./EMP_EPIC_TRACKER.md)
- [`DESIGN.md`](./DESIGN.md)
- [`figma/UI-Spec.md`](./figma/UI-Spec.md)
- [`UI-Improvement-Plan.md`](./UI-Improvement-Plan.md) (UI-16)
- [`UI_IMPROVEMENT_TRACKER.md`](./UI_IMPROVEMENT_TRACKER.md) (UI-16 row)
- [`PHASE1_TRACKER.md`](./PHASE1_TRACKER.md), [`rebuild_plan.md`](./rebuild_plan.md) (verification wording)

### Fix *(implemented 2026-04-02)*

- **`EmployeePayment.netPayAmount()`** in `EmployeePaymentAggregates.kt` — single source for list + tests.
- **`PaymentFormScreen`:** summary shows gross, **+** cash advance (in net), liquidated with “recorded only” note; net = gross + advance; warning when net is negative.
- **`PaymentCard`:** per-row net via `netPayAmount()`.
- **`EmployeePaymentNetPayTest`:** gross + advance; liquidated ignored.

### Verification
- `./gradlew :app:testDebugUnitTest --tests "*.EmployeePaymentNetPayTest"` ✅  
- `./gradlew assembleDebug` ✅  
- Manual: editing liquidated does not change net pay; gross + advance does.

---

## BUG-EMP-02 — Employee payment: signature optional; save draft without signing

### Report
- **Screens:** Add / edit employee payment (`PaymentFormScreen` / payment dialog flow).
- **Requirement:** **Signature is optional** while the user is entering or updating a payment. The entry **must be saveable** without capturing a signature (draft / work-in-progress).

### Fix *(implemented)*
- **`PaymentFormScreen`:** Removed signature check before **Save**; empty signature is persisted as `""`.
- **Copy:** Section label clarifies signature is optional for save; **Print Voucher** still requires gross + signature (snackbar unchanged).

### Files
- `app/src/main/java/com/redn/farm/ui/screens/manage/employees/payment/PaymentFormScreen.kt`

### Verification
- Add or edit a payment with all required monetary/period fields but **no** signature → save succeeds; reopen shows saved values; signature still empty until provided.
- `./gradlew assembleDebug` ✅

---

## BUG-EMP-03 — Employee payment: Finalize requires signature; finalized record not editable

### Report
- **Screens:** Add / edit employee payment.
- **Requirement:**
  - There must be a distinct **Finalize** (or equivalent) action **in addition to** ordinary save/draft behavior (**BUG-EMP-02**).
  - **Finalize** must **require** a captured **signature** before the payment can be committed as final; if signature is missing, block finalize with clear messaging.
  - Once a payment is **finalized**, it **cannot be edited** (read-only in UI and/or enforced in persistence layer as appropriate).

### Fix *(implemented)*
- **`employee_payments.is_finalized`** (Room schema **version 5**): no incremental migration yet — build phase uses **`fallbackToDestructiveMigration()`** / fresh DB; new installs get column via Room `onCreate`.
- **`PaymentFormScreen`:** **Save** keeps **`is_finalized = false`**; **Finalize** requires signature, sets **`is_finalized = true`**; finalized rows open as **View payment** (read-only fields, **Back** + **Print Voucher** only).
- **`EmployeePaymentRepository`:** **`updatePayment`** / **`deletePayment`** no-op when row is already finalized.
- **`PaymentCard`:** **Draft** vs **Finalized** label; view icon for finalized; **delete** hidden when finalized.
- **CSV export** includes **`IsFinalized`** column.

### Files
- `FarmDatabase.kt`, `EmployeePaymentEntity.kt`, `EmployeePayment.kt`, `EmployeePaymentRepository.kt`, `PaymentFormScreen.kt`, `PaymentCard.kt`, `CsvExportService.kt`, `DatabaseMigrationViewModel.kt`

### Verification
- Draft: save without signature OK (**BUG-EMP-02**). Finalize without signature → blocked. Finalize with signature → success; list shows **Finalized**; open → read-only, no save/finalize/delete.
- `./gradlew assembleDebug` ✅

---

## BUG-ACQ-02 — Add acquisition: price/unit optional when total is given

### Report
- **Screen:** Inventory (Acquire Produce) → **Add** / **Edit acquisition** dialog.
- **Requirement:** **Price per unit** should be **optional** when the user provides **total amount** (and quantity). In that case, **price per unit** is computed as **`total_amount / quantity`** (respect per-kg vs per-piece mode as today).
- **Preset / SRP pricing:** Unchanged — preset-driven SRP preview and stored preset snapshot behaviour stay as they are; this rule is only about how **cost** fields (quantity, unit price, total) relate on save.

### Fix *(implemented)*
- **`resolveAcquisitionQuantityPriceTotal`:** If **total** is positive, use **`price_per_unit = total ÷ quantity`** (total wins). Else if **price/unit** is positive, **`total = quantity × price`** (existing flow).
- **`AcquisitionDialog`:** Save enabled when quantity + (total **or** price) resolve; save passes resolved **`q` / `ppu` / `total`** to **`Acquisition`**. SRP **`LaunchedEffect`** uses the same resolution (preset logic unchanged).
- **Numeric pad:** Changing **total** fills **price** when quantity set; changing **quantity** recomputes **price** when total set, else recomputes **total** when price set.
- **Copy:** Price field **supportingText** + SRP empty-state hint updated.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/acquire/AcquireProduceScreen.kt` (`AcquisitionDialog` + helper)

### Verification
- Enter quantity + total only → save succeeds; stored **`price_per_unit`** matches total ÷ quantity; reopen/edit shows consistent numbers.
- Enter quantity + unit price (existing flow) → unchanged.
- Preset / SRP path unchanged vs current app.
- `./gradlew assembleDebug` ✅

---

## BUG-ACQ-03 — Add/Edit acquisition dialog: one field per row (except date + location)

### Report
- **Screen:** Inventory (Acquire Produce) → **Add** / **Edit acquisition** dialog (`AcquisitionDialog` in `AcquireProduceScreen.kt`).
- **Requirement:** Lay out **each input on its own row** (full width) for readability and touch targets on handheld POS — **quantity**, **price/unit**, **total**, **unit switch**, **pieces per kg** (when shown), **product** card, **SRP preview** block, etc.
- **Exception:** Keep **date** and **location** in the **same row** as today (side-by-side).

### Fix *(implemented)*
- **Quantity** and **Price/Unit** are separate **full-width** `OutlinedTextField`s (no shared `Row`).
- **Date + location** unchanged in one `Row`.
- **Unit switch** row uses **`fillMaxWidth()`** + **`SpaceBetween`** for a clear full-width control row.
- Dialog body **`heightIn(max = 600.dp)`** (was 520) to fit the taller stack.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/acquire/AcquireProduceScreen.kt` (`AcquisitionDialog`)

### Verification
- Add acquisition: each cost field occupies a full line; date + location still share one row; save and numeric pad unchanged.
- `./gradlew assembleDebug` ✅

---

## BUG-ACQ-04 — Acquire Produce list: SRP detail expander is hard to scan

### Report
- **Screen:** **Acquire Produce** → acquisition **list cards** (`AcquireProduceScreen` → `AcquisitionCard`).
- **Area:** Expandable **“All channel SRPs”** block (when `hasSrpDetail()`).
- **Symptom / UX:** Per-channel **per-kg** lines are readable, but **pack tiers** are three long single-line strings (**Online / Reseller / Store packs:** `500g … · 250g … · 100g …`). On narrow screens this is **dense**, **wraps awkwardly**, and makes it easy to **lose track of which channel** a number belongs to or to **confuse pack sizes** at a glance. Optional **per-piece** line packs three channels into one string with the same issue.

### Fix *(implemented)*
- **`AcquisitionSrpExpandedDetail`:** Renders **only channels that have data** (per-kg, any pack tier, or per-piece).
- **`AcquisitionSrpChannelBlock`:** Each channel in a **`Surface`** (`surfaceContainerHighest`, rounded) with a **title** (Online / Reseller / Store).
- **Per kg** on its own **labeled row** (`SpaceBetween` label vs price).
- **Packs** subsection with a small **“Packs”** caption and **three rows** (`500 g`, `250 g`, `100 g`) — each price **right-aligned**; missing tier shows **—**.
- **Per piece** appears **inside that channel’s block** (not one combined string for all channels).

### Files
- `app/src/main/java/com/redn/farm/ui/screens/acquire/AcquireProduceScreen.kt` (`AcquisitionCard`, `AcquisitionSrpExpandedDetail`, helpers)

### Verification
- Expand **All channel SRPs** on a narrow device: each price is tied to a **channel** and **tier** at a glance.
- `./gradlew assembleDebug` ✅

---

## BUG-ACQ-05 — Pieces per kg must allow decimals (not integer-only)

### Report
- **Screen:** **Add / Edit acquisition** (`AcquisitionFormScreen`).
- **Field:** **Pieces per kg** (shown when unit is **per piece**).
- **Expected:** Assistants can enter a **fractional** pieces-per-kg rate (e.g. **3.5**, **4.25**) when irregular sizing makes an integer misleading.
- **Observed:** Input is parsed with **`toIntOrNull()`** and the DB column is **`INTEGER`**, so only whole numbers persist; decimal entry is rejected or truncated.

### Root cause
- **`AcquisitionFormScreen`:** `pieceCountStr.toIntOrNull()` for preview and save; numeric pad / validation aligned to integers.
- **`Acquisition` / `AcquisitionEntity`:** `piece_count: Int?`.
- **`SrpCalculator` / `PricingChannelEngine`:** `pieceCount` / per-piece divisor typed as **`Int`** (`bulkQuantityKg`, `perPieceSrp`).
- **Products:** `default_piece_count` is also **`Int?`** — pre-fill from catalog shares the same limitation.

### Fix *(implemented)*
- UI: `AcquisitionFormScreen` now accepts decimal input in **Pieces per kg** (validated against a `digits(.digits)?` pattern) and parses with **`toDoubleOrNull()`** for both SRP preview and save.
- Domain/DB: promoted `piece_count` from **`Int?`** to **`Double?`** in `Acquisition` + `AcquisitionEntity`.
- Math: updated `SrpCalculator` and `PricingChannelEngine` so `pieceCount` is **`Double?`** and per-piece SRPs compute using **`SRP_piece = ceil(SRP / n)`** with **`n`** as a `Double`.
- Tests: added unit coverage for decimal `pieceCount` in `SrpCalculatorTest`.

### Files *(expected touch list)*
- `AcquisitionFormScreen.kt`, `Acquisition.kt`, `AcquisitionEntity.kt`, `AcquisitionDao` / **`FarmDatabase`** migration
- `SrpCalculator.kt`, `PricingChannelEngine.kt`, `AcquisitionRepository.kt`
- `Product.kt` / `ProductEntity` / product form if **`default_piece_count`** becomes fractional
- `docs/apis.md`, `docs/schema_evolution.sql`, optional **PricingReference** note if “positive integer” for `pieceCount` is relaxed

### Verification
- Enter **3.5** pieces/kg, **100** pieces → derived kg and SRP preview match spreadsheet; save/reload round-trip.
- `./gradlew testDebugUnitTest` (at least **`SrpCalculatorTest`**).

**Status:** `[x]`

---

## BUG-PRD-03 — Product form: after save, return to Manage Products

### Report
- **Screen:** Manage Products → **Add product** / **Edit product** (`ProductFormScreen`).
- **Expected:** After a successful **save** (add or edit), the app should return to the **Manage Products** list screen (same destination as tapping back from the form).
- **Observed (if applicable):** User can remain on the form until a success **Snackbar** finishes, because `SnackbarHostState.showSnackbar()` is **suspend** and runs **before** `navigateUp()` in the same coroutine — so navigation is delayed until the snackbar is dismissed or times out.

### Fix *(implemented)*
- On success: call **`onNavigateBack()`** immediately after `insertProduct` / `updateProduct` (do not **await** a success snackbar on the form first). Optional feedback on the list can be added later via a shared `ViewModel` message if product wants a toast after pop.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/manage/products/ProductFormScreen.kt`

### Verification
- Add product → Save → lands on **Manage Products** without waiting on an on-form snackbar.
- Edit product → Save → same.
- `./gradlew assembleDebug` ✅

---

## BUG-PRC-02 — Preset editor: after save, go to Preset History

### Report
- **Screen:** Settings → **Pricing presets** → **New preset** / **Preset editor** (`PricingPresetEditorScreen`).
- **Expected:** After a successful **save** (new inactive preset), the app should open **Preset History** so the user sees the saved preset in the list.
- **Observed (prior fix):** User stayed on the editor; success feedback used **`SnackbarHostState.showSnackbar()`** in a `LaunchedEffect(saveMessage)`, which also delayed any follow-up navigation if tied to the same flow.

### Fix *(implemented)*
- On success: **`clearSaveMessage()`** then **`onSaveSuccessNavigateToPresetHistory()`** (no success snackbar on the editor — avoids suspend-before-navigate and matches “land on history”).
- **`NavGraph`:** `navigate(Screen.PresetHistory.route) { popUpTo(Screen.PricingPresetsHome.route) { inclusive = false }; launchSingleTop = true }` so the back stack returns to **Pricing presets home** under history (back from history → home).

### Files
- `app/src/main/java/com/redn/farm/ui/screens/pricing/PricingPresetEditorScreen.kt`
- `app/src/main/java/com/redn/farm/navigation/NavGraph.kt`

### Verification
- Create/save a new preset from the editor → lands on **Preset History**.
- Errors still show a snackbar on the editor (validation / save failure).
- `./gradlew assembleDebug` ✅

---

## BUG-ACQ-06 — Per-piece SRP must be used for per-piece orders

### Report
- **Screen:** Acquire Produce → Add/Edit acquisition (`AcquisitionFormScreen`) and Take Order (`TakeOrder` flow).
- **Symptom / UX mismatch:**
  - When **Unit is Per Piece** on acquisition (unit switch = `is_per_kg = false`), the app must calculate and persist **per-channel per-piece SRPs**.
  - When later taking an order and choosing **Per Piece** for the product line (unit switch = `isPerKg = false`), the **order unit price must come from the per-piece SRP** for the active acquisition/channel, not from the per-kg SRP.

### Expected
1. Acquisition save persists **`srp_{online,reseller,offline}_per_piece`** computed for each channel from the per-piece pipeline.
2. Order pricing (unit switch = per piece) uses:
   - `srp_online_per_piece` / `srp_reseller_per_piece` / `srp_offline_per_piece`
   - and never substitutes `*_per_kg` values when `isPerKg = false`.
3. Channel-specific selection must be respected (online vs reseller vs offline).

### Observed
- In some flows, the SRP used for **per-piece** order lines appears to be derived from the **per-kg** SRPs (or the wrong acquisition SRP columns), resulting in incorrect unit prices.

### Root cause (hypothesis to confirm during fix)
- Either:
  - Acquisition save is not persisting the per-piece SRP columns correctly when `is_per_kg = false`, or
  - Order pricing is selecting the wrong acquisition SRP fields when `isPerKg = false`, or
  - Per-piece SRPs are computed but not wired into the order unit price resolver.

### Root cause (actual)
- `OrderPricingResolver` and `AcquisitionRepository` were correct all along — per-piece SRPs are stored and resolved properly for orders.
- The display surfaces and printout always read `srp_*_per_kg` unconditionally:
  - `ThermalPrintBuilders.buildAcquisitionReceivingSlip()` hardcoded `/kg` SRP regardless of `is_per_kg`.
  - `AcquireProduceScreen.AcquisitionSrpChannelBlock()` had no `isPerKg` param — showed per-kg row for all acquisitions (pipeline stores per-kg as an intermediate even for per-piece lots).
  - `ActiveSrpsScreen.ActiveSrpsChannelBlock()` — same problem.

### Fix
- `ThermalPrintBuilders.kt`: branch on `acquisition.is_per_kg`; per-piece path reads `srp_*_per_piece` (fallback derives via `PricingChannelEngine.perPieceSrp`), labels `/pc`.
- `AcquireProduceScreen.kt`: added `isPerKg: Boolean` to `AcquisitionSrpChannelBlock`; per-kg and pack rows hidden when `isPerKg = false`.
- `ActiveSrpsScreen.kt`: added `isPerKg: Boolean` to `ActiveSrpsChannelBlock`; same per-kg/pack suppression.

### Files
- `app/src/main/java/com/redn/farm/utils/ThermalPrintBuilders.kt`
- `app/src/main/java/com/redn/farm/ui/screens/acquire/AcquireProduceScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/ActiveSrpsScreen.kt`

### Verification
1. Add an acquisition for product `X` with `is_per_kg = false`, set `piece_count` and enter a quantity/total.
2. Acquire list → SRP expander shows **Per piece** only (no per-kg row, no pack rows).
3. Active SRPs screen → same: only per-piece row per channel.
4. Print acquisition receiving slip → SRP lines read `X/pc`, not `X/kg`.
5. In Take Order, select the same product, toggle **Per piece** → unit price matches stored per-piece SRP.

**Status:** `[x]`

---

## BUG-ACQ-07 — SrpCalculator applies spoilage to per-piece acquisitions (CLARIF-01)

### Report
- **Scope:** `SrpCalculator.compute()` — affects every per-piece acquisition save, the live SRP preview in `AcquisitionFormScreen`, every stored `srp_*_per_piece` / `srp_*_per_kg` value for per-piece lots, and all downstream displays and prints.
- **CLARIF-01 rule:** *"for item sold per piece, [spoilage] is 0. Is not part of the SRP calculations."*

### Expected (CLARIF-01 per-piece formula)
```
A   = bulk_purchase_cost / total_quantity      (cost per piece)
B   = additional_cost_per_kg / piece_count     (hauling per piece)
SRP = (A + B) × (1 + channel_markup)
```
No spoilage deduction anywhere in the per-piece path.

### Observed
`SrpCalculator.compute()` always applies `spoilageRate` regardless of `pieceCount`:
```kotlin
val sellable = input.bulkQuantityKg * (1.0 - input.spoilageRate)
// for per-piece: bulkQuantityKg = quantity / pieceCount
// sellable = (quantity / pieceCount) × (1 − 0.25) → 25% spoilage applied
val c = input.bulkCost / sellable   // inflated by ÷0.75 instead of ÷1.0
```
With the default 25% spoilage preset, per-piece SRPs are **~33% higher** than they should be.

### Root cause
`SrpCalculator.Input` has no `isPerKg` field; `compute()` cannot distinguish acquisition type and blindly uses the preset `spoilageRate` for all inputs.

### Fix
In `SrpCalculator.compute()`, derive `effectiveSpoilage` before computing `sellable`:
```kotlin
val effectiveSpoilage = if (input.pieceCount != null && input.pieceCount > 0.0) 0.0 else input.spoilageRate
val sellable = input.bulkQuantityKg * (1.0 - effectiveSpoilage)
```
Propagate `effectiveSpoilage` into `Output.spoilageRate` so the form preview shows "spoilage 0%" for per-piece.

### Impact on existing data
All per-piece acquisitions already in the DB have inflated SRPs. They will need to be re-saved (or re-computed via a migration helper) after the fix. New acquisitions will be correct immediately.

### Files
- `app/src/main/java/com/redn/farm/data/pricing/SrpCalculator.kt` — `compute()` + `Output.spoilageRate`

### Verification
1. Add a per-piece acquisition: 100 pcs, total ₱500, piece_count 10 pcs/kg, preset 25% spoilage, 35% online markup, additionalCostPerKg ≈ 10.29.
2. Expected SRP (online):
   - A = 500 / 100 = 5/pc; B = 10.29 / 10 = 1.029/pc; SRP = (5 + 1.029) × 1.35 = ₱8.14/pc
3. Before fix (wrong): sellable = (100/10) × 0.75 = 7.5 kg; c = 500/7.5 = 66.67/kg; SRP/kg = (66.67+10.29)×1.35 = ₱103.89/kg; SRP/pc = ceil(103.89/10) = ₱11/pc — **inflated**.
4. After fix (correct): SRP/pc = ₱8 or ₱9/pc depending on rounding.
5. Form preview shows "spoilage 0%" for per-piece acquisitions.

**Status:** `[x]`

### Fix *(implemented)*
- **`SrpCalculator.Input.isPerKgAcquisition`** (default `true`): when `false`, **`effectiveSpoilage = 0`**, **`Q_sell = Q`**, **`Output.spoilageRate`** reflects effective rate (0 for preview).
- **`AcquisitionRepository`:** passes **`isPerKgAcquisition = acquisition.is_per_kg`** / **`user.is_per_kg`** into **`compute()`**.
- **Tests:** `compute_perPieceAcquisition_skipsSpoilage`, `compute_perKgWithPieceCount_stillAppliesSpoilage`.

### Files
- `app/src/main/java/com/redn/farm/data/pricing/SrpCalculator.kt`
- `app/src/main/java/com/redn/farm/data/repository/AcquisitionRepository.kt`
- `app/src/test/java/com/redn/farm/data/pricing/SrpCalculatorTest.kt`

---

## BUG-ACQ-08 — SRP price list print: per-kg column shown for per-piece products

### Report
- **Screen:** Active SRPs → Print (thermal, `buildSrpPriceList`)
- **CLARIF-01:** per-piece products have a customer price of **`srp_*_per_piece`** only; the `/kg` value is an intermediate calculation, not a customer-facing price.

### Observed
- `ThermalSrpPrintRow` has no `isPerKg` field.
- `buildSrpPriceList` column header is fixed: `"Product  /kg    /500g"`.
- For per-piece products: `r.perKg` (the intermediate kg SRP) prints in the `/kg` column as if it were the customer price; `r.per500g` (irrelevant pack size) also prints; the actual `/pc` price appears only as a secondary secondary row `"  /pc  ₱X.XX"`.
- Result: customer-facing price list is confusing/incorrect for per-piece items.

### Fix
1. Add `isPerKg: Boolean` to `ThermalSrpPrintRow`.
2. In `buildSrpPriceList`, adapt per-piece rows:
   - Print `—` for the `/kg` and `/500g` columns.
   - Promote the `/pc` line as the primary price on the same line as the product name (or clearly labelled).
   - Consider a `*` footnote or separate header when the list contains a mix of kg and per-piece products.
3. Update call site in `ActiveSrpsScreen.kt` to pass `isPerKg = acq.is_per_kg` when building `ThermalSrpPrintRow`.

### Files
- `app/src/main/java/com/redn/farm/utils/ThermalPrintBuilders.kt` — `ThermalSrpPrintRow` data class + `buildSrpPriceList()`
- `app/src/main/java/com/redn/farm/ui/screens/order/ActiveSrpsScreen.kt` — `thermalRows` mapping (lines ~71–95)

### Verification
- Active SRPs → Print for a channel that has both kg and per-piece products.
- Per-kg rows: show `/kg` and `/500g` values as before.
- Per-piece rows: show `—` in the `/kg` column; `/pc` price on the same or next line.
- `./gradlew assembleDebug` ✅

**Status:** `[x]`

### Fix *(implemented)*
- **`ThermalSrpPrintRow.isPerKg`**; **`buildSrpPriceList`** branches per row (per-kg + packs vs **`₱X /pc`** primary); mixed-list header `*` + footnote.
- **`ActiveSrpsScreen`:** `isPerKg = acq.is_per_kg` on thermal rows.

---

## BUG-ACQ-09 — Add/Edit acquisition: align **numpad** UX with **preset editor** (icon-only; no scroll misfire)

### Report
- **Screen:** **Acquire Produce** → **Add** / **Edit acquisition** (`AcquisitionFormScreen.kt` — full-screen form with scroll).
- **Today:** The shared **`NumericPadBottomSheet`** is opened when **`collectIsPressedAsState()`** goes true on **quantity**, **price/unit**, **total**, **spoilage**, **custom SRP** fields (and related **`LaunchedEffect`** wiring). That matches the older **BUG-ACQ-01** “press to open pad” fix but conflicts with the pattern used on **Pricing Preset Editor** and **`NumericPadOutlinedTextField`**: by default the pad opens **only from the dialpad trailing icon**, so **scrolling** past fields does not accidentally open the pad (**`docs/build_framework.md`** §6.3).
- **Desired:** Same behavior as preset editor — **explicit dialpad (or equivalent) opens the pad** on this scrollable acquisition form; remove or disable press-to-open on the field body unless intentionally opted in (e.g. `openPadOnFieldPress = true` only where justified).

### Expected
- Scrolling the add/edit acquisition form does not open the numeric pad unless the user taps the **dialpad** control (or a clearly separate affordance per field).
- Parity with **`PricingPresetEditorScreen`** / **`NumericPadOutlinedTextField`** defaults; refactor or duplicate trailing-icon pattern for **`AcquisitionNumericPadTarget`** fields as needed.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/acquire/AcquisitionFormScreen.kt` (primary)
- See **Related screens (audit)** for other call sites to align or follow up.

### Related screens (audit)
- **Reference (already icon-only default):** **`PricingPresetEditorScreen`**, **`RemittanceFormScreen`** — use **`NumericPadOutlinedTextField`** with **`openPadOnFieldPress = false`** (pad opens from **dialpad** only unless opted in).
- **Aligned in same fix:** **`ProductSelectionDialog.kt`** (Take Order quantity), **`EditOrderScreen.kt`** (**`EditOrderItemDialog`** + add-line dialog quantity), **`ManageProductsScreen.kt`** **`SetFallbackPriceSheet`** (dialpad icons; removed **`clickable`** on fields).

### Fix *(implemented)*
- Removed **`collectIsPressedAsState()`** / **`LaunchedEffect`** pad-open wiring from **`AcquisitionFormScreen`** (quantity, total, price/unit, spoilage) and from **`CustomSrpPadField`**; pad opens only via **dialpad** **`IconButton`**.
- Same for **Take Order** / **Edit order** quantity fields above.
- **Set fallback price** sheet: **`Modifier.clickable`** on read-only fields replaced with **trailing dialpad** + **`LocalFocusManager.clearFocus()`** on open.

### Verification
- Scroll up/down through the form without touching dialpad icons → pad stays closed.
- Tap dialpad on each numeric field → pad opens with correct title/target.
- `./gradlew :app:compileDebugKotlin` ✅ + manual QA on device.

**Status:** `[x]`

---

## BUG-RMT-01 — Remittance: add/edit as full-screen form (not `AlertDialog`)

### Report
- **Screen:** **Remittances** (`RemittanceScreen` / `RemittanceFormScreen`).
- **Was:** **Add** and **Edit** used a private **`RemittanceDialog`** as **`AlertDialog`** (amount, date, remarks, Save/Cancel).
- **Expected:** Add/edit on a **full-screen** route with **`Scaffold`**, scrollable body, **Save** / **Cancel** in **`bottomBar`**, consistent with **ProductFormScreen** and **`docs/user_review_screens_stories.md`**.

### Why
- Aligns navigation and layout with the rest of the app on small POS / Sunmi-class devices.
- Avoids cramming date + fields into a dialog; improves thumb reach (**URS-08**: amount uses shared numeric pad on the form).

### Fix *(implemented)*
- Route **`remittance_add_edit/{remittanceId}`** (`"new"` for add); **`Screen.RemittanceForm`** + **`composable`** in **`NavGraph.kt`**, **`viewModel(..., viewModelStoreOwner = remittance list entry)`** + **`RemittanceViewModel.Factory`** (same **`RemittanceViewModel`** as the list).
- **`RemittanceFormScreen.kt`:** amount (**`NumericPadOutlinedTextField`**), date (**`OutlinedCard` → `DatePickerDialog`** — see **BUG-RMT-02**), remarks, snackbars / save flow.
- **`RemittanceScreen.kt`:** list, search, print, delete confirm, navigate to form ( **`RemittanceDialog`** removed).

### Files
- `app/src/main/java/com/redn/farm/ui/screens/remittance/RemittanceScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/remittance/RemittanceFormScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/remittance/RemittanceViewModel.kt`
- `app/src/main/java/com/redn/farm/navigation/NavGraph.kt`

### Verification
- Add/edit remittance → full-screen form → save → list + **`Remittance saved`** snackbar; date UX per **BUG-RMT-02**.
- `./gradlew assembleDebug` ✅

---

## BUG-RMT-02 — Remittance form: date picker clipped; full calendar + OK

### Report
- **Screen:** **Remittances** → **Add/Edit remittance** (`RemittanceFormScreen`).
- **Symptom:** The embedded Material3 **`DatePicker`** did not show the **full** month grid (calendar looked cut off) inside the scrollable form; hard to confirm the chosen day on small / POS screens.
- **Desired UX:** Calendar should use the full dialog layout and include explicit **OK** (and **Cancel**) so the user confirms the date before returning to the form.

### Root cause
- **`DatePicker`** was inside a scroll column with **`Modifier.heightIn(max = 420.dp)`**, which **clipped** the composable so the full calendar was not visible.
- Inline **`DatePicker`** has no confirm affordance; **OK** matches **`AcquisitionFormScreen`** / **`PaymentFormScreen`**.

### Fix *(implemented)*
- **Summary `OutlinedCard`** (“Date” + formatted value); tap opens **`DatePickerDialog`** with **`DatePicker`** (`showModeToggle = false`) and **OK** / **Cancel**.
- Selected instant stored in **`selectedDateMillis`** for **Save** and edit preload.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/remittance/RemittanceFormScreen.kt`

### Verification
- Add remittance → tap **Date** → full calendar in dialog → **OK** updates summary and save uses that date.
- `./gradlew assembleDebug` ✅

---

## BUG-ORD-03 — Order history: date filter should match Acquire Produce date picker pattern

### Report
- **Screen:** **Order History** → filters (`OrderHistoryFilters`).
- **Current:** One **Date range** card opens a **`DatePickerDialog`** that hosts Material3 **`DateRangePicker`** (embedded range calendar).
- **Expected / preference:** The flow should **feel like Acquire Produce** when choosing dates: an **`OutlinedCard`** (or paired cards) showing the selected value(s), then **`DatePickerDialog`** with the standard **`DatePicker`** composable (`showModeToggle = false`) — the same interaction model as **`AcquisitionFormScreen`** (date summary card → OK/Cancel dialog with classic picker), not the range-specific embedded UI.

### Why
- **Consistent UX** across inventory and orders on small POS / Sunmi-class devices.
- Users already trained on **acquisition** date picking get the **same** dialog pattern in **order history** filters.

### Fix *(implemented — Option A)*
- **From** / **To** **`OutlinedCard`**s side by side; each opens **`DatePickerDialog` + `DatePicker`** (`showModeToggle = false`) with OK/Cancel — same pattern as **`AcquisitionFormScreen`**.
- **From** → start-of-day local time; **To** → end-of-day **23:59:59**; **`OrderHistoryViewModel`** filter semantics unchanged.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/order/history/OrderHistoryFilters.kt`

### Verification
- Picking dates matches acquisition-style dialogs; list filtering unchanged for equivalent ranges.
- `./gradlew :app:testDebugUnitTest` ✅

**Status:** `[x]`

---

## BUG-PRC-03 — CLARIF per-piece **B** (lot hauling) vs **(A + B) × markup** and **FR-PC-14**

### Report
- **Docs:** **`docs/pricing_clarif.md`** (CLARIF-01) defines **B** = (**total_quantity** / **Estimated Qty per Kg**) × **additional costs** = **\(Q \times A_{\text{spec}}\)** — a **lot-level** hauling amount (PHP), not **per piece**.
- **Bug:** The CLARIF line **SRP** = (**A** + **B**) × (1 + **channel markup**) mixed **per-piece A** with **lot B** (wrong dimensions for a per-piece SRP).

### Expected (documented in **PricingReference.md** §4.3.1, §5.1.1)
- **Per-piece** customer math: **(A + B / P_tot) × (1 + μ)** = **(A + A_spec / n) × (1 + μ)** before rounding — **haul per piece** = **B / P_tot** (CLARIF uses **total_quantity** for **P_tot**).
- **App:** **`SrpCalculator` / FR-PC-14** apply markup on **\(C_{\text{bulk}} + A_{\text{spec}}\)** **per kg**, then **ceil** per kg, then **per-piece** = **ceil(SRP_kg / n)** — **rounding order** may still differ slightly from a single combined **(A + A_spec/n) × (1+μ)** cell.

### Fix *(implemented)*
- **`docs/pricing_clarif.md`:** per-piece **SRP** line is **(A + B / total_quantity)** × (1 + **channel markup**), with a short note that **B** is the **lot** total and **B / total_quantity** = **additional costs** / **Estimated Qty per Kg**.
- **`docs/PricingReference.md`** §4.3.1 table **SRP per piece** row updated to match.

### Follow-up *(optional, not tracked)*
- Training spreadsheets: align any legacy **(A + B)** cell (with lot **B**) to **(A + B/P_tot)**.
- Optional UI: acquisition SRP preview row for **B_lot** / **B/P_tot**; spot-check vs **FR-PC-14** on sample lots.

### Files (reference)
- **`docs/pricing_clarif.md`**, **`docs/PricingReference.md`** §4.3.1 / §5.1.1, **`docs/USER_STORIES.md`** INV-US-05, **`SrpCalculator.kt`**, **`PricingChannelEngine.kt`**

**Status:** `[x]`

---

## BUG-PRC-04 — CLARIF **by-weight** spoilage: **rate** vs **absolute kg** (`pricing_clarif.md` line 10)

### Report
- **`docs/pricing_clarif.md` line 10** now states spoilage as **25% of weight of total_quantity** acquired **or** **customized to actual weight** (e.g. **2 kg**).
- **Farm app today:** **`SrpCalculator`** / presets effectively support **rate-only** \(Q_{\text{sell}} = Q \times (1 - s)\) for **per-kg** acquisitions. There is **no** first-class **absolute spoilage kg** path on preset or acquisition, and **US-8** / reporting text assumed rate-shaped sellable kg.

### Expected (documented)
- **PricingReference.md** v0.9.33-draft: **§4.3** sellable-weight row, **§4.3.1** CLARIF table, **US-8**, **FR-PC-10** — **either** rate **or** \(Q - s_{\text{kg}}\) for **by-weight** lines; **mutually exclusive** per revision unless an ADR says otherwise; **`derived.sellableQuantityKg`** remains the single sellable kg used downstream (**gross sales**, audits).
- **Per-piece** path unchanged: **\(Q_{\text{sell}} = Q\)**, no spoilage in SRP divisor (**§5.1.1**).

### Fix *(implemented)*
- **Product:** Preset keeps **rate** only; **per-acquisition** optional **`spoilage_kg`** (assistant enters unsellable kg for this **per-kg** lot). Empty → preset rate path unchanged.
- **Schema:** `acquisitions.spoilage_kg` (Room **v7**); **`fallbackToDestructiveMigration()`** on bump (dev).
- **Engine:** **`SrpCalculator.Input.spoilageKg`** → **\(Q_{\text{sell}} = Q - s_{\text{kg}}\)** when set and **`isPerKgAcquisition`**; **`Output.spoilageAbsoluteKg`**; validation **`0 \le s_{\text{kg}} < Q`**.
- **UI:** **`AcquisitionFormScreen`** — “Unsellable kg (optional)” + preview line; CSV export **`SpoilageKg`** column.
- **Tests:** **`SrpCalculatorTest`** — equivalence with **25%** rate vs **25 kg** on **100 kg**, reject **`s_{\text{kg}} \ge Q`**, per-piece ignores absolute.

### Files *(touched)*
- `SrpCalculator.kt`, `AcquisitionRepository.kt`, **`Acquisition.kt`**, **`AcquisitionEntity.kt`**, **`FarmDatabase.kt`** (v7), **`AcquisitionFormScreen.kt`**, **`CsvExportService.kt`**, **`DatabaseMigrationViewModel.kt`**, androidTest **`AcquisitionRepositorySnapshotInstrumentedTest.kt`**, **`SrpCalculatorTest.kt`**, **`INV_ACQUISITION_SRP_TRACKER.md`**, **`CLAUDE.md`**, schema **`7.json`**

**Status:** `[x]`

---

## BUG-ARC-01 — **Hilt-only ViewModels** (remove manual `ViewModelProvider.Factory`)

### Report
- **`docs/build_framework.md`** §15.1: v2 would use **Hilt everywhere from day 1** — no manual `ViewModelFactory`.
- **Today:** Several screens still use **`ViewModelProvider.Factory`** (`viewModelFactory { }` or hand-written **`class Factory`**), e.g. **`RemittanceViewModel`**, **`ManageEmployeesViewModel`**, **`LoginViewModel`**, **`TakeOrderViewModel`**, **`ExportViewModel`**, **`OrderHistoryViewModel`**, **`ActiveSrpsViewModel`**, **`ManageCustomersViewModel`**, **`EmployeePaymentViewModel`**, **`DatabaseMigrationViewModel`**.

### Expected
- All ViewModels constructed via **Hilt** (`@HiltViewModel` + constructor `@Inject`); Nav / composables use **`hiltViewModel()`** (or equivalent) without custom `Factory` types.

### Fix *(implemented)*
1. Repositories **`CustomerRepository`**, **`OrderRepository`**, **`EmployeeRepository`**, **`EmployeePaymentRepository`**, **`RemittanceRepository`**: **`@Singleton` `@Inject` constructors**; **`CsvExportService`**: **`@Singleton` `@Inject` (`@ApplicationContext`)**.
2. Removed duplicate **`@Provides`** for **`ProductRepository`** / **`FarmOperationRepository`** from **`RepositoryModule`** (constructors already **`@Inject`**).
3. Migrated ViewModels to **`@HiltViewModel`** + **`@Inject`**: **`LoginViewModel`**, **`TakeOrderViewModel`**, **`ActiveSrpsViewModel`**, **`OrderHistoryViewModel`**, **`ManageCustomersViewModel`**, **`ExportViewModel`**, **`EmployeePaymentViewModel`**, **`DatabaseMigrationViewModel`**, **`RemittanceViewModel`**, **`ManageEmployeesViewModel`** — deleted all manual **`Factory` / `viewModelFactory`**.
4. Composables use **`hiltViewModel()`**; **`NavGraph`** uses **`hiltViewModel(parentBackStackEntry)`** where list + form share state (**customers**, **remittances**, **employees**, **order history**, **employee payments**). **`LoginScreen`** + **`SessionChecker`** use **`hiltViewModel(activity)`** so login state is shared.
5. **`DatabaseMigrationViewModel`**: **`exportAllDataBeforeMigration()`** + injected **`CsvExportService`** (screen no longer constructs **`CsvExportService` manually).

### Verification
- `./gradlew :app:compileDebugKotlin` ✅
- `./gradlew :app:testDebugUnitTest` ✅

**Status:** `[x]`

---

## BUG-ARC-02 — Room / domain: **epoch millis** for persisted timestamps (reduce `LocalDateTime` + converters)

### Report
- **`docs/build_framework.md`** §15.2: v2 would use **epoch millis everywhere** — no `LocalDateTime` with Room type converters.
- **Today:** **`DateTimeConverter`** maps **`Long` ↔ `LocalDateTime`**; **`OrderEntity`**, **`CustomerEntity`**, **`ProductPriceEntity`**, etc. use **`LocalDateTime`**. Other tables (e.g. acquisitions) already use **`Long`**.

### Expected
- Single convention: **store `Long` (epoch millis)** in SQLite; use **`Instant` / `LocalDate`** only at UI boundary if needed.
- Remove or narrow **`DateTimeConverter`** after migration; **incremental migration** or destructive policy per **`BUG-ARC-04`**.

### Fix *(implemented)*
- **Room v8:** **`CustomerEntity`**, **`ProductPriceEntity`** — `date_created` / `date_updated` as **`Long`** (epoch millis); **`@TypeConverters(EnumConverters::class)`** only — **`DateTimeConverter`** removed and deleted.
- **Domain:** **`Customer`**, **`ProductPrice`** use **`Long`** millis; **`Order`** dead import removed.
- **Repositories / seed:** **`CustomerRepository`**, **`ProductRepository`**, **`DatabaseInitializer`** (asset JSON → millis); **`SampleDataGenerator`**; **`CustomerFormScreen`** preserves timestamps on edit; **`ManageProductsScreen`** price history formatting from millis.
- **Tests:** **`DateTimeConverterTest`** removed (converter gone). Dev DB: **`fallbackToDestructiveMigration()`** on bump.

### Verification
- `./gradlew :app:compileDebugKotlin` ✅
- `./gradlew :app:testDebugUnitTest` ✅

**Status:** `[x]`

---

## BUG-ARC-03 — **RBAC audit** (parity with `Rbac.kt` / `user_roles.md`)

### Report
- **`docs/build_framework.md`** §15.3: v2 would define **RBAC in Phase 0** — not bolted on late.
- **Today:** **`Rbac.kt`**, role-based nav, and user management exist, but **§15** warns of retrofit gaps — easy to miss a screen or action.

### Expected
- Every route / destructive or export action matches **`docs/user_roles.md`** and **`Rbac.kt`**; management PIN flows documented and consistent.

### Action *(open)*
1. Matrix: screen × role × allowed actions.
2. Fix any holes (navigation visibility vs backend guard).
3. Optional: instrumented or manual test script per role.

**Status:** `[ ]`

---

## BUG-ARC-04 — Room: **incremental migrations** beyond 2→3 (production strategy)

### Report
- **`docs/build_framework.md`** §15.4: plan incremental migrations from the start; reserve version numbers.
- **Today:** Only **`MIGRATION_1_2`**, **`MIGRATION_2_3`**; newer versions rely on **`fallbackToDestructiveMigration()`** (see **`FarmDatabase.kt`**, **`CLAUDE.md`**).

### Expected
- Documented policy: **dev** may stay destructive; **production** ships **additive migrations** + tests for each bump.

### Action *(open)*
1. ADR: when to stop destructive-only.
2. Implement incremental migrations for current schema; **`RoomDatabase` Migration** tests.

**Status:** `[ ]`

---

## BUG-ARC-05 — Dev / QA: **clean install** as default regression path (`dev.sh fresh`)

### Report
- **`docs/build_framework.md`** §15.5: **`dev.sh fresh`** should be the **default** mental model for testing — destructive migration makes stale DBs cause phantom bugs.

### Expected
- **`docs/BACKLOG.md`** / **`CLAUDE.md`** / CI notes explicitly say: schema or migration PRs **verify on fresh install**; optional wrapper or CI job runs **`./scripts/dev.sh fresh`** (or documented equivalent).

### Action *(open)*
1. Add “fresh install” step to PR template or **`scripts/dev.sh`** help text cross-link.
2. Optional: CI job that installs debug APK on emulator after **clear data** for critical paths.

**Status:** `[ ]`

---

## BUG-ARC-06 — **Central numeric pad + date picker** (one pattern; migrate stragglers)

### Report
- **`docs/build_framework.md`** §15.6: build shared components in **Phase 1** — not three divergent patterns later.
- **Today:** **`NumericPadBottomSheet`**, **`NumericPadOutlinedTextField`**, **`DatePicker.kt`** exist, but acquire / remittance / order history / etc. still differ in dialog vs sheet, `DatePickerDialog` wiring, `imePadding` rules.

### Expected
- Thin **standard composables** (or documented pattern) for “money/qty field + pad” and “date field + full calendar + OK” — all feature screens conform or justify exception.

### Action *(open)*
1. Inventory screens; list deltas vs canonical pattern (**`AcquisitionFormScreen`** / **`RemittanceFormScreen`** references).
2. Refactor to shared APIs; delete duplicate behavior.

**Status:** `[ ]`

---

## BUG-ARC-07 — **Formula / pricing: tests gate** (TDD or merge checklist)

### Report
- **`docs/build_framework.md`** §15.7: **formula tests before wiring** — do not assume spec match without tests.

### Expected
- **`SrpCalculator`** (and related) changes require **unit tests** in the same PR; optional CI **`testDebugUnitTest`** required on pricing paths.

### Action *(open)*
1. Document in **`CLAUDE.md`** or contributor note: pricing PRs must touch **`SrpCalculatorTest`** (or sibling) + assert spec IDs.
2. Optional: CI fail if `data/pricing/` changes without test file touch (heuristic).

**Status:** `[ ]`

---

## BUG-ARC-08 — **`is_per_kg` / unit semantics** on **every** price display and print

### Report
- **`docs/build_framework.md`** §15.8: **`is_per_kg` check in every price display** — code review checklist, not a late bug (**BUG-ACQ-06/08** class issues).

### Expected
- Audit **list cards, Take Order, Active SRPs, thermal print, exports** — any **`/kg` vs `/pc`** or channel SRP must branch on acquisition **per-kg vs per-piece** (or product unit) consistently.

### Action *(open)*
1. Grep-driven audit + manual pass on Sunmi-class layout.
2. Add **`CLAUDE.md` PR checklist** line; fix any new gaps found.

**Status:** `[ ]`

---

## BUG-ARC-09 — **Epoch millis everywhere** ( **`docs/build_framework.md`** §15.2 / line 519 )

### Report
- **`docs/build_framework.md`** §15 item **2:** *“Epoch millis everywhere — no `LocalDateTime` with converters”* — v2 convention for all persisted times and domain models.
- **After BUG-ARC-02:** **`CustomerEntity` / `ProductPriceEntity`** and **`DateTimeConverter`** removal addressed part of Room; **many** code paths still center on **`java.time.LocalDateTime`** for domain, filters, migrations, and formatting pipelines (see grep under **Files**).

### Expected
- **SQLite + domain:** **`Long` (epoch millis)** (or **`Instant`** in Kotlin if team standardizes) for every stored event time and model field that represents one.
- **UI / export filenames:** may convert at the boundary only; no **Room `TypeConverter`** from **`LocalDateTime`** reintroduced without review.
- Aligns **`FarmOperationEntity`** (already **`Long`**) with **`FarmOperation`** domain, **`OrderHistory` / acquire / farm-ops** date filters, **`DatabaseMigration`**, and **`ThermalPrintBuilders`** date handling. Unused **`data/local/util/Converters.kt`** removed (**DI-04**).

### Done
- **`FarmOperation`** domain + **`FarmOperationRepository`** use **`Long`** for **`operation_date`**, **`date_created`**, **`date_updated`**; DAO queries use millis ranges.
- **`MillisDateRange`** (`utils/MillisDateRange.kt`) centralizes calendar-day **`Pair<Long?, Long?>`** filter logic; unit tests in **`MillisDateRangeTest`**.
- **Order history**, **acquire** (filters + form **`selectedDayMillis`**), **farm ops** screens/VMs: filter state and persistence paths use epoch millis; **`EditOrderScreen`** updates order date via millis.
- **`ThermalPrintBuilders`**, **CSV / DB export** filename timestamps: **`Instant`** + zoned formatters (no **`LocalDateTime.now()`**).
- Remaining **`LocalDateTime`** is acceptable at UI/format helpers only; further greps can tighten other screens if needed.

### Files *(touched in fix)*
- `app/src/main/java/com/redn/farm/data/model/FarmOperation.kt`
- `app/src/main/java/com/redn/farm/data/repository/FarmOperationRepository.kt`
- `app/src/main/java/com/redn/farm/utils/MillisDateRange.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/OrderHistoryViewModel.kt`, `OrderHistoryFilters.kt`, `EditOrderScreen.kt`, …
- `app/src/main/java/com/redn/farm/ui/screens/acquire/AcquisitionFormScreen.kt`, `AcquireProduceViewModel.kt`, `AcquireProduceScreen.kt`, …
- `app/src/main/java/com/redn/farm/ui/screens/farmops/*`, `DatabaseMigrationViewModel.kt`, `utils/ThermalPrintBuilders.kt`, export services / **`ExportViewModel`**

### Verification
- **`./gradlew :app:compileDebugKotlin`** + **`./gradlew :app:testDebugUnitTest`** (includes **`MillisDateRangeTest`**).

**Status:** `[x]`

---

## BUG-FOP-01 — **Farm Operations → Log Operation:** operation type as **dropdown** (not chip grid)

### Report
- **Screen:** **Farm Operations** → **Log Operation** (add/edit farm operation form).
- **Current UX:** **`FarmOperationFormScreen`** lays out **`FarmOperationType`** as multiple rows of **`FilterChip`** (three chips per row). This uses a lot of vertical space and reads more like a filter than a single field selection.
- **Desired UX:** One **simple dropdown** (e.g. **`ExposedDropdownMenuBox`** / Material3 **menu**) listing all operation types with the same human-readable labels as today (**`FarmOperationType.toString()`**).

### Expected
- Single compact control: tap → list of all types → pick one; selected value visible when closed.
- No behavior change to **`FarmOperation`** / save logic beyond binding **`selectedType`** the same way as today.

### Fix
- Replaced **`FilterChip`** grid with **`ExposedDropdownMenuBox`** + read-only **`OutlinedTextField`** + **`ExposedDropdownMenu`** (same pattern as **`FarmOperationFilters`** / **`PricingPresetEditorScreen`**). Labels remain **`FarmOperationType.toString()`**.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/farmops/FarmOperationFormScreen.kt`

### Verification
- `./gradlew :app:compileDebugKotlin` ✅
- Manual: open **Log Operation**, choose each type from the dropdown, save, confirm history/card shows correct type.

**Status:** `[x]`

---

## BUG-FOP-02 — **Log Operation:** **Related product** picker shows **no products** (empty list / “dropdown”)

### Report
- **Screen:** **Farm Operations** → **Log Operation** / **Edit operation** (`FarmOperationFormScreen.kt`).
- **Area:** Expand **Related product (optional)** → tap the card to open the **Select product** bottom sheet (search + list — users often describe this as a dropdown).
- **Symptom:** The product list is **empty** (no rows), so a related product cannot be chosen even when the app has products elsewhere (e.g. **Manage Products**, **Take Order**).

### Expected
- The sheet lists **all products** (or the same set other screens use from **`ProductRepository`**), with search filtering as implemented.
- If there are truly no products, show an explicit **empty state** message instead of a blank list.

### Root cause
- **`products`** used **`stateIn(..., SharingStarted.WhileSubscribed(5000))`** while **`FarmOperationsScreen`** never **`collect`**s **`products`** (only **`operations`**). After 5s with no subscribers the upstream collection **stopped**; restarting on the form was **unreliable**, so the sheet often saw only **`initialValue` `emptyList()`**.

### Fix *(implemented)*
- **`FarmOperationsViewModel.products`:** Replaced **`stateIn`** with **`MutableStateFlow`** + **`init { collect(getAllProducts()) }`** so the product list always tracks Room.
- **`refreshProductListFromDb()`:** Called when the related-product picker opens (**`LaunchedEffect(showProductSheet)`**) to **`first()`** the Flow and avoid stale/empty UI (**persisting FOP-02** follow-up).
- **`FarmOperationFormScreen`:** Product UI moved from **`ModalBottomSheet`** to **`Dialog` + `Surface`** (same class of layering as numeric pad) so the list isn’t an empty/glitched sheet; empty-state copy retained.
- **`NavGraph`:** **`FarmOperationForm`** VM owner falls back to the form **`NavBackStackEntry`** if **`farm_ops`** is not on the stack.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/farmops/FarmOperationsViewModel.kt`
- `app/src/main/java/com/redn/farm/ui/screens/farmops/FarmOperationFormScreen.kt`
- `app/src/main/java/com/redn/farm/navigation/NavGraph.kt`

### Verification
- With at least one product in DB: open **Log operation** → **Related product** → sheet shows products; select one → save → history/card shows product name.
- `./gradlew :app:compileDebugKotlin` ✅

**Status:** `[x]`

---

## BUG-FOP-03 — **Log Operation:** **Weather** as **dropdown** (**Hot/Dry**, **Rainy**, **Cloudy**) not free text

### Report
- **Screen:** **Farm Operations** → **Log operation** / **Edit operation** (`FarmOperationFormScreen.kt`).
- **Today:** **Weather** is a single-line **`OutlinedTextField`** (free text).
- **Desired UX:** A **dropdown** (e.g. **`ExposedDropdownMenuBox`**, same pattern as **operation type**) with exactly these labels:
  1. **Hot/Dry**
  2. **Rainy**
  3. **Cloudy**
- Optional: fourth choice **Other** + short text only if product needs it later — **not** in scope unless stakeholders ask; default is the three fixed values only.

### Expected
- User picks one of the three options; stored **`weather_condition`** (or equivalent **`FarmOperation` / entity** field) is that string (e.g. for print/history cards).
- **Edit** pre-selects the matching option when the saved string equals one of the three; legacy/custom saved text may map to a sensible default or first item (decide in implementation).

### Fix (implemented)
- **`FarmOperationFormScreen`:** **Weather** uses **`ExposedDropdownMenuBox`** with fixed options **Hot/Dry**, **Rainy**, **Cloudy**; new operations default to **Hot/Dry**. **`normalizeFarmOpWeather`** maps legacy free text (keywords **rain** / **cloud** / **hot**/**dry**) or defaults to **Hot/Dry**.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/farmops/FarmOperationFormScreen.kt` (primary)
- Display surfaces if needed: `FarmOperationCard.kt`, `FarmOperationHistoryScreen.kt`, `ThermalPrintBuilders.kt` / `buildFarmOperationLog` (verify weather line still reads well)

### Verification
- Log + edit operation: choose each weather value, save, reopen — field shows selection; list/card/print show expected text.
- `./gradlew :app:compileDebugKotlin`

**Status:** `[x]`

---

## BUG-FOP-04 — **Log Operation:** **Personnel** pre-filled with **logged-in user**

### Report
- **Screen:** **Farm Operations** → **Log operation** (`FarmOperationFormScreen.kt` — new operation; clarify behavior for **edit** below).
- **Today:** **Personnel** is an empty **`OutlinedTextField`** until the user types.
- **Desired UX:** For **new** operations, pre-populate **Personnel** with the current session user (**login username**), e.g. from **`SessionManager.getUsername()`** (same source as auth; see **`SessionManager.kt`**).
- **Edit existing operation:** Keep **stored** **`personnel`** from the record; do **not** overwrite with the current user unless the field was blank (product decision: default = preserve DB on edit).

### Expected
- **Add / Log operation:** Opening the form shows the logged-in username in **Personnel**; user may still edit or clear before save.
- **`FarmOperation.personnel`** persists whatever is shown on save (unchanged rule).

### Files
- `app/src/main/java/com/redn/farm/ui/screens/farmops/FarmOperationFormScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/farmops/FarmOperationsViewModel.kt`

### Fix *(implemented)*
- **`FarmOperationsViewModel.loggedInUsernameOrEmpty()`** — **`sessionManager.getUsername().orEmpty()`**.
- **`FarmOperationFormScreen`:** **`personnel`** **`mutableStateOf`** for **`operationIdKey == "new"`** uses that value; **edit** still starts **`""`** and **`LaunchedEffect(existing, …)`** sets **`personnel = op.personnel`**.

### Verification
- Log in as **user A** → **Log operation** → **Personnel** contains **user A**’s username (or display convention if you normalize case).
- Edit an operation saved with different personnel → field still shows saved value.
- `./gradlew :app:compileDebugKotlin` ✅

**Status:** `[x]`

---

## BUG-ORD-04 — **Order SRP** vs **Active SRPs thermal list**; receipts missing **`/kg` / `/pc`**

### Report
- **Screens / outputs:** Take Order → placed order lines; **Order detail** / **Edit order**; **Print receipt**; **Active SRPs → Print price list**.
- **Symptom:** Unit **SRP** on the printed price list or on receipts **did not match** the amount used when taking the order (or looked ambiguous: amount with no **per kg** vs **per piece** label).

### Root cause
1. **Thermal price list (`ActiveSrpsScreen.printPriceList`)** filled **`ThermalSrpPrintRow.perPiece`** from **raw** `srp_*_per_piece` columns only. **Orders** use **`OrderPricingResolver.srpFromAcquisition`**, which **derives** per-piece from **`srp_*_per_kg` + `piece_count`** when `*_per_piece` is **null** (same rule as the **expanded** Active SRPs channel blocks). So the **printed list** could show **wrong or missing `/pc`** while the cart and **`OrderItem.price_per_unit`** were correct.
2. **`buildOrderReceiptText`** (and some order UI lines) showed **`price_per_unit`** without an explicit **`/kg` or `/pc`** suffix, so staff could misread the number next to **kg** vs **pc** quantity.

### Fix
- **Print price list:** For the selected sales channel, set **per kg** and **per piece** via **`OrderPricingResolver.srpFromAcquisition(acq, channel, isPerKg = true/false)`**; keep pack tiers (**500 g**, etc.) from acquisition columns.
- **Receipt + order UI:** Append **`/kg`** or **`/pc`** after formatted unit price where the line shows **`price_per_unit`** (`buildOrderReceiptText`, **`OrderDetailScreen`**, **`EditOrderScreen`** row + legacy print block).

### Files
- `app/src/main/java/com/redn/farm/ui/screens/order/ActiveSrpsScreen.kt`
- `app/src/main/java/com/redn/farm/utils/ThermalPrintBuilders.kt` (`buildOrderReceiptText`)
- `app/src/main/java/com/redn/farm/ui/screens/order/history/OrderDetailScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/EditOrderScreen.kt`

### Verification
- `./gradlew :app:compileDebugKotlin` ✅  
- `./gradlew :app:testDebugUnitTest --tests "com.redn.farm.data.pricing.OrderPricingResolverTest"` ✅  
- Manual: compare **same channel** on Active SRPs print vs a new order line (per-piece derived-from-kg case).

**Status:** `[x]`

---

## BUG-ORD-05 — **Active SRPs:** channel chips filter **print** only; **on-screen** list still **all channels**

### Report
- **Screen:** Take Order → **Active SRPs** (`ActiveSrpsScreen`)
- **Symptom:** Choosing **Online / Reseller / Offline** and tapping **Print price list** prints prices for that channel correctly. The **UI** (collapsed row subtitle and **expanded** details) still shows **every** channel’s SRPs, so the filter feels broken compared to print.

### Root cause
- **`selectedChannel`** was only used when building **`printPriceList`** thermal content.
- **Collapsed** subtitle used **`ProductActiveSrpRow.summaryFromPerKg`** (**`minPerKgSrpAcrossChannels`**), not the selected channel.
- **Expanded** content was **`ActiveSrpsAllChannelsDetail`** (all channel blocks).

### Fix
- **`ActiveSrpsCollapsedSummary`:** **`OrderPricingResolver.srpFromAcquisition`** for **`selectedChannel`** — per-kg and/or per-piece label line (piece-primary when `!acquisition.is_per_kg`).
- **`ActiveSrpsSelectedChannelDetail`:** single **`ActiveSrpsChannelBlock`** for the chip channel; resolver-backed **`perPiece`**; empty state if that channel has no SRP columns on the acquisition.
- Helper copy: list, details, and print use the selected channel.
- **`ActiveSrpsViewModel`:** dropped **`summaryFromPerKg`**; list filter is **`acquisition != null`** so **piece-only** active SRPs (no per-kg column) still appear.

### Files
- `app/src/main/java/com/redn/farm/ui/screens/order/ActiveSrpsScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/ActiveSrpsViewModel.kt`

### Verification
- `./gradlew :app:compileDebugKotlin` ✅
- Manual: pick **Reseller** → collapsed and expanded show **only** reseller SRPs; print matches; repeat **Online** / **Offline**.

**Status:** `[x]`

---

## BUG-ORD-06 — Take / edit order: **per-piece** line used **per-kg** SRP when product `unit_type` disagreed with acquisition

### Report
- **Screens:** **Take Order** → Add product (`ProductSelectionDialog`); **Edit order** → add line (same pattern).
- **Symptom:** A product sold **per piece** (active acquisition recorded with **`is_per_kg = false`**) still got **per-kg** SRP (`srp_*_per_kg`) applied to the line when **`Product.unit_type`** was **`kg`** (or not **`piece`**), so **unit price** matched **₱/kg** instead of **₱/pc**.

### Root cause
- **`defaultIsPerKgForProduct`** only inspected **`Product.unit_type`**. It ignored the **active acquisition’s** costing basis (**`Acquisition.is_per_kg`**), which is what **INV-US-06** / **`OrderPricingResolver.srpFromAcquisition`** use to populate **per-kg vs per-piece** columns.
- **`resolveUnitPrice(..., isPerKg = true)`** reads **`srp_*_per_kg`**; with **`isPerKg = false`** it reads **per-piece** (or derives from **per-kg + piece_count**). Defaulting **`isPerKg`** from the product row alone could pick the **wrong branch** for the latest lot.

### Fix
- **`defaultIsPerKgForOrderLine(product, activeAcquisition?)`** in **`OrderUnitDefaults.kt`**: if an active acquisition exists, **`return activeAcquisition.is_per_kg`**; else keep the **`piece` / `pieces`** heuristic on **`Product.unit_type`**.
- **`TakeOrderViewModel.defaultIsPerKgForProductLine`** / **`OrderHistoryViewModel.defaultIsPerKgForProductLine`** wrap that helper.
- **`ProductSelectionDialog`** and **`EditOrderScreen`** product picker use **`defaultIsPerKgForProductLine`** when starting the add-line flow.

### Files
- `app/src/main/java/com/redn/farm/data/pricing/OrderUnitDefaults.kt` (new)
- `app/src/main/java/com/redn/farm/ui/screens/order/TakeOrderViewModel.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/ProductSelectionDialog.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/OrderHistoryViewModel.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/EditOrderScreen.kt`

### Verification
- `./gradlew :app:compileDebugKotlin` ✅  
- `./gradlew :app:testDebugUnitTest --tests "com.redn.farm.data.pricing.OrderUnitDefaultsTest"` ✅  
- Manual: product **`unit_type = kg`**, latest acquisition **per piece** with only **per-piece** SRPs filled → new line defaults **per piece** and **`OrderPricingResolver`** uses **pc** path.

**Status:** `[x]`

---

## BUG-ORD-07 — **Edit order:** no **kg / pc** toggle when **per-piece SRP** is only **derived** from per-kg + `piece_count`

### Report
- **Screens:** **Take Order** (cart row unit toggle, product picker); **Edit order** (add line / line editor).
- **Symptom:** On **Take Order**, staff can switch a line between **per kg** and **per piece** when the active acquisition has **per-kg SRPs** and **`piece_count`**, with **empty** **`srp_*_per_piece`** columns (resolver derives **₱/pc** via **`PricingChannelEngine.perPieceSrp`**). On **Edit order**, the same product may **not** show the unit toggle, so the line stays stuck on one basis even though both prices exist for pricing.

### Root cause
- **`TakeOrderViewModel.productSupportsDualUnit`** treats **per-piece** as present if **either** raw **`srp_*_per_piece`** **or** **derived** per-piece from **`srp_*_per_kg`** + **`piece_count`** (same idea as **`OrderPricingResolver.srpFromAcquisition`**).
- **`OrderHistoryViewModel.productSupportsDualUnit`** only checks **raw** **`srp_online_per_piece` / `srp_reseller_per_piece` / `srp_offline_per_piece`**. It does **not** fold in **derived** per-piece, so **`srpPc`** stays **false** when only kg columns + **`piece_count`** are set — **`showUnitToggle`** stays off in **`EditOrderScreen`**.

### Fix
- **`OrderPricingResolver.productSupportsDualUnit(productPrice, acquisition)`** — catalog **kg/pc** flags unchanged; acquisition side uses **`minPerKgSrpAcrossChannels`** and **`minPerPieceSrpAcrossChannels`** so **per-piece** matches **`srpFromAcquisition`** (including **derived** per-piece).
- **`TakeOrderViewModel`** / **`OrderHistoryViewModel`**: **`productSupportsDualUnit(Product)`** delegates to the resolver.

### Files
- `app/src/main/java/com/redn/farm/data/pricing/OrderPricingResolver.kt` — **`productSupportsDualUnit`**
- `app/src/main/java/com/redn/farm/ui/screens/order/TakeOrderViewModel.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/OrderHistoryViewModel.kt`
- `app/src/test/java/com/redn/farm/data/pricing/OrderPricingResolverTest.kt`

### Verification
- Active acquisition: **per-kg SRPs filled**, **`piece_count` > 0**, all **`srp_*_per_piece` null** → **Take Order** shows toggle; **Edit order** add-line / line row **also** shows toggle; switching unit reprices consistently with **`resolveUnitPrice` / `resolveOrderLinePrice`**.
- `./gradlew :app:compileDebugKotlin` ✅  
- `./gradlew :app:testDebugUnitTest --tests "com.redn.farm.data.pricing.OrderPricingResolverTest"` ✅

**Status:** `[x]`

---

## BUG-SYS-02 — **Logout** should land on **Login** in one step (no visible stepping through other screens)

### Report
- **Flow:** Main (or any path that triggers logout) → **Login**
- **Symptom:** After logout, the app **visibly moves through** intermediate destinations (back-stack pops or extra transitions) instead of going **straight** to the login screen.

### Expected
- **Single clear navigation** to **Login** with **no intermediate screen flashes**; back stack cleared so the user cannot “back” into the authenticated graph.

### Root cause
- **`NavGraph` `onLogout`** used **`popUpTo(0)`** together with a second navigation path; **`MainScreen`** called **`MainViewModel.logout()`** (session only) then **`onLogout()`**, while **`SessionChecker`** also reacted to **`LoginState`**, so **`LoginViewModel`** session/state could be out of sync or navigation could run twice.

### Fix
- **One logout path:** **`MainScreen`** calls activity-scoped **`LoginViewModel.logout()`** only (session + **`LoginState.Initial`**). Removed **`NavGraph` `onLogout`** and **`MainViewModel.logout()`**.
- **`SessionChecker`:** `navigate(Login) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }` so the **entire** **`NavHost`** back stack clears in one transaction (replaces **`popUpTo(0)`**).

### Files
- `app/src/main/java/com/redn/farm/ui/components/SessionChecker.kt`
- `app/src/main/java/com/redn/farm/navigation/NavGraph.kt`
- `app/src/main/java/com/redn/farm/ui/screens/main/MainScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/main/MainViewModel.kt`

### Verification
- `./gradlew :app:compileDebugKotlin` ✅  
- Manual: logout from **Main** → **Login** only; **`LoginState.Initial`** + deep routes: **`SessionChecker`** resets stack to Login; system back from Login does not return to app content.

**Status:** `[x]`

---

## BUG-PRT-01 — **Print:** snackbar says **“Print failed”** even when printing **succeeds**

### Report
- **Symptom:** After tapping **Print** (order receipt, acquisition batch, remittance, employee payment, Active SRPs list, farm ops log, etc.), a **Snackbar** shows **failure** text (e.g. **`Print failed`**, **`Print failed — check printer`**) even though the **physical printer produced the slip** or the **system print flow completed** as expected.
- **Impact:** Staff lose trust in on-screen feedback; they may reprint or assume hardware failure.

### Expected
- Snackbar (or toast) **matches outcome**: success only after a **reliable** success signal; failure only when the print path **actually** fails.
- If the platform is **asynchronous** (Sunmi callbacks, **`PrintManager`** job), UI should not treat **`Boolean`** from a **synchronous** wrapper as definitive until the **real** completion path is wired.

### Root cause
1. **`connectPrinter`:** **`InnerPrinterCallback.onDisconnected`** resumed the continuation with **`null`** before **`onConnected`**, so **`printMessage`** returned **`false`** even when the service connected moments later and printing worked.
2. **`printMessage`:** Exceptions from **`lineWrap`** / **`cutPaper`** after **`printText`** completed were treated as full failure (**`false`**), so the slip could print and the snackbar still said **failed**.
3. **`OrderHistoryScreen`:** **`getOrderSnapshotForPrint`** returning **`null`** showed **`Print failed`**, which is a **data** error, not a printer error.

### Fix *(implemented)*
- **`PrinterUtils.connectPrinter`:** **`withTimeoutOrNull`** (12s); **`resumeOnce`** (atomic) so late callbacks don’t double-resume; **do not resume on `onDisconnected`** for bind outcome; **`onDisconnected`** only clears **`printerService`**.
- **`PrinterUtils.printMessage`:** After **`printText`**, wrap **`lineWrap`** and **`cutPaper`** in try/catch + **`Log.w`** — still return **`true`** if body printed (cutter/feed failures are non-fatal for user messaging).
- **`OrderHistoryScreen`:** If snapshot is **`null`**, snackbar **`Could not load order for print`** instead of **`Print failed`**.

### Files
- `app/src/main/java/com/redn/farm/utils/PrinterUtils.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/OrderHistoryScreen.kt`

### Verification
- On **Sunmi** (and non-Sunmi fallback if applicable): print flows that **succeed** never show **failure** snackbars; intentional failures (no printer, no service) still show a **clear** error.
- `./gradlew :app:compileDebugKotlin` ✅

**Status:** `[x]`

---

## Completion tracker

| Bug ID | Title | Status | Notes |
|--------|-------|--------|-------|
| BUG-PRT-01 | Print: “Print failed” snackbar even when print succeeds | `[x]` | **`connectPrinter`** no resume on **`onDisconnected`**; **`lineWrap`/`cutPaper`** non-fatal; order history snapshot message |
| BUG-SYS-02 | Logout: direct to Login, no intermediate screen transitions | `[x]` | **`LoginViewModel.logout`** + **`SessionChecker`**: **`popUpTo(graph.id)`** **`inclusive`**; removed duplicate **`NavGraph.onLogout`** |
| BUG-ORD-05 | Active SRPs: channel chips drive list + expander + print | `[x]` | **`ActiveSrpsCollapsedSummary`** + **`ActiveSrpsSelectedChannelDetail`**; **`ProductActiveSrpRow`** no **`summaryFromPerKg`** |
| BUG-ORD-04 | Order SRP vs Active SRPs print (resolver alignment); receipts **`/kg`/`/pc`** | `[x]` | **`OrderPricingResolver.srpFromAcquisition`** in **`ActiveSrpsScreen.printPriceList`**; **`ThermalPrintBuilders.buildOrderReceiptText`** + order history screens |
| BUG-ORD-03 | Order history date filter → same picker pattern as Acquire Produce | `[x]` | `OrderHistoryFilters`: From/To cards + `DatePickerDialog` + `DatePicker` |
| BUG-RMT-01 | Remittance add/edit → full-screen form | `[x]` | `RemittanceFormScreen` + nav; see **BUG-RMT-01** section |
| BUG-RMT-02 | Remittance form: full date calendar + OK (not clipped inline picker) | `[x]` | `RemittanceFormScreen`: date card → `DatePickerDialog` + `DatePicker`; see **BUG-RMT-02** |
| BUG-ACQ-01 | Acquire numeric inputs not responding | `[x]` | Verified on device; numeric pad is a bottom-aligned Dialog above AlertDialog |
| BUG-ACQ-02 | Acquire: optional unit price when total given; derive price/qty | `[x]` | `resolveAcquisitionQuantityPriceTotal`; pad + preview + save |
| BUG-ACQ-03 | Acquire dialog: stacked fields; date+location same row | `[x]` | Full-width qty/price; switch row; max height 600dp |
| BUG-ACQ-04 | Acquire list cards: SRP expander layout confusing (pack lines) | `[x]` | Per-channel `Surface` blocks; labeled pack rows; per-piece per channel (`AcquireProduceScreen`) |
| BUG-ACQ-07 | SrpCalculator applies spoilage to per-piece acquisitions (CLARIF-01) | `[x]` | `Input.isPerKgAcquisition`; `AcquisitionRepository` passes `is_per_kg` |
| BUG-ACQ-08 | SRP price list print: per-kg column shown for per-piece products | `[x]` | `ThermalSrpPrintRow.isPerKg` + `buildSrpPriceList`; `ActiveSrpsScreen` |
| BUG-ACQ-06 | Per-piece SRP must be used for per-piece orders | `[x]` | Order pricing now derives `*_per_piece` from stored `*_per_kg` + `piece_count` when `*_per_piece` columns are null; Active SRP screen also shows derived per-piece values. |
| BUG-PRD-01 | Take Order: add product — IME/pad under dialog | `[x]` | imePadding + quantity pad trigger; verify on device |
| BUG-PRD-02 | Manage Products: reload default vs delete confusion | `[x]` | Reload-default removed; per-product delete kept; verified |
| BUG-PRD-03 | Product form: after save, return to Manage Products | `[x]` | Navigate immediately after save; no pre-navigation snackbar wait |
| BUG-SYS-01 | Orphan `reinitializeDatabase()` in DatabaseInitializer | `[x]` | Method removed; seed only via `onCreate`; verified |
| BUG-ORD-01 | Take Order: Place order visibility / app bar | `[x]` | Top: compact “Order” + SRP + History; thin bottom total bar |
| BUG-ORD-02 | Order: finalize only when paid + delivered | `[x]` | `isOrderFinalized`; verified on device |
| BUG-PRC-01 | Preset history: delete inactive only | `[x]` | DAO + repo + history/detail UI; active protected |
| BUG-PRC-02 | Preset editor: after save → Preset History | `[x]` | `onSaveSuccessNavigateToPresetHistory`; popUpTo pricing presets home |
| BUG-PRC-03 | CLARIF per-piece **B** (lot) vs **(A+B)×μ** and **FR-PC-14** rounding | `[x]` | **`pricing_clarif.md`** **SRP** = **(A + B/total_quantity)** × markup; **§4.3.1** table aligned |
| BUG-PRC-04 | CLARIF by-weight spoilage: **rate** vs **absolute kg** (line 10) | `[x]` | **`spoilage_kg`** on acquisition + **`SrpCalculator`**; form + tests v7 |
| BUG-EMP-01 | Employee payment net pay: gross + advance; liquidated not in net | `[x]` | `netPayAmount()`, `PaymentFormScreen`, `PaymentCard`, unit tests |
| BUG-EMP-02 | Employee payment: signature optional; draft save without signing | `[x]` | Save without signature; print voucher still requires signature |
| BUG-EMP-03 | Employee payment: Finalize requires signature; finalized not editable | `[x]` | `is_finalized` (v5 schema, no incremental migration yet); Finalize; repo guards |
| BUG-ARC-01 | Hilt-only ViewModels — remove manual `ViewModelProvider.Factory` | `[x]` | **`@HiltViewModel`** + **`hiltViewModel()`** / scoped parents; **`build_framework.md`** §15.1 |
| BUG-ARC-02 | Room timestamps → epoch millis; retire `LocalDateTime` converters | `[x]` | **`CustomerEntity`**, **`ProductPriceEntity`**, v8; **`DateTimeConverter`** removed |
| BUG-ARC-03 | RBAC audit vs `Rbac.kt` / `user_roles.md` | `[ ]` | **BUG-ARC-03**; §15.3 |
| BUG-ARC-04 | Incremental Room migrations (production strategy) | `[ ]` | **BUG-ARC-04**; §15.4 |
| BUG-ARC-05 | Dev/QA: `dev.sh fresh` / clean install as default regression | `[ ]` | **BUG-ARC-05**; §15.5 |
| BUG-ARC-06 | Unify numeric pad + date picker patterns | `[ ]` | **BUG-ARC-06**; §15.6 |
| BUG-ARC-07 | Pricing formula changes require tests / checklist | `[ ]` | **BUG-ARC-07**; §15.7 |
| BUG-ARC-08 | Audit all price displays + print for `is_per_kg` | `[ ]` | **BUG-ARC-08**; §15.8 |
| BUG-ARC-09 | Epoch millis everywhere (`build_framework` §15.2); domain/UI `LocalDateTime` sweep | `[ ]` | Extends **BUG-ARC-02**; **FarmOperation**, filters, migration, **ThermalPrintBuilders** |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-04-06 | **BUG-FOP-04** fixed: **`loggedInUsernameOrEmpty()`** + new-op **`personnel`** default; edit via **`LaunchedEffect`**; **`[x]`**. |
| 2026-04-03 | **BUG-FOP-03** fixed: **Log operation** **Weather** — **`ExposedDropdownMenu`** **Hot/Dry** / **Rainy** / **Cloudy**; **`normalizeFarmOpWeather`** for legacy text; tracker **`[x]`**. |
| 2026-04-06 | **BUG-FOP-03** logged: **Log operation** — **Weather** → dropdown **Hot/Dry**, **Rainy**, **Cloudy** (not free text); **`[ ]`**. |
| 2026-04-06 | **BUG-FOP-02** fixed: **`FarmOperationsViewModel.products`** **`SharingStarted.Eagerly`** (list screen never subscribed — **`WhileSubscribed`** dropped upstream); product sheet empty states; **`NavGraph`** form VM owner fallback; tracker **`[x]`**. |
| 2026-04-06 | **BUG-PRT-01** fixed: **`PrinterUtils.connectPrinter`** (**`withTimeoutOrNull`**, **`resumeOnce`**, no bind failure on **`onDisconnected`**); **`lineWrap`/`cutPaper`** errors after **`printText`** don’t force **`false`**; **`OrderHistoryScreen`** snapshot **`null`** → **Could not load order for print**; tracker **`[x]`**. |
| 2026-04-06 | **BUG-ARC-09** logged: **`docs/build_framework.md`** §15.2 — **epoch millis everywhere**; remaining **`LocalDateTime`** in domain/repos/UI beyond **BUG-ARC-02**; tracker **`[ ]`**. |
| 2026-04-06 | **BUG-SYS-02** fixed: single **`LoginViewModel.logout()`** path; **`SessionChecker`** **`popUpTo(navController.graph.id)`** **`inclusive`** + **`launchSingleTop`**; removed **`NavGraph.onLogout`** / **`MainViewModel.logout`**; tracker **`[x]`**. |
| 2026-04-06 | **BUG-ORD-05** fixed: **Active SRPs** UI filters collapsed + expanded detail to **selected** channel (resolver-aligned); **`ActiveSrpsViewModel`** list no longer drops piece-only rows; tracker **`[x]`**. |
| 2026-04-06 | **BUG-ORD-04** fixed + logged: **Active SRPs** thermal price list uses **`OrderPricingResolver.srpFromAcquisition`** (matches Take Order when **`srp_*_per_piece`** is null); order receipt + detail/edit UI label unit price with **`/kg` / `/pc`**; tracker **`[x]`**. |
| 2026-04-06 | **BUG-ARC-02** fixed: Room **v8** — **`customers`** / **`product_prices`** timestamps as epoch **millis**; **`Customer`** / **`ProductPrice`** domain aligned; **`DateTimeConverter`** removed; tracker **`[x]`**. |
| 2026-04-06 | **BUG-ARC-01** fixed: Hilt-only ViewModels — **`@Inject`** repos + **`CsvExportService`**, **`@HiltViewModel`** migration, **`hiltViewModel()`** / parent-scoped nav; login activity-scoped VM; tracker **`[x]`**. |
| 2026-04-05 | **BUG-ARC-01–08** logged (**architecture / v2 debt** from **`docs/build_framework.md`** §15): Hilt-only VMs, epoch millis, RBAC audit, incremental migrations, `dev.sh fresh` discipline, unified pad/date UX, pricing test gate, **`is_per_kg`** display audit. |
| 2026-04-05 | **BUG-PRC-04** fixed: **`acquisitions.spoilage_kg`**, **`SrpCalculator.spoilageKg`**, **`AcquisitionFormScreen`** optional unsellable kg, **`SrpCalculatorTest`**, Room v7, CSV **SpoilageKg**; tracker **`[x]`**. |
| 2026-04-03 | **BUG-PRC-04** logged: **`pricing_clarif.md`** line 10 — spoilage **rate** or **actual kg**; **PricingReference** v0.9.33-draft, **USER_STORIES**, tracker, **INV_ACQUISITION_SRP_TRACKER**, **CLAUDE.md**, **rebuild_plan** aligned; implementation open. |
| 2026-04-05 | **BUG-PRC-03** logged + fixed: lot-level CLARIF **B** vs mistaken **(A + B)** per-piece line — **`pricing_clarif.md`** now **(A + B/total_quantity)** × markup; **PricingReference** §4.3.1 **SRP per piece** row aligned; tracker **`[x]`**. |
| 2026-04-05 | **BUG-ACQ-07** fixed: `SrpCalculator.Input.isPerKgAcquisition`; per-piece acquisitions use **no spoilage in SRP**. **BUG-ACQ-08** fixed: thermal price list per-piece rows show **`/pc`** as primary. **BUG-ORD-03** fixed: order history **From/To** `DatePickerDialog` + `DatePicker` like acquisitions. **BUG-RMT-01** tracker aligned `[x]`. |
| 2026-04-03 | **BUG-ACQ-07** logged: `SrpCalculator.compute()` applies preset spoilage to per-piece acquisitions — violates CLARIF-01 (spoilage = 0 for per-piece); inflates stored SRPs by ~33% with 25% spoilage. Fix: `effectiveSpoilage = 0` when `pieceCount != null`. |
| 2026-04-03 | **BUG-ACQ-08** logged: `buildSrpPriceList` always prints per-kg SRP in the primary column even for per-piece products; `ThermalSrpPrintRow` needs `isPerKg`; `ActiveSrpsScreen` call site + `buildSrpPriceList` row logic need updating. |
| 2026-04-03 | **BUG-ACQ-06** fixed: per-piece acquisitions now show only per-piece SRPs on Acquire list + Active SRPs screen; receiving slip prints `/pc` SRP. Root cause: display composables and print builder were hardcoded to per-kg columns (`ThermalPrintBuilders.kt`, `AcquireProduceScreen.kt`, `ActiveSrpsScreen.kt`). |
| 2026-04-04 | **BUG-ACQ-04** fixed: Acquire list SRP expander — per-channel **`Surface`** blocks, **Packs** rows (500/250/100 g), per-piece per channel (`AcquireProduceScreen.kt`). |
| 2026-04-03 | **BUG-ACQ-02** implemented: optional price/unit when total set; `AcquireProduceScreen.kt`. |
| 2026-04-03 | **BUG-ACQ-03** implemented: acquisition dialog stacked qty/price rows; date+location paired; `heightIn` 600dp. |
| 2026-04-02 | **BUG-PRD-03** logged + fix: product form pops to Manage Products immediately after save (`ProductFormScreen.kt`). |
| 2026-04-02 | **BUG-PRC-02** logged + fix: preset editor navigates to Preset History after successful save (`PricingPresetEditorScreen.kt`, `NavGraph.kt`). |
| 2026-04-02 | **BUG-RMT-01** logged: remittance add/edit should move from `AlertDialog` to full-screen form (`RemittanceScreen.kt` / nav TBD). |
| 2026-04-03 | **BUG-RMT-01** marked **`[x]`**: `RemittanceFormScreen`, `Screen.RemittanceForm` / `NavGraph`, shared `RemittanceViewModel`. |
| 2026-04-03 | **BUG-RMT-02** logged + fix: remittance form date — `OutlinedCard` + `DatePickerDialog` with OK/Cancel (`RemittanceFormScreen.kt`); removes clipped inline `heightIn` picker. |
| 2026-04-02 | **BUG-ORD-03** logged: order history date range filter should use the same `OutlinedCard` + `DatePickerDialog` + `DatePicker` pattern as `AcquisitionFormScreen` (not `DateRangePicker` dialog UX). |

