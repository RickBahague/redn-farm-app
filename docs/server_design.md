# Server-Side Application Design — RedN Farm

**Version:** 0.1-draft  
**Date:** 2026-04-06  
**Companion documents:** `docs/apis.md`, `docs/DESIGN.md`, `docs/user_roles.md`, `docs/PricingReference.md`  
**Base URL pattern:** `https://api.rednfarm.com/api/v1`

---

## 0. Purpose and Goals

The server application consolidates data from multiple Android devices (Sunmi V2 Pro POS units) and provides:

1. **Centralized source of truth** — Products, pricing presets, employees, and user accounts are managed on the server. Devices pull updates on login and periodically.
2. **Data consolidation** — Operational records (orders, acquisitions, farm operations, remittances, employee payments) uploaded from devices are aggregated into a single queryable store.
3. **Reporting and business observability** — Aggregate queries over consolidated data feed dashboards, trend analysis, and management reporting.
4. **Configuration broadcast** — Changes made on the server (new preset activated, product added) propagate to all registered devices via pull polling or push notifications.
5. **Audit trail** — All sync events, preset activations, and config changes are logged with device, user, and timestamp.
6. **Device provenance** — Every operational record received from a device is permanently stamped with the originating device ID, the authenticated user who performed the sync, and the server receipt time. This ensures every piece of data can be traced back to the exact device and user that produced it, regardless of when it was synced.

The Android app remains **offline-first**. The server supplements — it is not required for day-to-day operations on the device. A device with no connectivity continues using its locally cached config and queues operational records for upload when reconnected.

---

## 1. Platform Choice: Drupal

The server is implemented as a set of **custom Drupal modules** on a standard Drupal 10/11 installation.

**Why Drupal:**
- Entity API provides structured storage with built-in CRUD, validation, and revision history — maps cleanly to config entities (products, presets) and content entities (synced records).
- Drupal's user system handles authentication, roles, and permissions out of the box; can be extended to issue JWT tokens via `simple_oauth` or a custom endpoint.
- Admin UI (Drupal admin panel) gives non-developer operators a web interface to manage products, employees, pricing presets, and users without building a separate frontend.
- REST and JSON:API contrib modules provide a foundation; custom REST resources override or extend them for the farm-specific API shapes defined in `apis.md`.
- Proven multi-site deployment, caching (Internal Page Cache, Dynamic Page Cache), and hook/event system.

**Limitations / mitigations:**
- Drupal's default REST module returns JSON:API-shaped responses which do not match `apis.md` payloads. Custom REST resource plugins (`\Drupal\rest\Plugin\ResourceBase` subclasses) produce the exact shapes specified in `apis.md`.
- JWT issuance requires `simple_oauth` contrib or a custom `farm_api` implementation — either is acceptable; `simple_oauth` is preferred for maintained token storage.

---

## 2. Module Architecture

All farm modules live under `web/modules/custom/` with the namespace prefix `farm_`.

```
farm_core          — shared utilities, base entity types, timestamp helpers
farm_api           — REST endpoint plugins (Groups A–H from apis.md)
farm_config_mgr    — config entities: products, pricing presets, employees
farm_sync          — sync processing: upsert logic, conflict resolution, sync log
farm_reports       — aggregate query service + reporting endpoint plugins (Group F)
farm_notifications — config version cache, FCM push dispatch
farm_rbac          — role definitions, permission map aligned with user_roles.md
```

### Dependency graph

```
farm_config_mgr ──┐
farm_sync       ──┼──► farm_core
farm_reports    ──┘         ▲
farm_notifications ──────────┤
farm_api ────────────────────┘
farm_rbac ──────────────────►  (no dependencies beyond Drupal user module)
```

`farm_api` depends on all other modules — it is the HTTP surface only; business logic lives in the respective module's service classes.

---

## 3. Entity Model

### 3.1 Config Entities (server owns, device pulls)

These are Drupal **config entities** — managed in the admin UI, versioned, and exportable. They constitute the canonical source of truth that devices pull via Group D endpoints.

#### `farm_product`
| Field | Type | Notes |
|---|---|---|
| `product_id` | string (machine name) | Matches Android `product_id`, e.g. `PROD001` |
| `product_name` | string | |
| `product_description` | text | |
| `unit_type` | string | `kg` or `piece` |
| `category` | string | Optional grouping |
| `default_piece_count` | float? | Estimated pieces per kg — carried to Android |
| `is_active` | boolean | Soft delete; deactivated products excluded from config pull |
| `date_created` | bigint | Epoch millis |
| `date_updated` | bigint | Epoch millis; drives `products_version` in Group G |

#### `farm_product_price`
Append-only price history. Never updated in place (mirrors Android `product_prices` table behavior).

| Field | Type |
|---|---|
| `price_id` | serial |
| `product_id` | FK → `farm_product` |
| `per_kg_price` | decimal? |
| `per_piece_price` | decimal? |
| `discounted_per_kg_price` | decimal? |
| `discounted_per_piece_price` | decimal? |
| `date_created` | bigint |

Latest price per product = `MAX(date_created)` per `product_id`.

#### `farm_pricing_preset`
Stores the full pricing configuration snapshot. One row is marked `is_active`.

| Field | Type | Notes |
|---|---|---|
| `preset_id` | uuid (string) | Shared key between server and device |
| `preset_name` | string | |
| `spoilage_rate` | decimal | e.g. `0.25` |
| `additional_cost_per_kg` | decimal | Hauling cost rate |
| `hauling_weight_kg` | decimal | |
| `hauling_fees_json` | text | JSON array of hauling fee line items |
| `channels_json` | text | JSON object per SalesChannel — markup%, fees, rounding |
| `categories_json` | text | Optional category-specific overrides |
| `is_active` | boolean | Only one preset is active at a time |
| `activated_at` | bigint? | Epoch millis of last activation |
| `activated_by` | string? | Username |
| `saved_at` | bigint | |
| `saved_by` | string | |
| `date_updated` | bigint | Drives `presets_version` in Group G |

Activation is an **explicit action** (not just toggling `is_active`) — it logs a row to `farm_preset_activation_log` and triggers a Group G version bump. At most one preset may be active; activating one deactivates the prior one.

#### `farm_employee`
| Field | Type |
|---|---|
| `employee_id` | serial |
| `firstname` | string |
| `lastname` | string |
| `contact` | string |
| `is_active` | boolean |
| `date_created` | bigint |
| `date_updated` | bigint |

