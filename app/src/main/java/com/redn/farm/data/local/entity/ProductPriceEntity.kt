package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_prices",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProductPriceEntity(
    @PrimaryKey(autoGenerate = true)
    val price_id: Int = 0,
    val product_id: String,
    val per_kg_price: Double?,
    val per_piece_price: Double?,
    val discounted_per_kg_price: Double? = null,
    val discounted_per_piece_price: Double? = null,
    val date_created: Long = System.currentTimeMillis()
)
