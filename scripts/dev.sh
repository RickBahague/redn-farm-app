#!/usr/bin/env bash
# Farm App — development helper script
# Usage: ./scripts/dev.sh <command> [options]
#
# Multiple devices: if you see "adb: more than one device/emulator", list targets with
#   ./scripts/dev.sh devices
# then pick one serial (first column) and either:
#   export ANDROID_SERIAL=<serial>   # applies to all adb commands in this shell
#   ./scripts/dev.sh install
# or pass the serial as the first argument after the command (shortcut):
#   ./scripts/dev.sh install <serial>
#   ./scripts/dev.sh fresh <serial>
#
# Commands:
#   install           Build debug APK and install on connected device
#   install-release   Build release APK and install (requires signing config)
#   launch            Launch the app on connected device
#   kill              Force-stop the app
#   uninstall         Uninstall the debug build
#   fresh             Uninstall + install (clean slate, clears all app data)
#
#   log               Tail filtered logcat (Farm app + DB + printer)
#   log-db            Tail only database-related logs
#   log-crash         Tail only errors and crashes
#
#   test              Run JVM unit tests
#   test-class <cls>  Run a single test class (short name, e.g. PasswordManagerTest)
#   test-device       Run instrumented tests on connected device
#
#   pull-exports      Pull all exported CSV/JSON files to ~/Desktop/farm_exports/
#   db                Open DB in Android Studio App Inspection (prints instructions)
#   db-pull           Pull the SQLite database file to ~/Desktop/farm_database.db
#
#   devices           List connected ADB devices
#   pair              Guide through wireless ADB pairing
#
#   clean             Clean Gradle build caches

set -e

PACKAGE_DEBUG="com.redn.farm.debug"
PACKAGE_RELEASE="com.redn.farm"
ACTIVITY="com.redn.farm.MainActivity"
DB_NAME="farm_database"
EXPORTS_DEVICE_PATH="/sdcard/Android/data/${PACKAGE_DEBUG}/files/exports/"
EXPORTS_LOCAL_PATH="${HOME}/Desktop/farm_exports"

# ── colours ──────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}▶ $*${NC}"; }
warn()  { echo -e "${YELLOW}⚠ $*${NC}"; }
error() { echo -e "${RED}✖ $*${NC}"; exit 1; }

# ── helpers ───────────────────────────────────────────────────────────────────
check_adb() {
    command -v adb &>/dev/null || error "adb not found. Add Android SDK platform-tools to PATH."
    local count
    count="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" { c++ } END { print c+0 }')"
    [[ "$count" -ge 1 ]] || error "No device connected. Run './scripts/dev.sh devices' for help."
    if [[ "$count" -gt 1 ]] && [[ -z "${ANDROID_SERIAL:-}" ]]; then
        error "Multiple devices connected. Pick a serial from 'adb devices' (first column), then either:\n  export ANDROID_SERIAL=<serial>\n  ./scripts/dev.sh install\nOr: ./scripts/dev.sh install <serial>"
    fi
}

# Optional device serial as first argument to a command (e.g. install emulator-5554).
apply_device_serial_arg() {
    if [[ -n "${1:-}" ]]; then
        export ANDROID_SERIAL="$1"
    fi
}

project_root() {
    # Script lives in scripts/, project root is one level up
    cd "$(dirname "$0")/.." || exit 1
}

# ── commands ──────────────────────────────────────────────────────────────────

cmd_install() {
    apply_device_serial_arg "${1:-}"
    project_root
    info "Building debug APK…"
    ./gradlew assembleDebug
    info "Installing on device…"
    check_adb
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    info "Done. Run './scripts/dev.sh launch' to open the app."
}

cmd_install_release() {
    apply_device_serial_arg "${1:-}"
    project_root
    warn "Release build requires a signing config in build.gradle.kts"
    info "Building release APK…"
    ./gradlew assembleRelease
    check_adb
    adb install -r app/build/outputs/apk/release/app-release.apk
    info "Done."
}

cmd_launch() {
    apply_device_serial_arg "${1:-}"
    check_adb
    info "Launching ${PACKAGE_DEBUG}…"
    adb shell am start -n "${PACKAGE_DEBUG}/${ACTIVITY}"
}

cmd_kill() {
    apply_device_serial_arg "${1:-}"
    check_adb
    info "Force-stopping ${PACKAGE_DEBUG}…"
    adb shell am force-stop "${PACKAGE_DEBUG}"
}

