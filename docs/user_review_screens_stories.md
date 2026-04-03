# User review screens — implementation plan & user stories

**Companion:** [`user_review_screens.md`](./user_review_screens.md) (screen-by-screen UX review)  
**Created:** 2026-04-04  
**Purpose:** Turn review findings into **trackable user stories** and a **phased implementation plan**. Update the **tracker table** as work lands; do not duplicate the full pattern rationale here — link back to the companion doc.

---

## How to use this file

| Column / section | Use |
|------------------|-----|
| **Implementation plan** | Sequencing, dependencies, rough effort |
| **User stories** | Acceptance criteria + mapping to `user_review_screens.md` § |
| **Tracker** | Single place for status (`[ ]` / `[~]` / `[x]`) |

**Status legend**

| Mark | Meaning |
|------|---------|
| `[ ]` | Not started |
| `[~]` | In progress / partial |
| `[x]` | Done / verified in app |

---

## Implementation plan

### Principles (from companion doc)

- Simple confirm → `AlertDialog`; **large / scrollable forms** → **full-screen route**
- **No nested** `DatePickerDialog` **inside** another modal where avoidable
- **Numeric entry** → shared numeric pad where possible (not soft keyboard)
- **Reversible toggles** → immediate apply + **Snackbar with Undo** (not per-toggle confirm)
- **Long forms** → **Save** duplicated in **bottom bar** (thumb reach)

### Phase 1 — P1 (ship first)

| Order | Theme | Work items | Depends on |
|------|--------|------------|------------|
| 1.1 | **Order detail toggles** | Snackbar undo for paid/delivered (`OrderDetailScreen`) | None |
| 1.2 | **Nested date pickers** | Remittance add/edit: no dialog-on-dialog; delete confirm. **Follow-up (full-screen form):** [`bugs.md`](./bugs.md) **BUG-RMT-01** | None |
| 1.3 | **Long-form Save** | `PricingPresetEditorScreen` bottom `FilledButton` Save (+ top bar keep) | None |
| 1.4 | **Full-screen: Products** | Nav route `product_form/{productId}` (`new` = add), migrate add/edit off `AlertDialog` | Nav + VM |
| 1.5 | **Full-screen: Customers** | Route `customer_form/{customerId}` (`new` = add); collapsible address; type chips | Nav + VM |
| 1.6 | **Full-screen: Acquisitions** | Route `acquisition_form/{acquisitionId}` (`new` = add); `DatePickerDialog` from date card; location **FilterChip** row; product **ModalBottomSheet** | Nav + VM; **`[x]`** |
| 1.7 | **Full-screen: Farm ops** | Route `farm_op_form/{operationId}` (`new` = add); multiline details; type chips; optional product sheet | Nav + VM |

*Rationale:* 1.1–1.3 are **small, high-impact** fixes. 1.4–1.7 are **parallelisable** after routes are defined (split by agent per screen family to reduce merge pain).

### Phase 2 — P2 (next iteration)

| Theme | Work items |
|--------|------------|
| **Numeric pad** | Wire shared pad to **Remittance** amount; **PricingPresetEditor** numeric fields |
| **Employee payments** | Collapse outstanding + period summary into expandable **banner**; move print to app bar or banner |
| **Preset detail** | Replace raw JSON with structured or pretty-printed read-only UI |
| **Export** | “Export *N* tables” badge on primary action |
| **User management** | Create-user dialog: **2-column** role chip grid where scroll is tight |
| **Active SRPs** | Optional full-width bottom **Print price list** (duplicate of app bar) |

### Phase 3 — P3 (polish)

| Theme | Work items |
|--------|------------|
| **Order history** | Compound **date range** control (fewer taps than two separate pickers) |
| **Payment form** | `ImeAction.Next` between amount / advance / liquidated |

### Verification-only (no structural change)

| Item | Companion § | Action |
|------|-------------|--------|
| Take Order kg **0.25** step | TakeOrderScreen | Device QA |
| **ManageEmployees** contact `KeyboardType.Phone` | ManageEmployeesScreen | Small tweak when touching file |
| **EditOrder** stepper parity with Take Order | EditOrderScreen | QA / parity pass |
| **MainScreen** UI-22 tile layout | MainScreen | **`[x]`** — see **URX-01** (update companion § MainScreen when convenient) |

---

## User stories

### Phase 1 — P1

#### URS-01 — Order status toggles with undo

**As a** store assistant **I want** **Paid** and **Delivered** to apply **immediately** **so that** I save taps during busy periods.

**Acceptance criteria**

- Toggling **Paid** or **Delivered** updates the order **without** a blocking confirm dialog.
- A **Snackbar** appears with an **Undo** action; undo restores the previous boolean within a short window (e.g. ~4s).
- **Irreversible** actions (e.g. delete order) **keep** confirm dialogs.

