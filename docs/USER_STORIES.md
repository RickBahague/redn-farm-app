# User Stories — RedN Farm App

## Overview

This document is the **source of truth** for all user stories in the RedN Farm App (Yong & Eyo's FARM). Stories reflect what is currently implemented and serve as the baseline for future development.

**App version:** 1.0.0  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Last updated:** 2026-04-02

---

## Actors

| Actor | Description | Primary Workflows |
|-------|-------------|-------------------|
| **Admin / Owner** | Full access. Configures products, prices, and employees. | Products, Pricing, Employees, Export |
| **Store Assistant** | Takes orders and manages customer relationships. | Orders, Customers, Remittance |
| **Farmer** | Records what happens in the field. | Farm Operations |
| **Purchasing Assistant** | Records incoming produce and procurement costs. | Inventory & Acquisitions |

> **Note:** The current app seeds two accounts (`admin` / `user`) but has no role-based access control in the UI — all authenticated users can access all features. Actor tags indicate *intended* ownership per feature for future RBAC.

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented and working |
| 🔧 | Partially implemented |
| 📋 | Planned / not yet built |

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
**Status:** 📋

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
**Status:** 📋

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
**Status:** ✅ (partial — channel-based SRP pre-fill is 📋)

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
**Status:** ✅ (partial — automatic SRP selection is 📋)

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
**Status:** ✅ (partial — channel SRP re-application on channel change is 📋)

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
**Status:** 📋

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
**Status:** 📋

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
**Status:** ✅ (partial — SRP display is 📋)

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
2. I can optionally assign a **product category** (from the list defined in MGT-US-03) so that the correct spoilage rate and hauling cost are applied when computing SRPs for acquisitions of this product.
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
**Status:** ✅ (partial — acquisition-derived SRP history is 📋)

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
**Status:** 📋

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
**Status:** ✅

**Acceptance criteria:**
1. I can select a product, enter quantity (decimal for kg), price per unit, acquisition date, and source location (Farm / Market / Supplier / Other).
2. The total amount is calculated automatically.
3. I can toggle whether the unit is per kg or per piece.
4. I can enter a **piece count** (number of pieces per kg) to enable per-piece SRP computation in INV-US-05. If the selected product has a default piece count (PRD-US-02), it is pre-filled; I can override it for this acquisition.

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
**Status:** 📋

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
**Status:** 📋

**Pricing formula** *(canonical reference: PricingReference.md §4.3, §5.2 FR-PC-10–14, US-6)*

Given:
- **B** = total acquisition cost (`quantity × price_per_unit`)
- **Q** = quantity in kg
- **s** = spoilage rate (default **25%**; management-configurable per product category)
- **A** = required additional cost per kg = (driver fee + fuel + toll + handling) / hauling weight
  - Defaults: (2,000 + 4,000 + 1,000 + 200) / 700 kg = **≈ 10.29 PHP/kg**
- Channel markups (management-configurable): **online 35%**, **offline 30%**, **reseller 25%**

Pipeline (executed for each channel):
1. **Sellable quantity:** `Q_sell = Q × (1 − s)`
2. **Cost per sellable kg:** `C = B / Q_sell`
3. **Price after markup:** `priceAfterCore = C × (1 + channel_markup)`
4. **Add hauling cost:** `priceBeforeFees = priceAfterCore + A`
5. **Round up to nearest whole PHP:** `SRP = ⌈priceBeforeFees⌉`
6. **Fractional packages** (per channel): `SRP_500g = ⌈SRP × 0.5⌉`, `SRP_250g = ⌈SRP × 0.25⌉`, `SRP_100g = ⌈SRP × 0.1⌉`
7. **Per piece** (when piece count is provided): `SRP_piece = ⌈SRP / piece_count⌉`

**Acceptance criteria:**
1. As I fill in quantity and price per unit on the acquisition form, suggested SRPs for **online**, **reseller**, and **offline** channels are computed and displayed in real time.
2. The SRP computation uses the **currently active preset** (MGT-US-06) — the one explicitly tagged as active by management. If no preset has been activated yet, SRP computation is unavailable and the form shows a notice prompting management to activate a preset.
3. The purchasing assistant sees the active preset's resolved values (spoilage rate, hauling cost, channel markups) as read-only context on the form. They cannot edit preset values.
4. Fractional package SRPs (500g, 250g, 100g) are shown for each channel.
5. If a piece count is entered for the product, per-piece SRP is also shown for each channel.
6. All SRPs are rounded **up** to the nearest whole PHP (e.g. 153.40 → 154, 165.50 → 166).
7. SRPs are saved on the acquisition record alongside the cost data.
8. If `Q_sell = 0` (100% spoilage), validation blocks save with a clear error message.
9. The full preset snapshot (spoilage rate, hauling fees, channel markups) and a `presetRef` pointing to the active preset record are stored on the acquisition at save time. This snapshot is immutable after save — it is never updated, even if the preset is later deactivated or modified.

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
| `amount` | Gross wage for the period — the base pay owed to the employee before advance accounting |
| `cash_advance_amount` | Additional cash given as an advance against future pay in this same transaction (optional, nullable) |
| `liquidated_amount` | Amount deducted this payment to settle a cash advance given in a **prior** transaction (optional, nullable). Reduces the employee's outstanding advance balance. |
| `date_paid` | Date this payment was made by the admin |
| `received_date` | Date the employee physically received the cash (may differ from `date_paid`) |

**Net pay formula:** `net_pay = amount − cash_advance + liquidated_amount`

> Example: Gross wage ₱5,000 − new advance ₱1,000 + prior advance recovered ₱500 = net pay ₱4,500.

**Acceptance criteria:**
1. I can enter gross wage (`amount`), payment date, and received date. These are required.
2. I can optionally enter a `cash_advance_amount` if I am giving the employee an advance in this same transaction.
3. I can optionally enter a `liquidated_amount` to recover all or part of a previously given cash advance. The liquidated amount is subtracted from the employee's outstanding advance balance (tracked across all their payment records).
4. The form displays the computed **net pay** in real time as I fill in the fields, using the formula above.
5. I can capture the employee's **signature** as acknowledgment of receipt. The signature input supports two modes: finger-drawn on screen, or typed name. The employee can use either.
6. The payment record is linked to the specific employee.

---

### EMP-US-06 — View employee payment history
**As** an admin, **I want to** view all payments for a specific employee with a running balance of outstanding advances **so that** I always know how much advance the employee still owes back.

**Actor:** Admin  
**Status:** ✅

**Acceptance criteria:**
1. Payment history lists each record with: payment ID, gross wage (`amount`), cash advance given, liquidated amount, net pay, payment date, and received date.
2. I can filter by time period: this month, last month, last 3 months, last 6 months, or a custom date range.
3. A summary card for the selected period shows:
   - **Total gross wages** paid (`sum of amount`)
   - **Total cash advances** given (`sum of cash_advance_amount`)
   - **Total liquidated** (`sum of liquidated_amount`)
   - **Outstanding advance balance** = cumulative total advances given across **all time** minus cumulative total liquidated across **all time** (not scoped to the current filter period — this is a running lifetime balance)
4. Outstanding advance balance is always shown regardless of the active filter so the admin always sees the current exposure.

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

## Epic 8 — Remittance

### REM-US-01 — Record a remittance
**As** a store assistant, **I want to** record a remittance transaction **so that** there is a log of money sent or received.

**Actor:** Store Assistant  
**Status:** ✅

**Acceptance criteria:**
1. I can enter an amount, date, and optional remarks.
2. The amount input supports currency formatting.

---

### REM-US-02 — View remittance history
**As** a store assistant or admin, **I want to** view all remittances **so that** I can track financial transactions.

**Actor:** Store Assistant, Admin  
**Status:** ✅

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
1. All remittance fields can be updated.
2. A confirmation dialog is shown before deletion.

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
`PaymentId, EmployeeId, EmployeeName, GrossWage, CashAdvanceAmount, LiquidatedAmount, NetPay, DatePaid, ReceivedDate, Signature, DeviceId`

> `NetPay` = `GrossWage − CashAdvanceAmount + LiquidatedAmount`

**farm_operations.csv**
`OperationId, OperationType, OperationDate, Details, Area, WeatherCondition, Personnel, ProductId, ProductName, DateCreated, DateUpdated, DeviceId`

**acquisitions.csv** *(includes computed SRPs and preset traceability — columns marked 📋 are added when INV-US-05 is implemented)*
`AcquisitionId, ProductId, ProductName, ProductCategory, Quantity, PricePerUnit, TotalAmount, IsPerKg, PieceCount, DateAcquired, Location,`
`PresetRef, SpoilageRate, AdditionalCostPerKg, HaulingWeightKg, HaulingFees,`
`OnlineMarkup, ResellerMarkup, OfflineMarkup,`
`SrpOnlinePerKg 📋, SrpResellerPerKg 📋, SrpOfflinePerKg 📋,`
`SrpOnline500g 📋, SrpOnline250g 📋, SrpOnline100g 📋,`
`SrpReseller500g 📋, SrpReseller250g 📋, SrpReseller100g 📋,`
`SrpOffline500g 📋, SrpOffline250g 📋, SrpOffline100g 📋,`
`SrpOnlinePerPiece 📋, SrpResellerPerPiece 📋, SrpOfflinePerPiece 📋,`
`DeviceId`

**remittances.csv**
`RemittanceId, Amount, Date, Remarks, DateUpdated, DeviceId`

**Acceptance criteria:**
1. All 10 tables listed above can be exported individually.
2. Exported files are saved to the app's external files directory (`exports/`) and can be shared/downloaded from the device.
3. SRP and preset columns on `acquisitions.csv` are included once INV-US-05 is implemented; prior to that they are omitted from the export.

---

### EXP-US-02 — Clear specific data tables
**As** an admin, **I want to** truncate any data table **so that** I can reset test data before going live.

**Actor:** Admin  
**Status:** 🔧 (currently only Customers, Products, Acquisitions — expanding to all is 📋)

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
**Status:** 📋

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
**Status:** 📋

**Canonical reference:** PricingReference.md US-6, §7.3, FR-PC-50–51

**Acceptance criteria:**
1. I can set a **default spoilage rate** (fraction 0–0.99, e.g. 0.25 for 25%) that applies to all products unless overridden by category.
2. I can configure the **hauling model** with individual named fee line items (e.g. driver fee, fuel, toll, handling) and a hauling weight in kg. The additional cost per kg `A` is derived automatically as `sum(fee amounts) / hauling weight`.
3. I can alternatively enter `A` as a direct override value instead of using the hauling model.
4. Before saving, the admin can enter an optional **preset name** (e.g. "Q2 2026 Rates"). If left blank, the system auto-generates a name from the save timestamp (e.g. "Preset 2026-04-02 14:30"). The preset is also assigned a unique system ID on save. Both the name and the ID are stored and displayed in the history list (MGT-US-05).
5. Saving a preset configuration creates a new **inactive** preset record in history. It does not affect SRP computations until explicitly activated (MGT-US-06).
6. Saved acquisition SRP records are **never** mutated when a new preset is saved or activated — they permanently retain the snapshot of the preset that was active at their save time.

---

### MGT-US-02 — Configure per-channel markup and rounding
**As** an admin, **I want to** set the markup percentage and rounding rule for each sales channel **so that** online, reseller, and offline SRPs reflect the different margin requirements for each channel.

**Actor:** Admin  
**Status:** 📋

**Canonical reference:** PricingReference.md US-6, §7.2, §11.1.2, FR-PC-05, FR-PC-14

**Channels:** online, reseller, offline

**Acceptance criteria:**
1. For each channel I can set exactly one of: **markup %** (e.g. 35%) or **margin %** — not both. The UI enforces this constraint and shows a validation error if both are set.
   - Markup formula: `priceAfterCore = C × (1 + markup%)`
   - Margin formula: `priceAfterCore = C / (1 − margin%)`
2. For each channel I can set a **rounding rule**. Default is `ceil_whole_peso` (round up to the nearest whole PHP). Other supported rules: `nearest_whole_peso`, `nearest_0.25`.
3. For each channel I can optionally configure **channel-attributable fees** (fixed PHP amount or %) applied after `priceAfterCore + A` and before rounding — e.g. delivery surcharge, payment processing fee.
4. Default values on first setup: online 35% markup, reseller 25% markup, offline 30% markup; all channels use `ceil_whole_peso` rounding.
5. Saving channel configuration creates a new inactive preset record (same as MGT-US-01 AC#4) — it does not take effect until activated (MGT-US-06).
6. The purchasing assistant sees the **active preset's** resolved channel values as read-only context on the acquisition form — they cannot edit them.

---

### MGT-US-03 — Manage product categories with per-category overrides
**As** an admin, **I want to** define product categories and assign category-specific spoilage rates and additional costs **so that** different product types use appropriate preset values when computing SRPs.

**Actor:** Admin  
**Status:** 📋

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
**Status:** 📋

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
**Status:** 📋

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
**Status:** 📋

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
**As** a developer rebuilding the app, **I want** a clean full-schema recreate that includes all new fields from planned stories, with a `schema_evolution.sql` file as the living record **so that** the database is built right from the start without incremental migration baggage.

**Actor:** System (developer convention)  
**Status:** 📋

**Decision:** For this build phase, **no Room migrations are used**. The database is created fresh with all tables and fields in their final planned shape. The `fallbackToDestructiveMigration()` strategy is used during development. When the app ships to production a proper migration strategy will be defined.

**New fields added to existing tables (vs v3 schema):**

| Table | New field | Type | Notes |
|-------|-----------|------|-------|
| `products` | `category` | String? | Links to pricing preset category (MGT-US-03) |
| `products` | `default_piece_count` | Int? | Used to pre-fill piece count on acquisitions (PRD-US-02) |
| `orders` | `channel` | String | `"online"` / `"reseller"` / `"offline"` (ORD-US-01) |
| `acquisitions` | `piece_count` | Int? | Pieces per kg for per-piece SRP (INV-US-01) |
| `acquisitions` | `preset_ref` | String? | FK to `pricing_presets.preset_id` (INV-US-05) |
| `acquisitions` | `spoilage_rate` | Double? | Snapshot value at save time |
| `acquisitions` | `additional_cost_per_kg` | Double? | Snapshot value at save time |
| `acquisitions` | `hauling_weight_kg` | Double? | Snapshot value at save time |
| `acquisitions` | `hauling_fees_json` | String? | Snapshot: JSON array of `{label, amount}` |
| `acquisitions` | `online_markup` | Double? | Snapshot value at save time |
| `acquisitions` | `reseller_markup` | Double? | Snapshot value at save time |
| `acquisitions` | `offline_markup` | Double? | Snapshot value at save time |
| `acquisitions` | `srp_online_per_kg` | Double? | Computed (INV-US-05) |
| `acquisitions` | `srp_reseller_per_kg` | Double? | Computed |
| `acquisitions` | `srp_offline_per_kg` | Double? | Computed |
| `acquisitions` | `srp_online_500g` | Double? | Computed |
| `acquisitions` | `srp_online_250g` | Double? | Computed |
| `acquisitions` | `srp_online_100g` | Double? | Computed |
| `acquisitions` | `srp_reseller_500g` | Double? | Computed |
| `acquisitions` | `srp_reseller_250g` | Double? | Computed |
| `acquisitions` | `srp_reseller_100g` | Double? | Computed |
| `acquisitions` | `srp_offline_500g` | Double? | Computed |
| `acquisitions` | `srp_offline_250g` | Double? | Computed |
| `acquisitions` | `srp_offline_100g` | Double? | Computed |
| `acquisitions` | `srp_online_per_piece` | Double? | Computed; null if no piece_count |
| `acquisitions` | `srp_reseller_per_piece` | Double? | Computed; null if no piece_count |
| `acquisitions` | `srp_offline_per_piece` | Double? | Computed; null if no piece_count |

**New tables:**

| Table | Purpose | Key fields |
|-------|---------|------------|
| `pricing_presets` | Stores each saved preset configuration | `preset_id`, `preset_name`, `saved_at`, `saved_by`, `is_active`, `spoilage_rate`, `additional_cost_per_kg`, `hauling_weight_kg`, `hauling_fees_json`, `online_markup`, `reseller_markup`, `offline_markup`, `rounding_rule`, `categories_json` |
| `preset_activation_log` | Append-only log of every activation event | `log_id`, `activated_at`, `activated_by`, `preset_id_activated`, `preset_id_deactivated` |

**`schema_evolution.sql` convention:**

- File lives at `docs/schema_evolution.sql`
- Each database version is delimited by a version header comment
- Contains the full `CREATE TABLE` statements for that version — not diffs
- New versions are appended at the bottom; old versions are never edited
- Format:

```sql
-- ============================================================
-- VERSION 3  (baseline — existing app before rebuild)
-- ============================================================
CREATE TABLE IF NOT EXISTS products ( ... );
...

-- ============================================================
-- VERSION 4  (rebuild — all new fields included)
-- ============================================================
CREATE TABLE IF NOT EXISTS products ( ... );  -- includes category, default_piece_count
CREATE TABLE IF NOT EXISTS pricing_presets ( ... );
CREATE TABLE IF NOT EXISTS preset_activation_log ( ... );
...
```

**Acceptance criteria:**
1. `FarmDatabase` is set to version 4 with `fallbackToDestructiveMigration()`. No `addMigrations()` calls remain.
2. All new fields listed above are present in their respective Room entities and annotated correctly.
3. `docs/schema_evolution.sql` exists in the repo, contains the v3 baseline CREATE statements followed by the v4 full CREATE statements.
4. The v4 `CREATE TABLE` statements in `schema_evolution.sql` match exactly what Room generates (verified by comparing against Room's generated schema JSON).
5. `SYS-US-01` seed data runs cleanly against the v4 schema on a fresh install.

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

**Status:** 📋

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
- Role-based access control (RBAC) in the UI
- Report generation / dashboards / charts
- Print support from within the app
- Barcode / QR scanning for products
