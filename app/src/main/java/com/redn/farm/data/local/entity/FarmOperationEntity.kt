package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import com.redn.farm.data.model.FarmOperationType

@Entity(
    tableName = "farm_operations",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("product_id")
    ]
)
data class FarmOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val operation_id: Int = 0,
    val operation_type: FarmOperationType,
    val operation_date: Long,
    val details: String,
    val area: String,
    val weather_condition: String,
    val personnel: String,
    val product_id: String? = null,
    val product_name: String = "",
    val date_created: Long = System.currentTimeMillis(),
    val date_updated: Long = System.currentTimeMillis()
) 