**Maps to:** `user_review_screens.md` → **OrderDetailScreen**; cross-cutting §3.

---

#### URS-02 — Remittance: date entry without stacked modals + safe delete

**As a** user recording remittances **I want** to pick a date **without** two overlapping dialogs **so that** the flow is clear on a small POS screen.

**Acceptance criteria**

- Add/edit remittance does **not** use `DatePickerDialog` **stacked on** the same surface as the form in a confusing way (e.g. bottom sheet form with inline date **or** single-modal pattern per companion).
- **Delete** remittance requires a **one-line confirmation** before removal.

**Maps to:** `user_review_screens.md` → **RemittanceScreen**; cross-cutting §5.

**Related (beyond URS-02):** Migrate add/edit from **`AlertDialog`** to a **full-screen** route for parity with products / presets — tracked as **BUG-RMT-01** in [`bugs.md`](./bugs.md).

---

#### URS-03 — Pricing preset editor: Save at bottom of long form

**As an** admin **I want** **Save** reachable **after scrolling** **so that** I don’t scroll back to the top on a long editor.

**Acceptance criteria**

- `PricingPresetEditorScreen` exposes **Save** in **`Scaffold.bottomBar`** (primary **Button**, same as Material filled style).
- Top app bar **Save** remains (duplicate primary action pattern).

**Maps to:** `user_review_screens.md` → **PricingPresetEditorScreen**; cross-cutting §6.

---

#### URS-04 — Products: full-screen add/edit

**As an** admin **I want** product add/edit on a **full screen** **so that** many fields (name, unit, category, prices, etc.) aren’t crammed in a scrolling `AlertDialog`.

**Acceptance criteria**

- New route `product_form/{productId}` (`new` for add) with **back** stack.
- **Delete** stays a simple confirm dialog.
- Existing **fallback price numeric pad** behaviour is **preserved**.

**Maps to:** `user_review_screens.md` → **ManageProductsScreen**; cross-cutting §1.

---

#### URS-05 — Customers: full-screen add/edit with clearer address & type

**As an** admin **I want** customer add/edit **full screen** with **address** grouped and **customer type** quick to select **so that** 8+ fields are usable on Sunmi.

**Acceptance criteria**

- Route `customer_form/{customerId}` (`new` for add).
- **Address** fields in a **collapsible** section (e.g. “Address details”) to reduce first paint noise.
- **Customer type** uses **segmented** control (single row), not a long dropdown-only pattern.

**Maps to:** `user_review_screens.md` → **ManageCustomersScreen**; cross-cutting §1.

---

#### URS-06 — Acquisitions: full-screen add/edit

**As a** purchasing user **I want** acquisition add/edit **full screen** **so that** quantity, cost, SRP preview, date, and location aren’t stacked in one dialog.

**Acceptance criteria**

- Route `acquisition_form/{acquisitionId}` (`new` for add).
- **Date** uses **one** `DatePickerDialog` from the date card (no dialog stacking on a form dialog).
- **Location** uses a **single-row** control (e.g. segmented row for FARM / MARKET / SUPPLIER / OTHER).
- **Batch print** (PRN-08) placement on list screen **unchanged** unless product says otherwise.

**Maps to:** `user_review_screens.md` → **AcquireProduceScreen** / **AcquisitionFormScreen**; cross-cutting §1, §5.

---

#### URS-07 — Farm operations: full-screen add/edit

**As a** farmer **I want** farm operation add/edit **full screen** **so that** multiline **details** and several fields aren’t cramped in a dialog.

**Acceptance criteria**

- Route `farm_op_form/{operationId}` (`new` for add).
- **Operation type** selectable via **chips** (grid if many types).
- **Details** field supports several lines visible (`maxLines` / min height appropriate).
- **Product** link optional in a collapsible block if it saves space.

**Maps to:** `user_review_screens.md` → **FarmOperationsScreen**; cross-cutting §1.

---

### Phase 2 — P2

#### URS-08 — Numeric pad & full-form for Remittance and Pricing Editor

**As a** user **I want** amount and key numeric fields to open the **app numeric pad** in a **full-screen form** (similar to farm ops and others) **so that** the soft keyboard doesn’t hide totals and the layout works well for multiple fields on the Sunmi device.

**Acceptance criteria**

- **Remittance add/edit** uses a **full-screen form route** instead of a dialog, following the pattern of FarmOps, Acquisitions, etc.
- Amount entry field opens the **shared app numeric pad** (not the standard IME).
- **PricingPresetEditor** also uses a **full-screen route** for add/edit, with all numeric fields wired to the project numeric-pad pattern.
- Ensure consistency with other form-heavy screens in layout and UX.

**Maps to:** `user_review_screens.md` → **RemittanceScreen**, **PricingPresetEditorScreen**; cross-cutting §1, §4.

---

