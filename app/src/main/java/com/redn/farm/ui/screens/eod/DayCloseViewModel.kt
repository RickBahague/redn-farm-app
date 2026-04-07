package com.redn.farm.ui.screens.eod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.entity.DayCloseEntity
import com.redn.farm.data.local.entity.DayCloseInventoryEntity
import com.redn.farm.data.repository.DayCloseRepository
import com.redn.farm.data.util.DateWindowHelper
import com.redn.farm.security.Rbac
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI state ──────────────────────────────────────────────────────────────

sealed class DayCloseUiState {
    object Loading : DayCloseUiState()
    data class Ready(
        val close: DayCloseEntity,
        val inventoryLines: List<DayCloseInventoryEntity>,
        val canEditInventoryCounts: Boolean,
        val showNegativeMarginWarning: Boolean = false,
    ) : DayCloseUiState()
    data class Error(val message: String) : DayCloseUiState()
}

sealed class DayCloseEvent {
    data class ShowSnackbar(val message: String) : DayCloseEvent()
    object ConfirmNegativeMargin : DayCloseEvent()
    object Finalized : DayCloseEvent()
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
                val close = repo.openOrCreate(businessDateMillis, username)
                val updated = repo.computeAndSaveRevenueCogs(close)
                val lines = repo.getInventoryLines(updated.close_id)
                val canEdit = Rbac.canEditInventoryCounts(role)
                _uiState.value = DayCloseUiState.Ready(
                    close = updated,
                    inventoryLines = lines,
                    canEditInventoryCounts = canEdit,
                )
            } catch (e: Exception) {
                _uiState.value = DayCloseUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Rebuild the inventory snapshot from aggregated data. */
    fun refreshInventorySnapshot(username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        viewModelScope.launch {
            try {
                val lines = repo.buildInventorySnapshot(
                    closeId = state.close.close_id,
                    businessDateMillis = state.close.business_date,
                    username = username,
                )
                _uiState.value = state.copy(inventoryLines = lines)
            } catch (e: Exception) {
                _events.value = DayCloseEvent.ShowSnackbar("Failed to refresh inventory: ${e.message}")
            }
        }
    }

    /** Update the physical count for a single inventory line. */
    fun enterActualCount(lineIndex: Int, actualKg: Double) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        val updated = state.inventoryLines.toMutableList()
        val line = updated[lineIndex]
        val varianceQty = actualKg - line.adjusted_theoretical_remaining
        val varianceCost = varianceQty * line.weighted_avg_cost_per_unit
        updated[lineIndex] = line.copy(
            actual_remaining = actualKg,
            variance_qty = varianceQty,
            variance_cost = varianceCost,
        )
        _uiState.value = state.copy(inventoryLines = updated)
        viewModelScope.launch {
            repo.getInventoryLines(state.close.close_id) // persist via update
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

    /** Request finalization. Shows confirmation dialog if margin is negative (D4). */
    fun requestFinalize(username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        val margin = state.close.gross_margin_amount
        if (margin != null && margin < 0) {
            _events.value = DayCloseEvent.ConfirmNegativeMargin
            return
        }
        doFinalize(username)
    }

    /** Called after user confirms negative margin dialog. */
    fun confirmFinalizeWithNegativeMargin(username: String) {
        doFinalize(username)
    }

    private fun doFinalize(username: String) {
        val state = _uiState.value as? DayCloseUiState.Ready ?: return
        viewModelScope.launch {
            try {
                val finalized = repo.finalize(state.close, username)
                _uiState.value = state.copy(close = finalized)
                _events.value = DayCloseEvent.Finalized
            } catch (e: Exception) {
                _events.value = DayCloseEvent.ShowSnackbar("Finalize failed: ${e.message}")
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
