# Screen Review — UX & Interaction Patterns

**Created:** 2026-04-03  
**Device target:** Sunmi V2 Pro (5.99", handheld POS, one or two-handed use)  
**Scope:** All 24 app screens — patterns, friction, dialog vs. full-screen judgement, tap-count analysis  
**Tracker (stories + implementation plan):** [`user_review_screens_stories.md`](./user_review_screens_stories.md)

---

## Quick Reference

| Rating | Meaning |
|---|---|
| ✅ Good | Current pattern is correct for POS use |
| ⚠️ Friction | Works, but adds unnecessary taps or scroll |
| ❌ Problem | Pattern actively hurts the daily workflow |

---

## Pattern Decision Guide

Before screen-by-screen notes, the core question for every interaction:

| Situation | Best pattern | Avoid |
|---|---|---|
| Simple confirmation (1–2 lines) | `AlertDialog` | Full-screen route |
| Form with ≤ 4 short fields | `AlertDialog` with `imePadding` | Full-screen (overkill) |
| Form with ≥ 5 fields **or** any scrolling | Full-screen route | `AlertDialog` (scroll-in-dialog) |
| Number / weight entry | `ModalBottomSheet` numeric pad | Soft keyboard |
| Date selection | Inline `DatePicker` card or `ModalBottomSheet` | Nested dialog-within-dialog |
| Destructive action | `AlertDialog` with error-coloured confirm | Immediate execution |
| Status toggle (paid, delivered) | Direct toggle + `Snackbar("Undo")` | Confirmation `AlertDialog` per toggle |
| Running total / primary submit | `Scaffold.bottomBar` — always visible | Scroll-to-reach button |

---

## Screen-by-Screen Review

---

### LoginScreen
**Pattern:** Full-screen inline form  
**Status:** ✅ Good

Login is already a full-screen route. Button is full-width at the bottom. `imePadding()` and `verticalScroll` are in place so the "Log In" button stays visible when the keyboard appears (UI-07 done).

**No changes needed.**

---

### MainScreen (Dashboard)
**Pattern:** 2-column `LazyVerticalGrid` of `OutlinedCard` tiles  
**Status:** ✅ Good / one open item

Grid layout is correct. Role-filtered tiles (UI-19) and RBAC gate are implemented. UI-22 (label-top, icon-bottom tile layout) is pending — label can wrap on some tiles with the current `Row` layout.

**Suggestion:** Implement UI-22 (already documented). No other changes.

---

### ManageProductsScreen
**Pattern:** Add/Edit — `AlertDialog` with scrollable form; fallback price — `ModalBottomSheet` numeric pad  
**Status:** ⚠️ Friction — dialog form is too large

The product form in the dialog has at least 6 fields (name, description, unit type, category, piece count, is_active toggle) plus the price section. On a 5.99" screen a scrollable form inside a modal overlay is awkward — the scroll gesture competes with dismiss-by-swipe and the user can easily lose their position.

**Suggestion — split the dialog:**
- Keep `AlertDialog` only for the delete confirmation.
- Move add/edit to a **full-screen route** (`product_add_edit/{productId}` — `productId` = `"new"` for add). This is already the pattern used for `EditOrderScreen` and `PaymentFormScreen`.
- The fallback price `ModalBottomSheet` with numeric pad is **excellent** — keep it exactly as-is.

```
Before (dialog-in-modal, scrolling):
  Tap Add → AlertDialog opens → scroll inside dialog → Save
  
After (full-screen):
  Tap Add → push product_add_edit/new → full screen form → Save → pop back
```

**Tap count:** Same (2 taps to reach save). But full-screen gives proper `imePadding`, `verticalScroll`, and thumb-zone action placement.

---

### ManageCustomersScreen
**Pattern:** Add/Edit — `AlertDialog` with scrollable form (8+ fields incl. address)  
**Status:** ❌ Problem — too many fields in a dialog

Customer add/edit has: firstname, lastname, contact, customer type dropdown, address, city, province, postal code. That is 8 fields with a dropdown — this does not fit an `AlertDialog` on a 5.99" screen without scrolling.

**Suggestion — full-screen route:**
- Route: `customer_add_edit/{customerId}` (`"new"` for add)
- Group address fields under a collapsible `OutlinedCard` section ("Address details ▸") to reduce initial visual weight — the address is rarely changed after first entry.
- Customer type (`RETAIL` / `REGULAR` / `WHOLESALE`) should be a `SegmentedButtonRow` (3 options, single line) instead of a dropdown.

**Tap count improvement:** Same entry point. Eliminates scroll-in-dialog. Full IME handling.

---

### ManageEmployeesScreen
**Pattern:** Add/Edit — `AlertDialog` with 3 fields (firstname, lastname, contact)  
**Status:** ✅ Good

Three fields fit an `AlertDialog` without scrolling. The pattern is appropriate here.

**Minor suggestion:** Contact field should use `KeyboardType.Phone` with `ImeAction.Done` to dismiss keyboard and immediately focus the Save button.

---

### EmployeePaymentScreen (payment history list)
**Pattern:** View-only list with filter, summary, print, delete  
**Status:** ⚠️ Friction — filter and summary compete for screen real estate

The period filter dropdown + outstanding advance card + period summary card all stack before the payment list. On a short viewport the user must scroll past 3 stacked summary rows to reach the first payment entry.

**Suggestion:**
- Collapse the outstanding advance and period totals into a single `SummaryBanner` row (not a card) — two `Text` labels side by side: `"Outstanding: ₱2,000 · Net this period: ₱14,000"`. Tapping expands a detail card.
- Move the "Print Summary" button to the top bar (icon button, print icon) or next to the summary banner — not a separate full-width button.
- Period filter stays as-is (dropdown is appropriate for 4–5 choices).

---

### PaymentFormScreen (add/edit payment — full-screen route)
**Pattern:** Full-screen route with live net pay summary  
**Status:** ✅ Good

Moving the payment form from `AlertDialog` to a full-screen route (UI-14) was the right call. `imePadding` works correctly, signature canvas has full height, and the live net pay summary (UI-16) is always visible.

**Minor suggestion:** Add `ImeAction.Next` chaining between fields (amount → cash advance → liquidated → confirm) so the user can move through numeric fields without tapping.

---

### TakeOrderScreen (order entry)
**Pattern:** Customer picker dialog + product picker dialog; inline cart; `Scaffold.bottomBar` for total + Place Order  
**Status:** ✅ Good — best-designed screen in the app

The pinned `bottomBar` with running total is the correct POS pattern. Channel `FilterChip` row is inline. Inline `−`/value/`+` stepper on each cart item is correct.

**Remaining friction:**
1. **Product picker dialog** opens a full-product list — add a barcode scan trigger at the top (UI-05, pending).
2. **"Place Order" success flow** shows a dialog asking "New order or view orders?" — this is correct behaviour for a POS (keep it, don't remove).
3. The cart uses the numeric pad for weight input (UI-04 done) — verify `0.25` step increment for kg entries works on the device.

**No structural changes needed.**

---

### OrderHistoryScreen
**Pattern:** Searchable list with inline filter panel; card-based with print icon + status chips  
**Status:** ✅ Good

Filter panel toggles inline — not a modal. Status chips (paid/unpaid, delivered/pending) on the right edge of each card allow visual scanning without reading text. Print icon directly on the card is correct (UI-15 plan, PRN-07).

**Minor suggestion:** The date range filter uses two separate date pickers. Wrap them in a `DateRangePicker` compound component to reduce taps.

---

### OrderDetailScreen
**Pattern:** Scrollable detail view; paid/delivered as `Switch` with confirmation `AlertDialog` per toggle; print in two places  
**Status:** ⚠️ Friction — confirmation dialogs on status toggles

**Specific problem:** Tapping "Paid" switch → `AlertDialog` ("Mark as paid?") → confirm. Tapping "Delivered" switch → another `AlertDialog`. That is 4 taps to mark an order as both paid and delivered. On a POS device doing 20+ orders a day, this is significant friction.

**Suggestion — direct toggle with undo:**
```
Before: Switch tap → AlertDialog → Confirm   (2 taps per toggle)
After:  Switch tap → immediate state change + Snackbar("Marked as paid · Undo")   (1 tap)
```
The `Snackbar` action gives a 4-second undo window. This is the standard Material3 pattern for reversible state changes. Destructive or irreversible actions (delete) should keep the confirmation dialog.

**Print duplication** (top bar + body button) is correct — keep both.

---

### EditOrderScreen
**Pattern:** Full-screen route for editing order items  
**Status:** ✅ Good (full-screen route is correct for this complexity)

No change in interaction pattern needed. Verify that the channel picker and item quantity controls use the same `−`/`+` stepper pattern as `TakeOrderScreen`.

---

### AcquireProduceScreen (acquisitions)
**Pattern:** List of acquisition cards with expandable SRP detail; add/edit as `AlertDialog`; date picker nested in dialog  
**Status:** ⚠️ Friction — add/edit dialog is large; date picker nesting

The acquisition form has: product picker, quantity, price per unit, is_per_kg toggle, piece count, date, location — plus an SRP preview card if a preset is active. That is 7+ controls in a dialog, some of which open nested pickers.

**Suggestion — full-screen route:**
- Route: `acquisition_add_edit/{acquisitionId}` (`"new"` for add)
- Full screen gives room for the live SRP preview card (UI-18, already implemented as collapsible)
- Date field: single `OutlinedCard` date selector inline — no dialog-within-dialog
- Location: `SegmentedButtonRow` (FARM / MARKET / SUPPLIER / OTHER) — 4 options, single line, no dropdown needed

**Batch print** (PRN-08 in `printing.md`) is triggered from the top bar icon — keep that placement.

---

### RemittanceScreen
**Pattern:** Add/Edit — `AlertDialog` with amount + date + remarks; date picker nested inside dialog  
**Status:** ⚠️ Friction — nested date picker and missing delete confirmation

**Two issues:**

1. **Date picker nesting:** The remittance dialog has a clickable date card that opens a `DatePickerDialog` — two overlapping modals. This is confusing on a small screen.

   **Fix:** Use a `ModalBottomSheet` for the whole add/edit form, with the `DatePicker` rendered inline inside the sheet. The sheet gives enough vertical space. Or keep the `AlertDialog` but replace the nested date card with a simple `OutlinedTextField` that opens a `DatePicker` directly (no intermediate alert).

2. **No delete confirmation:** Delete fires immediately. Add a single-line `AlertDialog` confirm for delete (same pattern as other screens).

**The form itself (3 fields) is small enough to stay in a dialog/sheet.** No need for a full-screen route.

---

### FarmOperationsScreen
**Pattern:** List with filter panel; add/edit — `AlertDialog` with large form  
**Status:** ⚠️ Friction — farm operations form has many fields

Farm operation form: type (enum), date, area, weather condition, personnel (text), product link (optional), details (multiline text). That is 6 fields with a multiline textarea — the `details` field alone can be several lines long.

**Suggestion — full-screen route:**
- Route: `farm_op_add_edit/{operationId}`
- Operation type: `FilterChip` row or `SegmentedButtonRow` (7 types — use a 2-row chip grid)
- Weather: `DropdownMenu` (Sunny, Cloudy, Rainy, Windy — 4 options)
- Details: `OutlinedTextField` with `maxLines = 6` — needs full height, not cramped in a dialog
- Product link: searchable dropdown, optional, collapsible section

---

### ExportScreen
**Pattern:** Full-screen with grouped card sections; all actions require confirmation dialogs  
**Status:** ✅ Good — confirmation dialogs are appropriate for destructive operations

Export, truncate, and generate-sample-data are admin-only, infrequent, and potentially destructive — confirmation dialogs are correct here. Destructive buttons using error colour is also correct.

**Minor suggestion:** The "Select all / Select none" TextButtons and checkbox list are good. Add a count badge next to the Export button ("Export 7 tables") so the admin can see what's selected at a glance without scrolling through the list.

---

### ProfileScreen
**Pattern:** View-only with navigation cards  
**Status:** ✅ Good — simple and correct

---

### ChangePasswordScreen
**Pattern:** Full-screen route with 3 fields  
**Status:** ✅ Good

Three fields, full-width submit at bottom, field-level error messages. Correct pattern.

---

### UserManagementScreen
**Pattern:** FAB → `AlertDialog` for create user; inline card actions for deactivate/reset  
**Status:** ✅ Good

User creation has 4 fields + role picker — borderline for a dialog, but because role uses `FilterChip` (not a dropdown) it stays compact. FAB is the correct affordance for an admin "add" action.

**Minor suggestion:** The 5-role `FilterChip` list inside the dialog may require vertical scroll on smaller viewports. Consider a 2-column chip grid (2×3 for 5 roles) instead of a single column.

---

### SettingsScreen
**Pattern:** Single navigation card  
**Status:** ✅ Good — will grow as more settings are added

---

### PricingPresetsHomeScreen
**Pattern:** Info display + 2 buttons  
**Status:** ✅ Good

---

### PricingPresetEditorScreen
**Pattern:** Full-screen scrollable form — very long  
**Status:** ⚠️ Friction — Save is in the top bar, form is very long

The editor is correctly a full-screen route (the form is far too complex for a dialog). The friction is that **Save is a `TextButton` in the `TopAppBar`** — after scrolling through the entire long form, the user must scroll back to the top to save, or remember the top-bar button exists.

**Suggestion:** Add a `Scaffold.bottomBar` with a `FilledButton("Save preset")` that duplicates the top-bar save action. Keep the top-bar `TextButton` too (same as `OrderDetailScreen` print duplication pattern). The bottom button is in thumb-reach after scrolling to the end of the form.

Also: the hauling fee rows and channel fee rows each have an inline `OutlinedButton("Add fee line")` — these are correct. The `IconButton(Delete)` on each row needs to be at least 48dp (verify).

---

### PresetHistoryScreen
**Pattern:** List of preset cards; click → full-screen detail  
**Status:** ✅ Good

---

### PresetDetailScreen
**Pattern:** Full-screen read-only view; Restore / Activate / Delete actions at bottom  
**Status:** ⚠️ Minor — JSON fields are not user-readable

Hauling fees, channels, and categories are displayed as raw JSON strings. Admin feature, but still poor readability.

**Suggestion:** Parse and render the JSON fields in the same structured format as the editor (or at minimum pretty-print them with indentation). This is a P2 polish item, not a blocker.

**Activate flow** (Detail → Activate → Preview → Confirm — 3 taps) is appropriate for a high-stakes admin action.

---

### PresetActivationPreviewScreen
**Pattern:** Full-screen preview + full-width Confirm / Cancel  
**Status:** ✅ Good

---

### ActiveSrpsScreen
**Pattern:** Channel `FilterChip` row; expandable product cards; print in top bar  
**Status:** ✅ Good

Expandable cards with `AnimatedVisibility` give good information density — summary visible at a glance, full breakdown on tap. Channel filter is inline (not modal).

**Minor suggestion:** The top-bar print icon is correct but small. If this screen is used frequently by store assistants to check prices, consider adding a full-width `OutlinedButton("Print Price List")` at the bottom (same pattern as `OrderDetailScreen`'s print duplication).

---

## Cross-Cutting Issues

### 1. Screens that should move from `AlertDialog` to full-screen route

| Screen | Fields in dialog | Recommendation |
|---|---|---|
| ManageProductsScreen | 6+ | Full-screen route `product_add_edit/{id}` |
| ManageCustomersScreen | 8+ | Full-screen route `customer_add_edit/{id}` |
| AcquireProduceScreen | 7+ | Full-screen route `acquisition_add_edit/{id}` |
| FarmOperationsScreen | 6+ multiline | Full-screen route `farm_op_add_edit/{id}` |

### 2. Screens where `AlertDialog` is correct (keep)

| Screen | Fields | Why it's fine |
|---|---|---|
| ManageEmployeesScreen | 3 | Fits without scrolling |
| RemittanceScreen | 3 | Fits without scrolling (fix nested date picker) |
| UserManagementScreen | 4 + role chips | Borderline — acceptable with chip grid |
| Delete confirmations | 0 (text only) | Always `AlertDialog` |

### 3. Status toggle pattern — replace dialog with Snackbar undo

Currently: `Switch` → `AlertDialog` ("Are you sure?") → Confirm  
Recommended: `Switch` → immediate change → `Snackbar("Marked as paid · Undo")`

Applies to: **OrderDetailScreen** (paid, delivered). Saves 1 tap per toggle per order.

### 4. Numeric input — standardize on `ModalBottomSheet` numeric pad

Already done: `ManageProductsScreen` (fallback price)  
Missing: `RemittanceScreen` (amount), `PricingPresetEditorScreen` (all numeric fields)

All currency and weight fields should trigger the `ModalBottomSheet` numeric pad instead of the soft keyboard. The numeric pad is already built — it just needs to be wired to more fields.

### 5. Date picker — no nested modals

Pattern to eliminate: `AlertDialog` → tap date card → `DatePickerDialog` (2 overlapping modals)  
Correct pattern: Single `ModalBottomSheet` containing the form + inline `DatePicker`  
Or: `OutlinedTextField` that opens a `DatePicker` directly (no intermediate alert)

Applies to: **RemittanceScreen**, **AcquireProduceScreen** (if still in dialog)

### 6. Long form — Save button visibility

Problem: `PricingPresetEditorScreen` has Save in the top bar only. After scrolling to the bottom of a long form the user can't see the save button.  
Fix: Duplicate Save as a `Scaffold.bottomBar` `FilledButton` (keep top-bar `TextButton` too).

Applies to: **PricingPresetEditorScreen**. May also apply to the new full-screen routes for Products, Customers, Acquisitions, FarmOps once they're created.

---

## Priority Order

| Priority | Change | Screens affected | Effort |
|---|---|:---:|---|
| **P1** | `AlertDialog` → full-screen for large forms | Products, Customers, Acquisitions, FarmOps | Medium each |
| **P1** | Fix nested date picker (no dialog-within-dialog) | Remittance, Acquisitions | Small |
| **P1** | Status toggle → direct + Snackbar undo | OrderDetail | Small |
| **P1** | Duplicate Save to bottom bar on long full-screen forms | PricingPresetEditor + new forms | Small |
| **P2** | `ModalBottomSheet` numeric pad for remaining currency fields | Remittance, PricingEditor | Small |
| **P2** | Collapse EmployeePayment summary to banner row | EmployeePaymentScreen | Small |
| **P2** | Render preset JSON fields as structured UI | PresetDetailScreen | Medium |
| **P2** | Add count badge on Export button | ExportScreen | Trivial |
| **P2** | 5-role chip grid (2-col) in UserManagement create dialog | UserManagementScreen | Small |
| **P3** | `DateRangePicker` compound component | OrderHistory | Medium |
| **P3** | `ImeAction.Next` chaining on PaymentFormScreen fields | PaymentFormScreen | Small |
