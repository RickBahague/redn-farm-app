package com.redn.farm.ui.screens.order

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.model.*
import com.redn.farm.data.pricing.OrderPricingResolver
import com.redn.farm.data.pricing.SalesChannel
import com.redn.farm.data.pricing.defaultOrderChannel
import com.redn.farm.data.repository.AcquisitionRepository
import com.redn.farm.data.repository.CustomerRepository
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.repository.OrderRepository
import com.redn.farm.data.repository.PricingPresetRepository
import com.redn.farm.data.repository.ProductRepository
import com.redn.farm.security.Rbac
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TakeOrderViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val database = FarmDatabase.getDatabase(application)
    private val customerRepository = CustomerRepository(database.customerDao())
    private val productRepository = ProductRepository(database.productDao(), database.productPriceDao())
    private val orderRepository = OrderRepository(database.orderDao())
    private val acquisitionRepository = AcquisitionRepository(
        database.acquisitionDao(),
        PricingPresetRepository(database.pricingPresetDao(), database.presetActivationLogDao()),
        database.productDao()
    )

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _channel = MutableStateFlow(SalesChannel.OFFLINE)
    val channel: StateFlow<String> = _channel.asStateFlow()

    private val _activeSrpsByProduct = MutableStateFlow<Map<String, Acquisition>>(emptyMap())
    val activeSrpsByProduct: StateFlow<Map<String, Acquisition>> = _activeSrpsByProduct.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems = _cartItems.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _orderPlaced = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val orderPlaced: SharedFlow<Unit> = _orderPlaced.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val cartTotal = cartItems.map { items ->
        items.sumOf { it.total }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val customers = combine(
        customerRepository.getAllCustomers(),
        orderRepository.getAllOrders(),
        searchQuery
    ) { customers, orders, query ->
        if (query.isEmpty()) {
            customers
        } else {
            val orderNumber = query.toIntOrNull()
            if (orderNumber != null) {
                val matchingOrder = orders.find { it.order_id == orderNumber }
                if (matchingOrder != null) {
                    customers.filter { it.customer_id == matchingOrder.customer_id }
                } else {
                    customers.filter { customer ->
                        customer.fullName.contains(query, ignoreCase = true) ||
                            customer.contact.contains(query)
                    }
                }
            } else {
                customers.filter { customer ->
                    customer.fullName.contains(query, ignoreCase = true) ||
                        customer.contact.contains(query)
                }
            }
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

    private val _productPrices = MutableStateFlow<Map<String, ProductPrice>>(emptyMap())
    val productPrices = _productPrices.asStateFlow()

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

    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
        setChannel(customer.customer_type.defaultOrderChannel())
    }

    fun setChannel(newChannel: String) {
        val normalized = SalesChannel.normalize(newChannel)
        if (_channel.value == normalized) return
        _channel.value = normalized
        repriceCart()
    }

    fun getLatestProductPrice(productId: String): ProductPrice? =
        _productPrices.value[productId]

    fun resolvePreviewUnitPrice(productId: String, isPerKg: Boolean): Double {
        val acq = _activeSrpsByProduct.value[productId]
        val pp = getLatestProductPrice(productId)
        return OrderPricingResolver.resolveUnitPrice(acq, _channel.value, isPerKg, pp)
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: Product, quantity: Double, isPerKg: Boolean) {
        val price = resolvePreviewUnitPrice(product.product_id, isPerKg)
        val cartItem = CartItem(
            product = product,
            quantity = quantity,
            isPerKg = isPerKg,
            price = price
        )
        _cartItems.value = _cartItems.value + cartItem
    }

    fun updateQuantity(productId: String, newQuantity: Double) {
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.product_id == productId) {
                item.copy(quantity = newQuantity)
            } else {
                item
            }
        }
    }

    fun toggleCartItemUnit(productId: String) {
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.product_id != productId) item
            else {
                val newPerKg = !item.isPerKg
                val price = resolvePreviewUnitPrice(productId, newPerKg)
                item.copy(isPerKg = newPerKg, price = price)
            }
        }
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.product.product_id == productId }
    }

    private fun repriceCart() {
        val ch = _channel.value
        val map = _activeSrpsByProduct.value
        _cartItems.value = _cartItems.value.map { item ->
            val pp = getLatestProductPrice(item.product.product_id)
            val acq = map[item.product.product_id]
            val price = OrderPricingResolver.resolveUnitPrice(acq, ch, item.isPerKg, pp)
            item.copy(price = price)
        }
    }

    fun placeOrder() {
        viewModelScope.launch {
            if (!Rbac.canWriteOrders(sessionManager.getRole())) {
                _userMessage.emit("You don't have permission to place orders.")
                return@launch
            }
            val customer = _selectedCustomer.value ?: return@launch
            val items = _cartItems.value
            val order = Order(
                customer_id = customer.customer_id,
                channel = _channel.value,
                customerName = "${customer.firstname} ${customer.lastname}",
                customerContact = customer.contact,
                total_amount = items.sumOf { it.total }
            )
            val orderItems = items.map { cartItem ->
                OrderItem(
                    product_id = cartItem.product.product_id,
                    product_name = cartItem.product.product_name,
                    quantity = cartItem.quantity,
                    price_per_unit = cartItem.price,
                    is_per_kg = cartItem.isPerKg,
                    total_price = cartItem.total
                )
            }
            orderRepository.createOrder(order, orderItems)
            _cartItems.value = emptyList()
            _selectedCustomer.value = null
            _channel.value = SalesChannel.OFFLINE
            _orderPlaced.emit(Unit)
        }
    }

    fun resetOrder() {
        _selectedCustomer.value = null
        _cartItems.value = emptyList()
        _channel.value = SalesChannel.OFFLINE
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                TakeOrderViewModel(application)
            }
        }
    }
}