#### `farm_customer`
Bidirectional — devices upload new customers (Group C), server makes them available to all devices (Group D). The server is the canonical deduplication point.

| Field | Type | Notes |
|---|---|---|
| `customer_id` | serial | Server-assigned canonical ID |
| `firstname`, `lastname` | string | |
| `contact` | string | Uniqueness hint (not hard constraint) |
| `customer_type` | enum | REGULAR / RETAIL / WHOLESALE |
| address fields | string | |
| `date_created` | bigint | |
| `date_updated` | bigint | Drives `customers_version` |
| `source_device_id` | string? | Device that created the record |

---

### 3.2 Operational Records (device uploads, server aggregates)

These are **content entities** — uploaded via Group C sync endpoints, stored verbatim with provenance metadata appended. Records are **never deleted and never overwritten** — every sync and resync produces a new row.

### Versioning model

Every `farm_synced_*` table uses an **append-only versioning model**:

- Each submission of a `(device_id, local_id)` pair is inserted as a new row with an incrementing `submission_seq`.
- Exactly one row per `(device_id, local_id)` is marked `is_canonical = true` — this is the version used by all reporting and analytics queries.
- When a resync arrives, the previous canonical row's `is_canonical` is set to `false` and `superseded_at` is set to the server's current time; the new row is inserted as `is_canonical = true`.
- The `is_canonical` flag is the **only mutable field** on a synced row after insertion. All other fields — including provenance stamps — are immutable.
- A partial unique index `UNIQUE (device_id, local_id) WHERE is_canonical = true` enforces that at most one canonical version exists per record at any time.

**Why append-only:** Re-uploading a record (after a device reset, data recovery, or correction) must not silently replace what the server already holds. Every submission is evidence — the audit trail must show that a record was re-sent, when, by whom, and what changed.

### Resync reason codes

Each submission carries a `resync_reason` that explains the context:

| Code | Meaning |
|---|---|
| `INITIAL` | First time this `(device_id, local_id)` has been seen by the server |
| `RECONNECT` | Routine re-upload after offline period; payload is identical to the canonical version |
| `CORRECTION` | Device re-submitted with changed field values (server detects diff vs. current canonical) |
| `RECOVERY` | Bulk resync triggered after device reset, app reinstall, or data loss |
| `FORCED` | Admin-initiated full resync from the web management panel |

The device may include `"resync_reason": "<code>"` at the top level of the batch payload. If omitted, the server infers the reason: `INITIAL` for first submissions; `RECONNECT` vs. `CORRECTION` by comparing payload against the current canonical row.

### Canonical views

All reporting, analytics, and Group F endpoints query **named views** that filter `WHERE is_canonical = true`. Raw tables are never queried directly by reporting code:

```sql
farm_canonical_orders           → SELECT * FROM farm_synced_orders WHERE is_canonical = true
farm_canonical_acquisitions     → SELECT * FROM farm_synced_acquisitions WHERE is_canonical = true
farm_canonical_remittances      → SELECT * FROM farm_synced_remittances WHERE is_canonical = true
farm_canonical_farm_operations  → SELECT * FROM farm_synced_farm_ops WHERE is_canonical = true
farm_canonical_employee_payments → SELECT * FROM farm_synced_emp_payments WHERE is_canonical = true
```

Audit and provenance queries use the raw tables directly and can see all versions.

### Version fields on every `farm_synced_*` table

In addition to the provenance stamps from §3.4:

| Field | Type | Notes |
|---|---|---|
| `submission_seq` | integer | Starts at 1; incremented per `(device_id, local_id)` on each resync |
| `resync_reason` | enum | `INITIAL / RECONNECT / CORRECTION / RECOVERY / FORCED` |
| `is_canonical` | boolean | `true` = used for reporting; only one per `(device_id, local_id)` |
| `superseded_at` | bigint? | Server time when this row was displaced by a newer submission; null while canonical |
| `superseded_by_seq` | integer? | `submission_seq` of the row that superseded this one; null while canonical |

#### `farm_synced_order`
| Field | Source | Mutable? |
|---|---|---|
| `server_id` | serial (server-assigned) | — |
| `device_id` | `X-Device-Id` header — **provenance: device** | No |
| `synced_by_user` | JWT subject at sync time — **provenance: user** | No |
| `synced_at` | server clock at acceptance — **provenance: receipt time** | No |
| `submission_seq` | server-assigned; 1 on first sync, +1 on each resync | No |
| `resync_reason` | server-inferred or device-supplied | No |
| `is_canonical` | `true` = used for reporting | **Yes** — toggled on supersession |
| `superseded_at` | server time when displaced by newer submission | **Yes** — set on supersession |
| `superseded_by_seq` | `submission_seq` of superseding row | **Yes** — set on supersession |
| `local_id` | from payload | No |
| `customer_id` | from payload | No |
| `channel` | `online / reseller / offline` | No |
| `total_amount` | decimal | No |
| `order_date` | bigint — origin time on device | No |
| `order_update_date` | bigint | No |
| `is_paid` | boolean; **mutable via Group E on canonical row only** | Yes (Group E only) |
| `is_delivered` | boolean; **mutable via Group E on canonical row only** | Yes (Group E only) |
| `status_updated_at` | bigint; set when Group E write occurs | Yes (Group E only) |
| `status_updated_by_user` | web admin username that triggered the Group E write | Yes (Group E only) |

#### `farm_synced_order_item`
| Field | Notes |
|---|---|
| `server_id` | serial |
| `order_server_id` | FK → `farm_synced_order.server_id` |
| `local_item_id` | from payload |
| `product_id` | string |
| `quantity` | decimal |
| `price_per_unit` | decimal — captured at order time, never retroactively changed |
| `is_per_kg` | boolean |
| `total_price` | decimal |

#### `farm_synced_acquisition`
All SRP snapshot columns carried verbatim — they are the computed result at capture time and must not be recalculated server-side.

