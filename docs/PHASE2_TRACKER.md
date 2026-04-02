# Phase 2 — Pricing preset system (detailed tracker)

**Created:** 2026-04-02  
**Related plan:** `rebuild_plan.md` Phase 2 (this file does **not** modify that document).  
**Goal:** Admin can create inactive presets, browse history/detail, restore as new draft, and activate via mandatory preview (MGT-US-00 … MGT-US-06, SYS-US-01).

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Implemented in codebase |
| `[~]` | Partial / simplified — see notes |
| `[ ]` | Manual QA / follow-up |

---

## P2-1 — `PricingPresetRepository`

| Item | Status | Notes |
|------|--------|--------|
| `savePreset` (insert inactive) | `[x]` | `PricingPresetRepository.savePreset` |
| `activatePreset` transaction | `[x]` | Delegates to `PricingPresetDao.activatePreset` + log row |
| `getActivePreset` | `[x]` | `Flow` |
| `getAllPresets` | `[x]` | `Flow` (via DAO) |
| `getActivationLog` | `[x]` | `Flow` — **UI not wired** in v1 Phase 2 (data available for later screen) |
| `getPresetById` | `[x]` | Extra helper for detail/editor/preview |

**File:** `app/src/main/java/com/redn/farm/data/repository/PricingPresetRepository.kt`

---

## P2-2 — JSON models & Gson

| Item | Status | Notes |
|------|--------|--------|
| `HaulingFeeItem`, `CategoryOverride`, `ChannelFee`, `ChannelConfig`, `ChannelsConfiguration` | `[x]` | `data/pricing/PricingPresetJsonModels.kt` |
| Encode/decode helpers | `[x]` | `PricingPresetGson` (+ defaults aligned with rebuild plan / PricingReference) |

**Files:** `app/src/main/java/com/redn/farm/data/pricing/PricingPresetJsonModels.kt`

---

## P2-3 — Settings entry (SYS-US-01 / MGT-US-00)

| Item | Status | Notes |
|------|--------|--------|
| Settings screen | `[x]` | `ui/screens/settings/SettingsScreen.kt` |
| Admin-only entry | `[x]` | Gear icon on `MainScreen` when `MainViewModel.isAdmin` |
| Session role | `[x]` | `SessionManager.createSession(username, role)`; `LoginViewModel` passes `user.role` |
| Fallback admin check | `[x]` | `MainViewModel` resolves `UserDao` if legacy session has no role key |

**Files:** `SettingsScreen.kt`, `MainScreen.kt`, `MainViewModel.kt`, `SessionManager.kt`, `LoginViewModel.kt`, `DatabaseModule.kt` (`UserDao`)

---

## P2-4 — Pricing presets home

| Item | Status | Notes |
|------|--------|--------|
| Active summary | `[x]` | `PricingPresetsHomeScreen` + `PricingPresetsHomeViewModel` |
| New Preset / History actions | `[x]` | Nav to editor (`new`) and history list |

**Files:** `ui/screens/pricing/PricingPresetsHomeScreen.kt`, `PricingPresetsHomeViewModel.kt`

---

## P2-5 / P2-6 — Preset editor & save

| Section | Status | Notes |
|---------|--------|--------|
| Spoilage & hauling | `[x]` | Rate, optional direct ₱/kg **or** hauling weight + fee lines |
| Channel markups/margins | `[x]` | Online / reseller / offline; markup **XOR** margin; rounding dropdown |
| Categories | `[x]` | Add/remove; optional spoilage & ₱/kg overrides |
| Save inactive + message | `[x]` | Snackbar with name + id prefix; validation errors via snackbar |

**Simplification (`[~]`):** Per-channel **fee list** (fixed/pct) is **not** exposed in the editor UI yet; JSON supports `fees` and engine in Phase 3 can read them if present. Default empty.

**Files:** `PricingPresetEditorScreen.kt`, `PricingPresetEditorViewModel.kt`

---

## P2-7 / P2-8 — History & detail

| Item | Status | Notes |
|------|--------|--------|
| Newest-first list | `[x]` | `PresetHistoryScreen` (DAO order) |
| Active badge | `[x]` | Check icon on active row |
| Read-only detail | `[x]` | JSON + key numbers in `PresetDetailScreen` |
| Restore | `[x]` | Opens editor with `sourcePresetId`; save creates **new** UUID |
| Activate from detail | `[x]` | Only if `!is_active`; routes to preview |

**Files:** `PresetHistoryScreen.kt`, `PresetHistoryViewModel.kt`, `PresetDetailScreen.kt`, `PresetDetailViewModel.kt`

---

## P2-9 / P2-10 — Activation preview & commit

| Item | Status | Notes |
|------|--------|--------|
| Mandatory preview | `[x]` | `PresetActivationPreviewScreen` — cannot activate from detail without this step |
| Example SRPs | `[x]` | `PresetPreviewCalculator` — ₱5,000 / 100 kg scenario |
| Confirm activates | `[x]` | `PricingPresetRepository.activatePreset` |
| Navigate back to home | `[x]` | `popBackStack(PricingPresetsHome, inclusive = false)` |

**Files:** `PresetActivationPreviewScreen.kt`, `PresetActivationPreviewViewModel.kt`, `PresetPreviewCalculator.kt`

---

## Navigation

| Route | Purpose |
|-------|---------|
| `settings` | Settings hub |
| `pricing_presets` | Presets home |
| `preset_history` | All presets |
| `pricing_preset_editor/{sourcePresetId}` | `new` or preset id to clone |
| `preset_detail/{presetId}` | Read-only + actions |
| `preset_activation_preview/{presetId}` | Activation gate |

**File:** `navigation/NavGraph.kt`

---

## DI

| Item | File |
|------|------|
| `PricingPresetRepository` binding | `di/RepositoryModule.kt` |
| DAOs (existing) | `di/DatabaseModule.kt` |

---

## Automated verification

| Command | Result | When |
|---------|--------|------|
| `./gradlew assembleDebug` | SUCCESS | 2026-04-02 |

---

## Manual QA checklist

1. Log in as **admin** → Settings (gear) visible; log in as **user** → no gear.  
2. Settings → Pricing Presets → New Preset → Save → appears in History (inactive).  
3. Open detail → Restore as new draft → Save → **new** row, old row unchanged.  
4. Inactive preset → Activate… → preview numbers → Confirm → home shows **ACTIVE** summary.  
5. Second activation → previous active cleared (DB); check `preset_activation_log` in inspector if needed.

---

## Follow-ups (not Phase 2 blockers)

- UI for **activation log** (`getActivationLog`).  
- Editor UI for per-channel **fees** list.  
- **FR-PC-53**-style “what if” on all active acquisitions (Phase 3 + acquisitions with SRPs).  
- **Preset-only** JSON file changelog (`preset-history.ndjson`) if product requires file audit separate from Room.

---

*End of Phase 2 tracker.*
