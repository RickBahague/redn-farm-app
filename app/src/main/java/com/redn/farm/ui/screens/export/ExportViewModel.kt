package com.redn.farm.ui.screens.export

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.export.CsvExportService
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.repository.*
import com.redn.farm.data.util.DatabasePopulator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class ExportViewModel(application: Application) : AndroidViewModel(application) {
    private val csvExportService = CsvExportService(application)
    private val database = FarmDatabase.getDatabase(application)
    private val orderRepository = OrderRepository(database.orderDao())
    private val customerRepository = CustomerRepository(database.customerDao())
    private val employeePaymentRepository = EmployeePaymentRepository(database.employeePaymentDao())
    private val employeeRepository = EmployeeRepository(database.employeeDao())
    private val farmOperationRepository = FarmOperationRepository(application, database, database.farmOperationDao())
    private val productRepository = ProductRepository(database.productDao(), database.productPriceDao())
    private val remittanceRepository = RemittanceRepository(database.remittanceDao())
    private val acquisitionRepository = AcquisitionRepository(database.acquisitionDao())
    
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState = _exportState.asStateFlow()
    
    fun exportCustomers() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                val customers = customerRepository.getAllCustomers().first()
                val file = csvExportService.exportCustomers(customers)
                _exportState.value = ExportState.Success(file = file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export customers")
            }
        }
    }
    
    fun exportOrders() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                val orders = orderRepository.getAllOrders().first()
                val file = csvExportService.exportOrders(orders)
                _exportState.value = ExportState.Success(file = file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export orders")
            }
        }
    }
    
    fun exportOrderItems() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                
                val orderItems = orderRepository.getAllOrderItems().first()
                val file = csvExportService.exportOrderItems(orderItems)
                
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export order items")
            }
        }
    }
    
    fun exportEmployeePayments() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                
                val payments = employeePaymentRepository.getAllPayments().first()
                val file = csvExportService.exportEmployeePayments(payments)
                
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export employee payments")
            }
        }
    }
    
    fun exportEmployees() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                val employees = employeeRepository.getAllEmployees().first()
                val file = csvExportService.exportEmployees(employees)
                _exportState.value = ExportState.Success(file = file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export employees")
            }
        }
    }
    
    fun exportFarmOperations() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                
                val operations = farmOperationRepository.getAllOperations().first()
                val file = csvExportService.exportFarmOperations(operations)
                
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export farm operations")
            }
        }
    }
    
    fun exportProductPrices() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                
                val prices = productRepository.getAllProductPrices().first()
                val file = csvExportService.exportProductPrices(prices)
                
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export product prices")
            }
        }
    }
    
    fun exportProducts() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                
                val products = productRepository.getAllProducts().first()
                val file = csvExportService.exportProducts(products)
                
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export products")
            }
        }
    }
    
    fun exportRemittances() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                
                val remittances = remittanceRepository.getAllRemittances().first()
                val file = csvExportService.exportRemittances(remittances)
                
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export remittances")
            }
        }
    }
    
    fun exportAcquisitions() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                
                val acquisitions = acquisitionRepository.getAllAcquisitions().first()
                val file = csvExportService.exportAcquisitions(acquisitions)
                
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export acquisitions")
            }
        }
    }
    
    fun generateSampleData() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                DatabasePopulator.populateCustomers(customerRepository, count = 20)
                _exportState.value = ExportState.Success(message = "Generated 20 sample customers successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to generate sample data")
            }
        }
    }
    
    fun generateSampleProducts() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                DatabasePopulator.populateProducts(productRepository, count = 20)
                _exportState.value = ExportState.Success(message = "Generated 20 sample products successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to generate sample products")
            }
        }
    }
    
    fun generateSampleAcquisitions() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading
                DatabasePopulator.populateAcquisitions(
                    acquisitionRepository = AcquisitionRepository(database.acquisitionDao()),
                    productRepository = productRepository,
                    count = 30
                )
                _exportState.value = ExportState.Success(message = "Generated 30 sample acquisitions successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to generate sample acquisitions")
            }
        }
    }
    
    fun dismissMessage() {
        _exportState.value = ExportState.Idle
    }
    
    fun truncateCustomers() {
        viewModelScope.launch {
            try {
                customerRepository.truncate()
                _exportState.value = ExportState.Success(message = "Customers data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear customers: ${e.message}")
            }
        }
    }

    fun truncateEmployees() {
        viewModelScope.launch {
            try {
                employeeRepository.truncate()
                _exportState.value = ExportState.Success(message = "Employees data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear employees: ${e.message}")
            }
        }
    }

    fun truncateOrders() {
        viewModelScope.launch {
            try {
                orderRepository.truncate()
                _exportState.value = ExportState.Success(message = "Orders data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear orders: ${e.message}")
            }
        }
    }

    fun truncateOrderItems() {
        viewModelScope.launch {
            try {
                orderRepository.truncate()
                _exportState.value = ExportState.Success(message = "Order items data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear order items: ${e.message}")
            }
        }
    }

    fun truncateFarmOperations() {
        viewModelScope.launch {
            try {
                farmOperationRepository.truncate()
                _exportState.value = ExportState.Success(message = "Farm operations data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear farm operations: ${e.message}")
            }
        }
    }

    fun truncateProducts() {
        viewModelScope.launch {
            try {
                productRepository.truncate()
                _exportState.value = ExportState.Success(message = "Products data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear products: ${e.message}")
            }
        }
    }

    fun truncateProductPrices() {
        viewModelScope.launch {
            try {
                productRepository.truncate()
                _exportState.value = ExportState.Success(message = "Product prices data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear product prices: ${e.message}")
            }
        }
    }

    fun truncateEmployeePayments() {
        viewModelScope.launch {
            try {
                employeePaymentRepository.truncate()
                _exportState.value = ExportState.Success(message = "Employee payments data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear employee payments: ${e.message}")
            }
        }
    }

    fun truncateRemittances() {
        viewModelScope.launch {
            try {
                remittanceRepository.truncate()
                _exportState.value = ExportState.Success(message = "Remittances data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear remittances: ${e.message}")
            }
        }
    }

    fun truncateAcquisitions() {
        viewModelScope.launch {
            try {
                acquisitionRepository.truncate()
                _exportState.value = ExportState.Success(message = "Acquisitions data cleared successfully")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Failed to clear acquisitions: ${e.message}")
            }
        }
    }
    
    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class Success(val file: File? = null, val message: String? = null) : ExportState()
        data class Error(val message: String) : ExportState()
    }
    
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ExportViewModel(
                    application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                )
            }
        }
    }
} 