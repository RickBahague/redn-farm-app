package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.ProductSoldQtyBreakdown

/**
 * Day close / WAC math: acquisitions and [weighted_avg_cost_per_unit] are in **kg**; order lines may be
 * **per kg** or **per piece**. Piece quantities convert via catalog aggregate **pieces per kg** (same
 * source as [com.redn.farm.data.local.dao.AcquisitionDao.getTotalAcquiredByProduct] **max_piece_count**).
 */
internal object DayCloseSoldQty {

    fun kgEquivalent(qtyKg: Double, qtyPc: Double, pieceCountFromAcq: Double?): Double {
        val pc = (pieceCountFromAcq ?: 1.0).let { if (it > 0) it else 1.0 }
        return qtyKg + qtyPc / pc
    }

    fun formatTopProductQtyLine(qtyKg: Double, qtyPc: Double): String {
        val hasKg = qtyKg > 1e-9
        val hasPc = qtyPc > 1e-9
        return when {
            hasKg && hasPc -> "${"%.2f".format(qtyKg)} kg + ${"%.2f".format(qtyPc)} pc"
            hasKg -> "${"%.2f".format(qtyKg)} kg"
            hasPc -> "${"%.2f".format(qtyPc)} pc"
            else -> "0"
        }
    }

    /** Pieces per kg from acquisition aggregate (same basis as day-close ledger kg conversion). */
    fun piecesPerKgFromAcqAggregate(maxPieceCount: Double?): Double =
        (maxPieceCount ?: 1.0).let { if (it > 0) it else 1.0 }

    /**
     * [DayCloseInventoryEntity] ledger quantities are stored in **kg** (see entity KDoc).
     * When the UI label is **pc** (latest acquisition per-piece), multiply by **piecesPerKg** so
     * operators see native piece counts (e.g. 5 pc, not 1.667 “pc”).
     */
    fun inventoryDisplayQuantity(storedKg: Double, unitLabel: String, piecesPerKg: Double?): Double {
        if (unitLabel != "pc") return storedKg
        return storedKg * piecesPerKgFromAcqAggregate(piecesPerKg)
    }

    /**
     * **Theoretical (adj.)** on the inventory card in the operator’s unit (**pc** or **kg**).
     *
     * When **Sold through close** uses **[ProductSoldQtyBreakdown]**, the sold kg for this row is
     * **[kgEquivalent]** of that breakdown so **Acquired − Sold through − Prior var.** matches the
     * displayed numbers (same **n** as elsewhere on the card). Otherwise uses **[ledgerSoldKg]**.
     */
    fun inventoryTheoreticalDisplayQuantity(
        acquiredKg: Double,
        soldThroughBreakdown: ProductSoldQtyBreakdown?,
        ledgerSoldKg: Double,
        priorVarianceKg: Double,
        unitLabel: String,
        piecesPerKg: Double?,
    ): Double {
        val soldKg =
            if (soldThroughBreakdown != null &&
                (soldThroughBreakdown.qty_kg_sold > 1e-9 || soldThroughBreakdown.qty_pc_sold > 1e-9)
            ) {
                kgEquivalent(
                    soldThroughBreakdown.qty_kg_sold,
                    soldThroughBreakdown.qty_pc_sold,
                    piecesPerKg,
                )
            } else {
                ledgerSoldKg
            }
        val remainingKg = acquiredKg - soldKg - priorVarianceKg
        return inventoryDisplayQuantity(remainingKg, unitLabel, piecesPerKg)
    }

    /** Physical count field: user types **pc** when [unitLabel] is **pc** → persist **kg**. */
    fun inventoryCountInputToStoredKg(input: Double, unitLabel: String, piecesPerKg: Double?): Double {
        if (unitLabel != "pc") return input
        val n = piecesPerKgFromAcqAggregate(piecesPerKg)
        return input / n
    }
}
