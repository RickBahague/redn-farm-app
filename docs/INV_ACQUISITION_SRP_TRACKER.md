# Inventory & acquisition SRP epic (INV-US-* + ORD-US-08) — implementation tracker

**Canon:** [`USER_STORIES.md`](./USER_STORIES.md) Epic 5 (Inventory & Acquisitions), **ORD-US-08** (active price list).

**Related gaps:** Acquisition → preset deep link (**MGT-US-05** AC5) — see [`PARTIAL_IMPLEMENTATION_PLAN.md`](./PARTIAL_IMPLEMENTATION_PLAN.md) **Stream B**. (**PRD-US-01** / **PRD-US-07** catalog + history: done 2026-04-09.)

**Pricing canon:** [`PricingReference.md`](./PricingReference.md) **§4.3**, **§4.3.1** / [`pricing_clarif.md`](./pricing_clarif.md) (CLARIF-01 — **by-weight spoilage:** **rate** or **absolute kg** per line 10; **per-piece B** = **(pieces / n) × additional costs** = **`Q × A_spec`** lot hauling; **per-piece share** **`B/P_tot = A_spec/n`**; per-piece spoilage **not** applied in SRP, **`Q_sell = Q`**), **§5.1.1**, **FR-PC-10–14** — pipeline **INV-US-05**: \(C_{\text{bulk}} = B/Q_{\text{sell}}\), **\(C = C_{\text{bulk}} + A\)**, then channel markup/margin, fees, rounding; **per-piece** uses **`quantity`** = total pieces, **`piece_count`** = pieces/kg → **\(Q = \text{quantity}/n\)**; **`s_eff = 0`** for SRP on per-piece rows. **BUG-PRC-03** *(closed):* per-piece **SRP** = **(A + B/total_quantity)** × (1+μ). **BUG-PRC-04** *(closed):* **by-weight** optional **`spoilage_kg`** on acquisition — **`SrpCalculator.Input.spoilageKg`**, **`AcquisitionEntity.spoilage_kg`**, form “Unsellable kg”, **`SrpCalculatorTest`**; DB v7. Preset remains rate-only; absolute kg is **per-line** override.

---

## INV-US-01 — Record acquisition

| Acceptance | Status | Notes |
|------------|--------|-------|
| AC1 Product, qty, price, date, location; per-kg vs per-piece **quantity** semantics | `[x]` | `AcquisitionFormScreen.kt` |
| AC2 Total amount auto | `[x]` | |
| AC3 Per kg / per pc toggle | `[x]` | |
| AC4 **piece_count** (pieces/kg), default from product | `[x]` | `default_piece_count` pre-fill |

**Primary files:** `AcquisitionFormScreen.kt`, `AcquireProduceScreen.kt`, `AcquisitionRepository.kt`.

---

## INV-US-02 — View history

| Acceptance | Status | Notes |
|------------|--------|-------|
| AC1 List fields + unit type | `[x]` | `AcquireProduceScreen.kt` |
| AC2 **piece_count** not in list row | `[x]` | |
| AC3 Filters | `[x]` | |

---

## INV-US-03 — Edit acquisition

| Acceptance | Status | Notes |
|------------|--------|-------|
| AC1–5 Cost vs metadata; recompute from **stored snapshot**; **preset_ref** unchanged | `[x]` | `AcquisitionRepository.updateWithPricing` |

---

## INV-US-04 — Delete

| Acceptance | Status | Notes |
|------------|--------|-------|
| AC1 Confirm | `[x]` | |

---

## INV-US-06 — Active SRP = latest acquisition

| Acceptance | Status | Notes |
|------------|--------|-------|
| AC1–2 Order by **date_acquired** DESC, tiebreak **created_at** DESC | `[x]` | `AcquisitionDao.getActiveSrpForProduct`, `getAllActiveSrps` |
| AC3 New save becomes active | `[x]` | Implicit from query |
| AC4 Delete restores prior | `[x]` | Re-query |
| AC5–6 Orders / price list / **preset_ref** trace | `[x]` | `TakeOrderViewModel`, `OrderPricingResolver`, `ActiveSrpsViewModel` |

**Primary files:** `AcquisitionDao.kt`, `AcquisitionEntity.kt` (`created_at` preserved on update).

---

## INV-US-05 — Auto SRP from acquisition

| Acceptance | Status | Notes |
|------------|--------|-------|
| AC1 Live preview | `[x]` | `AcquisitionRepository.previewDraftPricing`, form preview card |
| AC2 No active preset → notice | `[x]` | `AcquisitionFormScreen` |
| AC3 Read-only preset context | `[x]` | |
| AC4 Fractional packs 500/250/100 g | `[x]` | `SrpCalculator` + `PricingChannelEngine` |
| AC5 Per-piece when **piece_count** set | `[x]` | |
| AC6 Round up whole PHP | `[x]` | Channel rounding rules |
| AC7 Persist SRPs on save | `[x]` | `insertWithPricing` / `updateWithPricing` |
| AC8 Block **Q_sell = 0** | `[x]` | `SrpCalculator.compute` |
| AC9 Immutable snapshot + **preset_ref** | `[x]` | `withPresetSnapshotAndPricing`; edit uses `old.channels_snapshot_json`; snapshot stores **`spoilage_kg`** when used |
| AC10 Per-piece **Q** derivation | `[x]` | `SrpCalculator.bulkQuantityKg` |
| AC11 Optional **absolute spoilage kg** (per-kg) | `[x]` | **BUG-PRC-04** — `spoilage_kg` / form field; empty → preset rate |

**Primary files:** `SrpCalculator.kt`, `PricingChannelEngine.kt`, `AcquisitionRepository.kt`, `AcquisitionFormScreen.kt`.

**Tests:** `SrpCalculatorTest.kt` (incl. absolute vs rate equivalence).

**Related:** **MGT-US-07** custom per-kg SRP — `mergeCostContextWithCustomSrps`, `srp_custom_override` (`USER_STORIES.md`).

---

## ORD-US-08 — Active SRPs screen (store assistant)

| Acceptance | Status | Notes |
|------------|--------|-------|
| AC1 **From ₱X/kg** = min per-kg across channels on summary row | `[x]` | `OrderPricingResolver.minPerKgSrpAcrossChannels` + `ActiveSrpsScreen` |
| AC2 Expand → **all** channels, kg + packs + per piece | `[x]` | `ActiveSrpsScreen` expanded grid |
| AC3 Provenance: acquisition **id** + **date_acquired** | `[x]` | Summary line on card |
| AC4 Read-only | `[x]` | |

**Primary files:** `ActiveSrpsScreen.kt`, `ActiveSrpsViewModel.kt`.

**Channel chips** on this screen: **print** uses selected channel only; full multi-channel breakdown is in the expanded row.

---

## Story status in `USER_STORIES.md`

**INV-US-01–06**, **INV-US-05**, and **ORD-US-08** are marked **✅** with pointers to this file. Other epics (e.g. **PRD-US-01** partial, **MGT-US-***) stay as documented there.
