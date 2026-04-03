# Printing — RedN Farm App

**Created:** 2026-04-03  
**Device:** Sunmi V2 Pro — built-in 58mm thermal printer (bottom of device)  
**Infrastructure:** `utils/PrinterUtils.kt`

---

## Hardware & Infrastructure

### Sunmi 58mm thermal printer

| Property | Value |
|---|---|
| Roll width | 58 mm |
| Usable print width | ~48 mm |
| Default font (23pt) | ~32 chars per line |
| Large font (40pt, bold) | ~16 chars per line |
| Paper cut | `cutPaper()` — automatic cutter after each print job |
| Connection | Bound Android service (`InnerPrinterManager`) — survives across screens |

A 58mm monospace line at the default font fits approximately **32 characters**. Every content template in this document uses that as the column budget. Use `-` or `=` for dividers (32 chars).

### `PrinterUtils` modes

| Method | Output | Use case |
|---|---|---|
| `printMessage(context, text, isLarge)` | Sunmi thermal, raw text | Receipts and slips — immediate physical output |
| `printText(context, text)` | Android `PrintManager` (A4, HTML) | Not used for POS; useful for admin reports if a Bluetooth/WiFi printer is connected |

All new print actions should use `printMessage()`. Call it from a `scope.launch { }` inside the Composable; the coroutine suspends until the printer service binds.

### 58mm content conventions

```
================================  ← 32 × "="  (section divider)
--------------------------------  ← 32 × "-"  (light divider)
LABEL           VALUE             ← left label + right-aligned value
```

Left-pad amounts using `String.format("%-20s %10s", label, amount)` to get flush-right values in 32 chars.

---

## Current Implementation

Shared builders live in **`ThermalPrintBuilders.kt`** (32-column thermal helpers + slip bodies). New slips use **`PrinterUtils.printMessage(..., alignment = 0)`** (left-aligned body). Snackbars report success/failure on the screens below.

---

### CUR-01 — Order Receipt
**Screen:** `OrderDetailScreen` (route `order_detail/{orderId}`)  
**Trigger:** Two independent triggers — both call `PrinterUtils.printMessage()` with the same content:
- Top bar `IconButton` (Print icon) — always visible
- Full-width `OutlinedButton("Print Receipt")` — in the body action row below the item list

**Who uses it:** Store assistant prints the customer's receipt at order completion or on request

**Content:**
```
REDN GREENS FRESH
Order #42
Channel: Online
Date: Apr 3, 2026
Customer: Juan dela Cruz
Contact: 09171234567
--------------------------------
Tomatoes - PHP 350.00
5.0kg x PHP 70.00
Lettuce - PHP 120.00
3pc x PHP 40.00
--------------------------------
Total: PHP 470.00
PAID
NOT DELIVERED
```

**Notes:**
- Content is built inline inside the `onClick` lambda (duplicated between both triggers — same `buildString` block at lines ~90–107 and ~218–242 of `OrderDetailScreen.kt`)
- No error handling — `printMessage()` return value is not checked; a failed print has no UI feedback
- No business address, no "Thank you" footer, no separator after status lines
- `orderItems` is collected from a `StateFlow` on the screen — items must already be loaded when print is triggered
- Receipt text is built with **`buildOrderReceiptText()`**; print uses **left alignment** + snackbar on success/failure

---

### CUR-02 — Order Summary (aggregate)
**Screen:** `OrderSummaryDialog` — launched from `OrderHistoryScreen` via a summary button  
**Trigger:** `TextButton("Print")` inside the `AlertDialog`; shows "Printing…" while the coroutine runs and re-enables on completion

**Who uses it:** Store manager or owner prints an end-of-day or end-of-period aggregate across all filtered orders

**Content:**
```
ORDER SUMMARY
=============

PRODUCTS (3 unique items)
-------------------------
Tomatoes             55.00 kg
Lettuce              12.00 kg
Kangkong              8.25 kg

SUMMARY
-------
Total Kilograms: 75.25 kg
Total Pieces: 24 pc
Unique Customers: 6
Gross Est: PHP 28,450.00
```

