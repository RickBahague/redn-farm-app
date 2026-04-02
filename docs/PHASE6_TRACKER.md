# Phase 6 — Export completion (detailed tracker)

**Created:** 2026-04-02  
**Related plan:** `rebuild_plan.md` Phase 6 (this file does **not** modify that document).  
**Goal:** EXP-US-01 (all core tables + acquisitions SRP columns), EXP-US-02 (selective multi-file export), SYS-US-04 note (schema verification).

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Implemented in codebase |
| `[~]` | Partial / simplified — see notes |
| `[ ]` | Manual QA / deep diff follow-up |

---

## P6-1 — Audit `CsvExportService` (before this phase)

| Previously exported | Notes |
|---------------------|--------|
| customers, orders, order_items, employees, employee_payments, farm_operations, product_prices, products, remittances, acquisitions | Acquisitions CSV lacked pricing/SRP columns; orders lacked `channel`; products lacked v4 fields; **users** missing |

---

## P6-2 — All ten core tables + acquisition SRP columns (EXP-US-01)

| Table | Status | Notes |
|-------|--------|--------|
| users | `[x]` | `exportUsers`; **password hash omitted** from CSV |
| products | `[x]` | Adds `Category`, `DefaultPieceCount`; quoted `product_id` |
| customers | `[x]` | `DeviceId`; CSV escaping for text fields |
| orders | `[x]` | Adds **`Channel`** |
| order_items | `[x]` | `DeviceId` + escaping |
| employees | `[x]` | As before + escaping |
| employee_payments | `[x]` | As before + escaping |
| acquisitions | `[x]` | `PresetRef`, spoilage/hauling fields, **`ChannelsSnapshotJson`**, all **15 SRP** columns, `CreatedAt`, `PieceCount` |
| farm_operations | `[x]` | Escaping; enum types as strings |
| remittances | `[x]` | Escaping |

**Also retained (not in the “10”):** `product_prices` export includes discounted columns.

**Files:** `app/src/main/java/com/redn/farm/data/export/CsvExportService.kt`, `ExportViewModel.kt` (`exportUsers`), `ExportScreen.kt` (Users row).

---

## P6-3 — Selective export (EXP-US-02)

| Item | Status | Notes |
|------|--------|--------|
| Pick tables before export | `[x]` | Checkboxes for each `ExportBundleTable` |
| One CSV per table | `[x]` | Same `yyyyMMdd_HHmmss` suffix on every file in a batch |
| Output directory | `[x]` | `Android/data/.../files/exports/` (unchanged) |
| Admin-only UI | `[x]` | Selective card shown when `ExportViewModel.isAdmin` |

**Files:** `ExportBundleTable.kt`, `ExportViewModel.exportSelectedBundle`, `ExportScreen.kt`.

---

## P6-4 — SYS-US-04 (Room vs `schema_evolution.sql` VERSION 4)

| Item | Status | Notes |
|------|--------|--------|
| Generated DDL location | `[x]` | This project uses **KSP**, not kapt: `app/build/generated/ksp/debug/java/com/redn/farm/data/local/FarmDatabase_Impl.java` |
| Manual diff | `[ ]` | Compare `_db.execSQL("CREATE TABLE...` blocks in that file to `docs/schema_evolution.sql` **VERSION 4** section |
| `FarmDatabase` version | `[x]` | `@Database(version = 4)` in `FarmDatabase.kt` |

---

## Build

| Step | Status |
|------|--------|
| `./gradlew assembleDebug` | `[x]` (2026-04-02) |

---

## Follow-ups (optional)

- Share/export intent from success dialog (share sheet).
- Non-admin: hide or read-only Export screen if product policy requires it.
