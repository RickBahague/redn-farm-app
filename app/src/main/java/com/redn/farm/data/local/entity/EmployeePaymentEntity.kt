package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "employee_payments",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["employee_id"],
            childColumns = ["employee_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class EmployeePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val payment_id: Int = 0,
    val employee_id: Int,
    val amount: Double,
    val cash_advance_amount: Double? = null,
    val liquidated_amount: Double? = null,
    val date_paid: Long = System.currentTimeMillis(),
    val signature: String,
    val received_date: Long? = null,
    val is_finalized: Boolean = false,
)