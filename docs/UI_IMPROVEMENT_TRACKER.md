# UI Improvement — implementation tracker

**Companion:** [`UI-Improvement-Plan.md`](./UI-Improvement-Plan.md)  
**Created:** 2026-04-02  
**Audience:** Humans + coding agents (parallel tasks, clear dependencies).

This file does **not** modify `UI-Improvement-Plan.md`. Update **status** here as work lands.

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Done / verified in codebase |
| `[~]` | Partially done or superseded — see notes |
| `[ ]` | Not done |
| `[ ]` QA | Implemented; needs **device** check (Sunmi / keyboard / scanner) |

---

## Plan errata (read before splitting agent work)

| Issue | Resolution |
|-------|------------|
| **Two “UI-14” rows** in *Implementation Order* (lines 382 & 389) | Treat **UI-14** as *dialogs → full-screen forms* only. **Empty states** are **UI-17** (do not use UI-14 for empty states). |
| **UI-21** vs rebuild | **Active SRPs screen** (`ActiveSrpsScreen`) already exists (rebuild Phase 4 / ORD-US-08). Tracker treats UI-21 as **verify / enhance** (e.g. channel `FilterChip` row + footer preset text per plan) unless product drops extras. |
| **UI-18** vs rebuild | Phase 3 tracker lists **SRP preview** on acquire — verify parity with plan mock before marking done. |

---

## Agent-oriented review

**Principles**

1. **One agent per cohesive surface** when possible (e.g. “Take order only”, “theme only”) to reduce merge conflicts in the same `@Composable` files.
2. **Land foundation first:** `MainActivity` edge-to-edge + `FarmTheme` (UI-06 / UI-10) before wide `imePadding()` passes, so padding behavior is consistent.
3. **Shared components once:** Numeric pad (UI-04) and barcode receiver (UI-05) should be **one module** reused from acquisition + order flows; assign a single agent or strict sequence (pad → then wire call sites).
4. **Database migrations** (UI-05 `barcode` on products) serialize with any agent touching `ProductEntity` / DAO / importers — do not parallel with unrelated product edits.
5. **Screenshots / manual QA** on **720×1440 portrait** (or emulator with same logical width) for P1 items.

**Suggested parallel “packages” (after Wave 0)**

| Package | Items | Typical files / layer |
|--------|--------|------------------------|
| **A — Theme & global** | UI-06, UI-09, UI-10, UI-11 | `MainActivity.kt`, `ui/theme/*`, shared dialog patterns |
| **B — Dashboard & lists** | UI-01, UI-12, UI-17, UI-19 | `MainScreen.kt`, `MainViewModel.kt`, list screens, `OrderHistoryScreen.kt` |
| **C — Auth shell** | UI-07 | `LoginScreen.kt` |
| **D — Take order** | UI-02, UI-03, UI-04 (wiring), UI-05 (order path), UI-08 (subset) | `TakeOrderScreen.kt`, `OrderItemCard.kt`, `ProductSelectionDialog.kt`, new `NumericPad*` |
| **E — Acquire** | UI-04 (wiring), UI-05 (acquire path), UI-18 verify | `AcquireProduceScreen.kt`, dialogs VM |
| **F — Hardware** | UI-05 (receiver + manifest), product `barcode` column | `AndroidManifest.xml`, `ProductEntity`, migration, scanner util |
| **G — Print & thumb** | UI-13, UI-15 | `OrderDetailScreen.kt`, `OrderHistoryScreen.kt`, key dialogs |
| **H — Heavy forms** | UI-14, UI-16, UI-20 | Nav routes, `EmployeePaymentScreen` / payment flow, signature |
| **I — Polish** | Remaining UI-08 | Cross-screen chip / icon sizing |

**Dependency order (minimum)**

```
MainActivity edge-to-edge (UI-06 base)
    → Login (UI-07), form imePadding pass (UI-06)
FarmTheme colours + type (UI-10, UI-09)
    → UI-11, UI-12, UI-17

Numeric pad component (UI-04)
    → UI-03 stepper integration
    → Acquire + order quantity fields

Product.barcode + scanner util (UI-05 data)
    → UI-05 acquisition UI
    → UI-05 order dialog UI

Payment full-screen (UI-14)
    → UI-16 net pay
    → UI-20 signature canvas
```

---

## Implementation waves (for scheduling)

| Wave | Scope | Items |
|------|--------|--------|
| **0** | Enablers | UI-06 (`MainActivity` + audit pattern), UI-10, UI-09 |
| **1** | Quick wins | UI-07, UI-01, UI-11, UI-12, UI-08 (first pass) |
| **2** | Order UX | UI-02, UI-03, UI-04, UI-08 (order), UI-15, UI-13 (order surfaces) |
| **3** | Scanner + products DB | UI-05 (full), UI-17 (lists) |
| **4** | Forms / nav | UI-14, UI-16, UI-20 |
| **5** | Role & verify | UI-19, UI-18 verify, UI-21 verify/enhance |