cmd_uninstall() {
    apply_device_serial_arg "${1:-}"
    check_adb
    info "Uninstalling ${PACKAGE_DEBUG}…"
    adb shell pm uninstall "${PACKAGE_DEBUG}" || warn "Package not installed"
}

cmd_fresh() {
    apply_device_serial_arg "${1:-}"
    project_root
    check_adb
    info "Uninstalling old build…"
    adb shell pm uninstall "${PACKAGE_DEBUG}" 2>/dev/null || true
    cmd_install
    cmd_launch
}

cmd_log() {
    apply_device_serial_arg "${1:-}"
    check_adb
    info "Tailing Farm app logs (Ctrl-C to stop)…"
    adb logcat --pid="$(adb shell pidof -s ${PACKAGE_DEBUG} 2>/dev/null || echo 0)" \
        -s "FarmDatabase" "LoginViewModel" "DatabaseInitializer" \
           "DatabaseExporter" "PrinterUtils" "ExportViewModel" \
           "AcquireProduceViewModel" "TakeOrderViewModel" \
        2>/dev/null || \
    adb logcat -s "FarmDatabase" "LoginViewModel" "DatabaseInitializer" \
                  "DatabaseExporter" "PrinterUtils" "ExportViewModel"
}

cmd_log_db() {
    apply_device_serial_arg "${1:-}"
    check_adb
    info "Tailing database logs (Ctrl-C to stop)…"
    adb logcat -s "FarmDatabase" "SQLiteDatabase" "Room" "DatabaseInitializer"
}

cmd_log_crash() {
    apply_device_serial_arg "${1:-}"
    check_adb
    info "Tailing crashes and errors (Ctrl-C to stop)…"
    adb logcat "*:E" | grep -E "redn\.farm|FATAL|AndroidRuntime|ANR"
}

cmd_test() {
    project_root
    info "Running JVM unit tests…"
    ./gradlew testDebugUnitTest
    info "Report: app/build/reports/tests/testDebugUnitTest/index.html"
}

cmd_test_class() {
    local cls="$1"
    [[ -z "$cls" ]] && error "Usage: ./scripts/dev.sh test-class <ClassName>"
    project_root
    info "Running tests in ${cls}…"
    ./gradlew :app:testDebugUnitTest --tests "*.${cls}"
}

cmd_test_device() {
    apply_device_serial_arg "${1:-}"
    project_root
    check_adb
    info "Running instrumented tests on device…"
    ./gradlew connectedAndroidTest
    info "Report: app/build/reports/androidTests/connected/index.html"
}

cmd_pull_exports() {
    apply_device_serial_arg "${1:-}"
    check_adb
    info "Pulling exports from device to ${EXPORTS_LOCAL_PATH}…"
    mkdir -p "${EXPORTS_LOCAL_PATH}"
    adb pull "${EXPORTS_DEVICE_PATH}" "${EXPORTS_LOCAL_PATH}/" || \
        warn "No export files found at ${EXPORTS_DEVICE_PATH}. Run an export from the app first."
    info "Files saved to ${EXPORTS_LOCAL_PATH}"
    ls -lh "${EXPORTS_LOCAL_PATH}" 2>/dev/null || true
}

