package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import com.redn.farm.data.model.CustomerType

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val customer_id: Int = 0,
    val firstname: String,
    val lastname: String,
    val contact: String,
    val customer_type: CustomerType = CustomerType.RETAIL,
    val address: String,
    val city: String,
    val province: String,
    val postal_code: String,
    val date_created: LocalDateTime = LocalDateTime.now(),
    val date_updated: LocalDateTime = LocalDateTime.now()
) 