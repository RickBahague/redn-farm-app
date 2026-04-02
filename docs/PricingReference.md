# REDN Store — Utilities Suite: Design Specification

| Field | Value |
|--------|--------|
| **Product** | REDN Utilities (working name) |
| **Version** | 0.9.27-draft |
| **Status** | Specification — implementation-ready (**v2-only** JSON; **US-3a / FR-PC-36** active list — **hybrid** summary + detail + **print** full grid; **fractional tiers** **500g / 250g / 100g**; **US-1b** preset + **custom item** names — suggested, **§11.2**; **US-6 / FR-PC-14**; **US-8 / FR-PC-40**; **FR-PC-31**; **§15**; **[archi.md §6.5](./archi.md)**) |
| **Primary audience** | Implementation agents, future maintainers |

---

## 1. Purpose

Define a **collection of web utilities** for **REDN Store**, a business selling fresh farm produce and related products. This document is the single source of truth for **scope, behavior, data, and constraints** so that later work can produce a concrete architecture (tech stack, modules, APIs, storage layout) without re-deriving intent from chat history. **Architecture agents:** produce the deliverables listed in **§14** as a companion architecture / ADR set.

---

## 2. Context & stakeholders

| Stakeholder | Role |
|-------------|------|
| Store assistant | Runs **pricing simulations** (bulk cost + quantity), reviews **all-channel SRPs** at once, saves/tags **“in use now”**, opens a **dedicated active prices** page (**US-3a**), browses history |
| Management / owner | **Presets** additional-cost and markup/margin rules per channel; reviews history; sees **active simulation** overview; may run **gross sales** summaries from current active prices |
| Online, reseller, and walk-in customers | Indirect — outputs affect published or quoted prices |

**Assumption (explicit):** Utilities run in a **controlled internal context** (staff device or intranet). Public customer-facing e-commerce is **out of scope** unless specified in a future revision.

---

## 3. Product vision & scope

### 3.1 Vision

Provide **fast, repeatable, audit-friendly tools** that turn purchase costs and **management-defined policy** (presets) into **per-channel price guidance**, via **simulations** with **persistent history**, **active price** tagging per item, and management **reporting**.

### 3.2 In scope (this document)

- **App 1 — Pricing Calculator** (full functional spec below) is the **only** implemented utility in **v1**; include a **minimal shared shell** (product title, optional placeholder nav for future apps). No multi-app routing or i18n requirement beyond **English** for v1 unless business requests otherwise.
- Naming pattern for future apps: `REDN Utilities / <AppName>` with shared branding and navigation shell (exact UX deferred to architecture).

### 3.3 Out of scope (unless added later)

- Inventory management, POS integration, accounting systems.
- Full multi-user **authentication** (optional); however, **who may edit pricing presets** (management only) is in scope behaviorally — implementation may use simple shared admin PIN, deploy behind VPN, or full RBAC (see §4.2).
- **Native** mobile apps (iOS/Android); the product is **mobile-first responsive web** (see §6, §9), then enhanced for desktop.

---

## 4. Actors & user stories

### 4.1 Store assistant

- **US-1:** As an assistant, I enter only **bulk purchase price** and **quantity in kg**; when management has defined **categories**, I may select an **item category** so the correct additional-cost preset applies (§11.1.1). The tool computes a suggested **SRP** (suggested retail price) per selling unit using preset **additional costs**, **markup** and **spoilage** values provided by management. Each such run is a **simulation**. The system automatically computes and displays suggested SRPs for **all available customer channels** (online, reseller, offline) at once, using each channel's preset pricing rules. I do not set or choose these parameters. I can also choose to get a per piece pricing if I input **no of pieces**. In addition to SRP per kilogram (1kg), the simulation automatically computes and shows **SRPs for fractional package sizes**—**100g**, **250g**, and **500g**—for each customer channel, applying the same rounding and pricing rules as per kilogram. When saving a simulation, I provide a **Basket Label** (for identification of the save).

- **US-1b (suggested):** As an assistant, when management has configured a **preset list of item names** (standard products we price often), I can **select the item** from that list using a control such as a **dropdown, combobox, or searchable picker**, so the simulation’s **`itemName`** matches our standard labels (**item key** rules in §11.1.3 still apply). **I can also add a custom item:** when the product is **not** on the preset list, I **enter a custom `itemName`** (free text) — e.g. via an **“Other” / “Custom”** choice that reveals a text field, a **parallel** name field, or **type-ahead** that accepts a new string — and that custom name is saved and treated like any other **`itemName`** for **history**, **active prices** (**US-3a**), and **“in use now”**. **Default product policy (recommended when US-1b ships):** **preset pick *or* custom entry** are both available; **management** may optionally configure **preset-only** mode (no custom names) for stricter operations (**§11.2**). When **no preset list** exists or it is **empty**, I **enter `itemName` as free text only** (current v1 behavior per §11.1.1). **Management** maintains the preset list in configuration (schema per **§11.2**).

- **US-1c (suggested):** As a store assistant, I am able to enter a **batch of items for simulation** in a single workflow. This can be triggered by an **"Add Batch Purchase"** link or similar entry point. For each item in the batch, I provide: **item name**, **total amount**, **quantity in kg**, and (optionally) **number of pieces**. After saving details for an item, the form is cleared or a new entry is presented, allowing entry of the next item in the batch. 

  - **Basket Label:** All items in the current batch are grouped and tagged with the **same Basket Label**. By default, this label is auto-generated as **"Bulk Purchase - [Date and Time]"**, but the assistant may edit or override it before finalizing the batch. 
  - **Save behavior:** When I save the batch, all items entered are **saved together** as individual simulations, each tagged with the shared basket label. All items in the batch are **considered "in use now" by default** (tag: "in use now" is set automatically for all batch items at save).
  - **Batch Active Viewing:** When **US-1c** is implemented, the Active page displays batch-entered items individually and **must** support filtering or viewing by **Basket Label** (e.g. a **dropdown** among labels). Until **US-1c** ships, **US-3a** / **FR-PC-36** **optional** filter/search/sort rules apply without this requirement.
  - **Details:** All other per-item metadata, calculations (SRP per channel, package size breakdowns, etc.) follow the same pipeline as single-item entries. Editing, updating, or changing batch entries behaves identically to non-batch entries, except initial tagging and grouping.
  - **Acceptance criteria** *(conditional — applies only when US-1c is formally promoted from "suggested" to normative in the spec changelog; these criteria are **not** §14 exit criteria until that promotion occurs):*
      - Batch entry form enables entry of multiple items before a single save.
      - Shared basket label is prefilled as "Bulk Purchase - Date and Time" and is editable.
      - All batch items are marked "in use now" at save time.
      - Active Page supports filtering or viewing by basket label via dropdown.
      - All other simulation and reporting requirements apply.

- **US-2:** As an assistant, I can view at a glance the suggested prices for all channels for any simulation, and optionally focus on or highlight a specific channel for further action (e.g., when recording a sale) — **read-only** emphasis; save labeling uses **Basket Label** (**FR-PC-35**), not a channel picker.

- **US-3:** As an assistant, I can **save** a simulation with an identifiable **item/product label**, optional **Basket Label** (short tag for the save row — see **FR-PC-35**), and optional notes for retrieval and review. Saved simulations retain all computed channel prices for reference—including all available package sizes (1kg, 500g, 250g, 100g, per piece if provided)—for reference. When saving or updating a simulation, I can tag it as "**in use now**" to indicate that this is the current, active price guidance for that item, effective immediately until replaced by a newer simulation. A message must be shown with saving simulation. 

