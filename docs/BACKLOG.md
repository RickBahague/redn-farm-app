# Farm App — Improvement Backlog

Items are grouped by type and ordered by priority within each group. Priority levels: **P1** (broken), **P2** (design inconsistency), **P3** (technical debt), **P4** (enhancement).

Status legend: `[ ]` open · `[x]` done

---

## Completed

| ID | Item | Notes |
|---|---|---|
| — | `CLAUDE.md` created | Build commands, architecture overview, DI and nav conventions |
| — | `docs/DESIGN.md` created | Full design doc: schema, layer breakdown, feature details, known inconsistencies |
| — | Test dependencies added | `kotlinx-coroutines-test`, `room-testing`, `mockito-core/kotlin` added to `libs.versions.toml` + `build.gradle.kts` |
| — | `PasswordManagerTest.kt` | JVM unit tests for hash/verify; pins algorithm name so TD-08 upgrade is auditable |
| — | `DateTimeConverterTest.kt` | JVM tests exposing DI-05 (epoch seconds vs millis); one assertion intentionally fails until DI-05 is fixed |
| — | `OrderDaoTest.kt` | Instrumented Room test confirming BUG-02 current behaviour; commented regression test ready to uncomment after fix |
| — | `RemittanceDaoTest.kt` | Instrumented Room test that fails on BUG-03 until `date_updated` mapping is corrected |
| — | `scripts/dev.sh` | Dev script: install, logcat filters, export pull, DB Inspector queries, wireless ADB pairing, test runner |

---

## Bugs (P1 — things that are currently broken)

### [ ] BUG-01 · Location filter on Acquire screen does nothing
**File:** `ui/screens/acquire/AcquireProduceViewModel.kt:45`

`_selectedLocation` is included as a parameter in the `combine()` lambda but is never used in the filter predicate. Tapping a location filter chip has no effect.

```kotlin
// combine receives: acquisitions, query, location, dateRange
acquisitions.filter { acquisition ->
    val matchesSearch = ...
    val matchesDateRange = ...
    matchesSearch && matchesDateRange  // ← location never checked
}
```

**Fix:** Add `val matchesLocation = location == null || acquisition.location == location` and include it in the final predicate.

---

### [ ] BUG-02 · "Truncate Order Items" deletes the entire orders table
**File:** `ui/screens/export/ExportViewModel.kt:255`

```kotlin
fun truncateOrderItems() {
    viewModelScope.launch {
        orderRepository.truncate()  // ← deletes orders AND order_items
```

`OrderRepository.truncate()` calls `orderDao.truncate()` which runs `DELETE FROM order_items` then `DELETE FROM orders`. The intent was to delete only `order_items`. This silently wipes all order records.

**Fix:** Add a dedicated `truncateOrderItems()` to `OrderDao` and `OrderRepository`, then call only that.

---

### [ ] BUG-03 · `date_updated` dropped when mapping Remittance entity to model
**File:** `data/repository/RemittanceRepository.kt:34`

```kotlin
private fun RemittanceEntity.toRemittance() = Remittance(
    remittance_id = remittance_id,
    amount = amount,
    date = date,
    remarks = remarks   // ← date_updated not copied
)
```

`Remittance.date_updated` always defaults to `System.currentTimeMillis()` on every read. Any UI showing the last-updated timestamp will display the current time instead of the actual stored value.

**Fix:** Pass `date_updated = date_updated` in the mapping.

---

### [ ] BUG-04 · `AcquireProduceViewModel` subscribes to acquisitions twice
**File:** `ui/screens/acquire/AcquireProduceViewModel.kt:67`

The `init` block launches a coroutine that collects `acquisitionRepository.getAllAcquisitions()` into `_acquisitions`. That `StateFlow` is never observed anywhere — the exposed `acquisitions` property is the `combine()` flow which subscribes independently. Two active collectors run against the same DAO query for the lifetime of the ViewModel.

