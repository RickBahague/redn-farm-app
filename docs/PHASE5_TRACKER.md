# Phase 5 — Auth & profile (detailed tracker)

**Created:** 2026-04-02  
**Related plan:** `rebuild_plan.md` Phase 5 (this file does **not** modify that document).  
**Goal:** Profile (SYS-US-02), change password (AUTH-US-05), admin user management (AUTH-US-04). Can ship in parallel with other phases.

---

## Status legend

| Mark | Meaning |
|------|---------|
| `[x]` | Implemented in codebase |
| `[~]` | Partial / simplified — see notes |
| `[ ]` | Manual QA / follow-up |

---

## P5-1 — Profile screen (SYS-US-02)

| Item | Status | Notes |
|------|--------|--------|
| Username, full name, role (read-only) | `[x]` | Loaded from `UserDao.getUserByUsername` using `SessionManager.getUsername()` |
| Change password entry | `[x]` | Card → `ChangePasswordScreen` |
| Dashboard entry for all users | `[x]` | `MainScreen` top bar **Person** icon (before admin Settings) |

**Files:** `ProfileScreen.kt`, `ProfileViewModel.kt`, `MainScreen.kt`, `NavGraph.kt` (`Screen.Profile`)

---

## P5-2 — Change password (AUTH-US-05)

| Item | Status | Notes |
|------|--------|--------|
| Current / new / confirm fields | `[x]` | `ChangePasswordScreen` + `PasswordVisualTransformation` |
| Current password verified against hash | `[x]` | `PasswordManager.verifyPassword`; mismatch → field error on current |
| New ≠ confirm | `[x]` | Field-level error on confirm |
| Success keeps session | `[x]` | `updateUser` hash only; no logout |

**Files:** `ChangePasswordScreen.kt`, `ChangePasswordViewModel.kt`, `NavGraph.kt` (`Screen.ChangePassword`)

**Simplification (`[~]`):** Minimum new password length **4** characters (matches seeded demo passwords). Stricter policy can be added later.

---

## P5-3 — Admin user management (AUTH-US-04)

| Item | Status | Notes |
|------|--------|--------|
| Create user (username, full name, role, initial password) | `[x]` | FAB → dialog; roles **User** / **Admin**; `findByUsername` prevents duplicates |
| Deactivate / reactivate | `[x]` | `updateUser` with `is_active`; list shows all users |
| Reset another user’s password | `[x]` | Dialog → `updateUser` with new hash |
| Admin cannot deactivate self | `[x]` | Blocked when `user.username` matches session username |
| Admin-only surface | `[x]` | Profile shows **User management** card only if admin; route guarded in `UserManagementViewModel.isAuthorized` |

**Files:** `UserManagementScreen.kt`, `UserManagementViewModel.kt`, `UserDao.kt` (`findByUsername`, `getUserById`), `NavGraph.kt` (`Screen.UserManagement`)

---

## Data layer

| Item | Status | Notes |
|------|--------|--------|
| `UserDao.findByUsername` | `[x]` | Ignores `is_active` (signup / uniqueness) |
| `UserDao.getUserById` | `[x]` | Reset password target |

---

## Build / tests

| Step | Status | Notes |
|------|--------|--------|
| `./gradlew assembleDebug` | `[x]` | Green (2026-04-02) |
| Manual smoke | `[ ]` | Profile → change password; admin → create / deactivate / reactivate / reset; non-admin cannot use management (deep link shows denial) |

---

## Follow-ups (optional)

- Stronger password rules (length, complexity) and shared validation helper.
- Optional second entry to user management from `SettingsScreen` for admins.
