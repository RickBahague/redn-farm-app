# Farm App — Design Document

**App name:** Yong & Eyo's Farm
**Package:** `com.redn.farm`
**Platform:** Android (minSdk 25, targetSdk 34)
**Language:** Kotlin + Jetpack Compose (Material3)

---

## 0. Product vision & scope

Define a **collection of functions** for **Farm-to-Table Entity**, a business selling fresh farm produce and related products. This document is the single source of truth for **scope, behavior, data, and constraints** so that later work can produce a concrete architecture (tech stack, modules, APIs, storage layout) without re-deriving intent from chat history. **Architecture agents:** produce the deliverables listed in **§3** as a companion architecture / ADR set.

The product is an adroid app, which will also have a companion web app based on drupal in a separate product.

---

## 1. Purpose

A point-of-sale and operations management app for a farm-to-table business. It covers:

- Recording inventory acquisitions from various locations
- Managing products and pricing (with price history and discounted rates)
- Taking and tracking customer orders
- Logging farm operations (planting, harvesting, pesticide, etc.)
- Managing employees and their compensation (with cash advances / liquidations)
- Recording **sales remittances** (store assistant) and **disbursements** (funds received by purchasing) on the shared **Remittance** screen — **USER_STORIES.md** Epic 8 (**DISB-US-01–03**)
- **End of day (day close):** daily sales snapshot, inventory reconciliation vs. physical count, cash reconciliation, COGS/margin, thermal EOD slip, day-close history, outstanding inventory report (see **§14** and **USER_STORIES.md** Epic 12 — EOD-US-01–10)
- Exporting all data to CSV files
- Login / session management with hashed passwords

---
## 3. Actors and User Stories
- See **USER_STORIES.md** (includes **Epic 12 — End of Day Operations** EOD-US-01–10).

---

## 3. Architecture

**Pattern:** MVVM (Model–View–ViewModel)
**DI:** Dagger Hilt (`@HiltAndroidApp` on `FarmApplication`, `@AndroidEntryPoint` on `MainActivity`)
**Database:** Room (SQLite), single `FarmDatabase` singleton
**Async:** Kotlin Coroutines + Flow
**UI:** Jetpack Compose + Navigation Compose

```
MainActivity
  └── SessionChecker (wraps NavGraph, redirects to Login if session expired)
        └── NavGraph (all route definitions in one file)
              └── Screens (each screen has one ViewModel)
```

---

## 4. Navigation

Defined in `navigation/NavGraph.kt` as a sealed class `Screen`:

| Route | Screen |
|---|---|
| `login` | Login |
| `database_migration` | Database Setup |
| `main` | Main Dashboard |
| `orders` | Take Order |
| `active_srps` | Active SRPs (price list) |
| `order_history` | Order History |
| `order_detail/{orderId}` | Order Detail |
| `edit_order/{orderId}` | Edit Order |
| `products` | Manage Products |
| `product_form/{productId}` | Product Add/Edit |
| `customers` | Manage Customers |
| `customer_form/{customerId}` | Customer Add/Edit |
| `acquire` | Acquire Produce (Inventory) |
| `acquisition_form/{acquisitionId}` | Acquisition Add/Edit |
| `remittance` | Remittances & disbursements (single screen; **USER_STORIES** Epic 8 **DISB-US-01–03**) |
| `remittance_add_edit/{remittanceId}` | Remittance / Disbursement Form |
| `employees` | Manage Employees (Green Crew) |
| `employee_add_edit/{employeeId}` | Employee Add/Edit |
| `employee_payments/{employeeId}/{employeeName}` | Employee Payments |
| `employee_payment_form/{employeeId}/{employeeName}/{paymentId}` | Payment Add/Edit |
| `farm_ops` | Farm Operations |
| `farm_op_form/{operationId}` | Farm Operation Add/Edit |
| `farm_ops_history` | Farm Ops History |
| `export` | Export / Data Management |
| `about` | About |
| `profile` | Profile |
| `change_password` | Change Password |
| `user_management` | User Management (admin) |
| `settings` | Settings (admin-capable roles) |
| `pricing_presets` | Pricing Presets Home |
| `preset_history` | Preset History |
| `pricing_preset_editor/{sourcePresetId}` | Preset Editor |
| `preset_detail/{presetId}` | Preset Detail |
| `preset_activation_preview/{presetId}` | Preset Activation Preview |
| `day_close/{businessDateMillis}` | Day Close (**Epic 12**) |
| `day_close_history` | Day Close History |
| `outstanding_inventory` | Outstanding Inventory |

Start destination is `login`. After login succeeds, the back stack is cleared so back-press cannot return to the login screen. Same for logout — the entire back stack is cleared with `popUpTo(0) { inclusive = true }`.

**Session guard:** `SessionChecker` is a Composable wrapper around `NavGraph`. It observes `LoginViewModel.loginState`; if it reverts to `Initial` (i.e., session cleared) from any screen, it forces navigation to `login`.

