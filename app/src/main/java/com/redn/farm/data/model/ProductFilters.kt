package com.redn.farm.data.model

/** PRD-US-01 AC#7 — filter products by catalog active flag. */
enum class ProductActiveStatusFilter {
    /** Show both active and inactive products. */
    ALL,

    /** Only `is_active == true`. */
    ACTIVE_ONLY,

    /** Only `is_active == false`. */
    INACTIVE_ONLY,
}

data class ProductFilters(
    val searchQuery: String = "",
    val sortBy: String = "name",
    /** Substring match on [Product.unit_type] (case-insensitive). */
    val unitTypeFilter: String = "",
    /** Substring match on [Product.category] (case-insensitive). */
    val categoryFilter: String = "",
    val activeStatus: ProductActiveStatusFilter = ProductActiveStatusFilter.ACTIVE_ONLY,
)
