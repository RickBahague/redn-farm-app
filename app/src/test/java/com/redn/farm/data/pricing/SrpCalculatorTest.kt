package com.redn.farm.data.pricing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SrpCalculatorTest {

    private val channels = PricingPresetGson.defaultChannelsConfiguration()

    @Test
    fun bulkQuantityKg_perKg_returnsQuantity() {
        assertEquals(12.5, SrpCalculator.bulkQuantityKg(12.5, isPerKg = true, pieceCount = null)!!, 1e-9)
    }

    @Test
    fun bulkQuantityKg_perPiece_dividesByPieceCount() {
        assertEquals(2.0, SrpCalculator.bulkQuantityKg(100.0, isPerKg = false, pieceCount = 50)!!, 1e-9)
    }

    @Test
    fun bulkQuantityKg_perPiece_nullPieceCount_returnsNull() {
        assertNull(SrpCalculator.bulkQuantityKg(10.0, isPerKg = false, pieceCount = null))
    }

    @Test
    fun bulkQuantityKg_perPiece_nonPositivePieceCount_returnsNull() {
        assertNull(SrpCalculator.bulkQuantityKg(10.0, isPerKg = false, pieceCount = 0))
    }

    @Test
    fun compute_rejectsNonPositiveBulkCost() {
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 0.0,
                bulkQuantityKg = 10.0,
                spoilageRate = 0.1,
                additionalCostPerKg = 5.0,
                channels = channels,
                pieceCount = null
            )
        )
        assertTrue(r is SrpCalculator.Result.Error)
    }

    @Test
    fun compute_ok_returnsPositiveSrps() {
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 1000.0,
                bulkQuantityKg = 50.0,
                spoilageRate = 0.1,
                additionalCostPerKg = 10.0,
                channels = channels,
                pieceCount = 20
            )
        )
        assertTrue(r is SrpCalculator.Result.Ok)
        val out = (r as SrpCalculator.Result.Ok).output
        assertTrue(out.srpOnlinePerKg > 0)
        assertTrue(out.srpResellerPerKg > 0)
        assertTrue(out.srpOfflinePerKg > 0)
        assertTrue(out.srpOnlinePerPiece != null && out.srpOnlinePerPiece!! > 0)
    }

    @Test
    fun compute_doublingBulkCost_scalesOnlineSrpApproxDouble() {
        fun run(cost: Double) = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = cost,
                bulkQuantityKg = 50.0,
                spoilageRate = 0.1,
                additionalCostPerKg = 5.0,
                channels = channels,
                pieceCount = null
            )
        ) as SrpCalculator.Result.Ok
        val low = run(1000.0).output.srpOnlinePerKg
        val high = run(2000.0).output.srpOnlinePerKg
        val ratio = high / low
        assertTrue("expected ~2x after peso rounding, got ratio=$ratio", ratio in 1.85..2.15)
    }
}
