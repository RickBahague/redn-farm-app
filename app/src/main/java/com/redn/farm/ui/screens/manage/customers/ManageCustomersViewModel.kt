package com.redn.farm.ui.screens.manage.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.security.Rbac
import com.redn.farm.data.model.Customer
import com.redn.farm.data.repository.CustomerRepository
import com.redn.farm.data.repository.OrderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ManageCustomersViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val database = FarmDatabase.getDatabase(application)
    private val repository = CustomerRepository(database.customerDao())
    private val orderRepository = OrderRepository(database.orderDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    val customers = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllCustomers()
            } else {
                repository.searchCustomers(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                if (!Rbac.canWriteCustomers(sessionManager.getRole())) {
                    _uiState.value = UiState.Error("You don't have permission to add customers.")
                    return@launch
                }
                _uiState.value = UiState.Loading
                repository.addCustomer(customer)
                _uiState.value = UiState.Success("Customer added successfully")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to add customer")
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                if (!Rbac.canWriteCustomers(sessionManager.getRole())) {
                    _uiState.value = UiState.Error("You don't have permission to update customers.")
                    return@launch
                }
                _uiState.value = UiState.Loading
                repository.updateCustomer(customer)
                _uiState.value = UiState.Success("Customer updated successfully")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update customer")
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                if (!Rbac.canWriteCustomers(sessionManager.getRole())) {
                    _uiState.value = UiState.Error("You don't have permission to delete customers.")
                    return@launch
                }
                _uiState.value = UiState.Loading

                // Check if customer has any orders
                val customerOrders = orderRepository.getAllOrders().first()
                    .filter { it.customer_id == customer.customer_id }
                
                if (customerOrders.isNotEmpty()) {
                    _uiState.value = UiState.Error(
                        "Cannot delete customer. They have existing orders. " +
                        "Please delete or reassign their orders first."
                    )
                    return@launch
                }
                
                repository.deleteCustomer(customer)
                _uiState.value = UiState.Success("Customer deleted successfully")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.message ?: "Failed to delete customer. They may have existing orders."
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = UiState.Idle
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as Application)
                ManageCustomersViewModel(application)
            }
        }
    }
} 