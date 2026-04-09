package com.redn.farm.ui.screens.eod

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayCloseViewModelCashGuardTest {

    @Test
    fun noCashOnHand_zeroExpectedMinusRemitted_doesNotRequireRemarks() {
        val missing = cashRemarksMissingForFinalize(
            cashOnHand = null,
            expectedCashFromOrders = 500.0,
            remittedToday = 500.0,
            remarks = null,
        )

        assertFalse(missing)
    }

    @Test
    fun noCashOnHand_nonZeroExpectedMinusRemitted_withoutRemarks_requiresRemarks() {
        val missing = cashRemarksMissingForFinalize(
            cashOnHand = null,
            expectedCashFromOrders = 900.0,
            remittedToday = 600.0,
            remarks = "",
        )

        assertTrue(missing)
    }

    @Test
    fun noCashOnHand_nonZeroExpectedMinusRemitted_withRemarks_doesNotRequireRemarks() {
        val missing = cashRemarksMissingForFinalize(
            cashOnHand = null,
            expectedCashFromOrders = 900.0,
            remittedToday = 600.0,
            remarks = "Pending pickup at branch",
        )

        assertFalse(missing)
    }

    @Test
    fun cashOnHandEntered_countedVsDrawerMismatch_withoutRemarks_requiresRemarks() {
        val missing = cashRemarksMissingForFinalize(
            cashOnHand = 250.0,
            expectedCashFromOrders = 900.0,
            remittedToday = 600.0,
            remarks = " ",
        )

        assertTrue(missing)
    }

    @Test
    fun cashOnHandEntered_countedVsDrawerMismatch_withRemarks_doesNotRequireRemarks() {
        val missing = cashRemarksMissingForFinalize(
            cashOnHand = 250.0,
            expectedCashFromOrders = 900.0,
            remittedToday = 600.0,
            remarks = "Short by 50 after recount",
        )

        assertFalse(missing)
    }

    @Test
    fun cashOnHandEntered_countedMatchesDrawer_doesNotRequireRemarks() {
        val missing = cashRemarksMissingForFinalize(
            cashOnHand = 300.0,
            expectedCashFromOrders = 900.0,
            remittedToday = 600.0,
            remarks = null,
        )

        assertFalse(missing)
    }
}