**Fix:** Remove the `_acquisitions` private state and its `init` collection entirely.

---

### [ ] BUG-05 · `ProductRepository.getFilteredProducts()` calls a `suspend` DAO function inside a `Flow.map`
**File:** `data/repository/ProductRepository.kt:88`

```kotlin
"price" -> {
    val prices = productPriceDao.getLatestPricesSync()  // suspend fun inside map {}
    filteredProducts.sortedBy { ... }
}
```

`Flow.map` is not a coroutine-suspending block in the same way; calling a `suspend` function here compiles but runs synchronously on the collection thread (IO dispatcher in practice), blocking it. It also does not participate in the Flow's cancellation properly.

**Fix:** Restructure `getFilteredProducts()` to `combine()` the product flow with the prices flow, eliminating the sync call.

---

## Design Inconsistencies (P2 — from DESIGN.md)

### [ ] DI-01 · Mixed Hilt and manual ViewModel factories
Three ViewModels use `@HiltViewModel` + `@Inject` (`FarmOperationsViewModel`, `AcquireProduceViewModel`) while all others use `AndroidViewModel` with manual `viewModelFactory {}` companions that call `FarmDatabase.getDatabase(application)` directly.

This creates multiple repository instances for the same database connection (e.g., `FarmOperationRepository` is instantiated fresh in `ExportViewModel` while Hilt provides a singleton elsewhere).

**Fix:** Migrate all ViewModels to `@HiltViewModel`. Add the missing repositories to `RepositoryModule`. Remove all `companion object { val Factory ... }` blocks and all manual DAO/repository construction in ViewModels.

---

### [ ] DI-02 · `DatabaseMigrationScreen` is unreachable
The route `database_migration` is defined in the `Screen` sealed class and registered in `NavGraph`, but no screen navigates to it. `DatabaseMigrationViewModel` contains a full `getAllData()` bulk export used in DB setup, but the screen itself is dead.

**Decision needed:** Either wire it into the app launch flow (check for first-run or version upgrade), or remove the screen and move the bulk-export logic to `ExportViewModel`.

---

### [ ] DI-03 · `DatabaseInitializer` and JSON seed assets are disconnected
`app/src/main/assets/data/` contains `products.json`, `product_prices.json`, and `customers.json`. The domain models have `ProductList` / `ProductPriceList` / `CustomerList` wrappers for Gson parsing. However, `DatabaseInitializer.populateDatabase()` inserts two hardcoded products (Tomatoes, Lettuce) and ignores the JSON files entirely.

**Fix:** Either have `populateDatabase()` read and parse the JSON assets, or remove the JSON files and the list wrapper models if hardcoded seed data is intentional.

---

### [x] DI-04 · `Converters.kt` is dead code
`data/local/util/Converters.kt` was unused by **`FarmDatabase`**. **Fix applied:** file deleted (**BUG-ARC-09**). Active converters remain under **`data/local/converters/`**.

---

### [ ] DI-05 · Inconsistent date/time storage across entities
| Entity | Storage type | Converter |
|---|---|---|
| `CustomerEntity`, `ProductPriceEntity`, `FarmOperationEntity` (date_created/updated) | `LocalDateTime` stored as `Long` epoch **seconds** (UTC) via `DateTimeConverter` | `DateTimeConverter` |
| `OrderEntity`, `EmployeeEntity`, `EmployeePaymentEntity`, `RemittanceEntity`, `FarmOperationEntity` (operation_date) | Raw `Long` epoch **milliseconds** assigned via `System.currentTimeMillis()` | None |
| `AcquisitionEntity` | `Long` epoch **milliseconds** | None |

This means conversion code varies per field, and mixing up millis vs seconds produces dates 1000× off. For example, `DateTimeConverter.fromTimestamp()` divides by nothing — it treats the value as epoch **seconds**, but entities storing `System.currentTimeMillis()` pass epoch **milliseconds**, producing dates in the year 51,000+.

