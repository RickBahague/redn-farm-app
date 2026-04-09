package com.redn.farm.data.model

/** BUG-EMP-01 / EMP-US-05: net pay on form and history rows; `liquidated_amount` does not affect this value. */
fun EmployeePayment.netPayAmount(): Double =
    amount + (cash_advance_amount ?: 0.0)

/** EMP-US-06: `sum(cash_advance_amount) − sum(liquidated_amount)` over these rows (nulls as 0). */
fun List<EmployeePayment>.lifetimeOutstandingAdvance(): Double {
    val advances = sumOf { it.cash_advance_amount ?: 0.0 }
    val liquidated = sumOf { it.liquidated_amount ?: 0.0 }
    return advances - liquidated
}

data class EmployeePaymentPeriodTotals(
    val totalGross: Double,
    val totalCashAdvance: Double,
    val totalLiquidated: Double,
)

/** EMP-US-06 period summary: sums over the already-filtered list for the selected period. */
fun List<EmployeePayment>.periodTotals(): EmployeePaymentPeriodTotals =
    EmployeePaymentPeriodTotals(
        totalGross = sumOf { it.amount },
        totalCashAdvance = sumOf { it.cash_advance_amount ?: 0.0 },
        totalLiquidated = sumOf { it.liquidated_amount ?: 0.0 },
    )
