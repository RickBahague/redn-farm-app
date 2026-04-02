# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew :app:testDebugUnitTest --tests "*.PasswordManagerTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Dev Script

`scripts/dev.sh` wraps the most common tasks:

If **adb** reports *more than one device/emulator*, run `./scripts/dev.sh devices`, then either `export ANDROID_SERIAL=<serial>` or `./scripts/dev.sh install <serial>` (same optional serial works for `fresh`, `launch`, `log`, etc.).

```bash
./scripts/dev.sh install          # build + install debug APK
./scripts/dev.sh fresh            # uninstall + install + launch (clean slate)
./scripts/dev.sh log              # filtered logcat
./scripts/dev.sh log-crash        # errors and crashes only
./scripts/dev.sh pull-exports     # pull CSV exports to ~/Desktop/farm_exports/
./scripts/dev.sh db               # print DB Inspector SQL queries
./scripts/dev.sh test-class <Cls> # run one JVM test class by short name
./scripts/dev.sh pair             # guided wireless ADB pairing
```

Run `./scripts/dev.sh help` for the full list.

## Architecture Overview

Android app (Kotlin + Jetpack Compose) for farm management. Package: `com.redn.farm`.

**Tech stack:** MVVM, Hilt DI, Room (SQLite), Jetpack Navigation Compose, Material3

### Layer structure under `app/src/main/java/com/redn/farm/`:

- `data/local/` — Room entities, DAOs, FarmDatabase, session/security utilities
- `data/model/` — domain models (separate from Room entities)
- `data/repository/` — repositories wrapping DAOs
- `data/export/` — `CsvExportService` writes CSV to external files dir `exports/`
- `ui/screens/` — feature folders, each containing `*Screen.kt` + `*ViewModel.kt`
- `di/` — Hilt modules: `DatabaseModule` (provides DB + DAOs), `RepositoryModule` (provides select repositories)
- `navigation/NavGraph.kt` — all routes defined as `Screen` sealed class; start destination is `Login`
- `config/AppConfig.kt` — app-level constants

### Database

`FarmDatabase` (Room, version 3) in `data/local/FarmDatabase.kt`. Migrations are defined inline in the companion object (`MIGRATION_1_2`, `MIGRATION_2_3`). Always add new migrations there and increment the version in the `@Database` annotation.

On first create, default users are seeded: `admin` / `admin123` and `user` / `user123`.

Seed assets live in `app/src/main/assets/`: `data/products.json`, `data/product_prices.json`, `data/customers.json`, and a pre-populated `database/farm.db`.

### DI pattern

Not all repositories are bound in `RepositoryModule` — some are injected directly via constructor injection by Hilt. `DatabaseModule` provides all DAOs; ViewModels receive them through Hilt constructor injection.

### Session management

`SessionManager` (SharedPreferences) tracks login state. `LoginScreen` → `MainScreen` navigation pops the back stack so the user can't navigate back to login. Logout clears the entire back stack.

### Navigation

Routes are string-based. Parameterized routes (e.g., `edit_order/{orderId}`, `employee_payments/{employeeId}/{employeeName}`) are defined in the `Screen` sealed class with `createRoute(...)` helpers. Employee names have spaces replaced with `_` in the route and restored on the receiving end.
