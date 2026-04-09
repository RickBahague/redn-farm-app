package com.redn.farm.data.model

data class EmployeePayment(
    val payment_id: Int = 0,
    val employee_id: Int,
    val employeeName: String = "",
    val amount: Double,
    val cash_advance_amount: Double? = null,
    val liquidated_amount: Double? = null,
    val date_paid: Long = System.currentTimeMillis(),
    val signature: String,
    val received_date: Long? = null,
    val is_finalized: Boolean = false,
) 