| Field | Notes |
|---|---|
| `server_id` | serial |
| `device_id` | `X-Device-Id` header — **provenance: device** |
| `synced_by_user` | JWT subject at sync time — **provenance: user** |
| `synced_at` | server clock at acceptance — **provenance: receipt time** |
| `local_id` | device-local acquisition ID; composite key with `device_id` |
| `product_id`, `product_name` | |
| `quantity` | kg if `is_per_kg`; total pieces if not |
| `price_per_unit`, `total_amount` | |
| `is_per_kg`, `piece_count` | per CLARIF-01 / PricingReference §5.1.1 |
| `date_acquired`, `created_at` | epoch millis |
| `location` | FARM / MARKET / SUPPLIER / OTHER |
| `preset_ref` | UUID of pricing preset used |
| `spoilage_rate` | preset snapshot — audit only for per-piece rows |
| `additional_cost_per_kg` | preset snapshot |
| `srp_online_per_kg … srp_offline_per_piece` | all 12 SRP columns |
| `srp_online_100g … srp_offline_100g` | 3 pack columns |
| `srp_custom_override` | boolean |
| `channels_snapshot_json` | preset channels at capture time |
| `hauling_fees_json` | |
| `synced_at` | server receipt time |

#### `farm_synced_remittance`
Provenance fields: `device_id`, `synced_by_user`, `synced_at` (server-stamped).  
Payload fields: `local_id`, `amount`, `date`, `remarks`, `date_updated`.

#### `farm_synced_farm_operation`
Provenance fields: `device_id`, `synced_by_user`, `synced_at` (server-stamped).  
Payload fields (mirror `farm_operations`): `local_id`, `operation_type`, `operation_date`, `details`, `area`, `weather_condition`, `personnel`, `product_id?`, `product_name`, `date_created`, `date_updated`.

#### `farm_synced_employee_payment`
Provenance fields: `device_id`, `synced_by_user`, `synced_at` (server-stamped).  
Payload fields (mirror `employee_payments`): `local_id`, `employee_id`, `amount`, `cash_advance_amount?`, `liquidated_amount?`, `date_paid`, `received_date?`, `signature`.

---

### 3.3 Device Registry

#### `farm_device`
| Field | Notes |
|---|---|
| `device_id` | string PK — `Settings.Secure.ANDROID_ID` |
| `device_name` | human label |
| `app_version` | last reported |
| `db_version` | last reported |
| `registered_at` | epoch millis |
| `registered_by_user` | username of the account that performed first registration |
| `last_seen_at` | updated on heartbeat |
| `last_sync_at` | from heartbeat payload |
| `last_sync_user` | username of the account that last performed a sync from this device |
| `is_active` | admin can deactivate rogue devices |
| `deactivated_at` | epoch millis; set when admin deactivates |
| `deactivated_by_user` | admin username that deactivated |

**Deactivation policy:** Deactivating a device does not delete its historical records. All previously accepted records remain in the synced tables with their original `device_id`. New upload attempts from a deactivated device are rejected with `403 DEVICE_INACTIVE`. This allows historical queries to continue while stopping further data ingestion from a device that is lost, replaced, or compromised.

---

### 3.4 Device Provenance Model

Every operational record the server accepts carries **four provenance stamps**. These are non-nullable on all `farm_synced_*` tables.

| Stamp | Field | Set by | Meaning |
|---|---|---|---|
| Device identity | `device_id` | `X-Device-Id` header (enforced by auth middleware) | Which physical device produced the data |
| Authenticated user | `synced_by_user` | JWT subject at sync time | Which account was logged in on the device when the sync was performed |
| Server receipt time | `synced_at` | Server clock at record acceptance | When the server accepted the record — independent of device clock |
| Record origin time | `date_created` / `date_acquired` / `date_paid` (table-specific) | Device payload | When the event was recorded on the device (may differ from `synced_at` for offline queues) |

**Why `synced_by_user` matters separately from `device_id`:** A device may be shared among multiple staff members across shifts. The same Sunmi POS may be used by one store assistant in the morning and another in the afternoon. `device_id` identifies the hardware; `synced_by_user` identifies the person who authenticated and pressed "Sync" — these may differ and both are needed for accountability.

**Registration gate:** A device that is not registered in `farm_device` cannot upload data. The auth middleware calls `DeviceRegistrationService::assertDeviceActive($device_id)` before passing control to any Group C endpoint. Unknown or inactive devices receive `403 DEVICE_NOT_REGISTERED` or `403 DEVICE_INACTIVE` respectively, and the request body is discarded without being processed.

**Child records:** Order items (`farm_synced_order_items`) inherit provenance from their parent order via `order_server_id`. They do not carry duplicate provenance columns — the join to `farm_synced_orders` provides full provenance context.

**Provenance in queries:** Every reporting and admin view filters or groups by `device_id` and `synced_by_user`. No aggregated result is presented without the ability to drill down to the originating device and user.

---

### 3.5 Audit Entities

#### `farm_sync_log`
One row per `POST /sync/batch` or per-table sync call.

| Field | Notes |
|---|---|
| `sync_id` | uuid |
| `device_id` | from `X-Device-Id` header |
| `synced_by_user` | from JWT subject |
| `synced_at` | server clock |
| `ip_address` | remote IP for additional traceability |
| `app_version` | from device heartbeat / registration |
| `resync_reason_requested` | top-level `resync_reason` from batch payload (if supplied) |
| `tables_json` | `{ "orders": {"accepted":5, "new":3, "resynced":2, "rejected":0}, ... }` |

#### `farm_record_provenance_summary` (view / materialized)
A denormalized summary built as a database view over all `farm_synced_*` tables. Used by the admin Device Activity panel and provenance audit queries.

| Column | Source |
|---|---|
| `device_id` | synced table |
| `device_name` | joined from `farm_device` |
| `synced_by_user` | synced table |
| `table_name` | literal string per source table |
| `record_count` | COUNT per (device_id, synced_by_user, table_name) |
| `earliest_record_date` | MIN(date_created / date_acquired / date_paid) |
| `latest_record_date` | MAX same |
| `latest_synced_at` | MAX(synced_at) |

#### `farm_preset_activation_log`
| Field |
|---|
| `log_id` (serial) |
| `activated_at` |
| `activated_by` |
| `preset_id_activated` |
| `preset_id_deactivated?` |

---

## 4. API Layer (farm_api module)

All endpoints are implemented as **Drupal REST resource plugins** under `farm_api/src/Plugin/rest/resource/`. Each plugin maps to one API group from `apis.md`. The shapes, field names, and timestamp format (epoch millis) defined in `apis.md` are the contract — the plugins produce and consume exactly those shapes.

