package com.redn.farm.data.model

data class CartItem(
    val product: Product,
    val quantity: Double,
    val isPerKg: Boolean,
    val price: Double
) {
    val total: Double
        get() = quantity * price
} 