**Notes:**
- Product names truncated to 20 chars (`substring(0, 17) + "..."`) to fit the 32-char line with quantity column
- Quantity formatted as `%6.2f` (right-aligned 6-wide with 2 decimal places)
- Kg and piece totals shown conditionally — only the lines that are non-zero appear
- `isPrinting` guard prevents double-taps from firing two print jobs
- Uses `formatSummaryForPrint()` private function inside `OrderSummaryDialog.kt`
- Print uses **left alignment** (`alignment = 0`)

---

### CUR-03 — Recommended slips (PRN-01–08) — **implemented**

| ID | Screen / trigger | Main files |
|----|------------------|------------|
| PRN-01 | `PaymentFormScreen` → **Print Voucher** (bottom bar; requires gross + signature) | `PaymentFormScreen.kt`, `buildEmployeePaymentVoucher` |
| PRN-02 | `AcquireProduceScreen` → acquisition card **Print** icon | `AcquireProduceScreen.kt`, `buildAcquisitionReceivingSlip` |
| PRN-08 | `AcquireProduceScreen` → app bar **ListAlt** = print **filtered acquisition report** | `AcquireProduceScreen.kt`, `buildAcquisitionBatchReport` |
| PRN-03 | `RemittanceScreen` → row **Print** icon | `RemittanceScreen.kt`, `buildRemittanceSlip` |
| PRN-04 | `ActiveSrpsScreen` → app bar **Print** (current channel; snackbar if no SRP rows) | `ActiveSrpsScreen.kt`, `buildSrpPriceList` |
| PRN-05 | `FarmOperationsScreen` + `FarmOperationHistoryScreen` → card **Print** icon | `FarmOperationCard.kt`, `buildFarmOperationLog` |
| PRN-06 | `EmployeePaymentScreen` → **Print Summary** (under period filter) | `EmployeePaymentScreen.kt`, `buildEmployeePayrollSummary` |
| PRN-07 | `OrderHistoryScreen` → order card **Print** (loads order + items via `getOrderSnapshotForPrint`) | `OrderHistoryScreen.kt`, `OrderHistoryViewModel.kt` |

---

## Recommended Screens

Priority: **P1** = high operational value, straightforward content · **P2** = useful, slightly more complex

---

### PRN-01 — Employee Payment Voucher
**Priority:** P1  
**Screen:** `PaymentFormScreen` (route `employee_payment_form/...`)  
**Trigger:** `OutlinedButton("Print Voucher")` at the bottom of the form, available after a successful save  
**Who uses it:** Store manager hands the printout to the employee as proof of payment

**Content:**
```
================================
  REDN GREENS FRESH
  PAYMENT VOUCHER
================================
Employee : Maria Santos
Date Paid: 2026-04-03
Received : 2026-04-05
--------------------------------
Gross Wage       PHP  3,000.00
Cash Advance     PHP    500.00
--------------------------------
NET PAY          PHP  3,500.00
================================
Liquidated       PHP    200.00
  (audit/outstanding only)
--------------------------------

Signature: ___________________

================================
```

**Notes:**
- Net pay = `amount + cash_advance_amount` (per EMP-US-05 / BUG-EMP-01)
- Include `liquidated_amount` as a separate informational line (not in the net pay computation)
- Signature field on printout is a blank line — the drawn/typed signature in the app is the digital record; the printed copy is for physical handoff
- Print is only available *after* save — disable button while `isSaving`

---

### PRN-02 — Acquisition Receiving Slip
**Priority:** P1  
**Screen:** `AcquireProduceScreen` — acquisition card (after save) or add/edit form confirmation  
**Trigger:** `OutlinedButton("Print Slip")` on the acquisition card row (expand area), available on any saved acquisition  
**Who uses it:** Purchasing assistant prints and attaches to the physical stock on arrival