---

## 5. Authentication & Session

### Authentication flow
1. `LoginViewModel.login()` queries `UserDao.getUserByUsername(username)` on the IO dispatcher.
2. Password is verified via `PasswordManager.verifyPassword()` (PBKDF2WithHmacSHA1, 65,536 iterations, 16-byte random salt, 128-bit key; salt prepended to hash, Base64-encoded).
3. On success, `SessionManager.createSession(username)` writes username + login timestamp + `is_logged_in=true` to `SharedPreferences` (`farm_session`).
4. On logout (`MainViewModel.logout()`), `SessionManager.endSession()` clears the prefs.

### Default users (created at DB init)
| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `user` | `user123` | USER |

Users are created in `FarmDatabase`'s `onOpen` / `onCreate` callback if they don't already exist. Role is stored as a plain string in `users.role`. Dashboard and several flows already gate destinations using **`security/Rbac.kt`** and **`SessionManager`** (normalized role strings **`ADMIN`**, **`STORE_ASSISTANT`**, **`PURCHASING`**, etc.). **Epic 12 (EOD)** adds further gates per **USER_STORIES.md** (e.g. Close Day for store assistant + admin; Outstanding Inventory for admin + purchasing; Day Close History and un-finalize for admin).

---

## 6. Database Design

### `FarmDatabase` (Room)

Located at `data/local/FarmDatabase.kt`. The **`@Database(version = …)`** value is the source of truth (currently **10**); this document is updated when the schema bumps. Singleton accessed via `FarmDatabase.getDatabase(context)`. A `clearInstance()` method exists for reinitialization scenarios. Development builds use **`fallbackToDestructiveMigration()`**; see **`docs/schema_evolution.sql`** and **`USER_STORIES.md` SYS-US-04**.

**Type converters registered:**
- `EnumConverters` — `CustomerType`, `AcquisitionLocation`, `FarmOperationType` (and related enums) stored as their enum `name` strings

### Tables and relationships

```
products (product_id TEXT PK)
  ├── product_prices (product_id FK → CASCADE delete)
  ├── order_items   (product_id FK → RESTRICT delete)
  ├── acquisitions  (product_id FK → RESTRICT delete)
  └── farm_operations (product_id FK → SET NULL on delete)

customers (customer_id INT PK, autoincrement)
  └── orders (customer_id FK → RESTRICT delete)

orders (order_id INT PK, autoincrement)
  └── order_items (order_id FK → CASCADE delete)

day_closes (close_id INT PK, autoincrement)
  ├── day_close_inventory (composite PK or id + unique (close_id, product_id))
  └── day_close_audit (audit_id PK; optional FK close_id)

employees (employee_id INT PK, autoincrement)
  └── employee_payments (employee_id FK → RESTRICT delete)

remittances (remittance_id INT PK, autoincrement)

pricing_presets (preset_id TEXT PK)
preset_activation_log (log_id PK; append-only activation events)

users (user_id INT PK, autoincrement; unique index on username)
```

### Schema details

| Table | Key fields |
|---|---|
| `products` | `product_id` (String), `product_name`, `product_description`, `unit_type`, `category?`, `default_piece_count?`, `is_active` |
| `product_prices` | `price_id` (autoincr), `product_id`, `per_kg_price?`, `per_piece_price?`, `discounted_*`, `date_created` (epoch **ms**) |
| `customers` | `customer_id`, `firstname`, `lastname`, `contact`, `customer_type` (enum), address fields, `date_created/updated` (epoch **ms**) |
| `orders` | `order_id`, `customer_id`, **`channel`**, `total_amount`, `order_date`, `order_update_date`, `is_paid`, `is_delivered` |
| `order_items` | `id`, `order_id`, `product_id`, `quantity`, `price_per_unit`, `is_per_kg`, `total_price` |
| `employees` | `employee_id`, `firstname`, `lastname`, `contact`, `date_created/updated` |
| `employee_payments` | `payment_id`, `employee_id`, `amount`, `cash_advance_amount?`, `liquidated_amount?`, `date_paid`, `signature`, `received_date?` |
| `farm_operations` | `operation_id`, `operation_type` (enum), `operation_date`, `details`, `area`, `weather_condition`, `personnel`, `product_id?`, `product_name` |
| `acquisitions` | `acquisition_id`, `product_id`, `product_name`, `quantity`, `price_per_unit`, `total_amount`, `is_per_kg`, `piece_count?`, `preset_ref?`, `channels_snapshot_json`, SRP columns, `spoilage_kg?`, **`srp_custom_override`**, `date_acquired`, `created_at`, `location` (enum) — INV-US-01 / INV-US-05 / **PricingReference.md** |
| `remittances` | `remittance_id`, `amount`, `date`, `remarks`, `date_updated`, **`entry_type`** (`REMITTANCE` \| `DISBURSEMENT`) — Epic 8 |
| `users` | `user_id`, `username` (unique), `password_hash`, `full_name`, `role`, `is_active` |
| `pricing_presets` | `preset_id`, `preset_name`, `saved_at`, `saved_by`, `is_active`, `activated_at/by`, `spoilage_rate`, `additional_cost_per_kg`, `hauling_weight_kg`, `hauling_fees_json`, **`channels_json`**, **`categories_json`** |
| `preset_activation_log` | `log_id`, `activated_at`, `activated_by`, `preset_id_activated`, `preset_id_deactivated?` |
| `day_closes` | EOD snapshot header — see **§14.2** (`business_date`, `is_finalized`, cash / margin fields, etc.) |
| `day_close_inventory` | Per-product EOD row — see **§14.2** |
| `day_close_audit` | EOD audit trail — see **§14.13** |

