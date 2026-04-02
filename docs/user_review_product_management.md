# User Review — Epic 3: Product Management (Thorough)

**Reviewed:** 2026-04-02  
**Scope:** Full stack — UI, ViewModel, Repository, DAO, Entity, Domain Model, Initializer  
**Files reviewed:**
- `ui/screens/manage/products/ManageProductsScreen.kt`
- `ui/screens/manage/products/ManageProductsViewModel.kt`
- `data/repository/ProductRepository.kt`
- `data/local/dao/ProductDao.kt`
- `data/local/dao/ProductPriceDao.kt`
- `data/local/entity/ProductEntity.kt`
- `data/local/entity/ProductPriceEntity.kt`
- `data/model/Product.kt`
- `data/model/ProductPrice.kt`
- `data/model/ProductFilters.kt`
- `data/local/DatabaseInitializer.kt`
- `data/pricing/OrderPricingResolver.kt` (cross-epic consumer)
- `data/repository/AcquisitionRepository.kt` (cross-epic consumer)

**Stories reviewed:** PRD-US-01 through PRD-US-08  

---

## Overall Assessment

The Product Management implementation is split across two eras of design. The **data layer** (entity, DAO, repository) is largely aligned with v4 — `category`, `default_piece_count`, and the SRP-based pricing pipeline are wired in. The **UI layer** is still v3: price text boxes in Add/Edit dialogs, no unit type picker, no deactivate/delete buttons in the list, and the ViewModel's exposed state is bypassed entirely. Several architectural issues compound across all layers.

---

## Layer-by-Layer Findings

---

### 1. Domain Model — `Product.kt` / `ProductPrice.kt` / `ProductFilters.kt`

**`Product.kt` — Good**  
`category: String?` and `defaultPieceCount: Int?` are present. `unit_type: String` is present. No issues.

**`ProductPrice.kt` — v3 legacy field**  
`discounted_per_kg_price` and `discounted_per_piece_price` are present. The v4 stories have no "discounted price" concept — this concept was replaced by channel SRPs (online/reseller/offline). The fields survive in v4 only as part of the manual fallback story (PRD-US-06), but the stories define the fallback as a single per-kg or per-piece price, not a separate discounted variant. The field is currently repurposed in `OrderPricingResolver` to mean "reseller price" — an undocumented implicit assumption.

**`ProductFilters.kt` — Incomplete**  
```kotlin
data class ProductFilters(
    val searchQuery: String = "",
    val showOutOfStock: Boolean = false,
    val sortBy: String = "name"
)
```
PRD-US-01 AC#7 requires filters for unit type, category, and active status. None of these are in the model. The `showOutOfStock` boolean partially covers active status but inverts the meaning (it shows inactive products when true, whereas the story says "filter by active status").

---

### 2. Entity — `ProductEntity.kt` / `ProductPriceEntity.kt`

**`ProductEntity.kt` — Good**  
`category: String?` and `default_piece_count: Int?` were added in Phase 0. Correctly nullable with defaults. No issues.

**`ProductPriceEntity.kt` — v3 legacy schema**  
Still carries `discounted_per_kg_price` and `discounted_per_piece_price`. The `date_created` column is `LocalDateTime` stored via `DateTimeConverter` as epoch seconds. This is correct but note that `ProductPriceDao.getLatestPrices()` joins on `MAX(date_created)` — since `date_created` is stored as a Long (epoch seconds), two rows inserted within the same second will produce ambiguous results in the MAX join. Low risk in practice but worth noting.

---

### 3. DAO — `ProductDao.kt` / `ProductPriceDao.kt`

