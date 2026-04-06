# App Build Framework — Lessons from Farm App

**Written:** 2026-04-05  
**Derived from:** Farm App (`com.redn.farm`) — a Kotlin/Compose Android POS + operations management app built across 6 phases, 11 tracked epics, 155 source files, 23k lines.

This document is a practitioner's guide for building similar apps: small-to-medium Android business tools (POS, inventory, field ops, payroll) running on dedicated or consumer hardware with optional companion web services. It captures the sequence that worked, the patterns worth repeating, and the mistakes worth avoiding.

---

## 1. What "similar apps" means

This framework applies when:

- **Single organisation, internal use** — one business, staff devices, not a public app store product
- **Offline-first** — the device must work without a network connection; sync is a nice-to-have, not load-bearing
- **Data is the product** — the app's value is capturing and querying structured operational data (orders, inventory, payroll, etc.)
- **Mixed roles** — at least two user types (operator vs. manager) with different permissions
- **Thermal or receipt printing** — Sunmi, Epson, or system print
- **One developer or a small AI-assisted team** — decisions must be explicit and traceable; there is no institutional memory

---

## 2. Before writing any code

### 2.1 Write a DESIGN.md first

The single most valuable document in the project. It must answer:

| Question | Why it matters |
|----------|----------------|
| Who are the actors? | Drives screen inventory and RBAC |
| What data exists and how does it relate? | Drives the DB schema |
| What are the navigation routes? | Drives the nav graph |
| What are the authentication rules? | Prevents late-stage rework |
| What are the known inconsistencies? | Sets honest expectations |

Keep it short. One page of schema tables and one page of user stories is enough to start. **Do not over-design** — the document will drift from reality; treat it as a reference, not a contract.

### 2.2 Write USER_STORIES.md with acceptance criteria

Group stories by actor. For each story, write 3–10 acceptance criteria (ACs). ACs are your functional test cases. Any story without ACs is not implementable.

Minimum story fields:
```
US-XX: As a [actor], I can [action] so that [value].
AC1: ...
AC2: ...
```

### 2.3 Define roles early — even if not enforced yet

List every role and what it can and cannot do in a matrix. A single `admin` vs `user` distinction always splits into more roles before launch. Capturing this upfront prevents retrofitting RBAC into screens that were never designed for it.

### 2.4 Design the database schema before the first screen

Draw every table, every FK, every nullable column, and every index. Decide:
- Which IDs are auto-increment vs manually assigned
- Which deletes cascade, restrict, or set null
- Which timestamps are stored as `Long` (epoch millis) vs `LocalDateTime`
- What the schema version number is and what the migration strategy will be

During active development, **destructive migration is acceptable**. Define the crossover point (e.g. "first production install") at which you must write incremental migrations, and put it in writing.

---

## 3. Phase sequence that worked

The Farm App followed six phases in strict order. This sequence is worth repeating.

```
Phase 0 — DB Foundation
Phase 1 — Restore / Build all CRUD
Phase 2 — Business Logic Layer (presets, rules)
Phase 3 — Computation Engine (pure, tested)
Phase 4 — Integration (wire engine into flows)
Phase 5 — Auth & Roles
Phase 6 — Export & Audit
```

### Phase 0 — DB Foundation

**Goal:** A clean, compiling Room database with every entity and DAO you will need.

- Define all entities with all columns — including columns you won't use yet (mark them nullable)
- Register all entities in `FarmDatabase`
- Write all DAOs with at minimum `insert`, `update`, `delete`, `getAll`, `getById`
- Verify with `./gradlew assembleDebug` before touching any UI

**Why first:** Every subsequent phase depends on the schema. A schema change in Phase 3 forces rework across Phases 1 and 2. Get it right here.

**Key decision:** version number + migration strategy. Write it in DESIGN.md now.

### Phase 1 — Restore / Build all CRUD

**Goal:** Every entity has a working screen: list, add, edit, delete. No business logic yet — just basic persistence.

- One screen + one ViewModel per entity
- List → add form → save → back to list
- Basic search/filter
- No pricing, no calculations, no exports — just the data layer working end-to-end
- Smoke test on device before proceeding

