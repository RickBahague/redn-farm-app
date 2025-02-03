package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remittances")
data class RemittanceEntity(
    @PrimaryKey(autoGenerate = true)
    val remittance_id: Int = 0,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),  // Using timestamp for SQLite compatibility
    val remarks: String = "",
    val date_updated: Long = System.currentTimeMillis()  // Add this field
)