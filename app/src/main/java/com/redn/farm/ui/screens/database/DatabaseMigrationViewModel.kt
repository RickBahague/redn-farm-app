package com.redn.farm.ui.screens.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.model.*
import com.redn.farm.data.local.entity.*
import com.redn.farm.data.local.dao.OrderItemWithProduct
import com.redn.farm.data.local.dao.EmployeePaymentWithEmployee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

sealed class DatabaseMigrationState {
    object Checking : DatabaseMigrationState()
    object ExistingDatabaseFound : DatabaseMigrationState()
    object CreatingNewDatabase : DatabaseMigrationState()
    object Ready : DatabaseMigrationState()
    data class Error(val message: String) : DatabaseMigrationState()
}

data class AllData(
    val products: List<Product>,
    val productPrices: List<ProductPrice>,
    val customers: List<Customer>,
    val orders: List<Order>,
    val orderItems: List<OrderItem>,
    val employees: List<Employee>,
    val employeePayments: List<EmployeePayment>,
    val farmOperations: List<FarmOperation>,
    val acquisitions: List<Acquisition>,
    val remittances: List<Remittance>
)

class DatabaseMigrationViewModel(application: Application) : AndroidViewModel(application) {
    private val _migrationState = MutableStateFlow<DatabaseMigrationState>(DatabaseMigrationState.Checking)
    val migrationState: StateFlow<DatabaseMigrationState> = _migrationState
    private var database: FarmDatabase? = null

