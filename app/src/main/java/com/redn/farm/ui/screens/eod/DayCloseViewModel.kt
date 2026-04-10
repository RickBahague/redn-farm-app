package com.redn.farm.ui.screens.eod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.entity.DayCloseEntity
import com.redn.farm.data.local.entity.DayCloseInventoryEntity
import com.redn.farm.data.repository.DayCloseRepository
import com.redn.farm.data.util.DateWindowHelper
import com.redn.farm.data.pricing.SalesChannel
import com.redn.farm.security.Rbac
import com.redn.farm.utils.ThermalEodChannelRow
import com.redn.farm.utils.ThermalEodInventoryRow
import com.redn.farm.data.repository.DayCloseSoldQty
import com.redn.farm.utils.ThermalEodProductRow
import com.redn.farm.utils.ThermalEodUnpaidRow
import com.redn.farm.utils.buildEodSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject

private const val CASH_EPS = 0.01

internal fun cashDiscrepancyForFinalize(
    cashOnHand: Double?,
    expectedCashFromOrders: Double,
    remittedToday: Double,
): Double {
    val drawerExpected = expectedCashFromOrders - remittedToday
    return if (cashOnHand != null) cashOnHand - drawerExpected else drawerExpected
}

internal fun cashRemarksMissingForFinalize(
    cashOnHand: Double?,
    expectedCashFromOrders: Double,
    remittedToday: Double,
    remarks: String?,
): Boolean {
    val discrepancy = cashDiscrepancyForFinalize(
        cashOnHand = cashOnHand,
        expectedCashFromOrders = expectedCashFromOrders,
        remittedToday = remittedToday,
    )
    if (abs(discrepancy) <= CASH_EPS) return false
    return remarks.isNullOrBlank()
}

// ─── UI state ──────────────────────────────────────────────────────────────

sealed class DayCloseUiState {
    object Loading : DayCloseUiState()
    data class Ready(
        val close: DayCloseEntity,
        val inventoryLines: List<DayCloseInventoryEntity>,
        val snapshot: DayCloseRepository.EodUiSnapshot,
        val canEditInventoryCounts: Boolean,
        val isAdmin: Boolean,
        val inReviewStep: Boolean = false,
        val showNegativeMarginWarning: Boolean = false,
    ) : DayCloseUiState()

    data class Error(val message: String) : DayCloseUiState()
}

sealed class DayCloseEvent {
    data class ShowSnackbar(val message: String) : DayCloseEvent()
    object ConfirmNegativeMargin : DayCloseEvent()
    object Finalized : DayCloseEvent()
    object CashRemarksRequired : DayCloseEvent()
}

