package com.redn.farm.data.pricing

import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.model.Product
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderUnitDefaultsTest {

    private val productKg = Product(
        product_id = "P1",
        product_name = "Eggs",
        product_description = "",
        unit_type = "kg",
        is_active = true,
    )

    private val productPiece = Product(
        product_id = "P2",
        product_name = "Bunch",
        product_description = "",
        unit_type = "piece",
        is_active = true,
    )

    @Test
    fun prefersAcquisitionPerPiece_overProductKg() {
        val acq = Acquisition(
            product_id = "P1",
            product_name = "Eggs",
            quantity = 12.0,
            price_per_unit = 1.0,
            total_amount = 12.0,
            is_per_kg = false,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
        )
        assertFalse(defaultIsPerKgForOrderLine(productKg, acq))
    }

    @Test
    fun prefersAcquisitionPerKg_overProductPiece() {
        val acq = Acquisition(
            product_id = "P2",
            product_name = "Bunch",
            quantity = 2.0,
            price_per_unit = 1.0,
            total_amount = 2.0,
            is_per_kg = true,
            date_acquired = 0L,
            location = AcquisitionLocation.FARM,
        )
        assertTrue(defaultIsPerKgForOrderLine(productPiece, acq))
    }

    @Test
    fun noAcquisition_fallsBackToProductUnitType() {
        assertTrue(defaultIsPerKgForOrderLine(productKg, null))
        assertFalse(defaultIsPerKgForOrderLine(productPiece, null))
    }
}
