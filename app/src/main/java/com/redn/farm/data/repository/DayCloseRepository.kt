package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.AcquisitionDao
import com.redn.farm.data.local.dao.ChannelSalesRow
import com.redn.farm.data.local.dao.DayCloseAuditDao
import com.redn.farm.data.local.dao.DayCloseDao
import com.redn.farm.data.local.dao.DayCloseInventoryDao
import com.redn.farm.data.local.dao.DailyOrderBreakdown
import com.redn.farm.data.local.dao.DigitalCollectionsSummary
import com.redn.farm.data.local.dao.EmployeePaymentDao
import com.redn.farm.data.local.dao.EmployeePaymentWithEmployee
import com.redn.farm.data.local.dao.OrderDao
import com.redn.farm.data.local.dao.OrderWithDetails
import com.redn.farm.data.local.dao.ProductDao
import com.redn.farm.data.local.dao.ProductSoldQtyBreakdown
import com.redn.farm.data.local.dao.ProductTopRevenueRow
import com.redn.farm.data.local.dao.RemittanceDao
import com.redn.farm.data.local.entity.DayCloseAuditEntity
import com.redn.farm.data.local.entity.DayCloseEntity
import com.redn.farm.data.local.entity.DayCloseInventoryEntity
import com.redn.farm.data.util.DateWindowHelper
import com.redn.farm.data.util.InventoryFifoAllocator
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
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
    private val productDao: ProductDao,
    private val employeePaymentDao: EmployeePaymentDao,
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
     * If there are no rows yet for this close, builds and inserts the ledger (EOD-US-03).
     * Does not delete existing rows (preserves in-progress counts).
     */
    suspend fun ensureInventorySeeded(
        closeId: Int,
        businessDateMillis: Long,
        username: String,
    ): List<DayCloseInventoryEntity> {
        val existing = dayCloseInventoryDao.getByCloseId(closeId)
        if (existing.isNotEmpty()) return existing
        val rows = computeInventoryRows(closeId, businessDateMillis)
        dayCloseInventoryDao.insertAll(rows)
        audit(closeId, "ENTER_COUNTS", username, "Inventory snapshot seeded")
        return rows
    }

    /**
     * Rebuilds inventory from aggregated data (drops existing rows for this close).
     */
    suspend fun buildInventorySnapshot(
        closeId: Int,
        businessDateMillis: Long,
        username: String,
    ): List<DayCloseInventoryEntity> {
        dayCloseInventoryDao.deleteByCloseId(closeId)
        val rows = computeInventoryRows(closeId, businessDateMillis)
        dayCloseInventoryDao.insertAll(rows)
        audit(closeId, "ENTER_COUNTS", username, "Inventory snapshot rebuilt")
        return rows
    }

    private suspend fun computeInventoryRows(
        closeId: Int,
        businessDateMillis: Long,
    ): List<DayCloseInventoryEntity> {
        val start = DateWindowHelper.startOfDay(businessDateMillis)
        val end = DateWindowHelper.endOfDay(businessDateMillis)

        val acqSummaries = acquisitionDao.getTotalAcquiredByProduct()
            .associateBy { it.product_id }

        val soldUpTo = orderDao.getTotalSoldQtyBreakdownByProductUpTo(end)
            .associateBy { it.product_id }

        val soldToday = orderDao.getSoldQtyBreakdownByProductOnDate(start, end)
            .associateBy { it.product_id }

        val priorMap = dayCloseInventoryDao.getPriorPostedVarianceByProduct(start)
            .associate { it.product_id to it.total_variance }

        val nameMap = productDao.getAllProductIdNames()
            .associate { it.product_id to it.product_name }

        val rows = mutableListOf<DayCloseInventoryEntity>()

        for ((productId, acq) in acqSummaries) {
            val totalAcquiredKg = if (acq.is_per_kg_flag != 0) {
                acq.total_qty
            } else {
                val pc = (acq.max_piece_count ?: 1.0).let { if (it > 0) it else 1.0 }
                acq.total_qty / pc
            }

            val pc = acq.max_piece_count
            val totalSoldKg = soldUpTo[productId]?.let { b ->
                DayCloseSoldQty.kgEquivalent(b.qty_kg_sold, b.qty_pc_sold, pc)
            } ?: 0.0
            val soldTodayKg = soldToday[productId]?.let { b ->
                DayCloseSoldQty.kgEquivalent(b.qty_kg_sold, b.qty_pc_sold, pc)
            } ?: 0.0

            val wac = if (totalAcquiredKg > 0) acq.total_cost / totalAcquiredKg else 0.0

            val priorVariance = priorMap[productId] ?: 0.0

            val theoretical = totalAcquiredKg - totalSoldKg - priorVariance

            rows += DayCloseInventoryEntity(
                close_id = closeId,
                product_id = productId,
                product_name = nameMap[productId] ?: productId,
                total_acquired_all_time = totalAcquiredKg,
                total_sold_through_close_date = totalSoldKg,
                prior_posted_variance = priorVariance,
                adjusted_theoretical_remaining = theoretical,
                sold_this_close_date = soldTodayKg,
                weighted_avg_cost_per_unit = wac,
            )
        }

        return rows
    }

    suspend fun updateInventoryLine(line: DayCloseInventoryEntity) {
        dayCloseInventoryDao.update(line)
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

        val soldToday = orderDao.getSoldQtyBreakdownByProductOnDate(start, end)
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
            val kgSold = DayCloseSoldQty.kgEquivalent(
                soldRow.qty_kg_sold,
                soldRow.qty_pc_sold,
                acq.max_piece_count,
            )
            totalCogs += kgSold * wac
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

    // ─── EOD screen aggregates (Stream E) ─────────────────────────────────────

    data class CumulativePosition(
        val totalAcquisitionInvestment: Double,
        val totalRevenueCollectedAllTime: Double,
        val outstandingInventoryValue: Double,
        val netRecovered: Double,
    )

    data class LastAcquisitionDetail(
        val dateMillis: Long,
        val quantity: Double,
        val pricePerUnit: Double,
        val unitLabel: String,
    )

    data class EodUiSnapshot(
        val dailyBreakdown: DailyOrderBreakdown,
        val byChannel: List<ChannelSalesRow>,
        val topProducts: List<ProductTopRevenueRow>,
        val expectedCashFromOrders: Double,
        val remittedToday: Double,
        val disbursementsToday: Double,
        val digital: DigitalCollectionsSummary,
        val unpaidTodayCount: Int,
        val acquisitionsNoSalesCount: Int,
        val unpaidOrders: List<OrderWithDetails>,
        val employeePayments: List<EmployeePaymentWithEmployee>,
        val wagesTotalToday: Double,
        val cumulative: CumulativePosition,
        val lastAcqMillisByProduct: Map<String, Long>,
        val lastAcqDetailByProduct: Map<String, LastAcquisitionDetail>,
        val inventoryUnitByProduct: Map<String, String>,
        /** [AcquisitionDao] aggregate **max_piece_count** per product — display kg→pc for inventory rows. */
        val inventoryPiecesPerKgByProduct: Map<String, Double>,
        /** Same **order_items** breakdown as **Top products** for the business date (Sales Summary alignment). */
        val inventorySoldTodayBreakdownByProduct: Map<String, ProductSoldQtyBreakdown>,
        /** Line **revenue** on the business date per product (pairs with [inventorySoldTodayBreakdownByProduct]). */
        val inventoryDayRevenueByProduct: Map<String, Double>,
        /**
         * Cumulative **order_items** kg/pc split through end of close date — same basis as **Top products**
         * formatting for **Sold through close** (not kg→pc conversion of [DayCloseInventoryEntity.total_sold_through_close_date]).
         */
        val inventorySoldThroughCloseBreakdownByProduct: Map<String, ProductSoldQtyBreakdown>,
        val outstandingRevenueToday: Double,
    )

    suspend fun loadEodUiSnapshot(
        businessDateMillis: Long,
        theoreticalInventoryLines: List<DayCloseInventoryEntity>,
    ): EodUiSnapshot {
        val start = DateWindowHelper.startOfDay(businessDateMillis)
        val end = DateWindowHelper.endOfDay(businessDateMillis)

        val dailyBreakdown = orderDao.getDailyOrderBreakdownOnDate(start, end)
        val sales = orderDao.getSalesSummaryOnDate(start, end)
        val byChannel = orderDao.getSalesByChannel(start, end)
        val topProducts = orderDao.getTopProductsByRevenue(start, end, limit = 5)
        val expectedCash = orderDao.getPaidOfflineResellerTotalOnDate(start, end)
        val remitted = remittanceDao.getSumRemittancesOnDate(start, end)
        val disbursementsToday = remittanceDao.getSumDisbursementsOnDate(start, end)
        val digital = orderDao.getDigitalCollectionsOnDate(start, end)
        val unpaidTodayCount = orderDao.getUnpaidOrderCountOnDate(start, end)
        val acquisitionsNoSalesCount =
            acquisitionDao.countAcquiredProductsWithNoSalesOnSameDay(start, end)
        val unpaidOrders = orderDao.getAllUnpaidOrdersOldestFirst()
        val employeePayments = employeePaymentDao.getPaymentsOnDate(start, end)
        val wagesTotalToday = employeePayments.sumOf { it.payment.amount }

        val totalAcquisitionInvestment = acquisitionDao.getTotalAcquisitionSpendAllTime()
        val totalRevenueCollectedAllTime = orderDao.getTotalCollectedAllTime()
        val outstandingInventoryValue = theoreticalInventoryLines.sumOf { line ->
            max(0.0, line.adjusted_theoretical_remaining) * line.weighted_avg_cost_per_unit
        }
        val netRecovered = totalRevenueCollectedAllTime - totalAcquisitionInvestment

        val lastAcqMillisByProduct = acquisitionDao.getLastAcquisitionMillisByProduct()
            .associate { it.product_id to it.last_acquired_millis }
        val lastAcqDetailByProduct = acquisitionDao.getLatestAcquisitionDetailsByProduct()
            .associate { row ->
                row.product_id to LastAcquisitionDetail(
                    dateMillis = row.date_acquired,
                    quantity = row.quantity,
                    pricePerUnit = row.price_per_unit,
                    unitLabel = if (row.is_per_kg_flag) "kg" else "pc",
                )
            }
        val inventoryUnitByProduct = acquisitionDao.getLatestUnitModeByProduct()
            .associate { row -> row.product_id to if (row.is_per_kg_flag) "kg" else "pc" }
        val inventoryPiecesPerKgByProduct = acquisitionDao.getTotalAcquiredByProduct()
            .associate { row ->
                row.product_id to DayCloseSoldQty.piecesPerKgFromAcqAggregate(row.max_piece_count)
            }
        val inventorySoldTodayBreakdownByProduct =
            orderDao.getSoldQtyBreakdownByProductOnDate(start, end).associateBy { it.product_id }
        val inventoryDayRevenueByProduct =
            orderDao.getProductRevenueOnDate(start, end).associate { it.product_id to it.revenue }
        val inventorySoldThroughCloseBreakdownByProduct =
            orderDao.getTotalSoldQtyBreakdownByProductUpTo(end).associateBy { it.product_id }

        val outstandingRevenueToday = sales.total_sales - sales.total_collected

        return EodUiSnapshot(
            dailyBreakdown = dailyBreakdown,
            byChannel = byChannel,
            topProducts = topProducts,
            expectedCashFromOrders = expectedCash,
            remittedToday = remitted,
            disbursementsToday = disbursementsToday,
            digital = digital,
            unpaidTodayCount = unpaidTodayCount,
            acquisitionsNoSalesCount = acquisitionsNoSalesCount,
            unpaidOrders = unpaidOrders,
            employeePayments = employeePayments,
            wagesTotalToday = wagesTotalToday,
            cumulative = CumulativePosition(
                totalAcquisitionInvestment = totalAcquisitionInvestment,
                totalRevenueCollectedAllTime = totalRevenueCollectedAllTime,
                outstandingInventoryValue = outstandingInventoryValue,
                netRecovered = netRecovered,
            ),
            lastAcqMillisByProduct = lastAcqMillisByProduct,
            lastAcqDetailByProduct = lastAcqDetailByProduct,
            inventoryUnitByProduct = inventoryUnitByProduct,
            inventoryPiecesPerKgByProduct = inventoryPiecesPerKgByProduct,
            inventorySoldTodayBreakdownByProduct = inventorySoldTodayBreakdownByProduct,
            inventoryDayRevenueByProduct = inventoryDayRevenueByProduct,
            inventorySoldThroughCloseBreakdownByProduct = inventorySoldThroughCloseBreakdownByProduct,
            outstandingRevenueToday = outstandingRevenueToday,
        )
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

    // ─── Finalize / unfinalize ─────────────────────────────────────────────────

    /**
     * Persists [inventoryLines] for this close, then marks the close finalized.
     */
    suspend fun finalize(
        close: DayCloseEntity,
        inventoryLines: List<DayCloseInventoryEntity>,
        username: String,
    ): DayCloseEntity {
        dayCloseInventoryDao.deleteByCloseId(close.close_id)
        val rows = inventoryLines.map { line ->
            line.copy(close_id = close.close_id, id = 0)
        }
        if (rows.isNotEmpty()) {
            dayCloseInventoryDao.insertAll(rows)
        }
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
     * When today's day close for [businessDateMillis] is finalized, per-product
     * [DayCloseInventoryEntity.actual_remaining] overrides FIFO remaining (AC11).
     */
    suspend fun buildOutstandingInventory(
        businessDateMillis: Long,
        agingAmberDays: Int = 3,
        agingRedDays: Int = 7,
    ): List<OutstandingProductLine> {
        val dayStart = DateWindowHelper.startOfDay(businessDateMillis)
        val end = DateWindowHelper.endOfDay(businessDateMillis)

        val todayClose = dayCloseDao.getByDate(dayStart)
        val actualKgByProduct: Map<String, Double> =
            if (todayClose?.is_finalized == true) {
                dayCloseInventoryDao.getByCloseId(todayClose.close_id)
                    .mapNotNull { line ->
                        line.actual_remaining?.let { line.product_id to it }
                    }
                    .toMap()
            } else {
                emptyMap()
            }

        val allLots = acquisitionDao.getAllAcquisitionLotsOldestFirst()
        val acqSummaries = acquisitionDao.getTotalAcquiredByProduct()
            .associateBy { it.product_id }
        val soldUpTo = orderDao.getTotalSoldQtyBreakdownByProductUpTo(end)
            .associateBy { it.product_id }
        val priorSpoilageByProduct = dayCloseInventoryDao.getPriorPostedVarianceByProduct(dayStart)
            .associate { it.product_id to it.total_variance }

        val grouped = allLots.groupBy { it.product_id }
        val now = System.currentTimeMillis()

        return grouped.mapNotNull { (productId, lots) ->
            val totalSoldKg = soldUpTo[productId]?.let { b ->
                DayCloseSoldQty.kgEquivalent(
                    b.qty_kg_sold,
                    b.qty_pc_sold,
                    acqSummaries[productId]?.max_piece_count,
                )
            } ?: 0.0
            val result = InventoryFifoAllocator.allocate(lots, totalSoldKg, now)
            val totalAcquiredKg = lots.sumOf { lot ->
                if (lot.is_per_kg) {
                    lot.quantity
                } else {
                    val pieceCount = lot.piece_count ?: 1.0
                    if (pieceCount > 0.0) lot.quantity / pieceCount else lot.quantity
                }
            }
            val priorPostedSpoilageKg = priorSpoilageByProduct[productId] ?: 0.0
            val theoreticalOnHandKg = totalAcquiredKg - totalSoldKg - priorPostedSpoilageKg
            val weightedAverageCostPerKg = if (totalAcquiredKg > 0.0) {
                lots.sumOf { it.total_amount } / totalAcquiredKg
            } else {
                0.0
            }

            val actualOverride = actualKgByProduct[productId]
            val totalRemainingKg = actualOverride ?: result.totalRemainingKg
            val useFifoLots = actualOverride == null

            if (totalRemainingKg <= 0) return@mapNotNull null

            val fifoValuePhp = result.lots.sumOf { it.remainingQtyKg * it.pricePerUnit }
            val displayValuePhp = when {
                actualOverride != null && result.totalRemainingKg > 1e-9 ->
                    actualOverride * (fifoValuePhp / result.totalRemainingKg)
                actualOverride != null ->
                    lots.maxByOrNull { it.date_acquired }?.let { actualOverride * it.price_per_unit }
                        ?: 0.0
                else -> fifoValuePhp
            }

            val agingSourceMillis = when {
                !useFifoLots -> todayClose?.closed_at ?: result.oldestUnsoldDateMillis
                else -> result.oldestUnsoldDateMillis
            }

            val agingFlag = agingSourceMillis?.let { oldest ->
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
                totalRemainingKg = totalRemainingKg,
                displayValuePhp = displayValuePhp,
                oldestUnsoldDateMillis = agingSourceMillis,
                daysOnHand = agingSourceMillis?.let { ((now - it) / MS_PER_DAY).toInt().coerceAtLeast(0) } ?: 0,
                agingFlag = agingFlag,
                lots = if (useFifoLots) result.lots else emptyList(),
                usesActualFromDayClose = actualOverride != null,
                totalAcquiredKg = totalAcquiredKg,
                totalSoldKg = totalSoldKg,
                priorPostedSpoilageKg = priorPostedSpoilageKg,
                theoreticalOnHandKg = theoreticalOnHandKg,
                weightedAverageCostPerKg = weightedAverageCostPerKg,
            )
        }.sortedByDescending { line ->
            line.daysOnHand
        }
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
        val displayValuePhp: Double,
        val oldestUnsoldDateMillis: Long?,
        val daysOnHand: Int,
        val agingFlag: AgingFlag,
        val lots: List<InventoryFifoAllocator.LotResult>,
        val usesActualFromDayClose: Boolean = false,
        val totalAcquiredKg: Double,
        val totalSoldKg: Double,
        val priorPostedSpoilageKg: Double,
        val theoreticalOnHandKg: Double,
        val weightedAverageCostPerKg: Double,
    )

    companion object {
        private const val MS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
