# API Design — RedN Farm App

**Created:** 2026-04-03  
**App:** `com.redn.farm` (Android / Sunmi V2 Pro POS)  
**Web counterpart:** Drupal-based web app (separate product)  
**Base URL pattern:** `https://api.rednfarm.com/api/v1`  
**Protocol:** HTTPS REST, JSON request/response bodies

---

## Conventions

### Authentication header
Every request (except `POST /auth/token`) must include:
```
Authorization: Bearer <jwt_access_token>
```

### Device identification header
Every request from the Android app must include:
```
X-Device-Id: <Settings.Secure.ANDROID_ID>
```
This is the same identifier already embedded in CSV exports via `DeviceUtils.getDeviceId()`. The server uses it to namespace device-originated records and resolve conflicts.

### Timestamps
All timestamp fields are **epoch milliseconds (Long)** — matching the Room schema. The server must store and return the same format. Never use ISO 8601 strings in API payloads for this app.

### Delta sync anchor
Endpoints that return lists accept an optional `?since=<epoch_millis>` query parameter. The server returns only records with `date_updated > since`. The device tracks the last successful sync time per table and passes it as `since` on the next call.

### Idempotency
All `POST /sync/*` endpoints are **upsert** operations — the server identifies records by `(device_id, local_id)` composite key. Re-uploading the same record is safe.

### Error shape
```json
{
  "error": "VALIDATION_FAILED",
  "message": "product_id is required",
  "field": "product_id"
}
```

### Pagination
List endpoints that may return large results support `?page=1&limit=100`. Default limit: 100. Max limit: 500.

---

## API Groups

| Group | Direction | Purpose |
|---|---|---|
| A — Auth | Both | Token issuance, refresh, logout |
| B — Device | Device → Web | Registration, heartbeat |
| C — Upload (Sync) | Device → Web | Push operational records to the server |
| D — Config Pull | Web → Device | Pull master data for offline configuration |
| E — Order Status | Web → Device | Server-initiated order state updates |
| F — Reporting | Web | Aggregate views for the web dashboard |
| G — Notifications | Web → Device | Lightweight change signals (preset, config) |
| H — Audit | Both | Sync log, activation history |

---

## Group A — Authentication

### POST /auth/token
Obtain an access token and refresh token.

**Request:**
```json
{
  "username": "admin",
  "password": "admin123",
  "device_id": "abc123def456"
}
```

**Response `200`:**
```json
{
  "access_token": "<jwt>",
  "refresh_token": "<opaque>",
  "expires_in": 3600,
  "user": {
    "user_id": 1,
    "username": "admin",
    "full_name": "Administrator",
    "role": "ADMIN"
  }
}
```

**Notes:**
- The server validates `username` + `password` against the same PBKDF2 hash stored in the web DB, **not** the local Room DB. The local DB is the fallback when the device is offline.
- Short `expires_in` (1 hour) + long-lived refresh token. The app silently refreshes before expiry.
- On first online login the server may push a `role` update — the app should reconcile the local `users` table if the role differs.

---

### POST /auth/refresh
Exchange a refresh token for a new access token.

**Request:**
```json
{ "refresh_token": "<opaque>" }
```

**Response `200`:** Same shape as `/auth/token` minus `refresh_token`.

---

### DELETE /auth/token
Revoke the current session (logout). The server invalidates the refresh token.

**Request:** No body. Uses `Authorization` header.

**Response `204`:** No content.

---

### GET /auth/me
Return the authenticated user's current profile and role (useful after config pull to verify role sync).

**Response `200`:**
```json
{
  "user_id": 1,
  "username": "admin",
  "full_name": "Administrator",
  "role": "ADMIN",
  "is_active": true
}
```

---

## Group B — Device Registration

### POST /devices
Register this device with the server. Called on first launch after login while online.

**Request:**
```json
{
  "device_id": "abc123def456",
  "device_name": "Sunmi V2 Pro #1",
  "app_version": "1.4.0",
  "db_version": 4
}
```

**Response `200`:**
```json
{
  "device_id": "abc123def456",
  "registered_at": 1743638400000
}
```

---

### PATCH /devices/{deviceId}
Heartbeat — called periodically (e.g. on app foreground) to update last-seen time and flag the device as reachable.

