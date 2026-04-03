package com.redn.farm.data.pricing

import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}

