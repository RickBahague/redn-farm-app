package com.redn.farm.data.model

data class ProductFilters(
    val searchQuery: String = "",
    val showOutOfStock: Boolean = false,
    val sortBy: String = "name"
) 