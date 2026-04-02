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

## Completion tracker

| Bug ID | Title | Status | Notes |
|--------|-------|--------|-------|
| BUG-ACQ-01 | Acquire numeric inputs not responding | `[x]` | Verified on device; numeric pad is a bottom-aligned Dialog above AlertDialog |
| BUG-PRD-01 | Take Order: add product — IME/pad under dialog | `[x]` | imePadding + quantity pad trigger; verify on device |
| BUG-PRD-02 | Manage Products: reload default vs delete confusion | `[x]` | Reload-default removed; per-product delete kept; verified |
| BUG-SYS-01 | Orphan `reinitializeDatabase()` in DatabaseInitializer | `[x]` | Method removed; seed only via `onCreate`; verified |
| BUG-ORD-01 | Take Order: Place order visibility / app bar | `[x]` | Top: compact “Order” + SRP + History; thin bottom total bar |
| BUG-ORD-02 | Order: finalize only when paid + delivered | `[x]` | `isOrderFinalized`; verified on device |
| BUG-PRC-01 | Preset history: delete inactive only | `[x]` | DAO + repo + history/detail UI; active protected |
| BUG-EMP-01 | Employee payment net pay: gross + advance; liquidated not in net | `[x]` | `netPayAmount()`, `PaymentFormScreen`, `PaymentCard`, unit tests |

