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

## BUG-RMT-01 — Remittance: add/edit as full-screen form (not `AlertDialog`)

### Report
- **Screen:** **Remittances** (`RemittanceScreen`).
- **Current:** **Add** and **Edit** use a private **`RemittanceDialog`** built as **`AlertDialog`** (amount, inline date picker, remarks, Save/Cancel). Delete already uses a confirm dialog; list/print behavior lives on the main screen.
- **Expected:** Add/edit should use a **full-screen form** (e.g. dedicated composable with **`Scaffold`**, top app bar, scrollable body, primary **Save** in app bar and/or bottom bar), consistent with **Manage Products** (`ProductFormScreen`), **pricing preset editor**, **employee payment form**, and the direction in **`docs/user_review_screens_stories.md`** (full-screen routes for non-trivial forms).

### Why
- Aligns navigation and layout with the rest of the app on small POS / Sunmi-class devices.
- Avoids cramming **`DatePicker`** + fields into a dialog **`heightIn`**; improves thumb reach and visual hierarchy.
- Optional follow-on (**URS-08**): wire **amount** to the shared **numeric pad** instead of decimal IME.

### Fix *(not implemented yet)*
- Introduce a route such as `remittance_form/{remittanceId}` with `new` (or equivalent) for add; **`NavGraph`** + back stack from **`Screen.Remittance`**.
- Move form UI out of **`AlertDialog`** into **`RemittanceFormScreen`** (or inline full-screen in the same file initially). Reuse **`RemittanceViewModel`** scoped to the remittance list **`NavBackStackEntry`** (mirror **`ProductFormScreen`** + **`ManageProductsViewModel`**). **`RemittanceViewModel`** is **`ViewModelProvider.Factory`**-based today (not Hilt) — either keep **`viewModel(..., owner = parentEntry)`** or migrate the VM to Hilt for consistency.
- Preserve: delete confirm, print slip, search/list on the main remittance screen.

### Files *(expected touch list)*
- `app/src/main/java/com/redn/farm/ui/screens/remittance/RemittanceScreen.kt`
- `app/src/main/java/com/redn/farm/navigation/NavGraph.kt`
- New: e.g. `RemittanceFormScreen.kt` (if split out)

### Verification
- Add remittance → full-screen form → save → returns to list; edit flow same.
- No stacked modal-on-modal for date (inline date on the full screen is fine).
- `./gradlew assembleDebug` ✅ *(after implementation)*

---

## Completion tracker

| Bug ID | Title | Status | Notes |
|--------|-------|--------|-------|
| BUG-RMT-01 | Remittance add/edit → full-screen form | `[ ]` | Today: `RemittanceDialog` `AlertDialog`; align with `ProductForm` / preset editor pattern |
| BUG-ACQ-01 | Acquire numeric inputs not responding | `[x]` | Verified on device; numeric pad is a bottom-aligned Dialog above AlertDialog |
| BUG-ACQ-02 | Acquire: optional unit price when total given; derive price/qty | `[x]` | `resolveAcquisitionQuantityPriceTotal`; pad + preview + save |
| BUG-ACQ-03 | Acquire dialog: stacked fields; date+location same row | `[x]` | Full-width qty/price; switch row; max height 600dp |
| BUG-PRD-01 | Take Order: add product — IME/pad under dialog | `[x]` | imePadding + quantity pad trigger; verify on device |
| BUG-PRD-02 | Manage Products: reload default vs delete confusion | `[x]` | Reload-default removed; per-product delete kept; verified |
| BUG-PRD-03 | Product form: after save, return to Manage Products | `[x]` | Navigate immediately after save; no pre-navigation snackbar wait |
| BUG-SYS-01 | Orphan `reinitializeDatabase()` in DatabaseInitializer | `[x]` | Method removed; seed only via `onCreate`; verified |
| BUG-ORD-01 | Take Order: Place order visibility / app bar | `[x]` | Top: compact “Order” + SRP + History; thin bottom total bar |
| BUG-ORD-02 | Order: finalize only when paid + delivered | `[x]` | `isOrderFinalized`; verified on device |
| BUG-PRC-01 | Preset history: delete inactive only | `[x]` | DAO + repo + history/detail UI; active protected |
| BUG-PRC-02 | Preset editor: after save → Preset History | `[x]` | `onSaveSuccessNavigateToPresetHistory`; popUpTo pricing presets home |
| BUG-EMP-01 | Employee payment net pay: gross + advance; liquidated not in net | `[x]` | `netPayAmount()`, `PaymentFormScreen`, `PaymentCard`, unit tests |
| BUG-EMP-02 | Employee payment: signature optional; draft save without signing | `[x]` | Save without signature; print voucher still requires signature |
| BUG-EMP-03 | Employee payment: Finalize requires signature; finalized not editable | `[x]` | `is_finalized` (v5 schema, no incremental migration yet); Finalize; repo guards |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-04-03 | **BUG-ACQ-02** implemented: optional price/unit when total set; `AcquireProduceScreen.kt`. |
| 2026-04-03 | **BUG-ACQ-03** implemented: acquisition dialog stacked qty/price rows; date+location paired; `heightIn` 600dp. |
| 2026-04-02 | **BUG-PRD-03** logged + fix: product form pops to Manage Products immediately after save (`ProductFormScreen.kt`). |
| 2026-04-02 | **BUG-PRC-02** logged + fix: preset editor navigates to Preset History after successful save (`PricingPresetEditorScreen.kt`, `NavGraph.kt`). |
| 2026-04-02 | **BUG-RMT-01** logged: remittance add/edit should move from `AlertDialog` to full-screen form (`RemittanceScreen.kt` / nav TBD). |

