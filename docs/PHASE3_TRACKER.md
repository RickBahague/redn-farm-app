# Phase 3 tracker — acquisition SRP pipeline

Companion to `rebuild_plan.md` Phase 3 (SRP computation on save/update). This file may be updated as work lands.

## Done

- **Math / data**
  - `SrpCalculator`, `PricingChannelEngine`, preset snapshot JSON via `PricingPresetGson`.
- **Repository**
  - `AcquisitionRepository`: `insertWithPricing`, `updateWithPricing`, `insertPlain`, `getAcquisitionById`, `getActiveSrpForProduct`.
  - Outcomes: `Success`, `ValidationError`, `SavedWithoutActivePreset`.
  - Hilt: `AcquisitionRepository` and `PricingPresetRepository` use `@Singleton` + `@Inject` only (duplicate `@Provides` removed from `RepositoryModule`).
- **DAO**
  - `ProductDao.getProductById`, `AcquisitionDao.getById` (as used by repository).
- **Integration**
  - `DatabasePopulator.populateAcquisitions` → `insertPlain`.
  - `ExportViewModel` builds full `AcquisitionRepository` (preset + product DAO).
  - `AcquireProduceViewModel` uses new save APIs; `userMessage` → snackbar.
- **UI**
  - Acquire: pieces-per-kg field when unit is per-piece; edit resolves `Product` from catalog when possible; cards show online SRP line when present.
  - **Optional follow-ups (done):** debounced **SRP preview** in add/edit dialog (`previewDraftPricing` + scrollable dialog); **expandable “All channel SRPs”** on cards (per-kg, pack tiers, per-piece); `SrpDraftPreviewPanel`.
- **Tests**
  - `SrpCalculatorTest` (JVM), including bulk-cost scaling check.
  - `AcquisitionRepositorySnapshotInstrumentedTest` (androidTest / Room in-memory): snapshot recompute on cost change; metadata-only update keeps SRPs.

## Follow-ups (optional)

- Further polish: remember expanded state per card across scroll (e.g. `rememberSaveable` with id).
