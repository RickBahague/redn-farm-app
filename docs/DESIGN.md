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
- Recording remittances to employees
- Exporting all data to CSV files
- Login / session management with hashed passwords

---
## 3. Actors and User Stories
- refer to USER_STORIES.md

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
| `order_history` | Order History |
| `edit_order/{orderId}` | Edit Order |
| `products` | Manage Products |
| `customers` | Manage Customers |
| `acquire` | Acquire Produce (Inventory) |
| `remittance` | Remittance |
| `employees` | Manage Employees (Green Crew) |
| `employee_payments/{employeeId}/{employeeName}` | Employee Payments |
| `farm_ops` | Farm Operations |
| `farm_ops_history` | Farm Ops History |
| `export` | Export / Data Management |
| `about` | About |

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

Users are created in `FarmDatabase`'s `onOpen` / `onCreate` callback if they don't already exist. Role is stored as a plain string in `users.role` — no role-based access control is enforced in the UI currently.

---

## 6. Database Design

### `FarmDatabase` (Room, version 3)

Located at `data/local/FarmDatabase.kt`. Singleton accessed via `FarmDatabase.getDatabase(context)`. A `clearInstance()` method exists for reinitialization scenarios.

**Type converters registered:**
- `DateTimeConverter` — `LocalDateTime` ↔ `Long` (epoch seconds, UTC offset)
- `EnumConverters` — `CustomerType`, `AcquisitionLocation`, `FarmOperationType` stored as their enum `name` strings

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

employees (employee_id INT PK, autoincrement)
  └── employee_payments (employee_id FK → RESTRICT delete)

remittances (remittance_id INT PK, autoincrement)

users (user_id INT PK, autoincrement; unique index on username)
```

### Schema details

| Table | Key fields |
|---|---|
| `products` | `product_id` (String), `product_name`, `product_description`, `unit_type`, `is_active` |
| `product_prices` | `price_id` (autoincr), `product_id`, `per_kg_price?`, `per_piece_price?`, `discounted_per_kg_price?`, `discounted_per_piece_price?`, `date_created` |
| `customers` | `customer_id`, `firstname`, `lastname`, `contact`, `customer_type` (enum), address fields, `date_created/updated` |
| `orders` | `order_id`, `customer_id`, `total_amount`, `order_date`, `order_update_date`, `is_paid`, `is_delivered` |
| `order_items` | `id`, `order_id`, `product_id`, `quantity`, `price_per_unit`, `is_per_kg`, `total_price` |
| `employees` | `employee_id`, `firstname`, `lastname`, `contact`, `date_created/updated` |
| `employee_payments` | `payment_id`, `employee_id`, `amount`, `cash_advance_amount?`, `liquidated_amount?`, `date_paid`, `signature`, `received_date?` |
| `farm_operations` | `operation_id`, `operation_type` (enum), `operation_date`, `details`, `area`, `weather_condition`, `personnel`, `product_id?`, `product_name` |
| `acquisitions` | `acquisition_id`, `product_id`, `product_name`, `quantity`, `price_per_unit`, `total_amount`, `is_per_kg`, `date_acquired`, `location` (enum) |
| `remittances` | `remittance_id`, `amount`, `date`, `remarks`, `date_updated` |
| `users` | `user_id`, `username` (unique), `password_hash`, `full_name`, `role`, `is_active` |

### Migrations

Current phase is still build. During this build phase no migration is needed. Just recreate the full database if schema changes. 

Track schema changes.

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
- Payments record `amount` (gross), `cash_advance_amount` (optional deduction), `liquidated_amount` (optional), and `signature` (stored as a string — likely a name or initialism, not a bitmap)
- `received_date` is nullable (payment may be scheduled but not yet received)

### Farm Operations
- Operations are filtered in-memory by type, date range, and text search (details + personnel fields)
- Optional link to a product (`product_id` nullable, SET NULL on delete)

### Acquisitions
- Records incoming inventory with source location (enum), price per unit, and whether measured by kg or piece

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
- **Sunmi printer** (`printMessage`): uses the Sunmi SDK (`com.sunmi:printerlibrary:1.0.23`) to print directly to a built-in Sunmi POS printer via `SunmiPrinterService`

The Sunmi printer connection is managed as a coroutine-suspended bind (`suspendCancellableCoroutine`).

---

## 11. Utilities

| Class | Location | Purpose |
|---|---|---|
| `CurrencyFormatter` | `utils/` | Formats doubles as currency strings |
| `DeviceUtils` | `utils/` | Returns `Settings.Secure.ANDROID_ID` as device identifier |
| `PrinterUtils` | `utils/` | Android print + Sunmi thermal printer |
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

4. **`Converters.kt`** in `data/local/util/` exists alongside `converters/DateTimeConverter.kt` and `converters/EnumConverters.kt` — appears to be an unused or legacy file.

5. **Date/time storage inconsistency**: Some entities store dates as `Long` (epoch millis), others store `LocalDateTime` (converted via `DateTimeConverter` as epoch seconds). This causes subtle conversion differences across the codebase.
