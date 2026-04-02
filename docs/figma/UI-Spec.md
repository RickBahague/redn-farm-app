# Farm App UI Spec (Figma-Readable)

> Generated from current implementation of the following UI improvements:
> - UI-04 Numeric Pad
> - UI-18 Acquisition SRP preview (live)
> - UI-19 Role-filtered dashboard
> - UI-20 Finger-drawn signature field (payment)
> - UI-21 Active SRP reference screen (channel selector + preset footer)

---

## Global Notes (naming + interaction patterns)
- Currency formatting: `₱` PHP, typically 2 decimals.
- “Chips” = `FilterChip` row for mode/channel selection.
- “Sheet” = `ModalBottomSheet` used for the numeric pad.
- All fields that accept numeric input should prefer the numeric pad over the soft decimal keyboard.

---

## Component: Numeric Pad Bottom Sheet (UI-04)
**Component name (Figma):** `NumericPadBottomSheet`

### Frame description
**Type:** Bottom Sheet
**Visibility:** shown modally when tapping a numeric field

### Layout (copy/paste for Figma)
1. Header row
   - Left: `title` text (e.g., “Quantity”, “Price/Unit”, “Total Amount”)
   - Right: `X` close icon button
2. Read-only value display card
   - Large value text (uses the current `value`; displays `0` when empty)
3. Keypad grid (4 rows)
   - Row 1: `1 2 3`
   - Row 2: `4 5 6`
   - Row 3: `7 8 9`
   - Row 4:
     - `0`
     - decimal key `.`
     - trailing spacer (no-op)
4. Utility row (Backspace + Clear)
   - Backspace button: remove last character
   - Clear button: reset to empty
5. Footer action
   - `Done` button dismisses the sheet

### Behavior
- Decimal key `.`
  - Enabled when the field supports decimals (`decimalEnabled = true`)
  - Inserts `.` only once; restricts max decimals (`maxDecimalPlaces`)
- Backspace
  - Removes last character
- Clear
  - Sets value to empty string `""`

---

## Screen: Acquisition Add/Edit Dialog (UI-18 + UI-04)
**Screen name (Figma):** `Acquire Produce - Add/Edit Acquisition`

### Frame: Cost input section
**Inputs (replace soft keyboard with numeric pad):**
1. `Quantity` (read-only text field)
   - Tap opens `NumericPadBottomSheet`
2. `Price/Unit` (read-only text field, prefix `₱`)
   - Tap opens `NumericPadBottomSheet`
3. `Total Amount` (read-only text field, prefix `₱`)
   - Computed as `Quantity * Price/Unit`
   - Tap opens `NumericPadBottomSheet`

### Frame: Live SRP preview panel (collapsible, auto-expands)
**Component name (Figma):** `Acquisition SRP Preview Card`
**Header row:**
- Left: `SRP Preview`
- Right: expand/collapse chevron icon
- Header includes active preset name: `(Preset: <presetName>)`

**Card content (only when expanded):**
1. Sellable summary
   - `Sellable <X.XX> kg · cost/kg ₱<...>`
2. Channel SRPs
   - `Online: ₱<...>/kg · ₱<...>/500g · ₱<...>/250g`
   - `Reseller: ₱<...>/kg · ₱<...>/500g · ₱<...>/250g`
   - `Offline: ₱<...>/kg · ₱<...>/500g · ₱<...>/250g`
3. Optional per-piece SRPs
   - Only shown when acquisition is NOT per kg
   - `Per piece: Online ₱<...> · Reseller ₱<...> · Store ₱<...>`

**States:**
- `preview == null` (inputs missing)
  - “Enter quantity and price to preview SRPs.”
- `No active preset`
  - “No active preset — SRPs will not be computed.”
- `Invalid`
  - Shows preview error message in error color.

**Auto-expansion rule:**
- Expands automatically when `Quantity` and `Price/Unit` are both filled with valid > 0 numbers.

---

## Screen: Main Dashboard (Role-filtered tiles) (UI-19)
**Screen name (Figma):** `Main Dashboard`

### Frame: Top App Bar
- Title: `Yongy & Eyo's FARM`
- Actions:
  - Profile icon (always visible)
  - Settings icon (only for `ADMIN`)
  - About icon
  - Logout icon

### Frame: Menu tiles (2-column grid)
**Tiles present in base implementation:**
- Orders, Customers, Inventory, Farm Ops, Remittance, Employees, Products, Export

**Role filtering (visible tiles):**
- Admin:
  - All 8 tiles
- Store Assistant:
  - Orders, Customers, Remittance, Products
- Purchasing Assistant:
  - Inventory, Products
- Farmer:
  - Farm Ops

### Behavior
- Role is resolved from session; tiles update automatically after login/logout.

---

## Component: Signature Field (Finger Drawn + Type Mode) (UI-20)
**Component name (Figma):** `Employee Payment Signature Field`

### Frame: Mode toggle row
- Two `FilterChip` options:
  1. `Draw` (selected by default when `signature` looks like Base64/image)
  2. `Type`

### Frame: Draw mode
**Canvas box:**
- Bordered area with a “Sign here” style canvas (implemented with a draw area)
- Minimum height target: **~200dp**
- Clear button:
  - `Clear` resets the drawing and clears `signature`

**Stored value:**
- On stroke completion, exported as Base64 PNG and saved into the `signature` field.

### Frame: Type mode
- A normal single-line `OutlinedTextField` labeled `Signature`.

---

## Screen: Employee Payment Dialog (UI-20 + UI-04 relevance)
**Screen name (Figma):** `Employees - Payment Add/Edit`

### Layout summary
- Amount field
- Optional Cash Advance
- Optional Liquidated
- Signature field:
  - `Draw/Type` chips
  - Canvas or Text field depending on chip selection
- Date Paid + optional Date Received pickers

### Save button enable rule
- Save is enabled when:
  - `amount` parses as a number
  - `signature` is not blank

---

## Screen: Active SRPs Reference (UI-21)
**Screen name (Figma):** `Active SRPs`

### Frame: Channel selector (top chips)
**Row of FilterChips:**
- `Online`
- `Reseller`
- `Offline`

**Behavior:**
- Selecting a channel updates what SRP values are displayed in each product row.

### Frame: Product list (scrollable)
For each product card:
1. Product name (title)
2. Channel price summary:
   - Shows `Per kg`, plus per 500g and per 250g (based on selected channel)
3. Expansion affordance:
   - Card is clickable
   - Shows expanded content with additional details including per-piece price

### Footer: Active preset + activation timestamp
- Shows only when preset name is available:
  - `Preset: <presetName> · Activated <date-time>`

---

## Notes for Future Extensions (optional)
- UI-05 (barcode + receiver) and UI-04 (pad consistency across more screens) can reuse:
  - `NumericPadBottomSheet`
  - The same “field is read-only + opens pad” pattern.