Waves can overlap **only** where dependency graph allows (e.g. Wave 1 + Wave 0 theme in parallel if different files).

---

## Per-item tracker

| ID | Priority | Status | Agent package | Notes / primary files |
|----|----------|--------|---------------|------------------------|
| UI-01 | P1 | `[x]` | B | `MainScreen.kt` — `LazyVerticalGrid(GridCells.Fixed(2))`, 8 tiles, correct order |
| UI-02 | P1 | `[x]` | D | `TakeOrderScreen.kt` — `Scaffold.bottomBar` with total + Place Order; cart scrolls independently |
| UI-03 | P1 | `[x]` | D | `OrderItemCard.kt` — inline −/value/+ stepper; 48dp icon buttons; 0.25-step for kg, 1-step for pieces |
| UI-04 | P1 | `[x]` | D, E | Shared `ModalBottomSheet` numeric pad + wired to quantity/price/total + order quantity dialogs |
| UI-05 | P1 | `[ ]` | F, D, E | Sunmi broadcast; `ProductEntity` + migration `barcode`; acquire + product dialog |
| UI-06 | P1 | `[x]` | A | `WindowCompat.setDecorFitsSystemWindows`; `imePadding()` on Login, ManageCustomers, ManageEmployees, Remittances |
| UI-07 | P1 | `[x]` | C | `LoginScreen.kt` — header 120dp + `imePadding()` + `verticalScroll` |
| UI-08 | P1 | `[x]` | I | FilterChips `.height(48.dp)`; icon buttons `.size(48.dp)` (20dp icon inside) across order + cart screens |
| UI-09 | P1 | `[x]` | A | `Type.kt` — `headlineSmall`, `titleMedium`, etc. customized for POS readability |
| UI-10 | P1 | `[x]` | A | `Color.kt` — `FarmGreen = 0xFF2E7D32`; `FarmError = 0xFFB71C1C`; verify secondary/contrast on device |
| UI-11 | P1 | `[x]` | A | Destructive confirm buttons use `MaterialTheme.colorScheme.error` |
| UI-12 | P1 | `[x]` | B | `OrderHistoryScreen` — right-edge status chips (paid/unpaid + delivered/pending) |
| UI-13 | P1 | `[x]` | G | Primary actions at bottom of forms (AcquisitionDialog, dialogs use standard `confirmButton` bottom slot) |
| UI-14 | P1 | `[~]` | H | `EmployeePaymentScreen` is a full-screen route ✓; but individual *add/edit payment* form (`PaymentDialog.kt`) and employee add/edit are still `AlertDialog` — not migrated to full-screen |
| UI-15 | P1 | `[x]` | G | `OrderDetailScreen` — full-width `OutlinedButton(“Print Receipt”)` in action row; top bar icon remains |
| UI-16 | P2 | `[ ]` | H | Live net pay summary block not present in `PaymentDialog.kt` — three raw inputs, no computed total |
| UI-17 | P2 | `[x]` | B | Empty states + CTAs: Orders history, Acquisitions, Customers, Employees, Farm Ops |
| UI-18 | P2 | `[x]` | E | Acquisition live SRP preview: collapsible card + auto-expand on qty+price; preset name in header |
| UI-19 | P2 | `[x]` | B | `MainScreen` tiles filtered via `Rbac.dashboardTileTitles(Rbac.normalizeRole(role))`; `security/Rbac.kt` + `navigation/RbacGate.kt` |
| UI-20 | P2 | `[x]` | H | `SignatureCanvasField.kt` — Canvas touch tracking + Base64 PNG export; Draw/Type toggle in `PaymentDialog.kt` |
| UI-21 | P2 | `[x]` | B | Active SRPs: channel FilterChip selector + per-row SRPs for selected channel + preset footer |

---

## Done criteria (per agent handoff)

- **UI / theme agents:** `./gradlew assembleDebug` + visual check on 720×1440 or Sunmi.
- **UI-05:** Scan to product match + snackbar on miss; document barcode format expectations in this file when known.
- **UI-14+16+20:** Single navigation entry for “add/edit payment” with back stack correct from employees flow.

---

## Changelog

| Date | Change |
|------|--------|
| 2026-04-02 | Initial tracker from `UI-Improvement-Plan.md`; errata + agent waves added. |
| 2026-04-02 | Full codebase review pass: upgraded UI-01–03, 06–13, 15, 19, 20 from `[~]`/`[ ]` to `[x]`. UI-14 upgraded to `[~]` (list screen is full-screen; add/edit dialog is not). UI-16 confirmed `[ ]` (no net pay summary in PaymentDialog). UI-05 remains `[ ]`. |
