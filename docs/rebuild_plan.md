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

- [x] **P1-1** Auth (AUTH-US-01 / 02 / 03) — confirm login/logout/session still work with new DB
  - No entity changes; just verify `FarmDatabase` init + seed still runs correctly
  - Code scan: `LoginViewModel` correctly reads `userDao()` → no changes needed

- [x] **P1-2** Products CRUD (PRD-US-01 / 03 / 04 / 05) — new nullable fields require no UI change yet
  - `category` and `defaultPieceCount` are nullable with defaults; existing CRUD unaffected
  - Known pre-existing issues logged in `docs/user_review_product_management.md` (Epic 3 rebuild)

- [x] **P1-3** Customers CRUD (CUS-US-01 / 02 / 03 / 04) — no schema changes
  - Code scan: `ManageCustomersViewModel` — no entity changes, works as-is

- [x] **P1-4** Employees + Payments CRUD (EMP-US-01 through 05)
  - No schema changes
  - Verify net pay display formula: **`amount + cash_advance_amount`** (liquidated excluded — EMP-US-05 / `docs/bugs.md` BUG-EMP-01)

- [x] **P1-5** Farm Operations CRUD (FOP-US-01 through 04) — no schema changes
  - `FarmOperationsViewModel` uses `@HiltViewModel` — clean

- [x] **P1-6** Remittances CRUD (REM-US-01 / 02 / 03) — no schema changes
  - Code scan: `RemittanceViewModel` — no entity changes, works as-is

- [x] **P1-7** Orders + Order Items CRUD (ORD-US-03 / 04 / 05 / 06 / 07 / 09)
  - Fixed: `OrderRepository.updateOrder()` was silently writing `channel = "offline"`; now uses `order.channel`
  - `TakeOrderViewModel.placeOrder()` correctly passes `channel = _channel.value`
  - `EditOrderScreen` reads and displays `order.channel`; channel picker updates the order copy before save

- [x] **P1-8** Acquisitions CRUD (INV-US-01 / 02 / 03 / 04)
  - New nullable columns default to null on save — confirmed in `AcquireProduceScreen` `Acquisition(...)` constructor
  - `createdAt` set by `AcquisitionRepository.toEntityForSave(isInsert=true)` — ✓
  - Fixed: `DatePickerDialog` confirm button was not reading the selected date back into `selectedDate`
  - File: `ui/screens/acquire/AcquireProduceScreen.kt`

---

## Phase 2 — Pricing Preset System ✅

**Goal:** Admin can create, view, activate, and trace pricing presets (MGT-US-00 through MGT-US-06).  
**Depends on:** Phase 0. Can be built in parallel with Phase 1.

### Data / domain layer

- [x] **P2-1** `PricingPresetRepository`
  - File: `data/repository/PricingPresetRepository.kt`

- [x] **P2-2** JSON serialization helpers
  - File: `data/pricing/PricingPresetJsonModels.kt` — `PricingPresetGson`, `ChannelConfig`, `ChannelsConfiguration`, `HaulingFeeItem`, `CategoryOverride`, `ChannelFee`

### UI — Settings entry point

- [x] **P2-3** Settings screen — admin-gated icon in `MainScreen` top bar (checked via `isAdmin` state)
  - File: `ui/screens/settings/SettingsScreen.kt`

- [x] **P2-4** Pricing Presets home screen
  - Files: `ui/screens/pricing/PricingPresetsHomeScreen.kt` + `PricingPresetsHomeViewModel.kt`

### UI — Preset Editor (MGT-US-01 / 02 / 03)

- [x] **P2-5** Preset Editor screen — all three sections complete
  - Added: channel fees list UI (was missing; now each channel has Add/Edit/Delete fee rows with label, type, amount)
  - Files: `ui/screens/pricing/PricingPresetEditorScreen.kt` + `PricingPresetEditorViewModel.kt`

- [x] **P2-6** Save preset action — `PricingPresetEditorViewModel.save()` — snackbar confirms name/ID

### UI — Preset History & Detail (MGT-US-05)

- [x] **P2-7** Preset History screen — active preset shown with checkmark
  - Files: `ui/screens/pricing/PresetHistoryScreen.kt` + `PresetHistoryViewModel.kt`
- [x] **P2-8** Preset Detail screen — full JSON snapshot (read-only); Restore → editor, Activate → preview
  - Files: `ui/screens/pricing/PresetDetailScreen.kt` + `PresetDetailViewModel.kt`

### UI — Activation flow (MGT-US-04 / 06)

- [x] **P2-9** Preset Preview screen — shows illustrative per-channel SRPs (bulk cost ₱5000, 100 kg)
  - Files: `ui/screens/pricing/PresetActivationPreviewScreen.kt` + `PresetActivationPreviewViewModel.kt`
- [x] **P2-10** Activate action — `confirmActivate()` → `repository.activatePreset()` → pops back to Presets home

---

## Phase 3 — SRP Computation Pipeline ✅

**Goal:** Acquisitions auto-compute and store all SRPs using the active preset (INV-US-05, INV-US-06).  
**Depends on:** Phase 2 (active preset must exist).

### Computation

- [x] **P3-1** `SrpCalculator` — pure object, no Android dependencies, fully unit-testable
  - File: `data/pricing/SrpCalculator.kt` + `PricingChannelEngine.kt`
  - `SrpCalculatorTest`: 7 tests including exact-value assertions from PricingReference.md US-6 defaults
  - File: `test/.../data/pricing/SrpCalculatorTest.kt`

