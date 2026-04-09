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
| 1.2 | **Nested date pickers** | Remittance: full-screen `RemittanceFormScreen`; date **`OutlinedCard` → `DatePickerDialog`** + OK/Cancel (same as acquisitions; [`bugs.md`](./bugs.md) **BUG-RMT-02**); delete confirm on list. | None |
| 1.3 | **Long-form Save** | `PricingPresetEditorScreen` bottom `FilledButton` Save (+ top bar keep) | None |
| 1.4 | **Full-screen: Products** | Nav route `product_form/{productId}` (`new` = add), migrate add/edit off `AlertDialog` | Nav + VM |
| 1.5 | **Full-screen: Customers** | Route `customer_form/{customerId}` (`new` = add); collapsible address; type chips | Nav + VM |
| 1.6 | **Full-screen: Acquisitions** | Route `acquisition_form/{acquisitionId}` (`new` = add); `DatePickerDialog` from date card; location **FilterChip** row; product **ModalBottomSheet** | Nav + VM; **`[x]`** |
| 1.7 | **Full-screen: Farm ops** | Route `farm_op_form/{operationId}` (`new` = add); multiline details; type chips; optional product sheet | Nav + VM |

*Rationale:* 1.1–1.3 are **small, high-impact** fixes. 1.4–1.7 are **parallelisable** after routes are defined (split by agent per screen family to reduce merge pain).

### Phase 2 — P2 (next iteration)

| Theme | Work items |
|--------|------------|
| **Numeric pad** | Wire shared pad to **Remittance** amount; **PricingPresetEditor** numeric fields — **`[x]`** (`NumericPadOutlinedTextField*`, `RemittanceFormScreen`, `PricingPresetEditorScreen`) |
| **Employee payments** | Collapse outstanding + period summary into expandable **banner**; move print to app bar or banner — **`[x]`** (`EmployeePaymentScreen`: banner + app bar print + banner print) |
| **Preset detail** | Replace raw JSON with structured or pretty-printed read-only UI — **`[x]`** (`PresetDetailStructuredSections.kt`, `PresetDetailScreen`) |
| **Export** | “Export *N* tables” on bundle primary button — **`[x]`** (`ExportScreen`) |
| **User management** | Create-user dialog: **2-column** role chip grid — **`[x]`** (`UserManagementScreen`) |
| **Active SRPs** | Full-width bottom **Print price list** — **`[x]`** (`ActiveSrpsScreen` + top bar) |

### Phase 3 — P3 (polish)

| Theme | Work items |
|--------|------------|
| **Order history** | Compound **date range** control — **`[x]`** (`OrderHistoryFilters`: single card + `DateRangePicker` in `DatePickerDialog`) |
| **Payment form** | `ImeAction.Next` / **Done** chain on money fields — **`[x]`** (`PaymentFormScreen`: `FocusRequester`) |

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

**As a** user recording remittances **I want** to pick a date **without** two overlapping **add/edit** modals **so that** the flow is clear on a small POS screen.

**Acceptance criteria**

- Add/edit runs on **full-screen** **`RemittanceFormScreen`** (not an **`AlertDialog`** form).
- **Date:** **`OutlinedCard`** opens **one** **`DatePickerDialog`** (`DatePicker` + OK/Cancel), matching **`AcquisitionFormScreen`** — not “alert form + date dialog” stacked the old way.
- **Delete** remittance requires a **one-line confirmation** before removal.

**Maps to:** `user_review_screens.md` → **RemittanceScreen** / **RemittanceFormScreen**; cross-cutting §5; **BUG-RMT-02** ([`bugs.md`](./bugs.md)).

**Related:** Full-screen route was tracked as **BUG-RMT-01** — **implemented** (`remittance_add_edit/...`). **BUG-RMT-02** documents the date **card → dialog** UX fix.

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

#### URS-08 — Numeric pad: Remittance + Pricing preset editor