### Migrations

Build phase: **`fallbackToDestructiveMigration()`** on version bumps except legacy **`MIGRATION_1_2`** / **`MIGRATION_2_3`**. Canonical DDL snapshot: **`docs/schema_evolution.sql`** **VERSION 10** (must match KSP **`FarmDatabase_Impl.createAllTables`**). See **USER_STORIES.md** SYS-US-04.

### Price history

`ProductRepository.updateProductPrice()` always **inserts** a new `ProductPriceEntity` row (never updates in place). The latest price is retrieved by `MAX(date_created)` per product. This preserves a full price history.

---

## 7. Data Layer

### Domain models (`data/model/`)
Plain Kotlin data classes decoupled from Room entities. Repositories map between them.

Key enums:
- `CustomerType` — REGULAR, RETAIL, WHOLESALE
- `AcquisitionLocation` — FARM, MARKET, SUPPLIER, OTHER
- `FarmOperationType` — SOWING, HARVESTING, PESTICIDE_APPLICATION, FERTILIZER_APPLICATION, IRRIGATION, WEEDING, PRUNING, OTHER

`CartItem` is a transient model used only in the ordering flow (never persisted directly).

### DAOs (`data/local/dao/`)
All return `Flow<>` for live data, with `suspend fun` for mutations. Notable patterns:
- `OrderDao.createOrder()` is a `@Transaction` that inserts order then items with the generated ID
- `OrderDao.deleteOrderAndItems()` checks `is_paid == false` before deleting
- `ProductPriceDao.getLatestPrices()` uses a subquery to get one price row per product
- `EmployeePaymentDao` and `OrderDao` embed joined columns (`EmployeePaymentWithEmployee`, `OrderWithDetails`, `OrderItemWithProduct`) using `@Embedded` + `@ColumnInfo`

### Repositories (`data/repository/`)
Thin mapping layer — entity ↔ domain model. No business logic beyond mapping and timestamp management.

**Inconsistency to be aware of:** Some repositories (`FarmOperationRepository`, `ProductRepository`, `AcquisitionRepository`) are in `RepositoryModule` and use Hilt injection; others (`OrderRepository`, `CustomerRepository`, `EmployeeRepository`, `EmployeePaymentRepository`, `RemittanceRepository`) are instantiated manually in ViewModels via `FarmDatabase.getDatabase(application)`.

---

## 8. ViewModel Patterns

Two patterns coexist:

**Hilt-injected ViewModels** (used by Hilt-aware screens):
```kotlin
@HiltViewModel
class FarmOperationsViewModel @Inject constructor(
    private val repository: FarmOperationRepository,
    ...
) : ViewModel()
```

**Manual factory ViewModels** (majority of screens):
```kotlin
class TakeOrderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FarmDatabase.getDatabase(application)
    // repositories instantiated here

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory { ... }
    }
}
```
Screens using manual factories pass `viewModel(factory = XyzViewModel.Factory)`.

All ViewModels use `StateFlow` exposed as `asStateFlow()`, collected in Composables via `collectAsState()`. Filtering/search is done reactively using `combine()` of multiple flows.

---

## 9. Feature Details

### Orders
- `TakeOrderScreen` → select customer → add products to cart → place order
- Cart state lives in `TakeOrderViewModel` (`_cartItems: MutableStateFlow<List<CartItem>>`)
- Customer search supports lookup by name, contact, or order number
- `addToCart()` supports regular or discounted pricing via `useDiscountedPrice` flag
- `OrderHistoryScreen` supports search by customer name/contact/order ID and date range filtering
- Orders can be edited (items replaced), paid status toggled, delivery status toggled, and date changed
- Deletion is only allowed for unpaid orders (enforced at DAO level)

### Products & Pricing
- Products have a string `product_id` (not autoincrement) — must be unique, e.g. `"PROD001"`
- `unit_type` is a free-text string (e.g., "kg", "piece")
- Each product can have both per-kg and per-piece prices simultaneously
- Setting a new price inserts a new row; old prices are retained as history
- Discounted prices are optional fields added in DB v3