**Request:**
```json
{
  "app_version": "1.4.0",
  "db_version": 4,
  "last_sync_at": 1743638400000
}
```

**Response `200`:** Echo of the updated record.

---

## Group C — Upload (Sync: Device → Web)

All upload endpoints accept a **batch array** of records. The server upserts by `(device_id, local_id)`. The `device_id` comes from the `X-Device-Id` header — no need to repeat it in each record.

### POST /sync/orders
Upload orders and their line items together.

**Request:**
```json
{
  "records": [
    {
      "local_id": 42,
      "customer_id": 7,
      "channel": "online",
      "total_amount": 470.00,
      "order_date": 1743638400000,
      "order_update_date": 1743638400000,
      "is_paid": true,
      "is_delivered": false,
      "items": [
        {
          "local_item_id": 101,
          "product_id": "PROD001",
          "quantity": 5.0,
          "price_per_unit": 70.00,
          "is_per_kg": true,
          "total_price": 350.00
        }
      ]
    }
  ]
}
```

**Response `200`:**
```json
{
  "accepted": 1,
  "rejected": 0,
  "errors": []
}
```

---

### POST /sync/acquisitions
Upload acquisition records including all SRP columns and snapshot fields.

**Request:**
```json
{
  "records": [
    {
      "local_id": 88,
      "product_id": "PROD001",
      "product_name": "Tomatoes",
      "quantity": 100.0,
      "price_per_unit": 70.00,
      "total_amount": 7000.00,
      "is_per_kg": true,
      "piece_count": null,
      "date_acquired": 1743552000000,
      "created_at": 1743552000000,
      "location": "SUPPLIER",
      "preset_ref": "preset-uuid-here",
      "spoilage_rate": 0.25,
      "additional_cost_per_kg": 10.286,
      "srp_online_per_kg": 137.0,
      "srp_reseller_per_kg": 127.0,
      "srp_offline_per_kg": 132.0,
      "srp_online_500g": 69.0,
      "srp_reseller_500g": 64.0,
      "srp_offline_500g": 66.0,
      "srp_online_250g": 35.0,
      "srp_reseller_250g": 32.0,
      "srp_offline_250g": 33.0,
      "srp_online_100g": 14.0,
      "srp_reseller_100g": 13.0,
      "srp_offline_100g": 14.0,
      "srp_online_per_piece": null,
      "srp_reseller_per_piece": null,
      "srp_offline_per_piece": null,
      "channels_snapshot_json": "{...}",
      "hauling_fees_json": "[...]"
    }
  ]
}
```

---

### POST /sync/remittances
```json
{
  "records": [
    {
      "local_id": 15,
      "amount": 5000.00,
      "date": 1743638400000,
      "remarks": "Payment to hauler - April run",
      "date_updated": 1743638400000
    }
  ]
}
```

---

### POST /sync/farm-operations
```json
{
  "records": [
    {
      "local_id": 33,
      "operation_type": "HARVESTING",
      "operation_date": 1743638400000,
      "details": "Harvested approx 150kg",
      "area": "Block 3 / North Field",
      "weather_condition": "Sunny",
      "personnel": "Juan, Pedro, Maria",
      "product_id": "PROD001",
      "product_name": "Tomatoes",
      "date_created": 1743638400000,
      "date_updated": 1743638400000
    }
  ]
}
```

---

### POST /sync/employee-payments
```json
{
  "records": [
    {
      "local_id": 201,
      "employee_id": 5,
      "amount": 3000.00,
      "cash_advance_amount": 500.00,
      "liquidated_amount": 200.00,
      "date_paid": 1743638400000,
      "received_date": 1743724800000,
      "signature": "<base64_png_or_typed_name>"
    }
  ]
}
```

---

### POST /sync/customers
Upload new or updated customer records created on the device.

```json
{
  "records": [
    {
      "local_id": 9,
      "firstname": "Juan",
      "lastname": "dela Cruz",
      "contact": "09171234567",
      "customer_type": "RETAIL",
      "address": "123 Main St",
      "city": "Quezon City",
      "province": "Metro Manila",
      "postal_code": "1100",
      "date_created": 1743638400000,
      "date_updated": 1743638400000
    }
  ]
}
```

