# User Stories — RedN Farm App

## Overview

This document is the **source of truth** for all user stories in the RedN Farm App (Yong & Eyo's FARM). Stories reflect what is currently implemented and serve as the baseline for future development.

**App version:** 1.0.0  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Last updated:** 2026-04-09

---

## Actors

| Actor | Description | Primary Workflows |
|-------|-------------|-------------------|
| **Admin / Owner** | Full access. Configures products, prices, and employees. | Products, Pricing, Employees, Export |
| **Store Assistant** | Takes orders and manages customer relationships. | Orders, Customers, Remittance |
| **Farmer** | Records what happens in the field. | Farm Operations |
| **Purchasing Assistant** | Records incoming produce and procurement costs. | Inventory & Acquisitions |

> **Note:** The app seeds two accounts (`admin` / `user`). **Per-screen access** is enforced with `Rbac` (e.g. Settings and pricing presets for admin-capable roles, export data for export-capable roles, day close for store/admin roles). A full role matrix UI remains out of scope; see **Out of Scope (v1.0)** below.

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented and working |
| 🔧 | Partially implemented |
| 📋 | Planned / not yet built |

**Closing gaps:** Stories marked **✅ (partial)** or **🔧** should be tracked directly in this document and, when unresolved, mirrored in `KNOWN_ISSUES.md`.

---

## Epic 1 — Authentication

### AUTH-US-01 — Login to the app
**As** any user, **I want to** log in with my username and password **so that** my data is protected and access is controlled.

**Actor:** All  
**Status:** ✅

**Acceptance criteria:**
1. Login screen is the entry point — no other screen is reachable without authentication.
2. Entering a valid username/password combination navigates to the main dashboard.
3. Entering invalid credentials shows an error message without revealing which field is wrong.
4. A loading indicator is shown while authentication is in progress.
5. The back button cannot navigate to the login screen after a successful login.

---

### AUTH-US-02 — Log out
**As** any user, **I want to** log out **so that** my session is closed and another person can log in.

**Actor:** All  
**Status:** ✅

**Acceptance criteria:**
1. A logout button is accessible from the main dashboard at all times.
2. Logout clears the session and navigates back to the login screen.
3. After logout, pressing back does not return to any authenticated screen.

---

### AUTH-US-03 — Persist session across app restarts
**As** any user, **I want** my login session to persist when I close and reopen the app **so that** I don't have to log in every time.

**Actor:** All  
**Status:** ✅

**Acceptance criteria:**
1. If a valid session exists on app launch, the app navigates directly to the main dashboard, skipping login.

---

### AUTH-US-05 — Change own password
**As** any authenticated user, **I want to** change my own password from my profile screen **so that** I can keep my account secure.

**Actor:** All  
**Status:** ✅ *(**ProfileScreen** / **ChangePasswordScreen** — current password check, match validation, hash update; nav from dashboard top bar)*

**Acceptance criteria:**
1. A profile screen is accessible from the top bar of the dashboard (SYS-US-02).
2. The profile screen shows the logged-in user's username, full name, and role — all read-only.
3. A **Change Password** action is available on the profile screen.
4. The change password form requires three fields: **current password**, **new password**, **confirm new password**.
5. The current password is validated against the stored hash before any change is applied. If it does not match, an error is shown and no change is made.
6. New password and confirm new password must match; if they differ, save is blocked with a clear field-level error.
7. On success, the password hash is updated and a confirmation message is shown. The user remains logged in.
8. A user can only change their **own** password from this screen. Resetting another user's password is an admin action (AUTH-US-04).

---

### AUTH-US-04 — Manage user accounts
**As** an admin, **I want to** create and deactivate user accounts **so that** I control who can access the app and each saved record can be attributed to a specific user.

**Actor:** Admin  
**Status:** ✅ *(**UserManagementScreen** / **UserManagementViewModel** — create user with hashed password, activate/deactivate, admin password reset; `Rbac` gated. **AC6:** **`PricingPresetEditorViewModel.save`** sets **`saved_by`** from **`SessionManager.getUsername()`**; **`PresetActivationPreviewViewModel.confirmActivate`** passes the same for **`activated_by`** and the activation log — **`PricingPresetRepository.savePreset`** documents caller responsibility.)*

**Acceptance criteria:**
1. I can create a new user account with a username, full name, role, and initial password.
2. Passwords are never stored as plaintext; they are hashed before saving.
3. I can deactivate a user account; deactivated users cannot log in.
4. Deactivating a user does not delete their historical records — all data they created remains intact.
5. Usernames must be unique; attempting to create a duplicate shows a clear error.
6. The logged-in username is recorded as `saved-by` on preset saves (MGT-US-05) and any other audited records.
7. I can **reset any user's password** directly — without requiring the user's current password — for cases where a user is locked out. The new password must be entered twice to confirm.

---

## Epic 2 — Order Management

### ORD-US-01 — Take a new customer order
**As** a store assistant, **I want to** create a new order for a customer **so that** I can record what they are buying and calculate the total.

**Actor:** Store Assistant  
**Status:** ✅ *(**TakeOrderViewModel** — active acquisition SRPs by **sales channel** via `observeAllActiveSrps` + `OrderPricingResolver`; fallback **ProductPrice** when no acquisition SRP)*

**Acceptance criteria:**
1. I can select a customer from the existing customer list.
2. I can select a **sales channel** for the order (online, reseller, offline); the channel applies to all items in the order.
3. I can add one or more products to the order cart.
4. When a product is added, its unit price is **pre-filled from the active SRP for the selected channel**, as computed via management presets (MGT-US-01–03) from the latest acquisition (INV-US-05). I do not enter prices manually.
5. If no preset-computed SRP exists for a product yet, the manually set fallback price (PRD-US-06) is used instead.
6. Each line item shows product name, quantity, pre-filled unit price, and line total.
7. The order total updates automatically as items are added or removed.
8. I can remove individual items from the cart.
9. I can clear the entire cart to start over.
10. I can submit the order, which saves it with the channel, all SRP values, and an unpaid/undelivered default status.

---

### ORD-US-02 — Apply per-kg or per-piece SRP based on product unit type
**As** a store assistant, **I want** the correct unit SRP (per kg or per piece) to be applied automatically based on how the product is sold **so that** I don't have to decide or calculate prices myself.

**Actor:** Store Assistant  
**Status:** ✅ *(per-channel resolution in **OrderPricingResolver**; dual-unit products when supported via **productSupportsDualUnit** / line **isPerKg**)*

**Acceptance criteria:**
1. Products configured as sold per kg use the **per-kg SRP** for the selected channel; products sold per piece use the **per-piece SRP**.
2. If a product supports both unit types, the store assistant can toggle between per-kg and per-piece — the pre-computed SRP for that unit type and channel is applied automatically.
3. The line total recalculates on unit type change.
4. The unit type and the SRP value used are saved with the order item.

---

### ORD-US-03 — View order history
**As** a store assistant, **I want to** view all past orders **so that** I can track what was sold and follow up on unpaid or undelivered orders.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. Order history shows all orders with customer name, date, total amount, payment status, and delivery status.
2. I can search by customer name or order amount.
3. I can filter orders by date range.
4. When filters are applied, a summary of the filtered totals is shown.

---

### ORD-US-04 — Edit an unpaid order
**As** a store assistant, **I want to** edit an order that has not been paid yet **so that** I can correct mistakes or add items.

**Actor:** Store Assistant  
**Status:** ✅ *(**OrderHistoryViewModel.repriceOrderItems** reapplies active SRPs when the order channel changes)*

**Acceptance criteria:**
1. Tapping an unpaid order in the history list opens the **order detail screen** (ORD-US-10) first. An **Edit** button on the detail screen enters edit mode — edit mode is not directly reachable from the history list.
2. I can change quantities of existing items; the line total recalculates automatically.
3. Unit prices are not manually editable — they reflect the active SRP for the order's channel at the time of the edit. If the SRP has changed since the order was placed, the updated SRP is applied on save.
4. I can change the sales channel for the order; all item prices are re-applied from the active SRPs for the new channel.
5. I can add new items to the order; their prices are pre-filled from the active SRP for the order's channel.
6. I can remove items from the order.
7. Changes are saved and the order total is recalculated.
8. The **Edit** button is only shown for unpaid orders. Paid orders open the detail screen with no Edit button.

---

### ORD-US-05 — Mark an order as paid
**As** a store assistant, **I want to** mark an order as paid **so that** I can track which customers have settled their account.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. I can toggle the payment status of an order between paid and unpaid.
2. The updated status is reflected immediately in the order history list.

---

### ORD-US-06 — Mark an order as delivered
**As** a store assistant, **I want to** mark an order as delivered **so that** I know which orders have been fulfilled.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. I can toggle the delivery status of an order between delivered and not delivered.
2. The updated status is reflected immediately in the order history list.

---

### ORD-US-07 — Delete an unpaid order
**As** a store assistant, **I want to** delete an unpaid order **so that** I can remove cancelled or erroneous orders.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. Only unpaid orders can be deleted.
2. A confirmation dialog is shown before deletion.
3. Deleting an order also deletes all its line items.
4. Paid orders are protected from deletion.

### ORD-US-09 — Print an order
**As** a store assistant, **I want to** print an order **so that** I can give the customer a physical copy or keep a paper record.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. A print action is available on the order detail / edit screen.
2. The printed output includes: order ID, customer name and contact, order date, line items (product, quantity, unit, unit price, line total), order total, and payment/delivery status.
3. Print triggers the device's standard share/print dialog.

---

### ORD-US-10 — View order detail
**As** a store assistant, **I want to** view the full details of any order **so that** I can see exactly what was ordered, at what price, for which customer, and take quick actions without having to enter edit mode.

**Actor:** Store Assistant  
**Status:** ✅ *(**OrderDetailScreen** — line items, totals, paid/delivered toggles, print, **Edit** when unpaid)*

**Acceptance criteria:**
1. Tapping any order in the history list (ORD-US-03) opens this detail screen — for both paid and unpaid orders.
2. The screen displays the complete order picture:
   - Order ID and dates (order date, last updated date)
   - Customer name, contact number, and customer type
   - Sales channel (online / reseller / offline)
   - Line items table: product name, quantity, unit type (kg / piece), unit price, line total
   - Order total
   - Payment status (paid / unpaid)
   - Delivery status (delivered / not delivered)
3. The following actions are available on this screen:
   - **Print** — triggers the device print/share dialog (ORD-US-09)
   - **Mark as Paid / Unpaid** — toggles payment status (ORD-US-05)
   - **Mark as Delivered / Not Delivered** — toggles delivery status (ORD-US-06)
   - **Edit** — navigates to edit mode (ORD-US-04); shown only for unpaid orders
4. Status toggles (paid, delivered) take effect immediately and are reflected on the detail screen without navigating away.
5. Standard Android back navigation returns to the order history list.

---

### ORD-US-08 — View active SRPs before taking an order
**As** a store assistant, **I want to** see the current active selling prices for all products across all channels **so that** I can answer customer price questions and confirm prices before placing an order.

**Actor:** Store Assistant  
**Status:** ✅ *(implemented in `ActiveSrpsScreen`; see pricing sections in `DESIGN.md` and formulas in `PricingReference.md`)*  

**Canonical reference:** PricingReference.md US-3a, FR-PC-36

**Acceptance criteria:**
1. A dedicated price list screen shows every product that has an active SRP, with the minimum per-kg SRP across channels shown as a summary ("from ₱X").
2. Expanding a product row reveals the full SRP grid: per-kg, per-piece (if applicable) for each channel (online, reseller, offline).
3. The list shows when the SRP was last updated and from which acquisition it was derived.
4. The store assistant cannot edit any SRP values from this screen — all values are read-only. Prices are only changed by management presets (MGT-US-01–03) and new acquisitions (INV-US-05).

---

## Epic 3 — Product Management

### PRD-US-01 — View the product catalog
**As** an admin, **I want to** view all products **so that** I can see what is available for sale and what each product's current selling price is.

**Actor:** Admin  
**Status:** ✅ *(**ManageProductsScreen** / **ManageProductsViewModel** — active SRP via `observeAllActiveSrps`, **From ₱X/kg** & **From ₱X/pc** via **OrderPricingResolver**; **Manual price** / **No price** / **Acquisition SRP** chips; manual peso amounts only in **ProductFormScreen** history; filter: unit type & category substrings + search includes **product_id**.)*

**Acceptance criteria:**
1. Products are listed with their ID, name, description, unit type, category, and active status.
2. Each product row shows the **active SRP** (from the most recent acquisition, per INV-US-06) as the current price. The summary displays the minimum per-kg SRP across channels ("from ₱X").
3. Products that have **no acquisition SRP yet** (running on manual fallback price only) are visually distinguished — e.g. a "Manual Price" badge — so the admin can identify which products still need an acquisition recorded.
4. Products with no price at all (no SRP and no fallback) show a "No Price" indicator.
5. Manual fallback prices are **not** shown on the product list — they are accessible only via the product's price history (PRD-US-07).
6. I can search products by name or ID.
7. I can filter by unit type, category, and active status.

---

### PRD-US-02 — Add a new product
**As** an admin, **I want to** add a new product **so that** it becomes available for ordering.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. I can enter a product name, description, and unit type (kg / pieces).
2. I can optionally assign a **product category** (from the list defined in MGT-US-03) so that the correct spoilage rate and hauling cost are applied for **per kg** acquisitions; **per piece** acquisitions still use hauling from the preset but **do not** apply spoilage in SRP per **CLARIF-01** / **§5.1.1**.
3. I can optionally enter a **default piece count** — the number of pieces per kg — used to compute per-piece SRP in INV-US-05.
4. The new product is immediately available for selection when taking orders and recording acquisitions.
5. New products default to active status.

---

### PRD-US-03 — Edit a product
**As** an admin, **I want to** edit a product's details **so that** the catalog stays accurate.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. I can update the name, description, unit type, category, and default piece count of any existing product.
2. Changing the category does not recompute SRPs on past acquisitions — those retain their preset snapshots.
3. Changes to name and description are reflected everywhere the product appears.

---

### PRD-US-04 — Deactivate a product
**As** an admin, **I want to** deactivate a product **so that** it no longer appears for new orders without deleting its history.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. I can toggle a product's active status.
2. Inactive products are excluded from the product selection when taking orders.
3. Existing orders with inactive products are not affected.

---

### PRD-US-05 — Delete a product
**As** an admin, **I want to** delete a product **so that** I can remove items that are no longer relevant.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. A confirmation dialog is shown before deletion.
2. Products referenced by existing order items cannot be deleted (restricted by foreign key).

---

### PRD-US-06 — Set a manual fallback price for a product
**As** an admin, **I want to** manually set a per-kg or per-piece price for a product **so that** the store assistant can still take orders for products that have not yet had an acquisition recorded.

**Actor:** Admin  
**Status:** ✅

> **Note:** Manual pricing is a **fallback only**. Once a product has an acquisition with a preset-computed SRP (INV-US-05), those SRPs take precedence over manual prices in all order flows. Manual prices are used only when no active acquisition SRP exists for a product.

**Acceptance criteria:**
1. I can set a per-kg price, a per-piece price, or both for any product as a fallback.
2. I can set discounted per-kg and per-piece fallback prices. Discounted prices apply automatically when the customer's type maps to the **reseller** channel (see CUS-US-05); standard prices apply for retail and regular customers.
3. The fallback price is used in order taking only when no preset-computed SRP is available for the product.
4. When a preset-computed SRP becomes available, the order form uses the SRP and the manual fallback is no longer shown.

---

### PRD-US-07 — View product price history
**As** an admin, **I want to** see the full price history for a product **so that** I can trace how SRPs have changed over time and which preset version drove each change.

**Actor:** Admin  
**Status:** ✅ *(**ProductFormScreen** — merged **product_prices** + **acquisitions** timeline; per-channel SRP lines; **preset_ref** → **PresetDetail** when role allows; **Current SRP** chip on the active acquisition lot (**INV-US-06**).)*

**Acceptance criteria:**
1. Price history shows all entries for a product in reverse chronological order.
2. Each entry indicates whether the price was **preset-computed** (from an acquisition via INV-US-05) or **manually set** (fallback via PRD-US-06), along with the date and the per-channel SRP values where applicable.
3. For preset-computed entries, the `presetRef` is shown so the entry can be linked back to the exact preset version that produced the SRP (MGT-US-05).
4. The most recent active SRP is clearly marked as current.

---

### PRD-US-08 — Reinitialize products from seed data
**As** an admin, **I want to** reload the default product list from the app's built-in data **so that** I can restore the catalog to its initial state.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. The action imports products from `assets/data/products.json` and `assets/data/product_prices.json`.
2. A confirmation is shown before proceeding.

---

## Epic 4 — Customer Management

### CUS-US-01 — View all customers
**As** a store assistant, **I want to** view all customers **so that** I can find who to assign an order to.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. Customers are listed with first name, last name, contact number, address, and customer type.
2. I can search customers by name, contact number, or type.

---

### CUS-US-02 — Add a new customer
**As** a store assistant, **I want to** add a new customer **so that** I can take orders for them.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. Required fields: first name, last name, contact number, customer type (RETAIL / WHOLESALE / REGULAR).
2. Optional fields: address, city, province, postal code.
3. The new customer is immediately available when taking orders.

---

### CUS-US-03 — Edit a customer
**As** a store assistant, **I want to** edit a customer's details **so that** contact and address information stays current.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. All customer fields can be updated.
2. Changes are reflected in all associated orders.

---

### CUS-US-04 — Delete a customer
**As** a store assistant, **I want to** delete a customer **so that** inactive contacts are removed from the list.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. A confirmation dialog is shown before deletion.
2. A customer with unpaid orders cannot be deleted until those orders are resolved.

---

### CUS-US-05 — Customer type maps to a default sales channel
**As** a store assistant, **I want** the sales channel to be pre-selected based on the customer's type when I start a new order **so that** the correct SRP is applied automatically without me having to choose a channel manually each time.

**Actor:** Store Assistant  
**Status:** ✅ *(**TakeOrderViewModel.selectCustomer** → **`CustomerType.defaultOrderChannel()`**; channel remains overridable)*

**Mapping:**

| Customer type | Default channel | SRP tier used |
|---------------|-----------------|---------------|
| WHOLESALE | reseller | Reseller SRP (25% markup) |
| RETAIL | offline | Offline SRP (30% markup) |
| REGULAR | offline | Offline SRP (30% markup) |

**Acceptance criteria:**
1. When a customer is selected on the order form, the channel is pre-set to the customer's default channel per the mapping above.
2. The store assistant can override the pre-selected channel for a specific order if needed.
3. Changing the channel updates all pre-filled SRPs in the cart immediately.
4. The channel used is saved on the order record.

---

## Epic 5 — Inventory & Acquisitions

### INV-US-01 — Record a product acquisition
**As** a purchasing assistant, **I want to** record when and where I acquired produce **so that** there is a record of inventory intake and cost.

**Actor:** Purchasing Assistant  
**Status:** ✅ *(per-piece **§5.1.1** — see `PricingReference.md` and `pricing_clarif.md`)*  

**Acceptance criteria:**
1. I can select a product, enter quantity, price per unit, acquisition date, and source location (Farm / Market / Supplier / Other). In **per kg** mode, quantity is kg (decimal); in **per piece** mode, quantity is **total pieces** in the lot (**PricingReference.md** §5.1.1).
2. The total amount is calculated automatically.
3. I can toggle whether the unit is per kg or per piece.
4. I can enter a **piece count** (**pieces per kg** \(n\)) when per-piece SRP is needed: it converts **total pieces → kg** for the cost/SRP pipeline (`Q = quantity / n`) and defines **`SRP_piece = ⌈SRP / n⌉`** in INV-US-05. If the selected product has a default piece count (PRD-US-02), it is pre-filled; I can override it for this acquisition.

---

### INV-US-02 — View acquisition history
**As** a purchasing assistant or admin, **I want to** view past acquisitions **so that** I can review procurement history and costs.

**Actor:** Purchasing Assistant, Admin  
**Status:** ✅

**Acceptance criteria:**
1. Acquisitions are listed with product name, quantity, unit type (kg / piece), price per unit, total amount, date, and location.
2. `piece_count` is **not** shown in the list row — it is visible only when opening a specific acquisition record for detail or edit.
3. I can filter by product, location, or date range.

---

### INV-US-03 — Edit an acquisition
**As** a purchasing assistant, **I want to** correct an acquisition record **so that** the data is accurate.

**Actor:** Purchasing Assistant  
**Status:** ✅

**Edit behaviour (Option C):** Fields are classified into two types with different save behaviour.

| Field type | Fields | On save |
|------------|--------|---------|
| **Cost inputs** | quantity, price per unit, piece count | SRPs are recomputed |
| **Metadata** | date, location, notes | SRPs are unchanged |

**Acceptance criteria:**
1. All fields can be edited.
2. When **only metadata fields** are changed, stored SRPs are left exactly as they were — no recomputation occurs.
3. When any **cost input field** is changed, SRPs are recomputed using the **preset snapshot stored on this acquisition** (`presetRef`) — not the currently active preset. This ensures the SRP change reflects only the corrected cost, not any preset policy changes that may have occurred since.
4. The `presetRef` on the acquisition record is **never changed** on edit — the acquisition remains permanently tied to the preset that was active when it was first saved.
5. If the acquisition is the most recent for its product (and therefore the active SRP per INV-US-06), the recomputed SRP immediately becomes the new active SRP.

---

### INV-US-04 — Delete an acquisition
**As** a purchasing assistant, **I want to** delete an incorrect acquisition record **so that** the inventory history is clean.

**Actor:** Purchasing Assistant  
**Status:** ✅

**Acceptance criteria:**
1. A confirmation dialog is shown before deletion.

### INV-US-06 — Active SRP is always the most recent acquisition
**As** a store assistant or purchasing assistant, **I want** to know which SRP is considered "active" for a product **so that** orders and price lists always reflect the latest purchase cost.

**Actor:** Store Assistant, Purchasing Assistant  
**Status:** ✅ *(latest by **date_acquired**, tiebreak **created_at**; behavior documented in `DESIGN.md`)*  

**Acceptance criteria:**
1. The **active SRP** for a product is the SRP computed from the **most recently saved acquisition** for that product, determined by acquisition date (not save timestamp).
2. If two acquisitions share the same date, the one saved most recently (by DB insert time) is the active one.
3. When a new acquisition is saved for a product, its SRP immediately becomes the active SRP — no separate "mark active" step is needed.
4. Deleting the most recent acquisition restores the previous acquisition's SRP as the active one. If no prior acquisition exists, the product falls back to the manual fallback price (PRD-US-06).
5. The active SRP is the value pre-filled when the product is added to an order (ORD-US-01) and the value shown on the price list screen (ORD-US-08).
6. Every acquisition's stored SRP is permanently traceable to the preset that produced it via `presetRef` — this link is preserved even after that preset is deactivated (MGT-US-06). Viewing any acquisition's SRP detail can show the full preset snapshot that was active at save time.

---

### INV-US-05 — Auto-calculate suggested selling prices from acquisition cost
**As** a purchasing assistant, **I want** the app to automatically compute suggested selling prices for each sales channel whenever I enter an acquisition **so that** the store always has consistent, formula-driven SRPs derived from actual purchase cost — without me having to do the math manually.

**Actor:** Purchasing Assistant  
**Status:** ✅ *(pipeline + per-piece **§5.1.1** — `SrpCalculator` / `AcquisitionRepository`; see `PricingReference.md` and `pricing_clarif.md`)*  

**Pricing formula** *(normative in this story; shared IDs in **PricingReference.md** §4.3, §4.3.1 (CLARIF-01 — **`docs/pricing_clarif.md`**), §5.1.1, §5.2 **FR-PC-02**, **FR-PC-10–14**, **US-6** — spec **0.9.33-draft**+; pipeline order: **\(C_{\text{bulk}} + A\)** before channel markup)*

Given:
- **B** = total acquisition cost (`quantity × price_per_unit`)
- **Q** = quantity in **kg** for the pipeline. When the acquisition unit is **per kg**, **Q** is the entered quantity. When the unit is **per piece**, **Q** is **derived**: `Q = quantity / piece_count` where **quantity** = total pieces in the lot and **`piece_count`** = **Estimated Qty per Kg** (pieces per kg, user input — **PricingReference.md** §5.1.1). Preset **\(A\)** and markups are **not** overridden; only how **Q** is obtained changes.
- **Spoilage (by-weight only — `pricing_clarif.md` line 10):** preset (or policy) supplies **either** a **rate** **`s`** (fraction of acquired kg, default illustration **25%**; category overrides) **or** **absolute unsellable kg** **`s_kg`** for the lot. Stored on the acquisition snapshot for audit per **BUG-PRC-04** / **FR-PC-10**. **CLARIF-01 / §5.1.1:** for **per piece** acquisitions, **spoilage is not applied in SRP math** — use **`s_eff = 0`** so **`Q_sell = Q`** regardless of preset.
- **A** = required additional cost per kg (hauling) = (driver fee + fuel + toll + handling) / hauling weight — **not** the same symbol as CLARIF “**A**” in the by-weight row (**§4.3.1** maps CLARIF **A** there to **\(C_{\text{bulk}}\)**).
  - Defaults: (2,000 + 4,000 + 1,000 + 200) / 700 kg = **≈ 10.29 PHP/kg**
- Channel markups (management-configurable): **online 35%**, **offline 30%**, **reseller 25%**

Pipeline (executed for each channel):
1. **Sellable quantity:** **`Q_sell = Q × (1 − s)`** when using **rate** spoilage, **or** **`Q_sell = Q − s_kg`** when using **absolute kg** (by-weight only — **`pricing_clarif.md`**, **BUG-PRC-04**). When the acquisition unit is **per piece**, **`Q_sell = Q`** (**`s_eff = 0`** — **CLARIF-01**, **PricingReference.md** §5.1.1).
2. **Bulk cost per sellable kg** (spoilage in the divisor only when **per kg**): `C_bulk = B / Q_sell`. **Combined base before channel policy:** `C = C_bulk + A` (**PricingReference.md** §4.3 / **FR-PC-10** — hauling **A** is **not** folded into **C_bulk**).
3. **Price after markup:** `priceAfterCore = C × (1 + channel_markup)`
4. **Round up to nearest whole PHP:** `SRP = ⌈priceAfterCore⌉`
5. **Fractional packages** (per channel): `SRP_500g = ⌈SRP × 0.5⌉`, `SRP_250g = ⌈SRP × 0.25⌉`, `SRP_100g = ⌈SRP × 0.1⌉`
6. **Per piece** (when **piece_count** \(n\) is provided): `SRP_piece = ⌈SRP / n⌉` (same **\(n\)** as pieces per kg in the per-piece entry path).

**Per-piece lot entry (aligned with PricingReference §5.1.1 / CLARIF-01):**
- Assistant enters **total cost** \(B\), **total piece count** (as **quantity** in per-piece mode), and **pieces per kg** (**`piece_count`** — **Estimated Qty per Kg**).
- **Spoilage** is **not** part of per-piece SRP calculations: **`s_eff = 0`**, **`Q_sell = Q`** (preset **`spoilage_rate`** still saved on the snapshot for traceability).
- **Additional cost** **\(A\)** stays the preset’s **PHP per kg** (haul model / `additional_cost_per_kg`); it is applied after **\(Q\)** is known — **not** replaced by a separate per-piece hauling formula.
- **SRP per kg**, fractional packs, and **per-piece** selling prices use the **same** markup, rounding, and tier rules as a lot entered directly in kg. **CLARIF** per-piece **B** = **(pieces / n) × additional costs** = **`Q × A_spec`** (lot hauling PHP); **per-piece** hauling share **`B / P_tot = A_spec / n`**, matching **`C_bulk / n`** algebra when **`Q_sell = Q`**. Canonical per-piece **SRP** in **`pricing_clarif.md`**: **(A + B/total_quantity)** × (1+μ). Legacy sheets that used **(A + B)** with lot **B** should use **(A + B/P_tot)** instead — **PricingReference.md** §4.3.1.

**Acceptance criteria:**
1. As I fill in quantity and price per unit on the acquisition form, suggested SRPs for **online**, **reseller**, and **offline** channels are computed and displayed in real time.
2. The SRP computation uses the **currently active preset** (MGT-US-06) — the one explicitly tagged as active by management. If no preset has been activated yet, SRP computation is unavailable and the form shows a notice prompting management to activate a preset.
3. The purchasing assistant sees the active preset's resolved values (spoilage **rate** / policy per **BUG-PRC-04**, hauling cost, channel markups) as read-only context on the form. They cannot edit preset values except where **BUG-PRC-04** adds an optional per-line **spoilage kg** override.
4. Fractional package SRPs (500g, 250g, 100g) are shown for each channel.
5. If a piece count is entered for the product, per-piece SRP is also shown for each channel.
6. All SRPs are rounded **up** to the nearest whole PHP (e.g. 153.40 → 154, 165.50 → 166).
7. SRPs are saved on the acquisition record alongside the cost data.
8. If `Q_sell = 0` from the effective pipeline (e.g. **per kg** with **100%** rate spoilage, **s_kg ≥ Q**, or **Q ≤ 0**), validation blocks save with a clear error message. **Per piece:** **`Q_sell = Q`** implies **Q** must be positive (invalid **`piece_count`** / quantity).
9. The full preset snapshot (spoilage fields per **BUG-PRC-04** / **FR-PC-10**, hauling fees, channel markups) and a `presetRef` pointing to the active preset record are stored on the acquisition at save time. This snapshot is immutable after save — it is never updated, even if the preset is later deactivated or modified.
10. When **unit is per piece**, **quantity** is the **total number of pieces** in the lot, **`piece_count`** is **pieces per kg** (user input); the app derives **\(Q\)** in kg and applies **\(C_{\text{bulk}}\)** / **\(A\)** / markup with **no spoilage in the divisor** (**`Q_sell = Q`**, **CLARIF-01**). Preset snapshot still records **`spoilage_rate`** from the active preset for audit.

**Optional override (policy):** Per-line **custom / customer SRP per sales channel** on the same form, replacing preset-computed values for that save only, is specified in **MGT-US-07** (Epic 10).

---

## Epic 6 — Employee Management

### EMP-US-01 — View all employees
**As** an admin, **I want to** view all employees **so that** I can manage the team.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. Employees are listed with their ID, full name, contact number, and registration dates.
2. I can search employees by name.

---

### EMP-US-02 — Add an employee
**As** an admin, **I want to** add a new employee **so that** I can track their work and pay them.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. Required fields: first name, last name, contact number.
2. The new employee is immediately available for payment recording.

---

### EMP-US-03 — Edit an employee
**As** an admin, **I want to** update employee details **so that** contact information stays current.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. All employee fields can be updated.

---

### EMP-US-04 — Delete an employee
**As** an admin, **I want to** remove an employee who is no longer part of the team.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. A confirmation dialog is shown before deletion.
2. Employees with payment records are protected from deletion (restricted by foreign key).

---

### EMP-US-05 — Record an employee payment
**As** an admin, **I want to** record when I pay an employee **so that** there is an auditable payroll history with a clear breakdown of wages, advances, and deductions.

**Actor:** Admin  
**Status:** ✅

**Field definitions:**

| Field | Meaning |
|-------|---------|
| `amount` | Gross wage for the period. |
| `cash_advance_amount` | New cash advanced to the employee **in this same payment event** (optional, nullable). For **net pay** it is **added** to gross on add/edit and on each history row (same rule everywhere). |
| `liquidated_amount` | Amount applied in **this** payment to settle part or all of a cash advance from a **prior** payment (optional, nullable). **Recording and reporting only:** it affects **outstanding advance balance** (EMP-US-06) but **must not** change the displayed **net pay** line on the form or in history. |
| `date_paid` | Date this payment was made by the admin |
| `received_date` | Date the employee physically received the cash (optional, nullable; may differ from `date_paid`) |

**Net pay (this transaction — canonical formula):**

```
net_pay = amount + cash_advance_amount
```

Treat a null `cash_advance_amount` as `0`. **`liquidated_amount` is not part of this formula** (see field table).

The app may show a warning when computed values are invalid or inconsistent (e.g. negative inputs if ever allowed).

**Outstanding advance balance (lifetime, per employee — reporting only):**

```
outstanding = sum(cash_advance_amount) - sum(liquidated_amount)
```

across **all** of that employee’s payment rows (not limited to one period). See EMP-US-06 for how this appears in history.

> Example: Gross wage ₱5,000 + new cash advance ₱1,000 ⇒ **net pay ₱6,000**. A **liquidated** amount of ₱500 in the same row is still shown for audit and affects **outstanding advance** only, not net pay.

**History consistency:** Each row in **employee payment history** (EMP-US-06) MUST display **net pay** using this **same** formula (**gross + cash advance**, liquidated excluded) so the audit trail matches add/edit (`PaymentFormScreen`).

**UX (implementation target):** Add and edit use a **full-screen** form (`PaymentFormScreen` / nav route `employee_payment_form/{employeeId}/{employeeName}/{paymentId}`, `paymentId = -1` for new) with `imePadding`, standard system back, and a live read-only **net pay** summary block — not a multi-field `AlertDialog`.

**Acceptance criteria:**
1. I must enter gross wage (`amount`) and **date paid**. **Date received** is optional.
2. I can optionally enter a `cash_advance_amount` for new advance in this transaction.
3. I can optionally enter a `liquidated_amount` to record recovery of a previously given cash advance. It reduces the employee's **outstanding advance balance** (tracked across all their payment records) but **does not** change **net pay**.
4. The form displays the computed **net pay** in real time as I fill in the fields, using the **canonical formula** above (gross + cash advance only).
5. I can capture the employee's **signature** as acknowledgment of receipt. The signature input supports two modes: finger-drawn on screen (stored as Base64 PNG), or typed name.
6. The payment record is linked to the specific employee.

---

### EMP-US-06 — View employee payment history
**As** an admin, **I want to** view all payments for a specific employee with a running balance of outstanding advances **so that** I always know how much advance the employee still owes back.

**Actor:** Admin  
**Status:** ✅

**Net pay on each row:** Must match EMP-US-05 exactly:

```
net_pay = amount + cash_advance_amount
```

(null `cash_advance_amount` as `0`; **liquidated excluded** from net pay).

**Acceptance criteria:**

1. **Per-payment list (required)** — For each payment, the history shows at least: payment ID, gross wage (`amount`), cash advance (show 0 when none), liquidated (show 0 when none), **net pay** (formula above), **date paid**, **received date when set** (if unset, the UI may omit or label “Not set”), and an indication of **signature** (typed value or that an image was captured). Rows are scoped to the selected employee.
2. **Time filter (required)** — I can limit the list by period using presets by `date_paid`: *All Time*, *Today*, *This Week*, *This Month*, *Last month*, *Last 3 months*, *Last 6 months*, and *Custom date range*.
3. **Period summary card** — For the **currently filtered** payments, a card shows: total gross (`sum amount`), total cash advances (`sum cash_advance_amount`), total liquidated (`sum liquidated_amount`). Implemented in `EmployeePaymentScreen` (below the outstanding card).
4. **Lifetime outstanding advance** — Always-visible card:  
   `sum(cash_advance_amount) − sum(liquidated_amount)` over **all** that employee’s payments (ignore list filter). Implemented in `EmployeePaymentScreen`.

**Implementation notes:** `PaymentCard`, period filter (AC2a + AC2b including custom range controls), period summary (AC3), and lifetime outstanding (AC4) are in `EmployeePaymentScreen`.

---

### EMP-US-07 — Edit or delete an employee payment
**As** an admin, **I want to** correct or remove a payment record **so that** payroll data is accurate.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. All payment fields can be updated.
2. A confirmation dialog is shown before deletion.

---

## Epic 7 — Farm Operations

### FOP-US-01 — Log a farm operation
**As** a farmer, **I want to** log a farm activity **so that** there is a record of what was done in the field.

**Actor:** Farmer  
**Status:** ✅

**Screen:** This is the **entry screen** reached from the Farm Operations dashboard card. It is a dedicated form for recording a new operation — not the history list.

**Acceptance criteria:**
1. The Farm Operations card on the dashboard navigates directly to this log form.
2. I can select an operation type: Sowing, Harvesting, Pesticide Application, Fertilizer Application, Irrigation, Weeding, Pruning, or Other.
3. I can optionally link the operation to a specific product.
4. I can record: operation date, details/notes, area/field identifier, weather condition, and personnel involved.
5. A clearly visible **View History** button navigates to the Farm Operations history screen (FOP-US-02) without submitting the current form.

---

### FOP-US-02 — View farm operation history
**As** a farmer or admin, **I want to** view past farm operations **so that** I can track what has been done across the farm.

**Actor:** Farmer, Admin  
**Status:** ✅

**Screen:** This is a **separate screen** from the log form (FOP-US-01), reached via the View History button on FOP-US-01.

**Acceptance criteria:**
1. Operations are listed with type, date, product (if any), area, and details, newest first.
2. I can filter by operation type and date range.
3. I can search by operation details.
4. I can edit or delete any operation from this list (FOP-US-03, FOP-US-04).
5. Standard Android back navigation returns to the log form (FOP-US-01).

---

### FOP-US-03 — Edit a farm operation
**As** a farmer, **I want to** update a farm operation record **so that** the log stays accurate.

**Actor:** Farmer  
**Status:** ✅

**Acceptance criteria:**
1. All fields of a farm operation can be updated after creation.

---

### FOP-US-04 — Delete a farm operation
**As** a farmer, **I want to** remove an incorrect farm operation entry **so that** the operation log is clean.

**Actor:** Farmer  
**Status:** ✅

**Acceptance criteria:**
1. A confirmation dialog is shown before deletion.

---

## Epic 8 — Remittances & disbursements

**Definitions:**
- **Remittance** — Cash or equivalent that the **store assistant** turns over from sales (sales proceeds). This is the existing **Remittance** behavior (**REM-US-01–03**).
- **Disbursement** — Money **received by purchasing** (the purchaser), e.g. working float, capital injection, or supplier credit received — **not** the same as payroll (**employee_payments**).

**UI:** Both kinds are recorded on the **same navigation destination** as today’s **Remittance** feature (`RemittanceScreen`, `RemittanceFormScreen`). The list combines entries; filters and labels distinguish type (**DISB-US-02**).

**Data:** Extend the **`remittances`** table with **`entry_type`**: `REMITTANCE` \| `DISBURSEMENT` (default `REMITTANCE` for existing rows). Alternative names (`transaction_kind`, `direction`) are acceptable if documented; behavior must match this epic.

---

### REM-US-01 — Record a remittance
**As** a store assistant, **I want to** record a remittance **so that** sales proceeds handed in are logged.

**Actor:** Store Assistant  
**Status:** ✅ *(form evolves: new saves default to `entry_type = REMITTANCE` — see **DISB-US-02**)*

**Acceptance criteria:**
1. I can enter an amount, date, and optional remarks.
2. The amount input supports currency formatting.
3. New remittance rows are stored with **`entry_type = REMITTANCE`**.

---

### REM-US-02 — View remittance history
**As** a store assistant or admin, **I want to** view remittance activity **so that** I can track sales proceeds turned in.

**Actor:** Store Assistant, Admin  
**Status:** ✅ *(combined list, filters, and dual totals — **DISB-US-02**)*

**Acceptance criteria:**
1. Remittances are listed with amount, date, and remarks, newest first.
2. A summary card shows the total amount for the current view.
3. I can search by amount, date, or remarks.

---

### REM-US-03 — Edit or delete a remittance
**As** a store assistant, **I want to** correct or remove a remittance record **so that** the transaction log is accurate.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. All remittance fields can be updated **only** for rows with **`entry_type = REMITTANCE`** (or legacy null treated as remittance). Store assistant cannot edit **disbursement** rows (**DISB-US-03** / RBAC).
2. A confirmation dialog is shown before deletion.

---

### DISB-US-01 — Record a disbursement
**As** a purchasing assistant or admin, **I want to** record a disbursement **so that** money received for purchasing is tracked alongside store remittances.

**Actor:** Purchasing Assistant, Admin  
**Status:** ✅

**Acceptance criteria:**
1. From the **same** **Remittance** hub screen, I can add an entry of type **Disbursement** (e.g. FAB / **Add disbursement** or type toggle on the existing add flow).
2. Fields mirror a remittance: **amount**, **date**, **optional remarks** — same validation and currency formatting as **REM-US-01**.
3. Saved rows use **`entry_type = DISBURSEMENT`**.
4. **Store assistant** cannot create disbursements (`Rbac` / ViewModel enforcement).

---

### DISB-US-02 — View remittances and disbursements on one screen
**As** a user with access to the Remittance feature, **I want to** see remittances and disbursements in one place **so that** daily cash movement is clear.

**Actor:** Store Assistant, Admin, Purchasing Assistant (view); see RBAC for who sees which filters and totals  
**Status:** ✅

**Acceptance criteria:**
1. **`RemittanceScreen`** shows a **single chronological list** of both `REMITTANCE` and `DISBURSEMENT` rows (newest first), each row labeled or badged by type.
2. **Filter chips** (or dropdown): **All** | **Remittances only** | **Disbursements only**; search continues to match amount, date, remarks.
3. Summary area: **total remittances** and **total disbursements** for the **current filter** (when “All”, show both subtotals or two-line summary).
4. Purchasing assistant gains **dashboard access** to this screen for view + add disbursement (**`user_roles.md`**, **`Rbac`**).

---

### DISB-US-03 — Edit or delete a disbursement
**As** a purchasing assistant or admin, **I want to** correct or remove a disbursement **so that** purchasing cash records stay accurate.

**Actor:** Purchasing Assistant, Admin  
**Status:** ✅

**Acceptance criteria:**
1. Opening **`RemittanceFormScreen`** on a **`DISBURSEMENT`** row allows edit/delete with the same patterns as **REM-US-03**.
2. **Store assistant** cannot edit or delete disbursement rows.
3. Thermal **PRN-03** slip for a disbursement prints a distinct title (e.g. **DISBURSEMENT RECEIPT**) with the same body fields — see **`docs/printing.md`**.

---

## Epic 9 — Data Export

### EXP-US-01 — Export data to CSV
**As** an admin, **I want to** export any data table to a CSV file **so that** I can analyze data outside the app or keep backups.

**Actor:** Admin  
**Status:** ✅

**General rules:**
- Exported files are timestamped: `<table>_YYYYMMDD_HHmmss.csv`
- Every row includes a `DeviceId` column for cross-device tracking
- All entity fields are included (no fields omitted)
- Foreign key IDs are kept; related human-readable fields are added alongside them (denormalized)
- Text fields containing commas are quoted

**Column specs per export:**

**customers.csv**
`CustomerId, FirstName, LastName, Contact, CustomerType, Address, City, Province, PostalCode, DateCreated, DateUpdated, DeviceId`

**products.csv**
`ProductId, ProductName, ProductDescription, UnitType, Category, DefaultPieceCount, IsActive, DeviceId`

**product_prices.csv** *(manual fallback prices — PRD-US-06)*
`PriceId, ProductId, ProductName, PerKgPrice, PerPiecePrice, DiscountedPerKgPrice, DiscountedPerPiecePrice, DateCreated, DeviceId`

**orders.csv**
`OrderId, CustomerId, CustomerName, CustomerContact, Channel, TotalAmount, OrderDate, OrderUpdateDate, IsPaid, IsDelivered, DeviceId`

**order_items.csv**
`Id, OrderId, ProductId, ProductName, Quantity, PricePerUnit, IsPerKg, TotalPrice, DeviceId`

**employees.csv**
`EmployeeId, FirstName, LastName, Contact, DateCreated, DateUpdated, DeviceId`

**employee_payments.csv**
`PaymentId, EmployeeId, EmployeeName, Amount, CashAdvanceAmount, LiquidatedAmount, DatePaid, ReceivedDate, Signature, IsFinalized, DeviceId` *(exported column **Amount** is gross wage; **NetPay** is not a separate CSV column — compute as **Amount − CashAdvanceAmount + LiquidatedAmount** if needed)*

**farm_operations.csv**
`OperationId, OperationType, OperationDate, Details, Area, WeatherCondition, Personnel, ProductId, ProductName, DateCreated, DateUpdated, DeviceId`

**acquisitions.csv** *(computed SRPs, hauling snapshot JSON, channels snapshot, **SrpCustomOverride** — see **CsvExportService.exportAcquisitions**; column set may differ slightly from the narrative list below as the schema evolved)*
`AcquisitionId, ProductId, ProductName, Quantity, PricePerUnit, TotalAmount, IsPerKg, PieceCount, DateAcquired, CreatedAt, Location,`
`PresetRef, SpoilageRate, SpoilageKg, AdditionalCostPerKg, HaulingWeightKg, HaulingFeesJson, ChannelsSnapshotJson,`
`SrpOnlinePerKg, SrpResellerPerKg, SrpOfflinePerKg,`
`SrpOnline500g, SrpOnline250g, SrpOnline100g,`
`SrpReseller500g, SrpReseller250g, SrpReseller100g,`
`SrpOffline500g, SrpOffline250g, SrpOffline100g,`
`SrpOnlinePerPiece, SrpResellerPerPiece, SrpOfflinePerPiece,`
`SrpCustomOverride,`
`DeviceId`

**remittances.csv**
`RemittanceId, EntryType, Amount, Date, Remarks, DateUpdated, DeviceId` — **`EntryType`** = `REMITTANCE` \| `DISBURSEMENT` (**DISB-US-01–03**)

**Acceptance criteria:**
1. All 10 tables listed above can be exported individually. **Users** also have a dedicated export. **Selective batch export** (**ExportBundleTable**) includes the same core tables plus **Users** but not **product_prices** — export product prices with the single-table action.
2. Exported files are saved to the app's external files directory (`exports/`) and can be shared/downloaded from the device.
3. SRP and preset-related columns on `acquisitions.csv` are included (**INV-US-05** implemented).

---

### EXP-US-02 — Clear specific data tables
**As** an admin, **I want to** truncate any data table **so that** I can reset test data before going live.

**Actor:** Admin  
**Status:** ✅ *(**ExportScreen** — admin **Clear tables (EXP-US-02)** checklist, **Select all / none**, FK dependency prompts (products→acquisitions, customers→orders, employees→payments), two-step confirm; **`ExportViewModel.clearSelectedTables`** runs deletes in safe order; **Users (except default admin & user)**; **Pricing presets & activation log** in one transaction; **BUG-02** fixed — order line items-only path is `OrderDao.truncateOrderItems` / `OrderRepository.truncateOrderItemsOnly`. **Not in scope:** day-close tables; full `users` wipe (would require re-seed outside DB callback).)*

**Acceptance criteria:**
1. The screen presents a **checklist** of all clearable tables. The admin selects one or more before confirming:
   - Customers
   - Products
   - Product Prices
   - Orders & Order Items *(always cleared together)*
   - Employees
   - Employee Payments
   - Acquisitions
   - Farm Operations
   - Remittances
   - Pricing Presets & Activation Log *(always cleared together)*
2. A **Select All** / **Deselect All** toggle is available for convenience.
3. Before the admin can confirm, the UI validates the selection for foreign key dependency order and highlights conflicts — for example, if Products is checked but Acquisitions is not, a warning states "Acquisitions reference Products — include Acquisitions in this clear?" with a one-tap option to auto-add the dependent tables to the selection.
4. A confirmation dialog summarises exactly which tables will be cleared before the action executes.
5. The clear executes selected tables in the correct FK-safe order regardless of the order the admin checked them:
   - Employee Payments before Employees
   - Order Items cascade automatically when Orders is cleared
   - Acquisitions before Products
   - Preset Activation Log clears automatically when Pricing Presets is cleared
6. The action is irreversible; the confirmation dialog makes this explicit.

---

## Epic 10 — Pricing Presets (Management)

### MGT-US-00 — Settings screen and Pricing Presets navigation
**As** an admin, **I want** a Settings screen accessible from the top bar **so that** I can manage pricing presets without cluttering the main dashboard.

**Actor:** Admin  
**Status:** ✅ *(**SettingsScreen** → **PricingPresetsHomeScreen**; `Rbac` — **NavGraph** / **MainScreen**)*

**Screen structure:**

```
Top bar → Settings (admin only)
  └── Settings Screen
        └── Pricing Presets
              ├── Active Preset Summary         ← current active preset at a glance
              ├── New Preset          ──────────→ Preset Editor Screen
              │                                     ├── Section: Spoilage & Hauling (MGT-US-01)
              │                                     ├── Section: Channel Markups (MGT-US-02)
              │                                     └── Section: Categories (MGT-US-03)
              └── Preset History      ──────────→ Preset History Screen (MGT-US-05)
                    └── Tap any entry ──────────→ Preset Detail Screen
                                                      ├── Full snapshot view
                                                      ├── Restore → Preset Editor (as draft)
                                                      └── Activate → Preset Preview Screen (MGT-US-04)
                                                                        └── Confirm → activates (MGT-US-06)
```

**Acceptance criteria:**
1. Settings is accessible via the top bar on the dashboard and is visible only to admin users (SYS-US-02 AC#7).
2. The Settings screen contains a **Pricing Presets** entry. Additional settings categories may be added here in future without requiring dashboard changes.
3. The Pricing Presets home screen shows a summary of the currently active preset (name/ID, activation date, key values) and two actions: **New Preset** and **Preset History**.
4. **New Preset** opens the Preset Editor screen. The editor is divided into clearly labelled sections — Spoilage & Hauling, Channel Markups, Categories — navigable without confusion. Saving the editor creates a new inactive preset.
5. **Preset History** opens a list of all saved presets, newest first. The active preset is visually distinguished. Tapping any entry opens the Preset Detail screen.
6. The Preset Detail screen shows the full snapshot and two actions: **Restore** (loads into Preset Editor as a draft) and **Activate**.
7. Tapping **Activate** navigates to the Preset Preview screen (MGT-US-04). The admin must complete the preview before the activation is committed — there is no way to skip it.
8. All screens in this flow support standard Android back navigation to return to the previous screen.

---

### MGT-US-01 — Define spoilage rate and hauling costs
**As** an admin, **I want to** configure the spoilage rate and hauling-based additional costs **so that** all SRP calculations automatically reflect the store's real operating costs without hardcoding them.

**Actor:** Admin  
**Status:** ✅ *(preset editor + saved snapshot on acquisitions; by-weight uses preset rate with optional per-line absolute `spoilage_kg` override on acquisition, aligned with **BUG-PRC-04** implementation.)*

**Canonical reference:** PricingReference.md US-6, §7.3, FR-PC-50–51

**Acceptance criteria:**
1. I can set a **default spoilage** for **by-weight** SRP: **either** a **rate** (fraction 0–0.99, e.g. 0.25 for **25%** of acquired kg) **or**, per **`pricing_clarif.md`** line 10, policy for **absolute unsellable kg** (e.g. **2 kg**) — detail in **BUG-PRC-04** / **PricingReference.md** **FR-PC-10** (farm app may ship rate-only first). Category overrides follow the same pattern when implemented.
2. I can configure the **hauling model** with individual named fee line items (e.g. driver fee, fuel, toll, handling) and a hauling weight in kg. The additional cost per kg `A` is derived automatically as `sum(fee amounts) / hauling weight`.
3. I can alternatively enter `A` as a direct override value instead of using the hauling model.
4. Before saving, the admin can enter an optional **preset name** (e.g. "Q2 2026 Rates"). If left blank, the system auto-generates a name from the save timestamp (e.g. "Preset 2026-04-02 14:30"). The preset is also assigned a unique system ID on save. Both the name and the ID are stored and displayed in the history list (MGT-US-05).
5. Saving a preset configuration creates a new **inactive** preset record in history. It does not affect SRP computations until explicitly activated (MGT-US-06).
6. Saved acquisition SRP records are **never** mutated when a new preset is saved or activated — they permanently retain the snapshot of the preset that was active at their save time.

---

### MGT-US-02 — Configure per-channel markup and rounding
**As** an admin, **I want to** set the markup percentage and rounding rule for each sales channel **so that** online, reseller, and offline SRPs reflect the different margin requirements for each channel.

**Actor:** Admin  
**Status:** ✅ *(channel policy in presets; **PricingChannelEngine** — optional fees / rounding as implemented)*

**Canonical reference:** PricingReference.md US-6, §4.3, §4.3.1 (preset table / CLARIF), §7.2, §11.1.2, FR-PC-05, FR-PC-14

**Channels:** online, reseller, offline

**Acceptance criteria:**
1. For each channel I can set exactly one of: **markup %** (e.g. 35%) or **margin %** — not both. The UI enforces this constraint and shows a validation error if both are set.
   - Let `C_bulk = B / Q_sell` and `A` = required additional cost per kg (same symbols as **INV-US-05** / **§4.3** — not CLARIF’s by-weight **“A”**, which is **C_bulk**; see **§4.3.1**). Combined base before channel policy: `C = C_bulk + A`.
   - Markup formula: `priceAfterCore = C × (1 + markup%)`
   - Margin formula: `priceAfterCore = C / (1 − margin%)`
2. For each channel I can set a **rounding rule**. Default is `ceil_whole_peso` (round up to the nearest whole PHP). Other supported rules: `nearest_whole_peso`, `nearest_0.25`.
3. For each channel I can optionally configure **channel-attributable fees** (fixed PHP amount or %) applied **after** `priceAfterCore` (post–markup/margin subtotal) and **before** final rounding — e.g. delivery surcharge, payment processing fee (**FR-PC-14** / **§11.1.9**).
4. Default values on first setup: online 35% markup, reseller 25% markup, offline 30% markup; all channels use `ceil_whole_peso` rounding.
5. Saving channel configuration creates a new inactive preset record (same as MGT-US-01 AC#4) — it does not take effect until activated (MGT-US-06).
6. The purchasing assistant sees the **active preset's** resolved channel values as read-only context on the acquisition form — they cannot edit them.

---

### MGT-US-03 — Manage product categories with per-category overrides
**As** an admin, **I want to** define product categories and assign category-specific spoilage rates and additional costs **so that** different product types use appropriate preset values when computing SRPs.

**Actor:** Admin  
**Status:** ✅ *(categories JSON in presets; product **category** field; acquisition form picker)*

**Canonical reference:** PricingReference.md US-6, §7.3, FR-PC-03, FR-PC-07, FR-PC-51

**Default categories:** Vegetables, Fruits, Other Dry Goods

**Acceptance criteria:**
1. I can create, rename, and delete product categories.
2. Each category can optionally override the store-default spoilage rate and/or `additionalCostPerKg`.
3. Categories that have no override inherit the store defaults.
4. The category list drives the product category picker shown on the acquisition form (INV-US-05). If no categories are defined, the picker is hidden and store defaults are used for all calculations.
5. Category changes are saved as part of a preset record (inactive until activated, per MGT-US-01 AC#4).
6. Deleting a category does not affect saved acquisition records that referenced it — those retain their immutable preset snapshots.

---

### MGT-US-04 — Preview SRP impact before activating a preset
**As** an admin, **I want to** see how a saved preset would change current active SRPs before I activate it **so that** I can avoid unintentional price shifts reaching the order-taking flow.

**Actor:** Admin  
**Status:** ✅ *(**PresetActivationPreview** route — required before activation per **MGT-US-00** AC#7)*

**Canonical reference:** PricingReference.md FR-PC-53, §15.2

**Acceptance criteria:**
1. Before activating any saved preset (MGT-US-06), I can trigger a preview that shows — for every product with an active SRP — the **currently stored SRP**, the **SRP that would result from the candidate preset**, and the **delta (PHP)** per channel.
2. Items with no SRP change are summarized as a count ("N items unchanged") rather than listed individually.
3. The preview is a dry run only — it does not activate the preset, create any records, or alter stored data.
4. After reviewing the preview I can proceed to activate or cancel. Cancel leaves the preset inactive with no side effects.

---

### MGT-US-05 — View preset history and trace SRPs to their preset
**As** an admin, **I want** a complete, immutable history of all saved presets — showing which ones were active and when — **so that** I can audit how pricing policy has changed and trace any stored SRP back to the exact preset that produced it.

**Actor:** Admin  
**Status:** ✅ *(**PresetHistory** / **PresetDetail** / activation log; **AC5:** **`AcquisitionFormScreen`** — tappable **Preset:** link when **`preset_ref`** set and role may open **PresetDetail** (**ADMIN**); others see read-only ID; **PresetDetail** shows a clear message if the preset row is missing — Stream B 2026-04-09.)*

**Canonical reference:** PricingReference.md FR-PC-54, §8.2

**Acceptance criteria:**
1. Every preset save appends an immutable record to the changelog containing: `presetId`, `savedAt` timestamp, `savedBy` identity, activation status (`active` / `inactive`), and the full preset snapshot. Records are never modified or deleted.
2. Every preset activation appends a separate event to the changelog: `activatedAt` timestamp, `activatedBy` identity, and the `presetId` being activated. The previously active preset is recorded as deactivated in the same event.
3. The management UI lists all preset records newest first. Each row shows: **preset name**, **preset ID**, saved date, saved by, active/inactive badge, and a summary of key values (default spoilage rate, per-channel markups). The single currently active preset is clearly highlighted.
4. Each entry has a **Restore** action: loads that snapshot into the preset editor as a new draft. Saving the draft creates a new inactive preset record — it does not overwrite history. Activating it then follows MGT-US-06.
5. From any acquisition's detail view, tapping the `presetRef` navigates to the corresponding preset record in this history, showing the full snapshot that was used to compute that acquisition's SRPs — even if the preset is now inactive.

---

### MGT-US-06 — Activate a saved preset
**As** an admin, **I want to** explicitly activate a saved preset **so that** all new acquisition SRP computations use the pricing policy I intend, and I remain in full control of when pricing policy changes take effect.

**Actor:** Admin  
**Status:** ✅ *(single active preset; activation log; preview gate — **PricingPresetRepository** / **NavGraph**)*

**Acceptance criteria:**
1. Any saved preset record (from MGT-US-01–03) can be activated from the preset history list (MGT-US-05) or from the preset detail view.
2. Activating a preset sets its status to `active` and sets the previously active preset's status to `inactive`. Only one preset can be active at any time.
3. The activation is recorded in the changelog immediately: `activatedAt`, `activatedBy`, `presetId` activated, `presetId` deactivated (MGT-US-05 AC#2).
4. From the moment of activation, all new acquisition SRP computations (INV-US-05) use this preset's values.
5. Activating a preset does **not** recompute SRPs on any existing acquisition records — those are immutable and remain tied to the preset that was active at their save time (INV-US-05 AC#9, INV-US-03 AC#4).
6. If no preset has ever been activated, the acquisition form displays a notice that SRP computation is unavailable until management activates a preset (INV-US-05 AC#2).
7. Before activating, the admin **must** complete the impact preview (MGT-US-04) — activation cannot be triggered without going through the preview screen first (MGT-US-00 AC#7).
8. The currently active preset is always clearly identified in the management UI — its **preset name**, **preset ID**, activation timestamp, and activated-by user are shown.

---

### MGT-US-07 — Custom / customer SRP per channel on acquisition (overrides preset for that item)
**As** a purchasing assistant, **I want** to optionally set **customer SRPs per sales channel** for a specific product on an acquisition **so that** the **online**, **reseller**, and **offline** prices customers see can differ from what the active preset would compute — e.g. for a special buy or one-off deal — **without** changing the global preset.

**Actor:** Purchasing Assistant  
**Status:** ✅ *(core implementation; device QA recommended)*

**Implementation (2026):** `acquisitions.srp_custom_override` (Room v6, **destructive rebuild** / fresh install — no incremental 5→6 migration); **`AcquisitionFormScreen`** switch + per-channel SRP/kg (numeric pad); **`SrpCalculator.outputFromCustomerSrpPerKg`** / **`mergeCostContextWithCustomSrps`**; **`AcquisitionRepository`** insert/update; CSV **`SrpCustomOverride`**; list card label on **`AcquireProduceScreen`**.

**Terminology:** **Customer SRP** means the **customer-facing** suggested retail price for that product in a given **sales channel** (the same three channels as **MGT-US-02** / **INV-US-05**: online, reseller, offline). Override is **per channel**: each channel can have its own customer SRP on that acquisition line (not a single blended price unless the UI offers a shortcut that fills all channels).

**Related:** **INV-US-03** (edit acquisition), **INV-US-05** (preset-based SRP pipeline); acquisition UI is **`AcquisitionFormScreen`**. This story does **not** let anyone edit preset definitions on the form (**MGT-US-02** AC#6 remains: preset parameters are read-only there).

**Acceptance criteria:**
1. On **add** and **edit acquisition**, for the product line being saved, the assistant can choose **preset-computed customer SRPs** (default, current behaviour) or enable a **custom / customer SRP override** for that line only.
2. When **custom override** is **off**, customer SRPs **per channel** are computed exactly as in **INV-US-05** from the active preset and acquisition cost; the saved acquisition stores the computed values and **`presetRef`** / preset snapshot as today.
3. When **custom override** is **on**, the assistant enters **customer SRP per channel** (at minimum **online**, **reseller**, and **offline** per kg, consistent with stored acquisition shape). **Each channel may be set independently.** Those values **replace** the preset-derived customer SRPs for **that acquisition record only**. The UI may still show the preset-based **preview** (read-only) **per channel** so the assistant can compare against intended customer prices.
4. **Fractional package** SRPs (500g / 250g / 100g) and **per-piece** SRPs, when shown: either (a) recomputed from the custom per-kg **customer** SRP for each channel using the same rounding rules as **INV-US-05**, or (b) offered as optional additional override fields — the chosen approach (a or b) is documented in implementation notes; behaviour must be consistent for order-taking and exports.
5. Saving the acquisition persists the **final** **per-channel customer SRPs** used at sale time (computed or custom) and preserves **immutability** after save: later preset activation does **not** rewrite that row (**INV-US-05** AC#9, **MGT-US-06** AC#5). The record remains traceable to the **`presetRef`** / snapshot that was active at save time; if override was used, stored customer SRPs reflect the override while audit data still links to the preset policy context.
6. **Orders** and **active product pricing** continue to use the **latest acquisition’s stored per-channel SRPs** for that product (**INV-US-03** / **ORD-US-01** precedence), respecting the **channel** of the sale. When the latest acquisition used a custom override, those **per-channel customer SRPs** drive pre-filled order prices until a newer acquisition replaces them.
7. **Export / history** can distinguish lines that used **preset-computed** vs **custom / customer** per-channel SRPs (e.g. flag or parallel columns) so management can audit exceptions without opening every detail screen.

**Schema:** Boolean **`srp_custom_override`** on **`acquisitions`**; per-channel values remain **`srp_*_per_kg`** (and derived pack/piece columns). See **`FarmDatabase`** v6.

---

## Epic 11 — System & Setup

### SYS-US-01 — First-time database initialization
**As** a new app installation, **I want** the database to be seeded with default data on first launch **so that** the app is usable immediately without manual data entry.

**Actor:** System (runs automatically)  
**Status:** ✅

**Acceptance criteria:**
1. On first launch, the app detects an empty database and runs an initialization sequence before navigating to the login screen.
2. The initialization imports: products from `assets/data/products.json`, product prices from `assets/data/product_prices.json`, and customers from `assets/data/customers.json`.
3. Two default user accounts are created: `admin` / `admin123` and `user` / `user123`. Both passwords are stored hashed, never as plaintext.
4. A progress screen is shown during initialization so the user knows the app is loading, not frozen.
5. Initialization runs only once; subsequent launches skip it and go directly to login (or dashboard if a session exists).
6. If initialization fails partway through, the app retries on next launch rather than leaving a partially seeded database.

---

### SYS-US-02 — Main dashboard navigation hub
**As** any authenticated user, **I want** a central dashboard after login **so that** I can reach any feature of the app in one tap.

**Actor:** All  
**Status:** ✅

**Acceptance criteria:**
1. After login (or session restore), the app navigates to the dashboard as the home screen.
2. The dashboard shows a card/tile for each major feature: Orders, Products, Customers, Inventory, Remittance, Green Crew (Employees), Farm Operations, Export Data.
3. Each card shows a label and an icon describing the feature.
4. Tapping a card navigates to that feature's screen.
5. The dashboard is always reachable from feature screens via the back stack; it cannot be exited without logging out.
6. The top bar exposes four actions: **Profile**, **About**, **Settings**, **Logout**.
7. **Profile** is accessible to all authenticated users and opens the profile screen (AUTH-US-05).
8. **Settings** is visible only to admin users. Non-admin users do not see the Settings option in the top bar.

---

### SYS-US-04 — Schema recreate and evolution record
**As** a developer rebuilding the app, **I want** a clean full-schema recreate and a checked-in **`schema_evolution.sql`** evolution record **so that** the database layout is documented and can be diffed against Room output without relying on obsolete version targets.

**Actor:** System (developer convention)  
**Status:** ✅ *(**`FarmDatabase.kt`** `@Database(version = 10)` is the source of truth; **`docs/schema_evolution.sql`** ends with a **VERSION 10** block — full `CREATE TABLE` / index DDL copied from KSP **`FarmDatabase_Impl.createAllTables`**; older VERSION 3–9 blocks are historical. Dev builds use **`fallbackToDestructiveMigration()`** for bumps without incremental **`Migration`**; only **`MIGRATION_1_2`** and **`MIGRATION_2_3`** remain in code — Stream D 2026-04-09.)*

**Policy (build phase):** Production-grade incremental migrations for every version bump are **not** committed yet. On schema bump, entities are updated, **`version`** is incremented, **`schema_evolution.sql`** gains or updates the matching **VERSION N** snapshot from generated `FarmDatabase_Impl`, and existing installs rely on **`fallbackToDestructiveMigration()`** (data loss on upgrade path) until a release migration strategy exists.

**Where to look for the live column list:** `app/src/main/java/com/redn/farm/data/local/entity/*.kt` and the latest **VERSION N** section in **`docs/schema_evolution.sql`**.

**`schema_evolution.sql` convention:**

- Path: `docs/schema_evolution.sql`
- Append **VERSION N** blocks at the bottom; do not rewrite prior version sections (historical record).
- The **current** Room DDL is verified against:  
  `app/build/generated/ksp/debug/java/com/redn/farm/data/local/FarmDatabase_Impl.java` → `createAllTables` (omit `room_master_table`).

**Acceptance criteria:**
1. **`FarmDatabase`** **`version`** matches the latest **VERSION N** header in **`schema_evolution.sql`** (currently **10**).
2. **`fallbackToDestructiveMigration()`** is in use for the current build phase; incremental **`Migration`** objects beyond **1→2** and **2→3** are optional until production.
3. **`docs/schema_evolution.sql`** includes a **VERSION 10** block whose `CREATE` statements match Room KSP output for that version (per `FarmDatabase_Impl`).
4. **`SYS-US-01`** seeding / default users still run cleanly on a fresh install at the current version.

---

### SYS-US-03 — About screen
**As** any authenticated user, **I want** to view information about the app **so that** I know the version I am running and who built it.

**Actor:** All  
**Status:** ✅

**Acceptance criteria:**
1. The About screen is accessible from the **About** action in the dashboard top bar.
2. It displays the following content:
   - App name: **RedN Farm App** (Yong & Eyo's FARM)
   - Current version number: **1.0.0**
   - Copyright notice
   - Team credits
   - Technology stack
   - MIT License text
3. The screen is read-only — no editable fields.
4. Standard Android back navigation returns to the dashboard.

---

## Epic 12 — End of Day Operations

End of Day (EOD) is a formal daily close that creates a permanent snapshot of the business day: total sales, cash position, closing inventory, spoilage, and outstanding receivables. It is a reporting and reconciliation action — it does not lock or delete any existing records. Open (unpaid) orders remain open and carry over to the next day.

**New entity: `day_close`**
One record per closed business date. Fields: `close_id`, `business_date` (epoch millis, midnight of the day), `closed_by` (username), `closed_at` (epoch millis), `total_orders`, `total_sales_amount`, `total_collected`, `total_outstanding`, `total_acquisition_cost`, `notes`, `is_finalized` (boolean — once finalized, the record is read-only).

**New entity: `day_close_inventory`**
One row per product per day close. Fields: `close_id`, `product_id`, `product_name`, `total_acquired_all_time`, `total_sold_through_close_date`, `prior_posted_variance` (sum of `variance_qty` from all prior closes for this product), `adjusted_theoretical_remaining` (computed: total_acquired − total_sold − prior_variance), `sold_this_close_date` (qty from today's orders only), `actual_remaining` (entered by user; null if not counted), `variance_qty` (adjusted_theoretical − actual_remaining; null if not counted), `weighted_avg_cost_per_unit`, `variance_cost` (variance_qty × weighted_avg_cost; null if not counted).

---

### EOD-US-01 — Initiate day close

**As** a store assistant or admin, **I want to** start the end-of-day closing process **so that** the day's operations are formally concluded with a summary record.

**Actor:** Store Assistant, Admin
**Status:** ✅ *(**DayCloseScreen** / **MainScreen** "Day Close"; draft vs finalized; **AC4** warnings; **AC3** future date blocked + non-admin past date blocked; **AC6** admin **Un-finalize** on **DayCloseScreen**; **AC5** explicit Review → Confirm flow shipped via **EOD-US-11**.)*

**Acceptance criteria:**
1. A **"Close Day"** action is accessible from the main dashboard, visible only to users with `STORE_ASSISTANT` or `ADMIN` role.
2. Tapping it opens the Day Close screen for the current business date. If a close already exists for today, the screen shows the existing record (read-only if finalized, editable if draft).
3. The business date displayed is the current calendar date — the user cannot close for a future date. Closing for a past date is allowed only for admins (covers late reconciliation scenarios).
4. Before presenting the summary, the app computes and displays a warning if any of the following are true:
   - There are orders created today with `is_paid = false`.
   - There are acquisitions recorded today with no matching orders (i.e., all acquired stock appears unsold).
5. The close process has two steps: **Review** (read-only computed summary) → **Confirm** (user finalizes). Finalizing sets `is_finalized = true` and records `closed_at` and `closed_by`.
6. A finalized day close cannot be edited. Only an admin can un-finalize a close (for correction purposes).

---

### EOD-US-02 — Review daily sales summary

**As** a store assistant, **I want to** see a summary of today's sales before confirming the close **so that** I can verify the numbers are correct.

**Actor:** Store Assistant, Admin
**Status:** ✅ *(daily paid/unpaid/delivered rows now shown via **EOD-US-12**, alongside by-channel and top-products sections on **DayCloseScreen**.)*

**Acceptance criteria:**
1. The sales summary section of the Day Close screen shows:
   - **Total orders** placed today (count).
   - **Total sales amount** = sum of `total_amount` across all orders with `order_date` within the business day.
   - **Paid orders** = count and amount where `is_paid = true`.
   - **Unpaid / outstanding orders** = count and amount where `is_paid = false`.
   - **Delivered orders** = count where `is_delivered = true`.
2. Sales are broken down **by channel**:
   - Online: order count and total amount.
   - Reseller: order count and total amount.
   - Offline: order count and total amount.
3. Sales are broken down **by product** — top products by revenue for the day, showing product name, total quantity sold, and total revenue.
4. All amounts are displayed in Philippine Peso format.
5. The summary is computed on the fly from existing order records — it is not a separate ledger entry. Numbers shown here are the same as what would be shown in Order History filtered to today.

---

### EOD-US-03 — Post closing inventory

**As** a purchasing assistant or admin, **I want to** reconcile total stock on hand against actual remaining quantities at end of day **so that** spoilage, loss, and unsold inventory are formally tracked across all acquisitions, not just today's.

**Actor:** Purchasing Assistant, Admin
**Status:** ✅ *(prior posted variance from finalized closes; **zero-theoretical** toggle; **not counted** switch + spoilage total; persist counts via **DayCloseInventoryDao.update**; finalize bulk-writes inventory; aging highlight (**AC6–7**); **AC2** last acquisition now includes date + qty + unit cost via **EOD-US-13**.)*

**Background:** Acquisitions do not happen every day. A batch of 80 kg of tomatoes bought on Monday may still be selling on Wednesday and Thursday. The closing inventory must account for the full stock position — all acquisitions ever recorded minus all sales ever recorded — and reconcile that against a physical count at the end of each business day. This gives an accurate picture of what is on hand, regardless of when the stock was purchased.

**Stock balance model:**

The app maintains a running balance per product using two aggregates computed directly from existing records:

- **Total acquired (all time)** = `SUM(acquisitions.quantity)` for that product, all dates.
- **Total sold (all time, through close date)** = `SUM(order_items.quantity)` for that product, orders up to and including the close date.
- **Theoretical on hand** = total acquired − total sold.
- **Cumulative variance (all prior closes)** = total spoilage posted across all previous `day_close_inventory` records for that product.
- **Adjusted theoretical remaining** = theoretical on hand − cumulative prior variance. This removes already-posted spoilage from the calculation so it is not double-counted.

**Acceptance criteria:**
1. The inventory section of the Day Close screen lists **every product that has a non-zero adjusted theoretical remaining** — regardless of whether an acquisition happened today. Products with zero theoretical remaining are hidden by default but can be shown via a toggle.
2. For each product row, the screen displays:
   - **Total acquired (all time):** cumulative quantity across all acquisitions.
   - **Total sold (all time through today):** cumulative quantity across all order items.
   - **Previously posted spoilage:** total variance posted in all prior day closes for this product.
   - **Adjusted theoretical remaining:** total acquired − total sold − prior spoilage. This is the expected quantity on hand right now.
   - **Sold today:** quantity sold in today's orders only — shown for reference to understand today's movement.
   - **Last acquisition:** date, quantity, and cost per unit of the most recent acquisition for this product.
3. The user enters the **actual remaining quantity** (physical count) for each product. The field defaults to the adjusted theoretical remaining.
4. **Variance this period** is computed automatically:
   - `variance = adjusted_theoretical_remaining − actual_remaining`
   - Positive variance = spoilage / loss (actual is less than expected).
   - Negative variance = surplus discrepancy — actual is more than expected. This flags a likely missing acquisition record and is highlighted with a warning.
5. **Variance cost** = `variance × weighted_average_cost_per_unit`, where weighted average cost = `SUM(acquisitions.total_amount) / SUM(acquisitions.quantity)` for that product. Shown per product row and as a section total.
6. A product with recent acquisitions but no sales yet is shown with `sold_today = 0` and `adjusted_theoretical_remaining = total_acquired − prior_spoilage`. Its full acquisition value remains at risk of spoilage and is highlighted if its most recent acquisition date is older than 3 days (configurable threshold).
7. The inventory section may be saved in **draft** state (with or without physical counts) and finalized later. Individual product rows marked "not counted" are excluded from the spoilage total.
8. When the day close is finalized, the entered `actual_remaining` values are written to `day_close_inventory` as the permanent record for this close date. These feed into the "cumulative prior variance" calculation for future closes.

---

### EOD-US-04 — Cash reconciliation

**As** a store assistant, **I want to** reconcile the cash I collected today against what the system shows as paid **so that** any cash discrepancy is identified and recorded.

**Actor:** Store Assistant, Admin
**Status:** ✅ *(cash discrepancy guard and remarks policy finalized; cash section labels/validation copy aligned; finalize guard scenarios covered in ViewModel tests; manual Day Close flow checks passed.)*

**Acceptance criteria:**
1. The cash reconciliation section shows:
   - **Expected cash from orders** = total amount of today's orders marked `is_paid = true` and channel = `offline` or `reseller` (online orders may be paid via transfer and excluded or separately categorized).
   - **Total remitted today** = sum of **`remittances.amount`** for the business day where **`entry_type = REMITTANCE`** (store assistant handover; **Epic 8**). Rows with **`DISBURSEMENT`** are **not** included here. Optionally show **Disbursements received today** as a separate informational line.
   - **Difference** = expected cash − total remitted (remittance type only). Shown in a distinct color: green if zero or positive (surplus), red if negative (shortage).
2. The user may enter a **cash-on-hand** figure (actual counted cash) as a free-entry field. If entered, the screen shows: cash-on-hand vs. expected cash, and the shortage/surplus.
3. Any discrepancy (non-zero difference) must have a **remarks** field filled before the close can be finalized.
4. Online payment collections (GCash, bank transfer) are shown separately from cash — labeled as "Digital collections" with a count and total amount, but not included in the cash reconciliation math.

---

### EOD-US-05 — View and print end-of-day summary report

**As** an admin, **I want to** print a thermal end-of-day summary slip **so that** I have a physical record of the day's performance for filing.

**Actor:** Admin, Store Assistant
**Status:** ✅ *(**Print draft** / **Print** on **DayCloseScreen** via **`buildEodSummary`** + **`PrinterUtils.printMessage`**; slip covers sales, channels, top products, inventory, cash, COGS/margin, outstanding orders (cap 10), wages total; footer metadata completed via **EOD-US-15**.)*

**Acceptance criteria:**
1. A **Print EOD Summary** button is available on the Day Close screen (both draft and finalized states).
2. The printed slip (58mm thermal) includes:
   - Header: business name, "End of Day Report", date.
   - **Sales summary:** total orders, total sales, paid amount, outstanding amount.
   - **Channel breakdown:** online / reseller / offline — order count and amount each.
   - **Top 5 products by revenue:** product name and total revenue.
   - **Inventory close:** product name, adjusted theoretical remaining, actual remaining (if counted), variance qty — one line per product. Total variance cost at the bottom.
   - **Cash reconciliation:** expected cash, cash-on-hand (if entered), difference.
   - **Total variance cost (spoilage).**
   - **COGS today** = cost of goods sold based on weighted average acquisition cost.
   - **Gross margin** = collected revenue − COGS today, with margin %.
   - Footer: closed by (username), closed at (time).
3. If the close is not yet finalized, the slip header prints "DRAFT — NOT FINAL".
4. Print is triggered via the Sunmi built-in printer using `PrinterUtils.printMessage`.

---

### EOD-US-06 — View day close history

**As** an admin, **I want to** view the history of past day closes **so that** I can compare daily performance and access any day's EOD record.

**Actor:** Admin
**Status:** ✅ *(list + **All / 30 / 90 day** chips; row opens same **DayCloseScreen** (read-only when finalized); **re-print** and **un-finalize** from that screen; history rows include margin, closed by, and closed at via **EOD-US-14**.)*

**Acceptance criteria:**
1. A **Day Close History** screen is accessible from the main dashboard (admin only).
2. The list shows one row per closed date: business date, total sales, total orders, gross margin, closed by, closed at.
3. Tapping a row opens the full day close detail — all sections (sales summary, inventory, cash reconciliation) in read-only mode.
4. A finalized close record can be re-printed from the detail view.
5. The list is sorted by date descending. It supports date range filtering.
6. An admin can **un-finalize** a close from the detail view — this re-opens it for editing. Un-finalization is logged with the admin's username and timestamp.

---

### EOD-US-07 — Cost of goods sold vs. revenue

**As** an admin, **I want to** see today's revenue against the actual cost of the goods sold today **so that** I have a meaningful daily margin that reflects the real cost of what was sold, not just what was bought on that day.

**Actor:** Admin
**Status:** ✅ *(**Cost & margin** + **cumulative position** + **other outflows today** on **DayCloseScreen**; colored margin and net recovered; negative-margin confirmation copy aligned with **EOD-US-07 AC4**.)*

**Background:** Comparing today's revenue to today's acquisition spend is misleading — on most days there is no acquisition, making the "procurement cost" appear as zero. The correct measure is **cost of goods sold (COGS)**: the acquisition cost attributable to the specific quantities sold today. This is derived from the acquisition pool using a weighted average cost per product, applied to today's sold quantities.

**Acceptance criteria:**
1. The Day Close screen includes a **Revenue vs. COGS** section with the following figures:

   **Today's revenue:**
   - **Gross revenue today** = `SUM(order_items.total_price)` for all order items in orders with `order_date` = close date (all orders regardless of paid status).
   - **Collected revenue today** = same, but restricted to orders where `is_paid = true`.
   - **Outstanding revenue today** = gross − collected (what is still owed from today's orders).

   **Cost of goods sold (COGS) today:**
   - For each product sold today, COGS = `qty_sold_today × weighted_avg_cost_per_unit`.
   - **Weighted average cost per unit** = `SUM(acquisitions.total_amount) / SUM(acquisitions.quantity)` for that product, all acquisitions ever. This represents the average cost per kg (or per piece) across the entire acquisition history.
   - **Total COGS today** = sum of COGS across all products sold today.

   **Margin:**
   - **Gross margin (amount)** = collected revenue today − total COGS today.
   - **Gross margin (%)** = (gross margin ÷ collected revenue today) × 100, to 1 decimal place. Shown as `—` if collected revenue is zero.

2. A secondary **Cumulative position** subsection shows the broader picture:
   - **Total acquisition investment (all time)** = `SUM(acquisitions.total_amount)` for all products ever acquired.
   - **Total revenue collected (all time)** = `SUM(orders.total_amount)` for all paid orders ever.
   - **Outstanding inventory value** = adjusted theoretical remaining stock (from EOD-US-03) × weighted average cost per unit, summed across all products. This is how much capital is still tied up in unsold stock.
   - **Net recovered** = total revenue collected − total acquisition investment. Positive means the business has recovered more than it spent on stock overall; negative means cumulative acquisition investment still exceeds total collections.

3. A visual indicator (color) distinguishes positive from negative margin on both today's and the cumulative figures.
4. This section is informational and never blocks finalization. If today's gross margin is negative, a confirmation dialog appears: "Today's COGS exceeds collected revenue. Confirm close?" — allowing the user to proceed.
5. **Other outflows today** are listed separately below the margin line and are not subtracted from it:
   - Employee wages paid today = `SUM(employee_payments.amount)` for payments with `date_paid` = close date.
   - **Sales remittances today** (`entry_type = REMITTANCE`) = `SUM(remittances.amount)` for `date` = close date.
   - **Disbursements received today** (`entry_type = DISBURSEMENT`) = same filter, separate line (**Epic 8**).
   These are shown for completeness; net margin after labor and overhead is a server-side reporting concept (EOD reporting on device is gross margin only).

---

### EOD-US-10 — Outstanding inventory report

**As** an admin or purchasing assistant, **I want to** view all stock currently on hand across all acquisitions **so that** I know exactly what I have, what it cost, how long it has been sitting, and what is at risk of spoilage.

**Actor:** Admin, Purchasing Assistant
**Status:** ✅ *(search, category chips, **at-risk** toggle, total value, **print** via **`buildOutstandingInventoryReport`**; FIFO lots + aging colors; **AC11** uses **`actual_remaining`** when today's close is finalized; AC3 per-product ledger fields now shown on card with explicit units.)*

**Background:** "Outstanding inventory" is distinct from the EOD-US-03 inventory close. EOD-US-03 is a reconciliation done at close time that posts spoilage. The outstanding inventory report is a **live, always-available view** of the current theoretical stock position derived from all acquisition and order records — accessible at any time during the day, not just at close. It is the answer to "what do I still have on hand right now?"

**Acceptance criteria:**

**Access:**
1. The Outstanding Inventory screen is accessible from the main dashboard as a standalone navigation item, available to `ADMIN` and `PURCHASING` roles at any time — not only during day close.
2. It is also shown as a section within the Day Close screen (EOD-US-01) when the user reaches the inventory step.

**Per-product summary view (default):**
3. The screen lists every product with a non-zero theoretical remaining quantity. For each product:
   - **Product name.**
   - **Total acquired (all time):** cumulative quantity across all acquisition records.
   - **Total sold (all time):** cumulative quantity across all order items.
   - **Previously posted spoilage:** total variance from all finalized day closes.
   - **Theoretical on hand:** total acquired − total sold − posted spoilage.
   - **Weighted average cost/unit:** `SUM(total_amount) / SUM(quantity)` across all acquisitions for that product.
   - **Outstanding inventory value:** theoretical on hand × weighted avg cost/unit.
   - **Oldest unsold stock date:** date of the earliest acquisition that still has unrecovered quantity (based on FIFO — oldest acquisition's remaining quantity depleted first by sales).
   - **Days on hand (oldest lot):** calendar days since the oldest unsold acquisition date.
4. Products are sorted by **days on hand descending** by default (oldest unsold stock first), so aging inventory is immediately visible at the top.
5. A **total outstanding inventory value** is shown at the top of the screen: sum of outstanding value across all products.

**Per-acquisition drill-down:**
6. Tapping a product row expands it to show the individual acquisition lots contributing to the current stock position. For each acquisition lot:
   - Acquisition date, acquisition ID, quantity acquired, quantity sold (attributed to this lot, FIFO), quantity remaining in this lot, cost per unit, lot remaining value.
   - Age in days since acquisition date.
7. Lots with **age ≥ 3 days** (default; configurable in settings by admin) are flagged with a visual indicator (e.g., amber color) to indicate aging/spoilage risk. Lots ≥ 7 days are flagged as critical (red).

**Filtering and search:**
8. The list can be filtered by product category and searched by product name.
9. A toggle shows **"At-risk only"** — products where the oldest lot is beyond the aging threshold.

**Print:**
10. A **Print Outstanding Inventory** button prints a thermal slip with:
    - Header: "Outstanding Inventory — as of [date and time]"
    - One line per product: product name, theoretical on hand (qty + unit), days on hand (oldest lot), outstanding value.
    - Total outstanding value.
    - Footer: printed by (username).

**Relationship to EOD-US-03:**
11. The quantities shown in the Outstanding Inventory screen are the same theoretical figures used as the starting point for the EOD-US-03 physical count. If a day close has been finalized for today, the outstanding inventory screen reflects the `actual_remaining` values from that close instead of the theoretical remaining (i.e., finalized close values override the theoretical until the next day's sales update the running total).

---

### EOD-US-08 — Outstanding orders report at close

**As** an admin or store assistant, **I want to** see all unpaid orders as part of the day close **so that** I know exactly what receivables are open going into the next day.

**Actor:** Admin, Store Assistant
**Status:** ✅ *(section on **DayCloseScreen** with total, capped list, tap → **OrderDetail**; thermal section matches **EOD-US-08 AC6**.)*

**Acceptance criteria:**
1. The Day Close screen includes an **Outstanding Orders** section listing all orders where `is_paid = false` as of close time — not just today's orders but all historical unpaid orders.
2. Each row shows: order ID, customer name, channel, order date (age in days), and amount.
3. Orders are sorted by order date ascending (oldest outstanding first).
4. The section shows a total outstanding amount at the top.
5. Tapping an order row navigates to the Order Detail screen — allowing the user to mark it paid directly from within the day close flow.
6. The outstanding orders list is printed as a section on the thermal EOD slip (up to 10 rows; if more, shows count and total only to fit on 58mm paper).

---

### EOD-US-09 — Employee day summary at close

**As** an admin, **I want to** see a summary of which employees worked today and what wages are due **so that** payroll is accounted for each day even if payment is made later.

**Actor:** Admin
**Status:** ✅ *(payments today show employee, gross, cash advance, net (**BUG-EMP-01** formula); notes UX explicitly supports wages-due/non-blocking close; thermal line wording aligned to "Employee wages paid today".)*

**Acceptance criteria:**
1. The Day Close screen includes an **Employee Day Summary** section.
2. The section lists employees who have a payment record with `date_paid` equal to today's business date, showing: employee name, gross wage (`amount`), cash advance (`cash_advance_amount`), and net pay.
3. If no payments were recorded today, the section shows "No employee payments recorded today" — this is not a blocker for close.
4. **Wages due but not yet paid** can be noted in a free-text `notes` field on the day close record — the app does not enforce payment before close.
5. Total wages paid today = sum of `amount` from today's `employee_payments`. Shown as a summary line.
6. Wages due today appear on the printed EOD slip as a single total line: "Employee wages paid today: PHP X,XXX.00".

---

### EOD-US-11 — Day close review confirmation step

**As** a store assistant or admin, **I want** an explicit review step before final confirmation **so that** I can verify totals and warnings before locking the close.

**Actor:** Store Assistant, Admin  
**Status:** ✅ *(Day Close now has explicit **Review summary** state with separate **Confirm finalize** action; finalize requests are accepted only from review state.)*

**Acceptance criteria:**
1. The close flow has a visible **Review** state and a separate **Confirm finalize** action.
2. In review state, computed summaries are visible and editable fields remain available (if role permits).
3. Finalization is only executed from the explicit confirmation action.
4. The state transition is clear in UI copy and button labels.

---

### EOD-US-12 — Complete daily sales breakdown rows

**As** an admin, **I want** a full daily sales breakdown (paid, unpaid, delivered) in Day Close **so that** the close summary matches order operations at a glance.

**Actor:** Admin  
**Status:** ✅ *(implemented on **DayCloseScreen** via DAO aggregate in **OrderDao** + snapshot wiring in **DayCloseRepository**; includes paid/unpaid counts+amounts and delivered count for the business day.)*

**Acceptance criteria:**
1. Sales summary shows **paid orders count + amount** for the business day.
2. Sales summary shows **unpaid orders count + amount** for the business day.
3. Sales summary shows **delivered orders count** for the business day.
4. Values match Order History for the same date window.

---

### EOD-US-13 — Complete last acquisition snippet in inventory close

**As** a purchasing assistant or admin, **I want** each inventory row to show full last-acquisition details **so that** I can reconcile aging and cost context without leaving Day Close.

**Actor:** Purchasing Assistant, Admin  
**Status:** ✅ *(last acquisition date + quantity + unit cost snippet now shown on Day Close inventory rows via latest-acquisition snapshot in **DayCloseRepository** and row rendering in **DayCloseScreen**.)*

**Acceptance criteria:**
1. Each inventory row shows last acquisition **date**, **quantity**, and **cost per unit**.
2. For products with no acquisition history, row shows a clear placeholder (e.g., "No acquisition data").
3. Values are sourced from the most recent acquisition for that product by business date/time.

---

### EOD-US-14 — History list completeness for closed records

**As** an admin, **I want** richer metadata in Day Close History rows **so that** I can compare closed days without opening each record.

**Actor:** Admin  
**Status:** ✅ *(History rows now show total sales, total orders, gross margin, closed by, and closed at; list uses finalized closes and keeps All/30/90-day filters.)*

**Acceptance criteria:**
1. Each history row shows business date, total sales, total orders, gross margin, closed by, and closed at.
2. Sorting remains descending by business date.
3. Existing range filters continue to work with the enriched row data.

---

### EOD-US-15 — EOD slip footer close metadata

**As** an admin, **I want** finalized EOD slips to include close metadata **so that** printed records can be audited without opening the app.

**Actor:** Admin  
**Status:** ✅ *(`buildEodSummary` footer now prints **Printed by / Printed at** for drafts and **Closed by / Closed at** for finalized slips; wired from **DayCloseViewModel** + `DayCloseEntity.closed_by/closed_at`.)*

**Acceptance criteria:**
1. Finalized EOD slip footer includes **closed by** and **closed at**.
2. Draft slip footer includes **printed by** and **printed at** and keeps the draft banner.
3. Footer formatting remains legible within the 58mm thermal line width.

---

## Non-Functional Stories

### NFR-US-01 — Works on Android 7.0+
**As** a user with an older device, **I want** the app to run on my phone **so that** I don't need to upgrade hardware.

**Status:** ✅ (minSdk = 25)

---

### NFR-US-02 — Responsive layout on various screen sizes
**As** a user, **I want** the app to look correct on both phones and tablets **so that** the team can use whatever device is available.

**Status:** ✅ (adaptive grid layouts used on list screens)

---

### NFR-US-03 — Data persists across app restarts
**As** a user, **I want** all entered data to be saved permanently **so that** I never lose records due to closing the app.

**Status:** ✅ (Room/SQLite local database)

---

### NFR-US-05 — SRP computation is near-instant
**As** a purchasing assistant, **I want** SRP values to appear as I type without noticeable lag **so that** the acquisition form feels responsive.

**Status:** ✅ *(acquisition form SRP preview now debounces at 200ms and executes preview computation off the UI thread path.)*

**Acceptance criteria:**
1. SRP computation triggered by changes to quantity, price per unit, or piece count completes and updates the UI within **200ms** of the last keystroke.
2. Computation runs on a background thread — it must not block or jank the UI thread.
3. Rapid successive keystrokes are debounced; only the final value after typing stops triggers a full recompute.

---

### NFR-US-04 — Passwords are stored securely
**As** an admin, **I want** user passwords to be hashed, never stored as plaintext **so that** credentials are safe if the device is compromised.

**Status:** ✅ (`password_hash` field; plaintext never stored)

---

## Out of Scope (v1.0)

- Multi-device data synchronization (exports provide a manual bridge)
- Cloud backup / remote database
- Push notifications
- Full **RBAC** **administration** UI (role matrix, bulk assignment) — out of scope; **per-screen gates** use **`Rbac`** (**DESIGN.md** §5), including **Epic 12** day close (**DESIGN.md** §14.10)
- Report generation / dashboards / charts *(EOD daily reports are in scope as of Epic 12; multi-period dashboards and trend charts remain out of scope)*
- Print support from within the app *(EOD thermal slip printing is in scope as of Epic 12; general in-app print is out of scope)*
- Barcode / QR scanning for products