**Why second:** Business logic built on top of a broken CRUD layer is painful to debug. Confirm the data flows first.

### Phase 2 — Business Logic Layer

**Goal:** Configurable rules that govern how the app computes things — pricing presets, rate tables, role permissions, thresholds.

- Presets stored in the DB (not hardcoded)
- A management-only UI to define them
- History / audit trail of changes
- Snapshot pattern: when a preset is used to compute something, store a copy of the preset parameters alongside the result so old records remain auditable even after the preset changes

**Key pattern — the snapshot:**
```
acquisition.channels_snapshot_json  ← copy of preset at time of acquisition
acquisition.preset_ref              ← which preset was used
```
This was one of the most important architectural decisions in the Farm App. Without it, re-editing old records would silently recompute them with new preset values.

### Phase 3 — Computation Engine

**Goal:** A pure, testable computation layer with no Android dependencies.

- Separate `object` or class in `data/pricing/` (or equivalent)
- Takes `Input` data class, returns `Output` data class or `Result.Ok`/`Result.Error`
- Zero Room, zero Compose, zero Context
- Fully unit-testable with JVM tests only

**Why pure:** Compose previews, unit tests, and future web/server ports can all use the same logic. The Farm App's `SrpCalculator` + `PricingChannelEngine` followed this pattern and paid off immediately in testability and debuggability.

**Write the tests before wiring the UI.** If your formula is wrong, you want to know from a failing JUnit test, not from a confused user.

### Phase 4 — Integration

**Goal:** Wire the computation engine into the save/display flows.

- Repository layer: call the engine on save, persist outputs alongside inputs
- ViewModel layer: expose live preview (call engine with draft inputs before save)
- UI layer: show computed values reactively; never re-run the engine in a Composable

**Key pattern — live preview:**
Show the user what the system will compute *before* they commit. In the Farm App, the acquisition form shows SRP previews while the user types quantity and cost. This catches formula or input errors before they're stored.

### Phase 5 — Auth & Roles

**Goal:** Enforce role-based access. Never do this last — retrofitting RBAC into completed screens is expensive.

However, Phase 5 is intentionally after Phase 4 because:
- You cannot define the right permissions until you know what all the screens do
- You cannot write nav guards until the nav graph is stable
- Hilt-based ViewModels make it easy to inject a session manager anywhere

Minimum deliverables:
- `SessionManager` in SharedPreferences
- `Rbac` object: pure functions returning `Boolean` — no Android deps
- `RbacGate` composable: wraps nav graph destinations
- Dashboard tile filtering by role
- In-screen write guards (hide add/edit buttons for read-only roles)

### Phase 6 — Export & Audit

**Goal:** Every table exportable to CSV; every key action has an audit trail.

- `CsvExportService`: one method per table, writes to `getExternalFilesDir`
- Include `device_id` in every export row (Android ID for multi-device traceability)
- Export all columns including computed/snapshot columns — not just the "nice" ones
- Selective export UI (checkboxes) so users can pull one table at a time

**Don't skip export.** Even internal apps need data recovery paths, accounting integration, and debugging.

---

## 4. Architecture decisions

### 4.1 MVVM + Hilt + Room + Compose

This stack is the right default for this class of app. No alternatives needed.

**Caveat — mixed DI:** The Farm App ended up with a mix of Hilt-injected and manually-constructed ViewModels. This causes duplicate repository instances. Pick one pattern and stick to it. **Use Hilt everywhere** — the manual factory pattern adds boilerplate with no benefit.

### 4.2 StateFlow for UI state

All ViewModels expose `StateFlow<T>`, collected in Composables via `collectAsState()`. This is correct.

**Never** read from a `StateFlow.value` inside a Composable to drive display state — it won't recompose. Always use `collectAsState()`.

### 4.3 Repository as the only DB gateway

ViewModels never call DAOs directly. All DB access goes through a repository. Repositories do:
- Entity ↔ domain model mapping
- Timestamp management
- Computed column population (call the engine on save)
- Nothing else — no business logic beyond what's needed for mapping

### 4.4 Domain models separate from entities

Room entities are annotated Kotlin classes tied to the DB schema. Domain models are plain data classes the UI and engine work with. Always map between them in the repository.

