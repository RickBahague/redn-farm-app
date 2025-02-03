package com.redn.farm.ui.screens.farmops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.data.model.FarmOperationType
import com.redn.farm.data.repository.FarmOperationRepository
import com.redn.farm.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class FarmOperationsViewModel @Inject constructor(
    private val repository: FarmOperationRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow<FarmOperationType?>(null)
    val selectedType = _selectedType.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<LocalDateTime?, LocalDateTime?>>(null to null)
    val dateRange = _dateRange.asStateFlow()

    val operations = combine(
        repository.getAllOperations(),
        searchQuery,
        selectedType,
        dateRange
    ) { operations, query, type, dateRange ->
        operations.filter { operation ->
            val matchesSearch = operation.details.contains(query, ignoreCase = true) ||
                              operation.personnel.contains(query, ignoreCase = true)
            val matchesType = type == null || operation.operation_type == type
            val matchesDateRange = isWithinDateRange(operation.operation_date, dateRange)
            
            matchesSearch && matchesType && matchesDateRange
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val products = productRepository.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedType(type: FarmOperationType?) {
        _selectedType.value = type
    }

    fun updateDateRange(range: Pair<LocalDateTime?, LocalDateTime?>) {
        _dateRange.value = range
    }

    fun addOperation(operation: FarmOperation) {
        viewModelScope.launch {
            repository.addOperation(operation)
        }
    }

    fun updateOperation(operation: FarmOperation) {
        viewModelScope.launch {
            repository.updateOperation(operation)
        }
    }

    fun deleteOperation(operation: FarmOperation) {
        viewModelScope.launch {
            repository.deleteOperation(operation)
        }
    }

    private fun isWithinDateRange(
        date: LocalDateTime,
        range: Pair<LocalDateTime?, LocalDateTime?>
    ): Boolean {
        val (start, end) = range
        return when {
            start == null && end == null -> true
            start == null -> date.isBefore(end!!.plusDays(1))
            end == null -> date.isAfter(start.minusDays(1))
            else -> date.isAfter(start.minusDays(1)) && date.isBefore(end.plusDays(1))
        }
    }
} 