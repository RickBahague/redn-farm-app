package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["customer_id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val order_id: Int = 0,
    val customer_id: Int,
    val channel: String = "offline",
    val total_amount: Double,
    val order_date: Long = System.currentTimeMillis(),
    val order_update_date: Long = System.currentTimeMillis(),
    val is_paid: Boolean = false,
    val is_delivered: Boolean = false
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["order_id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val order_id: Int,
    val product_id: String,
    val quantity: Double,
    val price_per_unit: Double,
    val is_per_kg: Boolean,
    val total_price: Double
) 