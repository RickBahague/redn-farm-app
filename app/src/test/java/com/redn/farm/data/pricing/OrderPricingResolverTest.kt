package com.redn.farm.data.pricing

import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.model.ProductPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPricingResolverTest {

    @Test
    fun srpFromAcquisition_perPiece_usesDerivedFromPerKgWhenMissingPerPiece() {
        val acq = Acquisition(
            product_id = "p1",
            product_name = "Prod 1",
            quantity = 10.0,
            price_per_unit = 1.0,
            total_amount = 10.0,
            is_per_kg = false,
            piece_count = 2.0,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
            // Per-piece SRPs missing on purpose:
            srp_online_per_kg = 100.0,
            srp_online_per_piece = null,
        )

        val derived = OrderPricingResolver.srpFromAcquisition(acquisition = acq, channel = "online", isPerKg = false)
        assertEquals(PricingChannelEngine.perPieceSrp(100.0, 2.0), derived!!, 1e-9)
    }

    @Test
    fun srpFromAcquisition_perPiece_returnsNullWhenNoPieceCount() {
        val acq = Acquisition(
            product_id = "p1",
            product_name = "Prod 1",
            quantity = 10.0,
            price_per_unit = 1.0,
            total_amount = 10.0,
            is_per_kg = false,
            piece_count = null,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
            srp_online_per_kg = 100.0,
            srp_online_per_piece = null,
        )

        val derived = OrderPricingResolver.srpFromAcquisition(acquisition = acq, channel = "online", isPerKg = false)
        assertNull(derived)
    }

    @Test
    fun minPerPieceSrpAcrossChannels_returnsMinimumPositive() {
        val acq = Acquisition(
            product_id = "p1",
            product_name = "Prod 1",
            quantity = 1.0,
            price_per_unit = 1.0,
            total_amount = 1.0,
            is_per_kg = true,
            piece_count = 4.0,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
            srp_online_per_kg = 100.0,
            srp_reseller_per_kg = 80.0,
            srp_offline_per_kg = 120.0,
        )
        val minPc = OrderPricingResolver.minPerPieceSrpAcrossChannels(acq)!!
        val expectedOffline = PricingChannelEngine.perPieceSrp(120.0, 4.0)
        val expectedReseller = PricingChannelEngine.perPieceSrp(80.0, 4.0)
        assertEquals(minOf(expectedOffline, expectedReseller), minPc, 1e-9)
    }

    @Test
    fun catalogSrpSummaryAmounts_nullWhenNoPositiveSrps() {
        val acq = Acquisition(
            product_id = "p1",
            product_name = "Prod 1",
            quantity = 1.0,
            price_per_unit = 1.0,
            total_amount = 1.0,
            is_per_kg = true,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
        )
        assertNull(OrderPricingResolver.catalogSrpSummaryAmounts(acq))
    }

    @Test
    fun productSupportsDualUnit_trueWhenPerKgSrpAndDerivedPerPiece_noExplicitPerPieceColumns() {
        val pp = ProductPrice(product_id = "p1", per_kg_price = 10.0, per_piece_price = null)
        val acq = Acquisition(
            product_id = "p1",
            product_name = "Prod 1",
            quantity = 1.0,
            price_per_unit = 1.0,
            total_amount = 1.0,
            is_per_kg = true,
            piece_count = 4.0,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
            srp_online_per_kg = 100.0,
            srp_online_per_piece = null,
            srp_reseller_per_piece = null,
            srp_offline_per_piece = null,
        )
        assertTrue(OrderPricingResolver.productSupportsDualUnit(pp, acq))
    }

    @Test
    fun productSupportsDualUnit_falseWhenNoProductPriceRow() {
        val acq = Acquisition(
            product_id = "p1",
            product_name = "Prod 1",
            quantity = 1.0,
            price_per_unit = 1.0,
            total_amount = 1.0,
            is_per_kg = true,
            piece_count = 4.0,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
            srp_online_per_kg = 100.0,
        )
        assertFalse(OrderPricingResolver.productSupportsDualUnit(null, acq))
    }

    @Test
    fun productSupportsDualUnit_falseWhenOnlyPerKgSrpAndNoPieceCountForDerivation() {
        val pp = ProductPrice(product_id = "p1", per_kg_price = 10.0, per_piece_price = null)
        val acq = Acquisition(
            product_id = "p1",
            product_name = "Prod 1",
            quantity = 1.0,
            price_per_unit = 1.0,
            total_amount = 1.0,
            is_per_kg = true,
            piece_count = null,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
            srp_online_per_kg = 100.0,
            srp_online_per_piece = null,
        )
        assertFalse(OrderPricingResolver.productSupportsDualUnit(pp, acq))
    }
}