### Routing summary

| Group | Plugin class | Path prefix |
|---|---|---|
| A — Auth | `AuthTokenResource`, `AuthRefreshResource`, `AuthMeResource` | `/api/v1/auth` |
| B — Device | `DeviceResource` | `/api/v1/devices` |
| C — Upload | `SyncOrdersResource`, `SyncAcquisitionsResource`, `SyncBatchResource`, … | `/api/v1/sync` |
| D — Config | `ConfigResource`, `ConfigProductsResource`, `ConfigPresetsResource`, … | `/api/v1/config` |
| E — Order Status | `OrderStatusResource` | `/api/v1/orders` |
| F — Reporting | `ReportSalesResource`, `ReportAcquisitionsResource`, … | `/api/v1/reports` |
| G — Notifications | `ConfigVersionResource`, `NotificationsSubscribeResource` | `/api/v1/config/version`, `/api/v1/notifications` |
| H — Audit | `AuditSyncLogResource`, `AuditPresetActivationsResource` | `/api/v1/audit` |

### Authentication middleware

`farm_api` registers a **request subscriber** (`FarmApiAuthSubscriber`) that:
1. Skips `POST /api/v1/auth/token` (public).
2. Validates `Authorization: Bearer <jwt>` on all other requests.
3. Requires `X-Device-Id` header for all Group B–G requests originating from the Android app.
4. Calls `DeviceRegistrationService::assertDeviceActive($device_id)` — rejects with `403 DEVICE_NOT_REGISTERED` if the device has never registered, or `403 DEVICE_INACTIVE` if it has been deactivated by an admin.
5. Loads the Drupal user tied to the JWT and stores it in the request attributes alongside the validated device ID; downstream resource plugins read both via `$request->attributes->get('farm_user')` and `$request->attributes->get('farm_device_id')`.

The combination of validated JWT (user identity) and validated `X-Device-Id` (device identity) together form the provenance context that is stamped onto every accepted record.

JWT issuance and validation use **`simple_oauth`** contrib module with a custom `password` grant handler that verifies PBKDF2WithHmacSHA1 hashes (same algorithm as the Android app).

### Permission checks

Each resource plugin's `permissions()` method returns a Drupal permission string defined by `farm_rbac`. Permission checks are layered:

| Layer | Where |
|---|---|
| HTTP method gate | `permissions()` on the resource plugin |
| Role check | `FarmRbacService::assertPermission($user, $action)` called in plugin `get()`/`post()` |
| Row-level filter | Config pull endpoints filter `is_active` rows; user pull filters by `role` unless admin |

---

## 5. Sync Logic (farm_sync module)

### 5.1 Upload (Device → Server)

**Entry point:** `SyncService::processBatch(array $payload, string $deviceId, string $syncedByUser)`

Both `$deviceId` (from `X-Device-Id` header) and `$syncedByUser` (from JWT subject) are passed in from the auth middleware — they are never accepted from the payload body.

For each record in each table:
1. **Validate** required fields; reject and add to `errors[]` if invalid.
2. **Query** existing rows for this `(device_id, local_id)` to get current `MAX(submission_seq)` and find the current canonical row.
3. **Determine resync reason:**
   - No existing rows → `INITIAL`
   - Rows exist, payload fields are byte-for-byte identical to canonical → `RECONNECT`
   - Rows exist, payload fields differ from canonical → `CORRECTION`
   - Batch top-level `resync_reason` is `RECOVERY` or `FORCED` → use that value regardless
4. **INSERT** a new row with:
   - `submission_seq = previous_max + 1` (or `1` for `INITIAL`)
   - `resync_reason` from step 3
   - `is_canonical = true`
   - `superseded_at = null`, `superseded_by_seq = null`
   - Provenance stamps: `device_id`, `synced_by_user`, `synced_at = server_now()` — all from auth context
5. **Supersede** the previous canonical row (if any):
   - `UPDATE SET is_canonical = false, superseded_at = server_now(), superseded_by_seq = new_seq WHERE server_id = old_canonical_server_id`
6. Increment `accepted` counter; track `new` (INITIAL) vs `resynced` (any other reason) sub-counts.
7. After all tables processed, **write `farm_sync_log`** with device, user, time, and per-table counts including resync breakdown.

**No record is ever deleted or overwritten.** The only fields modified after insert are `is_canonical`, `superseded_at`, and `superseded_by_seq` on rows being displaced. All payload data and all provenance stamps are immutable per row.

**Customer deduplication:** When a device uploads a new customer (`firstname + lastname + contact` match an existing server record), the server returns a `409 CONFLICT` with `canonical_customer_id`. The device should reconcile its local record in a future config pull.

### 5.2 Config Pull (Server → Device)

`ConfigService::buildConfigPayload(int $since, string $role)`:
1. Query each config entity table where `date_updated > $since`.
2. Apply role filter: non-admin users receive only their own user row from `config/users`.
3. Serialize to the shape defined in `apis.md` Group D.
4. Return `server_time` = current epoch millis so the device updates its `last_pulled_at`.

### 5.3 Conflict Resolution

| Scenario | Resolution |
|---|---|
| Same `(device_id, local_id)` uploaded twice, same data | New row inserted as `RECONNECT`; previous canonical superseded. Both rows stored. Analytics see only the new canonical — same values, so no impact on counts or totals. |
| Same `(device_id, local_id)` uploaded twice, data differs | New row inserted as `CORRECTION`; previous canonical superseded. Analytics use the corrected values. Full diff is queryable in audit. |
| Web marks order paid; device also updated order | Group E writes `is_paid / is_delivered` on the **current canonical row** only. Non-canonical rows retain their original values for audit. On next Group E pull, device receives current canonical state. |
| Two devices create customers with same contact | Server accepts both; dedup is advisory (409 hint) not enforced |
| Preset activated on server while device offline | Device continues with cached preset; on reconnect `GET /config/pricing-presets` or `GET /config/version` detects the change and the device pulls the new preset |
| Product deactivated on server | In-flight device carts complete normally; next config pull sets `is_active = false` locally; product hidden from new orders |
| Admin pins older submission as canonical | Admin can set `is_canonical = true` on a specific `submission_seq` via the audit panel; current canonical is superseded. Used for data recovery when the latest sync was known-bad. |