**Content:**
```
================================
  REDN GREENS FRESH
  RECEIVING SLIP
================================
Acq ID : 0042
Date   : 2026-04-03
Product: Tomatoes
Location: SUPPLIER
--------------------------------
Quantity : 100.00 kg
Price/kg : PHP    70.00
TOTAL    : PHP 7,000.00
--------------------------------
Preset   : Q2 2026 Rates
SRP Online : PHP 137.00/kg
SRP Reseller: PHP 127.00/kg
SRP Offline: PHP 132.00/kg
================================
Received by: ___________________
================================
```

**Notes:**
- SRP section only printed when `preset_ref` is non-null (acquisition was computed)
- Show `—` for SRP fields that are null (null spoilage/no preset)
- "Received by" line provides a physical sign-off slot
- For a **full run of acquisitions** matching the same filters as the list (search, location, date range), see **PRN-08 — Acquisition batch report**

---

### PRN-08 — Acquisition batch report (filtered / full list)
**Priority:** P1  
**Screen:** `AcquireProduceScreen`  
**Trigger (implemented):** Top app bar **`IconButton`** with **`ListAlt`** (“Print filtered acquisition report”) — prints **every acquisition row currently shown** using the same filtered list as the grid (`searchQuery`, `selectedLocation`, `selectedDateRange`). Empty list → snackbar *"Nothing to print — adjust filters."* Payload over **16k chars** → *"List too long — narrow filters."*  
**Who uses it:** Purchasing or ops prints one thermal document covering **all** matching receipts for the day, supplier, or search (e.g. audit, handoff to accounting, physical binder).

**Purpose:** Same **detail family** as **PRN-02** (per line: id, dates, product, location, qty, unit price, line total, optional SRP snapshot), repeated for **each** filtered acquisition, plus a **report header** and **roll-ups** so it works as a batch / full acquisition report.

**Suggested content format (32-char thermal):**

```
================================
  REDN GREENS FRESH
  ACQUISITION REPORT
================================
Printed : 2026-04-03 14:05
Filter  : Loc SUPPLIER · Search
          "tomato"
Date    : Apr 1 – Apr 3, 2026
--------------------------------
Records  : 12
TOTAL   : PHP 84,250.00
================================
--- Line 1 / 12 ---
Acq ID : 0042
Date   : 2026-04-03
Product: Tomatoes
Qty    : 100.00 kg
Total  : P 3000.00 (from total_amount)
Location: SUPPLIER
--------------------------------
--- Line 2 / 12 ---
Acq ID : 0041
...
================================
END OF REPORT
Verified by: _________________
================================
```

**Formatting rules:**
- **Header block:** Document title **ACQUISITION REPORT** (or **BATCH RECEIVING REPORT**). **Printed** = device local date-time. **Filter** = human-readable summary of active filters (truncate/wrap at 30 cols with continuation prefix `>` or indent; if no search, omit that line). **Date** = acquisition date-range filter if set, else *"All dates"* or omit.
- **Roll-up:** **Lines** = count of acquisitions in the filtered set. **TOTAL** = `sum(total_amount)` over those rows.
- **Per acquisition:** Repeat a **mini slip** patterned on PRN-02: `--- Line i / N ---` divider, then Acq ID, `date_acquired` (ISO or short), `product_name` (truncate ~18 chars; wrap name on second line if needed), `location` enum label, `quantity` + `kg`/`pcs`, `price_per_unit` with `/kg` or `/pc`, **Line** = `total_amount`. **Preset** line only if `preset_ref != null`; **SRP** = one compressed line if any per-kg SRPs exist (e.g. `SRP On : 137/kg · Res 127 · Off 132`), else `—`. Omit pack-tier SRP lines in v1 to save paper; optional v2 expand per PRN-02.
- **Long jobs:** If the string exceeds practical length, split into **multiple print jobs** (cut between acquisitions) or warn *"List too long — narrow filters"* (product decision).
- **Footer:** Single **Verified by** for the whole batch (not per line).

