package com.redn.farm.data.model

import java.time.LocalDateTime

data class Order(
    val order_id: Int = 0,
    val customer_id: Int,
    val channel: String = "offline",
    val customerName: String = "",
    val customerContact: String = "",
    val total_amount: Double,
    val order_date: Long = System.currentTimeMillis(),
    val order_update_date: Long = System.currentTimeMillis(),
    val is_paid: Boolean = false,
    val is_delivered: Boolean = false
)

data class OrderItem(
    val id: Int = 0,
    val order_id: Int = 0,
    val product_id: String,
    val product_name: String,
    val quantity: Double,
    val price_per_unit: Double,
    val is_per_kg: Boolean,
    val total_price: Double
) 