This separation means you can change the DB schema without touching ViewModels or Composables.

### 4.5 Single NavGraph file

All routes defined in one `NavGraph.kt` as a `Screen` sealed class with `createRoute()` helpers. Every parameterized route (e.g. `edit_order/{orderId}`) has a typed helper. This is the right pattern — nav bugs are much easier to track when there is one file to read.

### 4.6 Computation engine is a pure Kotlin object

No Android imports. Input/Output are data classes. Results are sealed `Result.Ok`/`Result.Error`. This is the most important structural decision for a calculation-heavy app.

---

## 5. Data layer patterns

### 5.1 Price / rate history — never overwrite

Always **insert** a new row when a price changes. Retrieve the current value via `MAX(date_created)`. This gives you a free audit trail and lets you recompute historical records against the price that was active at the time.

```sql
-- Latest price per product
SELECT * FROM product_prices p1
WHERE date_created = (SELECT MAX(date_created) FROM product_prices p2 WHERE p1.product_id = p2.product_id)
```

### 5.2 Snapshot pattern for computed results

When a calculation uses configurable parameters (preset, rate, formula), store a JSON snapshot of those parameters alongside the result. This is critical for auditability and for re-editing records without silently changing historical values.

```
acquisitions.channels_snapshot_json  ← preset channel config at acquisition time
acquisitions.preset_ref              ← FK to preset used
acquisitions.spoilage_rate           ← resolved value at acquisition time
acquisitions.additional_cost_per_kg  ← resolved value at acquisition time
```

On edit: if cost inputs changed, recompute using the **stored snapshot**, not the current preset. If the user explicitly re-activates a new preset, that is a separate action.

### 5.3 Nullable computed columns, not a separate table

Per-channel SRP outputs (`srp_online_per_kg`, `srp_reseller_per_kg`, etc.) live as nullable columns on the `acquisitions` table, not in a separate table. This simplifies queries enormously. Null = no preset was active at time of save; the UI falls back gracefully.

### 5.4 `created_at` vs `date_acquired`

Every entity that represents a real-world event needs two timestamps:
- `date_acquired` / `order_date` — the **business date** (what the user entered; may be backdated)
- `created_at` — the **system timestamp** at DB insert time

Never use `created_at` for business logic. Use it only for tiebreaking queries (e.g., "latest acquisition per product").

### 5.5 Consistent timestamp storage

Pick one and enforce it everywhere. The Farm App mixed `Long` (epoch millis) in some entities and `LocalDateTime` (converted via DateTimeConverter as epoch seconds) in others. This caused subtle off-by-1000 bugs. **Use epoch millis (`Long`) everywhere** — it is unambiguous, JSON-friendly, and needs no converter.

---

## 6. UI patterns for POS / ops apps

### 6.1 Dialog vs. full-screen: the decision rule

| Form size | Pattern |
|-----------|---------|
| ≤ 4 short fields, no scrolling | `AlertDialog` with `imePadding()` |
| ≥ 5 fields, or any scrolling | Full-screen route with `Scaffold` |
| Number / weight entry | `ModalBottomSheet` numeric pad — never soft keyboard |
| Date selection | `OutlinedCard` tap → `DatePickerDialog` with `DatePicker` — no inline clipped pickers |
| Destructive confirm | Always `AlertDialog` — never immediate |
| Status toggle (paid, delivered) | Direct toggle + `Snackbar("Undo")` — no confirmation dialog |

### 6.2 Primary action placement

Always in `Scaffold.bottomBar` or a full-width `FilledButton` at the bottom of a scrollable form. A button the user has to scroll to reach is a button they will miss. On a 5.99" POS device, anything above the fold may be covered by the keyboard.

### 6.3 Shared numeric pad component

Build one `NumericPadBottomSheet` composable and reuse it everywhere quantity, price, or amount is entered. Hardcoding numeric input into multiple screens diverges quickly. The Farm App built this in Phase 2 and used it across acquisition, order, payment, and remittance flows.