**Implementation notes:**
- Reuse `ThermalPrintBuilders` helpers (`thermalDividerHeavy`, `formatThermalLine`, `buildAcquisitionReceivingSlip` field logic extracted or shared) to avoid drift from PRN-02.
- Data source = the **same** `List<Acquisition>` (or Flow snapshot) used to render the filtered grid after filters apply.

---

### PRN-03 — Remittance Slip
**Priority:** P1  
**Screen:** `RemittanceScreen`  
**Trigger:** `IconButton` (Print icon) on each remittance row in the list, or inside an add/edit confirmation  
**Who uses it:** Owner documents cash remitted to a supplier or staff member

**Content:**
```
================================
  REDN GREENS FRESH
  REMITTANCE SLIP
================================
Remittance # : 0015
Date         : 2026-04-03
Amount       : PHP  5,000.00
--------------------------------
Remarks:
Payment to hauler - April run

================================
Acknowledged: ________________
================================
```

**Notes:**
- Simple content — minimal formatting needed
- "Acknowledged" signature line for the recipient

---

### PRN-04 — SRP Price List
**Priority:** P1  
**Screen:** `ActiveSrpsScreen`  
**Trigger:** Top bar `IconButton` (Print icon)  
**Who uses it:** Store assistant posts the price list at the counter or hands it to a reseller

**Content (per selected channel or all channels):**
```
================================
  REDN GREENS FRESH
  PRICE LIST — ONLINE
================================
Preset: Q2 2026 Rates
As of: 2026-04-03
--------------------------------
Product          /kg    /500g
--------------------------------
Tomatoes       137.00   69.00
Lettuce        145.00   73.00
Kangkong        98.00   49.00
--------------------------------
* All prices in PHP
================================
```

**Notes:**
- Print the currently selected channel's prices (Online / Reseller / Offline)
- If "per piece" products are in the list, add a second column pass for `/pc`
- Truncate product names to 16 chars to fit the 32-char line with two price columns
- If no active preset, show: `"No active preset — SRPs not computed"`

---

### PRN-05 — Farm Operations Log
**Priority:** P2  
**Screen:** `FarmOperationsHistoryScreen` or individual operation detail  
**Trigger:** `IconButton` (Print icon) on each operation row  
**Who uses it:** Farmer prints a daily/operation log for physical records or compliance

**Content:**
```
================================
  REDN GREENS FRESH
  FARM OPERATION LOG
================================
Date     : 2026-04-03
Type     : HARVESTING
Product  : Tomatoes
Area     : Block 3 / North Field
Personnel: Juan, Pedro, Maria
Weather  : Sunny
--------------------------------
Details:
Harvested approx 150kg. Some
blight observed on Row 4.
================================
```

**Notes:**
- "Details" field is free text; wrap at 30 chars per line (leave 2-char indent)
- Personnel is a comma-separated string — print as-is

---

### PRN-06 — Employee Period Payment Summary
**Priority:** P2  
**Screen:** `EmployeePaymentScreen` (payment history list for one employee)  
**Trigger:** `OutlinedButton("Print Summary")` in the period filter header row  
**Who uses it:** Manager prints a monthly payroll summary per employee

**Content:**
```
================================
  REDN GREENS FRESH
  PAYROLL SUMMARY
================================
Employee : Maria Santos
Period   : Mar 2026
Payments : 4
--------------------------------
Total Gross Wage : PHP 12,000.00
Total Advance    : PHP  2,000.00
--------------------------------
TOTAL NET PAY    : PHP 14,000.00
================================
Outstanding Advance: PHP 1,800.00
================================
Prepared by: __________________
================================
```