---

### POST /sync/batch
Single-trip alternative to the per-table endpoints above. Wraps all pending changes in one request. Preferred when re-connecting after extended offline use.

**Request:**
```json
{
  "orders": [ ... ],
  "acquisitions": [ ... ],
  "remittances": [ ... ],
  "farm_operations": [ ... ],
  "employee_payments": [ ... ],
  "customers": [ ... ],
  "synced_at": 1743638400000
}
```

**Response `200`:**
```json
{
  "results": {
    "orders":            { "accepted": 3, "rejected": 0 },
    "acquisitions":      { "accepted": 5, "rejected": 0 },
    "remittances":       { "accepted": 1, "rejected": 0 },
    "farm_operations":   { "accepted": 2, "rejected": 0 },
    "employee_payments": { "accepted": 4, "rejected": 0 },
    "customers":         { "accepted": 1, "rejected": 0 }
  },
  "server_time": 1743638405000
}
```

---

## Group D — Config Pull (Web → Device)

Used on login, on demand ("Sync now" button), or when a change signal (Group G) arrives. The device calls these to refresh its local master data.

### GET /config
Pull everything in one response. Recommended for initial setup and full refresh.

**Query params:** `?since=<epoch_millis>` — return only records changed after this time. Omit to get full set.

**Response `200`:**
```json
{
  "users": [ ... ],
  "products": [ ... ],
  "product_prices": [ ... ],
  "pricing_presets": [ ... ],
  "employees": [ ... ],
  "customers": [ ... ],
  "server_time": 1743638400000
}
```

---

### GET /config/users
Return all active user accounts. Used to create/update local `users` table entries so any device can authenticate offline.

**Response `200`:**
```json
{
  "users": [
    {
      "user_id": 1,
      "username": "admin",
      "password_hash": "<pbkdf2_hash>",
      "full_name": "Administrator",
      "role": "ADMIN",
      "is_active": true,
      "date_created": 1743638400000,
      "date_updated": 1743638400000
    }
  ]
}
```

**Notes:**
- `password_hash` is the same PBKDF2WithHmacSHA1 format the app already uses locally — the device replaces its local hash verbatim.
- Only `ADMIN` role users may call this endpoint. `STORE_ASSISTANT`, `FARMER`, `PURCHASING` receive their own row only.

---

### GET /config/products
Return the full product catalog with active prices.

**Response `200`:**
```json
{
  "products": [
    {
      "product_id": "PROD001",
      "product_name": "Tomatoes",
      "product_description": "Fresh from the farm",
      "unit_type": "kg",
      "category": "Vegetables",
      "default_piece_count": null,
      "is_active": true,
      "prices": [
        {
          "price_id": 12,
          "per_kg_price": 70.00,
          "per_piece_price": null,
          "discounted_per_kg_price": 60.00,
          "discounted_per_piece_price": null,
          "date_created": 1743638400000
        }
      ]
    }
  ]
}
```

---

### GET /config/pricing-presets
Return all pricing presets including the currently active one.

**Response `200`:**
```json
{
  "active_preset_id": "preset-uuid-here",
  "presets": [
    {
      "preset_id": "preset-uuid-here",
      "preset_name": "Q2 2026 Rates",
      "saved_at": 1743638400000,
      "saved_by": "admin",
      "is_active": true,
      "activated_at": 1743638400000,
      "activated_by": "admin",
      "spoilage_rate": 0.25,
      "additional_cost_per_kg": 10.286,
      "hauling_weight_kg": 700.0,
      "hauling_fees_json": "[...]",
      "channels_json": "{...}",
      "categories_json": "[]"
    }
  ]
}
```

---

### GET /config/employees
Return the employee list for the device's payroll screens.

**Response `200`:**
```json
{
  "employees": [
    {
      "employee_id": 5,
      "firstname": "Maria",
      "lastname": "Santos",
      "contact": "09181234567",
      "date_created": 1743638400000,
      "date_updated": 1743638400000
    }
  ]
}
```

---

### GET /config/customers
Return the customer list for offline order-taking. The device uses this to pre-populate the customer search on `TakeOrderScreen`.

**Query params:** `?since=<epoch_millis>`, `?page=1&limit=500`

