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
        assertEquals(2.0, SrpCalculator.bulkQuantityKg(100.0, isPerKg = false, pieceCount = 50.0)!!, 1e-9)
    }

    @Test
    fun bulkQuantityKg_perPiece_allowsDecimalPieceCount() {
        // quantity=100 pieces, pieceCount=3.5 pieces/kg → 100/3.5 = 28.571428...
        assertEquals(28.57142857142857, SrpCalculator.bulkQuantityKg(100.0, isPerKg = false, pieceCount = 3.5)!!, 1e-9)
    }

    @Test
    fun bulkQuantityKg_perPiece_nullPieceCount_returnsNull() {
        assertNull(SrpCalculator.bulkQuantityKg(10.0, isPerKg = false, pieceCount = null))
    }

    @Test
    fun bulkQuantityKg_perPiece_nonPositivePieceCount_returnsNull() {
        assertNull(SrpCalculator.bulkQuantityKg(10.0, isPerKg = false, pieceCount = 0.0))
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
    fun compute_perKgWithPieceCount_stillAppliesSpoilage() {
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 7000.0,
                bulkQuantityKg = 100.0,
                spoilageRate = 0.25,
                additionalCostPerKg = 7200.0 / 700.0,
                channels = channels,
                pieceCount = 10.0,
                isPerKgAcquisition = true,
            ),
        ) as SrpCalculator.Result.Ok
        assertEquals(75.0, r.output.sellableQuantityKg, 1e-9)
        assertEquals(0.25, r.output.spoilageRate, 1e-9)
    }

    @Test
    fun compute_perPieceAcquisition_skipsSpoilage() {
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 500.0,
                bulkQuantityKg = 10.0,
                spoilageRate = 0.25,
                additionalCostPerKg = 7200.0 / 700.0,
                channels = channels,
                pieceCount = 10.0,
                isPerKgAcquisition = false,
            ),
        ) as SrpCalculator.Result.Ok
        assertEquals(10.0, r.output.sellableQuantityKg, 1e-9)
        assertEquals(0.0, r.output.spoilageRate, 1e-9)
        assertEquals(50.0, r.output.costPerSellableKg, 1e-9)
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
                pieceCount = 20.0
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
        // additionalCostPerKg = 0 so C scales ~linearly with bulk cost (hauling is a fixed addend per kg).
        fun run(cost: Double) = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = cost,
                bulkQuantityKg = 50.0,
                spoilageRate = 0.1,
                additionalCostPerKg = 0.0,
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
     *   sellable = 75 kg  →  B/Q_sell ≈ 93.333;  A = 72/7;  C = B/Q_sell + A ≈ 103.619
     *   online:   ceil(103.619×1.35) = ceil(139.886) = 140
     *   reseller: ceil(103.619×1.25) = ceil(129.524) = 130
     *   offline:  ceil(103.619×1.30) = ceil(134.705) = 135
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
                pieceCount = 10.0
            )
        )
        assertTrue(r is SrpCalculator.Result.Ok)
        val o = (r as SrpCalculator.Result.Ok).output

        val cBulk = 7000.0 / 75.0
        assertEquals(100.0, o.bulkQuantityKg, 1e-9)
        assertEquals(0.25, o.spoilageRate, 1e-9)
        assertEquals(additionalCostPerKg, o.additionalCostPerKg, 1e-9)
        assertEquals(cBulk, o.costPerSellableKg, 1e-9)
        assertEquals(cBulk + additionalCostPerKg, o.coreCostPerKg, 1e-9)

        // Per-kg SRPs
        assertEquals(140.0, o.srpOnlinePerKg, 1e-9)
        assertEquals(130.0, o.srpResellerPerKg, 1e-9)
        assertEquals(135.0, o.srpOfflinePerKg, 1e-9)

        // 500g tiers (×0.5, then ceil)
        assertEquals(70.0, o.srpOnline500g, 1e-9)
        assertEquals(65.0, o.srpReseller500g, 1e-9)
        assertEquals(68.0, o.srpOffline500g, 1e-9)

        // 250g tiers (×0.25, then ceil)
        assertEquals(35.0, o.srpOnline250g, 1e-9)
        assertEquals(33.0, o.srpReseller250g, 1e-9)
        assertEquals(34.0, o.srpOffline250g, 1e-9)

        // 100g tiers (×0.1, then ceil)
        assertEquals(14.0, o.srpOnline100g, 1e-9)
        assertEquals(13.0, o.srpReseller100g, 1e-9)
        assertEquals(14.0, o.srpOffline100g, 1e-9)

        // Per-piece SRPs (pieceCount=10; ceil whole ₱ per piece)
        assertEquals(14.0, o.srpOnlinePerPiece!!, 1e-9)
        assertEquals(13.0, o.srpResellerPerPiece!!, 1e-9)
        assertEquals(14.0, o.srpOfflinePerPiece!!, 1e-9)
    }

    /**
     * Zero-spoilage edge case: sellable == bulk quantity, formula simplifies.
     * bulkCost=3000, qty=30 kg, spoilage=0, additionalCostPerKg=10
     *   C = 100 + 10 = 110 ₱/kg
     *   online:   ceil(110×1.35) = ceil(148.5) = 149
     *   reseller: ceil(110×1.25) = ceil(137.5) = 138
     *   offline:  ceil(110×1.30) = ceil(143) = 143
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

        assertEquals(149.0, o.srpOnlinePerKg, 1e-9)
        assertEquals(138.0, o.srpResellerPerKg, 1e-9)
        assertEquals(143.0, o.srpOfflinePerKg, 1e-9)
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

    @Test
    fun outputFromCustomerSrpPerKg_usesChannelRoundingAndDerivesFractional() {
        val r = SrpCalculator.outputFromCustomerSrpPerKg(
            channels,
            srpOnlinePerKg = 100.0,
            srpResellerPerKg = 90.0,
            srpOfflinePerKg = 95.0,
            pieceCount = 10.0,
        ) as SrpCalculator.Result.Ok
        val o = r.output
        assertEquals(100.0, o.srpOnlinePerKg, 1e-9)
        assertEquals(50.0, o.srpOnline500g, 1e-9)
        assertEquals(10.0, o.srpOnlinePerPiece!!, 1e-9)
    }

    @Test
    fun outputFromCustomerSrpPerKg_allowsDecimalPieceCount() {
        // 100 / 2.5 = 40 (ceil to whole php should remain 40).
        val r = SrpCalculator.outputFromCustomerSrpPerKg(
            channels,
            srpOnlinePerKg = 100.0,
            srpResellerPerKg = 90.0,
            srpOfflinePerKg = 95.0,
            pieceCount = 2.5,
        ) as SrpCalculator.Result.Ok

        assertEquals(40.0, r.output.srpOnlinePerPiece!!, 1e-9)
    }

    @Test
    fun compute_absoluteSpoilageKg_sameSellableAs25PercentRate() {
        val additionalCostPerKg = 7200.0 / 700.0
        val rateOut = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 7000.0,
                bulkQuantityKg = 100.0,
                spoilageRate = 0.25,
                additionalCostPerKg = additionalCostPerKg,
                channels = channels,
                pieceCount = 10.0,
                spoilageKg = null,
            ),
        ) as SrpCalculator.Result.Ok
        val absOut = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 7000.0,
                bulkQuantityKg = 100.0,
                spoilageRate = 0.25,
                additionalCostPerKg = additionalCostPerKg,
                channels = channels,
                pieceCount = 10.0,
                spoilageKg = 25.0,
            ),
        ) as SrpCalculator.Result.Ok
        assertEquals(rateOut.output.sellableQuantityKg, absOut.output.sellableQuantityKg, 1e-9)
        assertEquals(75.0, absOut.output.sellableQuantityKg, 1e-9)
        assertEquals(25.0, absOut.output.spoilageAbsoluteKg!!, 1e-9)
        assertEquals(0.25, absOut.output.spoilageRate, 1e-9)
        assertEquals(rateOut.output.srpOnlinePerKg, absOut.output.srpOnlinePerKg, 1e-9)
    }

    @Test
    fun compute_absoluteSpoilageKg_rejectsWhenNotLessThanBulk() {
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 1000.0,
                bulkQuantityKg = 50.0,
                spoilageRate = 0.1,
                additionalCostPerKg = 5.0,
                channels = channels,
                pieceCount = null,
                spoilageKg = 50.0,
            ),
        )
        assertTrue(r is SrpCalculator.Result.Error)
    }

    @Test
    fun compute_perPieceAcquisition_ignoresAbsoluteSpoilageKg() {
        val r = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 500.0,
                bulkQuantityKg = 10.0,
                spoilageRate = 0.25,
                additionalCostPerKg = 7200.0 / 700.0,
                channels = channels,
                pieceCount = 10.0,
                isPerKgAcquisition = false,
                spoilageKg = 5.0,
            ),
        ) as SrpCalculator.Result.Ok
        assertEquals(10.0, r.output.sellableQuantityKg, 1e-9)
        assertEquals(null, r.output.spoilageAbsoluteKg)
    }

    @Test
    fun mergeCostContextWithCustomSrps_preservesSellableContext() {
        val cost = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 1000.0,
                bulkQuantityKg = 50.0,
                spoilageRate = 0.1,
                additionalCostPerKg = 5.0,
                channels = channels,
                pieceCount = null,
            ),
        ) as SrpCalculator.Result.Ok
        val m = SrpCalculator.mergeCostContextWithCustomSrps(
            cost.output,
            channels,
            200.0,
            180.0,
            190.0,
            null,
        ) as SrpCalculator.Result.Ok
        assertEquals(cost.output.sellableQuantityKg, m.output.sellableQuantityKg, 1e-9)
        assertEquals(cost.output.costPerSellableKg, m.output.costPerSellableKg, 1e-9)
        assertEquals(cost.output.coreCostPerKg, m.output.coreCostPerKg, 1e-9)
        assertEquals(cost.output.bulkQuantityKg, m.output.bulkQuantityKg, 1e-9)
        assertEquals(cost.output.spoilageRate, m.output.spoilageRate, 1e-9)
        assertEquals(cost.output.spoilageAbsoluteKg, m.output.spoilageAbsoluteKg)
        assertEquals(cost.output.additionalCostPerKg, m.output.additionalCostPerKg, 1e-9)
        assertEquals(200.0, m.output.srpOnlinePerKg, 1e-9)
    }
}