**Notes:**
- "Outstanding advance" = lifetime outstanding (all-time, unfiltered) — same figure already shown on `EmployeePaymentScreen`
- "Total net pay" = sum of per-payment `netPayAmount()` for the selected period
- Period label follows the existing filter state: "Today", "This Week", "Mar 2026", etc.

---

### PRN-07 — Order Receipt from History Card (quick reprint)
**Priority:** P2  
**Screen:** `OrderHistoryScreen` — order list card  
**Trigger:** Print `IconButton` directly on each order card (right side), without opening the detail screen  
**Who uses it:** Store assistant reprints a receipt for a customer who lost theirs

**Notes:**
- Content is identical to the existing `OrderDetailScreen` receipt (PRN-implemented)
- Requires order items to be loaded — trigger a one-shot DAO query at click time
- Show a `Snackbar("Printing…")` while the coroutine runs; `Snackbar("Print failed")` on error
- This is noted in `UI-Improvement-Plan.md` UI-15 as a follow-up to the existing print button

---

## Implementation Checklist

The current `printMessage()` sends a single raw string with centre-alignment applied globally. For the content templates in this doc, **`PrinterUtils` must be extended** as tracked by **PU-*** rows below.

### Screen / feature work

| ID | Item | Priority | Status |
|---|---|:---:|---|
| PRN-01 | Employee Payment Voucher | P1 | `[x]` |
| PRN-02 | Acquisition Receiving Slip | P1 | `[x]` |
| PRN-03 | Remittance Slip | P1 | `[x]` |
| PRN-04 | SRP Price List | P1 | `[x]` |
| PRN-05 | Farm Operations Log | P2 | `[x]` |
| PRN-06 | Employee Period Summary | P2 | `[x]` |
| PRN-07 | Order Quick Reprint (history card) | P2 | `[x]` |
| PRN-08 | Acquisition batch report (filtered list) | P1 | `[x]` |

### `PrinterUtils` enhancements

| ID | Item | Priority | Status |
|---|---|:---:|---|
| PU-01 | Left-align support — `formatThermalLine` + dividers in `ThermalPrintBuilders.kt` | P1 | `[x]` |
| PU-02 | Alignment — `printMessage(..., alignment = 0)` for slips; orders still default centre if omitted | P1 | `[~]` |
| PU-03 | Bold section headers (`ENABLE_BOLD` on/off) per block | P1 | `[ ]` |
| PU-04 | Error feedback — snackbars on new print entry points + order detail | P1 | `[~]` |
| PU-05 | Printer connection state in ViewModel (optional) | P2 | `[ ]` |

*Last checklist update: 2026-04-03 — PRN-01–08 shipped (`buildAcquisitionBatchReport`, app bar ListAlt on Acquire); PU-03/PU-05 backlog.*

### PU-01 — Left-align support

Most receipt lines need left-aligned label + right-aligned value on the same line. Add a helper:

```kotlin
fun formatLine(label: String, value: String, width: Int = 32): String {
    val gap = width - label.length - value.length
    return if (gap > 0) label + " ".repeat(gap) + value
    else label.take(width - value.length - 1) + " " + value
}
```

### PU-02 — Per-block alignment control

`setAlignment(0)` = left, `setAlignment(1)` = centre, `setAlignment(2)` = right.  
Business name / document title → centre. Body lines → left.

### PU-03 — Bold section headers

`setPrinterStyle(WoyouConsts.ENABLE_BOLD, 1)` before headers, `ENABLE_BOLD, 0` after.

### PU-04 — Error feedback to UI

`printMessage()` returns `Boolean`. All print call sites should handle `false`:

```kotlin
val ok = PrinterUtils.printMessage(context, content)
if (!ok) snackbarHostState.showSnackbar("Print failed — check printer connection")
```

### PU-05 — Printer connection state in ViewModel (optional)

For screens that print frequently (order history), a shared `PrinterStatusViewModel` (Hilt singleton scope) can keep the `SunmiPrinterService` bound across screens, avoiding the per-print bind latency.