### 5.4 Order Status Feedback Loop (Group E)

When an admin marks an order paid or delivered via the web dashboard:
1. `farm_sync` updates `farm_synced_order.is_paid / is_delivered` and sets `status_updated_at`.
2. On the device's next sync (`GET /orders/pending-status-updates?since=...&device_id=...`), the server returns changed rows.
3. The device writes the update to its local `orders` table.

This is a **one-way feedback path** (web → device) for status only. The web does not modify any other fields of uploaded operational records. Group E writes target the canonical row only.

### 5.5 Resync Policy

**Resyncs are allowed at any time and for any table.** There is no server-side restriction on re-uploading previously synced records. The sync endpoint does not distinguish an initial sync from a resync at the HTTP level — the versioning logic in `SyncService` handles classification automatically.

#### When resyncs occur

| Trigger | Resync reason | Who initiates |
|---|---|---|
| App reconnects after offline period, queued records re-sent | `RECONNECT` | Device (automatic) |
| User edits a record on-device and syncs again | `CORRECTION` | Device (automatic) |
| Device was reset / app reinstalled; full local DB restored from export and synced | `RECOVERY` | Device (user action) |
| Admin presses "Force full resync" in the web panel for a specific device | `FORCED` | Web admin |
| Device sends same batch twice due to network timeout/retry | `RECONNECT` | Device (automatic) |

#### Batch response for resyncs

The sync response includes a sub-count distinguishing new records from resyncs:

```json
{
  "results": {
    "orders": { "accepted": 5, "new": 3, "resynced": 2, "rejected": 0 },
    "acquisitions": { "accepted": 8, "new": 6, "resynced": 2, "rejected": 0 }
  },
  "server_time": 1743638405000
}
```

The device can display this breakdown in the Sync screen to give the operator visibility into whether records were fresh or resubmitted.

#### What "no duplicates in analysis" means

"No duplicates" does not mean preventing resyncs — it means ensuring analytics always query the **canonical layer**:

1. All reporting endpoints and Group F queries use the canonical views (`farm_canonical_orders`, etc.) which filter `WHERE is_canonical = true`.
2. Aggregate functions (SUM, COUNT) applied to canonical views produce correct totals regardless of how many times a record has been resynced.
3. The raw `farm_synced_*` tables are **never queried directly** by reporting code — only by audit and provenance queries.
4. Every canonical view is a simple filtered view of its raw table, not a copy — there is no ETL or batch job needed to maintain it. The `is_canonical` flag is kept consistent transactionally in `SyncService`.

#### Audit queries vs. analytics queries

| Query type | Table / view | Purpose |
|---|---|---|
| Analytics / reporting | `farm_canonical_*` views | Counts, totals, trends — no duplicates |
| Full submission history for a record | `farm_synced_orders WHERE device_id = ? AND local_id = ?` | See all versions with timestamps |
| Diff between versions | Compare rows by `submission_seq` | What changed on a correction |
| Resync frequency per device | `farm_synced_orders GROUP BY device_id, resync_reason` | Detect abnormal resync patterns |
| Records still pending canonical selection | `farm_synced_orders WHERE is_canonical = false AND superseded_by_seq IS NULL` | Should be empty in normal operation; if not, indicates a bug in `SyncService` |

#### Admin canonical override

An admin can manually set any `submission_seq` as canonical for a given `(device_id, local_id)` pair from `/admin/farm/audit/record-versions`. This is the data recovery path when the latest submission is known to be bad (e.g., device uploaded corrupted data). The override:
1. Sets the chosen row's `is_canonical = true`, clears `superseded_at` and `superseded_by_seq`.
2. Sets all other rows for that `(device_id, local_id)` to `is_canonical = false`, `superseded_at = now()`.
3. Logs the override to `farm_sync_log` with `resync_reason = ADMIN_OVERRIDE` and the admin's username.

---

## 6. Config Management Web UI

Drupal's admin panel (`/admin`) provides the management interface for:

| Feature | Admin path | Notes |
|---|---|---|
| Product catalog | `/admin/farm/products` | Add, edit, deactivate products; set prices (inserts new price row) |
| Pricing presets | `/admin/farm/presets` | Create presets, configure channels, **activate** (triggers version bump + optional FCM push) |
| Employee list | `/admin/farm/employees` | Add, edit, deactivate employees |
| User accounts | `/admin/farm/users` | Create accounts, assign roles (all 5 farm roles), reset passwords, deactivate |
| Device registry | `/admin/farm/devices` | View registered devices, last-seen time, last sync time, `last_sync_user`; deactivate |
| Order status | `/admin/farm/orders` | View consolidated order list (canonical only); mark paid / delivered |
| **Record version history** | `/admin/farm/audit/record-versions` | Look up all submissions for any `(device_id, local_id)` across all tables; see diffs between versions; perform admin canonical override |
| **Resync log** | `/admin/farm/audit/resync-log` | Filter sync log by `resync_reason`; see which devices resync frequently; detect abnormal patterns |
| Sync log | `/admin/farm/audit/sync-log` | Per-device sync history with new/resynced breakdown per table |
| Preset activation log | `/admin/farm/audit/preset-activations` | Full activation history |

These views are standard Drupal entity list builders and form displays — no custom frontend required.

---

## 7. Reporting (farm_reports module)

`farm_reports` exposes a `ReportQueryService` that wraps raw SQL (Drupal Database API) against the synced operational tables. All report endpoints from Group F are read-only and callable only with ADMIN or MANAGEMENT role tokens (not from the Android app).

### 7.1 Available Reports

All reports query the **canonical views** (`farm_canonical_*`). This guarantees no duplicate counting regardless of how many times a record has been resynced — only the current canonical version of each record is counted.

All reports accept `?device_id=` and `?synced_by_user=` as optional filter parameters. This allows an admin to scope any report to a specific POS unit or specific operator. When these parameters are omitted, the report aggregates across all devices and users.