@HiltViewModel
class DayCloseViewModel @Inject constructor(
    private val repo: DayCloseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DayCloseUiState>(DayCloseUiState.Loading)
    val uiState: StateFlow<DayCloseUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<DayCloseEvent?>(null)
    val events: StateFlow<DayCloseEvent?> = _events.asStateFlow()

    /** Load or create a day close for the given business date. */
    fun open(businessDateMillis: Long, username: String, role: String) {
        viewModelScope.launch {
            _uiState.value = DayCloseUiState.Loading
            try {
                val todayStart = DateWindowHelper.startOfDay(System.currentTimeMillis())
                val targetStart = DateWindowHelper.startOfDay(businessDateMillis)
                val roleNorm = Rbac.normalizeRole(role)

                if (targetStart > todayStart) {
                    _uiState.value = DayCloseUiState.Error("Cannot run day close for a future date.")
                    return@launch
                }
                if (targetStart < todayStart && roleNorm != Rbac.ADMIN) {
                    _uiState.value = DayCloseUiState.Error(
                        "Only an administrator can open day close for a past date. Use Day Close History."
                    )
                    return@launch
                }

                val close = repo.openOrCreate(businessDateMillis, username)
                val updated = repo.computeAndSaveRevenueCogs(close)
                val lines = if (updated.is_finalized) {
                    repo.getInventoryLines(updated.close_id)
                } else {
                    repo.ensureInventorySeeded(updated.close_id, updated.business_date, username)
                    repo.getInventoryLines(updated.close_id)
                }
                val snap = repo.loadEodUiSnapshot(updated.business_date, lines)
                val canEdit = Rbac.canEditInventoryCounts(role)
                _uiState.value = DayCloseUiState.Ready(
                    close = updated,
                    inventoryLines = lines,
                    snapshot = snap,
                    canEditInventoryCounts = canEdit,
                    isAdmin = roleNorm == Rbac.ADMIN,
                    inReviewStep = false,
                )
            } catch (e: Exception) {
                _uiState.value = DayCloseUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun refreshStateAfterCloseMutation(
        close: DayCloseEntity,
        username: String,
        role: String,
    ) {
        val recomputed = repo.computeAndSaveRevenueCogs(close)
        val lines = if (recomputed.is_finalized) {
            repo.getInventoryLines(recomputed.close_id)
        } else {
            repo.getInventoryLines(recomputed.close_id).ifEmpty {
                repo.ensureInventorySeeded(recomputed.close_id, recomputed.business_date, username)
            }
        }
        val snap = repo.loadEodUiSnapshot(recomputed.business_date, lines)
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        _uiState.value = state.copy(
            close = recomputed,
            inventoryLines = lines,
            snapshot = snap,
            canEditInventoryCounts = Rbac.canEditInventoryCounts(role),
            isAdmin = Rbac.normalizeRole(role) == Rbac.ADMIN,
        )
    }

    /** Rebuild the inventory snapshot from aggregated data (resets counts). */
    fun refreshInventorySnapshot(username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        if (state.close.is_finalized) return
        viewModelScope.launch {
            try {
                repo.buildInventorySnapshot(
                    closeId = state.close.close_id,
                    businessDateMillis = state.close.business_date,
                    username = username,
                )
                val lines = repo.getInventoryLines(state.close.close_id)
                val snap = repo.loadEodUiSnapshot(state.close.business_date, lines)
                _uiState.value = state.copy(inventoryLines = lines, snapshot = snap)
            } catch (e: Exception) {
                _events.value = DayCloseEvent.ShowSnackbar("Failed to refresh inventory: ${e.message}")
            }
        }
    }

    /**
     * Physical count in **display** units: **kg** when the row is per-kg, **pieces** when per-piece
     * (**BUG-EOD-03**); persisted as **kg** in [DayCloseInventoryEntity].
     */
    fun enterActualCount(productId: String, rawInput: Double) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        if (state.close.is_finalized) return
        val idx = state.inventoryLines.indexOfFirst { it.product_id == productId }
        if (idx < 0) return
        val line = state.inventoryLines[idx]
        if (!line.is_counted) return
        val snap = state.snapshot
        val unit = snap.inventoryUnitByProduct[productId] ?: "kg"
        val ppk = snap.inventoryPiecesPerKgByProduct[productId]
        val actualKg = DayCloseSoldQty.inventoryCountInputToStoredKg(rawInput, unit, ppk)
        val varianceQty = actualKg - line.adjusted_theoretical_remaining
        val varianceCost = varianceQty * line.weighted_avg_cost_per_unit
        val updatedLine = line.copy(
            actual_remaining = actualKg,
            variance_qty = varianceQty,
            variance_cost = varianceCost,
        )
        val newList = state.inventoryLines.toMutableList().also { it[idx] = updatedLine }
        _uiState.value = state.copy(inventoryLines = newList)
        viewModelScope.launch {
            repo.updateInventoryLine(updatedLine)
            val refreshedSnap = repo.loadEodUiSnapshot(state.close.business_date, newList)
            (_uiState.value as? DayCloseUiState.Ready)?.let { r ->
                _uiState.value = r.copy(snapshot = refreshedSnap)
            }
        }
    }

    fun setLineCounted(productId: String, counted: Boolean) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        if (state.close.is_finalized) return
        val idx = state.inventoryLines.indexOfFirst { it.product_id == productId }
        if (idx < 0) return
        val line = state.inventoryLines[idx]
        val updatedLine = if (counted) {
            line.copy(is_counted = true)
        } else {
            line.copy(
                is_counted = false,
                actual_remaining = null,
                variance_qty = null,
                variance_cost = null,
            )
        }
        val newList = state.inventoryLines.toMutableList().also { it[idx] = updatedLine }
        _uiState.value = state.copy(inventoryLines = newList)
        viewModelScope.launch {
            repo.updateInventoryLine(updatedLine)
            val snap = repo.loadEodUiSnapshot(state.close.business_date, newList)
            (_uiState.value as? DayCloseUiState.Ready)?.let { r ->
                _uiState.value = r.copy(snapshot = snap)
            }
        }
    }

    /** Save cash reconciliation data. */
    fun saveCash(cashOnHand: Double?, remarks: String?, username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        viewModelScope.launch {
            val updated = repo.saveCashReconciliation(state.close, cashOnHand, remarks, username)
            _uiState.value = state.copy(close = updated)
        }
    }

    fun saveNotes(notes: String?, username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        viewModelScope.launch {
            val updated = repo.saveNotes(state.close, notes, username)
            _uiState.value = state.copy(close = updated)
        }
    }

    fun unfinalize(username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        if (!state.isAdmin || !state.close.is_finalized) return
        viewModelScope.launch {
            try {
                val draft = repo.unfinalize(state.close, username)
                val lines = repo.getInventoryLines(draft.close_id)
                val snap = repo.loadEodUiSnapshot(draft.business_date, lines)
                _uiState.value = state.copy(close = draft, inventoryLines = lines, snapshot = snap)
                _events.value = DayCloseEvent.ShowSnackbar("Day close unlocked for editing")
            } catch (e: Exception) {
                _events.value = DayCloseEvent.ShowSnackbar("Un-finalize failed: ${e.message}")
            }
        }
    }

    private fun cashRemarksMissing(state: DayCloseUiState.Ready): Boolean {
        return cashRemarksMissingForFinalize(
            cashOnHand = state.close.cash_on_hand,
            expectedCashFromOrders = state.snapshot.expectedCashFromOrders,
            remittedToday = state.snapshot.remittedToday,
            remarks = state.close.cash_reconciliation_remarks,
        )
    }

    /** Request finalization. Shows confirmation dialog if margin is negative (D4). */
    fun requestFinalize(username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        if (!state.inReviewStep) return
        if (cashRemarksMissing(state)) {
            _events.value = DayCloseEvent.CashRemarksRequired
            return
        }
        val margin = state.close.gross_margin_amount
        if (margin != null && margin < 0) {
            _events.value = DayCloseEvent.ConfirmNegativeMargin
            return
        }
        doFinalize(username)
    }

    /** Called after user confirms negative margin dialog. */
    fun confirmFinalizeWithNegativeMargin(username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        if (!state.inReviewStep) return
        if (cashRemarksMissing(state)) {
            _events.value = DayCloseEvent.CashRemarksRequired
            return
        }
        doFinalize(username)
    }

    fun startReview() {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        if (state.close.is_finalized) return
        _uiState.value = state.copy(inReviewStep = true)
    }

    fun cancelReview() {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        if (state.close.is_finalized) return
        _uiState.value = state.copy(inReviewStep = false)
    }

    private fun doFinalize(username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        viewModelScope.launch {
            try {
                val finalized = repo.finalize(state.close, state.inventoryLines, username)
                val lines = repo.getInventoryLines(finalized.close_id)
                val snap = repo.loadEodUiSnapshot(finalized.business_date, lines)
                _uiState.value = state.copy(close = finalized, inventoryLines = lines, snapshot = snap, inReviewStep = false)
                _events.value = DayCloseEvent.Finalized
            } catch (e: Exception) {
                _events.value = DayCloseEvent.ShowSnackbar("Finalize failed: ${e.message}")
            }
        }
    }

    fun buildEodPrintText(printedBy: String): String? {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return null
        val s = state.snapshot
        val channels = s.byChannel.map { row ->
            val label = SalesChannel.label(SalesChannel.normalize(row.channel))
            ThermalEodChannelRow(label, row.order_count, row.total_sales)
        }
        val top = s.topProducts.map { p ->
            ThermalEodProductRow(
                name = p.product_name,
                qtyDisplay = DayCloseSoldQty.formatTopProductQtyLine(p.qty_kg_sold, p.qty_pc_sold),
                revenue = p.revenue,
            )
        }
        val inv = state.inventoryLines.map { line ->
            val unitLabel = s.inventoryUnitByProduct[line.product_id] ?: "kg"
            val ppk = s.inventoryPiecesPerKgByProduct[line.product_id]
            ThermalEodInventoryRow(
                name = line.product_name,
                theoreticalQty = DayCloseSoldQty.inventoryDisplayQuantity(
                    line.adjusted_theoretical_remaining,
                    unitLabel,
                    ppk,
                ),
                actualQty = line.actual_remaining?.let {
                    DayCloseSoldQty.inventoryDisplayQuantity(it, unitLabel, ppk)
                },
                varianceQty = line.variance_qty?.let {
                    DayCloseSoldQty.inventoryDisplayQuantity(it, unitLabel, ppk)
                },
                unitLabel = unitLabel,
            )
        }
        val spoilageCost = state.inventoryLines
            .filter { it.is_counted }
            .sumOf { line ->
                val v = line.variance_qty
                if (v != null && v > 0) line.variance_cost ?: 0.0 else 0.0
            }
        val unpaid = s.unpaidOrders
        val cap = 10
        val head = unpaid.take(cap)
        val extra = (unpaid.size - cap).coerceAtLeast(0)
        val listedTotal = head.sumOf { it.order.total_amount }
        val unpaidRows = head.map { o ->
            ThermalEodUnpaidRow(
                orderId = o.order.order_id,
                customer = o.customerName,
                amount = o.order.total_amount,
            )
        }
        return buildEodSummary(
            isDraft = !state.close.is_finalized,
            businessDateMillis = state.close.business_date,
            totalOrders = state.close.total_orders,
            grossRevenue = state.close.gross_revenue_today ?: 0.0,
            collected = state.close.collected_revenue_today ?: 0.0,
            unpaidAllCount = state.close.snapshot_all_unpaid_count,
            unpaidAllAmount = state.close.snapshot_all_unpaid_amount,
            channels = channels,
            topProducts = top,
            inventory = inv,
            totalVarianceCost = spoilageCost,
            expectedCash = s.expectedCashFromOrders,
            remitted = s.remittedToday,
            cashOnHand = state.close.cash_on_hand,
            remarks = state.close.cash_reconciliation_remarks,
            cogs = state.close.total_cogs_today ?: 0.0,
            margin = state.close.gross_margin_amount ?: 0.0,
            marginPct = state.close.gross_margin_percent,
            wagesToday = s.wagesTotalToday,
            unpaidOrders = unpaidRows,
            unpaidOrdersExtraCount = extra,
            outstandingPrintedTotal = listedTotal,
            printedBy = printedBy,
            printedAtMillis = System.currentTimeMillis(),
            closedBy = state.close.closed_by,
            closedAtMillis = state.close.closed_at,
        )
    }

    fun consumeEvent() {
        _events.value = null
    }
}
