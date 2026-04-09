package com.redn.farm.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** BUG-EMP-01 / EMP-US-05: `amount + cash_advance_amount` (null advance as 0); liquidated excluded. */
class EmployeePaymentNetPayTest {

    private fun payment(
        amount: Double,
        cashAdvance: Double? = null,
        liquidated: Double? = null,
    ) = EmployeePayment(
        employee_id = 1,
        amount = amount,
        cash_advance_amount = cashAdvance,
        liquidated_amount = liquidated,
        signature = "",
    )

    @Test
    fun exampleFromStories_grossPlusAdvance() {
        assertEquals(6000.0, payment(5000.0, 1000.0, 500.0).netPayAmount(), 0.001)
    }

    @Test
    fun nullAdvanceTreatedAsZero() {
        assertEquals(3000.0, payment(3000.0, null, null).netPayAmount(), 0.001)
    }

    @Test
    fun liquidatedDoesNotAffectNetPay() {
        val withLiquidated = payment(100.0, 50.0, 999.0)
        val without = payment(100.0, 50.0, null)
        assertEquals(without.netPayAmount(), withLiquidated.netPayAmount(), 0.001)
        assertEquals(150.0, withLiquidated.netPayAmount(), 0.001)
    }
}
