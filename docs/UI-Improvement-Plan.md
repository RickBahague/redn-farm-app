# UI Improvement Plan — RedN Farm App

**Primary target device:** Sunmi V2 Pro (handheld mobile POS, 5.99", 720×1440, portrait)  
**Created:** 2026-04-02  
**Implementation tracker:** [UI_IMPROVEMENT_TRACKER.md](./UI_IMPROVEMENT_TRACKER.md) (status, agent waves, errata).  
**Status legend:** 📋 planned · 🔧 in progress · ✅ done

---

## Device Context

The Sunmi V2 Pro is a handheld Android POS device. At 720×1440 and 5.99" it sits in the normal phone size class (~428dp wide × ~856dp tall) but is used in a commercial setting — not a casual personal phone.

| Factor | Implication |
|--------|-------------|
| 5.99" portrait, ~428dp wide | Single-column layouts; no two-panel side-by-side; 2-column grid max |
| Handheld, one or two-handed use | Primary actions must be in the bottom 1/3 (thumb reach); top-bar icons are secondary |
| Built-in 58mm thermal printer (bottom of device) | Print is a first-class action; receipt output is immediate and physical |
| Built-in 1D/2D barcode scanner (rear) | Product selection by scan is faster than search; integrate into acquisition + order flows |
| Soft keyboard occupies ~45% of screen height | Every form with numeric fields needs a custom numeric pad; all forms need `imePadding()` |
| Mobile use — store counter, field, moving around | Battery awareness; no assumption of WiFi; bright ambient lighting |
| Fingers, possibly gloves or damp hands (farm/market) | Touch targets ≥ 56dp; avoid small tap zones near screen edges |

---

## Improvement Items

Priority: **P1** = implement during current rebuild · **P2** = next iteration · **P3** = nice-to-have

---

### UI-01 — Dashboard: 2-column grid instead of scrolling list
**Priority:** P1  
**Screen:** `MainScreen`  
**Problem:** The dashboard is a single-column `Column` of `OutlinedCard` rows — a phone-style nav list that wastes vertical space and requires scrolling to see all 8 entries.  
**Fix:**
- Replace with `LazyVerticalGrid(GridCells.Fixed(2))` — 2 columns fit comfortably at 428dp wide.
- Each tile: icon (40dp), bold label, min height 100dp. No subtitle text needed (saves space).
- All 8 tiles fit in 4 rows with no scrolling.
- Sort tiles by frequency of use: Orders top-left, then Customers, Inventory, Farm Ops, Remittance, Employees, Products, Export.
- The top bar (profile, settings, logout) stays in the `TopAppBar`.

```
┌──────────┬──────────┐
│  Orders  │Customers │
├──────────┼──────────┤
│Inventory │ Farm Ops │
├──────────┼──────────┤
│Remittance│Employees │
├──────────┼──────────┤
│ Products │  Export  │
└──────────┴──────────┘
```

---

### UI-02 — Order screen: pinned bottom bar for total + place order
**Priority:** P1  
**Screen:** `TakeOrderScreen`  
**Problem:** The cart total and "Place Order" button are at the bottom of a vertically scrolling `Column`. When the cart has several items the total scrolls off screen, and the user cannot place the order without scrolling down first.  
**Fix:** Keep the layout single-column (correct for 428dp) but restructure so the total + action are always visible:
- Use a `Scaffold` with a `bottomBar` containing the order total and "Place Order" button.
- The cart `LazyColumn` fills the remaining space and scrolls independently.
- Customer selection and channel chips stay at the top, above the cart.

```
┌────────────────────────────┐  ← TopAppBar
│  [Customer]   [Channel]    │
│  ──────────────────────    │
│  Cart item 1               │
│  Cart item 2               │  ← scrollable
│  Cart item 3               │
│  ...                       │
├────────────────────────────┤
│  Total:        ₱ 1,250.00  │  ← pinned bottomBar
│  [    PLACE ORDER    ]     │  ← never scrolls away
└────────────────────────────┘
```

---

### UI-03 — Inline quantity stepper on cart items
**Priority:** P1  
**Screen:** `OrderItemCard`, `TakeOrderScreen`  
**Problem:** Changing a cart item quantity requires tapping to open an edit field — 3+ taps per change.  
**Fix:** Show `−` / value / `+` stepper inline on each cart row. Tapping `−` at 1 shows a brief "Remove?" confirmation inline (no separate dialog). Tapping `+` triggers the numeric pad (UI-04) if the product is sold by weight.

```
┌──────────────────────────────────────────┐
│ Tomatoes (kg)                            │
│ [−]  2.5 kg  [+]           ₱ 87.50  [×] │
└──────────────────────────────────────────┘
```

---

### UI-04 — Custom numeric pad for weight and price input
**Priority:** P1  
**Screens:** Acquisition form, order item quantity, any weight/price field  
**Problem:** The system soft keyboard on a 5.99" device occupies ~45% of screen height when triggered for `KeyboardType.Decimal` fields. On the acquisition form this hides most of the form fields. On order item entry it hides the product name and current total.  
**Fix:** Replace all weight/price text fields with a `ModalBottomSheet` numeric pad:
- 10 digit keys + decimal point + backspace, in a 3×4 grid.
- Keys minimum 64dp tall, full-width of sheet.
- Running value displayed at the top of the sheet in large text.
- "Done" button confirms; tapping outside dismisses.
- Sheet height is fixed (~50% of screen) — does not fight with the system keyboard.

This is the single highest-impact change for daily use speed. The soft keyboard must not appear for any weight or price field.

---

### UI-05 — Barcode scanner integration
**Priority:** P1  
**Screens:** Acquisition add/edit, order product selection  
**Problem:** The Sunmi V2 Pro has a built-in 1D/2D rear barcode scanner that is completely unused. Product selection currently requires opening a list dialog and searching by name — slow when handling produce.  
**Fix:**
- Register a broadcast receiver for Sunmi scanner output (`com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED`).
- On the acquisition form: a scan icon in the product field triggers the scanner. A successful scan matches the barcode against `products.product_id` or a `barcode` field (add `barcode: String?` to `ProductEntity` — store the product's standard barcode or QR code here).
- On the order product selection dialog: a persistent scan button at the top triggers the scanner and jumps the search directly to the matched product.
- If the scanned barcode does not match any product, show a `Snackbar`: "Product not found — search manually?"
- Hardware trigger button (side button on V2 Pro) should activate the scanner when these screens are in focus.

---

### UI-06 — `imePadding()` audit — forms scroll under soft keyboard
**Priority:** P1  
**Screens:** All forms with text input (login, customer add/edit, employee add/edit, farm operation dialog)  
**Problem:** When the soft keyboard appears, form fields below the fold get hidden behind the keyboard with no way to reach them without dismissing the keyboard first. This is caused by missing `imePadding()` / `imeNestedScroll()` on the form containers.  
**Fix:** For every screen or dialog that has text input:
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .imePadding()   // ← add this
) { ... }
```
Also set `WindowCompat.setDecorFitsSystemWindows(window, false)` in `MainActivity` to enable edge-to-edge and allow `imePadding()` to work correctly.

---

### UI-07 — Login screen: prevent keyboard from hiding the login button
**Priority:** P1  
**Screen:** `LoginScreen`  
**Problem:** The current layout has a 200dp image header followed by username, password, and login button. On a 5.99" screen in portrait, when the soft keyboard appears for the password field, the "Log In" button is pushed off screen. The user cannot see or tap it without dismissing the keyboard.  
**Fix:**
- Apply `imePadding()` and `verticalScroll` to the login column (same as UI-06 pattern).
- Reduce the header image to 120dp in height — the branding is recognisable at that size and gives the form more room.
- Alternative: show no header image when keyboard is visible (`WindowInsets.isImeVisible`), revealing the full form.

---

### UI-08 — Touch target audit
**Priority:** P1  
**All screens**  
**Problem:** Several interactive elements are below the 48dp minimum touch target size, which matters more on a handheld POS handled quickly:
- `FilterChip` for sales channel — label-only chips are small
- `TextButton("Save")` / `TextButton("Cancel")` dialog footers have the same visual weight as destructive actions
- Cart item remove/edit icon buttons — currently icon-only, no minimum size enforcement

**Fix:**
- All primary `Button` / `OutlinedButton`: `Modifier.fillMaxWidth().height(56.dp)`.
- "Place Order": `height = 64.dp`, `FilledButton` (not plain `Button`).
- `FilterChip` for channel: `Modifier.height(48.dp)` with horizontal padding.
- Icon-only buttons in list rows: wrap in `Box(Modifier.size(48.dp))` to guarantee hit area.

---

### UI-09 — Typography scale for handheld POS readability
**Priority:** P1  
**All screens**  
**Problem:** Default Material3 typography is designed for standard phone reading distance. On a POS device the user frequently glances while handling goods — key values (totals, product names, prices) need to be immediately readable.

**Recommended overrides in `FarmTheme`:**

| Element | Current | Recommended |
|---------|---------|-------------|
| Cart / order total | `titleMedium` (16sp) | `headlineSmall` (24sp, bold) |
| Product name in cart row | `bodyLarge` (16sp) | `titleMedium` (18sp, bold) |
| Price per unit in cart | `bodyMedium` | `titleSmall` (14sp, bold) |
| Order list — customer name | `titleMedium` | `titleMedium` (keep, already OK) |
| Section labels / captions | `labelMedium` | `labelLarge` |

Do not exceed 24sp for the total — at 720px wide, 28sp+ starts to crowd on the same row as the label.

---

### UI-10 — Colour theme — farm brand + outdoor contrast
**Priority:** P1  
**Theme:** `ui/theme/`  
**Problem:** Default Material3 purple has no farm context. Contrast is mediocre under the bright overhead lighting typical of markets and store counters.

**Recommended seed colours:**
- **Primary:** `#2E7D32` (Material Green 800) — farm green
- **Secondary:** `#F9A825` (Amber 800) — harvest/earthy tone
- **Error:** `#B71C1C` (Red 900) — high-contrast for destructive actions

Generate the full colour scheme via Material Theme Builder. Verify all `onSurface` / `onBackground` text meets WCAG AA (4.5:1). Pay particular attention to `onSurfaceVariant` — this is used for sub-labels and is often too light.

---

### UI-11 — Destructive actions use error colour
**Priority:** P1  
**Screens:** Delete confirmation dialogs, data clear screen  
**Problem:** Delete and Cancel share the same `TextButton` style — same size, same colour, equal visual weight. On a handheld device used quickly, accidental deletes happen.  
**Fix:**
```kotlin
Button(
    onClick = onConfirm,
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error
    )
) { Text("Delete") }
```

---

### UI-12 — Status chips on order list
**Priority:** P1  
**Screen:** `OrderHistoryScreen`, order cards  
**Problem:** Paid/delivered status is plain text — the user must read each item to find outstanding orders rather than scanning visually.  
**Fix:** Replace with small coloured `SuggestionChip`:
- **Paid** — green container, white text
- **Unpaid** — amber container, dark text
- **Delivered** — teal container, white text
- **Pending delivery** — surface variant, muted text

Position chips on the right side of the order card so they form a scannable right-edge column.

---

### UI-13 — Thumb-reach zone: primary actions at the bottom
**Priority:** P1  
**All screens**  
**Problem:** Several screens put the primary action in the `TopAppBar` or at the top of the content area — awkward to reach on a handheld, especially one-handed. Examples: the acquisition "Add" FAB is at the bottom (correct), but the form Save/Confirm actions inside dialogs are at the top right of the dialog.  
**Fix:**
- Primary action buttons (Save, Confirm, Place Order, Add Payment) always go at the bottom of their container.
- Dialogs and bottom sheets: action row at the bottom, not at the top.
- FABs: keep bottom-right position (already correct on some screens).
- Destructive secondary actions (Delete, Cancel): bottom-left; primary action: bottom-right.

---

### UI-14 — Dialogs → full-screen for complex forms
**Priority:** P1  
**Screens:** Payment dialog, acquisition add/edit dialog  
**Problem:** `AlertDialog` wrapping multi-field forms (amount, cash advance, liquidated, two date pickers, signature) is scrollable inside an overlay — the scroll-within-dialog pattern breaks on small screens and fights with nested date picker overlays.  
**Fix:** Replace with a dedicated `NavController`-managed screen composable. This gives:
- Full screen height and safe-area handling.
- Proper `imePadding()` (see UI-06).
- Standard Android back navigation.
- No scroll-within-dialog issues.

`AlertDialog` should be reserved for simple confirmations (1–2 lines of text + OK/Cancel).

---

### UI-15 — Print button: always visible, one tap
**Priority:** P1  
**Screens:** Order detail, order history list cards  
**Problem:** The built-in 58mm printer is the V2 Pro's most distinctive hardware feature, but the Print action is currently an `IconButton` tucked in the `TopAppBar` — easy to miss and in the hardest-to-reach thumb zone.  
**Fix:**
- On the order detail screen: `OutlinedButton("Print Receipt")` in the action row alongside "Mark Paid" / "Mark Delivered" — bottom of screen, full width.
- On the order history list: a `Print` icon button directly on each card (right side), not hidden behind a context menu.
- The print action should be available regardless of paid/delivered status.

---

### UI-16 — Net pay summary on payment form
**Priority:** P2  
**Screen:** Payment add/edit (after UI-14 makes it a full screen)  
**Problem:** The cashier must mentally compute net pay from three separate number fields.  
**Fix:** A live-computed read-only summary block below the input fields (**canonical:** gross + cash advance; liquidated is **not** in net pay — see `USER_STORIES.md` EMP-US-05 / `BUG-EMP-01`):

```
Gross wage:          ₱ 3,000.00
Cash advance:        ₱   500.00   (included in net pay)
Liquidated:          ₱   200.00   (recorded only; does not change net pay)
─────────────────────────────────
Net pay:             ₱ 3,500.00
```

Updates in real time as the user types. Optional amber highlight if net pay is invalid or negative per product rules.

---

### UI-17 — Empty states with CTAs
**Priority:** P2  
**Screens:** All list screens  
**Problem:** Empty lists show a blank screen with no guidance.  
**Fix:** Each list screen: centred icon + short message + action button.

| Screen | Icon | Message | CTA |
|--------|------|---------|-----|
| Orders | ShoppingCart | "No orders yet" | "Take first order" |
| Acquisitions | Inventory | "No acquisitions recorded" | "Record acquisition" |
| Customers | People | "No customers yet" | "Add customer" |
| Employees | Person | "No employees yet" | "Add employee" |
| Farm Ops | Agriculture | "No operations logged" | "Log operation" |

---

### UI-18 — Acquisition form: live SRP preview
**Priority:** P2 (implement with Phase 3 of rebuild)  
**Screen:** Acquisition add/edit  
**Problem:** The purchasing assistant has no feedback on what SRPs will be computed until after saving.  
**Fix:** A collapsible card below the cost fields — expands automatically when quantity and price fields are filled:

```
▼ SRP Preview  (Preset: "Q2 2026 Rates")
  Online:    ₱ 85/kg  ·  ₱ 43/500g  ·  ₱ 26/250g
  Reseller:  ₱ 78/kg  ·  ₱ 39/500g  ·  ₱ 23/250g
  Offline:   ₱ 82/kg  ·  ₱ 41/500g  ·  ₱ 25/250g
  Per piece: ₱ 12  (7 pcs/kg)
```

Shows "No active preset — SRPs will not be computed" if no preset is active.

---

### UI-19 — Role-filtered dashboard
**Priority:** P2 (depends on AUTH-US-04)  
**Screen:** `MainScreen`  
**Problem:** All tiles are shown to all users. A Farmer opening the app sees Orders, Export, and Employees — none of which they use.  
**Fix:** Filter tiles based on `userRole` from session:

| Role | Visible tiles |
|------|---------------|
| Admin | All 8 |
| Store Assistant | Orders, Customers, Remittance, Products (read-only) |
| Purchasing Assistant | Inventory, Products (read-only) |
| Farmer | Farm Operations |

---

### UI-20 — Signature field: finger-drawn signature
**Priority:** P2  
**Screen:** Payment form (after UI-14 conversion)  
**Problem:** Signature is a plain `OutlinedTextField`. EMP-US-02 requires finger-drawn signature or typed name.  
**Fix:**
- Default mode: a `Canvas` composable bordered box ("Sign here") with `Path` stroke capture.
- "Type instead" toggle switches to a text field.
- "Clear" resets the canvas.
- On save: canvas encoded as Base64 PNG, stored in `signature` column.
- Signature canvas should be at least 200dp tall to give enough room for a real signature.

---

### UI-21 — Active SRP reference screen
**Priority:** P2 (depends on Phase 3 of rebuild)  
**Screen:** New `ActiveSrpsScreen`  
**Problem:** Store assistant has no way to view current selling prices before taking an order (ORD-US-08).  
**Fix:** Single-column scrollable list of active products with their SRPs. Channel selector (`FilterChip` row) at the top updates all displayed prices.

```
[ Online ] [ Reseller ] [ Offline ]

Tomatoes
  Per kg:    ₱ 85
  Per 500g:  ₱ 43
  Per piece: —

Lettuce
  Per piece: ₱ 12
```

Footer: "Preset: Q2 2026 Rates · Activated Apr 1, 2026"

---

## Implementation Order

| Item | Rebuild Phase | Effort |
|------|--------------|--------|
| UI-01 Dashboard 2-column grid | Phase 1 | Small |
| UI-06 `imePadding()` audit | Phase 1 | Small |
| UI-07 Login keyboard fix | Phase 1 | Small |
| UI-08 Touch target audit | Phase 1 | Small |
| UI-09 Typography scale | Phase 1 | Small |
| UI-10 Colour theme | Phase 1 | Small |
| UI-11 Destructive colour | Phase 1 | Small |
| UI-12 Status chips | Phase 1 | Small |
| UI-14 Empty states | Phase 1 | Small |
| UI-02 Pinned bottom order bar | Phase 4 | Small |
| UI-03 Inline cart stepper | Phase 4 | Medium |
| UI-04 Numeric pad | Phase 4 | Medium |
| UI-05 Barcode scanner | Phase 4 | Medium |
| UI-13 Thumb-reach audit | Phase 4 | Small |
| UI-15 Print prominence | Phase 4 | Small |
| UI-14 Full-screen forms | Phase 2 | Medium |
| UI-16 Net pay summary | Phase 2 | Small |
| UI-17 Empty states | Phase 2 | Small |
| UI-18 SRP preview | Phase 3 | Medium |
| UI-19 Role dashboard | Phase 5 | Medium |
| UI-20 Drawn signature | Phase 5 | Large |
| UI-21 Active SRP screen | Phase 4 | Medium |