**Response `200`:**
```json
{
  "customers": [
    {
      "customer_id": 7,
      "firstname": "Juan",
      "lastname": "dela Cruz",
      "contact": "09171234567",
      "customer_type": "RETAIL",
      "address": "123 Main St",
      "city": "Quezon City",
      "province": "Metro Manila",
      "postal_code": "1100",
      "date_created": 1743638400000,
      "date_updated": 1743638400000
    }
  ]
}
```

---

## Group E — Order Status Sync

Allows the web dashboard to mark orders as paid or delivered and have that update flow back to the device on next sync.

### GET /orders/pending-status-updates
Return orders whose `is_paid` or `is_delivered` status was changed on the web since the device's last sync.

**Query params:** `?since=<epoch_millis>&device_id=<id>`

**Response `200`:**
```json
{
  "updates": [
    {
      "local_id": 42,
      "device_id": "abc123def456",
      "is_paid": true,
      "is_delivered": true,
      "updated_at": 1743638400000
    }
  ]
}
```

The device applies these updates to its local `orders` table after a config pull or batch sync.

---

### PATCH /orders/{deviceId}/{localId}/status
Web operator marks an order as paid or delivered from the web dashboard.

**Request:**
```json
{
  "is_paid": true,
  "is_delivered": false
}
```

**Response `200`:** Updated order summary.

---

## Group F — Reporting

Read-only endpoints consumed by the Drupal web dashboard. Not called from the Android app.

### GET /reports/sales
Aggregate sales by period.

**Query params:** `?from=<epoch_millis>&to=<epoch_millis>&channel=online|reseller|offline&device_id=<id>`

**Response `200`:**
```json
{
  "period_from": 1743552000000,
  "period_to":   1743638400000,
  "total_orders": 24,
  "total_amount": 48250.00,
  "by_channel": {
    "online":   { "orders": 10, "amount": 22000.00 },
    "reseller": { "orders":  8, "amount": 18000.00 },
    "offline":  { "orders":  6, "amount":  8250.00 }
  },
  "by_product": [
    { "product_id": "PROD001", "product_name": "Tomatoes", "total_qty_kg": 75.5, "total_amount": 28450.00 }
  ]
}
```

---

### GET /reports/acquisitions
Procurement cost summary by period and supplier.

**Query params:** `?from=<epoch_millis>&to=<epoch_millis>&location=SUPPLIER|FARM|MARKET`

**Response `200`:**
```json
{
  "total_cost": 84250.00,
  "total_qty_kg": 950.0,
  "avg_cost_per_kg": 88.68,
  "by_product": [
    { "product_id": "PROD001", "qty_kg": 500.0, "total_cost": 35000.00 }
  ]
}
```

---

### GET /reports/payroll
Employee payment totals by period.

**Query params:** `?from=<epoch_millis>&to=<epoch_millis>&employee_id=<id>`

**Response `200`:**
```json
{
  "total_gross": 48000.00,
  "total_advances": 8000.00,
  "total_net_pay": 56000.00,
  "by_employee": [
    {
      "employee_id": 5,
      "name": "Maria Santos",
      "gross": 12000.00,
      "advance": 2000.00,
      "net_pay": 14000.00
    }
  ]
}
```

---

### GET /reports/farm-operations
Operations log summary by type and date.

**Query params:** `?from=<epoch_millis>&to=<epoch_millis>&type=HARVESTING|SOWING|...`

**Response `200`:**
```json
{
  "total_operations": 18,
  "by_type": {
    "HARVESTING": 6,
    "SOWING": 4,
    "PESTICIDE_APPLICATION": 8
  },
  "operations": [ { "operation_id": 33, "type": "HARVESTING", ... } ]
}
```

---

## Group G — Change Notifications

Lightweight polling alternative to a full config pull. The app calls this on foreground resume to check if anything has changed before deciding whether to do a full pull.

### GET /config/version
Return a version token for each config domain. The device compares against its locally cached versions; if any differs, it triggers a targeted pull.

**Response `200`:**
```json
{
  "users_version":    "v-1743638400000",
  "products_version": "v-1743600000000",
  "presets_version":  "v-1743552000000",
  "employees_version":"v-1743500000000",
  "customers_version":"v-1743638000000"
}
```