#### URS-09 — Employee payments: denser summary header

**As a** user **I want** outstanding and period totals **summarised compactly** **so that** I reach the payment list with less scrolling.

**Acceptance criteria**

- Outstanding + period net (or equivalent) available in a **compact banner**; **tap expands** detail if needed.
- **Print summary** moved to **top bar** and/or banner adjacency (not only a full-width button below stacks).

**Maps to:** `user_review_screens.md` → **EmployeePaymentScreen**.

---

#### URS-10 — Preset detail: readable structured data

**As an** admin **I want** hauling / channel / category data **readable** **so that** I don’t read raw JSON strings.

**Acceptance criteria**

- JSON-backed fields render as **structured UI** or **pretty-printed** text with clear labels.

**Maps to:** `user_review_screens.md` → **PresetDetailScreen**.

---

#### URS-11 — Export: selection count on primary action

**As an** admin **I want** the Export action to show **how many tables** are selected **so that** I don’t miscount before running export.

**Acceptance criteria**

- Export button (or adjacent label) shows **count** of selected tables (e.g. “Export 7 tables”).

**Maps to:** `user_review_screens.md` → **ExportScreen**.

---

#### URS-12 — User management: role chips in two columns

**As an** admin **I want** role selection **dense** **so that** the create-user dialog doesn’t scroll on small height.

**Acceptance criteria**

- Role `FilterChip`s arranged in a **2-column** grid (or equivalent) for five roles.

**Maps to:** `user_review_screens.md` → **UserManagementScreen**.

---

#### URS-13 — Active SRPs: optional bottom print

**As a** store assistant **I want** an obvious **Print price list** control **so that** I don’t rely only on a small top icon.

**Acceptance criteria**

- Optional **full-width** bottom **OutlinedButton** duplicates top-bar print (pattern aligned with order print duplication).

**Maps to:** `user_review_screens.md` → **ActiveSrpsScreen**.

---

### Phase 3 — P3

#### URS-14 — Order history: date range in fewer taps

**As a** user **I want** to set **from/to dates** with **fewer steps** **so that** filtering orders is faster.

**Acceptance criteria**

- Date range filter uses a **single compound** control (custom `DateRangePicker` UX or Material3 equivalent) instead of two fully separate pickers where feasible.

**Maps to:** `user_review_screens.md` → **OrderHistoryScreen**.

---

#### URS-15 — Payment form: keyboard next between money fields

**As a** cashier **I want** **Next** between gross / advance / liquidated **so that** I move through fields without extra taps.

**Acceptance criteria**

- `ImeAction` chain (or equivalent) across the listed fields on `PaymentFormScreen`.

**Maps to:** `user_review_screens.md` → **PaymentFormScreen**.

---

## Tracker (URS-01–URS-15)

**Audit (2026-04-04):** Original audit had all URS rows **`[ ]`**; **URS-01–URS-07** (including **URS-06** acquisitions full-screen) are **`[x]`** in code (see changelog). Use the **URX** table for slices that map to companion “✅ Good” items without satisfying full URS acceptance.

| ID | Story | Phase | Status | Primary surfaces / files | Notes |
|----|--------|-------|--------|---------------------------|--------|
| URS-01 | Order toggles + Snackbar undo | P1 | `[x]` | `OrderDetailScreen` | Paid/delivered apply immediately; Snackbar **Undo** reverts within short duration. |
| URS-02 | Remittance date + delete confirm | P1 | `[x]` | `RemittanceScreen` | Inline `DatePicker` in add/edit dialog; delete uses confirm `AlertDialog`. **BUG-RMT-01** (`bugs.md`): add/edit → full-screen form — **`[ ]`**. |
| URS-03 | Preset editor bottom Save | P1 | `[x]` | `PricingPresetEditorScreen` | `Scaffold.bottomBar` **Save preset** primary `Button`; top **Save** kept. |
| URS-04 | Products full-screen add/edit | P1 | `[x]` | `ProductFormScreen`, `ManageProductsScreen`, `NavGraph` | Route `product_form/{productId}` (`new` = add); scoped `ManageProductsViewModel`; fallback price sheet unchanged. |
| URS-05 | Customers full-screen add/edit | P1 | `[x]` | `CustomerFormScreen`, `ManageCustomersScreen`, `NavGraph` | `customer_form/{customerId}`; scoped `ManageCustomersViewModel` (`Factory`); collapsible address; type **FilterChip** row. |
| URS-06 | Acquisitions full-screen add/edit | P1 | `[x]` | `AcquisitionFormScreen`, `AcquireProduceScreen`, `NavGraph` | Route `acquisition_form/{acquisitionId}`; shared `AcquireProduceViewModel` from acquire back stack; location chips; product sheet; BUG-ACQ-02 — **URX-02**. |
| URS-07 | Farm ops full-screen add/edit | P1 | `[x]` | `FarmOperationFormScreen`, `FarmOperationsScreen`, `FarmOperationHistoryScreen`, `NavGraph` | `farm_op_form/{operationId}`; type chips (rows); multiline details; optional product **ModalBottomSheet**; date via full-screen `DatePickerDialog`. |
| URS-08 | Numeric pad: Remittance + pricing editor | P2 | `[ ]` | `RemittanceScreen`, `PricingPresetEditorScreen` | No `NumericPad` wiring in these screens. |
| URS-09 | Employee payment summary density | P2 | `[ ]` | `EmployeePaymentScreen` | Stacked cards + full-width print unchanged. |
| URS-10 | Preset detail structured JSON | P2 | `[ ]` | `PresetDetailScreen` | Raw JSON lines still shown. |
| URS-11 | Export selection count badge | P2 | `[ ]` | `ExportScreen` | Button text still “Export selected tables” without count. |
| URS-12 | User management 2-col role chips | P2 | `[ ]` | `UserManagementScreen` | Role chips single column (`fillMaxWidth()` each). |
| URS-13 | Active SRPs bottom print | P2 | `[ ]` | `ActiveSrpsScreen` | Print is top-bar icon only. |
| URS-14 | Order history date range compound | P3 | `[ ]` | `OrderHistoryScreen`, filters | Two separate date pickers. |
| URS-15 | Payment form Ime Next chain | P3 | `[ ]` | `PaymentFormScreen` | No `ImeAction.Next` chain on money fields. |

