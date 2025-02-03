package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.EmployeePaymentDao
import com.redn.farm.data.local.entity.EmployeePaymentEntity
import com.redn.farm.data.model.EmployeePayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EmployeePaymentRepository(
    private val employeePaymentDao: EmployeePaymentDao
) {
    fun getAllPayments(): Flow<List<EmployeePayment>> {
        return employeePaymentDao.getAllPayments().map { payments ->
            payments.map { payment ->
                EmployeePayment(
                    payment_id = payment.payment.payment_id,
                    employee_id = payment.payment.employee_id,
                    employeeName = payment.employeeName,
                    amount = payment.payment.amount,
                    cash_advance_amount = payment.payment.cash_advance_amount,
                    liquidated_amount = payment.payment.liquidated_amount,
                    date_paid = payment.payment.date_paid,
                    signature = payment.payment.signature,
                    received_date = payment.payment.received_date
                )
            }
        }
    }

    suspend fun addPayment(payment: EmployeePayment) {
        employeePaymentDao.addPayment(
            EmployeePaymentEntity(
                payment_id = payment.payment_id,
                employee_id = payment.employee_id,
                amount = payment.amount,
                cash_advance_amount = payment.cash_advance_amount,
                liquidated_amount = payment.liquidated_amount,
                date_paid = payment.date_paid,
                signature = payment.signature,
                received_date = payment.received_date
            )
        )
    }

    suspend fun updatePayment(payment: EmployeePayment) {
        employeePaymentDao.updatePayment(
            EmployeePaymentEntity(
                payment_id = payment.payment_id,
                employee_id = payment.employee_id,
                amount = payment.amount,
                cash_advance_amount = payment.cash_advance_amount,
                liquidated_amount = payment.liquidated_amount,
                date_paid = payment.date_paid,
                signature = payment.signature,
                received_date = payment.received_date
            )
        )
    }

    suspend fun deletePayment(payment: EmployeePayment) {
        employeePaymentDao.deletePayment(payment.toEntity())
    }

    suspend fun truncate() {
        employeePaymentDao.truncate()
    }

    private fun EmployeePaymentEntity.toPayment() = EmployeePayment(
        payment_id = payment_id,
        employee_id = employee_id,
        amount = amount,
        cash_advance_amount = cash_advance_amount,
        liquidated_amount = liquidated_amount,
        date_paid = date_paid,
        signature = signature,
        received_date = received_date
    )

    private fun EmployeePayment.toEntity() = EmployeePaymentEntity(
        payment_id = payment_id,
        employee_id = employee_id,
        amount = amount,
        cash_advance_amount = cash_advance_amount,
        liquidated_amount = liquidated_amount,
        date_paid = date_paid,
        signature = signature,
        received_date = received_date
    )
} 