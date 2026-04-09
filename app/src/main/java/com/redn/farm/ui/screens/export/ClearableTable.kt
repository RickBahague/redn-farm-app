package com.redn.farm.ui.screens.export

/**
 * EXP-US-02 — tables an admin may clear in FK-safe batch order via [ExportViewModel.clearSelectedTables].
 */
enum class ClearableTable(val label: String) {
    CUSTOMERS("Customers"),
    PRODUCTS("Products (and all product prices)"),
    PRODUCT_PRICES("Product prices only"),
    ORDERS("Orders & order items"),
    EMPLOYEE_PAYMENTS("Employee payments"),
    EMPLOYEES("Employees"),
    ACQUISITIONS("Acquisitions"),
    FARM_OPERATIONS("Farm operations"),
    REMITTANCES("Remittances"),
    PRICING_PRESETS("Pricing presets & activation log"),
    USERS_NON_SEED("Users (except default admin & user)"),
    ;

    companion object {
        /**
         * Tables to add so FK-related data is cleared in a safe order (EXP-US-02 AC3).
         * Returns null when [selected] is already consistent.
         */
        fun suggestedDependencyAdditions(selected: Set<ClearableTable>): Set<ClearableTable>? {
            val add = mutableSetOf<ClearableTable>()
            if (PRODUCTS in selected && ACQUISITIONS !in selected) add.add(ACQUISITIONS)
            if (CUSTOMERS in selected && ORDERS !in selected) add.add(ORDERS)
            if (EMPLOYEES in selected && EMPLOYEE_PAYMENTS !in selected) add.add(EMPLOYEE_PAYMENTS)
            return add.takeIf { it.isNotEmpty() }
        }

        fun dependencyPromptMessage(additions: Set<ClearableTable>): String {
            val parts = mutableListOf<String>()
            if (ACQUISITIONS in additions) {
                parts += "Acquisitions reference products — include Acquisitions in this clear."
            }
            if (ORDERS in additions) {
                parts += "Orders reference customers — include Orders & order items in this clear."
            }
            if (EMPLOYEE_PAYMENTS in additions) {
                parts += "Employee payments reference employees — include Employee payments in this clear."
            }
            return parts.joinToString("\n\n")
        }
    }
}
