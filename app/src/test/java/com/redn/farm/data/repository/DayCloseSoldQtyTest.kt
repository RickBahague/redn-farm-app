package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.ProductSoldQtyBreakdown
import org.junit.Assert.assertEquals
import org.junit.Test

class DayCloseSoldQtyTest {

    @Test
    fun kgEquivalent_addsKgAndPcOverPieceCount() {
        assertEquals(10.0, DayCloseSoldQty.kgEquivalent(10.0, 0.0, null), 1e-9)
        assertEquals(2.0, DayCloseSoldQty.kgEquivalent(0.0, 10.0, 5.0), 1e-9)
        assertEquals(12.0, DayCloseSoldQty.kgEquivalent(10.0, 10.0, 5.0), 1e-9)
    }

    @Test
    fun formatTopProductQtyLine() {
        assertEquals("3.00 kg", DayCloseSoldQty.formatTopProductQtyLine(3.0, 0.0))
        assertEquals("5.00 pc", DayCloseSoldQty.formatTopProductQtyLine(0.0, 5.0))
        assertEquals("1.00 kg + 2.00 pc", DayCloseSoldQty.formatTopProductQtyLine(1.0, 2.0))
    }

    @Test
    fun inventoryDisplayQuantity_pc_multipliesByPiecesPerKg() {
        val storedKg = 5.0 / 3.0
        assertEquals(5.0, DayCloseSoldQty.inventoryDisplayQuantity(storedKg, "pc", 3.0), 1e-9)
        assertEquals(storedKg, DayCloseSoldQty.inventoryDisplayQuantity(storedKg, "kg", 3.0), 1e-9)
    }

    @Test
    fun inventoryCountInputToStoredKg_pc_dividesByPiecesPerKg() {
        assertEquals(5.0 / 3.0, DayCloseSoldQty.inventoryCountInputToStoredKg(5.0, "pc", 3.0), 1e-9)
        assertEquals(5.0, DayCloseSoldQty.inventoryCountInputToStoredKg(5.0, "kg", 3.0), 1e-9)
    }

    @Test
    fun inventoryTheoreticalDisplayQuantity_pc_matchesAcquiredMinusSoldMinusPrior_whenBreakdownUsed() {
        val n = 3.0
        val acquiredKg = 10.0 / n
        val breakdown = ProductSoldQtyBreakdown("egg", 0.0, 3.0)
        val soldKg = DayCloseSoldQty.kgEquivalent(0.0, 3.0, n)
        val dqAcq = DayCloseSoldQty.inventoryDisplayQuantity(acquiredKg, "pc", n)
        val dqSold = DayCloseSoldQty.inventoryDisplayQuantity(soldKg, "pc", n)
        val dqPrior = DayCloseSoldQty.inventoryDisplayQuantity(0.0, "pc", n)
        val t = DayCloseSoldQty.inventoryTheoreticalDisplayQuantity(
            acquiredKg,
            breakdown,
            ledgerSoldKg = 99.0,
            priorVarianceKg = 0.0,
            unitLabel = "pc",
            piecesPerKg = n,
        )
        assertEquals(dqAcq - dqSold - dqPrior, t, 1e-9)
        assertEquals(7.0, t, 1e-9)
    }

    @Test
    fun inventoryTheoreticalDisplayQuantity_kg_usesBreakdownKgEquivalent() {
        val breakdown = ProductSoldQtyBreakdown("x", 1.0, 4.0)
        val n = 2.0
        val soldKg = DayCloseSoldQty.kgEquivalent(1.0, 4.0, n)
        val t = DayCloseSoldQty.inventoryTheoreticalDisplayQuantity(
            acquiredKg = 10.0,
            soldThroughBreakdown = breakdown,
            ledgerSoldKg = 99.0,
            priorVarianceKg = 1.0,
            unitLabel = "kg",
            piecesPerKg = n,
        )
        assertEquals(10.0 - soldKg - 1.0, t, 1e-9)
    }
}
