# Promote to Production — Doc Cleanup Checklist

Goal: consolidate all design docs into a clean, agent-readable reference set that captures
the as-built state of the app. Iteration artifacts are removed; authoritative specs remain.

---

## Phase 1 — Resolve Open Items Before Deleting Trackers

- [ ] **Epic 3 — 3 pending items:** Check `EPIC3_PRODUCT_MANAGEMENT_TRACKER.md` for the
      three incomplete steps (product filters, price history screen, active SRP badge).
      Either mark the stories as open (`📋`) in `USER_STORIES.md` or close and mark done.
      Do not delete the tracker until this is resolved.

- [ ] **Stream H — doc sync:** Confirm in `PARTIAL_IMPLEMENTATION_PLAN.md` that Stream H
      (documentation sync) is fully closed. If any outstanding items remain, capture them
      in `USER_STORIES.md` or `KNOWN_ISSUES.md` before deleting.

- [ ] **Open bugs:** Read `bugs.md` and extract all open (unfixed) bugs. Either file them
      as GitHub Issues or add them to `KNOWN_ISSUES.md`. Do the same for open items in
      `BACKLOG.md` (bugs, design inconsistencies, tech debt, enhancements).

---

## Phase 2 — Create KNOWN_ISSUES.md

- [ ] Create `docs/KNOWN_ISSUES.md` containing:
      - Open bugs migrated from `bugs.md`
      - Open backlog items migrated from `BACKLOG.md` (bugs, inconsistencies, tech debt,
        enhancements — skip items already marked `[x]`)
      - The 3 pending Epic 3 items if not closed in Phase 1

- [ ] Format each entry with: ID, description, severity/type, and any known workaround.

---

## Phase 3 — Absorb UI Specs into DESIGN.md

- [ ] Review `UI-Improvement-Plan.md` and `UI_IMPROVEMENT_TRACKER.md` for any implemented
      UI patterns (layout decisions, numeric pad behavior, form conventions, print formatting)
      that are not already documented in `DESIGN.md` or `figma/UI-Spec.md`.

- [ ] Add a **UI Conventions** section to `DESIGN.md` capturing those patterns.

- [ ] Confirm `figma/UI-Spec.md` reflects the final implemented layout (5.99" portrait,
      handheld POS form factor).

---

## Phase 4 — Delete Iteration Artifacts

Delete the following files after Phases 1–3 are complete:

**Phase trackers (all phases completed):**
- [ ] `docs/PHASE1_TRACKER.md`
- [ ] `docs/PHASE2_TRACKER.md`
- [ ] `docs/PHASE3_TRACKER.md`
- [ ] `docs/PHASE4_TRACKER.md`
- [ ] `docs/PHASE5_TRACKER.md`
- [ ] `docs/PHASE6_TRACKER.md`

**Feature trackers (all ACs reflected in USER_STORIES.md):**
- [ ] `docs/EMP_EPIC_TRACKER.md`
- [ ] `docs/EOD_EPIC_TRACKER.md`
- [ ] `docs/EPIC3_PRODUCT_MANAGEMENT_TRACKER.md`  ← only after Phase 1 is done
- [ ] `docs/EPIC8_REMITTANCE_DISBURSEMENT_TRACKER.md`
- [ ] `docs/INV_ACQUISITION_SRP_TRACKER.md`
- [ ] `docs/USER_ROLES_TRACKER.md`

**UI iteration docs (absorbed into DESIGN.md in Phase 3):**
- [ ] `docs/UI_IMPROVEMENT_TRACKER.md`
- [ ] `docs/UI-Improvement-Plan.md`

**Closure / execution records:**
- [ ] `docs/PARTIAL_IMPLEMENTATION_PLAN.md`  ← only after Stream H confirmed closed

**Build planning (migration complete):**
- [ ] `docs/rebuild_plan.md`

**Design review findings (acted on; changes are in the code):**
- [ ] `docs/user_review_screens.md`
- [ ] `docs/user_review_screens_stories.md`
- [ ] `docs/user_review_product_management.md`

**Consolidated bug/backlog (migrated to KNOWN_ISSUES.md or GitHub Issues in Phase 2):**
- [ ] `docs/bugs.md`
- [ ] `docs/BACKLOG.md`

---

## Phase 5 — Verify Final Doc Set

After all deletions, the `docs/` folder should contain only:

```
docs/
  # As-Built Requirements
  USER_STORIES.md               ← all epics, stories, ACs — ✅ / 📋 / 🔧 status
  US-ACTOR-MANAGEMENT.md        ← admin/owner story summary
  US-ACTOR-STORE-ASSISTANT.md   ← store assistant story summary
  US-ACTOR-PURCHASING-ASSISTANT.md
  US-ACTOR-FARMER.md

  # Architecture & Design
  DESIGN.md                     ← canonical architecture, nav routes, DI, auth
  user_roles.md                 ← RBAC permission matrix
  schema_evolution.sql          ← DDL history
  figma/UI-Spec.md              ← UI layout spec

  # Domain Specs
  PricingReference.md           ← SRP math, markups, rounding
  pricing_clarif.md             ← edge cases (CLARIF-01, BUG-PRC-04, etc.)
  printing.md                   ← all 10 print types (PRN-01 through PRN-10)

  # Maintenance Reference
  KNOWN_ISSUES.md               ← open bugs and enhancements (created in Phase 2)
  build_framework.md            ← lessons learned for future agents
  other_uses.md                 ← business reuse analysis (15 adaptations)
  code_summary.md               ← codebase snapshot (regenerate at release)

  # Future / Deferred
  apis.md                       ← REST API spec (backend not yet built)
  server_design.md              ← Drupal backend architecture (planned)
```

- [ ] Confirm every file in the list above exists and is current.
- [ ] Confirm no files remain in `docs/` outside this list.
- [ ] Update `CLAUDE.md` if any references to deleted doc files need to be removed.

---

## Notes

- `code_summary.md` will go stale. Regenerate it at each production release or rely on
  agents reading the code directly. Decision can be deferred.
- `apis.md` and `server_design.md` are kept as future-scoped items; mark them clearly
  as **"not yet implemented"** at the top of each file if not already done.
- `other_uses.md` is business IP — keep as-is.
