package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true)
    val employee_id: Int = 0,
    val firstname: String,
    val lastname: String,
    val contact: String,
    val date_created: Long = System.currentTimeMillis(),
    val date_updated: Long = System.currentTimeMillis()
) 