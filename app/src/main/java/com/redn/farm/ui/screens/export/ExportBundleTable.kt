package com.redn.farm.ui.screens.export

/**
 * The ten core business tables in EXP-US-01 / Phase 6 (excludes e.g. product_prices, presets).
 */
enum class ExportBundleTable(val label: String, val filePrefix: String) {
    USERS("Users", "users"),
    PRODUCTS("Products", "products"),
    CUSTOMERS("Customers", "customers"),
    ORDERS("Orders", "orders"),
    ORDER_ITEMS("Order items", "order_items"),
    EMPLOYEES("Employees", "employees"),
    EMPLOYEE_PAYMENTS("Employee payments", "employee_payments"),
    ACQUISITIONS("Acquisitions", "acquisitions"),
    FARM_OPERATIONS("Farm operations", "farm_operations"),
    REMITTANCES("Remittances", "remittances")
}
