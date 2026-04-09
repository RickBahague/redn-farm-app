package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.redn.farm.data.model.RemittanceEntryType

@Entity(tableName = "remittances")
data class RemittanceEntity(
    @PrimaryKey(autoGenerate = true)
    val remittance_id: Int = 0,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),  // Using timestamp for SQLite compatibility
    val remarks: String = "",
    val date_updated: Long = System.currentTimeMillis(),
    /** [com.redn.farm.data.model.RemittanceEntryType] */
    val entry_type: String = RemittanceEntryType.REMITTANCE,
)