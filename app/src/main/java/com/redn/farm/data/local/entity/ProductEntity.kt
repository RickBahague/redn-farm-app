package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val product_id: String,
    val product_name: String,
    val product_description: String,
    val unit_type: String,
    val is_active: Boolean = true
) 