- **US-3a:** As an assistant, I can view, in a dedicated active page, a list of all simulations currently tagged as **"in use now"**. The page uses a **hybrid layout** so the list stays scannable while full prices stay clear:
    - **Default (on-screen):** **One list entry per item key** with a **compact summary row**: **item name** shown **inline** with the text **"from P<reference price>"**, where **reference price** is the **minimum per-kg SRP** among **online, reseller, and offline** (as in **P** of **US-8**/**FR-PC-40** from saved **`derived.byChannel`**). This summary row also displays: **spoilage**, **bulk quantity (kg)**, **category** (if any), **date/time** last set active, **basket label**, and **notes** (when present). To display full details for an item, interact directly with the **item name** itself—no button or separate control—showing all channels and package sizes (**per-kg**, **500g**, **250g**, **100g**, and **per piece** if available) in a clear, non-confusing expanded section beneath the summary row (such as a table or labeled grid; avoid dense, single-line breakdowns).
    - **Print (offline reference):** **Browser print** (or equivalent) must output the **full** per-channel, per-package-size price grid for **every** active item **without** relying on the user having expanded rows in the interactive UI — e.g. **`@media print`** CSS that expands detail, a **print-only** block, or equivalent so posted sheets are complete. Layout should remain **readable** (adequate contrast, alignment, optional light borders) for black-and-white posting.
    - **CSV export** remains the **complete** dataset: all columns for **per-channel** **kg + 500g + 250g + 100g + piece**, metadata, filters, and sort (**FR-PC-36**).

    - **Rationale:** **At-a-glance** summary rows; **offline** posting via **print** + **CSV** with full detail; **discovery** via optional filter (category), search (item name), and sort (item / active date).

    - **Acceptance criteria (US-3a):**
      - Each **in use now** item appears as **one** summary row in the default list; expanding (or **print**) shows **all** channel SRPs for **kg, 500g, 250g, 100g**, and **per piece** when applicable.
      - CSV contains **all** numeric columns for export (per-channel package sizes + metadata).
      - **Print** output shows the **full** price grid for every item (not summary-only); suitable for physical posting.
      - (Implementation note: This complements but does **not** replace the simulation history/browse view; it is purpose-built for showing and distributing only the current active price guidance.)

- **US-4:** As an assistant, I can **browse, search, and select** past simulations by item name, date range, **basket label**, or free-text in notes (minimum: item + date; architecture may extend search fields). When viewing historical simulations, I can see the prices computed for all customer channels at the time of calculation, including for all package sizes. I can **update** an existing simulation (adding a new **revision** to the same line) or start a **new** simulation line (e.g., after **using a past row as a template** — see §11.1.4). I can tag the active simulation ("in use now") for an item. The system clearly surfaces which simulation is currently marked as active for each item.

### 4.2 Management workflows

- **US-5:** As management, I can review historical simulations to validate pricing consistency (read-oriented; same UI as assistant for browsing detail, with **preset editing** on separate gated routes — §11.1.6). Each revision clearly shows the SRP calculations for all customer channels **as recorded at save time**. For **v1**, the UI indicates **whether that revision is the one currently marked “in use now”** for its item; **full historical timeline** of past active revisions (which revision was active when) is **deferred** unless `inUseNowChangedAt` on the active row suffices for “last change” (US-7). When viewing a lineage detail, management can see a **price history timeline** — a combined view of a **table** listing each revision's per-channel per-kg SRPs alongside a **chart** plotting those SRPs over time — to quickly identify pricing trends and validate that changes were intentional (**FR-PC-34b**).

- **US-6:** As management, I can preset the **additional costs** (default or category-based) and the **markup/margin** rules that the system uses to calculate SRPs for each channel, ensuring these parameters are standardized and not alterable by assistants. I expect the following presets:
    **hauling weight**: 700kg  
    **driver Fee**: 2000  
    **fuel**: 4000  
    **toll**: 1000  
    **handling**: 200  
    **additional costs**: (**driver fee** + **fuel** + **toll** + **handling**) / **hauling weight**  
    channel markup: **online markup**: .35, **offline markup**: .30, **reseller markup**: .25  
    **spoilage**: 25% of weight of item  
    **Categories**: Vegetables, Fruits, Other Dry Goods  
    **SRP Formula Per Channel**: ((**bulk purchase price** / (**quantity with unit** - (**quantity with unit** x **spoilage**))) x (1 + **channel markup**) + (**required additional costs**))  
    **All calculated SRPs must be rounded up to the nearest peso** (e.g., 153.4 → 154.00, 165.5 → 166.00).
    **SRP per Piece**: (**SRP Formula Per Channel** / Store Assistant inputted **no of pieces**) and then **rounded up to the nearest peso** (same rounding rule).


- **US-7:** As management, I can view an overview of which simulation is currently tagged as "in use now" for each item, and trace when changes to active simulations were made. Where assistants have recorded **`actualSpoilageKg`** on past revisions for an item, I can see — per item — the **preset spoilage rate** used at calculation time vs the **actual spoilage rate** derived from the recorded kg (`actualSpoilageKg / inputs.bulkQuantityKg`), so I can identify items where the preset consistently over- or under-estimates real spoilage and adjust accordingly.

- **US-8:** As management, I can generate a report of the expected **gross sales** based on simulations currently marked as **in use now**. For each item, the report uses the **lowest SRP across all channels** (**P**, PHP per kg, from the active revision) and the **bulk quantity (kg)** and **spoilage rate** recorded on that revision. **Gross sales** (revenue-style expectation per row) **must** be **P × (kg of item − kg of item × spoilage)** = **P × sellable kg**, i.e. **P × Q × (1 − s)** with **Q** = `inputs.bulkQuantityKg` and **s** = `presetSnapshot.spoilageRate` (**FR-PC-10**, **FR-PC-40**). **v1** uses stored **`derived.sellableQuantityKg`** for **Q × (1 − s)** so the figure matches the saved simulation. Optional **estimated sell-through** or **past volume overlays** (manual or import) are **deferred** unless added by ADR (**§11.2**).

### 4.3 Canonical pricing model (US-1 / US-6) — **normative**

**US-1** and **US-6** are fully reflected in **§5.2** (**FR-PC-10–14**), **§7.2–7.3**, **§8.1**, **§11.1.1–11.1.2**, **§11.1.8**, and **§11.1.9**. Implementations **must** use that pipeline.

| Topic | Rule |
|--------|------|
| Assistant quantity | **Bulk quantity** is **kilograms** for the primary cost line (**FR-PC-02**). Optional **`pieceCount`** (positive integer) enables **SRP per piece** = channel SRP ÷ `pieceCount` when set. |
| Sellable weight | **Spoilage** is a preset **fraction of purchased weight** \(s \in [0, 1)\). **Sellable quantity (kg)** \(Q_{\text{sell}} = Q \times (1 - s)\). Require \(Q_{\text{sell}} > 0\) (FR-PC-12). |
| Unit cost | **Cost per sellable kg** \(C = \text{bulkCost} / Q_{\text{sell}}\) — **not** \((\text{bulk} + \text{lump sums}) / Q\) (that pre-0.9 formulation is **superseded**). |
| Per-kg add-on | **Required additional cost per kg** \(A\) is management-defined (**§7.3**), e.g. \((\sum \text{fee amounts}) / \text{haulingWeight}\) when using the **hauling** preset shape. |
| Channel SRP | Per channel: apply **markup XOR margin** to \(C\) (**§11.1.2**), then **add** \(A\) (PHP per kg), then optional **channel fees** (**§7.2**, **§11.1.9**), then **rounding** (**FR-PC-14**). **US-6:** default REDN policy is **round up** to the nearest whole PHP per kg and for **SRP per piece** (see **`ceil_whole_peso`** in **§11.1.8**). |
| Management defaults | **Illustrative REDN constants** (hauling **700 kg**, driver/fuel/toll/handling fees, **25%** spoilage default, markups **35% / 30% / 25%** online / offline / reseller, categories **Vegetables, Fruits, Other Dry Goods**) appear as **§11.1.8** example values; production files may differ if management edits config. |

**Superseded material:** Any earlier draft text that described **lump-sum additional costs rolled into the numerator** of \((\text{bulk} + \sum\text{add-ons}) / Q\) before markup is **void** for v1 pricing.

**Persistence (0.9.1):** New implementations use **only** **`schemaVersion: 2`** files for presets and revisions — **rebuild** `data/` as needed; **[archi.md §6.5](./archi.md)** (**no** legacy loaders).

---

## 5. App 1 — Pricing Calculator: functional requirements

Requirements align with **§4 Actors & user stories**. A **simulation** is a computed pricing run (bulk inputs + management presets → SRPs for every channel); it becomes **persistent** when saved.

### 5.1 Inputs (assistant-facing)

| ID | Requirement |
|----|----------------|
| FR-PC-01 | System accepts **bulk purchase total cost** (currency assumed single; default **PHP** unless configured). |
| FR-PC-02 | System accepts **bulk quantity in kilograms** \(Q > 0\) for the primary purchase line. **Optional `pieceCount`:** positive integer when the assistant needs **SRP per piece** (US-1); if omitted or empty, per-piece outputs are not shown. **Extension** to other purchase units (lb, crate) is **deferred** unless added by ADR. |
| FR-PC-03 | **Pricing parameters** (spoilage rate, **required additional cost per kg** \(A\), and any category overrides) come from **management-defined presets** (§7.3, §11.1.1). **Preset resolution (v1):** Assistant picks optional **item category** from the management-maintained list; if none selected or list empty, use **store defaults** for spoilage and \(A\). Assistants **do not** edit preset math; calculator shows resolved values read-only. |
| FR-PC-04 | For every simulation, the system computes SRPs for **all** customer channels **concurrently**: **online**, **reseller**, **offline** (labels may be localized; semantics fixed). Assistants do **not** pick a channel to “run the math” for. The UI may **highlight** a channel for readability only (**US-2**); **save-time** identification uses **Basket Label** (**FR-PC-35**), not a channel selector. |
| FR-PC-05 | **Markup / margin and rounding** per channel are **management configuration only** (§7.2, §11.1.2). Assistants cannot change these parameters in the calculator. |
| FR-PC-06 | Each unsaved run is a **simulation**; the UI makes clear that results depend on current management presets. |
| FR-PC-07 | Assistant-facing **item category** control (optional dropdown or picker): options are **exactly** the category keys under **`categories`** in management config (**§11.1.8**); changing category **recomputes** the simulation. If no categories exist, hide the control and always use the store **`default`** preset scope (**§7.3**). |

### 5.2 Computation (behavioral)

| ID | Requirement |
|----|----------------|
| FR-PC-10 | Let **bulkCost** \(B > 0\), **bulk quantity** \(Q > 0\) (kg), resolved **spoilage rate** \(s \in [0, 1)\) and **required additional cost per kg** \(A \ge 0\) from §7.3 / §11.1.1. **Sellable quantity (kg):** \(Q_{\text{sell}} = Q \times (1 - s)\). **Cost per sellable kg:** \(C = B / Q_{\text{sell}}\). **Validation:** reject \(Q_{\text{sell}} = 0\) (FR-PC-12). \(A\) is expressed in **currency per kg** and is applied **after** markup/margin on \(C\) (FR-PC-14), **not** added to \(B\) before division. |
| FR-PC-11 | For **each** channel, final **SRP** follows **FR-PC-14**. The UI shows **transparent** intermediates **per channel:** \(Q\), \(s\), \(Q_{\text{sell}}\), \(C\), markup *or* margin step, \(A\), optional channel fees, rounding; optional **SRP per piece** when `pieceCount` is set (US-1, US-6). **At-a-glance** view of all channel SRPs (US-2). |
| FR-PC-12 | Calculator prevents division by zero and validates positive numeric inputs where applicable; shows clear validation messages. |
| FR-PC-13 | **Persistence of preset snapshot:** Each **saved** simulation stores enough data to reproduce or explain results later (embed preset values used, or preset version IDs + resolved numbers) so history reflects “what was true at save time” (US-5). |
| FR-PC-14 | **Pricing pipeline (v1, per channel):** Let \(C\) be **cost per sellable kg** from **FR-PC-10**, and \(A\) **required additional cost per kg** from the resolved preset. **(1)** Apply **exactly one** of **markup** or **margin** from that channel’s config (§11.1.2) to \(C\) → **`priceAfterCore`** (pre-fee price per kg, before \(A\)). **(2)** Compute **`priceBeforeFees`** = **`priceAfterCore` + \(A\)**. **(3)** Apply optional **channel-attributable fees** from §7.2 to **`priceBeforeFees`** per **§11.1.9** (fixed or %; architecture locks base and order). **(4)** Apply **rounding rule** for that channel → **SRP** (currency per kg). **US-6 / REDN default:** **`ceil_whole_peso`** — **round up** to the nearest whole PHP (e.g. 153.4 → 154, 165.5 → 166). Other configured rules (e.g. **`nearest_0.25`**, **`whole_peso`**) remain valid where management sets them (**§7.2**). **(5)** If **`pieceCount`** \(n > 0\) is present, derive stored **`srpPerPiece`** by **rounding up** **SRP \(/ n\)** to the **nearest whole PHP** (**same rule as US-6**) — **`ceil_whole_peso`** / **[archi.md §6.4](./archi.md)** — **independent** of optional fractional channel rules for per-kg SRP. Same pipeline for online, reseller, offline. |
| FR-PC-15 | **Preset change while unsaved:** If management updates presets while the user has an **unsaved** simulation open, the app **recomputes** displayed results from **current** presets (on debounced config refresh or next explicit **Calculate** — architecture chooses) and shows a **short, non-blocking notice** (e.g., “Pricing settings were updated; figures refreshed”). **Saved revisions are immutable** and never auto-updated. |
| FR-PC-16 | **Lineage vs revision (v1):** Behavior matches §11.1.4. |
| FR-PC-17 | **Gross margin and markup display (management only):** When the user is in a **management session** (authenticated via the management PIN — §11.1.6), the per-channel breakdown (calculator and history detail) additionally shows two derived profitability metrics per channel — **not** visible to assistants: **(a) Gross margin %** = \((\text{SRP} - T) / \text{SRP} \times 100\); **(b) Gross markup %** = \((\text{SRP} - T) / T \times 100\); where **total cost per sellable kg** \(T = C + A + \text{channelFeeTotal}\), with \(C\) = `costPerSellableKg` (bulk cost ÷ sellable kg, already incorporating spoilage effect), \(A\) = `additionalCostPerKg` (hauling-derived per-kg cost from preset), and `channelFeeTotal` = sum of all channel fee amounts applied for that channel per **FR-PC-14**. All inputs are already stored on the revision (`derived` + `presetSnapshot`); no new persistence required. Displayed as read-only labeled fields (e.g., "Gross margin: 34.2% · Markup: 52.0%") beneath the SRP in the per-channel breakdown. |

> **Note for architects:** Isolate “pricing engine,” “preset resolution,” and “config load” in testable modules. VAT and tax handling remain **open** (§11.2).

> **Fractional package SRPs (§4.1 / US-1):** After the rounded **per-kg `srp`** for each channel, derive **`srpPer500g` = SRP × 0.5**, **`srpPer250g` = SRP × 0.25**, **`srpPer100g` = SRP × 0.1**, each passed through that channel’s **`roundingRule`**, and persist on **`derived.byChannel`** (calculator, history, **`/active`**, **CSV** — **FR-PC-36**). **US-8** gross sales still use **per-kg `srp`** only. **No** automatic migration from **`srpPer200g`** or other superseded keys — **rebuild** JSON per **[archi.md §6.5](./archi.md)**.

### 5.3 Persistence (simulations)

| ID | Requirement |
|----|----------------|
| FR-PC-20 | Persist **saved simulations** to **file-based storage** (JSON on server or local app data). No relational DB required in v1. |
| FR-PC-21 | Assistants may **update** an existing saved simulation (per US-4). **All updates must be fully versioned:** prior content remains recoverable and auditable. Acceptable patterns include (a) append-only **revision records** linked by a stable **simulation lineage id**, or (b) an embedded **revisions** array ordered by time. Each revision stores a **full snapshot** of inputs, preset snapshot, and derived outputs (not only a diff). Required metadata per revision: `createdAt` (or `savedAt`), `updatedAt`, and a **revision id** (monotonic or UUID). **`updatedBy`** is required when the deployment provides user identity (**NFR-06**); otherwise store a nullable field or a fixed label such as `staff`. A human-readable **change summary** or structured diff is encouraged but not mandatory for v1. |
| FR-PC-22 | Storage must support **many** simulations and **all revisions** without manual file editing. List and search views must define whether they show **latest revision only**, **all revisions**, or **collapsed lineage** (architecture documents default UX). |
| FR-PC-23 | Data must be **human-readable** and **easy to back up** (plain JSON or NDJSON, including version history). |
| FR-PC-24 | **“In use now”** is an **exclusive flag per item**. **Item key (v1):** Unicode **NFC** normalization, **trim** whitespace, **case-insensitive** comparison on `itemName` (§11.1.3). Optional `itemId` in schema reserved for future catalog integration. At any time, **at most one revision** may be `inUseNow: true` for a given item key. Marking a revision **in use** clears `inUseNow` on any other revision for that item. The UI shows **which revision** is active (US-3, US-3a, US-4, US-7). |
| FR-PC-25 | **Concurrency (v1):** Target **low concurrency** (one store or few simultaneous writers). File-based stores may use **documented** last-write-wins, simple **file lock**, or atomic rename; architects state assumptions. High concurrency or multi-site sync is **out of scope** for v1. |
| FR-PC-26 | **Actual spoilage recording:** After a batch sells through, an assistant may optionally record **`actualSpoilageKg`** (non-negative number, ≤ `inputs.bulkQuantityKg`) on an existing saved revision. This is a **lightweight patch** to the revision — it does **not** create a new revision or alter any stored pricing fields (`derived`, `presetSnapshot`, SRPs). The patch updates only `actualSpoilageKg` and `updatedAt`. **REST (illustrative):** `PATCH /api/simulations/:revisionId` with `{ "actualSpoilageKg": <number> }`. The field is **optional** on the revision schema — absence means the assistant did not record actual spoilage for that batch. Assistants may update or correct the value after the fact. The field is visible in the history detail view (**FR-PC-32**) alongside the preset `spoilageRate` used at calculation time so the comparison is clear. |

### 5.4 History, search & active state

| ID | Requirement |
|----|----------------|
| FR-PC-30 | **History** lists saved simulations **newest first** by default (typically **one row per lineage** showing **latest revision**, or a flat revision feed — architecture documents). **Pagination:** default page size **25** lineages per page; `page` query param (1-based integer, default `1`); response includes `total` (total matching lineage count) and `pageCount` so the UI can render page controls. Architecture may substitute cursor-based pagination — document the chosen scheme. History is expected to grow over time; pagination is required, not optional. **Same item key, multiple lineages:** Forks (§11.1.4) may produce **several lineages** per normalized `itemName`; each row must **disambiguate** (e.g., show **`lineageId` short suffix**, **last updated** datetime, and/or **latest revision** note) so assistants pick the intended line. |
| FR-PC-31 | **Search / filter** by at least: **item name**, **date range**; optional: **basket label** text, **”in use now”**, free-text in notes. Filters apply to the **latest revision** shown per **lineage** in list views (**FR-PC-30**). **v1 implementation:** linear scan over revision records is acceptable while total revisions **≤ 5,000**; beyond that, add an index or compaction strategy (architecture documents). **REST (illustrative):** `GET /api/simulations?item=&from=&to=` (`YYYY-MM-DD`, UTC day bounds), `basket=`, `notes=`, `inUse=1`; **`format=csv`** or **`format=json&export=1`** for optional export (**FR-PC-41**). **Supported `sort` values for history:** `updated_desc` (**default** — newest first), `updated_asc`, `item_asc` (alphabetical A→Z), `item_desc` (alphabetical Z→A). Invalid `sort` values fall back to `updated_desc`. |
| FR-PC-32 | Detail view shows **all inputs** (including **`bulkQuantityKg`**, **`pieceCount`** when set, **`basketLabel`** when set), **preset snapshot** (spoilage, \(A\), hauling lines, channel rules), **per-channel breakdown** (**FR-PC-14** intermediates), timestamps, **in use now** flag, and optional assistant **notes**. When viewed in a **management session**, the detail view also shows per-channel **gross margin %** and **gross markup %** per **FR-PC-17**. |
| FR-PC-33 | User can **select** a historical simulation (typically **latest revision**, or any revision — architecture defines) to **load into** the calculator as the basis for a new run or new save. |
| FR-PC-34 | **Management overview (US-7):** A view listing **which simulation is in use now** per item and **when** active status last changed (timestamps sufficient; full audit trail optional). Implementations may satisfy this with the same **active** data surface as **FR-PC-36** or a lighter summary. |
| FR-PC-34b | **Price history timeline (US-5 — management):** Within a lineage detail view, display a **combined table + chart** of all revisions for that item showing how per-channel per-kg SRPs changed over time. **Table columns:** revision index, `createdAt` date, `basketLabel` (if set), per-channel SRP (online / reseller / offline), and whether that revision was `inUseNow`. **Chart:** line chart with `createdAt` on the x-axis and SRP (PHP/kg) on the y-axis, one line per channel (online, reseller, offline). Both table and chart are derived entirely from already-persisted revision data — no new storage required. Designed for a **handful of revisions** per lineage (typical: 5–15); no pagination required within the timeline view. Accessible from the lineage detail screen; not shown in the flat history list. **Not** exposed on the assistant-facing history view — management only. |
| FR-PC-35 | On **save**, assistant may set an optional **Basket Label** — a short, assistant-chosen string to identify the save (e.g. basket, batch, or display tag). **All channel SRPs** are always stored; **no** “focus channel” or channel picker is used for save labeling. Empty basket label is allowed. |
| FR-PC-36 | **Assistant active price list (US-3a):** A **dedicated** assistant page listing **every** item with an **`inUseNow`** revision (**one list entry per item key**). **On-screen default:** **compact summary** per item — **item name**; **min per-kg SRP** across channels (**P**-style, **US-8**); **spoilage**, **bulk quantity**, **category**, **when** active, **basket label**, **notes**; **expand/collapse** (or equivalent) reveals **full** breakdown: **all channels** with **per kg, 500g, 250g, 100g**, **per piece** when stored — in a **clear** layout (table or grid), not only a single unreadable run-on line. **Print:** **full** breakdown for **all** items without manual expand (**§4.1** US-3a). **CSV:** **all** columns (per-channel package sizes + metadata). Optional **filter** / **search** / **sort**. **REST (illustrative):** **`GET /api/overview/active`** with **`item`**, **`category`** (**`__none__`**), **`sort`**; **`format=csv`**. **Supported `sort` values for active page:** `item_asc` (**default** — alphabetical A→Z), `item_desc`, `active_desc` (most recently activated first), `active_asc`, `srp_asc` (min SRP low→high), `srp_desc` (min SRP high→low). Invalid `sort` values fall back to `item_asc`. Does **not** replace history browse (**FR-PC-30**). |
| FR-PC-38 | **Active price change notification (NFR-13):** When a revision is marked **"in use now"** (via `POST /api/simulations/:revisionId/in-use`), the server fires a non-blocking webhook POST to `NOTIFICATION_WEBHOOK_URL` (if configured) with the following payload: `{ "event": "active_price_changed", "itemName": "<string>", "activatedAt": "<ISO-8601>", "activatedBy": "<string or null>", "prices": { "online": <srp>, "reseller": <srp>, "offline": <srp> }, "minSrp": <number>, "category": "<string or null>", "basketLabel": "<string or null>" }`. The payload uses already-computed stored SRP values — no recompute at notification time. Notification fires for **every** "in use now" change regardless of item or category. |
| FR-PC-37 | **Price freshness indicator:** When a category scope in `pricing-presets.json` includes an optional **`freshnessWarningDays`** (positive integer), any active simulation for an item in that category whose `inUseNowChangedAt` is older than that many calendar days is considered **stale**. The active page (**US-3a**) displays a **visual staleness indicator** on the compact summary row — e.g., an age badge ("7d ago") in a muted or warning color. The indicator is **informational only**: it does not block viewing, printing, or CSV export, and does not prevent the simulation from remaining "in use now." If `freshnessWarningDays` is omitted for a category or the store default, no staleness indicator is shown for items in that scope. **`freshnessWarningDays`** is not inherited from `default` to categories — each scope opts in explicitly. **Illustrative values:** `vegetables: 7`, `fruits: 7`, `other_dry_goods: 30`. |

### 5.5 Management reporting

| ID | Requirement |
|----|----------------|
| FR-PC-40 | **Gross sales expectation report (US-8):** “Gross sales” here means **revenue-style expectation** from **active** price guidance, not accounting gross profit. For each **item key** with exactly one revision **`inUseNow: true`**, read from **that revision** only (no live preset recompute). Let **P** = **min(SRP_online, SRP_reseller, SRP_offline)** in **PHP**, from stored **`derived.byChannel`**, **after rounding** as saved. Let **Q** = **`inputs.bulkQuantityKg`** and **s** = **`presetSnapshot.spoilageRate`**. **Sellable kg** \(Q_{\text{sell}} = Q(1-s)\) — **v1:** use stored **`derived.sellableQuantityKg`** (must equal \(Q(1-s)\) from save-time math; **FR-PC-10**). **Per row:** **`line_total` = `round_currency`\(P \times Q_{\text{sell}}\)** (**US-8**). **Report total** = sum of `line_total`. Optional **grand total** row. Output: on-screen table + optional **CSV** with columns at minimum: item key, item name, **P**, **bulkQuantityKg**, **spoilageRate** (and/or **sellableKg**), **line_total**. **REST:** **`GET /api/reports/gross-sales`**; **`format=csv`** or **`export=csv`** for download. **Time scope:** the report is a **snapshot of all `inUseNow: true` revisions at request time** — there is no date range parameter and no historical report mode in v1. The response (JSON and CSV) includes a top-level **`generatedAt`** field (ISO-8601 UTC) so printed or exported copies are timestamped. |
| FR-PC-41 | *(Optional)* Export arbitrary selected history rows as **CSV** / **JSON**. |

### 5.6 Configuration operations (management)

| ID | Requirement |
|----|----------------|
| FR-PC-50 | Only **management** (or equivalent privileged mode) may create/update **additional cost presets** and **per-channel markup/margin/rounding** (US-6). Assistants see effective values as read-only context in the calculator. **v1 delivery (pick one or combine; document in architecture):** (A) **In-app management UI** on gated routes that reads/writes the same config the app loads, or (B) **Edit JSON on server** (e.g., `pricing-presets.json`) + **reload** / process restart, with optional read-only preview in app. Both satisfy the spec if changes take effect predictably and **FR-PC-15** semantics hold for open sessions. |
| FR-PC-51 | Presets may support **default** and **category-based** (or similar) applicability; exact taxonomy is an architecture/business detail documented in config schema. |
| FR-PC-54 | **Preset version history and restore:** Every time management saves updated presets (**FR-PC-50**), the system appends a record to an **append-only preset changelog** (`data/preset-history.ndjson` or equivalent) containing: `savedAt` (ISO-8601 UTC), `savedBy` (identity string or `"owner"` if no user identity), and the **full preset snapshot** saved. The changelog is never truncated or overwritten — it is the authoritative audit trail for preset changes. **Management UI** exposes a preset history list (newest first) showing `savedAt`, `savedBy`, and a summary of key values (e.g., default spoilage rate, per-channel markup). Each entry has a **Restore** action: loading that snapshot into the preset editor as a draft — management reviews and confirms before it is saved as a **new entry** in the changelog (restore never deletes or overwrites history; it creates a new "restored to…" record). **REST (illustrative):** `GET /api/management/presets/history` returns the log entries (newest first); `POST /api/management/presets` remains the single write path for both new edits and restores. **`presetRef`** on saved simulation revisions (§8.1) is the `savedAt` timestamp of the matching changelog entry, enabling forward lookup ("which revisions used this preset version?"). |
| FR-PC-53 | **Preset impact preview ("What If"):** Before management **saves** updated presets, the system computes and displays the effect on all currently active simulations (`inUseNow: true`). For each affected item, show: **item name**, **channel**, **current SRP** (stored), **new SRP** (recomputed from draft presets), and **delta** (PHP). Items with no SRP change may be summarized as a count ("N items unchanged") rather than listed individually. The preview is triggered automatically when the management preset form is complete and valid — management must **confirm** before the save is committed. **REST (illustrative):** `POST /api/management/presets/preview` — accepts the same body as `POST /api/management/presets`; returns `{ affectedCount, unchanged Count, items: [{ itemName, channel, currentSrp, newSrp, delta }] }` without writing to disk. The actual save remains a separate `POST /api/management/presets` call. **Scope:** applies to changes in hauling fees, `additionalCostPerKg`, markup/margin %, channel fees, spoilage rate, and any combination thereof. |

---

## 6. Non-functional requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-01 | Usability | UI is **modern and minimal**: clear typography, high contrast, large tap targets for field use in a store. |
| NFR-02 | Performance | Calculator feedback **< 200 ms** perceived for compute; history list **< 2 s** for first page on **mid-tier phone** and desktop. |
| NFR-03 | Reliability | No silent failures on save; user sees confirmation or error. |
| NFR-04 | Maintainability | Pricing **presets** and defaults live in **one configuration place** (file or env), not scattered literals; assistant-visible values are derived from that source. |
| NFR-05 | Privacy | Data describes **business pricing**, not end-customer PII; still assume storage is **not public** without access control if exposed to network. |
| NFR-06 | Authorization (lightweight) | Where deployment exposes preset editing, restrict it to **management** (US-6); calculator remains usable for assistants without preset edit rights. **v1 pattern:** separate **gated** route(s) for preset editing (e.g., `/settings`, `/management/presets`) using **env-based admin PIN**, HTTP Basic on that path only, or VPN — see §11.1.6. **Access model:** store assistants access the app on-site (store Wi-Fi / LAN); management accesses remotely (mobile data or home network). Because management routes are accessed over the **public internet**, the app **must** be served over **HTTPS** (NFR-09) and the architecture document **must** address remote management access explicitly — PIN over HTTPS is a minimum; a VPN or a second factor is **strongly recommended** for management routes exposed to the internet. |
| NFR-07 | Responsive layout | **Mobile-first:** layout, navigation, and touch targets are designed for **narrow viewports** (typical phone) as the **default**. **Desktop and tablet** are **progressive enhancement**: use available width for multi-column summaries, side-by-side calculator + detail, and denser tables **without** removing or degrading the mobile experience. No separate “desktop-only” critical path. |
| NFR-08 | Connectivity | **v1:** App is **online-required** for save/load and preset fetch; **offline mode** and **PWA installability** are **not** required. Optional service-worker caching may be added later without changing core semantics. |
| NFR-09 | Security baseline | **Production:** serve over **HTTPS**; do not expose management/preset **write** paths without the same **gate** as privileged UI. **PIN / shared secrets:** validation MUST be **server-side** (or **reverse-proxy** auth), **not** a copy of the secret embedded in a client bundle for comparison in the browser. **HTTP Basic** and **proxy auth** satisfy this when credentials are verified off-device. No end-customer PII in scope — avoid **open CORS** to arbitrary origins; keep **secrets** in env or secret store, not in shipped client assets. Exact hardening in architecture doc. |
| NFR-10 | Test viewport | **Reference minimum width** for mobile QA: **360px** CSS pixels (e.g., 360×800); secondary check at **390px** width. Core calculator and save flows must not require horizontal scroll at 360px except optional wide tables **behind** desktop breakpoint. |
| NFR-11 | Accessibility | **v1 target:** align with **WCAG 2.1 Level AA** where practical for internal use (focus order, labels, contrast, touch targets per **NFR-01**). **Full audit** and remediation backlog may be deferred; architecture doc states **scope** and any **known gaps**. |
| NFR-12 | Localization | **v1 UI copy:** **English** primary. **Tagalog** (or other) UI strings are **out of scope** for v1 unless the business requests; **FR-PC-04** channel *labels* may stay English internally with localized display labels later. Number/date formatting remains **Philippines-oriented** (§7.1). |
| NFR-13 | Notifications | When `NOTIFICATION_WEBHOOK_URL` is set in the server environment, the server fires a **non-blocking outbound HTTP POST** to that URL on every "in use now" change (**FR-PC-38**). The webhook URL is **server-side only** — never exposed to the client bundle (**NFR-09**). Delivery failure must not block or roll back the in-use operation; log the error server-side only. Compatible with **Slack incoming webhooks**, **Viber bots**, **Telegram bots**, **Messenger webhooks**, or any HTTP endpoint that accepts a JSON POST. If `NOTIFICATION_WEBHOOK_URL` is unset, the feature is silently disabled — no error, no UI change. |

---

## 7. Configuration & policy

### 7.1 Currency & locale

- Default currency: **PHP**.
- Number formatting and date display: locale-aware where possible; **Philippines** context is primary.
- **UI language:** see **NFR-12** (English v1; Tagalog UI deferred unless requested).

### 7.2 Channel rules — management only (architecture must parameterize)

For each channel (`online`, `reseller`, `offline`), configuration must include at minimum:

- **Exactly one** of **markup** or **margin** (not both) applied to **\(C\)** (**cost per sellable kg** from **FR-PC-10**) — see **§11.1.2** for canonical formulas and validation.
- **Rounding rule** (e.g. **`ceil_whole_peso`** per **US-6**, round to nearest 0.25 PHP, or nearest whole peso — see **§11.1.8**).

Optionally, per channel, configurable **channel-attributable fees** (fixed amount or percentage) applied **after** **`priceBeforeFees`** = **`priceAfterCore` + \(A\)** (**FR-PC-14**) and **before** final rounding — e.g. payment processing, delivery surcharge. **`\(A\)`** is the **store/category required additional cost per kg** from §7.3 (hauling-derived or override); do **not** double-count the same economic cost in both §7.3 and channel fees.

**Editors:** management (or privileged deploy-time access). **Consumers:** pricing engine for every simulation. Initial values may be placeholders; **business owner** adjusts via config without code change.

### 7.3 Presets: spoilage, hauling-based per-kg cost, and categories

Management defines **store defaults** and optionally **named categories** (e.g. Vegetables, Fruits, Other Dry Goods). The **category name list** drives the assistant’s **item category** picker (FR-PC-07). If **no** categories exist, use **defaults only** and hide the picker.

**Spoilage (v1):** Each scope (**store default** and each **category** override if present) provides **`spoilageRate`** ∈ [0, 1) — fraction of **purchased kg** that does not reach sellable inventory. **Sellable kg** \(Q_{\text{sell}} = Q \times (1 - \text{spoilageRate})\) (**FR-PC-10**).

**Required additional cost per kg (\(A\)):** Each scope provides **`additionalCostPerKg`** (currency per kg), typically **derived** from a **hauling model**:

- **`haulingWeightKg`** \(H > 0\) — denominator weight (e.g. **700** kg truck load).
- **`haulingFees`** — array of **{ label, amount }** line items in currency (e.g. driver, fuel, toll, handling).
- **Formula:** \(A = (\sum \text{amounts}) / H\). Architecture **may** allow a **literal `additionalCostPerKg`** override instead of or after derivation — document in schema.

**Resolution:** Assistant’s category (or default) yields resolved **`spoilageRate`** and **`additionalCostPerKg`** used in **FR-PC-10** and **FR-PC-14**. Snapshots (**FR-PC-13**, **§8.1**) must record **labels and numbers** used so history is auditable.

**Illustrative JSON:** **§11.1.8**.

---

## 8. Data model (logical)

Agents should map this to concrete JSON schemas and file layout.

### 8.1 Saved simulation — revision record (illustrative)

Each persisted row is one **revision**. A **lineage** groups revisions of the “same” simulation (same item + logical save chain).

```json
{
  "schemaVersion": 2,
  "lineageId": "uuid-stable-for-this-simulation-chain",
  "revisionId": "uuid-or-monotonic-per-revision",
  "revisionIndex": 1,
  "itemName": "string",
  "itemId": "optional-stable-item-key",
  "itemCategory": "vegetables|fruits|other_dry_goods|null",
  "createdAt": "ISO-8601-datetime",
  "updatedAt": "ISO-8601-datetime",
  "updatedBy": "string-or-null",
  "inUseNow": false,
  "inUseNowChangedAt": "ISO-8601-datetime-or-null",
  "basketLabel": "optional short string for save identification",
  "inputs": {
    "bulkCost": 5000,
    "bulkQuantityKg": 100,
    "pieceCount": null
  },
  "presetSnapshot": {
    "spoilageRate": 0.25,
    "additionalCostPerKg": 10.285714285714286,
    "haulingSnapshot": {
      "haulingWeightKg": 700,
      "feesResolved": [
        { "label": "driver", "amount": 2000 },
        { "label": "fuel", "amount": 4000 },
        { "label": "toll", "amount": 1000 },
        { "label": "handling", "amount": 200 }
      ]
    },
    "presetRef": "2026-03-31T10:30:00.000Z",
    "channels": {
      "online": {
        "markupPercent": 35,
        "roundingRule": "ceil_whole_peso",
        "channelFees": []
      },
      "reseller": {
        "markupPercent": 25,
        "roundingRule": "ceil_whole_peso",
        "channelFees": []
      },
      "offline": {
        "markupPercent": 30,
        "roundingRule": "ceil_whole_peso",
        "channelFees": []
      }
    }
  },
  "derived": {
    "sellableQuantityKg": 75,
    "costPerSellableKg": 66.66666666666667,
    "additionalCostPerKg": 10.285714285714286,
    "byChannel": {
      "online": {
        "srp": 101,
        "srpPerPiece": null,
        "breakdown": {
          "priceAfterCore": 90,
          "priceBeforeFees": 100.28571428571429,
          "feesApplied": [],
          "beforeRounding": 100.28571428571429
        }
      },
      "reseller": {
        "srp": 94,
        "srpPerPiece": null,
        "breakdown": {
          "priceAfterCore": 83.33333333333333,
          "priceBeforeFees": 93.61904761904762,
          "feesApplied": [],
          "beforeRounding": 93.61904761904762
        }
      },
      "offline": {
        "srp": 97,
        "srpPerPiece": null,
        "breakdown": {
          "priceAfterCore": 86.66666666666667,
          "priceBeforeFees": 96.95238095238095,
          "feesApplied": [],
          "beforeRounding": 96.95238095238095
        }
      }
    }
  },
  "notes": "optional free text",
  "actualSpoilageKg": null
}
```

**Field length constraints:** Validation must reject values exceeding these limits (FR-PC-12 principle — clear validation messages):

| Field | Max length |
|-------|-----------|
| `itemName` | 120 characters |
| `basketLabel` | 80 characters |
| `notes` | 1,000 characters |
| `updatedBy` | 60 characters |

Empty string is allowed for `basketLabel` and `notes`; `itemName` must be non-empty after normalization (§11.1.3).

**Constraints:** `revisionId` unique. Timestamps in UTC or explicit offset. **Item key** for `inUseNow` exclusivity: **§11.1.3** (v1: normalized `itemName`). **At most one revision** in the store may have `inUseNow: true` per item key (FR-PC-24). **`presetSnapshot.channels`:** each channel records **either** `markupPercent` **or** `marginPercent`, plus `roundingRule` and resolved `channelFees` (FR-PC-13, US-5). **`schemaVersion`:** implementations support **only `2`** for new persistence (**FR-PC-10/14** shape above). **Greenfield / rebuild:** reject or omit pre-release JSON that does not match this shape — **no** requirement to read older draft schemas (**[archi.md §6.5](./archi.md)**). **`derived.byChannel.breakdown`** must support audit of **FR-PC-14** (including **`priceAfterCore`**, **`priceBeforeFees`**, fees, **beforeRounding**). Each **`derived.byChannel`** channel row (**`online`**, **`reseller`**, **`offline`**) **must** persist **`srp`**, **`srpPer500g`**, **`srpPer250g`**, **`srpPer100g`**, **`srpPerPiece`** (nullable), and **`breakdown`**, per **§5.2** fractional note and **US-1** / **US-3** — the JSON example above omits package-tier fields for brevity only. **Gross sales report (FR-PC-40)** uses stored **`srp`** (per kg) × **`derived.sellableQuantityKg`** from **in use** revisions (**US-8**). When **`pieceCount`** is set, store **`srpPerPiece`** per channel (**archi §6.4**). **`presetRef`** on each revision is the `savedAt` timestamp (ISO-8601 UTC) of the matching entry in the preset changelog (**FR-PC-54**); this enables forward lookup from a changelog entry to all revisions that used that preset version.

### 8.2 Storage layout (non-prescriptive)

Examples acceptable to architecture:

- Directory `data/simulations/<lineageId>/revisions/<revisionId>.json` (clear versioning; easy backup).
- Single `history.json` array of all revisions (simple; concurrent write risk — document if single-user only).
- Single **NDJSON** append-only log of revisions plus optional index file for “latest per lineage” and “in use per item”.

**Preset changelog:** `data/preset-history.ndjson` — one JSON line per preset save, format: `{ “savedAt”: “<ISO-8601>”, “savedBy”: “<string>”, “snapshot”: { <full pricing-presets content> } }`. Append-only; never truncated. `pricing-presets.json` remains the live config; the changelog is the history. Architecture documents how the two stay in sync on save (**FR-PC-54**).

---

## 9. UX / UI direction

### 9.1 Principles — mobile-first, then desktop

All apps in this suite follow **mobile-first** delivery (**NFR-07**):

- **Default (phone):** single-column flow, full-width primary actions, **bottom sheets** or full-screen steps for history and secondary tasks, **stacked** channel cards or vertical channel list with expand/collapse for breakdowns. Touch-friendly spacing; avoid relying on hover.
- **Enhanced (tablet / desktop):** same routes and features; add **wider layouts** (e.g., calculator + live breakdown **side by side**), **multi-column** channel summary, **drawer** or persistent side panel for history where it improves efficiency. Tables and denser management views are acceptable **from a minimum breakpoint upward** only.

Shared across breakpoints:

- **One primary task per screen** on mobile (wizard or clear sections); desktop may show more at once without duplicating flows.
- **Show the math:** per-channel breakdowns and resolved preset costs must be visible; **all channel SRPs** appear together (US-2) — on small screens use stacking, accordions, or horizontal swipe between channel detail if needed; do not hide channels behind a single-channel-only view.
- **Calculator inputs — layout:** **Bulk cost (PHP)**, **quantity (kg)**, and **no. of pieces** appear on **one row** on sufficiently wide viewports; stack vertically on the narrowest phones (**NFR-07**). **Basket Label** (**FR-PC-35**) is a separate field; **do not** use a “focus channel” control for save labeling.
- **Channel emphasis (read-only):** optional UI highlight of one channel for legibility (**US-2**); not a mode that hides other channels’ results.
- **Active state:** clearly show **in use now** per item in history, the **dedicated active prices** page (**US-3a** — **hybrid** summary + expandable detail; **print** = full grid), and management overview where applicable (US-4, US-7).
- **Duplicate item names / multiple lineages:** When several lineages share an item key, history rows must not be visually identical **(FR-PC-30)** — use lineage hint, timestamps, and active badge.
- **Edit vs template entry points (§11.1.4):** Each history row exposes **two distinct actions** with different visual prominence: **"Update price"** (primary action — bound to the lineage, loads inputs into the calculator, saves as a new revision on the same chain) and **"Copy"** (secondary action — loads inputs as a starting point but saves as a new lineage / fork, does not mutate the source). Labels are fixed at "Update price" and "Copy" — implementations must not rename these or merge them into a single ambiguous action. On mobile, both actions must meet ~44px touch height; "Copy" may be placed in a row action menu (e.g., swipe or overflow) to reduce accidental taps.
- **Accessible:** labels tied to inputs, keyboard usable on desktop, sufficient color contrast; respect viewport meta and zoom (**NFR-11**).

### 9.3 Error states

All errors remain visible until the user **explicitly dismisses** them — no auto-dismiss. This ensures staff do not miss a failed save or a load problem on a busy or noisy screen.

| Scenario | Presentation | Dismissal |
|----------|-------------|-----------|
| **Field validation** (e.g., empty item name, non-numeric cost) | Inline message beneath the offending field; field border highlighted | Clears automatically when the field value becomes valid |
| **Save / network failure** (e.g., revision failed to write) | Persistent banner or modal at the top of the screen with error summary and a **Retry** button | User taps/clicks **Retry** (re-attempts the operation) or **Dismiss** (abandons retry) |
| **Preset load failure** (app cannot fetch `pricing-presets.json`) | Full-screen or top-level blocking notice: "Could not load pricing settings. Please reload." with a **Reload** button | User taps **Reload**; if reload fails, notice remains |
| **Management auth failure** (wrong PIN or expired session) | Inline error on the PIN entry screen; gated routes redirect to login | User re-enters credentials |
| **Actual spoilage patch failure** (FR-PC-26 PATCH fails) | Persistent inline error on the revision detail with **Retry** | User taps **Retry** or **Dismiss** |

**General rules:**
- Errors never silently disappear (**NFR-03**).
- Error messages use plain language — avoid raw HTTP status codes or server stack traces in the UI.
- On mobile (**NFR-07**), banners and modals must not obscure primary actions; place at top of viewport or as a full-width bar above content.
- Retry operations must be **idempotent** where possible (e.g., re-saving the same revision should not create a duplicate).

### 9.2 Agent design exploration (patterns)

Architecture or design agents choose **one** primary interaction pattern, justified for **phone-first** use, then describe **desktop adaptations** (breakpoint behavior). Evaluate **at least three** of:

1. **Wizard (mobile-native):** Bulk inputs (cost + kg + pieces on one row where width allows) → Review **all-channel** breakdown → Save (**Basket Label** + **in use now**) / History. **Desktop:** optional single view combining steps 1–2 side by side.
2. **Single primary screen + sheets:** Calculator full width; **bottom sheet** (mobile) or **right drawer** (desktop) for history and active overview; channel details collapsible on phone, **three-column** or card row on wide screens.
3. **Card-first vertical:** Large inputs; **one card per channel** stacked on mobile; **grid of channel cards** on desktop; history as full-screen list (mobile) or table with badges (desktop).
4. **Dense “pro” table (desktop-secondary):** Optional **≥ large breakpoint** management/history table; **never** the only path on mobile — phone uses list/cards.

Deliverable from design phase: **chosen pattern** + **mobile wireframe** (primary) + **desktop wireframe** (enhancement) + noted breakpoints (no pixel-perfect mandate).

---

## 10. Quality & verification

| Area | Expectation |
|------|-------------|
| Pricing engine | Unit tests: zero / invalid **sellable** qty (FR-PC-12), spoilage edge \(s \to 1\), rounding (**US-6** **`ceil_whole_peso`** + optional rules), **all three channels**, preset resolution (default vs category), **markup XOR margin**, order: markup/margin → **+\(A\)** → fees → rounding; **per-piece** SRP when `pieceCount` set (**ceil** to whole PHP); **fractional tiers** (**`srpPer500g`**, **`srpPer250g`**, **`srpPer100g`**) match **§5.2** (× **0.5 / 0.25 / 0.1** + channel **`roundingRule`**). |
| Config / UX | **FR-PC-15:** after simulated preset reload, UI updates and notice; no mutation of saved revisions. |
| Persistence | Integration: save → **second save as new revision** (same `lineageId`) → list/search shows expected revision behavior → **in use now** exclusivity per item; verify prior revision unchanged. Automated **HTTP + store** tests against a temp `data/` tree are recommended (**§14** acceptance). |
| Reports | Integration or unit: **FR-PC-40** — **P = min** of stored channel SRPs; **line_total = round_currency(P × derived.sellableQuantityKg)** (**US-8**); document currency rounding. |
| UI | Smoke tests or checklist: **NFR-10** viewports; **wide** desktop check; validation; all-channel display; active tagging; **US-3a** — **summary + expand** on screen, **print** shows **full** per-channel/package grid for every item, **CSV** complete; **FR-PC-31** filter controls + export links where implemented; no horizontal scroll for core flows at **360px** unless intentional (e.g., wide tables behind breakpoint). |

---

## 11. Architectural resolutions & open items

This section **closes** prior specification gaps with **v1 defaults** architects should implement unless a later ADR overrides them. **Remaining** business or environment choices stay under §11.2.

### 11.1 Resolved defaults (v1)

#### 11.1.1 Preset resolution (assistant context)

1. Management maintains **store defaults** and **zero or more named categories** (§7.3), each with **`spoilageRate`** and **`additionalCostPerKg`** (or hauling fields from which \(A\) is derived).
2. The assistant sees an **optional item category** control (FR-PC-07) populated **only** from those names. **Unselected** or **no categories configured** → use **store defaults** for spoilage and \(A\).
3. There is **no** separate item master or SKU catalog in **v1**; **`itemName`** is **free text** at save time. **Per-item preset overrides** (management-only) are **deferred** unless implemented as extra categories. **Suggested enhancement:** **US-1b** — optional **preset item name list** maintained by management; when present, assistants **pick** from the list **and/or enter a custom item name** when the product is not listed (see **§11.2**).

#### 11.1.2 Markup vs margin (canonical math)

Here **`unitCost`** means **\(C\)** — **cost per sellable kg** from **FR-PC-10** (bulk cost ÷ sellable kg, **after** spoilage). Per channel, config contains **exactly one** of:

| Mode | Config field (illustrative) | Formula |
|------|-----------------------------|--------|
| **Markup** | `markupPercent` | `priceAfterCore = C × (1 + markupPercent / 100)` |
| **Margin** | `marginPercent` (strictly between 0 and 100) | `priceAfterCore = C / (1 − marginPercent / 100)` |

Validation: **XOR** — setting both or neither is invalid at config load. Then **`priceBeforeFees` = `priceAfterCore` + \(A\)** (**FR-PC-14**). **Optional channel fees** (§7.2) apply to **`priceBeforeFees`** per **§11.1.9**, then **rounding** → **SRP**.

#### 11.1.3 Item identity & “in use now” key

- **v1 item key** = **normalize**(`itemName`): Unicode **NFC**, **trim** ASCII/Unicode whitespace, compare **case-insensitively** (recommend locale **en-PH** or **root** for stability — document in architecture).
- Optional field **`itemId`** may exist in JSON for **future** catalog use; **v1** logic must **not** require it for uniqueness or active-flag rules.

#### 11.1.4 New lineage vs new revision

| User action | Result |
|-------------|--------|
| **Save** from calculator with **no** open “edit” context (blank or only ephemeral template) | **New `lineageId`**, first `revisionId`. |
| **Open history → Edit / update this simulation** (explicit edit mode bound to a `lineageId`) → **Save** | **New revision**, **same `lineageId`**. |
| **Load from history as template** (duplicate / “use as starting point” **without** binding to that lineage’s edit mode) → **Save** | **New `lineageId`** (fork); does not mutate the source lineage. |

Architecture must expose **two distinct entry paths** (edit vs template) so assistants do not accidentally fork when they mean to revise.

#### 11.1.5 Ephemeral simulation vs changing presets

Covered by **FR-PC-15**: **recompute** unsaved UI from current presets + **non-blocking notice**; saved rows **immutable**.

#### 11.1.6 Management vs assistant surfaces

- **Single** web app: **default routes** for calculator + history (assistant).
- **Preset / management** editing on **separate paths** (e.g., `/settings`, `/management/presets`) protected by **one** of: **admin PIN** (validated **server-side** — **NFR-09**), **HTTP Basic** on those paths, **reverse-proxy auth**, or **VPN-only** deployment — chosen in deployment ADR. Assistants **never** see editable preset controls on shared screens unless authenticated as management.
- **Remote access reality:** Management accesses the app remotely (mobile data / home network); store assistants are on-site. The deployment ADR **must** explicitly state how management routes are secured for remote access. PIN over HTTPS is the minimum acceptable baseline; a **VPN** or **second factor** is strongly recommended. The architecture must **not** assume all users are on a trusted LAN.
- **How presets are edited (v1):** See **FR-PC-50** — in-app gated UI and/or server-side JSON edit + reload; architecture states which applies and how **config reload** propagates to clients (**FR-PC-15**).

#### 11.1.7 History search at scale

As in **FR-PC-31**: **O(n)** scan acceptable for **≤ 5,000** revisions total; plan index/compaction **before** exceeding that in production.

#### 11.1.8 Illustrative management config (spoilage, hauling, categories + channels)

Single-file example for **`pricing-presets.json`** (or equivalent). Semantics **must** match **§7.2**, **§7.3**, **§11.1.1**, and **§11.1.2**. **`schemaVersion`:** **2** only for implementations aligned with this spec; loaders **reject** any other value (**[archi.md §6.5](./archi.md)**).

**Store defaults:** `default` provides **`spoilageRate`** and either **`additionalCostPerKg`** (literal) **or** **`haulingWeightKg` + `haulingFees`** from which \(A = (\sum \text{amounts}) / H\) is computed at load or at each simulation.

**Categories:** Optional map of **category key** → same shape as `default` (override **`spoilageRate`** and/or hauling / **`additionalCostPerKg`**). Keys populate the assistant picker (FR-PC-07). Example keys align with **US-6** naming.

**Channels:** Each of `online` | `reseller` | `offline` includes **exactly one** of `markupPercent` or `marginPercent`. **US-6** illustrates **35% / 30% / 25%** markup for online / offline / reseller. **`roundingRule`:** v1 schema supports at least **`ceil_whole_peso`** (**US-6** — round up to whole PHP), **`nearest_0.25`**, and **`whole_peso`** (nearest whole PHP, half-up). Illustrative REDN config uses **`ceil_whole_peso`**. `channelFees` optional — apply per **§11.1.9** to **`priceBeforeFees`** (**FR-PC-14**).

```json
{
  "schemaVersion": 2,
  "default": {
    "spoilageRate": 0.25,
    "haulingWeightKg": 700,
    "haulingFees": [
      { "label": "driver", "amount": 2000 },
      { "label": "fuel", "amount": 4000 },
      { "label": "toll", "amount": 1000 },
      { "label": "handling", "amount": 200 }
    ]
  },
  "categories": {
    "vegetables": {
      "spoilageRate": 0.25,
      "freshnessWarningDays": 7
    },
    "fruits": {
      "spoilageRate": 0.22,
      "freshnessWarningDays": 7
    },
    "other_dry_goods": {
      "spoilageRate": 0.15,
      "additionalCostPerKg": 9.5,
      "freshnessWarningDays": 30
    }
  },
  "channels": {
    "online": {
      "markupPercent": 35,
      "roundingRule": "ceil_whole_peso",
      "channelFees": []
    },
    "offline": {
      "markupPercent": 30,
      "roundingRule": "ceil_whole_peso",
      "channelFees": []
    },
    "reseller": {
      "markupPercent": 25,
      "roundingRule": "ceil_whole_peso",
      "channelFees": []
    }
  }
}
```

**Edge cases:**

- **`categories` omitted or `{}`:** Use only `default`; hide assistant category control (FR-PC-07).
- **Category omits hauling:** Inherit **`additionalCostPerKg`** from **`default`** after resolving **`default`**’s hauling — merge precedence is locked in **[archi.md §6.3](./archi.md)**.
- **`other_dry_goods` example:** Literal **`additionalCostPerKg`** overrides hauling-derived \(A\) for that category when schema defines override precedence.
- **Config load:** Reject if any channel has both/neither `markupPercent` and `marginPercent`, if `spoilageRate` is outside [0,1), if hauling yields \(H \le 0\), or if `schemaVersion` is unsupported.

**`presetRef` (formalized — FR-PC-54):** `presetRef` on each saved revision is the `savedAt` timestamp (ISO-8601 UTC) of the matching entry in `preset-history.ndjson`. This enables forward lookup: given a changelog entry, find all revisions that used that preset version. Architecture must **not** use a hash or mtime — the `savedAt` timestamp is the canonical identifier (resolves **§11.2 #6** ambiguity; see **FR-PC-54** and **archi §4**).

#### 11.1.9 `channelFees` — evaluation contract (for schema authors)

Architects MUST document in the **architecture spec** (see **§14**):

1. **Ordered list** of supported `type` values (e.g., `percent_of_price_after_core`, `fixed_add`). *(Legacy name `percent_of_price_after_core` may be retained in schema; its numeric **base** is **`priceBeforeFees`** per **FR-PC-14**, not `priceAfterCore` alone.)*
2. For **percentage** fees: **base** is **`priceBeforeFees`** = **`priceAfterCore` + \(A\)** unless a type explicitly defines another base — **no compounding** across % fees unless a type says otherwise.
3. **Multiple fees** on one channel: apply in **array order**; each fee updates the **running price** after prior fees (architecture locks whether all % fees share the **initial** `priceBeforeFees` base or a running total — **one** rule globally).
4. **Rounding:** fee line amounts and final SRP: document **per-step** vs **end-only** (recommend **end-only** where possible; final channel rounding **last** per **FR-PC-14**).

#### 11.1.10 VAT handling (v1 default)

**All SRPs are all-in prices.** Every channel SRP displayed and stored is the final amount the customer pays — no VAT is added on top, no VAT line is broken out separately. There is no VAT computation or VAT display in v1.

**Rationale:** REDN Store operates on all-in pricing across all channels (online, reseller, offline); neither staff nor customers work with VAT-exclusive figures. Fresh agricultural produce is VAT-exempt under PH law (NIRC / RA 10963 TRAIN Act), so no VAT component applies to the current product scope. Should the store add VATable products (e.g., processed or packaged goods) or require VAT-exclusive invoicing for resellers in a future version, that is an ADR-gated v2 addition.

**Implementation note:** No schema change, no pipeline change, no UI change required for v1. Architects do **not** need to reserve a `vatRule` field or a VAT display slot.

---

### 11.2 Remaining open questions (business or deployment)

**v1 implementation defaults** are closed in **[archi.md §8](./archi.md)**; use this list when the business **overrides** those defaults or for **v2** planning.

1. **VAT / tax:** **Closed for v1** — see **§11.1.10**. All SRPs are all-in prices; no VAT computation or display. VAT-exclusive pricing and per-channel VAT rules deferred to v2.
2. **Selling unit vs purchase unit:** **Closed for v1:** purchase quantity is **kg**; optional **`pieceCount`** derives **SRP per piece** (**§5.1**, **FR-PC-14**). Further units (lb, bundle) remain **deferred** unless ADR.
3. **Deployment topology:** Local (Electron/Tauri), single internal server, or static + serverless API — still **environment-specific**; file storage path and backup procedure follow choice.
4. **Gross sales report quantities:** **Closed for v1:** row total = **min SRP × sellable kg** from the active revision (**US-8**, **FR-PC-40**). **Manual / CSV overlays** on top of sellable kg **deferred** to v2 unless ADR.
5. **Authentication strength:** **Closed for v1** — see **NFR-06** and **§11.1.6**. Management is remote (public internet); assistants are on-site. HTTPS is mandatory; PIN over HTTPS is the minimum baseline; VPN or second factor strongly recommended for management routes. The deployment ADR must document the remote-access hardening chosen. Further strengthening (hardware tokens, SSO) deferred to v2.
6. **Preset item names + custom entry (US-1b):** Whether to add an **`itemName` preset list** (e.g. array on **`pricing-presets.json`** or separate file) and **picker + custom** UX on the calculator. **Recommended default when implemented:** assistants may **always** enter a **custom `itemName`** if the item is not on the list; **optional** **preset-only** mode (block custom names) is a **management / deployment** choice. **Deferred** until the business adopts **US-1b** and architecture locks schema + **FR** row.

---

## 12. Glossary

| Term | Meaning |
|------|---------|
| **Simulation** | A pricing run from bulk inputs and management presets; may be unsaved (ephemeral) or **saved** to history. |
| **SRP** | Suggested retail price — guidance per **channel** for what to charge end customers. |
| **Bulk purchase** | Wholesale or lot purchase by the store; basis for cost allocation. |
| **Channel** | Sales path: online, reseller (B2B), or offline (walk-in). All three are computed for every simulation. |
| **Lineage** | Stable id grouping all **revisions** of one saved simulation over time (same item, successive edits). |
| **Revision** | One immutable stored snapshot of a simulation at a save/update; older revisions remain readable (FR-PC-21). |
| **In use now** | Flag on **at most one revision per item**: the store’s **current** price guidance until another revision for that item is marked active. |
| **Preset** | Management-defined **spoilage**, **required additional cost per kg** (§7.3), and per-channel markup/margin/rounding and fees (§7.2); assistants consume, not edit. |
| **Sellable quantity (kg)** | \(Q \times (1 - \text{spoilageRate})\) — weight available for sale after spoilage (**FR-PC-10**). |
| **Cost per sellable kg (\(C\))** | Bulk cost divided by sellable kg — basis for markup/margin (**FR-PC-10**, **§11.1.2**). |
| **Mobile-first** | UI designed for phone-sized viewports first; desktop/tablet layouts extend the same features (**NFR-07**, §9). |
| **Item key** | v1 identifier for **in use now** exclusivity: normalized `itemName` (**§11.1.3**). |
| **Basket Label** | Optional assistant-entered string on **save** to tag the simulation row (**FR-PC-35**); stored as **`basketLabel`** on the revision (**§8.1**). Not a sales channel. |
| **Preset item list** *(suggested)* | Management-defined **standard item names** assistants may **select** for **`itemName`** (**US-1b**); assistants may still **enter a custom item name** when the product is not listed unless **preset-only** mode is configured (**§11.2**). Not required in v1. |

---

## 13. Document history

| Version | Date | Summary |
|---------|------|---------|
| 0.1-draft | 2026-03-26 | Expanded from notes into architecture-ready specification. |
| 0.2-draft | 2026-03-26 | Aligned §2–§12 with §4: simulations, all-channel outputs, management presets, **in use now**, updates, gross sales report, data model. |
| 0.3-draft | 2026-03-26 | Review pass: version header sync; FR-PC-21/24/22 clarified (versioning, `updatedBy`, active flag); §7.2/§7.3 split channel fees vs store presets; §8 lineage/revision model and storage; glossary; verification and FR-PC-33 wording. |
| 0.4-draft | 2026-03-26 | **Mobile-first** UI mandate (**NFR-07**), §3.3/§9/§10/§12 updated; desktop as progressive enhancement. |
| 0.5-draft | 2026-03-26 | **§11** gap resolutions: preset resolution, markup/margin XOR, pipeline FR-PC-14, lineage rules, item key, concurrency, search scale, management routes; **NFR-08–10**; **FR-PC-07, 15, 16, 25**; US-4/5 and §7 updates. |
| 0.6-draft | 2026-03-26 | **§11.1.8** illustrative `pricing-presets.json` (default + category additional costs, per-channel XOR, fees, rounding); §7.3 cross-reference; `presetRef` note. |
| 0.7-draft | 2026-03-26 | Architecture-agent handoff: **FR-PC-30/40/50** expanded; **§8.1** snapshot shows markup+margin channels + breakdown; **NFR-09** server-side PIN, **NFR-11/12**; **§11.1.9** channelFees contract; **§9.1** multi-lineage UX; **§14** deliverables checklist. |
| 0.7.1-draft | 2026-03-26 | **§14** links to companion **[archi.md](./archi.md)**. |
| 0.7.2-draft | 2026-03-26 | **§14** links **[implementation-playbook.md](./implementation-playbook.md)** for agent-driven implementation. |
| 0.8.0-draft | 2026-03-26 | **US-1** (kg + optional pieces, spoilage wording); **US-6** REDN hauling/spoilage/markup/SRP-per-piece narrative. **§4.3** consistency note: **§5.2 / §11.1.2 / §11.1.8** pending reconciliation. |
| 0.9.0-draft | 2026-03-26 | **§4.3 resolved:** **FR-PC-10/11/14** = spoilage + **\(C\)** + **+\(A\)** pipeline; **§7.3** hauling model; **§8.1** `schemaVersion` **2** + `bulkQuantityKg` / `pieceCount`; **§11.1.8** presets v2; **§11.1.9** fee base **`priceBeforeFees`**; **§10** / **§11.2** / glossary updated. |
| 0.9.1-draft | 2026-03-26 | **Rebuild stance:** **§8.1** / **§11.1.8** / **§14** — **v2-only** persistence, no legacy revision or preset schema; **FR-PC-07** / **FR-PC-14** cross-link **archi §6.3–6.4**; **§11.1.8** category merge pointer **archi §6.3**. |
| 0.9.2-draft | 2026-03-26 | **FR-PC-35 / §8.1:** **`focusChannel`** → **`basketLabel`**; **FR-PC-04** / **FR-PC-31** / **§9.1** / **§9.2** / **US-3–4** / glossary — calculator row layout (cost + kg + pieces); remove save-time channel picker. |
| 0.9.3-draft | 2026-03-26 | **FR-PC-31** concrete filters + **FR-PC-41** / **FR-PC-40** CSV; **§10** integration-test note; **§15** visual expectations (assistant + management); **§14** acceptance extended. |
| 0.9.4-draft | 2026-03-26 | **US-8** / **FR-PC-40:** gross sales = **P × sellable kg** (stored **`derived.sellableQuantityKg`**); **§8.1**, **§10**, **§11.2 #4**, **§15**; remove v1 manual-Q report mode. |
| 0.9.5-draft | 2026-03-28 | **US-6** reconciliation: **FR-PC-14** **`ceil_whole_peso`** for per-kg SRP (default) and **SRP per piece**; **§4.3**, **§7.2**, **§8.1**, **§11.1.8** examples and **`roundingRule`** enum note. |
| 0.9.6-draft | 2026-03-28 | **US-3a** assistant **active prices** page: **FR-PC-36**; **FR-PC-34** cross-link; **§9.1**, **§10**, **§15**, stakeholder row. |
| 0.9.7-draft | 2026-03-28 | **US-1b (suggested):** preset **item name** picker for assistants; **§11.1.1** note; **§11.2 #6**; glossary; **§15.1** calculator note. |
| 0.9.8-draft | 2026-03-28 | **US-1b:** assistant **custom item name** alongside preset pick; **§11.2 #6** (recommended **picker + custom**; optional **preset-only**); glossary + **§15.1**. |
| 0.9.9-draft | 2026-03-29 | **US-3a / FR-PC-36:** **hybrid** active page — **compact summary** (incl. **min per-kg SRP** = **P**-style) + **expandable** full channel/package grid; **print** always **full** grid; **§4.1** acceptance, **§5.4**, **§9.1**, **§10**, **§15.1**. **Companion bump:** [archi.md](./archi.md) **0.8.11-draft** (companion **v0.9.9-draft**), [implementation-playbook.md](./implementation-playbook.md), project [README.md](../README.md) — pins and **US-3a** build notes aligned with this spec. |
| 0.9.10-draft | 2026-03-29 | **Package tiers:** **250g** replaces **200g** — **US-1**, **US-3**, **US-3a**, **FR-PC-36**, **§5.2** normative note; persisted field **`srpPer250g`** (× **0.25** + channel **`roundingRule`**). **No** read-time migration from **`srpPer200g`** — **rebuild** JSON (**archi §6.5**). **Companion bump:** [archi.md](./archi.md) **0.8.15-draft**, [implementation-playbook.md](./implementation-playbook.md), [README.md](../README.md). |
| 0.9.11-draft | 2026-03-29 | **Consistency pass:** §8.1 **Constraints** require **`srpPer500g` / `srpPer250g` / `srpPer100g`** (example JSON still abbreviated); §10 pricing-engine row + **fractional tiers**; **US-1c** labeled **(suggested)** and **Active** basket-label dropdown aligned with **FR-PC-36** until **US-1c** ships; **US-3a** rationale + **Acceptance criteria (US-3a)** nesting. **Companion:** [archi.md](./archi.md) **0.8.17-draft**, [implementation-playbook.md](./implementation-playbook.md) (agent execution + **§14** pointer). |
| 0.9.12-draft | 2026-03-31 | **§11.1.10 VAT default:** all SRPs are all-in prices; no VAT computation or display in v1. Closes **§11.2 #1**. |
| 0.9.13-draft | 2026-03-31 | **FR-PC-53 Preset impact preview:** before saving presets, system shows per-item SRP delta across all active simulations; management must confirm before commit. **§15.2** visual note added. |
| 0.9.14-draft | 2026-03-31 | **FR-PC-37 Price freshness indicator:** optional `freshnessWarningDays` per category scope in presets; stale active items show age badge on active page. **§11.1.8** schema example updated. **§15.1** visual note added. |
| 0.9.15-draft | 2026-03-31 | **FR-PC-26 Actual spoilage recording:** assistants may patch `actualSpoilageKg` on a revision post-sale; **§8.1** schema updated; **US-7** extended — management sees preset vs actual spoilage per item. |
| 0.9.16-draft | 2026-03-31 | **FR-PC-34b Price history timeline:** management lineage detail shows combined table + chart of per-channel SRP across all revisions over time. **US-5** extended. **§15.2** visual note added. |
| 0.9.17-draft | 2026-03-31 | **FR-PC-17 Gross margin and markup display:** management-session-only per-channel gross margin % and markup % derived from \(C + A + \text{channelFees}\) vs SRP; shown in calculator and history detail. **FR-PC-32** extended. **§15.2** visual note added. |
| 0.9.18-draft | 2026-03-31 | **FR-PC-54 Preset version history and restore:** append-only `preset-history.ndjson` changelog; management UI shows history list with Restore action (creates new entry, never overwrites). **§8.1** `presetRef` formalized as `savedAt` timestamp. **§8.2** storage note added. **§15.2** visual note added. |
| 0.9.19-draft | 2026-03-31 | **NFR-13 + FR-PC-38 Active price change notifications:** optional `NOTIFICATION_WEBHOOK_URL` env var; server fires non-blocking POST on every "in use now" change; compatible with Slack, Viber, Telegram, Messenger. **§14 deliverable #8** updated. |
| 0.9.20-draft | 2026-03-31 | **§8.1 field length constraints:** max lengths for `itemName` (120), `basketLabel` (80), `notes` (1,000), `updatedBy` (60). |
| 0.9.21-draft | 2026-03-31 | **Sort fields enumerated:** **FR-PC-31** history (`updated_desc` default + 3 options); **FR-PC-36** active page (`item_asc` default + 5 options including `active_desc`, `srp_asc/desc`). |
| 0.9.22-draft | 2026-03-31 | **Edit vs template UX:** action labels fixed as **"Update price"** (new revision, same lineage) and **"Copy"** (new lineage fork). Added to **§9.1** and **§15.1** history visual note. |
| 0.9.23-draft | 2026-03-31 | **FR-PC-30 pagination:** default page size 25, `page` param (1-based), `total` + `pageCount` in response; pagination required (history expected to grow). |
| 0.9.24-draft | 2026-03-31 | **FR-PC-40 time scope:** gross sales report is a snapshot at request time; `generatedAt` timestamp added to JSON and CSV output. |
| 0.9.25-draft | 2026-03-31 | **§9.3 Error states:** all errors persist until explicitly acknowledged; per-scenario table (field validation, save failure, preset load failure, auth failure, spoilage patch failure) with presentation and dismissal rules. |
| 0.9.26-draft | 2026-03-31 | **NFR-06 + §11.1.6 remote access:** management is remote (mobile data / home network); assistants are on-site. LAN-only assumption removed; HTTPS required; VPN or second factor strongly recommended for management routes. |
| 0.9.27-draft | 2026-03-31 | **US-1c acceptance criteria:** prefixed with conditional note — criteria are not §14 exit criteria until US-1c is formally promoted from "suggested" to normative. |

---

## 14. Architecture specification deliverables (for agents)

**Living draft:** [archi.md](./archi.md) — companion architecture document; keep in sync as §11.2 decisions are closed.

**Agent implementation order:** [implementation-playbook.md](./implementation-playbook.md) — phased build, task templates, PR gates.

The following should exist as a **separate architecture document** (or ADR set) derived from this spec. It is **not** a second product spec — it **instantiates** open choices and **locks** implementation details.

| # | Deliverable | Purpose |
|---|-------------|---------|
| 1 | **Context diagram** | **C4 Level 1** (or equivalent): browser, optional API server, file/storage paths, who accesses management routes. Clarify **NFR-08** (online-required) vs **Electron/Tauri** “local app data” if chosen (**§11.2**). |
| 2 | **Container / module diagram** | **C4 Level 2** or block diagram: UI, **preset loader**, **pricing engine**, **persistence adapter**, **report** generator; boundaries for tests (§5.2 note). |
| 3 | **API or IPC surface** | If web + server: **REST/route list** (calculator read presets, save revision, list/search, report, management write). If **static + file only**: document **no API** and **reload** semantics (**FR-PC-50**). |
| 4 | **JSON Schemas** (or Zod/OpenAPI) | **§11.1.8** `pricing-presets.json`; **§8.1** revision record; **§11.1.9** fee `type` enum and ordering rules. |
| 5 | **Write / transaction strategy** | Atomic steps for: **new revision**, **set in use now** (clear others for item key — **FR-PC-24**), concurrent writers (**FR-PC-25**). |
| 6 | **§11.2 decision table** | One row per open question: chosen **v1** answer or **explicitly deferred** + owner/date. |
| 7 | **Threat model (short)** | Internal LAN assumption; **management** path abuse; **file path** exposure; **CORS**; align with **NFR-09**. |
| 8 | **Deployment & backup** | Where files live, how `pricing-presets.json`, `preset-history.ndjson`, and simulation data are **backed up** and restored. Document `NOTIFICATION_WEBHOOK_URL` env var — how to configure for Slack, Viber, Telegram, or Messenger; note that delivery failure is non-fatal (**NFR-13**, **FR-PC-38**). |

**Acceptance:** Implementation follows **FR-PC-10/14**, **FR-PC-24**, **FR-PC-31** (list/search on latest-per-lineage + minimum field set), **FR-PC-36** (**US-3a** hybrid active page + **`GET /api/overview/active`** CSV; **print** = full grid per **§4.1**), and **item key** rules without ambiguity; **only** **`schemaVersion: 2`** for **`pricing-presets.json`** and simulation revisions per **§8.1** / **§11.1.8** — greenfield JSON, **no** legacy schema path (**archi §6.5**). Optional **CSV** / **JSON** exports (**FR-PC-40**, **FR-PC-41**) and **§15** visual checks complete the v1 assistant/management bar when claimed “spec-complete.”

---

## 15. Visual expectations on the app

Normative **behavior** stays in **§5–§9**; this section is the **visual / UX bar** for “done” reviews (mobile-first **NFR-07**, touch **NFR-10**, contrast **NFR-11**).

### 15.1 Store assistant

- **Calculator:** Single-column on phone; **bulk cost**, **quantity (kg)**, **no. of pieces** stacked on the narrowest widths, **one row** from the breakpoint defined in implementation (see **§9.1**). **Basket Label** is a separate labeled field below numeric inputs — never styled as a “channel” choice. If **US-1b** is implemented, **item name** may combine a **preset picker** with a clear path to **enter a custom name** when the item is not in the list (**§11.2**); **preset-only** mode is optional.
- **All-channel SRPs:** Every channel’s price is visible on the same task (cards, list, or accordion — **§9.1**); no flow that hides two channels behind a single-channel-only default.
- **History:** List shows **one row per lineage** (latest revision) with **item name**, **last updated**, **lineage hint**, **in use** badge, and **basket label** when set (**FR-PC-30**, **US-4**). A **search / filter** block exposes at least **item text**, **date range**, optional **basket label**, **notes**, and **in use only** (**FR-PC-31**). Each row has two distinct actions: **"Update price"** (primary — new revision, same lineage) and **"Copy"** (secondary — new lineage fork). Both meet **~44px** touch height; "Copy" may be in a row overflow menu to reduce accidental forks (**§9.1**, **§11.1.4**).
- **Active / Report:** **Active prices** (**US-3a** / **FR-PC-36**): default list = **compact row** per item (**item**, **min per-kg SRP** across channels, spoilage, kg, category, active time, basket, notes); **expand** for **full** channel × package grid; **print** = **always** full grid for every item (**printed** sheets / physical posting — not the **offline** sales **channel**); **CSV** = full data. Filters/sort as specified. Management active overview (**US-7** / **FR-PC-34**) may share the same API. Gross-sales report matches **FR-PC-40** / **US-8**; optional **Download CSV** matches on-screen columns.
- **Preset drift:** When presets change while the calculator is open, user sees a **short, non-blocking notice** after refresh (**FR-PC-15**).
- **Freshness indicator (FR-PC-37):** On the active prices page, compact summary rows for stale items show an age badge (e.g., "7d ago") in a muted or warning color. The badge does not obscure the item name or SRP. No indicator is shown for items whose category has no `freshnessWarningDays` configured.

### 15.2 Management

- **Settings / presets:** Gated route; **PIN** never echoed in UI after submit (**NFR-09**). Preset editor is **JSON** or structured form per implementation; invalid JSON shows a clear error, no silent fail.
- **Preset history and restore (FR-PC-54):** Settings page includes a preset changelog list (newest first) with `savedAt`, `savedBy`, and key values summary. Each entry has a **Restore** action that loads the snapshot into the editor as a draft — management confirms before it is committed as a new entry. Restore never deletes history.
- **Gross margin and markup (FR-PC-17):** Per-channel breakdown shows two additional read-only lines — **gross margin %** and **gross markup %** — computed from \(T = C + A + \text{channelFeeTotal}\) vs SRP. Displayed in the calculator (live) and history detail (from stored values). **Management session only** — not visible to assistants.
- **Price history timeline (FR-PC-34b):** In the lineage detail view, a **table + chart** combination shows per-channel per-kg SRP across all revisions. Table lists revision index, date, basket label, per-channel SRPs, and in-use status. Chart is a multi-line plot (one line per channel) with date on x-axis and SRP on y-axis. Both are visible on the same screen; on narrow viewports the chart may stack below the table. Management-only view — not shown to assistants.
- **Preset impact preview (FR-PC-53):** After editing presets and before the final save, the UI shows the impact table inline — item name, channel, current SRP → new SRP, delta. A **Confirm save** and **Cancel** action are clearly distinguished. If no active items exist, show "No active simulations affected" and allow save directly. The preview re-runs automatically if management edits the draft further before confirming.
- **Same history affordances** as assistant for read-only review where shared (**US-5**); editing presets does not mutate saved revision files (**FR-PC-15**).

---

*End of specification.*