### Employee Payments
- Payments record `amount` (gross wage), optional `cash_advance_amount`, optional `liquidated_amount`, and `signature` (typed name **or** Base64-encoded drawn signature from `SignatureCanvasField`)
- **Net pay (form + each history row):** `EmployeePayment.netPayAmount()` = `amount + cash_advance_amount` (null advance as `0`). **Liquidated** stored/shown for audit and outstanding balance only — EMP-US-05
- Add/edit: full-screen `PaymentFormScreen` on route `employee_payment_form/...`; list remains `EmployeePaymentScreen` → `employee_payments/...`
- **History (EMP-US-06):** `EmployeePaymentScreen` — list, period filter (All Time / Today / This Week / This Month / Last Month / Last 3 Months / Last 6 Months / Custom Range), **period summary** (totals for filtered rows), **lifetime outstanding advance** (all-time, ignores filter).
- `received_date` is nullable (payment may be scheduled but not yet received)

### Farm Operations
- Operations are filtered in-memory by type, date range, and text search (details + personnel fields)
- Optional link to a product (`product_id` nullable, SET NULL on delete)

### Acquisitions
- Records incoming inventory with source location (enum), price per unit, **`is_per_kg`**, optional **`piece_count`** (pieces per kg — CLARIF **Estimated Qty per Kg**). **Per kg:** **`quantity`** is kg; preset spoilage applies in SRP. **Per piece:** **`quantity`** is total pieces; engine derives kg **`quantity / piece_count`**; **CLARIF-01:** spoilage **not** applied in per-piece SRP (**`Q_sell = Q`** — **PricingReference.md** §5.1.1); preset **`spoilage_rate`** remains on the snapshot for audit. **Additional cost per kg** applies on the derived mass basis (**INV-US-05**, **§4.3.1**).

### Remittances & disbursements
- **`RemittanceScreen`** / **`RemittanceFormScreen`** (same routes as today) show **both** transaction kinds (**USER_STORIES.md** Epic 8): **`REMITTANCE`** = sales proceeds handed in by the **store assistant**; **`DISBURSEMENT`** = money **received by purchasing** (e.g. float, refunds). List/filter, print slip title, and **EOD** cash math distinguish **Remitted today** (REMITTANCE only) from disbursements (**DESIGN.md** §14.3 / §14.12).

### Export / Data Management
`ExportScreen` / `ExportViewModel` provides per-table CSV export and per-table truncation (data wipe). It also has buttons to generate sample data (`DatabasePopulator`).

Two export mechanisms exist:
1. `CsvExportService` — per-entity CSV files written to `getExternalFilesDir(null)/exports/`
2. `DatabaseExporter` — full JSON dump of all tables to the same directory, with `machine_id` (Android ID) appended to every row

The `DatabaseMigrationScreen` is shown if it is somehow reached; it checks for an existing DB file and offers to export before proceeding.

---

## 10. Printing

`PrinterUtils` (in `utils/`) supports two modes:
- **Standard Android print** (`printText`): renders content in a WebView then calls `PrintManager.print()` with A4 media size
- **Sunmi printer** (`printMessage`): uses the Sunmi SDK (`com.sunmi:printerlibrary:1.0.23`) to print directly to a built-in Sunmi POS printer via `SunmiPrinterService`. Optional **`alignment`** argument: `0` = left (58mm slips built in **`ThermalPrintBuilders.kt`**), default `1` = centre.

The Sunmi printer connection is managed as a coroutine-suspended bind (`suspendCancellableCoroutine`). Slip templates and triggers are documented in **`docs/printing.md`** (PRN-01–07).

---

## 11. Utilities

| Class | Location | Purpose |
|---|---|---|
| `CurrencyFormatter` | `utils/` | Formats doubles as currency strings |
| `DeviceUtils` | `utils/` | Returns `Settings.Secure.ANDROID_ID` as device identifier |
| `PrinterUtils` | `utils/` | Android print + Sunmi thermal printer (`printMessage`, alignment) |
| `ThermalPrintBuilders` | `utils/` | 32-column thermal strings: orders, payroll, single + **batch** acquisition reports, SRP list, etc. |
| `DatabasePopulator` | `data/util/` | Generates sample customers, products, acquisitions |
| `SampleDataGenerator` | `data/util/` | (Additional sample data generation utilities) |
| `DatePicker` | `ui/components/` | Custom date picker Composable |
| `SessionChecker` | `ui/components/` | Composable that guards all screens against expired sessions |

---

## 12. Assets & Seed Data

`app/src/main/assets/`:
- `data/products.json` — seed product list (parsed into `ProductList`)
- `data/product_prices.json` — seed prices (parsed into `ProductPriceList`)
- `data/customers.json` — seed customer list (parsed into `CustomerList`)
- `database/farm.db` — pre-populated SQLite database file