**Scroll vs. press:** Do not open the pad from **`collectIsPressedAsState()`** (or similar) on the **read-only field** inside a **vertically scrolling** form (e.g. pricing preset editor). A scrolling finger briefly presses children as it moves, which opens the pad by mistake. Prefer opening the pad **only from an explicit control** (dialpad trailing icon). If you need tap-on-field for a tiny non-scroll dialog, gate it behind a parameter (Farm App: `NumericPadOutlinedTextField(..., openPadOnFieldPress = true)` only where the trade-off is acceptable). Track regressions in **`docs/bugs.md`**.

### 6.4 Live computation preview

Show the user what the system will compute before they save. This is especially important for:
- Pricing / SRP (show the calculated SRP as qty and cost are typed)
- Net pay (show gross + advance = net pay on the payment form)
- Order line totals (show qty × price = total as items are added)

Keep preview logic in the ViewModel (`resolvePreviewUnitPrice`, `previewDraftPricing`), not in the Composable.

### 6.5 Cart / session state in ViewModel, not in Composable

Never store cart items, unsaved form state, or multi-step wizard progress in `remember {}`. It disappears on recomposition. Use `MutableStateFlow` in the ViewModel and clear it explicitly on success or cancel.

### 6.6 Edge-to-edge + imePadding

Set `WindowCompat.setDecorFitsSystemWindows(window, false)` in `MainActivity`. Apply `imePadding()` to every screen with a text field. Without this, the keyboard covers the primary action button on small-screen devices.

---

## 7. Printing

### 7.1 Two print modes, not one

| Mode | When | API |
|------|------|-----|
| Sunmi thermal (`printMessage`) | On-device Sunmi POS; fast, 58mm slip | Sunmi SDK `SunmiPrinterService` |
| Android print (`printText`) | Other devices; full-page receipts | `PrintManager` + WebView |

Build both from day one. The Sunmi SDK is device-specific and not testable in an emulator — stub it behind an interface if you need unit tests.

### 7.2 Central print builder module

All thermal string construction belongs in one file (`ThermalPrintBuilders.kt` or equivalent). Keep it pure — no Android imports, no context, just `buildString {}` functions that take domain models and return `String`.

Benefits:
- Testable without a device
- Consistent 32-char line width enforcement
- Easy to add new slip types without scattering `buildString` blocks across screens

### 7.3 Per-unit-type awareness in every print function

Every print function that shows a price must check `is_per_kg` and use the appropriate column and label (`/kg` vs `/pc`). This was a real bug in the Farm App (BUG-ACQ-06 / BUG-ACQ-08). Template:

```kotlin
if (acquisition.is_per_kg) {
    appendLine(formatThermalLine("SRP Online:", "${fmt(acq.srp_online_per_kg)}/kg"))
} else {
    appendLine(formatThermalLine("SRP Online:", "${fmt(acq.srp_online_per_piece)}/pc"))
}
```

---

## 8. SRP / pricing engine design

This section is specific to apps with a configurable markup/cost pipeline, but the pattern generalises to any rule-based computation.

### 8.1 Separate input from formula from output

```
Input (data class)  →  Engine (pure object)  →  Output (data class)
```

Never mix UI state, DB entities, or Android context into the engine.

### 8.2 Handle per-unit-type differences in the formula, not in the caller

CLARIF-01 established that per-piece acquisitions use a different formula (no spoilage, cost per piece directly). This belongs inside the engine:

```kotlin
val effectiveSpoilage = if (input.pieceCount != null && input.pieceCount > 0.0) 0.0
                        else input.spoilageRate
```

If you push this logic into the caller (repository, screen), it will be inconsistently applied.

### 8.3 Output carries everything the UI needs

Include diagnostic fields in `Output` — `bulkQuantityKg`, `spoilageRate` (effective), `additionalCostPerKg`, `costPerSellableKg` — so the form preview can show its working without recomputing. Users trust a form that shows its maths.

### 8.4 Preset activation is a transaction

Activating a new preset must:
1. Mark the previous active preset as inactive
2. Insert a `preset_activation_log` row
3. Record `activated_at` timestamp

Do this in a single `@Transaction` DAO method. Never let a crash leave two active presets.

---

## 9. RBAC implementation pattern