**Fix:** Standardize all date columns to `Long` epoch milliseconds, update `DateTimeConverter` to divide/multiply by 1000 correctly, and audit all repository mapping code.

---

## Technical Debt (P3)

### [ ] TD-01 · `UserEntity.role` is an untyped String
Role is stored as `"ADMIN"` or `"USER"` as a raw string. There is no `UserRole` enum, so role checks anywhere in code would be stringly-typed and error-prone.

**Fix:** Create a `UserRole` enum (`ADMIN`, `USER`) and add an `EnumConverter` for it, parallel to how `CustomerType` is handled.

---

### [ ] TD-02 · No role-based access control in the UI
Both ADMIN and USER accounts see the same main menu and all features. The `role` field on `UserEntity` / `SessionManager` is never read after login to restrict any screen or action.

**Fix:** At minimum, restrict the Export/Data Management screen and the truncate operations to ADMIN role only. Store the role in `SessionManager` alongside the username.

---

### [ ] TD-03 · Session never expires
`SessionManager` stores `is_logged_in = true` indefinitely. There is no idle timeout, no token expiry, and no re-authentication prompt. The `KEY_LOGIN_TIME` field is stored but never checked.

**Fix:** On `SessionManager.isLoggedIn()`, compare `KEY_LOGIN_TIME` against a configurable max idle duration (e.g., 8 hours) and return `false` if exceeded.

---

### [ ] TD-04 · Bluetooth permissions declared but unused
`AndroidManifest.xml` declares `BLUETOOTH` and `BLUETOOTH_ADMIN` permissions. No code in the project uses Bluetooth APIs. The Sunmi printer connects via AIDL (`InnerPrinterManager`), not Bluetooth.

**Fix:** Remove both permission declarations.

---

### [ ] TD-05 · No confirmation dialog before truncating table data
`ExportViewModel` exposes `truncateCustomers()`, `truncateOrders()`, `truncateFarmOperations()`, etc. These permanently delete all records. There is no confirmation step in the ViewModel or Screen before calling them.

**Fix:** Show an `AlertDialog` ("This will permanently delete all X records. Continue?") in `ExportScreen` before calling any truncate method.

---

### [ ] TD-06 · `DatabaseInitializer.reinitializeDatabase()` deletes the live database unsafely
It calls `FarmDatabase.getDatabase(context).close()`, `FarmDatabase.clearInstance()`, then `context.deleteDatabase("farm_database")` while the app may have active DAOs open. This can cause `SQLiteDatabaseLockedException` or data corruption.

**Fix:** Wrap in a proper database rebuild: close all active database connections first, wait for any in-flight coroutines, then rebuild.

---

### [ ] TD-07 · `product_id` is a manually managed String with no generation UI
`ProductEntity.product_id` is a `TEXT PRIMARY KEY` (not autoincrement). When adding a new product, the app must supply a unique string ID. There is no evident UI field or auto-generation logic for it in `ManageProductsScreen`.

**Fix:** Either switch to autoincrement integer PKs, or implement auto-generation of IDs (e.g., `"PROD" + UUID` or sequential) in `ProductRepository.insertProduct()`.

---

### [ ] TD-08 · `PasswordManager` uses PBKDF2WithHmacSHA1
SHA-1 is considered weak for password hashing. The 65,536 iteration count also applies to the SHA-1 variant, which is faster to brute-force than a SHA-256 equivalent.

**Fix:** Switch to `PBKDF2WithHmacSHA256` and increase iterations to at least 120,000 (OWASP 2023 recommendation). Note: existing stored hashes will need to be re-hashed on next login.

---

## Enhancements (P4)

### [ ] ENH-01 · Add user management screen
No UI exists to create new users, change passwords, or deactivate accounts despite `UserDao` supporting all three operations (`insertUser`, `updateUser`, `deactivateUser`).

