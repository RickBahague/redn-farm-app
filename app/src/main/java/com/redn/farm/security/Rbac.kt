package com.redn.farm.security

import java.util.Locale

/**
 * Role constants and permission helpers aligned with `docs/user_roles.md` (do not edit that file here).
 */
object Rbac {

    const val ADMIN = "ADMIN"
    const val STORE_ASSISTANT = "STORE_ASSISTANT"
    const val PURCHASING = "PURCHASING"
    const val FARMER = "FARMER"
    const val USER = "USER"

    val ALL_ROLES: Set<String> = setOf(ADMIN, STORE_ASSISTANT, PURCHASING, FARMER, USER)

    /** Dashboard tile titles used in [com.redn.farm.ui.screens.main.MainScreen]. */
    fun dashboardTileTitles(normalizedRole: String): Set<String> = when (normalizedRole) {
        ADMIN -> setOf("Orders", "Customers", "Inventory", "Farm Ops", "Remittance", "Employees", "Products", "Export")
        STORE_ASSISTANT -> setOf("Orders", "Customers", "Remittance", "Products")
        PURCHASING -> setOf("Inventory", "Products")
        FARMER -> setOf("Farm Ops", "Products")
        USER -> setOf("Products")
        else -> setOf("Products")
    }

    fun normalizeRole(raw: String?): String {
        if (raw.isNullOrBlank()) return USER
        return when (raw.uppercase(Locale.getDefault()).trim()) {
            ADMIN -> ADMIN
            STORE_ASSISTANT, "STORE" -> STORE_ASSISTANT
            PURCHASING, "PURCHASING_ASSISTANT" -> PURCHASING
            FARMER -> FARMER
            USER -> USER
            else -> USER
        }
    }

    fun normalizeRoleForStorage(raw: String): String = normalizeRole(raw)

    fun displayName(normalizedRole: String): String = when (normalizeRole(normalizedRole)) {
        ADMIN -> "Administrator"
        STORE_ASSISTANT -> "Store Assistant"
        PURCHASING -> "Purchasing Assistant"
        FARMER -> "Farmer"
        USER -> "User"
        else -> "User"
    }

    fun canMutateProducts(role: String?): Boolean {
        val r = normalizeRole(role)
        return r == ADMIN || r == STORE_ASSISTANT || r == PURCHASING
    }

    /** Write access aligned with `docs/user_roles.md` matrix (defense in depth alongside navigation). */
    fun canWriteCustomers(role: String?) = normalizeRole(role) in ROLES_CUSTOMERS
    fun canWriteOrders(role: String?) = normalizeRole(role) in ROLES_ORDERS_FLOW
    fun canWriteAcquisitions(role: String?) = normalizeRole(role) in ROLES_ACQUIRE
    fun canWriteRemittances(role: String?) = normalizeRole(role) in ROLES_REMITTANCE
    fun canWriteEmployees(role: String?) = normalizeRole(role) in ROLES_EMPLOYEES
    fun canWriteFarmOperations(role: String?) = normalizeRole(role) in ROLES_FARM_OPS
    fun canExport(role: String?) = normalizeRole(role) in ROLES_EXPORT
    fun canManageSettingsAndPricing(role: String?) = normalizeRole(role) in ROLES_SETTINGS_AND_PRICING
    fun canManageUsers(role: String?) = normalizeRole(role) in ROLES_USER_MANAGEMENT

    val ROLES_PRODUCTS: Set<String> = ALL_ROLES
    val ROLES_CUSTOMERS: Set<String> = setOf(ADMIN, STORE_ASSISTANT)
    val ROLES_ORDERS_FLOW: Set<String> = setOf(ADMIN, STORE_ASSISTANT)
    val ROLES_ACTIVE_SRPS: Set<String> = setOf(ADMIN, STORE_ASSISTANT, PURCHASING)
    val ROLES_ACQUIRE: Set<String> = setOf(ADMIN, PURCHASING)
    val ROLES_REMITTANCE: Set<String> = setOf(ADMIN, STORE_ASSISTANT)
    val ROLES_EMPLOYEES: Set<String> = setOf(ADMIN)
    val ROLES_FARM_OPS: Set<String> = setOf(ADMIN, FARMER)
    val ROLES_EXPORT: Set<String> = setOf(ADMIN)
    val ROLES_SETTINGS_AND_PRICING: Set<String> = setOf(ADMIN)
    val ROLES_USER_MANAGEMENT: Set<String> = setOf(ADMIN)
}
