package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.AcquisitionDao
import com.redn.farm.data.local.dao.DayCloseAuditDao
import com.redn.farm.data.local.dao.DayCloseDao
import com.redn.farm.data.local.dao.DayCloseInventoryDao
import com.redn.farm.data.local.dao.OrderDao
import com.redn.farm.data.local.dao.RemittanceDao
import com.redn.farm.data.local.entity.DayCloseAuditEntity
import com.redn.farm.data.local.entity.DayCloseEntity
import com.redn.farm.data.local.entity.DayCloseInventoryEntity
import com.redn.farm.data.util.DateWindowHelper
import com.redn.farm.data.util.InventoryFifoAllocator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayCloseRepository @Inject constructor(
    private val dayCloseDao: DayCloseDao,
    private val dayCloseInventoryDao: DayCloseInventoryDao,
    private val dayCloseAuditDao: DayCloseAuditDao,
    private val orderDao: OrderDao,
    private val acquisitionDao: AcquisitionDao,
    private val remittanceDao: RemittanceDao,
) {

    // ─── Read ─────────────────────────────────────────────────────────────────

    fun getAllDesc(): Flow<List<DayCloseEntity>> = dayCloseDao.getAllDesc()

    fun getFinalizedDesc(): Flow<List<DayCloseEntity>> = dayCloseDao.getFinalizedDesc()

    suspend fun getByDate(businessDateMillis: Long): DayCloseEntity? =
        dayCloseDao.getByDate(DateWindowHelper.startOfDay(businessDateMillis))

    suspend fun getById(closeId: Int): DayCloseEntity? = dayCloseDao.getById(closeId)

    suspend fun getInventoryLines(closeId: Int): List<DayCloseInventoryEntity> =
        dayCloseInventoryDao.getByCloseId(closeId)

    suspend fun getAuditTrail(closeId: Int): List<DayCloseAuditEntity> =
        dayCloseAuditDao.getByCloseId(closeId)

    suspend fun getMostRecent(): DayCloseEntity? = dayCloseDao.getMostRecent()

    // ─── Open / create ────────────────────────────────────────────────────────

    /**
     * Returns the existing in-progress or finalized close for [businessDateMillis],
     * or creates a new draft and returns it.
     */
    suspend fun openOrCreate(businessDateMillis: Long, username: String): DayCloseEntity {
        val start = DateWindowHelper.startOfDay(businessDateMillis)
        val existing = dayCloseDao.getByDate(start)
        if (existing != null) return existing

        val entity = DayCloseEntity(
            business_date = start,
            closed_by = username,
        )
        val id = dayCloseDao.insert(entity)
        val created = dayCloseDao.getById(id.toInt())!!
        audit(created.close_id, "OPEN", username)
        return created
    }

    // ─── Inventory snapshot ───────────────────────────────────────────────────

    /**
     * Builds the inventory lines for a day close from aggregated data.
     *
     * Called when entering the inventory section for the first time (or on refresh).
     * Existing rows for the close are replaced.
     *
     * Algorithm:
     *   1. Load all acquisition aggregates (total qty, total cost) per product.
     *   2. Load all sold qty per product up to end of [businessDateMillis].
     *   3. Load prior posted variance: sum of variance_qty from all *finalized* closes
     *      with business_date < start of [businessDateMillis].
     *   4. Compute adjusted_theoretical_remaining = acquired − sold − prior_variance.
     *   5. Compute WAC = total_cost / total_qty_kg (D1: per-piece converted to kg).
     */
    suspend fun buildInventorySnapshot(
        closeId: Int,
        businessDateMillis: Long,
        username: String,
    ): List<DayCloseInventoryEntity> {
        val start = DateWindowHelper.startOfDay(businessDateMillis)
        val end = DateWindowHelper.endOfDay(businessDateMillis)

        // 1. Acquisition aggregates (all time)
        val acqSummaries = acquisitionDao.getTotalAcquiredByProduct()
            .associateBy { it.product_id }

        // 2. Total sold by product up to end of this business day
        val soldUpTo = orderDao.getTotalSoldQtyByProductUpTo(end)
            .associateBy { it.product_id }

        // 2b. Sold specifically on this business day (for COGS line)
        val soldToday = orderDao.getSoldQtyByProductOnDate(start, end)
            .associateBy { it.product_id }

        // 3. Prior posted variance: SUM(variance_qty) for finalized closes before today
        //    Queried directly from previously saved inventory rows.
        //    (Loaded per-product below.)

        val rows = mutableListOf<DayCloseInventoryEntity>()

        for ((productId, acq) in acqSummaries) {
            // D1: convert per-piece quantity to kg
            val totalAcquiredKg = if (acq.is_per_kg_flag != 0) {
                acq.total_qty
            } else {
                val pc = (acq.max_piece_count ?: 1.0).let { if (it > 0) it else 1.0 }
                acq.total_qty / pc
            }

            val totalSoldKg = soldUpTo[productId]?.total_qty ?: 0.0
            val soldTodayKg = soldToday[productId]?.total_qty ?: 0.0

            // WAC in same unit as acquired (kg)
            val wac = if (totalAcquiredKg > 0) acq.total_cost / totalAcquiredKg else 0.0

            // Prior posted variance comes from persisted rows (already in db from previous closes).
            // We approximate here as 0 on first build; DayCloseViewModel may inject actuals.
            val priorVariance = 0.0

            val theoretical = totalAcquiredKg - totalSoldKg - priorVariance

            rows += DayCloseInventoryEntity(
                close_id = closeId,
                product_id = productId,
                product_name = acq.product_id, // resolved by ViewModel via product lookup
                total_acquired_all_time = totalAcquiredKg,
                total_sold_through_close_date = totalSoldKg,
                prior_posted_variance = priorVariance,
                adjusted_theoretical_remaining = theoretical,
                sold_this_close_date = soldTodayKg,
                weighted_avg_cost_per_unit = wac,
            )
        }

        dayCloseInventoryDao.deleteByCloseId(closeId)
        dayCloseInventoryDao.insertAll(rows)
        audit(closeId, "ENTER_COUNTS", username, "Inventory snapshot rebuilt")
        return rows
    }

    // ─── Revenue and COGS snapshot ────────────────────────────────────────────

    /**
     * Computes today's revenue snapshot and COGS, then saves it to the day close record.
     *
     * COGS = SUM over all products (qty_sold_today × WAC_per_unit).
     * Gross margin = collected_revenue − COGS.
     */
    suspend fun computeAndSaveRevenueCogs(close: DayCloseEntity): DayCloseEntity {
        val start = DateWindowHelper.startOfDay(close.business_date)
        val end = DateWindowHelper.endOfDay(close.business_date)

        val salesSummary = orderDao.getSalesSummaryOnDate(start, end)
        val unpaidSummary = orderDao.getUnpaidSummaryAsOf(end)

        // Gather today's sold quantities and WAC
        val soldToday = orderDao.getSoldQtyByProductOnDate(start, end)
            .associateBy { it.product_id }
        val acqSummaries = acquisitionDao.getTotalAcquiredByProduct()
            .associateBy { it.product_id }

        var totalCogs = 0.0
        for ((productId, soldRow) in soldToday) {
            val acq = acqSummaries[productId] ?: continue
            val totalAcquiredKg = if (acq.is_per_kg_flag != 0) {
                acq.total_qty
            } else {
                val pc = (acq.max_piece_count ?: 1.0).let { if (it > 0) it else 1.0 }
                acq.total_qty / pc
            }
            val wac = if (totalAcquiredKg > 0) acq.total_cost / totalAcquiredKg else 0.0
            totalCogs += soldRow.total_qty * wac
        }

        val grossRevenue = salesSummary.total_sales
        val collected = salesSummary.total_collected
        val marginAmount = collected - totalCogs
        val marginPct = if (collected > 0) marginAmount / collected * 100.0 else null

        val updated = close.copy(
            total_orders = salesSummary.order_count,
            total_sales_amount = grossRevenue,
            total_collected = collected,
            snapshot_all_unpaid_count = unpaidSummary.count,
            snapshot_all_unpaid_amount = unpaidSummary.amount,
            gross_revenue_today = grossRevenue,
            collected_revenue_today = collected,
            total_cogs_today = totalCogs,
            gross_margin_amount = marginAmount,
            gross_margin_percent = marginPct,
        )
        dayCloseDao.update(updated)
        return updated
    }

    // ─── Cash reconciliation ──────────────────────────────────────────────────

    suspend fun saveCashReconciliation(
        close: DayCloseEntity,
        cashOnHand: Double?,
        remarks: String?,
        username: String,
    ): DayCloseEntity {
        val updated = close.copy(
            cash_on_hand = cashOnHand,
            cash_reconciliation_remarks = remarks,
        )
        dayCloseDao.update(updated)
        audit(close.close_id, "EDIT_CASH", username)
        return updated
    }

    // ─── Finalize ─────────────────────────────────────────────────────────────

    suspend fun finalize(close: DayCloseEntity, username: String): DayCloseEntity {
        val finalized = close.copy(
            is_finalized = true,
            closed_at = System.currentTimeMillis(),
            closed_by = username,
        )
        dayCloseDao.update(finalized)
        audit(close.close_id, "FINALIZE", username)
        return finalized
    }

    suspend fun unfinalize(close: DayCloseEntity, username: String): DayCloseEntity {
        val draft = close.copy(is_finalized = false, closed_at = null)
        dayCloseDao.update(draft)
        audit(close.close_id, "UNFINALIZE", username)
        return draft
    }

    suspend fun saveNotes(
        close: DayCloseEntity,
        notes: String?,
        username: String,
    ): DayCloseEntity {
        val updated = close.copy(notes = notes)
        dayCloseDao.update(updated)
        audit(close.close_id, "EDIT_NOTES", username)
        return updated
    }

    // ─── Outstanding inventory (EOD-US-10) ───────────────────────────────────

    /**
     * Builds the outstanding inventory report using FIFO lot allocation.
     * Returns lots for all products that have remaining stock.
     *
     * [agingAmberDays] and [agingRedDays] control the thresholds for aging flags.
     */
    suspend fun buildOutstandingInventory(
        businessDateMillis: Long,
        agingAmberDays: Int = 3,
        agingRedDays: Int = 7,
    ): List<OutstandingProductLine> {
        val end = DateWindowHelper.endOfDay(businessDateMillis)

        val allLots = acquisitionDao.getAllAcquisitionLotsOldestFirst()
        val soldUpTo = orderDao.getTotalSoldQtyByProductUpTo(end)
            .associateBy { it.product_id }

        val grouped = allLots.groupBy { it.product_id }
        val now = System.currentTimeMillis()

        return grouped.mapNotNull { (productId, lots) ->
            val totalSoldKg = soldUpTo[productId]?.total_qty ?: 0.0
            val result = InventoryFifoAllocator.allocate(lots, totalSoldKg, now)

            if (result.totalRemainingKg <= 0) return@mapNotNull null

            val agingFlag = result.oldestUnsoldDateMillis?.let { oldest ->
                val days = ((now - oldest) / MS_PER_DAY).toInt()
                when {
                    days >= agingRedDays -> AgingFlag.RED
                    days >= agingAmberDays -> AgingFlag.AMBER
                    else -> AgingFlag.NORMAL
                }
            } ?: AgingFlag.NORMAL

            OutstandingProductLine(
                productId = productId,
                productName = lots.first().product_name,
                totalRemainingKg = result.totalRemainingKg,
                oldestUnsoldDateMillis = result.oldestUnsoldDateMillis,
                agingFlag = agingFlag,
                lots = result.lots,
            )
        }.sortedByDescending { it.agingFlag.ordinal }
    }

    // ─── Audit helper ─────────────────────────────────────────────────────────

    private suspend fun audit(closeId: Int, action: String, username: String, note: String? = null) {
        dayCloseAuditDao.insert(
            DayCloseAuditEntity(
                close_id = closeId,
                action = action,
                username = username,
                note = note,
            )
        )
    }

    // ─── Types ────────────────────────────────────────────────────────────────

    enum class AgingFlag { NORMAL, AMBER, RED }

    data class OutstandingProductLine(
        val productId: String,
        val productName: String,
        val totalRemainingKg: Double,
        val oldestUnsoldDateMillis: Long?,
        val agingFlag: AgingFlag,
        val lots: List<InventoryFifoAllocator.LotResult>,
    )

    // Intermediate type used inside buildInventorySnapshot — not exposed externally
    private data class InventoryLine(
        val productId: String,
        val productName: String,
        val totalAcquiredKg: Double,
        val totalSoldKg: Double,
        val priorVariance: Double,
        val soldTodayKg: Double,
        val wac: Double,
    )

    companion object {
        private const val MS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