---

### [ ] ENH-02 · Dashboard summary on MainScreen
The main screen is a plain menu. Adding a daily summary card (today's orders count, total revenue, pending deliveries) would make it more useful as a POS home screen.

---

### [ ] ENH-03 · Print receipt from Order screen
`PrinterUtils` exists and supports both Android print and Sunmi thermal printing, but `TakeOrderScreen` and `OrderHistoryScreen` have no "Print Receipt" action. The order summary data is all available in the ViewModel.

---

### [ ] ENH-04 · Paginate large lists
All list screens load the entire table into memory. With moderate use (hundreds of orders, acquisitions), this will grow. Room supports `PagingSource` via the Paging 3 library.

---

### [ ] ENH-05 · Customer search in DAO is LIKE-based, not used in all screens
`CustomerDao.searchCustomers()` exists and handles name/contact search at the SQL level. `TakeOrderViewModel` reimplements search in-memory on top of `getAllCustomers()`. Use the DAO-level search consistently.

---

### [ ] ENH-06 · Export shares via FileProvider, not just saves to disk
`AndroidManifest.xml` already configures a `FileProvider` and `file_paths.xml` exists. Currently, export files are saved to `getExternalFilesDir(null)/exports/` but there is no share intent triggered. Add a share button after successful export using `FileProvider.getUriForFile()`.

---

## [x] Helpers & Test Files

These files were added to assist in verifying and fixing the backlog items above.

### Test dependencies added
`gradle/libs.versions.toml` and `app/build.gradle.kts` now include:
- `kotlinx-coroutines-test:1.7.3` — for Flow/coroutine assertions
- `mockito-core:5.7.0` + `mockito-kotlin:5.2.1` — for mocking in unit tests
- `androidx.room:room-testing` — for in-memory Room database in instrumented tests

### Test files

| File | Type | Covers |
|---|---|---|
| `app/src/test/.../security/PasswordManagerTest.kt` | JVM | TD-08 (algorithm doc), hash/verify correctness |
| `app/src/test/.../converters/DateTimeConverterTest.kt` | JVM | DI-05 (epoch seconds vs millis inconsistency) |
| `app/src/androidTest/.../dao/OrderDaoTest.kt` | Instrumented | BUG-02 (truncate order items bug), order CRUD |
| `app/src/androidTest/.../dao/RemittanceDaoTest.kt` | Instrumented | BUG-03 (date_updated dropped in mapping) |

**Run all unit tests:**
```bash
./gradlew testDebugUnitTest
```

**Run a single test class:**
```bash
./scripts/dev.sh test-class PasswordManagerTest
./scripts/dev.sh test-class DateTimeConverterTest
```

**Run instrumented tests on device:**
```bash
./scripts/dev.sh test-device
# or target one class:
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.redn.farm.data.local.dao.OrderDaoTest
```

### `scripts/dev.sh`

A single script that wraps all common dev tasks:

```
./scripts/dev.sh install          # build debug APK + install
./scripts/dev.sh fresh            # uninstall + install + launch (clean slate)
./scripts/dev.sh launch           # open the app on device
./scripts/dev.sh kill             # force-stop
./scripts/dev.sh log              # filtered logcat for Farm tags
./scripts/dev.sh log-db           # Room/SQLite logs only
./scripts/dev.sh log-crash        # errors and crashes only
./scripts/dev.sh pull-exports     # pull CSV exports to ~/Desktop/farm_exports/
./scripts/dev.sh db               # print DB Inspector queries + instructions
./scripts/dev.sh db-pull          # pull SQLite file to ~/Desktop (emulator/rooted)
./scripts/dev.sh devices          # list ADB devices
./scripts/dev.sh pair             # guided wireless ADB pairing
./scripts/dev.sh test             # run JVM unit tests
./scripts/dev.sh test-class <Cls> # run one test class
./scripts/dev.sh test-device      # run instrumented tests
./scripts/dev.sh clean            # gradle clean
```

---

## Testing Workflow

### Prerequisites

**Enable developer options on the device:**
1. Go to *Settings → About Phone* → tap *Build Number* 7 times
2. Go to *Settings → Developer Options* → enable *USB Debugging*

For **wireless ADB** (Android 11+):
1. *Developer Options → Wireless Debugging* → enable → tap *Pair device with pairing code*
2. On your Mac: `adb pair <ip>:<port>` with the displayed code
3. Then: `adb connect <ip>:<port2>` (the port shown after pairing)

---

### Install & Run (debug build)

```bash
# Build and install directly to connected device
./gradlew installDebug

# Or build the APK, then push manually
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Verify the app is installed
adb shell pm list packages | grep redn.farm

# Launch the app from terminal
adb shell am start -n com.redn.farm.debug/com.redn.farm.MainActivity
```

**For release build testing** (uses ProGuard — catches minification issues):
```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
# Note: release builds require a signing config; unsigned release APKs won't install
```

---

### Logcat Filtering

Open a filtered log stream before launching the app:

```bash
# All Farm app logs
adb logcat -s "FarmDatabase" "LoginViewModel" "DatabaseInitializer" "DatabaseExporter"

# Just crash logs
adb logcat "*:E" | grep -E "redn.farm|FATAL|ANR"

# Room/SQLite query issues
adb logcat -s "SQLiteDatabase" "Room"

# Printer-related
adb logcat -s "PrinterUtils" "SunmiPrinterService"
```

In Android Studio: open *Logcat*, set package filter to `com.redn.farm`.

---

### Feature Test Scenarios

Work through these in order on a fresh install (new device or after `adb shell pm clear com.redn.farm.debug`).

#### 1. First Launch & Login
- [ ] App shows Login screen on first launch
- [ ] Login with `admin` / `admin123` → lands on Main screen
- [ ] Login with `user` / `user123` → lands on Main screen
- [ ] Login with wrong password → shows error, stays on Login screen
- [ ] Login with blank fields → shows validation error
- [ ] After successful login, press device back button → should NOT go back to Login
- [ ] Logout from top-right icon → returns to Login, back button does not go to Main

#### 2. Products
- [ ] Navigate to Products → list loads
- [ ] Add a new product with a unique `product_id` string
- [ ] Set a per-kg price and a per-piece price
- [ ] Set a discounted price
- [ ] Edit an existing product name → change is reflected in the list
- [ ] Verify adding a second price creates a new row (price history), not an update

#### 3. Customers
- [ ] Add a RETAIL and a WHOLESALE customer
- [ ] Search by first name, last name, contact number
- [ ] Edit a customer's address
- [ ] Attempt to delete a customer who has orders → should fail (RESTRICT FK)

#### 4. Take Order
- [ ] Select a customer (search by name and by order number)
- [ ] Add a product priced per-kg, enter quantity
- [ ] Add a product priced per-piece
- [ ] Add same product twice → verify two separate line items (or check if it merges)
- [ ] Remove an item from cart
- [ ] Verify cart total updates correctly
- [ ] Place order → cart clears, navigate to Order History to confirm it appears
- [ ] Place order with discounted price selected
- [ ] Attempt to place order with no customer selected → should be blocked

#### 5. Order History
- [ ] All placed orders appear, sorted by date descending
- [ ] Search by customer name
- [ ] Search by order ID number
- [ ] Filter by date range — pick a range that includes and excludes specific orders
- [ ] Mark an order as paid → paid badge appears
- [ ] Mark an order as delivered
- [ ] Edit an order: change a product quantity, save → verify updated total
- [ ] Delete an **unpaid** order → it disappears
- [ ] Attempt to delete a **paid** order → should be prevented (or confirm behavior)
- [ ] View order summary dialog → check product totals, customer count

#### 6. Inventory (Acquire Produce)
- [ ] Add an acquisition for an existing product, from FARM location
- [ ] Add another from MARKET
- [ ] **Verify location filter chip actually filters** (currently broken per BUG-01)
- [ ] Filter by date range
- [ ] Edit an existing acquisition
- [ ] Delete an acquisition

#### 7. Farm Operations
- [ ] Add an operation of each type (SOWING, HARVESTING, etc.)
- [ ] Link an operation to a product
- [ ] Filter by operation type
- [ ] Filter by date range
- [ ] Search by details text and by personnel name
- [ ] Edit an operation
- [ ] Delete an operation

#### 8. Employee / Green Crew
- [ ] Add an employee
- [ ] Navigate to their payments screen
- [ ] Add a payment with a cash advance amount
- [ ] Add a payment with a liquidated amount
- [ ] Edit a payment
- [ ] Delete a payment

#### 9. Remittance
- [ ] Add a remittance with a date and remarks
- [ ] Edit the remittance amount
- [ ] Delete a remittance

#### 10. Export
- [ ] Export each table individually → confirm files appear in device storage
  - Path: `Android/data/com.redn.farm.debug/files/exports/`
  - Access via: `adb shell ls /sdcard/Android/data/com.redn.farm.debug/files/exports/`
  - Pull a file: `adb pull /sdcard/Android/data/com.redn.farm.debug/files/exports/<filename>.csv ~/Desktop/`
- [ ] Open CSV in a spreadsheet app and verify all columns are correct
- [ ] **Verify "Truncate Order Items" only deletes items, not orders** (currently broken per BUG-02)
- [ ] Confirm truncate prompts for confirmation before deleting (currently missing per TD-05)

#### 11. Printer (Sunmi device only)
- [ ] Only relevant on a Sunmi POS device (T2 mini or equivalent)
- [ ] Confirm `woyou.aidlservice.jiuiv5` service is present: `adb shell pm list packages | grep woyou`
- [ ] Trigger a print from any screen that uses `PrinterUtils`
- [ ] Confirm paper feeds and cuts correctly

#### 12. Session Guard
- [ ] Log in, then force-stop the app (`adb shell am force-stop com.redn.farm.debug`)
- [ ] Reopen app → should be logged in (session persists across process restart)
- [ ] Log out, reopen → should land on Login screen
- [ ] Log in on device A, then uninstall/reinstall → confirm session is cleared

---

### Pulling Export Files from Device

```bash
# List exported files
adb shell ls -la /sdcard/Android/data/com.redn.farm.debug/files/exports/

# Pull all exports to desktop
adb pull /sdcard/Android/data/com.redn.farm.debug/files/exports/ ~/Desktop/farm_exports/

# View SQLite database directly (requires root or emulator)
adb shell "run-as com.redn.farm.debug cat /data/data/com.redn.farm.debug/databases/farm_database" > ~/Desktop/farm_database.db
# Then open with: DB Browser for SQLite or sqlite3 ~/Desktop/farm_database.db
```

For **non-rooted physical devices**, use Android Studio's *App Inspection → Database Inspector* tab to browse and query `farm_database` live while the app is running.

---

### Running Tests

```bash
# JVM unit tests only
./gradlew test

# Single test class
./gradlew :app:testDebugUnitTest --tests "com.redn.farm.ExampleUnitTest"

# Instrumented tests (device must be connected)
./gradlew connectedAndroidTest

# Run and watch logcat simultaneously (two terminals)
# Terminal 1:
adb logcat -s "TestRunner"
# Terminal 2:
./gradlew connectedAndroidTest
```

> **Note:** There are currently no meaningful tests beyond the example scaffolding. The first unit tests worth writing are for `PasswordManager` (hash/verify round-trip), `DateTimeConverter` (epoch conversion correctness — see DI-05), and repository mapping functions (especially the `date_updated` bug in BUG-03).