```kotlin
// 1. Pure constants + helper functions
object Rbac {
    const val ADMIN = "ADMIN"
    const val STORE_ASSISTANT = "STORE_ASSISTANT"
    // ...
    fun canWriteOrders(role: String): Boolean = role in listOf(ADMIN, STORE_ASSISTANT)
    fun dashboardTileTitles(role: String): List<String> = when (role) { ... }
}

// 2. Composable nav guard
@Composable
fun RequireRole(role: String, allowed: List<String>, navController: NavController, content: @Composable () -> Unit) {
    if (Rbac.normalize(role) in allowed) content() else LaunchedEffect(Unit) { navController.navigateUp() }
}

// 3. ViewModel double-check (defence in depth)
fun placeOrder() {
    if (!Rbac.canWriteOrders(sessionManager.getRole())) {
        _userMessage.emit("You don't have permission to place orders.")
        return
    }
    // ...
}
```

Rules:
- RBAC functions are pure — no Android, no DB, just string in / boolean out
- Enforce at nav layer (hide destinations) AND in ViewModels (reject mutations)
- Never rely on hiding UI elements alone — a determined user can navigate around them
- Seed at least 3 demo users with different roles in `FarmDatabase.onCreate`

---

## 10. Testing strategy

### What to test

| Layer | Test type | Priority |
|-------|-----------|----------|
| Computation engine (`SrpCalculator`, etc.) | JVM unit | P1 — test before wiring |
| RBAC functions | JVM unit | P1 |
| Domain model helpers (`netPayAmount`, etc.) | JVM unit | P1 |
| DAO queries | Instrumented (Room in-memory) | P2 |
| Repository save/load round-trips | Instrumented | P2 |
| Screen logic (ViewModel) | JVM unit with Mockito | P3 |
| End-to-end UI | Espresso / manual | P3 |

### What not to test

- Composable rendering (high maintenance, low signal for business logic)
- Room entity field definitions (the schema JSON export covers this)

### Engine-first testing

Write `SrpCalculatorTest` **before** wiring `AcquisitionRepository`. Verify the formula with hand-calculated examples from the spec (CLARIF-01, user stories ACs). A passing formula test means the UI will be correct; a failing formula test found in production means weeks of data reconciliation.

---

## 11. Documentation that pays off

Not all docs are worth maintaining. These are:

| Doc | Value | Keep updated? |
|-----|-------|---------------|
| `DESIGN.md` | Schema, navigation, patterns | Yes — it is the source of truth |
| `USER_STORIES.md` | ACs drive implementation | Yes — add new stories, never delete old ones |
| `bugs.md` | Tracks open/closed bugs with root cause | Yes — every bug gets a root cause |
| `CLAUDE.md` | Build commands, architecture summary for AI agents | Yes — always current |
| Phase trackers (`PHASE1_TRACKER.md`, etc.) | Sequencing and status | No — close them when done, don't update retroactively |
| `code_summary.md` | LOC, file inventory | Regenerate on demand |

Docs that waste time:
- Architecture diagrams that drift from reality within a week
- Detailed sequence diagrams for flows that change every sprint
- A `CHANGELOG.md` that duplicates git log

---

## 12. Dev tooling

### `scripts/dev.sh`

Write this early. Minimum commands:

```bash
./scripts/dev.sh install     # build + install debug APK
./scripts/dev.sh fresh       # uninstall + install + launch (clean state for testing schema changes)
./scripts/dev.sh log         # filtered logcat (your app tag only)
./scripts/dev.sh log-crash   # errors and crashes only
./scripts/dev.sh pull-exports # pull CSVs to desktop
./scripts/dev.sh devices     # list connected devices (multi-device setups)
```

The `fresh` command (uninstall → install → launch) is essential when using `fallbackToDestructiveMigration()` — it ensures you're always testing against a clean DB schema.

### CLAUDE.md