**As a** user **I want** amount and preset numeric fields to open the **shared app numeric pad** **so that** the soft keyboard doesn’t cover the form on a small POS screen.

**Acceptance criteria**

- **Remittance** add/edit dialog: **Amount** uses the shared pad (`NumericPadOutlinedTextField` / `NumericPadBottomSheet`).
- **PricingPresetEditor** (already full-screen): spoilage, hauling cost/weight, hauling fee ₱, category overrides, channel markup/margin %, and channel fee amounts use the same pad pattern (`NumericPadOutlinedTextFieldForDouble` / `ForNullableDouble` where needed).
- **Follow-up (not this story):** Remittance **full-screen** form route — **BUG-RMT-01** / **URS-02** tracker notes.

**Maps to:** `user_review_screens.md` → **RemittanceScreen**, **PricingPresetEditorScreen**; cross-cutting §4.

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
| URS-02 | Remittance date + delete confirm | P1 | `[x]` | `RemittanceScreen`, `RemittanceFormScreen` | Full-screen add/edit; date **`OutlinedCard` → `DatePickerDialog`** (**BUG-RMT-02**); delete confirm on list. |
| URS-03 | Preset editor bottom Save | P1 | `[x]` | `PricingPresetEditorScreen` | `Scaffold.bottomBar` **Save preset** primary `Button`; top **Save** kept. |
| URS-04 | Products full-screen add/edit | P1 | `[x]` | `ProductFormScreen`, `ManageProductsScreen`, `NavGraph` | Route `product_form/{productId}` (`new` = add); scoped `ManageProductsViewModel`; fallback price sheet unchanged. |
| URS-05 | Customers full-screen add/edit | P1 | `[x]` | `CustomerFormScreen`, `ManageCustomersScreen`, `NavGraph` | `customer_form/{customerId}`; scoped `ManageCustomersViewModel` (`Factory`); collapsible address; type **FilterChip** row. |
| URS-06 | Acquisitions full-screen add/edit | P1 | `[x]` | `AcquisitionFormScreen`, `AcquireProduceScreen`, `NavGraph` | Route `acquisition_form/{acquisitionId}`; shared `AcquireProduceViewModel` from acquire back stack; location chips; product sheet; BUG-ACQ-02 — **URX-02**. |
| URS-07 | Farm ops full-screen add/edit | P1 | `[x]` | `FarmOperationFormScreen`, `FarmOperationsScreen`, `FarmOperationHistoryScreen`, `NavGraph` | `farm_op_form/{operationId}`; type chips (rows); multiline details; optional product **ModalBottomSheet**; date via full-screen `DatePickerDialog`. |
| URS-08 | Numeric pad: Remittance + pricing editor | P2 | `[x]` | `NumericPadOutlinedTextField.kt`, `RemittanceFormScreen`, `PricingPresetEditorScreen` | Shared pad for remittance **amount** + editor numerics. |
| URS-09 | Employee payment summary density | P2 | `[x]` | `EmployeePaymentScreen` | Compact **secondaryContainer** banner: outstanding + period net; tap/expand chevron for gross/advances/liquidated/net; **Print** in top bar + banner. |
| URS-10 | Preset detail structured JSON | P2 | `[x]` | `PresetDetailScreen`, `PresetDetailStructuredSections.kt` | Parse via `PricingPresetGson`; hauling / channels / categories UI; monospace fallback if parse fails. |
| URS-11 | Export selection count badge | P2 | `[x]` | `ExportScreen` | Bundle button: `Export N table(s)` when N ≥ 1. |
| URS-12 | User management 2-col role chips | P2 | `[x]` | `UserManagementScreen` | `CreateUserDialog` roles `chunked(2)` + `Row`/`weight`. |
| URS-13 | Active SRPs bottom print | P2 | `[x]` | `ActiveSrpsScreen` | `Scaffold.bottomBar` **OutlinedButton** duplicates top-bar print. |
| URS-14 | Order history date range compound | P3 | `[x]` | `OrderHistoryFilters.kt` | One **Date range** card; `DateRangePicker` (`showModeToggle = false`); start/end-of-day same as before. |
| URS-15 | Payment form Ime Next chain | P3 | `[x]` | `PaymentFormScreen.kt` | Gross → **Next** → advance → **Next** → liquidated → **Done** + `FocusRequester`. |

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
| URX-08 | **ActiveSrpsScreen** channel chips + expandable rows + top-bar print (companion ✅) | `[x]` | `ActiveSrpsScreen.kt` — bottom print added (**URS-13**) |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-04-05 | **Docs:** Per-piece acquisition semantics (**§5.1.1**) aligned across `USER_STORIES.md` (INV-US-01 / INV-US-05), `apis.md`, `DESIGN.md`, `CLAUDE.md`, `rebuild_plan.md`, `PricingReference.md` implementation note + **§4.3** row; `user_review_screens.md` (acquire form bullet); `schema_evolution.sql` (`acquisitions.quantity` / `piece_count` comments). |
| 2026-04-02 | **Docs:** **INV-US-05** SRP pipeline (**A** before markup) — `PricingReference.md` v0.9.28-draft, `USER_STORIES.md` (MGT-US-02), `apis.md` / `printing.md` sample numbers, `CLAUDE.md`, `rebuild_plan.md`. |
| 2026-04-03 | **Companion + URS-02 / Phase 1.2 / tracker:** Remittance date UX = **`OutlinedCard` → `DatePickerDialog`** (aligned with acquisitions; **BUG-RMT-02**); full-screen **`RemittanceFormScreen`**; SCR-REM-01 + cross-cutting §4–§5 updated in `user_review_screens.md`. |
| 2026-04-04 | **MGT-US-07** (`USER_STORIES.md`): custom / customer **SRP per channel** on **`AcquisitionFormScreen`**; `srp_custom_override` + Room v6 (dev: destructive migration / fresh install); `SrpCalculator` + CSV export flag. |
| 2026-04-04 | Initial plan, stories URS-01–URS-15, tracker table. |
| 2026-04-04 | Code audit: URS table + **Notes** column; **`[x]`** only on **URX** slice rows; partial callouts for URS-01 / URS-06. |
| 2026-04-02 | **URS-01–URS-04** P1: order detail Snackbar undo; remittance inline date + delete confirm; preset editor bottom Save; products full-screen form + nav. |
| 2026-04-02 | Linked **BUG-RMT-01** (`bugs.md`) from Phase **1.2**, **URS-02**, and tracker **Notes** (remittance full-screen form follow-up). |
| 2026-04-02 | **URS-05** + **URS-07**: `CustomerFormScreen` (`customer_form/...`), `FarmOperationFormScreen` (`farm_op_form/...`); removed `FarmOperationDialog.kt`; history edit uses form route. |
| 2026-04-02 | **URS-06**: `AcquisitionFormScreen` (`acquisition_form/...`); list screen navigates to form; `AcquisitionDialog` removed from `AcquireProduceScreen`. |
| 2026-04-02 | **URS-08** (Phase 2): `NumericPadOutlinedTextField` / `ForDouble` / `ForNullableDouble`; Remittance amount + `PricingPresetEditorScreen` numeric fields on shared pad. |
| 2026-04-02 | **URS-09**: `EmployeePaymentScreen` expandable summary banner + print on top app bar and beside banner. |
| 2026-04-02 | **URS-10–URS-13** (Phase 2): preset detail structured sections; export N-table label; 2-col role chips; Active SRPs bottom print. |
| 2026-04-02 | **URS-14–URS-15** (Phase 3): `DateRangePicker` in order history filters; payment form IME Next/Done on gross / advance / liquidated. |

---

## Note on companion doc freshness

Some lines in `user_review_screens.md` (e.g. **MainScreen** UI-22, **Acquire** dialog layout) may lag the codebase. When implementing, **prefer this tracker + git truth**; update the companion doc in the same PR when behaviour changes materially.
