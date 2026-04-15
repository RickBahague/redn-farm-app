# Bugs & Fix Tracker

**Created:** 2026-04-15  
**Purpose:** Track open defects with enough detail to implement and verify fixes.

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Fixed in codebase |
| `[~]` | Mitigated / partial |
| `[ ]` | Not fixed yet |

---

## BUG-ORD-09 — Order "Add Product" uses dialog; should be full-screen flow

### Report
- **Screen/flow:** `TakeOrderScreen` -> **Add Product** -> `ProductSelectionDialog`
- **Current behavior:** Tapping **Add Product** opens an `AlertDialog`-style product picker (`Select Product` / `Add Product`), which constrains visible space for search, product list, and quantity actions.
- **Issue:** This conflicts with the app's POS UX rule that complex entry flows should be full-screen and keyboard-safe, not scrollable dialogs.

### Expected behavior
- Tapping **Add Product** in Order opens a dedicated **full-screen** route (Compose screen), not a dialog box.
- Product search, list, and add controls should be laid out with full-height content and bottom-anchored primary actions where applicable.
- Back navigation should behave like a normal screen transition (back returns to order screen with current cart intact).

### Severity / type
- **Severity:** Medium
- **Type:** UI/UX consistency and usability

### Suspected root cause
- Add-product selection is still implemented via `ProductSelectionDialog` in order flows, likely inherited from earlier dialog-based patterns.

### Fix direction
1. Create a dedicated order product-picker screen (for example, `order_product_picker` route).
2. Move dialog content from `ProductSelectionDialog` into that screen composable.
3. Update `TakeOrderScreen` (and optionally edit-order flow for parity) to navigate to the full-screen picker.
4. Preserve selected product and quantity behavior; return result to originating screen.

### Candidate files
- `app/src/main/java/com/redn/farm/ui/screens/order/TakeOrderScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/ProductSelectionDialog.kt`
- `app/src/main/java/com/redn/farm/navigation/NavGraph.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/history/EditOrderScreen.kt` *(if parity update is included)*

### Verification target
- From Order screen, **Add Product** opens full-screen picker (no dialog chrome).
- Product search and list are fully usable on handheld portrait without dialog clipping.
- Add item succeeds and returns to order screen with cart updated.
- System back from picker returns to order screen without data loss.

### Fix *(implemented 2026-04-15)*
- Added full-screen `OrderProductPickerScreen` and new navigation route `order_product_picker`.
- Updated `TakeOrderScreen` Add Product action to navigate to the full-screen picker instead of opening `ProductSelectionDialog`.
- Wired picker to the same `TakeOrderViewModel` instance via `NavGraph` back-stack owner sharing, so selected items are added directly to the current cart before returning.

### Verification
- `./gradlew :app:compileDebugKotlin` ✅

**Status:** `[x]`

---

## BUG-ORD-13 — Take Order "Select Customer" still uses dialog; should be full-screen flow

### Report
- **Screen/flow:** `TakeOrderScreen` -> customer card -> `CustomerSelectionDialog`
- **Current behavior:** Selecting customer opens an `AlertDialog` picker with search and list.
- **Issue:** This is inconsistent with the new full-screen order selection/edit patterns (`Add Product`, `Edit Item`) and can feel cramped on handheld POS screens.

### Expected behavior
- Tapping the customer card opens a full-screen customer picker (not dialog chrome).
- Search and customer list should use full-height layout with standard back behavior.
- Picking a customer returns to `TakeOrderScreen` with selection applied and cart/session state preserved.

### Severity / type
- **Severity:** Medium
- **Type:** UI/UX consistency

### Candidate files
- `app/src/main/java/com/redn/farm/ui/screens/order/TakeOrderScreen.kt`
- `app/src/main/java/com/redn/farm/ui/screens/order/CustomerSelectionDialog.kt`
- `app/src/main/java/com/redn/farm/navigation/NavGraph.kt`

