# Phase 6 — Export completion (detailed tracker)

**Created:** 2026-04-02 · **Aligned:** 2026-04-09  
**Related plan:** `rebuild_plan.md` Phase 6 (this file does **not** modify that document).  
**Goal:** EXP-US-01 (all core tables + acquisitions SRP columns), **batch CSV export** (checkbox bundle), SYS-US-04 note (schema verification).  

**Naming note:** In **`USER_STORIES.md`**, **EXP-US-02** is **Clear / truncate tables** (still partial — see [`PARTIAL_IMPLEMENTATION_PLAN.md`](./PARTIAL_IMPLEMENTATION_PLAN.md) Stream C). This tracker’s **P6-3** is that **selective multi-table export**, not truncate.

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

## P6-3 — Selective **export** (batch CSV — maps to **EXP-US-01** bundle UX)

| Item | Status | Notes |
|------|--------|--------|
| Pick tables before export | `[x]` | Checkboxes for each `ExportBundleTable` |
| One CSV per table | `[x]` | Same `yyyyMMdd_HHmmss` suffix on every file in a batch |
| Output directory | `[x]` | `Android/data/.../files/exports/` (unchanged) |
| Admin-only UI | `[x]` | Selective card shown when `ExportViewModel.isAdmin` |
| `product_prices` in bundle | `[ ]` | Still **individual export** only; optional follow-up |

**Files:** `ExportBundleTable.kt`, `ExportViewModel.exportSelectedBundle`, `ExportScreen.kt`.

---

## P6-4 — SYS-US-04 (Room vs `schema_evolution.sql`)

| Item | Status | Notes |
|------|--------|--------|
| Generated DDL location | `[x]` | This project uses **KSP**: `app/build/generated/ksp/debug/java/com/redn/farm/data/local/FarmDatabase_Impl.java` |
| Manual diff | `[x]` | **VERSION 10** in **`docs/schema_evolution.sql`** matches KSP **`FarmDatabase_Impl.createAllTables`** (Stream D 2026-04-09) |
| `FarmDatabase` version | `[x]` | **`FarmDatabase.kt` `version = 10`**; bump → refresh **VERSION N** + identity hash note in **`schema_evolution.sql`** |

---

## Build

| Step | Status |
|------|--------|
| `./gradlew assembleDebug` | `[x]` (2026-04-02) |

---

## Follow-ups (optional)

- Share/export intent from success dialog (share sheet).
- Non-admin: hide or read-only Export screen if product policy requires it.
- ~~**EXP-US-02** batch clear~~ — done 2026-04-09 (Stream C); see **USER_STORIES.md**.
