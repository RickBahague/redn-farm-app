# Known Issues

Purpose: consolidated open issues migrated from the former bug/backlog trackers.

Format per item: **ID**, **Description**, **Severity/Type**, **Workaround**.

---

## Migrated from legacy bug tracker (open statuses at migration time)

- **ID:** BUG-ARC-03  
  **Description:** RBAC parity audit is incomplete; route/action permissions may drift from `Rbac.kt` and `user_roles.md`.  
  **Severity/Type:** High / Architecture-Process  
  **Workaround:** Restrict release approvals to admin-only sensitive paths after a manual role walkthrough.

- **ID:** BUG-ARC-04  
  **Description:** Incremental Room migrations are missing beyond `2->3`; newer schema bumps still rely on destructive fallback.  
  **Severity/Type:** High / Data-Migration  
  **Workaround:** Require clean installs for release candidates and avoid in-place upgrade expectations in production-like testing.

- **ID:** BUG-ARC-05  
  **Description:** Fresh-install regression (`./scripts/dev.sh fresh`) is not enforced as the default QA path.  
  **Severity/Type:** Medium / QA-Process  
  **Workaround:** Add explicit "fresh install" as a manual PR and release checklist step.

- **ID:** BUG-ARC-06  
  **Description:** Numeric pad and date-picker UX patterns are not fully standardized across screens.  
  **Severity/Type:** Medium / UI-Consistency  
  **Workaround:** Follow the existing canonical patterns (`AcquisitionFormScreen`, `RemittanceFormScreen`) for new changes.

- **ID:** BUG-ARC-07  
  **Description:** Pricing/formula changes are not consistently gated by required unit tests.  
  **Severity/Type:** High / Test-Governance  
  **Workaround:** Treat `testDebugUnitTest` and targeted pricing tests as mandatory reviewer checks.

- **ID:** BUG-ARC-08  
  **Description:** Full repo audit for `/kg` vs `/pc` display/print semantics is still open.  
  **Severity/Type:** High / Pricing-Units  
  **Workaround:** Manually verify unit-sensitive views (Take Order, Active SRPs, thermal print) before release sign-off.

---

## Migrated from legacy backlog (open items, excluding `[x]` at migration time)

### Bugs (P1)

- **ID:** BUG-01  
  **Description:** Acquire location filter is not applied in filtering predicate.  
  **Severity/Type:** P1 / Bug  
  **Workaround:** Use search/date filters and verify location manually in list rows.

- **ID:** BUG-03  
  **Description:** `date_updated` is dropped in remittance entity-to-model mapping.  
  **Severity/Type:** P1 / Bug  
  **Workaround:** Treat remittance "updated" timestamps as unreliable until mapping is fixed.

- **ID:** BUG-04  
  **Description:** Acquire view model subscribes to acquisitions twice.  
  **Severity/Type:** P1 / Bug-Performance  
  **Workaround:** None functional; monitor for unnecessary reactive load in large datasets.

- **ID:** BUG-05  
  **Description:** Product filtering path calls a suspend DAO method inside `Flow.map`.  
  **Severity/Type:** P1 / Concurrency-Bug  
  **Workaround:** Avoid heavy "sort by price" workflows on low-end devices during large list sessions.

### Design inconsistencies (P2)

- **ID:** DI-01  
  **Description:** Mixed Hilt and manual ViewModel factory patterns remain in the app.  
  **Severity/Type:** P2 / Architecture  
  **Workaround:** Prefer Hilt-injected code paths for new screens and refactors.

- **ID:** DI-02  
  **Description:** `DatabaseMigrationScreen` route exists but is unreachable from UI.  
  **Severity/Type:** P2 / Navigation-DeadPath  
  **Workaround:** Use DB export/migration tasks through existing dev utilities, not UI route.

- **ID:** DI-03  
  **Description:** Backlog doc still flags seed JSON and initializer disconnect.  
  **Severity/Type:** P2 / Documentation-Drift  
  **Workaround:** Follow current on-create seed behavior documented in Epic closure notes.

- **ID:** DI-05  
  **Description:** Date/time consistency concerns remain listed in backlog for full cross-layer alignment.  
  **Severity/Type:** P2 / Data-Consistency  
  **Workaround:** Standardize new persistence changes on epoch millis and verify conversion boundaries manually.

### Technical debt (P3)

- **ID:** TD-01  
  **Description:** `UserEntity.role` remains stringly typed in backlog tracking.  
  **Severity/Type:** P3 / Type-Safety  
  **Workaround:** Keep role checks centralized and avoid ad hoc string comparisons.

- **ID:** TD-02  
  **Description:** Backlog tracks incomplete UI RBAC enforcement as debt.  
  **Severity/Type:** P3 / Security-UX  
  **Workaround:** Restrict sensitive operational actions to known admin flows during UAT.

- **ID:** TD-03  
  **Description:** Session expiry/idle timeout is not enforced.  
  **Severity/Type:** P3 / Security  
  **Workaround:** Use manual logout policy for shared devices.

- **ID:** TD-04  
  **Description:** Unused Bluetooth permissions remain declared in manifest.  
  **Severity/Type:** P3 / Manifest-Hygiene  
  **Workaround:** None required for runtime behavior; track for hardening pass.

- **ID:** TD-05  
  **Description:** Destructive truncate actions lack confirmation dialogs in backlog tracking.  
  **Severity/Type:** P3 / Data-Safety  
  **Workaround:** Require operator double-check and role supervision before truncate actions.

- **ID:** TD-06  
  **Description:** Legacy reinitialize flow flagged as unsafe full DB deletion path.  
  **Severity/Type:** P3 / Data-Safety  
  **Workaround:** Do not expose or use full reinitialize in production paths.

- **ID:** TD-07  
  **Description:** Product ID creation strategy remains listed as manual/fragile debt.  
  **Severity/Type:** P3 / Data-Model  
  **Workaround:** Enforce team conventions for product ID generation during data entry.

- **ID:** TD-08  
  **Description:** Password hashing algorithm upgrade (SHA-1 variant to stronger profile) remains open in backlog.  
  **Severity/Type:** P3 / Security  
  **Workaround:** Keep strong credential policies and prioritize upgrade before broad rollout.

### Enhancements (P4)

- **ID:** ENH-01  
  **Description:** User management UI enhancement remains open.  
  **Severity/Type:** P4 / Enhancement  
  **Workaround:** Use existing admin-only operational flows for user lifecycle tasks.

- **ID:** ENH-02  
  **Description:** Main screen dashboard summary enhancement remains open.  
  **Severity/Type:** P4 / Enhancement  
  **Workaround:** Use existing reports/screens for operational daily summaries.

- **ID:** ENH-03  
  **Description:** Print receipt action from order screen remains open.  
  **Severity/Type:** P4 / Enhancement  
  **Workaround:** Use available print/report entry points outside direct order flow.

- **ID:** ENH-04  
  **Description:** Pagination for large list screens remains open.  
  **Severity/Type:** P4 / Performance-Enhancement  
  **Workaround:** Use tighter search/filter windows for heavy datasets.

- **ID:** ENH-05  
  **Description:** DAO-backed customer search is not fully used across all relevant screens.  
  **Severity/Type:** P4 / Enhancement  
  **Workaround:** Use narrower query terms and date filters to reduce in-memory list pressure.

- **ID:** ENH-06  
  **Description:** Export flow does not yet include FileProvider share action.  
  **Severity/Type:** P4 / Enhancement  
  **Workaround:** Retrieve exported files manually from app storage.
