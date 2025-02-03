package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.EmployeePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeePaymentDao {
    @Query("""
        SELECT p.*, e.firstname || ' ' || e.lastname as employeeName 
        FROM employee_payments p
        INNER JOIN employees e ON e.employee_id = p.employee_id
    """)
    fun getAllPayments(): Flow<List<EmployeePaymentWithEmployee>>

    @Insert
    suspend fun addPayment(payment: EmployeePaymentEntity)

    @Update
    suspend fun updatePayment(payment: EmployeePaymentEntity)

    @Delete
    suspend fun deletePayment(payment: EmployeePaymentEntity)

    @Query("SELECT * FROM employee_payments WHERE payment_id = :id")
    suspend fun getPayment(id: Int): EmployeePaymentEntity?

    @Query("DELETE FROM employee_payments")
    suspend fun truncate()
}

data class EmployeePaymentWithEmployee(
    @Embedded val payment: EmployeePaymentEntity,
    val employeeName: String
) 