### Verification target
- Customer picker opens full-screen from Take Order.
- Selecting customer applies customer and channel defaults correctly.
- Back/cancel returns to Take Order without data loss.

### Fix *(implemented 2026-04-15)*
- Added full-screen `OrderCustomerPickerScreen` and new route `order_customer_picker`.
- Updated `TakeOrderScreen` customer card action to navigate to the full-screen picker instead of opening `CustomerSelectionDialog`.
- Wired picker to the same `TakeOrderViewModel` instance through `NavGraph` shared back-stack entry, so selecting a customer applies directly to current order state and returns.

### Verification
- `./gradlew :app:compileDebugKotlin` ✅

**Status:** `[x]`

---

## BUG-ORD-12 — Edit Order "Edit Item" still uses dialog; should be full-screen flow

### Report
- **Screen/flow:** `EditOrderScreen` -> item row -> **Edit** -> `EditOrderItemDialog`
- **Current behavior:** Item editing opens an `AlertDialog`-style form for quantity/unit changes.
- **Issue:** This is inconsistent with the full-screen POS pattern now used for Add Product flows in order screens.

### Expected behavior
- Edit Item should open as a full-screen flow with standard back/save behavior.
- Quantity numeric pad, unit toggle, and pricing preview should remain available.
- Returning/saving should preserve the current Edit Order session state.

### Severity / type
- **Severity:** Medium
- **Type:** UI/UX consistency

### Candidate files
- `app/src/main/java/com/redn/farm/ui/screens/order/history/EditOrderScreen.kt`

### Verification target
- Tapping Edit Item opens a full-screen editor (not dialog chrome).
- Saving updates the selected line item and recalculates totals.
- Back/cancel returns to Edit Order without data loss.

### Fix *(implemented 2026-04-15)*
- Replaced `EditOrderItemDialog` with full-screen `EditOrderItemScreen` in `EditOrderScreen`.
- Moved edit-item flow to full-screen overlay state (rendered before the main scaffold), matching the Add Product full-screen pattern.
- Preserved quantity numeric pad, kg/pc toggle, unit price, and line-total preview.
- Save updates the edited line item and recalculates order total; back returns to edit-order without losing session state.

### Verification
- `./gradlew :app:compileDebugKotlin` ✅

**Status:** `[x]`

---

## BUG-ORD-10 — Edit Order "Add Product" still uses dialog; should be full-screen flow

### Report
- **Screen/flow:** `EditOrderScreen` -> **Add Product** -> in-file `ProductSelectionDialog`
- **Current behavior:** Add Product in edit-order opens an `AlertDialog` picker, which constrains search + list + quantity controls.
- **Issue:** This is inconsistent with the full-screen POS pattern now used in `TakeOrderScreen`.

### Expected behavior
- In `EditOrderScreen`, Add Product opens a full-screen picker flow (not dialog chrome).
- Product list/search and quantity entry should use full-height layout and keep numeric pad behavior.
- Returning from picker should preserve edit-order context and append the selected line item.

### Severity / type
- **Severity:** Medium
- **Type:** UI/UX consistency and usability

### Candidate files
- `app/src/main/java/com/redn/farm/ui/screens/order/history/EditOrderScreen.kt`

### Verification target
- Add Product from edit-order uses full-screen picker.
- Add selected product returns to edit-order with line item appended and total updated.
- Back from picker returns to edit-order without data loss.

### Fix *(implemented 2026-04-15)*
- Replaced dialog-based add-product flow in `EditOrderScreen` with a full-screen picker composable (`EditOrderProductPickerScreen`) rendered in-screen.
- Updated Add Product trigger to open the full-screen picker state instead of `AlertDialog`.
- Preserved existing behavior for product search, unit toggle (kg/pc), numeric pad quantity entry, and line-total preview.
- On add, selected item is appended to `orderItems`, order total is recalculated, edit state is preserved, and picker closes back to edit-order content.

### Verification
- `./gradlew :app:compileDebugKotlin` ✅

**Status:** `[x]`