`DatabaseInitializer` can drop and recreate the database, populating it with a hardcoded set of sample products/prices/customers (Tomatoes, Lettuce). The JSON assets and the `DatabaseInitializer` hardcoded data appear to be two separate mechanisms — the hardcoded data in `populateDatabase()` is what's actually used.

---

## 13. Known Design Inconsistencies

1. **Mixed DI approach**: Most ViewModels bypass Hilt and instantiate repositories manually, while `FarmOperationsViewModel` uses `@HiltViewModel`. This causes duplicate repository instances for the same database connection.

2. **`DatabaseMigrationScreen` is unreachable**: The route `database_migration` exists in `NavGraph` and `Screen`, but no navigation action in any other screen navigates to it. It appears to be a leftover feature.

3. **`DatabaseInitializer` vs. seed assets**: `DatabaseInitializer` has its own hardcoded seed data (overriding any JSON assets). The JSON files in `assets/data/` are parsed as models (`ProductList`, `CustomerList`) but `DatabaseInitializer.populateDatabase()` does not read them.

4. ~~**`Converters.kt`**~~ — removed; it was never registered on **`FarmDatabase`** (**DI-04** / **BUG-ARC-09**). Active Room converters live under **`data/local/converters/`**.

5. **Date/time storage inconsistency**: Domain paths for farm ops, order-history filters, and acquire flows now standardize on **epoch millis** where models cross layers; some entities or UI helpers may still differ — see **`BUG-ARC-09`** and **`BACKLOG.md`** **DI-05**.

---

## 14. End of day (Epic 12 — EOD-US-01–10)

This section condenses **USER_STORIES.md** Epic 12 into **implementation-facing** decisions: schema, queries, UI entry points, and shared logic. Full acceptance criteria remain in **USER_STORIES.md**.

### 14.1 Scope and principles

- **Day close** is a **snapshot + reconciliation** flow. It does not delete or lock `orders`, `acquisitions`, or `order_items`. Unpaid orders stay open (EOD-US-08).
- **Business date** = calendar day in the device default zone, stored as **epoch millis at local start-of-day** (reuse **`MillisDateRange.startOfDayMillis`** consistently with acquisitions / farm ops).
- **Channel** breakdowns use existing order field **`orders.channel`** normalized via **`SalesChannel`** (`online`, `reseller`, `offline` — **EOD-US-02**).
- **Weighted average cost (WAC)** per product: `SUM(acquisitions.total_amount) / SUM(acquisitions.quantity)` over all acquisitions for that product (same unit basis as **`is_per_kg`** on the acquisition — implementation must treat kg vs piece consistently per product; see **§14.5**).
- **Draft vs finalized:** `day_closes.is_finalized = false` allows saving partial inventory counts and editing; **`true`** freezes the header row and child inventory rows. **Admin un-finalize** (EOD-US-06) sets back to draft and should append an **audit record** — see **`day_close_audit`** (**§14.13**).
- **Live vs snapshot (implementation rule):** While `is_finalized = false`, sales, COGS, channel breakdowns, and warning lists are **computed from live queries** on each load. **On finalize**, copy all figures that must appear unchanged on **Day Close History** / detail / thermal slip into **`day_closes`** and **`day_close_inventory`** (and optional receivables snapshot fields in **§14.10**). After finalize, **detail and re-print** read persisted snapshot fields, not recomputed history (avoids drift if orders are edited later). Corrections: admin **un-finalize**, fix, re-finalize.
- **RBAC:** Extend **`Rbac`** with destination sets for **Day Close**, **Day Close History**, **Outstanding Inventory**, matching Epic 12 actors. Use the same pattern as existing dashboard visibility (`MainScreen` / `Rbac.canAccessDestination`). **§14.10** locks section-level behavior.

### 14.2 Room: `day_closes` and `day_close_inventory`

Align entity fields with **USER_STORIES.md** §Epic 12 intro; add columns needed by EOD-US-04 / EOD-US-07 as needed for **persisted** draft and finalized snapshots.

**`day_closes` (suggested)**

