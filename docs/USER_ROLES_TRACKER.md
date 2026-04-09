# User roles (RBAC) implementation tracker

Source of truth for role definitions: `docs/user_roles.md` (read-only for this work — do not edit that file here).

## Phase 1 — Core RBAC primitives

| Item | Status |
|------|--------|
| `Rbac.kt` — constants, normalization, dashboard tiles, `displayName`, `canMutateProducts`, route role sets | Done |
| `RbacGate.kt` — `RequireRole` composable | Done |
| `MainScreen` / `MainViewModel` — dashboard from `Rbac.dashboardTileTitles`, normalized role | Done |
| `LoginViewModel` — session stores `Rbac.normalizeRoleForStorage(user.role)` | Done |

## Phase 2 — Navigation guards

| Item | Status |
|------|--------|
| `NavGraph` — `RequireRole` on products, customers, orders (+ history/detail/edit), active SRPs, acquire, remittance, employees (+ payments), farm ops (+ history), export, settings + all pricing preset routes, user management | Done |
| Profile / change password / about / main / login — no extra gate (any logged-in user) | N/A |

## Phase 3 — Products read-only (farmer / user)

| Item | Status |
|------|--------|
| `ManageProductsViewModel` — `canMutateProducts` from session + `Rbac` | Done |
| `ManageProductsScreen` / `ProductCard` — hide add / edit / delete / fallback when read-only | Done |

## Phase 4 — User management UI

| Item | Status |
|------|--------|
| `CreateUserDialog` — five roles using `Rbac` constants | Done |
| `UserAdminCard` / `ProfileScreen` — `Rbac.displayName` | Done |
| `UserManagementViewModel` / `ProfileViewModel` — admin = `ROLES_USER_MANAGEMENT` + normalized role | Done |

## Phase 5 — Optional / follow-up

| Item | Status |
|------|--------|
| In-screen write guards (customers, orders, remittance, acquisitions, farm ops, employees, products, export, user management) via `Rbac.canWrite*` / `canMutateProducts` / `canExport` / `canManageUsers` | Done |
| DB seed: idempotent demo users `store` / `purchasing` / `farmer` (passwords `store123`, `purchase123`, `farmer123`) in `FarmDatabase.ensureDemoRoleUsers` | Done |
| Unit tests `RbacWritePermissionsTest`; instrumented `RbacSessionInstrumentedTest` (session + RBAC smoke) | Done |

**Notes**

- Full Compose navigation tests per role would require a Hilt/Compose test harness; Phase 5 delivers ViewModel/write guards + session matrix tests instead.
- Demo accounts are created on DB open when missing; they are not a Room migration (app uses destructive fallback migrations in dev).

_Last updated: 2026-04-02._