| Report | Key dimensions | Aggregates |
|---|---|---|
| Sales by period | date range, channel, **device**, **user**, product | total orders, total amount, by-channel breakdown, by-product qty + amount |
| Acquisition / procurement cost | date range, location, **device**, **user**, product | total cost, total qty, avg cost/kg, by-product |
| Payroll summary | date range, employee, **device** | gross, advances, net pay, by-employee |
| Farm operations log | date range, operation type, **device**, **user** | count by type, detailed list |
| Revenue vs. cost margin | date range, product, **device** | acquisition cost vs. sales revenue, implied margin |
| **Device activity** | device, user | per-device: last seen, last sync, `synced_by_user` on last sync, record counts per table (orders / acquisitions / farm_ops / remittances / employee_payments), earliest and latest record dates per table |
| **User activity** | user | across all devices: total records submitted per table, devices used, date range of activity — useful for auditing staff contributions |

### 7.2 Potential Analytics Extensions (future)

- **SRP drift analysis** — compare computed SRP at acquisition time vs. actual sale price per product over time.
- **Spoilage-adjusted cost trend** — track `spoilage_rate` in acquisition snapshots over time per product.
- **Channel mix** — proportion of orders by channel (Online / Reseller / Offline) per period.
- **Employee advance balance** — outstanding advance = sum of `cash_advance_amount` − sum of `liquidated_amount` per employee, all-time.
- **Inventory turnover estimate** — acquisition qty inbound vs. order qty outbound per product per period.

---

## 8. Notifications (farm_notifications module)

### 8.1 Config Version Polling (lightweight)

`GET /api/v1/config/version` returns `MAX(date_updated)` per config domain as a version token. The device caches the last-seen token per domain and compares on foreground resume. If any token changed, the device calls the targeted `GET /config/{domain}` endpoint.

Version tokens are computed **on request** from a cached `farm_notifications` state record (one row per domain, updated whenever a config write occurs via Drupal hook). This avoids a `MAX()` query on every device check-in.

### 8.2 FCM Push (optional)

`farm_notifications` stores a `farm_device_fcm_token` table (`device_id`, `fcm_token`, `updated_at`). On events that warrant immediate device notification:

| Event | Trigger hook | Devices notified |
|---|---|---|
| Pricing preset activated | `farm_config_mgr_preset_activated` | All active devices |
| Product added or deactivated | `farm_config_mgr_product_updated` | All active devices |
| User account deactivated | `farm_rbac_user_deactivated` | That device's last-seen session only |

FCM dispatch is handled asynchronously via Drupal's Queue API (`farm_notifications_fcm_queue` worker) to avoid blocking the admin UI action.

---

## 9. Role and Permission Model (farm_rbac module)

The server mirrors the five roles from `user_roles.md`:

| Drupal role machine name | Display | Permissions |
|---|---|---|
| `farm_admin` | Administrator | All farm permissions |
| `farm_store_assistant` | Store Assistant | Orders, customers, remittances, SRP view |
| `farm_purchasing` | Purchasing | Acquisitions, SRP view, product catalog view |
| `farm_farmer` | Farmer | Farm operations, product catalog view |
| `farm_user` | User | Dashboard view only |

Permissions are defined in `farm_rbac.permissions.yml`. Each API resource plugin checks the appropriate permission via `FarmRbacService`.

**Role synchronization with devices:** `GET /config/users` returns the user's `role` string matching the Android app's role enum (`ADMIN`, `STORE_ASSISTANT`, `PURCHASING`, `FARMER`, `USER`). The Android app reconciles its local `users` table on config pull.

---

## 10. Database Schema (Drupal / MySQL)

Custom tables (not Drupal entities — raw tables for performance):