| Column | Type | Notes |
|--------|------|--------|
| `close_id` | `INTEGER` PK auto | |
| `business_date` | `INTEGER` | Start-of-day millis; **unique** index per business day (one close record per calendar day, draft or final). |
| `closed_by` | `TEXT` | Username; set on finalize. |
| `closed_at` | `INTEGER?` | Epoch millis; set on finalize. |
| `is_finalized` | `INTEGER` (bool) | |
| `total_orders` | `INTEGER` | Snapshot or recomputed in UI until finalize — recommend **snapshot on finalize** for stable history. |
| `total_sales_amount` | `REAL` | |
| `total_collected` | `REAL` | Paid orders on business date (per EOD-US-07 / EOD-US-02). |
| `snapshot_unpaid_today_count` | `INTEGER?` | Count of orders with `order_date` on business date and `is_paid = false` — **EOD-US-02**; set on finalize. |
| `snapshot_unpaid_today_amount` | `REAL?` | Sum of `orders.total_amount` for those rows — **EOD-US-02**; set on finalize. |
| `snapshot_all_unpaid_count` | `INTEGER?` | Count of all orders with `is_paid = false` at finalize time — **EOD-US-08** slip / audit. |
| `snapshot_all_unpaid_amount` | `REAL?` | Sum of `total_amount` for those orders — **EOD-US-08**. |
| `total_acquisition_cost` | `REAL?` | Optional; if present, prefer **cumulative all-time** `SUM(acquisitions.total_amount)` at finalize (**EOD-US-07** cumulative block) — not “today’s purchase spend” unless explicitly labeled in UI. |
| `notes` | `TEXT?` | Free text; wages “due but not paid” (EOD-US-09). |
| **Cash (EOD-US-04)** | | Persist user-entered values so draft survives rotation. |
| `cash_on_hand` | `REAL?` | Optional physical count. |
| `cash_reconciliation_remarks` | `TEXT?` | Required before finalize when discrepancy rules in EOD-US-04 fire. |
| **Margin snapshot (optional but useful for history)** | | `gross_revenue_today`, `collected_revenue_today`, `total_cogs_today`, `gross_margin_amount`, `gross_margin_percent` — can be denormalized at finalize. |

Indexes: **unique(`business_date`)**; optional `INDEX(is_finalized, business_date DESC)` for history list.

**`day_close_inventory` (suggested)**

| Column | Type | Notes |
|--------|------|--------|
| `id` | `INTEGER` PK auto | Or composite PK (`close_id`, `product_id`). |
| `close_id` | `INTEGER` FK → `day_closes` CASCADE | |
| `product_id` | `TEXT` FK → `products` optional | Match project FK style (`RESTRICT` if preferred). |
| `product_name` | `TEXT` | Denormalized for stable print/history. |
| `total_acquired_all_time` | `REAL` | |
| `total_sold_through_close_date` | `REAL` | |
| `prior_posted_variance` | `REAL` | Sum of `variance_qty` from **prior** finalized closes. |
| `adjusted_theoretical_remaining` | `REAL` | acquired − sold − prior variance (EOD-US-03). |
| `sold_this_close_date` | `REAL` | |
| `actual_remaining` | `REAL?` | Physical count; null = not counted. |
| `variance_qty` | `REAL?` | theoretical − actual; null if not counted. |
| `weighted_avg_cost_per_unit` | `REAL` | |
| `variance_cost` | `REAL?` | variance × WAC. |

**Finalize behavior:** Upsert one row per product that participates in the close (non-zero adjusted theoretical by default; “show zero” toggle may still persist rows — product decision). Rows **not counted** exclude variance from spoilage totals (EOD-US-03).

**Bump `FarmDatabase` version**; follow existing project practice (**`fallbackToDestructiveMigration()`** during build, **`schema_evolution.sql`** + JSON schema when policy requires incremental migrations).

### 14.3 Aggregation and query layer

Implement a **`DayCloseRepository`** (or split **`DayCloseAggregator` + repository**) that:

1. **Sales summary (EOD-US-02):** Orders whose `order_date` falls on the same calendar day as `business_date`, using **`MillisDateRange.startOfDayMillis(business_date)`** … **`MillisDateRange.endOfDayMillis(business_date)`** (inclusive bounds, consistent with **`MillisDateRange.contains`** / order-history filtering). Group counts and sums by **`SalesChannel.normalize(channel)`**, compute top products by revenue from **`order_items`** joined to orders.
2. **COGS today (EOD-US-07):** For each product with sales on `business_date`, `qty_sold_today × WAC`. Sum to **total COGS today**; margin vs **collected** revenue.
3. **Inventory bridge (EOD-US-03 / EOD-US-10):** Per product: total acquired (all time), total sold (all time through date), prior spoilage from **sum of `variance_qty`** on **`day_close_inventory`** for finalized closes before `business_date`. **Adjusted theoretical** = acquired − sold − prior spoilage. If today has a **finalized** close, **EOD-US-10** says **actual_remaining** overrides theoretical until new sales/acquisitions change the rolling picture — implement as: base stock position = last finalized `actual_remaining` for that product’s most recent close date, then **add acquisitions after that date** and **subtract sales after that date** (or equivalent single formula documented in code).
4. **Cash (EOD-US-04):** **Expected cash** = sum of paid order totals on `business_date` where channel is **`offline` or `reseller`** (online/digital separate line). **Remitted today** = sum of **`remittances.amount`** where **`date`** is on `business_date` and **`entry_type = REMITTANCE`** (store assistant remits only; **disbursements** are excluded from this line — show **disbursements received today** separately if needed). Until **`entry_type`** ships, all existing rows count as remittances.
5. **Outstanding orders (EOD-US-08):** All orders `is_paid = 0`, sorted oldest first; not limited to today.
6. **Employee payments (EOD-US-09):** Payments with `date_paid` on `business_date`.

