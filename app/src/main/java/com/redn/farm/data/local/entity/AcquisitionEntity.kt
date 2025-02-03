package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.redn.farm.data.model.AcquisitionLocation
import java.time.LocalDateTime

@Entity(
    tableName = "acquisitions",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("product_id")
    ]
)
data class AcquisitionEntity(
    @PrimaryKey(autoGenerate = true)
    val acquisition_id: Int = 0,
    val product_id: String,
    val product_name: String,
    val quantity: Double,
    val price_per_unit: Double,
    val total_amount: Double,
    val is_per_kg: Boolean,
    val date_acquired: Long,
    val location: AcquisitionLocation
) 