# Epic 8 — Remittances & disbursements — implementation tracker

**Goal:** One hub screen (`RemittanceScreen` / `RemittanceFormScreen`) for **REMITTANCE** (store assistant) and **DISBURSEMENT** (purchasing), `remittances.entry_type`, RBAC, CSV, thermal slip, EOD-ready DAO sums.

| Item | Status |
|------|--------|
| `RemittanceEntryType` + `entry_type` on entity/model/repository | `[x]` |
| `FarmDatabase` v10 (destructive rebuild adds column) | `[x]` |
| `RemittanceDao` sums: remittances only vs disbursements only | `[x]` |
| `Rbac`: hub access, `canWriteDisbursements` | `[x]` |
| `NavGraph` + `MainScreen` purchasing tile | `[x]` |
| `RemittanceViewModel` permissions + `addRemittance(..., entryType)` | `[x]` |
| `RemittanceScreen` filters, dual totals, type badge, dual add actions | `[x]` |
| `RemittanceFormScreen` `new` / `new_disbursement`, titles | `[x]` |
| `CsvExportService` `EntryType` column | `[x]` |
| `buildRemittanceSlip` disbursement title | `[x]` |
| `DatabaseMigrationViewModel` mapping | `[x]` |
| Instrumented `RemittanceDaoTest` entity defaults | `[x]` |
| `USER_STORIES.md` / `printing.md` statuses | `[x]` |

**Follow-up (optional):** Wire `DayCloseRepository` to `getSumRemittancesOnDate` / `getSumDisbursementsOnDate` when cash UI is built.