The version token is the `MAX(date_updated)` over that table, returned as a string. The device stores the last-seen version per domain; if a domain version differs it calls the targeted `GET /config/{domain}` endpoint.

---

### POST /notifications/subscribe *(optional — push alternative)*
Register a Firebase Cloud Messaging (FCM) token so the server can push a silent notification when the active pricing preset changes. The device wakes up and calls `GET /config/pricing-presets` to pull the update.

**Request:**
```json
{
  "device_id": "abc123def456",
  "fcm_token": "<firebase_token>"
}
```

**Response `200`:** `{ "subscribed": true }`

**Events that trigger a push:**
- Pricing preset activated
- New product added or deactivated
- User account deactivated

---

## Group H — Audit

### GET /audit/sync-log
Return a log of all sync events from all devices. Admin only.

**Query params:** `?device_id=<id>&from=<epoch_millis>&to=<epoch_millis>`

**Response `200`:**
```json
{
  "log": [
    {
      "sync_id": "uuid",
      "device_id": "abc123def456",
      "synced_at": 1743638400000,
      "tables": {
        "orders": { "accepted": 3 },
        "acquisitions": { "accepted": 5 }
      }
    }
  ]
}
```

---

### GET /audit/preset-activations
Return the preset activation log — mirrors `preset_activation_log` from the device DB, consolidated server-side.

**Response `200`:**
```json
{
  "log": [
    {
      "log_id": 1,
      "activated_at": 1743638400000,
      "activated_by": "admin",
      "preset_id_activated": "preset-uuid-here",
      "preset_id_deactivated": null
    }
  ]
}
```

---

## Conflict Resolution

| Scenario | Resolution |
|---|---|
| Same order uploaded from two devices | Server keeps both; `(device_id, local_id)` is unique — no collision |
| Web dashboard changes `is_paid` on an order the device also updated | **Last write wins** by `updated_at`; device pulls reconciled state on next Group E sync |
| Web changes a product's price; device has offline orders at old price | Order items store `price_per_unit` at capture time — no retro-reconciliation |
| Web deactivates a product the device still has in a cart | Device-side cart is transient; product deactivation takes effect on next config pull — in-flight carts complete normally |
| Preset activated on web while device is offline | Device continues using its locally stored active preset; on reconnect `GET /config/pricing-presets` replaces it and new acquisitions use the updated preset |

---

## Android Implementation Notes

### Retrofit service interface (suggested)
```kotlin
interface FarmApiService {
    @POST("auth/token")
    suspend fun login(@Body req: LoginRequest): TokenResponse

    @GET("config")
    suspend fun pullConfig(@Query("since") since: Long?): ConfigResponse

    @POST("sync/batch")
    suspend fun syncBatch(@Body batch: SyncBatchRequest): SyncBatchResponse

    @GET("config/version")
    suspend fun getConfigVersion(): ConfigVersionResponse

    @GET("orders/pending-status-updates")
    suspend fun getPendingStatusUpdates(@Query("since") since: Long): StatusUpdatesResponse
}
```

### Suggested new files
| File | Purpose |
|---|---|
| `data/remote/FarmApiService.kt` | Retrofit interface |
| `data/remote/ApiClient.kt` | OkHttpClient + Retrofit builder (auth interceptor, device-id header) |
| `data/remote/dto/` | Request/response data classes mirroring the shapes above |
| `data/repository/SyncRepository.kt` | Orchestrates `POST /sync/batch` — reads pending local records, posts, marks synced |
| `data/repository/ConfigRepository.kt` | Orchestrates `GET /config` pulls, writes to Room |
| `di/NetworkModule.kt` | Hilt module providing `FarmApiService` |
| `ui/screens/sync/SyncScreen.kt` | "Sync now" screen — shows last sync time, pending count, manual trigger |

### Sync state tracking
Add a `sync_state` table (or `SharedPreferences`) with one row per table:
```
table_name    TEXT PK
last_synced_at  INTEGER   -- epoch millis of last successful upload
last_pulled_at  INTEGER   -- epoch millis of last successful config pull
pending_count   INTEGER   -- count of local records not yet uploaded
```
The `SyncRepository` reads `pending_count` and shows a badge on the main screen when the device is online but has unsynced records.