```sql
-- Config tables (server is source of truth)
farm_products           (product_id, product_name, unit_type, category,
                         default_piece_count, is_active, date_created, date_updated)
farm_product_prices     (price_id SERIAL, product_id, per_kg_price, per_piece_price,
                         discounted_per_kg_price, discounted_per_piece_price, date_created)
farm_pricing_presets    (preset_id, preset_name, spoilage_rate, additional_cost_per_kg,
                         hauling_weight_kg, hauling_fees_json, channels_json,
                         categories_json, is_active, activated_at, activated_by,
                         saved_at, saved_by, date_updated)
farm_employees          (employee_id SERIAL, firstname, lastname, contact,
                         is_active, date_created, date_updated)
farm_customers          (customer_id SERIAL, firstname, lastname, contact,
                         customer_type, address, city, province, postal_code,
                         source_device_id, date_created, date_updated)

-- Operational records (device uploads — append-only, versioned)
--
-- Version / resync columns (on all farm_synced_* tables):
--   submission_seq   INTEGER NOT NULL DEFAULT 1
--   resync_reason    ENUM('INITIAL','RECONNECT','CORRECTION','RECOVERY','FORCED','ADMIN_OVERRIDE') NOT NULL
--   is_canonical     TINYINT(1) NOT NULL DEFAULT 1
--   superseded_at    BIGINT NULL           -- server time when displaced; null while canonical
--   superseded_by_seq INTEGER NULL         -- submission_seq of displacing row; null while canonical
--
-- Constraint: UNIQUE (device_id, local_id, submission_seq)
-- Partial unique (enforced via application layer + trigger):
--   at most one row per (device_id, local_id) may have is_canonical = 1
--
-- Analytics NEVER query these tables directly — use the canonical views below.

farm_synced_orders      (server_id SERIAL PK,
                         -- provenance
                         device_id NOT NULL, synced_by_user NOT NULL, synced_at NOT NULL,
                         -- versioning
                         submission_seq, resync_reason, is_canonical,
                         superseded_at, superseded_by_seq,
                         -- payload
                         local_id, customer_id, channel, total_amount,
                         order_date, order_update_date,
                         -- Group E mutable fields (canonical row only)
                         is_paid, is_delivered, status_updated_at, status_updated_by_user,
                         UNIQUE (device_id, local_id, submission_seq))

farm_synced_order_items (server_id SERIAL PK,
                         order_server_id FK → farm_synced_orders.server_id,
                         local_item_id, product_id, quantity,
                         price_per_unit, is_per_kg, total_price)
                         -- inherits provenance from parent order via join

farm_synced_acquisitions (server_id SERIAL PK,
                         -- provenance
                         device_id NOT NULL, synced_by_user NOT NULL, synced_at NOT NULL,
                         -- versioning
                         submission_seq, resync_reason, is_canonical,
                         superseded_at, superseded_by_seq,
                         -- payload
                         local_id, product_id, product_name, quantity,
                         price_per_unit, total_amount, is_per_kg, piece_count,
                         date_acquired, created_at, location, preset_ref,
                         spoilage_rate, additional_cost_per_kg,
                         srp_online_per_kg, srp_reseller_per_kg, srp_offline_per_kg,
                         srp_online_500g, srp_reseller_500g, srp_offline_500g,
                         srp_online_250g, srp_reseller_250g, srp_offline_250g,
                         srp_online_100g, srp_reseller_100g, srp_offline_100g,
                         srp_online_per_piece, srp_reseller_per_piece, srp_offline_per_piece,
                         srp_custom_override, channels_snapshot_json, hauling_fees_json,
                         UNIQUE (device_id, local_id, submission_seq))

farm_synced_remittances  (server_id SERIAL PK,
                          device_id NOT NULL, synced_by_user NOT NULL, synced_at NOT NULL,
                          submission_seq, resync_reason, is_canonical,
                          superseded_at, superseded_by_seq,
                          local_id, amount, date, remarks, date_updated,
                          UNIQUE (device_id, local_id, submission_seq))

farm_synced_farm_ops     (server_id SERIAL PK,
                          device_id NOT NULL, synced_by_user NOT NULL, synced_at NOT NULL,
                          submission_seq, resync_reason, is_canonical,
                          superseded_at, superseded_by_seq,
                          local_id, operation_type, operation_date, details, area,
                          weather_condition, personnel, product_id, product_name,
                          date_created, date_updated,
                          UNIQUE (device_id, local_id, submission_seq))

farm_synced_emp_payments (server_id SERIAL PK,
                          device_id NOT NULL, synced_by_user NOT NULL, synced_at NOT NULL,
                          submission_seq, resync_reason, is_canonical,
                          superseded_at, superseded_by_seq,
                          local_id, employee_id, amount, cash_advance_amount,
                          liquidated_amount, date_paid, received_date, signature,
                          UNIQUE (device_id, local_id, submission_seq))

-- Canonical views (used by ALL reporting and analytics — no direct table queries)
CREATE VIEW farm_canonical_orders           AS SELECT * FROM farm_synced_orders WHERE is_canonical = 1;
CREATE VIEW farm_canonical_acquisitions     AS SELECT * FROM farm_synced_acquisitions WHERE is_canonical = 1;
CREATE VIEW farm_canonical_remittances      AS SELECT * FROM farm_synced_remittances WHERE is_canonical = 1;
CREATE VIEW farm_canonical_farm_operations  AS SELECT * FROM farm_synced_farm_ops WHERE is_canonical = 1;
CREATE VIEW farm_canonical_employee_payments AS SELECT * FROM farm_synced_emp_payments WHERE is_canonical = 1;

-- Device registry and audit
farm_devices             (device_id PK, device_name, app_version, db_version,
                          registered_at, registered_by_user,
                          last_seen_at, last_sync_at, last_sync_user,
                          is_active, deactivated_at, deactivated_by_user)
farm_sync_log            (sync_id CHAR(36), device_id, synced_by_user,
                          synced_at, ip_address, app_version, tables_json)
farm_preset_activation_log (log_id SERIAL, activated_at, activated_by,
                            preset_id_activated, preset_id_deactivated)
farm_config_versions     (domain PK, version_token, updated_at)
farm_device_fcm_tokens   (device_id PK, fcm_token, updated_at)

-- Provenance columns on all synced operational tables
-- (shown here as the canonical pattern; applies to all farm_synced_* tables above)
-- device_id       VARCHAR NOT NULL  -- FK → farm_devices.device_id
-- synced_by_user  VARCHAR NOT NULL  -- JWT subject; not a FK to allow deleted users' history
-- synced_at       BIGINT  NOT NULL  -- server clock; set on INSERT, immutable
-- UNIQUE (device_id, local_id)      -- idempotent upsert key
```

**Timestamp convention:** All `*_at`, `*_date`, `date_*` columns are **epoch milliseconds (BIGINT)** — same as the Android Room schema. No ISO 8601 in the database.

---

## 11. Data Flow Diagrams

### 11.1 Sync Cycle (Device → Server)

```
Device (offline queue fills up)
  │
  ├── [app foreground / manual "Sync Now"]
  │
  ▼
POST /api/v1/sync/batch
  │  { orders: [...], acquisitions: [...], ... }
  │
  ▼
farm_api: SyncBatchResource.post()
  │
  ▼
farm_sync: SyncService.processBatch()
  ├── For each record: upsert by (device_id, local_id)
  ├── Validate fields → reject malformed
  └── Write farm_sync_log
  │
  ▼
Response: { results: { orders: {accepted, rejected}, ... }, server_time }
  │
  ▼
Device: marks records as synced, updates last_synced_at
```

### 11.2 Config Pull Cycle (Server → Device)

```
Device [login / "Sync Now" / version change detected]
  │
  ▼
GET /api/v1/config/version
  │  Response: { products_version, presets_version, ... }
  │
  ├── Compare against locally cached versions
  │
  ├── [versions match] → no-op
  │
  └── [version differs for domain X]
        │
        ▼
      GET /api/v1/config/{domain}?since=<last_pulled_at>
        │
        ▼
      farm_api: ConfigXxxResource.get()
        │
        ▼
      farm_config_mgr: ConfigService.buildConfigPayload()
        │
        ▼
      Response: { products: [...], server_time }
        │
        ▼
      Device: upserts into local Room table, updates last_pulled_at + version cache
```

### 11.3 Preset Activation Flow

```
Admin (web)
  │
  ▼
/admin/farm/presets → click "Activate" on preset
  │
  ▼
farm_config_mgr: PresetActivationService.activate(preset_id, admin_user)
  ├── Set previous active preset is_active = false
  ├── Set new preset is_active = true, activated_at, activated_by
  ├── Write farm_preset_activation_log row
  └── Update farm_config_versions.presets_version = now()
  │
  ▼
farm_notifications: dispatch FCM queue item
  │
  ▼
[async] FCM push to all registered devices
  │
  ▼
Devices: silent push received → call GET /config/pricing-presets
```

---

## 12. Security Considerations

