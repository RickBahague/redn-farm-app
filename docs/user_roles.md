# User Roles & Permissions — RedN Farm App

**Created:** 2026-04-02

---

## Overview

User accounts are stored in the `users` table with a `role` TEXT column. The role string is stored uppercase and controls which features are visible and writable for that user.

**Current implementation state:** The code enforces two levels — `ADMIN` vs. everything else. The screens listed in the permission matrix below are *all accessible* to any authenticated user at present. This document defines the intended target state for full RBAC, where the app will gate navigation items and write actions by role.

---

## Roles

| Role value (DB) | Display name | Intended actor |
|---|---|---|
| `ADMIN` | Administrator | Owner / Farm manager |
| `STORE_ASSISTANT` | Store Assistant | Front-of-store staff who take orders |
| `PURCHASING` | Purchasing Assistant | Staff who handle produce procurement |
| `FARMER` | Farmer | Field staff who log farm operations |
| `USER` | User | General read-only account |

> **Seeded accounts:** On first install, `admin` is created with role `ADMIN` and `user` with role `USER`.  
> **Normalization (current code):** `UserManagementViewModel.createUser()` normalizes any unrecognized role string to `USER`. This must be updated to accept all five values.

---

## Permission Matrix

`✅` = full read + write access  
`👁` = read-only  
`❌` = hidden / blocked  
`🔑` = self only (e.g. own profile)

| Feature area | ADMIN | STORE_ASSISTANT | PURCHASING | FARMER | USER |
|---|:---:|:---:|:---:|:---:|:---:|
| **Dashboard** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Profile / Change own password** | 🔑 | 🔑 | 🔑 | 🔑 | 🔑 |
| **Products — view catalog** | ✅ | 👁 | 👁 | 👁 | 👁 |
| **Products — add / edit / deactivate** | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Customers — view** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Customers — add / edit / deactivate** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Orders — take order** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Orders — view history / detail** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Orders — edit / mark paid / delivered** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Active SRPs (price board)** | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Acquisitions — view** | ✅ | ❌ | ✅ | ❌ | ❌ |
| **Acquisitions — add / edit** | ✅ | ❌ | ✅ | ❌ | ❌ |
| **Remittances — view** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Remittances — add / edit** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Employees — view** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Employees — add / edit** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Employee Payments — view / add** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Farm Operations — view** | ✅ | ❌ | ❌ | ✅ | ❌ |
| **Farm Operations — add / edit** | ✅ | ❌ | ❌ | ✅ | ❌ |
| **Export (CSV)** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Settings / Pricing Presets** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **User Management** | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## Role Descriptions

### ADMIN
Full access to every screen and action. The only role that can:
- Access Settings and configure pricing presets
- Create, deactivate, reactivate, and reset passwords for other users
- Export data to CSV
- Manage products and employees

An admin cannot deactivate their own account.

### STORE_ASSISTANT
Day-to-day sales operations. Can:
- Take, edit, mark paid, and mark delivered orders
- Manage the customer list
- Record and view remittances
- View the active SRP price board

Cannot see acquisitions, employees, pricing configuration, or export data.

### PURCHASING
Handles incoming produce. Can:
- Record and edit acquisitions
- View the active SRP price board (to compare cost vs. target selling price)
- View the product catalog (read-only)

Cannot see orders, customers, employees, or settings.

### FARMER
Field operations staff. Can:
- Add and edit farm operations
- View farm operations history
- View the product catalog (read-only, so they can reference product IDs)

Cannot see orders, acquisitions, customers, employees, or settings.

### USER
Fallback / general read-only role. Can view the dashboard only. Assigned automatically when a role is not explicitly specified during account creation. Intended for accounts that need app access but no specific workflow responsibility.

---

## Implementation Notes

### Where role checks live today
| Check | Location |
|---|---|
| `isAdmin()` — boolean gate for Settings icon | `MainScreen` top bar |
| `isAdmin()` — boolean gate for User Management card | `ProfileScreen` |
| `resolveAdmin()` — blocks non-admins from `createUser`, `deactivateUser`, `resetPassword` | `UserManagementViewModel` |

### What needs to change for full RBAC
1. **`SessionManager`** — add `getRole(): String?` (already exists); add helpers `isStoreAssistant()`, `isPurchasing()`, `isFarmer()`, or a single `hasPermission(Permission)` method.
2. **`UserManagementViewModel.createUser()`** — expand `normalizedRole` `when` block to accept all five role strings.
3. **`UserManagementScreen`** — the role picker currently offers only Admin / User toggle; replace with a dropdown or radio group covering all five roles.
4. **`MainScreen` dashboard** — hide nav tiles for feature areas the current role cannot access.
5. **Screen-level guards** — each ViewModel should re-check role on `init` (same pattern as `UserManagementViewModel.resolveAdmin()`) and emit an error if an unauthorized user navigates directly via deep link or back stack.
6. **Write-action guards** — buttons that mutate data (add, edit, delete) should be invisible or disabled when the role is `👁` or `❌` for that area.