---

## Done in code — slice tracker (URX)

These rows are **`[x]`** where behaviour is **already implemented** and aligns with `user_review_screens.md` (✅ sections, closed bugs, or UI improvement items). They are **not** substitutes for finishing the URS stories above when acceptance criteria are stricter.

| ID | Slice | Status | Evidence |
|----|--------|--------|----------|
| URX-01 | MainScreen **UI-22** — dashboard tiles (label top, icon below) | `[x]` | `MainScreen.kt` — `LazyVerticalGrid` tiles use `Column`, `maxLines = 1`, `heightIn(min = 110.dp)` |
| URX-02 | Acquisition form — BUG-ACQ-02 cost derive + BUG-ACQ-03 stacked fields | `[x]` | `AcquisitionFormScreen.kt` — `resolveAcquisitionQuantityPriceTotal`, full-width qty/price; date card + location chip row (full-screen) |
| URX-03 | **PaymentFormScreen** full-screen + live net pay (companion ✅; UI-14 / UI-16) | `[x]` | `PaymentFormScreen.kt`, `employee_payment_form/...` route in `NavGraph.kt` |
| URX-04 | **TakeOrderScreen** pinned total + place order + cart stepper (companion ✅) | `[x]` | `TakeOrderScreen.kt`, `OrderItemCard.kt` |
| URX-05 | **OrderHistoryScreen** filters + status chips + per-card print (companion ✅) | `[x]` | `OrderHistoryScreen.kt` |
| URX-06 | **OrderDetailScreen** print in app bar + full-width “Print Receipt” (companion ✅) | `[x]` | `OrderDetailScreen.kt` |
| URX-07 | **LoginScreen** `imePadding` + scroll + header (UI-07; companion ✅) | `[x]` | `LoginScreen.kt` |
| URX-08 | **ActiveSrpsScreen** channel chips + expandable rows + top-bar print (companion ✅) | `[x]` | `ActiveSrpsScreen.kt` — bottom duplicate print is still **URS-13** |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-04-04 | Initial plan, stories URS-01–URS-15, tracker table. |
| 2026-04-04 | Code audit: URS table + **Notes** column; **`[x]`** only on **URX** slice rows; partial callouts for URS-01 / URS-06. |
| 2026-04-02 | **URS-01–URS-04** P1: order detail Snackbar undo; remittance inline date + delete confirm; preset editor bottom Save; products full-screen form + nav. |
| 2026-04-02 | Linked **BUG-RMT-01** (`bugs.md`) from Phase **1.2**, **URS-02**, and tracker **Notes** (remittance full-screen form follow-up). |
| 2026-04-02 | **URS-05** + **URS-07**: `CustomerFormScreen` (`customer_form/...`), `FarmOperationFormScreen` (`farm_op_form/...`); removed `FarmOperationDialog.kt`; history edit uses form route. |
| 2026-04-02 | **URS-06**: `AcquisitionFormScreen` (`acquisition_form/...`); list screen navigates to form; `AcquisitionDialog` removed from `AcquireProduceScreen`. |

---

## Note on companion doc freshness

Some lines in `user_review_screens.md` (e.g. **MainScreen** UI-22, **Acquire** dialog layout) may lag the codebase. When implementing, **prefer this tracker + git truth**; update the companion doc in the same PR when behaviour changes materially.