Use **`@Transaction`** where multiple reads must be consistent for finalize. Prefer **single source** for “theoretical stock” used by **Outstanding Inventory** and **Day Close inventory step** to avoid drift.

### 14.4 FIFO / lot aging (EOD-US-10)

- **Outstanding Inventory** drill-down: allocate **`order_items`** quantities to **`acquisitions`** in **FIFO** order (by `date_acquired`, then `acquisition_id`) per product for **remaining quantity per lot** and **oldest unsold acquisition date**.
- Encapsulate in a pure Kotlin type (e.g. **`InventoryFifoAllocator`**) unit-tested with small fixtures. **Aging thresholds:** **§14.15**.
- **Category filter** uses **`products.category`** (nullable — treat null as “Uncategorized”).

### 14.5 Units (kg vs piece)

Acquisitions and order lines store **`is_per_kg`** on both line items and acquisitions; quantities use the **product unit**. WAC calculations must **not** mix kg lots with piece lots incorrectly:

- For **EOD v1**, recommended rule: **one WAC per product per valuation unit** — if the product is effectively tracked in kg, convert piece acquisitions using **`piece_count`** (same as pricing) when present; if mixed history exists, document the chosen rule in **`OrderPricingResolver`** / acquisition docs and keep EOD consistent with that rule. Flag ambiguous products in QA.

### 14.6 UI flow (EOD-US-01)

- **Dashboard:** **“Close Day”** → `Screen.DayClose` (or similar). Hide per **RBAC**.
- **Wizard steps (conceptual):** Review warnings (**§14.11**) → **Sales & margin** → **Inventory counts** (**§14.10** edit rights) → **Cash** (**§14.12**) → **Outstanding orders** → **Employee summary** (**§14.14**) → **Confirm**. Exact tabbing vs single scroll is flexible; **Review → Confirm** gating matches EOD-US-01 AC.
- **Past date:** Only **admin** may select a `business_date` before today (late close); others always **today**.
- **Navigation:** **Order detail** from outstanding orders list (**EOD-US-08**) — **`Screen.OrderDetail.createRoute(orderId)`** (**§14.16**).
- **Negative gross margin (**EOD-US-07**):** If **collected revenue today − total COGS today** `< 0`, show a one-shot confirmation dialog before finalize; user may still proceed (AC4).

### 14.7 Printing (EOD-US-05)

- Add **`ThermalPrintBuilders.buildEodSummary(...)`** (58mm, left alignment): sections in **USER_STORIES** EOD-US-05; **“DRAFT — NOT FINAL”** when `!is_finalized`.
- Reuse **`PrinterUtils.printMessage`**; mirror patterns from order / acquisition slips (**`docs/printing.md`** PRN numbering if extended).

### 14.8 CSV export (optional follow-up)

Not required by Epic 12 AC; **`CsvExportService`** may later add **`day_closes`** / **`day_close_inventory`** for parity with other tables.

### 14.9 Named metrics (**EOD-US-02** vs **EOD-US-08**)

Use **distinct labels** in UI and on the thermal slip so totals are never conflated:

| Label | Definition | When computed |
|--------|-------------|---------------|
| **Unpaid orders (today)** | Orders with `order_date` on `business_date` and `is_paid = false` — count and sum of `total_amount`. | Live in draft; snapshot **`snapshot_unpaid_today_*`** on finalize (**§14.2**). |
| **Open receivables (all)** | All orders with `is_paid = false` (any date) — **EOD-US-08** list and header total. | Live in draft; snapshot **`snapshot_all_unpaid_*`** on finalize for historical slip accuracy. |
| **Digital collections (today)** | Paid orders (`is_paid = true`) on `business_date` with **`SalesChannel.normalize(channel) == SalesChannel.ONLINE`** — count and sum of `total_amount`. | Live; optional snapshot columns if history must freeze this line. |

There is no single “outstanding” number: always specify **today unpaid** vs **all unpaid**.

### 14.10 Wizard RBAC (single screen)

- **One route** (e.g. `DayCloseScreen`) for draft and in-progress close; optional **`closeId`** or **`business_date`** argument for admin back-close.
- **Dashboard — Close Day** (`EOD-US-01`): visible to **`ADMIN`** and **`STORE_ASSISTANT`** (`Rbac`).
- **Dashboard — Day Close History** (`EOD-US-06`): **`ADMIN`** only.
- **Dashboard — Outstanding Inventory** (`EOD-US-10`): **`ADMIN`** and **`PURCHASING`**.
- **Inventory count fields** (`EOD-US-03` — `actual_remaining`, not-counted toggles): **editable** only for **`ADMIN`** and **`PURCHASING`**. **Store assistants** see the same section **read-only** (theoretical columns + message that purchasing/admin enters counts), or a collapsed summary—pick one in UI polish; **enforcement** must be server-side in ViewModel/repo (reject updates if role ∉ `{ ADMIN, PURCHASING }`).
- **Finalize** remains available to roles allowed to complete **EOD-US-01** (**store assistant + admin**) unless product later restricts finalize to admin only.

