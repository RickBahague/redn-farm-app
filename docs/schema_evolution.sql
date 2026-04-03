-- ============================================================
-- schema_evolution.sql
-- RedN Farm App — Database Schema Evolution Record
--
-- Convention:
--   • Each version block contains the FULL CREATE TABLE statements
--     for that version — not diffs.
--   • Append new versions at the bottom; never edit past versions.
--   • Version numbers must match FarmDatabase.kt @Database(version = N) (currently 5).
--   • Verify v4+ CREATE statements against Room's generated schema JSON
--     (build/generated/source/kapt/.../FarmDatabase_Impl.java) per SYS-US-04.
-- ============================================================


-- ============================================================
-- VERSION 3  (baseline — existing app before rebuild)
-- ============================================================

CREATE TABLE IF NOT EXISTS `users` (
    `user_id`      INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `username`     TEXT    NOT NULL,
    `password_hash` TEXT   NOT NULL,
    `full_name`    TEXT    NOT NULL,
    `role`         TEXT    NOT NULL,
    `is_active`    INTEGER NOT NULL DEFAULT 1,
    `date_created` INTEGER NOT NULL,
    `date_updated` INTEGER NOT NULL,
    UNIQUE(`username`)
);

CREATE TABLE IF NOT EXISTS `products` (
    `product_id`          TEXT    PRIMARY KEY NOT NULL,
    `product_name`        TEXT    NOT NULL,
    `product_description` TEXT    NOT NULL,
    `unit_type`           TEXT    NOT NULL,
    `is_active`           INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS `customers` (
    `customer_id`   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `firstname`     TEXT    NOT NULL,
    `lastname`      TEXT    NOT NULL,
    `contact`       TEXT    NOT NULL,
    `customer_type` TEXT    NOT NULL DEFAULT 'RETAIL',
    `address`       TEXT    NOT NULL,
    `city`          TEXT    NOT NULL,
    `province`      TEXT    NOT NULL,
    `postal_code`   TEXT    NOT NULL,
    `date_created`  TEXT    NOT NULL,   -- LocalDateTime stored as ISO string
    `date_updated`  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS `orders` (
    `order_id`         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `customer_id`      INTEGER NOT NULL,
    `total_amount`     REAL    NOT NULL,
    `order_date`       INTEGER NOT NULL,
    `order_update_date` INTEGER NOT NULL,
    `is_paid`          INTEGER NOT NULL DEFAULT 0,
    `is_delivered`     INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(`customer_id`) REFERENCES `customers`(`customer_id`) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS `order_items` (
    `id`             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `order_id`       INTEGER NOT NULL,
    `product_id`     TEXT    NOT NULL,
    `quantity`       REAL    NOT NULL,
    `price_per_unit` REAL    NOT NULL,
    `is_per_kg`      INTEGER NOT NULL,
    `total_price`    REAL    NOT NULL,
    FOREIGN KEY(`order_id`)   REFERENCES `orders`(`order_id`)     ON DELETE CASCADE,
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS `employees` (
    `employee_id`  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `firstname`    TEXT    NOT NULL,
    `lastname`     TEXT    NOT NULL,
    `contact`      TEXT    NOT NULL,
    `date_created` INTEGER NOT NULL,
    `date_updated` INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `employee_payments` (
    `payment_id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `employee_id`          INTEGER NOT NULL,
    `amount`               REAL    NOT NULL,
    `cash_advance_amount`  REAL,
    `liquidated_amount`    REAL,
    `date_paid`            INTEGER NOT NULL,
    `signature`            TEXT    NOT NULL,
    `received_date`        INTEGER,
    FOREIGN KEY(`employee_id`) REFERENCES `employees`(`employee_id`) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS `acquisitions` (
    `acquisition_id`  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `product_id`      TEXT    NOT NULL,
    `product_name`    TEXT    NOT NULL,
    `quantity`        REAL    NOT NULL,
    `price_per_unit`  REAL    NOT NULL,
    `total_amount`    REAL    NOT NULL,
    `is_per_kg`       INTEGER NOT NULL,
    `date_acquired`   INTEGER NOT NULL,
    `location`        TEXT    NOT NULL,
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS `index_acquisitions_product_id` ON `acquisitions`(`product_id`);

CREATE TABLE IF NOT EXISTS `farm_operations` (
    `operation_id`     INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `operation_type`   TEXT    NOT NULL,
    `operation_date`   INTEGER NOT NULL,
    `details`          TEXT    NOT NULL,
    `area`             TEXT    NOT NULL,
    `weather_condition` TEXT   NOT NULL,
    `personnel`        TEXT    NOT NULL,
    `product_id`       TEXT,
    `product_name`     TEXT    NOT NULL DEFAULT '',
    `date_created`     INTEGER NOT NULL,
    `date_updated`     INTEGER NOT NULL,
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS `index_farm_operations_product_id` ON `farm_operations`(`product_id`);

CREATE TABLE IF NOT EXISTS `remittances` (
    `remittance_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `amount`        REAL    NOT NULL,
    `date`          INTEGER NOT NULL,
    `remarks`       TEXT    NOT NULL DEFAULT '',
    `date_updated`  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `product_prices` (
    `price_id`                   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `product_id`                 TEXT    NOT NULL,
    `per_kg_price`               REAL,
    `per_piece_price`            REAL,
    `discounted_per_kg_price`    REAL,
    `discounted_per_piece_price` REAL,
    `date_created`               TEXT    NOT NULL,  -- LocalDateTime stored as ISO string
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE
);


-- ============================================================
-- VERSION 4  (rebuild — all new fields from planned stories)
-- ============================================================
-- Changes from v3:
--   products           + category, default_piece_count
--   orders             + channel
--   acquisitions       + piece_count, preset_ref, preset snapshot fields, all SRP fields
--   NEW: pricing_presets
--   NEW: preset_activation_log
-- ============================================================

CREATE TABLE IF NOT EXISTS `users` (
    `user_id`       INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `username`      TEXT    NOT NULL,
    `password_hash` TEXT    NOT NULL,
    `full_name`     TEXT    NOT NULL,
    `role`          TEXT    NOT NULL,
    `is_active`     INTEGER NOT NULL DEFAULT 1,
    `date_created`  INTEGER NOT NULL,
    `date_updated`  INTEGER NOT NULL,
    UNIQUE(`username`)
);

CREATE TABLE IF NOT EXISTS `products` (
    `product_id`          TEXT    PRIMARY KEY NOT NULL,
    `product_name`        TEXT    NOT NULL,
    `product_description` TEXT    NOT NULL,
    `unit_type`           TEXT    NOT NULL,
    `category`            TEXT,                       -- MGT-US-03 / PRD-US-02
    `default_piece_count` INTEGER,                    -- PRD-US-02; pre-fills acquisitions
    `is_active`           INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS `customers` (
    `customer_id`   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `firstname`     TEXT    NOT NULL,
    `lastname`      TEXT    NOT NULL,
    `contact`       TEXT    NOT NULL,
    `customer_type` TEXT    NOT NULL DEFAULT 'RETAIL',
    `address`       TEXT    NOT NULL,
    `city`          TEXT    NOT NULL,
    `province`      TEXT    NOT NULL,
    `postal_code`   TEXT    NOT NULL,
    `date_created`  TEXT    NOT NULL,
    `date_updated`  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS `orders` (
    `order_id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `customer_id`       INTEGER NOT NULL,
    `channel`           TEXT    NOT NULL DEFAULT 'offline',  -- ORD-US-01; 'online'|'reseller'|'offline'
    `total_amount`      REAL    NOT NULL,
    `order_date`        INTEGER NOT NULL,
    `order_update_date` INTEGER NOT NULL,
    `is_paid`           INTEGER NOT NULL DEFAULT 0,
    `is_delivered`      INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(`customer_id`) REFERENCES `customers`(`customer_id`) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS `order_items` (
    `id`             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `order_id`       INTEGER NOT NULL,
    `product_id`     TEXT    NOT NULL,
    `quantity`       REAL    NOT NULL,
    `price_per_unit` REAL    NOT NULL,
    `is_per_kg`      INTEGER NOT NULL,
    `total_price`    REAL    NOT NULL,
    FOREIGN KEY(`order_id`)   REFERENCES `orders`(`order_id`)     ON DELETE CASCADE,
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS `employees` (
    `employee_id`  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `firstname`    TEXT    NOT NULL,
    `lastname`     TEXT    NOT NULL,
    `contact`      TEXT    NOT NULL,
    `date_created` INTEGER NOT NULL,
    `date_updated` INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `employee_payments` (
    `payment_id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `employee_id`         INTEGER NOT NULL,
    `amount`              REAL    NOT NULL,     -- gross wage
    `cash_advance_amount` REAL,                 -- advance given this transaction
    `liquidated_amount`   REAL,                 -- prior advance recovered this transaction
    `date_paid`           INTEGER NOT NULL,
    `signature`           TEXT    NOT NULL,
    `received_date`       INTEGER,
    FOREIGN KEY(`employee_id`) REFERENCES `employees`(`employee_id`) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS `acquisitions` (
    -- core fields
    `acquisition_id`         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `product_id`             TEXT    NOT NULL,
    `product_name`           TEXT    NOT NULL,
    `quantity`               REAL    NOT NULL,            -- kg when is_per_kg; total pieces when not (INV-US-01)
    `price_per_unit`         REAL    NOT NULL,
    `total_amount`           REAL    NOT NULL,
    `is_per_kg`              INTEGER NOT NULL,
    `piece_count`            REAL,                       -- INV-US-01; pieces per kg when not is_per_kg; PricingReference §5.1.1 / §4.3.1 (may be fractional)
    `date_acquired`          INTEGER NOT NULL,           -- user-entered acquisition date
    `created_at`             INTEGER NOT NULL,           -- INV-US-06 tiebreaker: DB insert timestamp
    `location`               TEXT    NOT NULL,
    -- preset traceability (INV-US-05)
    `preset_ref`             TEXT,                       -- FK to pricing_presets.preset_id
    -- preset snapshot at save time (INV-US-05 — immutable audit trail)
    `spoilage_rate`          REAL,                       -- preset snapshot; CLARIF-01: not applied in SRP when is_per_kg=0 (design — PricingReference §5.1.1)
    `additional_cost_per_kg` REAL,
    `hauling_weight_kg`      REAL,
    `hauling_fees_json`      TEXT,                       -- JSON: [{label, amount}, ...]
    -- full per-channel config snapshot (markup/margin, rounding_rule, fees) — same shape as pricing_presets.channels_json
    `channels_snapshot_json` TEXT,
    -- computed SRPs per kg
    `srp_online_per_kg`      REAL,
    `srp_reseller_per_kg`    REAL,
    `srp_offline_per_kg`     REAL,
    -- computed SRPs fractional packages
    `srp_online_500g`        REAL,
    `srp_online_250g`        REAL,
    `srp_online_100g`        REAL,
    `srp_reseller_500g`      REAL,
    `srp_reseller_250g`      REAL,
    `srp_reseller_100g`      REAL,
    `srp_offline_500g`       REAL,
    `srp_offline_250g`       REAL,
    `srp_offline_100g`       REAL,
    -- computed SRPs per piece (null when piece_count is null)
    `srp_online_per_piece`   REAL,
    `srp_reseller_per_piece` REAL,
    `srp_offline_per_piece`  REAL,
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS `index_acquisitions_product_id`  ON `acquisitions`(`product_id`);
CREATE INDEX IF NOT EXISTS `index_acquisitions_preset_ref`  ON `acquisitions`(`preset_ref`);
CREATE INDEX IF NOT EXISTS `index_acquisitions_date`        ON `acquisitions`(`date_acquired`);

CREATE TABLE IF NOT EXISTS `farm_operations` (
    `operation_id`      INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `operation_type`    TEXT    NOT NULL,
    `operation_date`    INTEGER NOT NULL,
    `details`           TEXT    NOT NULL,
    `area`              TEXT    NOT NULL,
    `weather_condition` TEXT    NOT NULL,
    `personnel`         TEXT    NOT NULL,
    `product_id`        TEXT,
    `product_name`      TEXT    NOT NULL DEFAULT '',
    `date_created`      INTEGER NOT NULL,
    `date_updated`      INTEGER NOT NULL,
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS `index_farm_operations_product_id` ON `farm_operations`(`product_id`);

CREATE TABLE IF NOT EXISTS `remittances` (
    `remittance_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `amount`        REAL    NOT NULL,
    `date`          INTEGER NOT NULL,
    `remarks`       TEXT    NOT NULL DEFAULT '',
    `date_updated`  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `product_prices` (
    `price_id`                   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `product_id`                 TEXT    NOT NULL,
    `per_kg_price`               REAL,
    `per_piece_price`            REAL,
    `discounted_per_kg_price`    REAL,
    `discounted_per_piece_price` REAL,
    `date_created`               TEXT    NOT NULL,
    FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE
);

-- Pricing presets (MGT-US-01–03, MGT-US-06)
CREATE TABLE IF NOT EXISTS `pricing_presets` (
    `preset_id`              TEXT    PRIMARY KEY NOT NULL,  -- UUID
    `preset_name`            TEXT    NOT NULL,              -- admin-entered or auto-generated e.g. "Preset 2026-04-02 14:30"
    `saved_at`               INTEGER NOT NULL,
    `saved_by`               TEXT    NOT NULL,
    `is_active`              INTEGER NOT NULL DEFAULT 0,    -- 1 = active, 0 = inactive
    `activated_at`           INTEGER,                       -- null until first activation
    `activated_by`           TEXT,
    -- store-wide defaults (MGT-US-01)
    `spoilage_rate`          REAL    NOT NULL,
    `additional_cost_per_kg` REAL    NOT NULL,
    `hauling_weight_kg`      REAL    NOT NULL,
    `hauling_fees_json`      TEXT    NOT NULL,              -- JSON: [{label, amount}, ...]
    -- per-channel config (MGT-US-02): markup OR margin, rounding rule, optional fees
    -- JSON shape: {"online":{"markup":0.35,"margin":null,"rounding_rule":"ceil_whole_peso","fees":[]},"reseller":{...},"offline":{...}}
    -- fees element shape: {"label":"delivery surcharge","type":"fixed|pct","amount":10.0}
    `channels_json`          TEXT    NOT NULL,
    -- per-category overrides (MGT-US-03)
    `categories_json`        TEXT    NOT NULL DEFAULT '[]'  -- JSON: [{name, spoilageRate?, additionalCostPerKg?}]
);

-- Append-only activation event log (MGT-US-05, MGT-US-06)
CREATE TABLE IF NOT EXISTS `preset_activation_log` (
    `log_id`                INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `activated_at`          INTEGER NOT NULL,
    `activated_by`          TEXT    NOT NULL,
    `preset_id_activated`   TEXT    NOT NULL,
    `preset_id_deactivated` TEXT                            -- null on very first activation
);

-- ============================================================
-- VERSION 5  (BUG-EMP-03 — employee payment finalize / lock)
-- ============================================================
-- Build phase: no incremental Room migration. Schema is v5 in code; new DBs get full DDL from
-- Room `onCreate`. Existing installs hit `fallbackToDestructiveMigration()` until a migration
-- is added (data loss on upgrade path — acceptable during build).
--
-- `employee_payments` columns (v5):
--   payment_id, employee_id, amount, cash_advance_amount, liquidated_amount,
--   date_paid, signature, received_date, is_finalized
--
-- ============================================================
-- VERSION 6  (MGT-US-07 — customer / custom SRP per acquisition)
-- ============================================================
-- Build phase: no incremental 5→6 Room migration. New DBs get full DDL from Room `onCreate`.
-- Existing v5 installs hit `fallbackToDestructiveMigration()` when opening v6 (acceptable during dev).
--
-- `acquisitions` adds boolean column: `srp_custom_override` (0/1)

-- ============================================================
-- VERSION 8  (BUG-ARC-02 — epoch millis for customer / product price timestamps)
-- ============================================================
-- `customers.date_created`, `customers.date_updated`, `product_prices.date_created`:
-- INTEGER NOT NULL, epoch **milliseconds** (system default zone at UI/seed boundaries).
-- Room **`DateTimeConverter`** (epoch seconds ↔ LocalDateTime) removed from `FarmDatabase`.
-- No incremental 7→8 migration in code; dev relies on `fallbackToDestructiveMigration()`.
