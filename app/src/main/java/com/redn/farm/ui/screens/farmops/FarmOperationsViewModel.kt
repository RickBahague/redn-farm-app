package com.redn.farm.ui.screens.farmops

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.data.model.FarmOperationType
import com.redn.farm.data.model.Product
import com.redn.farm.data.repository.FarmOperationRepository
import com.redn.farm.data.repository.ProductRepository
import com.redn.farm.security.Rbac
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import com.redn.farm.utils.MillisDateRange
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FarmOperationsViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val repository: FarmOperationRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow<FarmOperationType?>(null)
    val selectedType = _selectedType.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
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
            val matchesDateRange = MillisDateRange.contains(operation.operation_date, dateRange)
            
            matchesSearch && matchesType && matchesDateRange
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.getAllProducts().collect { list -> _products.value = list }
        }
    }

    /** Re-read DB when opening the related-product picker (BUG-FOP-02 — sheet/flow timing). */
    fun refreshProductListFromDb() {
        viewModelScope.launch {
            _products.value = productRepository.getAllProducts().first()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedType(type: FarmOperationType?) {
        _selectedType.value = type
    }

    fun updateDateRange(range: Pair<Long?, Long?>) {
        _dateRange.value = range
    }

    fun addOperation(operation: FarmOperation) {
        viewModelScope.launch {
            if (!Rbac.canWriteFarmOperations(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to add farm operations.")
                return@launch
            }
            repository.addOperation(operation)
        }
    }

    fun updateOperation(operation: FarmOperation) {
        viewModelScope.launch {
            if (!Rbac.canWriteFarmOperations(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to update farm operations.")
                return@launch
            }
            repository.updateOperation(operation)
        }
    }

    fun deleteOperation(operation: FarmOperation) {
        viewModelScope.launch {
            if (!Rbac.canWriteFarmOperations(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to delete farm operations.")
                return@launch
            }
            repository.deleteOperation(operation)
        }
    }

    /** BUG-FOP-04: pre-fill **Personnel** on new log operation from session. */
    fun loggedInUsernameOrEmpty(): String = sessionManager.getUsername().orEmpty()
} 