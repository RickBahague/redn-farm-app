package com.redn.farm.data.util

import com.redn.farm.data.local.entity.AcquisitionEntity

/**
 * FIFO lot allocator for outstanding inventory reports (EOD-US-10, D7).
 *
 * Given a list of acquisition lots for a single product (oldest-first) and the
 * total quantity sold to date, allocates sold quantity to lots in order and
 * returns the remaining quantity in each lot plus aging metadata.
 *
 * FIFO is used only for per-lot drill-down and aging analysis. Weighted average
 * cost (WAC) is computed separately in DayCloseRepository for pricing.
 */
object InventoryFifoAllocator {

    data class LotResult(
        val acquisitionId: Int,
        val dateAcquired: Long,
        /** Original acquisition quantity in kg. */
        val originalQtyKg: Double,
        /** Remaining quantity in kg after FIFO allocation. */
        val remainingQtyKg: Double,
        val pricePerUnit: Double,
        /** Days since date_acquired at the time of computation. */
        val ageDays: Int,
    )

    data class AllocationResult(
        val lots: List<LotResult>,
        /** Oldest date_acquired among lots that still have remaining inventory. */
        val oldestUnsoldDateMillis: Long?,
        /** Total remaining qty across all lots (kg). */
        val totalRemainingKg: Double,
    )

    /**
     * Allocates [totalSoldKg] quantity to [lots] in FIFO order.
     *
     * @param lots Acquisition lots for a single product, sorted by date_acquired ASC.
     * @param totalSoldKg Total quantity sold (in kg) up to and including the close date.
     * @param nowMillis Reference time for age computation. Defaults to System.currentTimeMillis().
     */
    fun allocate(
        lots: List<AcquisitionEntity>,
        totalSoldKg: Double,
        nowMillis: Long = System.currentTimeMillis(),
    ): AllocationResult {
        var remainingToAllocate = totalSoldKg
        val results = mutableListOf<LotResult>()

        for (lot in lots) {
            val lotKg = if (lot.is_per_kg) {
                lot.quantity
            } else {
                // D1: per-piece lots converted to kg via piece_count
                val pc = lot.piece_count ?: 1.0
                if (pc > 0) lot.quantity / pc else lot.quantity
            }

            val consumed = minOf(remainingToAllocate, lotKg)
            val remaining = lotKg - consumed
            remainingToAllocate -= consumed

            val ageDays = ((nowMillis - lot.date_acquired) / MS_PER_DAY).toInt().coerceAtLeast(0)

            results += LotResult(
                acquisitionId = lot.acquisition_id,
                dateAcquired = lot.date_acquired,
                originalQtyKg = lotKg,
                remainingQtyKg = remaining,
                pricePerUnit = lot.price_per_unit,
                ageDays = ageDays,
            )

            if (remainingToAllocate <= 0.0) break
        }

        val unsoldLots = results.filter { it.remainingQtyKg > 0 }
        val oldestUnsold = unsoldLots.minOfOrNull { it.dateAcquired }
        val totalRemaining = unsoldLots.sumOf { it.remainingQtyKg }

        return AllocationResult(
            lots = results,
            oldestUnsoldDateMillis = oldestUnsold,
            totalRemainingKg = totalRemaining,
        )
    }

    private const val MS_PER_DAY = 24L * 60 * 60 * 1000
}
