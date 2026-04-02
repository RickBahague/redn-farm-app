# Rebuild Plan — RedN Farm App

**Created:** 2026-04-02  
**Branch:** `dev-feature`  
**DB target version:** 4 (destructive migration from v3)  
**Source of truth for stories:** [USER_STORIES.md](./USER_STORIES.md)  
**Schema reference:** [schema_evolution.sql](./schema_evolution.sql)

---

## How to use this document

- Each phase is a self-contained unit of work. Complete phases in order — later phases depend on earlier ones as noted.
- Each task has a status field. Update it as work progresses:
  - `[ ]` — not started
  - `[~]` — in progress
  - `[x]` — done
- Story IDs (e.g. `INV-US-05`) refer to acceptance criteria in USER_STORIES.md.
- When a task is done, note the key file(s) changed so the next agent has fast orientation.

---

## Phase 0 — DB Foundation

**Goal:** Room compiles with the v4 schema. No runtime or device needed.  
**Blocks:** everything else.

### Tasks

- [x] **P0-1** Bump `FarmDatabase` version to 4, add `fallbackToDestructiveMigration()`
  - File: `app/src/main/java/com/redn/farm/data/local/FarmDatabase.kt`

- [x] **P0-2** Update `Product` entity — add `category: String?`, `defaultPieceCount: Int?`
  - Story: MGT-US-03 / PRD-US-02
  - File: `data/local/entity/ProductEntity.kt`

- [x] **P0-3** Update `Order` entity — add `channel: String` (default `"offline"`)
  - Story: ORD-US-01
  - File: `data/local/entity/OrderEntity.kt`

- [x] **P0-4** Update `Acquisition` entity — add all new columns
  - `piece_count: Int?`, `created_at: Long`, `preset_ref: String?`
  - Snapshot fields: `spoilage_rate?`, `additional_cost_per_kg?`, `hauling_weight_kg?`, `hauling_fees_json?`, `channels_snapshot_json?`
  - 15 SRP columns (all `Double?`)
  - Added indices: `preset_ref`, `date_acquired`
  - File: `data/local/entity/AcquisitionEntity.kt`

- [x] **P0-5** Create `PricingPresetEntity`
  - File: `data/local/entity/PricingPresetEntity.kt`

- [x] **P0-6** Create `PresetActivationLogEntity`
  - File: `data/local/entity/PresetActivationLogEntity.kt`

- [x] **P0-7** Create/update DAOs for new entities
  - `PricingPresetDao`: insert, getAll, getActive, getById, activatePreset (transaction)
  - `PresetActivationLogDao`: insert, getAll, truncate
  - `AcquisitionDao`: updated sort order; added `getActiveSrpForProduct`, `getAllActiveSrps`
  - Files: `data/local/dao/`

- [x] **P0-8** Register new entities in `@Database(entities = [...])` and expose new DAOs in `FarmDatabase`

- [x] **P0-9** Expose new DAOs in `DatabaseModule` (Hilt)
  - File: `di/DatabaseModule.kt`

- [x] **P0-10** Confirm clean compile: `./gradlew assembleDebug` — **BUILD SUCCESSFUL**

---

## Phase 1 — Restore All ✅ CRUD Features

**Goal:** Every already-implemented story still works after the schema change. No new features yet.  
**Depends on:** Phase 0 complete.

Work table-by-table. After each group, run `./scripts/dev.sh install` and smoke-test on device.

### Tasks

- [ ] **P1-1** Auth (AUTH-US-01 / 02 / 03) — confirm login/logout/session still work with new DB
  - No entity changes; just verify `FarmDatabase` init + seed still runs correctly

- [ ] **P1-2** Products CRUD (PRD-US-01 / 03 / 04 / 05) — new nullable fields require no UI change yet

- [ ] **P1-3** Customers CRUD (CUS-US-01 / 02 / 03 / 04) — no schema changes