cmd_db() {
    info "To inspect the live database:"
    echo ""
    echo "  1. Connect your device and open Android Studio"
    echo "  2. Go to View → Tool Windows → App Inspection"
    echo "  3. Select your device and the ${PACKAGE_DEBUG} process"
    echo "  4. Click 'Database Inspector' tab"
    echo "  5. Select '${DB_NAME}'"
    echo ""
    info "Useful SQL queries to paste in the inspector:"
    echo ""
    echo "  -- Check all tables have data"
    echo "  SELECT 'products' as t, COUNT(*) FROM products"
    echo "  UNION ALL SELECT 'customers', COUNT(*) FROM customers"
    echo "  UNION ALL SELECT 'orders', COUNT(*) FROM orders"
    echo "  UNION ALL SELECT 'order_items', COUNT(*) FROM order_items"
    echo "  UNION ALL SELECT 'acquisitions', COUNT(*) FROM acquisitions"
    echo "  UNION ALL SELECT 'remittances', COUNT(*) FROM remittances"
    echo "  UNION ALL SELECT 'employees', COUNT(*) FROM employees"
    echo "  UNION ALL SELECT 'employee_payments', COUNT(*) FROM employee_payments"
    echo "  UNION ALL SELECT 'farm_operations', COUNT(*) FROM farm_operations"
    echo "  UNION ALL SELECT 'users', COUNT(*) FROM users;"
    echo ""
    echo "  -- Verify default users"
    echo "  SELECT user_id, username, role, is_active FROM users;"
    echo ""
    echo "  -- Latest price per product (price history check)"
    echo "  SELECT p.product_name, pp.per_kg_price, pp.per_piece_price, pp.date_created"
    echo "  FROM product_prices pp"
    echo "  JOIN products p ON p.product_id = pp.product_id"
    echo "  ORDER BY pp.date_created DESC;"
    echo ""
    echo "  -- Orders with unpaid status"
    echo "  SELECT o.order_id, c.firstname, c.lastname, o.total_amount, o.is_paid"
    echo "  FROM orders o JOIN customers c ON c.customer_id = o.customer_id"
    echo "  WHERE o.is_paid = 0"
    echo "  ORDER BY o.order_date DESC;"
}

cmd_db_pull() {
    apply_device_serial_arg "${1:-}"
    check_adb
    local dest="${HOME}/Desktop/farm_database.db"
    info "Pulling database file to ${dest}…"
    # Works on emulator or rooted device; on physical device use App Inspection instead
    adb shell "run-as ${PACKAGE_DEBUG} cat /data/data/${PACKAGE_DEBUG}/databases/${DB_NAME}" \
        > "${dest}" 2>/dev/null && \
        info "Saved to ${dest}. Open with DB Browser for SQLite or: sqlite3 '${dest}'" || \
        warn "Could not pull DB directly (device may not be rooted). Use Android Studio App Inspection instead."
}

cmd_devices() {
    command -v adb &>/dev/null || error "adb not found"
    echo ""
    adb devices -l
    echo ""
    info "For wireless ADB (Android 11+):"
    echo "  1. Settings → Developer Options → Wireless Debugging → Enable"
    echo "  2. Tap 'Pair device with pairing code' — note the IP, port, and code"
    echo "  3. Run: adb pair <ip>:<pairing-port>  (enter the code when prompted)"
    echo "  4. Run: adb connect <ip>:<connect-port>"
}

cmd_pair() {
    command -v adb &>/dev/null || error "adb not found"
    echo ""
    info "Wireless ADB pairing"
    echo "  On your Android device:"
    echo "  1. Settings → Developer Options → Wireless Debugging → Enable"
    echo "  2. Tap 'Pair device with pairing code'"
    echo "  3. Note the IP address, port, and 6-digit code shown on screen"
    echo ""
    read -rp "Enter IP:port shown under 'Pair device with pairing code': " pair_addr
    adb pair "${pair_addr}"
    echo ""
    read -rp "Enter IP:port shown under 'IP address & Port' (NOT the pairing port): " connect_addr
    adb connect "${connect_addr}"
    adb devices
}

cmd_clean() {
    project_root
    info "Cleaning build…"
    ./gradlew clean
}

cmd_help() {
    grep '^#' "$0" | sed 's/^# *//'
}

# ── dispatch ──────────────────────────────────────────────────────────────────
case "${1:-help}" in
    install)          cmd_install "${2:-}" ;;
    install-release)  cmd_install_release "${2:-}" ;;
    launch)           cmd_launch "${2:-}" ;;
    kill)             cmd_kill "${2:-}" ;;
    uninstall)        cmd_uninstall "${2:-}" ;;
    fresh)            cmd_fresh "${2:-}" ;;
    log)              cmd_log "${2:-}" ;;
    log-db)           cmd_log_db "${2:-}" ;;
    log-crash)        cmd_log_crash "${2:-}" ;;
    test)             cmd_test ;;
    test-class)       cmd_test_class "$2" ;;
    test-device)      cmd_test_device "${2:-}" ;;
    pull-exports)     cmd_pull_exports "${2:-}" ;;
    db)               cmd_db ;;
    db-pull)          cmd_db_pull "${2:-}" ;;
    devices)          cmd_devices ;;
    pair)             cmd_pair ;;
    clean)            cmd_clean ;;
    help|--help|-h)   cmd_help ;;
    *) error "Unknown command: $1. Run './scripts/dev.sh help' for usage." ;;
esac