- [x] **P3-2** Wire into acquisition save path — `AcquisitionRepository.insertWithPricing()`
  - No active preset → saves with null SRPs, returns `SavedWithoutActivePreset` (snackbar warning shown in VM)
  - File: `data/repository/AcquisitionRepository.kt`

- [x] **P3-3** Wire into acquisition edit path — Option C — `AcquisitionRepository.updateWithPricing()`
  - Cost inputs changed → recompute from stored `channels_snapshot_json` (or active preset if no snapshot)
  - Metadata-only changes → preserve all SRP/snapshot columns unchanged

- [x] **P3-4** `getActiveSrpForProduct(productId)` — DAO + repository
  - Files: `data/local/dao/AcquisitionDao.kt`, `data/repository/AcquisitionRepository.kt`

- [x] **P3-5** SRP display on acquisition card — online SRP headline + expandable per-channel/package breakdown; null = "—"
  - File: `ui/screens/acquire/AcquireProduceScreen.kt` (`AcquisitionCard`, `hasSrpDetail()`)

---

## Phase 4 — Order-Taking with SRP Pre-fill ✅

**Goal:** Store assistant selects channel → SRP pre-fills from the active acquisition (ORD-US-01, ORD-US-02, ORD-US-08, ORD-US-10).  
**Depends on:** Phase 3.

- [x] **P4-1** CUS-US-05 — `CustomerType.defaultOrderChannel()` extension
  - WHOLESALE → `reseller`, RETAIL/REGULAR → `offline`; online must be manually selected
  - File: `data/pricing/SalesChannel.kt`

- [x] **P4-2** Order form — channel picker (FilterChip ×3); defaults from customer via `selectCustomer()`
  - On channel change: `setChannel()` → `repriceCart()` re-resolves price for every cart item
  - File: `ui/screens/order/TakeOrderScreen.kt`, `TakeOrderViewModel.kt`

- [x] **P4-3** SRP pre-fill in `ProductSelectionDialog`
  - `resolvePreviewUnitPrice()` → `OrderPricingResolver.resolveUnitPrice()` (SRP first, product_prices fallback)
  - Price shown as `Text` in dialog and `OrderItemCard` — not editable by staff
  - Files: `ui/screens/order/ProductSelectionDialog.kt`, `OrderItemCard.kt`, `data/pricing/OrderPricingResolver.kt`

- [x] **P4-4** ORD-US-08 — `ActiveSrpsScreen` — channel filter chips, per-product SRP rows, active preset footer
  - Fixed: dead conditional `"500g" else "500g"` → plain `"500g"`
  - Files: `ui/screens/order/ActiveSrpsScreen.kt`, `ActiveSrpsViewModel.kt`

- [x] **P4-5** ORD-US-10 — `OrderDetailScreen` — Print (top bar icon + body button), Mark as Paid (Switch + confirm dialog), Mark as Delivered (Switch), Edit (when unpaid)
  - File: `ui/screens/order/history/OrderDetailScreen.kt`

---

## Phase 5 — Auth & Profile ✅

**Goal:** Any user can change their own password; admin can manage user accounts (AUTH-US-04, AUTH-US-05).  
**Depends on:** Phase 1. Can be built in parallel with Phases 2–4.

- [x] **P5-1** Profile screen — username, full name, role (read-only); Change Password card; User Management card (admin-only)
  - Files: `ui/screens/profile/ProfileScreen.kt`, `ProfileViewModel.kt`

- [x] **P5-2** Change Password flow — current/new/confirm fields; verifies hash; field-level + banner errors; navigates back on success; session preserved
  - Files: `ui/screens/profile/ChangePasswordScreen.kt`, `ChangePasswordViewModel.kt`

- [x] **P5-3** Admin user management — create (username, full name, role, initial password); deactivate/reactivate; reset password; cannot self-deactivate; authorization double-checked in VM
  - Files: `ui/screens/profile/UserManagementScreen.kt`, `UserManagementViewModel.kt`

---

## Phase 6 — Export Completion ✅

**Goal:** All 10 tables exportable; selective export; SRP columns included on acquisitions (EXP-US-01, EXP-US-02).  
**Depends on:** Phase 1 (all tables populated).

- [x] **P6-1** Audit existing `CsvExportService` — identify which tables are currently exported
  - File: `data/export/CsvExportService.kt`
  - All 10 tables present with DeviceId on every row

- [x] **P6-2** Add missing tables to export (EXP-US-01)
  - All 10: users, products, customers, orders, order_items, employees, employee_payments, acquisitions, farm_operations, remittances
  - Each row includes `DeviceId` header column
  - Acquisitions export includes: `preset_ref`, all 15 SRP columns, `channels_snapshot_json`

- [x] **P6-3** EXP-US-02 — selective export
  - UI lets admin pick which tables to include before exporting — `bundleSelection: Set<ExportBundleTable>` with 10 entries, checkboxes, Select all/none, Export button
  - Files: `ui/screens/export/ExportScreen.kt`, `ExportViewModel.kt`, `ExportBundleTable.kt`

- [x] **P6-4** SYS-US-04 — verify Room generated schema JSON matches schema_evolution.sql VERSION 4
  - Verified against `app/schemas/com.redn.farm.data.local.FarmDatabase/4.json`
  - All 13 tables present; all 15 SRP columns on acquisitions; indices (product_id, preset_ref, date_acquired) ✅
  - Minor doc discrepancy (customers/product_prices `date_created` is INTEGER in Room vs TEXT in SQL reference — pre-existing from v3, non-functional)

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
