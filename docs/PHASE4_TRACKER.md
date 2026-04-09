# Phase 4 — Order-taking with SRP pre-fill (detailed tracker)

**Created:** 2026-04-02  
**Related plan:** `rebuild_plan.md` Phase 4 (this file does **not** modify that document).  
**Goal:** Channel-aware orders with SRP pre-fill from active acquisitions, active SRP browse (ORD-US-08), and order detail flow (ORD-US-10). Depends on Phase 3.

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Implemented in codebase |
| `[~]` | Partial / simplified — see notes |
| `[ ]` | Manual QA / follow-up |

---

## P4-1 — Customer type → default channel (CUS-US-05)

| Item | Status | Notes |
|------|--------|--------|
| Map type → default `Order.channel` | `[x]` | `CustomerType.defaultOrderChannel()` in `data/pricing/SalesChannel.kt`: **WHOLESALE** → `reseller`; **RETAIL** / **REGULAR** → `offline` |
| Online is manual | `[x]` | Not in default map; user picks **Online** via channel chips |

**Files:** `SalesChannel.kt` (`SalesChannel` + `defaultOrderChannel()`), `TakeOrderViewModel` (customer select applies default)

**Plan delta:** Rebuild text says “RETAIL → offline, RESELLER → reseller”; app enum uses **WHOLESALE** (not “RESELLER”) for reseller default.

---

## P4-2 — Channel picker + repricing

| Item | Status | Notes |
|------|--------|--------|
| Picker (online / reseller / offline) | `[x]` | `FilterChip`s on `TakeOrderScreen`; `TakeOrderViewModel.setChannel` + `repriceCart` |
| Change channel updates line prices | `[x]` | Cart repriced from active SRP map + fallback |
| Edit unpaid order | `[x]` | `EditOrderScreen` channel chips (unpaid) → `repriceOrderItems` |

**Files:** `TakeOrderScreen.kt`, `TakeOrderViewModel.kt`, `EditOrderScreen.kt`, `OrderHistoryViewModel.kt`

---

## P4-3 — Line items: SRP pre-fill (ORD-US-01 / ORD-US-02)

| Item | Status | Notes |
|------|--------|--------|
| Resolve unit price from acquisition + channel | `[x]` | `OrderPricingResolver.resolveUnitPrice` — SRP from active acquisition row; **PRD-US-06** reseller fallbacks via `ProductPrice` discounted/regular when SRP missing |
| Read-only unit price in UI | `[x]` | Take-order `ProductSelectionDialog` shows preview; line uses resolved price (no free typing) |
| Dual unit (kg / piece) | `[x]` | When catalog + SRP support both: switch in dialog / `OrderItemCard` |

**Files:** `OrderPricingResolver.kt`, `ProductSelectionDialog.kt` (take order), `OrderItemCard.kt`, `TakeOrderViewModel.kt`

**Simplification (`[~]`):** Resolver uses in-memory **active SRP map** (`observeAllActiveSrps`) aligned with “latest active acquisition per product,” not necessarily a single DAO call named `getActiveSrpForProduct` at click time.

---

## P4-4 — View active SRPs (ORD-US-08)

| Item | Status | Notes |
|------|--------|--------|
| Screen listing products with SRPs | `[x]` | `ActiveSrpsScreen` + `ActiveSrpsViewModel` |
| Per-channel kg / piece | `[x]` | Expandable rows; “from ₱X/kg” style summary via `OrderPricingResolver.minPerKgSrpAcrossChannels` |
| Entry before order | `[x]` | Take-order top bar → Active SRPs (`NavGraph` + `Icons.Filled.AttachMoney`) |

**Files:** `ActiveSrpsScreen.kt`, `ActiveSrpsViewModel.kt`, `NavGraph.kt`, `TakeOrderScreen.kt`

---

## P4-5 — Order detail (ORD-US-10)

| Item | Status | Notes |
|------|--------|--------|
| Read-only order + items | `[x]` | `OrderDetailScreen` |
| Customer context | `[x]` | `OrderHistoryViewModel.getCustomer` + `CustomerRepository.getCustomerById` |
| Channel shown | `[x]` | Label via `SalesChannel` |
| Print | `[x]` | Toolbar / existing print path |
| Mark paid / delivered | `[x]` | Switches + confirmation pattern aligned with history |
| Edit → existing editor | `[x]` | Unpaid only; `OrderDetail` → `EditOrder`; history list opens **detail** first (not straight to edit) |

**Files:** `OrderDetailScreen.kt`, `OrderHistoryScreen.kt`, `NavGraph.kt`, `CustomerRepository.kt`, `CustomerDao.kt`

---

## Done (quick index)

- **Pricing / domain:** `SalesChannel`, `CustomerType.defaultOrderChannel()`, `OrderPricingResolver`.
- **Data:** `CustomerDao.getById` / `CustomerRepository.getCustomerById`; `AcquisitionRepository.observeAllActiveSrps()`.
- **Take order:** channel chips, cart repricing, SRP preview dialog, `Order.channel` on place order.
- **Edit order:** channel chips + repricing; add-line / edit-line dialogs use order channel + resolver (private `ProductSelectionDialog` in `EditOrderScreen`).
- **Navigation:** `Screen.ActiveSrps`, `Screen.OrderDetail`; history → detail → edit.

---

## Build / tests

| Step | Status | Notes |
|------|--------|--------|
| `./gradlew assembleDebug` | `[x]` | Green after Phase 4 wiring (2026-04-02) |
| `./gradlew testDebugUnitTest` | `[ ]` | Run if you touch pricing or VM logic |
| Manual smoke | `[ ]` | Default channel by customer; channel change reprices; Active SRPs; history → detail → edit unpaid |

---

## Follow-ups (optional)

- Align rebuild_plan P4-1 wording with **WHOLESALE** vs “RESELLER” customer type naming.
- Clear trivial compiler warnings (`!!` on discounted prices, `ArrowBack` → `AutoMirrored`) if desired.
