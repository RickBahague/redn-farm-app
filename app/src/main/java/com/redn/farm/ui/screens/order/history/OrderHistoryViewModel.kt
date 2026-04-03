package com.redn.farm.ui.screens.order.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.security.Rbac
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.Customer
import com.redn.farm.data.model.Order
import com.redn.farm.data.model.OrderItem
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.pricing.OrderPricingResolver
import com.redn.farm.data.repository.AcquisitionRepository
import com.redn.farm.data.repository.CustomerRepository
import com.redn.farm.data.repository.OrderRepository
import com.redn.farm.data.repository.PricingPresetRepository
import com.redn.farm.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.redn.farm.utils.MillisDateRange
import kotlinx.coroutines.launch

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val acquisitionRepository: AcquisitionRepository
) : ViewModel() {

    private val sessionManager = SessionManager(appContext)

    private val _activeSrpsByProduct = MutableStateFlow<Map<String, Acquisition>>(emptyMap())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
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
            
            val matchesDateRange = MillisDateRange.contains(order.order_date, dateRange)
            
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
        viewModelScope.launch {
            acquisitionRepository.observeAllActiveSrps().collect { list ->
                _activeSrpsByProduct.value = list.associateBy { it.product_id }
            }
        }
    }

    suspend fun getCustomer(customerId: Int): Customer? =
        customerRepository.getCustomerById(customerId)

    fun resolveOrderLinePrice(productId: String, channel: String, isPerKg: Boolean): Double {
        val acq = _activeSrpsByProduct.value[productId]
        val pp = getLatestProductPrice(productId)
        return OrderPricingResolver.resolveUnitPrice(acq, channel, isPerKg, pp)
    }

    fun repriceOrderItems(items: List<OrderItem>, channel: String): List<OrderItem> {
        val map = _activeSrpsByProduct.value
        return items.map { item ->
            val pp = getLatestProductPrice(item.product_id)
            val acq = map[item.product_id]
            val price = OrderPricingResolver.resolveUnitPrice(acq, channel, item.is_per_kg, pp)
            item.copy(price_per_unit = price, total_price = price * item.quantity)
        }
    }

    fun productSupportsDualUnit(product: Product): Boolean {
        val pp = getLatestProductPrice(product.product_id) ?: return false
        val hasKg = (pp.per_kg_price ?: 0.0) > 0 || (pp.discounted_per_kg_price != null && pp.discounted_per_kg_price!! > 0)
        val hasPc = (pp.per_piece_price ?: 0.0) > 0 || (pp.discounted_per_piece_price != null && pp.discounted_per_piece_price!! > 0)
        val acq = _activeSrpsByProduct.value[product.product_id]
        val srpKg = acq?.let {
            listOfNotNull(it.srp_online_per_kg, it.srp_reseller_per_kg, it.srp_offline_per_kg).any { v -> v > 0 }
        } ?: false
        val srpPc = acq?.let {
            listOfNotNull(it.srp_online_per_piece, it.srp_reseller_per_piece, it.srp_offline_per_piece).any { v -> v > 0 }
        } ?: false
        return (hasKg && hasPc) || (srpKg && srpPc) || (hasKg && srpPc) || (hasPc && srpKg)
    }

    fun defaultIsPerKgForProduct(product: Product): Boolean =
        !product.unit_type.equals("piece", ignoreCase = true) &&
            !product.unit_type.equals("pieces", ignoreCase = true)

    fun getLatestProductPrice(productId: String): ProductPrice? {
        return _productPrices.value[productId]
    }

    fun updatePaymentStatus(orderId: Int, isPaid: Boolean) {
        viewModelScope.launch {
            if (!Rbac.canWriteOrders(sessionManager.getRole())) return@launch
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
            if (!Rbac.canWriteOrders(sessionManager.getRole())) return@launch
            orderRepository.deleteOrder(orderId)
        }
    }

    fun getOrder(orderId: Int): Flow<Order?> {
        return orderRepository.getOrderById(orderId)
    }

    fun getOrderItems(orderId: Int): Flow<List<OrderItem>> {
        return orderRepository.getOrderItems(orderId)
    }

    suspend fun getOrderSnapshotForPrint(orderId: Int): Pair<Order?, List<OrderItem>> {
        val order = orderRepository.getOrderById(orderId).first()
        val items = orderRepository.getOrderItems(orderId).first()
        return order to items
    }

    suspend fun updateOrder(order: Order, items: List<OrderItem>) {
        if (!Rbac.canWriteOrders(sessionManager.getRole())) return
        orderRepository.updateOrder(order, items)
    }

    fun saveOrder(order: Order, items: List<OrderItem>, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (!Rbac.canWriteOrders(sessionManager.getRole())) return@launch
            updateOrder(order, items)
            onComplete()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateDateRange(range: Pair<Long?, Long?>) {
        _dateRange.value = range
    }

    fun updateOrderDate(orderId: Int, orderDateMillis: Long) {
        viewModelScope.launch {
            if (!Rbac.canWriteOrders(sessionManager.getRole())) return@launch
            val currentOrder = orderRepository.getOrderById(orderId).first()
            currentOrder?.let {
                val updatedOrder = it.copy(
                    order_date = orderDateMillis
                )
                val currentItems = orderRepository.getOrderItems(orderId).first()
                updateOrder(updatedOrder, currentItems)
            }
        }
    }

    fun updateOrderDeliveryStatus(orderId: Int, isDelivered: Boolean) {
        viewModelScope.launch {
            if (!Rbac.canWriteOrders(sessionManager.getRole())) return@launch
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
} 