If you use AI coding assistants, maintain a `CLAUDE.md` at the project root with:
- Build commands
- Architecture overview (layer structure, DI pattern, DB version, migration strategy)
- Key design decisions (pricing formula, session management, navigation conventions)
- Known inconsistencies (so the AI doesn't "fix" them by accident)

A well-maintained `CLAUDE.md` eliminates 80% of the context-setting needed at the start of each AI session.

---

## 13. Common pitfalls

| Pitfall | What happened | Prevention |
|---------|---------------|------------|
| **Mixed DI pattern** | Some VMs use Hilt, others use manual factory; duplicate repository instances | Use Hilt everywhere from Phase 0 |
| **Timestamp inconsistency** | Some entities use epoch millis, others use epoch seconds; off-by-1000 bugs | Pick one (millis) and enforce it in code review |
| **Print builder duplication** | Same `buildString` block copied into two screen files | Build central `ThermalPrintBuilders.kt` on first print requirement |
| **Formula in wrong layer** | Spoilage not applied correctly to per-piece (BUG-ACQ-07) | All formula logic in the pure engine; none in Repository or UI |
| **Stale bug tracker** | Tracker says `[x]` but fix was incomplete (BUG-ACQ-06) | Verify on device before marking done; include verification steps in the bug entry |
| **RBAC retrofitted late** | Nav guards added after screens built; easy to miss an unprotected route | Plan roles in Phase 0 even if not enforced until Phase 5 |
| **No snapshot on configurable compute** | Edit with new preset silently changes historical record | Snapshot pattern on every configured computation |
| **Dialog for complex forms** | 8-field form crammed into AlertDialog; keyboard covers save button | Apply the 4-field rule strictly from Phase 1 |
| **Per-unit-type blindness in print** | Print builder always showed `/kg` SRP, even for per-piece acquisitions | Every price display checks `is_per_kg`; per-piece products must never show kg-labelled prices |
| **Numeric pad opens while scrolling** | Press detection on the field fires during scroll gestures | Icon-only open on scrollable screens; see §6.3 |

---

## 14. Suggested phase checklist for a new app

```
[ ] DESIGN.md: actors, schema, nav routes, auth rules
[ ] USER_STORIES.md: all stories with ACs
[ ] Roles matrix (even if RBAC not enforced yet)
[ ] FarmDatabase with all entities and DAOs — compile green
[ ] Phase 1: all CRUD screens working — smoke test on device
[ ] Phase 2: configurable rules / presets in DB with admin UI
[ ] Phase 3: pure computation engine + JVM unit tests pass
[ ] Phase 4: engine wired into save + live preview
[ ] Phase 5: RBAC — nav guards + VM double-check
[ ] Phase 6: CSV export all tables with device_id
[ ] scripts/dev.sh with install / fresh / log / pull-exports
[ ] CLAUDE.md with build commands + architecture summary
[ ] bugs.md tracking open issues
[ ] code_summary.md (generated)
```

---

## 15. Things this app would do differently in v2

**Bugs:** Every defect worth fixing should be filed in **`docs/bugs.md`** with report, root cause (once known), fix, files, verification, and **`[ ]` / `[x]`** status — not only “architecture” items. Examples: **BUG-ARC-*** (structure, Room, millis), **BUG-FOP-*** (farm ops UX), **BUG-ORD-***, **BUG-ACQ-***, **BUG-PRD-***, etc. That file is the single tracker; this section lists **v2 conventions**, not every bug id.

**Tracker snapshot (architecture-heavy):** **`docs/bugs.md`** — **BUG-ARC-01** through **BUG-ARC-09** (e.g. §15.2 epoch millis: **BUG-ARC-09**; Room/domain migration: **BUG-ARC-02** / **BUG-ARC-04**). See **`bugs.md`** for the full list and open items.

1. **Hilt everywhere from day 1** — no manual ViewModelFactory anywhere
2. **Epoch millis everywhere** — no `LocalDateTime` with converters
3. **RBAC defined in Phase 0** — not retrofitted in Phase 5
4. **Incremental migrations planned from the start** — even if not written yet; reserve the version numbers
5. **`dev.sh fresh` is the default test command** — destructive migration means stale data causes phantom bugs; always test on a clean install
6. **Central numeric pad + date picker components** — built in Phase 1, not discovered later when three screens already do it differently
7. **Formula tests written before formula is wired** — never assume the formula matches the spec until a test proves it
8. **`is_per_kg` check in every price display** — make it a code review checklist item, not a bug to find later