| Concern | Mitigation |
|---|---|
| Token leakage | Short-lived JWT (1h) + opaque refresh token stored server-side; refresh token revoked on logout (DELETE /auth/token) |
| Device spoofing | `X-Device-Id` is validated against `farm_devices` on every upload request; unregistered or inactive devices are rejected before any payload is processed. `ANDROID_ID` is not cryptographically strong but acts as a namespace; the JWT provides the authentication guarantee |
| Provenance forgery | `device_id` and `synced_by_user` are **never accepted from the request body** — both are extracted from validated auth context (header + JWT) by the server and stamped server-side. A client cannot claim to be a different device or user |
| Provenance tampering | `device_id`, `synced_by_user`, and `synced_at` are set on INSERT and never updated — even re-uploading the same `(device_id, local_id)` pair cannot overwrite the original provenance stamps |
| Shared device accountability | Because `synced_by_user` is captured from the authenticated JWT (not the device), swapping user accounts on a shared device correctly attributes records to the actual logged-in operator |
| Password storage | Server stores PBKDF2WithHmacSHA1 hashes (same format as Android). Passwords never sent in config pull responses in plaintext — only the hash, so the device can do offline auth |
| Reporting endpoint exposure | Group F reporting endpoints require `farm_admin` Drupal role. Not callable from Android app tokens that carry device-level roles |
| Re-upload replay | Upsert by `(device_id, local_id)` is idempotent — replaying an old batch does not duplicate records; `date_updated` field prevents stale data overwriting newer state |
| SQL injection | All queries use Drupal Database API parameterized queries — no raw string interpolation |
| Mass data extraction | List endpoints are paginated (max 500 per request); reporting endpoints require ADMIN role |

---

## 13. Module File Structure

```
web/modules/custom/
├── farm_core/
│   ├── farm_core.info.yml
│   ├── farm_core.module
│   └── src/
│       ├── Service/TimestampHelper.php
│       └── Service/PaginationHelper.php
│
├── farm_config_mgr/
│   ├── farm_config_mgr.info.yml
│   ├── farm_config_mgr.module          # hook_farm_preset_activated, etc.
│   ├── farm_config_mgr.install          # hook_schema for custom tables
│   └── src/
│       ├── Service/ConfigService.php   # builds config pull payloads
│       ├── Service/ProductService.php
│       ├── Service/PresetService.php
│       ├── Service/PresetActivationService.php
│       ├── Service/EmployeeService.php
│       └── Service/CustomerService.php
│
├── farm_sync/
│   ├── farm_sync.info.yml
│   ├── farm_sync.install
│   └── src/
│       ├── Service/SyncService.php     # processBatch(), upsert logic
│       ├── Service/ConflictResolver.php
│       └── Service/OrderStatusService.php
│
├── farm_reports/
│   ├── farm_reports.info.yml
│   └── src/
│       └── Service/ReportQueryService.php
│
├── farm_notifications/
│   ├── farm_notifications.info.yml
│   ├── farm_notifications.install
│   └── src/
│       ├── Service/ConfigVersionService.php
│       ├── Service/FcmDispatchService.php
│       └── Plugin/QueueWorker/FcmQueueWorker.php
│
├── farm_rbac/
│   ├── farm_rbac.info.yml
│   ├── farm_rbac.permissions.yml
│   └── src/
│       └── Service/FarmRbacService.php
│
└── farm_api/
    ├── farm_api.info.yml
    ├── farm_api.routing.yml            # REST resource route overrides
    └── src/
        ├── Authentication/
        │   └── FarmApiAuthSubscriber.php
        └── Plugin/rest/resource/
            ├── AuthTokenResource.php
            ├── AuthRefreshResource.php
            ├── AuthMeResource.php
            ├── DeviceResource.php
            ├── SyncOrdersResource.php
            ├── SyncAcquisitionsResource.php
            ├── SyncRemittancesResource.php
            ├── SyncFarmOperationsResource.php
            ├── SyncEmployeePaymentsResource.php
            ├── SyncCustomersResource.php
            ├── SyncBatchResource.php
            ├── ConfigResource.php
            ├── ConfigProductsResource.php
            ├── ConfigPresetsResource.php
            ├── ConfigEmployeesResource.php
            ├── ConfigCustomersResource.php
            ├── ConfigUsersResource.php
            ├── ConfigVersionResource.php
            ├── OrderStatusResource.php
            ├── ReportSalesResource.php
            ├── ReportAcquisitionsResource.php
            ├── ReportPayrollResource.php
            ├── ReportFarmOpsResource.php
            ├── AuditSyncLogResource.php
            ├── AuditPresetActivationsResource.php
            └── NotificationsSubscribeResource.php
```

---

## 14. Deployment Notes

- **Drupal version:** 10.x or 11.x (LTS)
- **PHP:** 8.2+
- **Database:** MySQL 8.0+ (or MariaDB 10.6+)
- **Required contrib modules:** `simple_oauth` (JWT), `restui` (dev/debug), standard Drupal REST module enabled
- **Caching:** `farm_config_versions` table makes `GET /config/version` responses cacheable per domain; internal page cache can be applied to this endpoint with a short TTL
- **Queue backend:** Drupal Database Queue is sufficient for low FCM volume; swap to Redis Queue for higher throughput
- **HTTPS:** Required. JWT tokens must not transit over plain HTTP
- **CORS:** Configure `services.yml` `cors.config` to allow requests from the Android app's origin (none needed for mobile — CORS applies to browser clients only; include if a web dashboard SPA is added later)

---

## 15. Open Questions for Server User Stories

The following questions should be resolved when writing `server_user_stories.md`:

1. **Web dashboard frontend:** Does the reporting/analytics UI live within Drupal Views + admin theme, or is a separate SPA (React/Vue) needed for richer charting?
2. **Multi-farm / multi-tenant:** Is this a single business installation, or should the server support multiple farm accounts under one deployment?
3. **Customer deduplication:** What is the intended UX when the server detects a likely duplicate customer from two devices? Advisory 409, or server-side merge?
4. **Preset push priority:** Should FCM push be required for preset activation, or is polling-on-foreground sufficient given the business's connectivity conditions?
5. **Historical SRP recalculation:** Should the server ever recompute SRPs for historical acquisitions (e.g., after discovering BUG-ACQ-07)? Or is stored SRP immutable after sync?
6. **Signature storage:** `employee_payments.signature` may be a large Base64 PNG — should the server store it verbatim, or accept a separate file upload and store only a URL?
7. **Offline-only devices:** Some devices may never have connectivity. Is there a manual export path (e.g., upload via CSV from the Android Export screen)?
8. **Web-initiated orders:** Can the web dashboard create orders directly, or is order creation always device-originated?
