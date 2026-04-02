package com.redn.farm.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EmployeePaymentAggregatesTest {

    private fun payment(
        id: Int,
        employeeId: Int = 1,
        amount: Double,
        advance: Double? = null,
        liquidated: Double? = null,
        datePaid: Long = 0L,
    ) = EmployeePayment(
        payment_id = id,
        employee_id = employeeId,
        employeeName = "Test",
        amount = amount,
        cash_advance_amount = advance,
        liquidated_amount = liquidated,
        date_paid = datePaid,
        signature = "",
        received_date = null,
    )

    @Test
    fun lifetimeOutstanding_matchesSumAdvanceMinusLiquidated() {
        val rows = listOf(
            payment(1, amount = 1000.0, advance = 500.0, liquidated = null),
            payment(2, amount = 1000.0, advance = null, liquidated = 200.0),
        )
        assertEquals(300.0, rows.lifetimeOutstandingAdvance(), 0.001)
    }

    @Test
    fun lifetimeOutstanding_nullsAsZero() {
        val rows = listOf(payment(1, amount = 100.0, advance = null, liquidated = null))
        assertEquals(0.0, rows.lifetimeOutstandingAdvance(), 0.001)
    }

    @Test
    fun periodTotals_sumsFilteredSubset() {
        val rows = listOf(
            payment(1, amount = 100.0, advance = 10.0, liquidated = 5.0),
            payment(2, amount = 200.0, advance = null, liquidated = 20.0),
        )
        val t = rows.periodTotals()
        assertEquals(300.0, t.totalGross, 0.001)
        assertEquals(10.0, t.totalCashAdvance, 0.001)
        assertEquals(25.0, t.totalLiquidated, 0.001)
    }
}