- [ ] **P1-4** Employees + Payments CRUD (EMP-US-01 through 05)
  - No schema changes
  - Verify net pay display formula: `amount − cash_advance_amount + liquidated_amount` (EMP-US-04 AC#5)

- [ ] **P1-5** Farm Operations CRUD (FOP-US-01 through 04) — no schema changes

- [ ] **P1-6** Remittances CRUD (REM-US-01 / 02 / 03) — no schema changes

- [ ] **P1-7** Orders + Order Items CRUD (ORD-US-03 / 04 / 05 / 06 / 07 / 09)
  - `channel` defaults to `"offline"` — existing order save must supply this default
  - SRP pre-fill not required yet (comes in Phase 4)

- [ ] **P1-8** Acquisitions CRUD (INV-US-01 / 02 / 03 / 04)
  - New nullable columns default to null on save — existing form still works
  - `createdAt` must be set to `System.currentTimeMillis()` on every insert
  - SRP computation not required yet (comes in Phase 3)

---

## Phase 2 — Pricing Preset System

**Goal:** Admin can create, view, activate, and trace pricing presets (MGT-US-00 through MGT-US-06).  
**Depends on:** Phase 0. Can be built in parallel with Phase 1.

### Data / domain layer

- [ ] **P2-1** `PricingPresetRepository` — wraps `PricingPresetDao` and `PresetActivationLogDao`; expose:
  - `savePreset(preset)` — always saves as inactive
  - `activatePreset(presetId, activatedBy)` — transaction: deactivate current active, set new active, append log row
  - `getActivePreset(): Flow<PricingPreset?>`
  - `getAllPresets(): Flow<List<PricingPreset>>`
  - `getActivationLog(): Flow<List<PresetActivationLog>>`

- [ ] **P2-2** JSON serialization helpers for `channelsJson`, `haulingFeesJson`, `categoriesJson`
  - Use `kotlinx.serialization` or `Gson` — match whatever the project already uses
  - Define sealed/data classes: `ChannelConfig`, `HaulingFeeItem`, `CategoryOverride`

### UI — Settings entry point

- [ ] **P2-3** Settings screen (SYS-US-01 / MGT-US-00)
  - Accessible from dashboard top bar (admin only per SYS-US-02 AC#7)
  - Contains "Pricing Presets" entry (and room for future settings categories)

- [ ] **P2-4** Pricing Presets home screen (MGT-US-00 AC#3)
  - Shows active preset summary (name, activation date, key values)
  - Two actions: **New Preset** → Preset Editor; **Preset History** → Preset History screen

### UI — Preset Editor (MGT-US-01 / 02 / 03)

- [ ] **P2-5** Preset Editor screen — three clearly labelled sections:

  **Section: Spoilage & Hauling** (MGT-US-01)
  - Default spoilage rate (fraction 0–0.99)
  - Hauling model: named fee line items + hauling weight → auto-derives `A`
  - OR: direct `A` override input
  - Optional preset name field; auto-generates name if blank

  **Section: Channel Markups** (MGT-US-02)
  - For each channel (online / reseller / offline):
    - Markup % OR margin % — UI enforces mutual exclusivity
    - Rounding rule picker: `ceil_whole_peso` (default), `nearest_whole_peso`, `nearest_0.25`
    - Optional channel fees list (label, type fixed/pct, amount)
  - Default values on first open: online 35% markup, reseller 25% markup, offline 30% markup; all `ceil_whole_peso`

  **Section: Categories** (MGT-US-03)
  - Create / rename / delete categories
  - Each category: optional spoilage rate override, optional `additionalCostPerKg` override
  - Categories without overrides inherit store defaults

- [ ] **P2-6** Save preset action — creates inactive record; shows confirmation with preset name/ID

### UI — Preset History & Detail (MGT-US-05)

- [ ] **P2-7** Preset History screen — all presets newest-first; active preset visually distinguished
- [ ] **P2-8** Preset Detail screen — full snapshot (read-only); **Restore** and **Activate** actions
  - Restore loads preset into Preset Editor as a draft (new preset on save, does not overwrite original)

### UI — Activation flow (MGT-US-04 / 06)

- [ ] **P2-9** Preset Preview screen — mandatory step before activation
  - Shows computed SRP examples using the to-be-activated preset values
  - Admin cannot skip; Confirm button commits activation
- [ ] **P2-10** Activate action — calls `activatePreset()`; navigates back to Presets home with updated active summary

---

## Phase 3 — SRP Computation Pipeline

**Goal:** Acquisitions auto-compute and store all SRPs using the active preset (INV-US-05, INV-US-06).  
**Depends on:** Phase 2 (active preset must exist).

### Computation

- [ ] **P3-1** `SrpCalculator` — pure object, no Android dependencies, fully unit-testable
  - Input: `quantity`, `pricePerUnit`, `isPerKg`, `pieceCount?`, `spoilageRate`, `additionalCostPerKg`, `channelsJson`
  - Core: `C = pricePerUnit / (quantity * (1 - spoilageRate))` then per-channel: `apply markup|margin → add A → add channel fees → apply rounding rule`
  - Output: data class with all 15 SRP fields
  - Write `SrpCalculatorTest` with cases from PricingReference.md

- [ ] **P3-2** Wire `SrpCalculator` into acquisition save path (INV-US-05)
  - On save: fetch active preset → compute SRPs → persist snapshot columns + SRP columns
  - If no active preset: SRP columns saved as null; show warning toast "No active preset — SRPs not computed"

- [ ] **P3-3** Wire `SrpCalculator` into acquisition edit path — Option C (INV-US-03 AC#6–7)
  - Cost-input changes (`quantity`, `pricePerUnit`, `pieceCount`) → recompute using stored `channelsSnapshotJson`
  - Metadata changes (`dateAcquired`, `location`) → no recompute, SRP columns unchanged

- [ ] **P3-4** `getActiveSrpForProduct(productId)` DAO query (INV-US-06)
  - `SELECT * FROM acquisitions WHERE product_id = :productId ORDER BY date_acquired DESC, created_at DESC LIMIT 1`
  - Expose via `AcquisitionRepository`

- [ ] **P3-5** Display SRP values on acquisition detail / history screen (INV-US-02 AC#6)
  - Show per-channel SRP breakdown; null SRPs shown as "—"

---

## Phase 4 — Order-Taking with SRP Pre-fill

**Goal:** Store assistant selects channel → SRP pre-fills from the active acquisition (ORD-US-01, ORD-US-02, ORD-US-08, ORD-US-10).  
**Depends on:** Phase 3.

- [ ] **P4-1** CUS-US-05 — `customerType` maps to default channel
  - RETAIL → `offline`, RESELLER → `reseller`; online must be manually selected

- [ ] **P4-2** Order form — channel picker (online / reseller / offline); defaults from customer type
  - On channel change: re-fetch SRP for each item in the order and update pre-filled prices

- [ ] **P4-3** Order item entry — SRP pre-fill per product (ORD-US-01 / ORD-US-02)
  - Calls `getActiveSrpForProduct(productId)` → reads the SRP column matching selected channel + unit type
  - `price_per_unit` field is read-only (pre-filled); store assistant cannot override

- [ ] **P4-4** ORD-US-08 — "View active SRPs" screen
  - List of all active products with their current active SRP per channel (per-kg and per-piece where applicable)
  - Accessible before taking an order

- [ ] **P4-5** ORD-US-10 — Order detail screen
  - Read-only full view of the order and its items
  - Action buttons: Print (ORD-US-09), Mark as Paid (ORD-US-05), Mark as Delivered (ORD-US-06)
  - Edit navigates to the existing edit order screen

---

## Phase 5 — Auth & Profile

**Goal:** Any user can change their own password; admin can manage user accounts (AUTH-US-04, AUTH-US-05).  
**Depends on:** Phase 1. Can be built in parallel with Phases 2–4.

- [ ] **P5-1** Profile screen (SYS-US-02) — username, full name, role (read-only); Change Password action
  - Accessible from dashboard top bar for all users

- [ ] **P5-2** Change Password flow (AUTH-US-05)
  - Three fields: current password, new password, confirm new password
  - Validate current password against stored hash; mismatch shows error, no change applied
  - New ≠ confirm → field-level error, save blocked
  - On success: hash updated, user remains logged in

- [ ] **P5-3** Admin user management screen (AUTH-US-04)
  - Create new user (username, full name, role, initial password)
  - Deactivate / reactivate users
  - Reset another user's password (admin only)
  - Admin cannot deactivate their own account

---

## Phase 6 — Export Completion

**Goal:** All 10 tables exportable; selective export; SRP columns included on acquisitions (EXP-US-01, EXP-US-02).  
**Depends on:** Phase 1 (all tables populated).

- [ ] **P6-1** Audit existing `CsvExportService` — identify which tables are currently exported
  - File: `data/export/CsvExportService.kt`

- [ ] **P6-2** Add missing tables to export (EXP-US-01)
  - All 10: users, products, customers, orders, order_items, employees, employee_payments, acquisitions, farm_operations, remittances
  - Each row includes `DeviceId` header column
  - Acquisitions export includes: `preset_ref`, all 15 SRP columns, `channels_snapshot_json`

- [ ] **P6-3** EXP-US-02 — selective export
  - UI lets admin pick which tables to include before exporting
  - Export writes one CSV file per selected table to `exports/`

- [ ] **P6-4** SYS-US-04 — verify Room generated schema JSON matches schema_evolution.sql VERSION 4
  - Build project → inspect `build/generated/source/kapt/.../FarmDatabase_Impl.java`
  - Compare CREATE TABLE statements against schema_evolution.sql VERSION 4 block

---

## Dependencies Summary

```
Phase 0  ──────────────────────────────┐
    │                                  │
Phase 1                            Phase 2
    │                                  │
    │                              Phase 3
    │                                  │
    └──────────────────────────────Phase 4
    │
Phase 5 (parallel to 2–4)
    │
Phase 6 (after Phase 1)
```

---

## Key References

| Resource | Purpose |
|----------|---------|
| [USER_STORIES.md](./USER_STORIES.md) | Full acceptance criteria for every story ID |
| [schema_evolution.sql](./schema_evolution.sql) | Canonical DB schema; VERSION 4 is the rebuild target |
| [PricingReference.md](./PricingReference.md) | SRP formula, rounding rules, channel markup defaults |
| `data/local/FarmDatabase.kt` | Room DB class — version, migrations, entity list |
| `di/DatabaseModule.kt` | Hilt DAO providers |
| `navigation/NavGraph.kt` | All routes — add new screens here |
| `scripts/dev.sh` | Build, install, log shortcuts |