### 14.11 Day-close warnings (**EOD-US-01**)

1. **Unpaid orders today:** Exists an order with `order_date` on `business_date` and `is_paid = false`.
2. **Acquired today, no sales today (per product):** Exists a `product_id` such that **today’s acquisitions** sum `quantity > 0` for that product (acquisition `date_acquired` on `business_date`) **and** **today’s sales** sum `order_items.quantity` for that product (orders with `order_date` on `business_date`) **`= 0`**. Show one consolidated banner listing affected product names (cap list length in UI if needed).

### 14.12 Cash reconciliation (**EOD-US-04**)

- **Expected cash from orders** = sum of **`orders.total_amount`** for orders where `order_date` is on `business_date`, `is_paid = true`, and **`SalesChannel.normalize(channel)`** is **`offline`** or **`reseller`** (not online).
- **Total remitted today** = sum of **`remittances.amount`** where **`remittances.date`** falls on the same calendar day **and** **`entry_type`** is **`REMITTANCE`** (or null during migration). **Disbursements** same day: separate informational total (`entry_type = DISBURSEMENT`).
- **Difference (expected vs remitted)** = expected cash − total remitted.
- **Cash on hand** (optional user entry): compare to **expected cash**; show second surplus/shortage line.
- **Remarks required before finalize** when **any** of:
  - `abs(difference_expected_minus_remittance) > epsilon` (use a small currency epsilon, e.g. **0.01**), or
  - cash on hand entered and `abs(cash_on_hand - expected_cash) > epsilon`.
  Persist in **`cash_reconciliation_remarks`**. Digital-only variance does not by itself satisfy “cash discrepancy” unless you also treat online totals as informational only (already excluded from expected cash).

### 14.13 `day_close_audit` (EOD-US-06 un-finalize log)

| Column | Type | Notes |
|--------|------|--------|
| `audit_id` | `INTEGER` PK auto | |
| `close_id` | `INTEGER` FK → `day_closes` | `ON DELETE CASCADE` acceptable |
| `action` | `TEXT` | e.g. `UNFINALIZE` |
| `username` | `TEXT` | Actor |
| `at_millis` | `INTEGER` | |
| `note` | `TEXT?` | Optional free text |

### 14.14 Employee day summary (**EOD-US-09**)

- List **`employee_payments`** with **`date_paid`** on `business_date` (same day window as orders).
- **Net pay** in UI must match payroll screens: use **`EmployeePayment.netPayAmount()`** (`data/model/EmployeePaymentAggregates.kt`).
- **Total wages paid today** = sum of **`amount`** (gross wage field), not net — align with **USER_STORIES** AC5; show net per row from `netPayAmount()`.

### 14.15 Inventory aging defaults (**EOD-US-03** / **EOD-US-10**)

- **v1:** Define **`AppConfig`** (or module-level) constants: warn at **≥ 3** days, critical at **≥ 7** days since relevant acquisition date (lot or “most recent acquisition” row per AC).
- **v2 (backlog):** Persist admin-tunable thresholds (settings / pref) when **USER_STORIES** “configurable” is scheduled.

### 14.16 Computed UI fields (no extra columns)

- **Last acquisition** (**EOD-US-03**): latest row per product by **`date_acquired`**, then **`acquisition_id`** — show date, qty, **`price_per_unit`** / cost fields from **`Acquisition`** / entity.
- **Outstanding orders navigation:** From EOD flow, tap row → **`Screen.OrderDetail.createRoute(orderId)`** (existing **`OrderDetailScreen`**); user marks paid there and returns—no new route required.

### 14.17 Printing doc

- When **`buildEodSummary`** / **`buildOutstandingInventoryReport`** exist, add a **PRN-** entry and field list to **`docs/printing.md`** next to existing thermal templates.

### 14.18 Implementation checklist (suggested order)

1. Entities (**§14.2**, **`day_close_audit` §14.13**), DAOs, **`DayCloseRepository`**, domain models `DayClose`, `DayCloseInventoryLine`.
2. **Stock + FIFO** pure functions + unit tests (feeds EOD-US-03 and EOD-US-10); **§14.3** bridge + **§14.9** metrics helpers.
3. **Day close** ViewModel + main screen; persist draft; finalize writes **snapshots** (**§14.1**, **§14.9**) + inventory rows.
4. **Day close history** + detail + admin un-finalize + **`day_close_audit`**.
5. **Outstanding inventory** screen + thermal **`buildOutstandingInventoryReport`** (**§14.17**).
6. **Dashboard** entries + **Rbac** constants (**§14.10**).
7. **Thermal EOD** slip (**§14.7**, **§14.17**) + manual QA per **USER_STORIES**.

Canonical acceptance criteria and edge-case wording: **USER_STORIES.md** Epic 12 (**EOD-US-01** through **EOD-US-10**).
