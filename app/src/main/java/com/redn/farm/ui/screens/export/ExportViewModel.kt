package com.redn.farm.ui.screens.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.export.CsvExportService
import com.redn.farm.data.local.dao.UserDao
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.security.Rbac
import com.redn.farm.data.repository.*
import com.redn.farm.data.util.DatabasePopulator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val csvExportService: CsvExportService,
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val employeePaymentRepository: EmployeePaymentRepository,
    private val employeeRepository: EmployeeRepository,
    private val farmOperationRepository: FarmOperationRepository,
    private val productRepository: ProductRepository,
    private val remittanceRepository: RemittanceRepository,
    private val pricingPresetRepository: PricingPresetRepository,
    private val acquisitionRepository: AcquisitionRepository,
    private val userDao: UserDao,
    @ApplicationContext appContext: Context
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        viewModelScope.launch {
            _isAdmin.value = resolveAdmin()
        }
    }

    private suspend fun resolveAdmin(): Boolean {
        if (Rbac.canExport(sessionManager.getRole())) return true
        val username = sessionManager.getUsername() ?: return false
        return Rbac.canExport(userDao.getUserByUsername(username)?.role)
    }

    private fun guardExport(): Boolean {
        if (Rbac.canExport(sessionManager.getRole())) return true
        _exportState.value = ExportState.Error("You don't have permission to export or modify data here.")
        return false
    }

    private fun sharedExportTimestamp(): String =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState = _exportState.asStateFlow()

    fun exportUsers() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                val users = userDao.getAllUsers().first()
                val file = csvExportService.exportUsers(users)
                _exportState.value = ExportState.Success(file = file, message = "Users")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export users")
            }
        }
    }

    /**
     * EXP-US-01 batch export: one CSV per selected table, same timestamp suffix on all files in the batch.
     */
    fun exportSelectedBundle(selected: Set<ExportBundleTable>) {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            if (selected.isEmpty()) {
                _exportState.value = ExportState.Error("Select at least one table")
                return@launch
            }
            try {
                _exportState.value = ExportState.Loading
                val ts = sharedExportTimestamp()
                val files = mutableListOf<File>()
                if (ExportBundleTable.USERS in selected) {
                    files.add(csvExportService.exportUsers(userDao.getAllUsers().first(), ts))
                }
                if (ExportBundleTable.PRODUCTS in selected) {
                    files.add(csvExportService.exportProducts(productRepository.getAllProducts().first(), ts))
                }
                if (ExportBundleTable.CUSTOMERS in selected) {
                    files.add(csvExportService.exportCustomers(customerRepository.getAllCustomers().first(), ts))
                }
                if (ExportBundleTable.ORDERS in selected) {
                    files.add(csvExportService.exportOrders(orderRepository.getAllOrders().first(), ts))
                }
                if (ExportBundleTable.ORDER_ITEMS in selected) {
                    files.add(csvExportService.exportOrderItems(orderRepository.getAllOrderItems().first(), ts))
                }
                if (ExportBundleTable.EMPLOYEES in selected) {
                    files.add(csvExportService.exportEmployees(employeeRepository.getAllEmployees().first(), ts))
                }
                if (ExportBundleTable.EMPLOYEE_PAYMENTS in selected) {
                    files.add(
                        csvExportService.exportEmployeePayments(
                            employeePaymentRepository.getAllPayments().first(),
                            ts
                        )
                    )
                }
                if (ExportBundleTable.ACQUISITIONS in selected) {
                    files.add(
                        csvExportService.exportAcquisitions(
                            acquisitionRepository.getAllAcquisitions().first(),
                            ts
                        )
                    )
                }
                if (ExportBundleTable.FARM_OPERATIONS in selected) {
                    files.add(
                        csvExportService.exportFarmOperations(
                            farmOperationRepository.getAllOperations().first(),
                            ts
                        )
                    )
                }
                if (ExportBundleTable.REMITTANCES in selected) {
                    files.add(
                        csvExportService.exportRemittances(
                            remittanceRepository.getAllRemittances().first(),
                            ts
                        )
                    )
                }
                _exportState.value = ExportState.Success(
                    files = files,
                    message = "Exported ${files.size} CSV file(s) (timestamp $ts)"
                )
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Batch export failed")
            }
        }
    }

    fun exportCustomers() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                val customers = customerRepository.getAllCustomers().first()
                val file = csvExportService.exportCustomers(customers)
                _exportState.value = ExportState.Success(file = file, message = "Customers")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export customers")
            }
        }
    }
    
    fun exportOrders() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                val orders = orderRepository.getAllOrders().first()
                val file = csvExportService.exportOrders(orders)
                _exportState.value = ExportState.Success(file = file, message = "Orders")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export orders")
            }
        }
    }
    
    fun exportOrderItems() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                
                val orderItems = orderRepository.getAllOrderItems().first()
                val file = csvExportService.exportOrderItems(orderItems)
                _exportState.value = ExportState.Success(file = file, message = "Order items")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export order items")
            }
        }
    }
    
    fun exportEmployeePayments() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                
                val payments = employeePaymentRepository.getAllPayments().first()
                val file = csvExportService.exportEmployeePayments(payments)
                _exportState.value = ExportState.Success(file = file, message = "Employee payments")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export employee payments")
            }
        }
    }
    
    fun exportEmployees() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                val employees = employeeRepository.getAllEmployees().first()
                val file = csvExportService.exportEmployees(employees)
                _exportState.value = ExportState.Success(file = file, message = "Employees")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export employees")
            }
        }
    }
    
    fun exportFarmOperations() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                
                val operations = farmOperationRepository.getAllOperations().first()
                val file = csvExportService.exportFarmOperations(operations)
                _exportState.value = ExportState.Success(file = file, message = "Farm operations")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export farm operations")
            }
        }
    }
    
    fun exportProductPrices() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                
                val prices = productRepository.getAllProductPrices().first()
                val file = csvExportService.exportProductPrices(prices)
                _exportState.value = ExportState.Success(file = file, message = "Product prices")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export product prices")
            }
        }
    }
    
    fun exportProducts() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                
                val products = productRepository.getAllProducts().first()
                val file = csvExportService.exportProducts(products)
                _exportState.value = ExportState.Success(file = file, message = "Products")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export products")
            }
        }
    }
    
    fun exportRemittances() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                
                val remittances = remittanceRepository.getAllRemittances().first()
                val file = csvExportService.exportRemittances(remittances)
                _exportState.value = ExportState.Success(file = file, message = "Remittances")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export remittances")
            }
        }
    }
    
    fun exportAcquisitions() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                
                val acquisitions = acquisitionRepository.getAllAcquisitions().first()
                val file = csvExportService.exportAcquisitions(acquisitions)
                _exportState.value = ExportState.Success(file = file, message = "Acquisitions")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Failed to export acquisitions")
            }
        }
    }
    
    fun generateSampleData() {
        viewModelScope.launch {
            if (!guardExport()) return@launch
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
            if (!guardExport()) return@launch
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
            if (!guardExport()) return@launch
            try {
                _exportState.value = ExportState.Loading
                DatabasePopulator.populateAcquisitions(
                    acquisitionRepository = acquisitionRepository,
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

    /**
     * EXP-US-02 / Stream C: clears [expanded] in FK-safe order (independent of checkbox order).
     * Caller should merge dependency tables (e.g. Acquisitions when Products selected) before calling.
     */
    fun clearSelectedTables(expanded: Set<ClearableTable>) {
        viewModelScope.launch {
            if (!guardExport()) return@launch
            if (expanded.isEmpty()) {
                _exportState.value = ExportState.Error("Select at least one table to clear")
                return@launch
            }
            try {
                _exportState.value = ExportState.Loading
                if (ClearableTable.ORDERS in expanded) orderRepository.truncate()
                if (ClearableTable.CUSTOMERS in expanded) customerRepository.truncate()
                if (ClearableTable.EMPLOYEE_PAYMENTS in expanded) employeePaymentRepository.truncate()
                if (ClearableTable.EMPLOYEES in expanded) employeeRepository.truncate()
                if (ClearableTable.ACQUISITIONS in expanded) acquisitionRepository.truncate()
                when {
                    ClearableTable.PRODUCTS in expanded -> productRepository.truncate()
                    ClearableTable.PRODUCT_PRICES in expanded -> productRepository.truncateProductPrices()
                }
                if (ClearableTable.FARM_OPERATIONS in expanded) farmOperationRepository.truncate()
                if (ClearableTable.REMITTANCES in expanded) remittanceRepository.truncate()
                if (ClearableTable.PRICING_PRESETS in expanded) {
                    pricingPresetRepository.truncatePresetsAndActivationLog()
                }
                if (ClearableTable.USERS_NON_SEED in expanded) userDao.deleteNonSeedUsers()
                val summary = expanded
                    .sortedBy { it.ordinal }
                    .joinToString("\n") { "• ${it.label}" }
                _exportState.value = ExportState.Success(
                    message = "Cleared selected tables:\n$summary",
                )
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Clear failed")
            }
        }
    }
    
    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class Success(
            val file: File? = null,
            val files: List<File>? = null,
            val message: String? = null
        ) : ExportState()
        data class Error(val message: String) : ExportState()
    }
} 