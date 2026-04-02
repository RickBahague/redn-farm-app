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

    /**
     * Exact-value test derived from PricingReference.md US-6 defaults:
     *   hauling weight = 700 kg
     *   fees: driver 2000 + fuel 4000 + toll 1000 + handling 200 = 7200
     *   additionalCostPerKg = 7200 / 700 ≈ 10.2857
     *   markups: online 35%, reseller 25%, offline 30%
     *   spoilage = 25%
     *   rounding: ceil_whole_peso
     *
     * Input: bulkCost=7000, qty=100 kg, pieceCount=10
     *   sellable = 75 kg  →  C = 7000/75 ≈ 93.333
     *   online:   ceil(93.333×1.35 + 72/7) = ceil(136.286) = 137
     *   reseller: ceil(93.333×1.25 + 72/7) = ceil(126.952) = 127
     *   offline:  ceil(93.333×1.30 + 72/7) = ceil(131.619) = 132
     */
    @Test
    fun compute_us6DefaultValues_exactSrps() {
        val additionalCostPerKg = 7200.0 / 700.0   // = 72/7
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 7000.0,
                bulkQuantityKg = 100.0,
                spoilageRate = 0.25,
                additionalCostPerKg = additionalCostPerKg,
                channels = channels,
                pieceCount = 10
            )
        )
        assertTrue(r is SrpCalculator.Result.Ok)
        val o = (r as SrpCalculator.Result.Ok).output

        // Per-kg SRPs
        assertEquals(137.0, o.srpOnlinePerKg, 1e-9)
        assertEquals(127.0, o.srpResellerPerKg, 1e-9)
        assertEquals(132.0, o.srpOfflinePerKg, 1e-9)

        // 500g tiers (×0.5, then ceil)
        assertEquals(69.0, o.srpOnline500g, 1e-9)   // ceil(68.5 - eps) = 69
        assertEquals(64.0, o.srpReseller500g, 1e-9)  // ceil(63.5 - eps) = 64
        assertEquals(66.0, o.srpOffline500g, 1e-9)   // ceil(66.0 - eps) = 66

        // 250g tiers (×0.25, then ceil)
        assertEquals(35.0, o.srpOnline250g, 1e-9)    // ceil(34.25 - eps) = 35
        assertEquals(32.0, o.srpReseller250g, 1e-9)  // ceil(31.75 - eps) = 32
        assertEquals(33.0, o.srpOffline250g, 1e-9)   // ceil(33.0 - eps) = 33

        // 100g tiers (×0.1, then ceil)
        assertEquals(14.0, o.srpOnline100g, 1e-9)    // ceil(13.7 - eps) = 14
        assertEquals(13.0, o.srpReseller100g, 1e-9)  // ceil(12.7 - eps) = 13
        assertEquals(14.0, o.srpOffline100g, 1e-9)   // ceil(13.2 - eps) = 14

        // Per-piece SRPs (pieceCount=10; ceil whole ₱ per piece)
        assertEquals(14.0, o.srpOnlinePerPiece!!, 1e-9)   // ceil(13.7 - eps) = 14
        assertEquals(13.0, o.srpResellerPerPiece!!, 1e-9) // ceil(12.7 - eps) = 13
        assertEquals(14.0, o.srpOfflinePerPiece!!, 1e-9)  // ceil(13.2 - eps) = 14
    }

    /**
     * Zero-spoilage edge case: sellable == bulk quantity, formula simplifies.
     * bulkCost=3000, qty=30 kg, spoilage=0, additionalCostPerKg=10
     *   C = 100 ₱/kg
     *   online:   ceil(100×1.35 + 10) = ceil(145) = 145
     *   reseller: ceil(100×1.25 + 10) = ceil(135) = 135
     *   offline:  ceil(100×1.30 + 10) = ceil(140) = 140
     */
    @Test
    fun compute_zeroSpoilage_exactSrps() {
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 3000.0,
                bulkQuantityKg = 30.0,
                spoilageRate = 0.0,
                additionalCostPerKg = 10.0,
                channels = channels,
                pieceCount = null
            )
        )
        assertTrue(r is SrpCalculator.Result.Ok)
        val o = (r as SrpCalculator.Result.Ok).output

        assertEquals(145.0, o.srpOnlinePerKg, 1e-9)
        assertEquals(135.0, o.srpResellerPerKg, 1e-9)
        assertEquals(140.0, o.srpOfflinePerKg, 1e-9)
        assertEquals(null, o.srpOnlinePerPiece)
    }

    /** Whole-peso per-kg value does not ceil up due to EPS guard. */
    @Test
    fun compute_exactWholePesoSrp_doesNotRoundUp() {
        // bulkCost=6750, qty=100, spoilage=0.10, additionalCostPerKg=0
        // sellable=90, C=75.0 exactly
        // online: ceil(75*1.35 + 0) = ceil(101.25 - eps) = 102
        // Verify no double-rounding artefact when C is exact
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 6750.0,
                bulkQuantityKg = 100.0,
                spoilageRate = 0.10,
                additionalCostPerKg = 0.0,
                channels = channels,
                pieceCount = null
            )
        ) as SrpCalculator.Result.Ok
        assertEquals(102.0, r.output.srpOnlinePerKg, 1e-9)   // ceil(101.25 - eps) = 102
        assertEquals(94.0,  r.output.srpResellerPerKg, 1e-9)  // ceil(93.75 - eps) = 94
        assertEquals(98.0,  r.output.srpOfflinePerKg, 1e-9)   // ceil(97.5 - eps) = 98
    }
}