**`ProductDao.kt` — Missing queries**  
- No query to filter by `category`, `unit_type`, or `is_active` — all filtering is done in-memory in `ProductRepository.getFilteredProducts()`. This is fine for small catalogs but the filter for `product_id` search is absent entirely (PRD-US-01 AC#6).
- `getAllProducts()` orders by `product_name ASC` — reasonable default.

**`ProductPriceDao.kt` — `getPriceHistory()` is unused**  
`getPriceHistory(productId: String)` exists at line 55 and returns all prices for a product in reverse order — exactly what PRD-US-07 needs. It is defined but never called anywhere in the codebase.

---

### 4. Repository — `ProductRepository.kt`

**`insertProduct()` and `updateProduct()` — Good**  
Both correctly pass `category` and `default_piece_count` through to the entity. `toProduct()` correctly maps them back. No issues.

**`getFilteredProducts()` — Two bugs**

*Bug 1 — Search does not cover product ID (PRD-US-01 AC#6):*
```kotlin
// Current — misses product_id
product.product_name.contains(filters.searchQuery, ignoreCase = true) ||
product.product_description.contains(filters.searchQuery, ignoreCase = true)

// Required
product.product_id.contains(filters.searchQuery, ignoreCase = true) ||
product.product_name.contains(filters.searchQuery, ignoreCase = true) ||
product.product_description.contains(filters.searchQuery, ignoreCase = true)
```

*Bug 2 — `showOutOfStock` filter logic is inverted:*
```kotlin
// Current: when showOutOfStock = false, filter to only is_active = true (correct)
// But the flag name implies showing OUT OF STOCK items, not hiding inactive ones
// PRD-US-01 AC#7 says "filter by active status" — should be an explicit active/inactive/all filter
if (!filters.showOutOfStock) {
    filteredProducts = filteredProducts.filter { it.is_active }
}
```

**`updateProductPrice()` is named misleadingly**  
It always inserts a new row (by design, to maintain history). It is named `update` but performs an insert. This is a correct implementation of the price history pattern but the naming causes confusion for anyone reading the code later. Should be `insertProductPrice()` (which already exists) and `updateProductPrice()` should be removed or clearly documented as "insert a new historical entry."

Actually it is worse: both `insertProductPrice()` and `updateProductPrice()` both insert new rows (`price_id = 0`), with no actual difference except the DAO method called. One of them is redundant.

---

### 5. ViewModel — `ManageProductsViewModel.kt`

**ViewModel exposes flows that the screen ignores**

The ViewModel exposes `products` and `productPrices` as `StateFlow` (lines 32–44). The screen does not use them. Instead, the screen creates its own `ProductRepository` instance in a `remember` block (lines 49–51) and collects from it in `LaunchedEffect(Unit)` blocks. This means:

- There are **two separate `ProductRepository` instances** in the same screen — one owned by the ViewModel, one owned by the screen.
- Updates made via the ViewModel (e.g., `viewModel.deleteProduct()`) are observed by the ViewModel's flow, not the screen's local flow. The screen's list won't update after a delete unless the screen's own `LaunchedEffect` happens to re-collect.
- The `LaunchedEffect(Unit)` with `collect {}` pattern never re-collects unless the composable leaves and re-enters the composition.

**This is the root cause of potential stale list state** — after any add/edit/delete, the product list may not update.

**`deleteProduct()` exists but is never called from the screen**  
The ViewModel has `fun deleteProduct(productId: String)` at line 78. The screen has `var showDeleteDialog by remember { mutableStateOf<Product?>(null) }` at line 57 which is declared but never set, never shown, and never used.

**`reinitializeDatabase()` is never triggered from the screen**  
The method exists at line 46. The screen collects `isReinitializing` and shows a `CircularProgressIndicator`. But no button in the screen calls `viewModel.reinitializeDatabase()`. The `Icons.Default.Refresh` icon is imported at line 9 but not placed in any composable.

**ViewModel bypasses Hilt injection**  
`ManageProductsViewModel` does not use `@HiltViewModel`. It creates its own `ProductRepository` via direct constructor call (lines 21–24). This means:
- The ViewModel cannot receive injected dependencies.
- It calls `FarmDatabase.getDatabase(application)` twice — once for `productDao()` and once for `productPriceDao()`. Both calls return the same singleton instance, so this is harmless, but the pattern is inconsistent with the rest of the app.

---

### 6. Screen — `ManageProductsScreen.kt`

#### `AddProductDialog` — Multiple critical issues

**Issue 1 — `unit_type` is hardcoded as `""`:**
```kotlin
val newProduct = Product(
    product_id = "",
    product_name = name,
    product_description = description,
    unit_type = "",   // ← always empty string
    is_active = true
)
```
Every product added through this dialog has an empty `unit_type`. This breaks:
- `defaultIsPerKgForProduct()` in `TakeOrderViewModel` — it returns `true` for any non-"piece" type, so empty string defaults to kg mode. Accidentally correct for kg products, wrong for piece products.
- `productSupportsDualUnit()` — determines dual-unit support partly from `unit_type` via the `per_kg_price`/`per_piece_price` fallback check.
- PRD-US-02 AC#1 explicitly requires unit type.

**Issue 2 — Price fields should not be in Add dialog (PRD-US-02):**  
The dialog has four price fields: Per Kg, Per Piece, Discounted Per Kg, Discounted Per Piece. Per PRD-US-02, a new product should have no price at add time — price is set separately via PRD-US-06 (manual fallback) or computed by the first acquisition.

**Issue 3 — Save is gated on price entry:**
```kotlin
enabled = name.isNotEmpty() && (perKgPrice.isNotEmpty() || perPiecePrice.isNotEmpty())
```
A product cannot be saved without entering at least one price. The story requires a product to be saveable without any price.

**Issue 4 — Missing fields:** No `unit_type` picker, no `category` field, no `default_piece_count` field.

#### `EditProductDialog` — Same issues plus a silent side effect

**Issue 1 — Merges product attribute edit and price write in one save:**
```kotlin
onSave = { updatedProduct, updatedPrice ->
    scope.launch {
        repository.updateProduct(updatedProduct)
        repository.updateProductPrice(updatedPrice)  // ← always runs
        showEditDialog = null
    }
}
```
Editing a product's name always also writes a new `product_prices` row with whatever is in the price fields. If the admin edits only the name, a duplicate price row is inserted silently. This pollutes the price history (PRD-US-07).

**Issue 2 — Missing fields:** No `unit_type` picker (current value is not even shown), no `category`, no `default_piece_count`, no active/inactive toggle.

**Issue 3 — Price fields should not be here** (same as Add — PRD-US-03 scope).

#### `ProductCard` — Wrong price source

```kotlin
val price = productPrices.find { it.product_id == product.product_id }
ProductCard(product = product, productPrice = price, ...)
```

Prices displayed come from `product_prices` (manual fallback table). Per PRD-US-01 AC#2, the list should show the **active SRP from the most recent acquisition**. The `product_prices` table is the fallback of last resort, not the primary price display.

The card shows `per_kg_price` and `per_piece_price` side by side and always renders them identically regardless of `unit_type`. A piece-sold product shows "Per Kg: —" and "Per Piece: ₱25" — the "Per Kg" label shouldn't appear at all.

Missing from card display per PRD-US-01:
- "from ₱X" cross-channel minimum SRP
- "Manual Price" badge for products with only fallback prices
- "No Price" indicator for products with neither SRP nor fallback
- Active/inactive visual distinction

#### Dead composables — Three unreachable composables in the file

| Composable | Defined | Called | Notes |
|------------|---------|--------|-------|
| `EditPriceDialog` | Line 421 | Never | Has only per_kg and per_piece fields — no discounted |
| `ProductPriceInputs` | Line 493 | Never | Helper for two price fields |
| `PriceHistoryCard` | Line 534 | Never | Has a `LazyColumn` inside an `AlertDialog` — would be broken anyway |

#### `FilterDialog` — Incomplete filters

Existing filters: search query (by name/description — missing ID), show-out-of-stock checkbox, sort by name or price. Missing filters required by PRD-US-01 AC#7: unit type, category, active status.

---

### 7. `DatabaseInitializer.kt` — Structural problems

**Two initialization paths with different schemas**

The `callback` object (lines 29–66) manually executes `CREATE TABLE` SQL with a v1-era schema:
```sql
CREATE TABLE IF NOT EXISTS products (
    product_id TEXT PRIMARY KEY NOT NULL,
    product_name TEXT NOT NULL,
    product_description TEXT NOT NULL,
    unit_type TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1
    -- No category, no default_piece_count
)
```

Room's `@Database` with `fallbackToDestructiveMigration()` will re-create the tables from the entity definitions, so these manual `CREATE TABLE` calls will either no-op (tables already exist) or conflict. The callback is likely never triggered because the database is created by Room before the callback fires for the schema. This code is misleading — it suggests a schema that no longer matches the entities.

**`reinitializeDatabase()` has a race condition**

```kotlin
FarmDatabase.getDatabase(context).close()
FarmDatabase.clearInstance()
// ← Any concurrent call to getDatabase() here gets null or a closed instance
val database = FarmDatabase.getDatabase(context)
populateDatabase()
```

Between `clearInstance()` and the next `getDatabase()` call, any in-flight coroutine that touches the database will crash or get an unexpected instance. This is low-risk in practice (only triggered by the admin explicitly, and the UI shows a spinner) but is architecturally fragile.

**`populateDatabase()` seeds only 2 products**

The method creates 2 hardcoded products (Tomatoes, Lettuce) and 2 customers. The user story PRD-US-08 says "reload the default product list from the app's built-in data" — there are seed JSON files in `assets/data/products.json` and `assets/data/product_prices.json` which `DatabaseInitializer` parses via `Gson` (imports `ProductList`, `CustomerList`, etc.) but the current `populateDatabase()` does not use them. The JSON assets are declared as dependencies but never used in the active code path.

---

### 8. Cross-Epic Impact — `OrderPricingResolver.kt`

**Implicit "discounted = reseller" assumption is undocumented**

```kotlin
fun fallbackUnitPrice(productPrice: ProductPrice?, channel: String, isPerKg: Boolean): Double {
    val useDiscounted = SalesChannel.normalize(channel) == SalesChannel.RESELLER
    return if (isPerKg) {
        when {
            useDiscounted && pp.discounted_per_kg_price != null -> pp.discounted_per_kg_price!!
            else -> pp.per_kg_price ?: 0.0
        }
    } ...
}
```

`OrderPricingResolver` repurposes `discounted_per_kg_price` as the reseller fallback price. This is a reasonable bridge interpretation but it's not written in any user story. When PRD-US-06 is rebuilt, whoever implements it needs to know this linkage exists or the fallback pricing logic will break silently.

**`productSupportsDualUnit()` has a fragile OR-chain**

```kotlin
return (hasKg && hasPc) || (srpKg && srpPc) || (hasKg && srpPc) || (hasPc && srpKg)
```

This allows mixing: product has a manual kg fallback price AND a computed per-piece SRP → dual unit toggle appears. This could show the toggle for a product that is only physically sold in one unit if the data is inconsistent. The correct source of truth for dual-unit support is `product.unit_type` — not price availability.

---

## Summary of All Gaps

### Critical (blocks correct operation)

| # | Issue | File | Story |
|---|-------|------|-------|
| C1 | `unit_type = ""` hardcoded on every new product | `ManageProductsScreen.kt` `AddProductDialog` | PRD-US-02 AC#1 |
| C2 | Screen uses its own repository, ignoring ViewModel flows — list may not update after mutations | `ManageProductsScreen.kt` | All |
| C3 | Saving an edit always writes a new price row, even if only name changed | `ManageProductsScreen.kt` `EditProductDialog` | PRD-US-03 |
| C4 | Price display reads `product_prices` (fallback table), not active acquisition SRP | `ManageProductsScreen.kt` `ProductCard` | PRD-US-01 AC#2, AC#5 |

### High (missing required functionality)

| # | Issue | File | Story |
|---|-------|------|-------|
| H1 | No unit type picker in Add or Edit | `ManageProductsScreen.kt` | PRD-US-02 AC#1, PRD-US-03 |
| H2 | No category field in Add or Edit | `ManageProductsScreen.kt` | PRD-US-02 AC#1 |
| H3 | No default piece count field in Add or Edit | `ManageProductsScreen.kt` | PRD-US-02 AC#1 |
| H4 | Delete action scaffolded but unreachable — `showDeleteDialog` never set | `ManageProductsScreen.kt` | PRD-US-05 |
| H5 | Deactivate/reactivate action absent entirely | `ManageProductsScreen.kt` | PRD-US-04 |
| H6 | Reinitialize button missing — method exists in ViewModel, no button calls it | `ManageProductsScreen.kt` | PRD-US-08 |
| H7 | Price history screen is dead code — `PriceHistoryCard` never called | `ManageProductsScreen.kt` | PRD-US-07 |
| H8 | Price fields should not be in Add/Edit — price entry is a separate action (PRD-US-06) | `ManageProductsScreen.kt` | PRD-US-02, PRD-US-03 |
| H9 | Save blocked unless price entered — should save without price | `ManageProductsScreen.kt` | PRD-US-02 AC#1 |
| H10 | Missing "Manual Price" badge and "No Price" indicator on product card | `ManageProductsScreen.kt` | PRD-US-01 AC#3, AC#4 |
| H11 | Missing "from ₱X" cross-channel minimum SRP on product card | `ManageProductsScreen.kt` | PRD-US-01 AC#2 |

### Medium (incorrect or incomplete behaviour)

| # | Issue | File | Story |
|---|-------|------|-------|
| M1 | Search does not cover `product_id` | `ProductRepository.kt` | PRD-US-01 AC#6 |
| M2 | Filter missing unit type, category, active status | `ProductFilters.kt`, `ProductRepository.kt`, `ManageProductsScreen.kt` | PRD-US-01 AC#7 |
| M3 | `showOutOfStock` filter naming inverted vs story intent | `ProductFilters.kt` | PRD-US-01 AC#7 |
| M4 | `insertProductPrice` and `updateProductPrice` both insert new rows — one is redundant | `ProductRepository.kt` | — |
| M5 | `DatabaseInitializer.callback` creates tables with v1 schema, not v4 — misleading dead code | `DatabaseInitializer.kt` | PRD-US-08 |
| M6 | `reinitializeDatabase()` does not load from JSON assets — seeds only 2 hardcoded products | `DatabaseInitializer.kt` | PRD-US-08 |
| M7 | ViewModel not annotated `@HiltViewModel`, creates its own DB instances | `ManageProductsViewModel.kt` | — |
| M8 | `discounted_per_kg/piece_price` repurposed as reseller fallback — undocumented assumption | `OrderPricingResolver.kt` | PRD-US-06 |

### Low (code quality / polish)

| # | Issue | File |
|---|-------|------|
| L1 | Three dead composables: `EditPriceDialog`, `ProductPriceInputs`, `PriceHistoryCard` | `ManageProductsScreen.kt` |
| L2 | Product card renders "Per Kg: —" for piece-only products — label should not appear | `ManageProductsScreen.kt` |
| L3 | `product_description` field commented out on `ProductCard` — unclear if intentional | `ManageProductsScreen.kt` |
| L4 | `getPriceHistory(productId)` DAO query exists but is never used | `ProductPriceDao.kt` |
| L5 | `race condition in reinitializeDatabase()` between `clearInstance()` and `getDatabase()` | `DatabaseInitializer.kt` |
| L6 | `productSupportsDualUnit()` uses price availability to infer unit type instead of `unit_type` field | `TakeOrderViewModel.kt` |

---

## Rebuild Instructions — Priority Order

### Step 1 — Fix the screen architecture (unblocks everything else)

Convert `ManageProductsScreen` to use the ViewModel's `products` and `productPrices` StateFlows instead of its own repository instance. Wire `viewModel` correctly:

```kotlin
// Remove these from the screen:
val database = remember { FarmDatabase.getDatabase(context) }
val repository = remember { ProductRepository(...) }
var products by remember { mutableStateOf(...) }
LaunchedEffect(Unit) { repository.getAllProducts().collect { ... } }

// Replace with:
val products by viewModel.products.collectAsState()
val productPrices by viewModel.productPrices.collectAsState()
```

Convert ViewModel to `@HiltViewModel` with `@Inject constructor`.

### Step 2 — Fix Add Product dialog

Remove all price fields. Add required fields: unit type picker (`kg` / `piece` / `both`), category dropdown (optional), default piece count (numeric, shown only when unit type includes piece). Remove price-gated save condition.

### Step 3 — Fix Edit Product dialog

Remove all price fields. Add unit type picker, category, default piece count, active/inactive toggle. Decouple product update from price insert — `updateProduct()` only, no `updateProductPrice()`.

### Step 4 — Wire Delete and Deactivate

Set `showDeleteDialog` from a swipe action or long-press on the product card. Call `viewModel.deleteProduct()` on confirm. Handle FK constraint exception (product has orders/acquisitions) with a user message.

Add a toggle on the product card or edit screen for `is_active`.

### Step 5 — Wire Reinitialize

Add `Icons.Default.Refresh` to `TopAppBar`. Confirmation dialog → `viewModel.reinitializeDatabase()`. Fix `populateDatabase()` to load from `assets/data/products.json` using the Gson parser that is already imported.

### Step 6 — Set Manual Fallback Price (separate action — PRD-US-06)

New "Set Fallback Price" bottom sheet or full-screen form accessible from the product detail/edit screen. Fields: per-kg price (optional), per-piece price (optional). No discounted fields. Save calls `repository.insertProductPrice()`. Clearly label this as "Manual fallback — used when no acquisition SRP exists."

### Step 7 — Fix product card price display (Phase 3, after SRP pipeline)

Change `ProductCard` to read from `AcquisitionRepository.observeAllActiveSrps()` mapped by product ID. Apply badge logic:
- Active SRP exists → show "from ₱X/kg", no badge
- Only manual fallback → show fallback price, amber "Manual Price" chip  
- No price at all → show grey "No Price" chip

### Step 8 — Fix filters

Add `unitType: String?`, `category: String?`, `activeStatus: ActiveStatus` (enum: ALL / ACTIVE_ONLY / INACTIVE_ONLY) to `ProductFilters`. Update `getFilteredProducts()` to apply them. Update `FilterDialog` UI to expose all three. Add `product_id` to the search predicate.

### Step 9 — Price history screen (Phase 3)

Wire `ProductPriceDao.getPriceHistory()` through a new `getProductPriceHistory(productId)` repository method. Build a new screen (not a card inside a dialog) showing each entry with: source type chip (Computed/Manual), date, per-channel SRP values or fallback values, preset reference for computed entries.

---

## Files to Change

| File | Change | Priority |
|------|--------|----------|
| `ManageProductsScreen.kt` | Remove local repository; use ViewModel flows; fix Add/Edit dialogs; wire delete/deactivate/reinitialize; remove dead composables | C / Step 1–4 |
| `ManageProductsViewModel.kt` | Add `@HiltViewModel`; add deactivate action; wire reinitialize button | C / Step 1 |
| `ProductFilters.kt` | Add unitType, category, activeStatus fields | M / Step 8 |
| `ProductRepository.kt` | Fix `getFilteredProducts()` search + filters; rename/remove duplicate insertProductPrice/updateProductPrice | M / Step 8 |
| `DatabaseInitializer.kt` | Remove v1 schema from callback; fix `populateDatabase()` to use JSON assets | M / Step 5 |
| `data/model/Product.kt` | No changes needed — already complete |  |
| `data/local/entity/ProductEntity.kt` | No changes needed — already complete |  |
| `data/local/dao/ProductDao.kt` | No changes needed for now |  |
| `data/local/dao/ProductPriceDao.kt` | No changes needed — `getPriceHistory()` already exists |  |
| New: `ProductPriceHistoryScreen.kt` | Full-screen price history | PRD-US-07 / Step 9 |
| `OrderPricingResolver.kt` | Document the discounted=reseller assumption; consider removing discounted fields in favour of explicit reseller fallback | M8 |