    fun checkExistingDatabase() {
        viewModelScope.launch {
            try {
                _migrationState.value = DatabaseMigrationState.Checking
                
                // Check if database file exists
                val dbFile = getApplication<Application>().getDatabasePath(FarmDatabase.DATABASE_NAME)
                
                if (dbFile.exists()) {
                    database = FarmDatabase.getDatabase(getApplication())
                    _migrationState.value = DatabaseMigrationState.ExistingDatabaseFound
                } else {
                    createNewDatabase()
                }
            } catch (e: Exception) {
                _migrationState.value = DatabaseMigrationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun proceedWithExistingDatabase() {
        viewModelScope.launch {
            try {
                // Initialize the database without replacing the existing one
                database = database ?: FarmDatabase.getDatabase(getApplication())
                _migrationState.value = DatabaseMigrationState.Ready
            } catch (e: Exception) {
                _migrationState.value = DatabaseMigrationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun createNewDatabase() {
        viewModelScope.launch {
            try {
                _migrationState.value = DatabaseMigrationState.CreatingNewDatabase
                // Create and initialize a new database
                database = FarmDatabase.getDatabase(getApplication())
                _migrationState.value = DatabaseMigrationState.Ready
            } catch (e: Exception) {
                _migrationState.value = DatabaseMigrationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun getAllData(): AllData? {
        return database?.let { db ->
            AllData(
                products = db.productDao().getAllProducts().first().map { entity ->
                    Product(
                        product_id = entity.product_id,
                        product_name = entity.product_name,
                        product_description = entity.product_description,
                        unit_type = entity.unit_type,
                        is_active = entity.is_active,
                        category = entity.category,
                        defaultPieceCount = entity.default_piece_count
                    )
                },
                productPrices = db.productPriceDao().getAllProductPrices().first().map { entity ->
                    ProductPrice(
                        price_id = entity.price_id,
                        product_id = entity.product_id,
                        per_kg_price = entity.per_kg_price,
                        per_piece_price = entity.per_piece_price,
                        discounted_per_kg_price = entity.discounted_per_kg_price,
                        discounted_per_piece_price = entity.discounted_per_piece_price,
                        date_created = entity.date_created
                    )
                },
                customers = db.customerDao().getAllCustomers().first().map { entity ->
                    Customer(
                        customer_id = entity.customer_id,
                        firstname = entity.firstname,
                        lastname = entity.lastname,
                        contact = entity.contact,
                        customer_type = entity.customer_type,
                        address = entity.address,
                        city = entity.city,
                        province = entity.province,
                        postal_code = entity.postal_code,
                        date_created = entity.date_created,
                        date_updated = entity.date_updated
                    )
                },
                orders = db.orderDao().getAllOrders().first().map { orderWithDetails ->
                    Order(
                        order_id = orderWithDetails.order.order_id,
                        customer_id = orderWithDetails.order.customer_id,
                        channel = orderWithDetails.order.channel,
                        customerName = orderWithDetails.customerName,
                        customerContact = orderWithDetails.customerContact,
                        total_amount = orderWithDetails.order.total_amount,
                        order_date = orderWithDetails.order.order_date,
                        order_update_date = orderWithDetails.order.order_update_date,
                        is_paid = orderWithDetails.order.is_paid,
                        is_delivered = orderWithDetails.order.is_delivered
                    )
                },
                orderItems = db.orderDao().getAllOrderItems().first().map { orderItemWithProduct ->
                    OrderItem(
                        id = orderItemWithProduct.orderItem.id,
                        order_id = orderItemWithProduct.orderItem.order_id,
                        product_id = orderItemWithProduct.orderItem.product_id,
                        product_name = orderItemWithProduct.product_name,
                        quantity = orderItemWithProduct.orderItem.quantity,
                        price_per_unit = orderItemWithProduct.orderItem.price_per_unit,
                        is_per_kg = orderItemWithProduct.orderItem.is_per_kg,
                        total_price = orderItemWithProduct.orderItem.total_price
                    )
                },
                employees = db.employeeDao().getAllEmployees().first().map { entity ->
                    Employee(
                        employee_id = entity.employee_id,
                        firstname = entity.firstname,
                        lastname = entity.lastname,
                        contact = entity.contact,
                        date_created = entity.date_created,
                        date_updated = entity.date_updated
                    )
                },
                employeePayments = db.employeePaymentDao().getAllPayments().first().map { paymentWithEmployee ->
                    EmployeePayment(
                        payment_id = paymentWithEmployee.payment.payment_id,
                        employee_id = paymentWithEmployee.payment.employee_id,
                        employeeName = paymentWithEmployee.employeeName,
                        amount = paymentWithEmployee.payment.amount,
                        cash_advance_amount = paymentWithEmployee.payment.cash_advance_amount,
                        liquidated_amount = paymentWithEmployee.payment.liquidated_amount,
                        date_paid = paymentWithEmployee.payment.date_paid,
                        signature = paymentWithEmployee.payment.signature,
                        received_date = paymentWithEmployee.payment.received_date
                    )
                },
                farmOperations = db.farmOperationDao().getAllOperations().first().map { entity ->
                    FarmOperation(
                        operation_id = entity.operation_id,
                        operation_type = entity.operation_type,
                        operation_date = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(entity.operation_date),
                            ZoneId.systemDefault()
                        ),
                        details = entity.details,
                        area = entity.area,
                        weather_condition = entity.weather_condition,
                        personnel = entity.personnel,
                        product_id = entity.product_id,
                        product_name = entity.product_name,
                        date_created = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(entity.date_created),
                            ZoneId.systemDefault()
                        ),
                        date_updated = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(entity.date_updated),
                            ZoneId.systemDefault()
                        )
                    )
                },
                acquisitions = db.acquisitionDao().getAllAcquisitions().first().map { entity ->
                    Acquisition(
                        acquisition_id = entity.acquisition_id,
                        product_id = entity.product_id,
                        product_name = entity.product_name,
                        quantity = entity.quantity,
                        price_per_unit = entity.price_per_unit,
                        total_amount = entity.total_amount,
                        is_per_kg = entity.is_per_kg,
                        piece_count = entity.piece_count,
                        date_acquired = entity.date_acquired,
                        created_at = entity.created_at,
                        location = entity.location,
                        preset_ref = entity.preset_ref,
                        spoilage_rate = entity.spoilage_rate,
                        additional_cost_per_kg = entity.additional_cost_per_kg,
                        hauling_weight_kg = entity.hauling_weight_kg,
                        hauling_fees_json = entity.hauling_fees_json,
                        channels_snapshot_json = entity.channels_snapshot_json,
                        srp_online_per_kg = entity.srp_online_per_kg,
                        srp_reseller_per_kg = entity.srp_reseller_per_kg,
                        srp_offline_per_kg = entity.srp_offline_per_kg,
                        srp_online_500g = entity.srp_online_500g,
                        srp_online_250g = entity.srp_online_250g,
                        srp_online_100g = entity.srp_online_100g,
                        srp_reseller_500g = entity.srp_reseller_500g,
                        srp_reseller_250g = entity.srp_reseller_250g,
                        srp_reseller_100g = entity.srp_reseller_100g,
                        srp_offline_500g = entity.srp_offline_500g,
                        srp_offline_250g = entity.srp_offline_250g,
                        srp_offline_100g = entity.srp_offline_100g,
                        srp_online_per_piece = entity.srp_online_per_piece,
                        srp_reseller_per_piece = entity.srp_reseller_per_piece,
                        srp_offline_per_piece = entity.srp_offline_per_piece
                    )
                },
                remittances = db.remittanceDao().getAllRemittances().first().map { entity ->
                    Remittance(
                        remittance_id = entity.remittance_id,
                        amount = entity.amount,
                        date = entity.date,
                        remarks = entity.remarks,
                        date_updated = entity.date_updated
                    )
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                DatabaseMigrationViewModel(application)
            }
        }
    }
} 