package com.redn.farm.data.model

import java.time.LocalDateTime

data class Acquisition(
    val acquisition_id: Int = 0,
    val product_id: String,
    val product_name: String,
    val quantity: Double,
    val price_per_unit: Double,
    val total_amount: Double,
    val is_per_kg: Boolean,
    val date_acquired: Long,
    val date_created: Long = System.currentTimeMillis(),
    val date_updated: Long = System.currentTimeMillis(),
    val location: AcquisitionLocation
)

enum class AcquisitionLocation {
    FARM,
    MARKET,
    SUPPLIER,
    OTHER;

    override fun toString(): String {
        return when (this) {
            FARM -> "Farm"
            MARKET -> "Market"
            SUPPLIER -> "Supplier"
            OTHER -> "Other"
        }
    }
} 