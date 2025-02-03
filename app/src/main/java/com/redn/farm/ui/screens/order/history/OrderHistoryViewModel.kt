package com.redn.farm.ui.screens.order.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.model.Order
import com.redn.farm.data.model.OrderItem
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.repository.OrderRepository
import com.redn.farm.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class OrderHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FarmDatabase.getDatabase(application)
    private val orderRepository = OrderRepository(database.orderDao())
    private val productRepository = ProductRepository(
        database.productDao(),
        database.productPriceDao()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<LocalDateTime?, LocalDateTime?>>(null to null)
    val dateRange = _dateRange.asStateFlow()

    val products = productRepository.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val orders = combine(
        orderRepository.getAllOrders(),
        searchQuery,
        dateRange
    ) { orders, query, dateRange ->
        orders.filter { order ->
            val matchesSearch = order.customerName.contains(query, ignoreCase = true) ||
                              order.customerContact.contains(query, ignoreCase = true) ||
                              order.order_id.toString().contains(query)
            
            val orderDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(order.order_date),
                ZoneId.systemDefault()
            )
            val matchesDateRange = isWithinDateRange(orderDateTime, dateRange)
            
            matchesSearch && matchesDateRange
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _productPrices = MutableStateFlow<Map<String, ProductPrice>>(emptyMap())
    val productPrices = _productPrices.asStateFlow()

    data class OrderSummary(
        val productSummaries: List<ProductSummary>,
        val uniqueProductCount: Int,
        val uniqueCustomerCount: Int,
        val totalAmount: Double
    )

    data class ProductSummary(
        val productName: String,
        val totalQuantity: Double,
        val isPerKg: Boolean
    )

    private val _orderSummary = MutableStateFlow<OrderSummary?>(null)
    val orderSummary = _orderSummary.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.getAllProductPrices().collect { prices ->
                _productPrices.value = prices.associateBy { it.product_id }
            }
        }
    }

    fun getLatestProductPrice(productId: String): ProductPrice? {
        return _productPrices.value[productId]
    }

    fun updatePaymentStatus(orderId: Int, isPaid: Boolean) {
        viewModelScope.launch {
            val currentOrder = orderRepository.getOrderById(orderId).first()
            currentOrder?.let {
                val updatedOrder = it.copy(
                    is_paid = isPaid,
                    order_update_date = System.currentTimeMillis()
                )
                val currentItems = orderRepository.getOrderItems(orderId).first()
                updateOrder(updatedOrder, currentItems)
            }
        }
    }

    fun deleteOrder(orderId: Int) {
        viewModelScope.launch {
            orderRepository.deleteOrder(orderId)
        }
    }

    fun getOrder(orderId: Int): Flow<Order?> {
        return orderRepository.getOrderById(orderId)
    }

    fun getOrderItems(orderId: Int): Flow<List<OrderItem>> {
        return orderRepository.getOrderItems(orderId)
    }

    suspend fun updateOrder(order: Order, items: List<OrderItem>) {
        orderRepository.updateOrder(order, items)
    }

    fun saveOrder(order: Order, items: List<OrderItem>, onComplete: () -> Unit) {
        viewModelScope.launch {
            updateOrder(order, items)
            onComplete()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateDateRange(range: Pair<LocalDateTime?, LocalDateTime?>) {
        _dateRange.value = range
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

    fun updateOrderDate(orderId: Int, newDate: LocalDateTime) {
        viewModelScope.launch {
            val currentOrder = orderRepository.getOrderById(orderId).first()
            currentOrder?.let {
                val updatedOrder = it.copy(
                    order_date = newDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
                val currentItems = orderRepository.getOrderItems(orderId).first()
                updateOrder(updatedOrder, currentItems)
            }
        }
    }

    fun updateOrderDeliveryStatus(orderId: Int, isDelivered: Boolean) {
        viewModelScope.launch {
            val currentOrder = orderRepository.getOrderById(orderId).first()
            currentOrder?.let {
                val updatedOrder = it.copy(
                    is_delivered = isDelivered,
                    order_update_date = System.currentTimeMillis()
                )
                val currentItems = orderRepository.getOrderItems(orderId).first()
                orderRepository.updateOrder(updatedOrder, currentItems)
            }
        }
    }

    fun calculateSummary() {
        viewModelScope.launch {
            val currentOrders = orders.value
            val orderItems = currentOrders.map { order ->
                orderRepository.getOrderItems(order.order_id).first()
            }.flatten()

            // Calculate product summaries
            val productQuantities = orderItems.groupBy { it.product_id }
                .mapValues { (_, items) ->
                    val firstItem = items.first()
                    ProductSummary(
                        productName = firstItem.product_name,
                        totalQuantity = items.sumOf { it.quantity },
                        isPerKg = firstItem.is_per_kg
                    )
                }

            // Calculate unique customers and total amount
            val uniqueCustomers = currentOrders.map { it.customer_id }.distinct().size
            val totalAmount = currentOrders.sumOf { it.total_amount }

            _orderSummary.value = OrderSummary(
                productSummaries = productQuantities.values.toList(),
                uniqueProductCount = productQuantities.size,
                uniqueCustomerCount = uniqueCustomers,
                totalAmount = totalAmount
            )
        }
    }

    fun clearSummary() {
        _orderSummary.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as Application)
                OrderHistoryViewModel(application)
            }
        }
    }
} 