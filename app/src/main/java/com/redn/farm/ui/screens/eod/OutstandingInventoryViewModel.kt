package com.redn.farm.ui.screens.eod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.dao.ProductDao
import com.redn.farm.data.repository.DayCloseRepository
import com.redn.farm.utils.buildOutstandingInventoryReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OutstandingInventoryUiState {
    object Loading : OutstandingInventoryUiState()
    data class Ready(
        val allLineCount: Int,
        val filteredLines: List<DayCloseRepository.OutstandingProductLine>,
        val totalValue: Double,
        val searchQuery: String,
        val category: String?,
        val atRiskOnly: Boolean,
        val categories: List<String>,
    ) : OutstandingInventoryUiState()

    data class Error(val message: String) : OutstandingInventoryUiState()
}

@HiltViewModel
class OutstandingInventoryViewModel @Inject constructor(
    private val repo: DayCloseRepository,
    private val productDao: ProductDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OutstandingInventoryUiState>(OutstandingInventoryUiState.Loading)
    val uiState: StateFlow<OutstandingInventoryUiState> = _uiState.asStateFlow()

    private var allLines: List<DayCloseRepository.OutstandingProductLine> = emptyList()
    private var categoryByProduct: Map<String, String?> = emptyMap()
    private var categories: List<String> = emptyList()

    private var searchQuery: String = ""
    private var category: String? = null
    private var atRiskOnly: Boolean = false

    private fun recomputeUi() {
        val query = searchQuery.trim().lowercase()
        val filtered = allLines.filter { line ->
            if (query.isNotEmpty() && !line.productName.lowercase().contains(query)) return@filter false
            if (category != null) {
                val pc = categoryByProduct[line.productId]?.trim()?.takeIf { it.isNotEmpty() } ?: return@filter false
                if (!pc.equals(category, ignoreCase = true)) return@filter false
            }
            if (atRiskOnly && line.agingFlag == DayCloseRepository.AgingFlag.NORMAL) return@filter false
            true
        }
        val total = filtered.sumOf { it.displayValuePhp }
        _uiState.value = OutstandingInventoryUiState.Ready(
            allLineCount = allLines.size,
            filteredLines = filtered,
            totalValue = total,
            searchQuery = searchQuery,
            category = category,
            atRiskOnly = atRiskOnly,
            categories = categories,
        )
    }

    fun setSearchQuery(q: String) {
        searchQuery = q
        if (_uiState.value is OutstandingInventoryUiState.Ready || allLines.isNotEmpty()) recomputeUi()
    }

    fun setCategory(c: String?) {
        category = c
        if (allLines.isNotEmpty()) recomputeUi()
    }

    fun setAtRiskOnly(v: Boolean) {
        atRiskOnly = v
        if (allLines.isNotEmpty()) recomputeUi()
    }

    fun load(businessDateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _uiState.value = OutstandingInventoryUiState.Loading
            try {
                allLines = repo.buildOutstandingInventory(businessDateMillis)
                val products = productDao.getAllProductsList()
                categoryByProduct = products.associate { it.product_id to it.category }
                categories = products.mapNotNull { it.category?.trim()?.takeIf { c -> c.isNotEmpty() } }
                    .distinct()
                    .sorted()
                recomputeUi()
            } catch (e: Exception) {
                _uiState.value = OutstandingInventoryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun buildPrintText(printedBy: String): String? {
        val state = _uiState.value as? OutstandingInventoryUiState.Ready ?: return null
        val lines = state.filteredLines.map { line ->
            Triple(line.productName, line.totalRemainingKg, line.daysOnHand)
        }
        val suffix = when {
            state.searchQuery.isNotBlank() -> "filtered"
            state.category != null -> state.category
            state.atRiskOnly -> "at-risk"
            else -> null
        }
        return buildOutstandingInventoryReport(
            lines = lines,
            totalValue = state.totalValue,
            printedBy = printedBy,
            titleExtra = suffix,
        )
    }
}
