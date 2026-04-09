package com.redn.farm.data.model

data class ProductFilters(
    val searchQuery: String = "",
    val showOutOfStock: Boolean = false,
    val sortBy: String = "name",
    /** Substring match on [Product.unit_type] (case-insensitive). */
    val unitTypeFilter: String = "",
    /** Substring match on [Product.category] (case-insensitive). */
    val categoryFilter: String = "",
) 