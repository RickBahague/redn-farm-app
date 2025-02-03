package com.redn.farm.ui.screens.order

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.model.*
import com.redn.farm.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class TakeOrderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FarmDatabase.getDatabase(application)
    private val customerRepository = CustomerRepository(database.customerDao())
    private val productRepository = ProductRepository(database.productDao(), database.productPriceDao())
    private val orderRepository = OrderRepository(database.orderDao())

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems = _cartItems.asStateFlow()

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
            // Try to parse query as order number
            val orderNumber = query.toIntOrNull()
            if (orderNumber != null) {
                // If query is a number, find matching order and its customer
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
                // If query is not a number, search by name or contact
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
    }

    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
    }

    fun getLatestProductPrice(productId: String): ProductPrice? {
        return _productPrices.value[productId]
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: Product, quantity: Double, isPerKg: Boolean) {
        val productPrice = getLatestProductPrice(product.product_id)
        val price = if (isPerKg) {
            productPrice?.per_kg_price
        } else {
            productPrice?.per_piece_price
        } ?: 0.0

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

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.product.product_id == productId }
    }

    fun placeOrder() {
        viewModelScope.launch {
            val customer = _selectedCustomer.value ?: return@launch
            val items = _cartItems.value
            
            val order = Order(
                customer_id = customer.customer_id,
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
            
            // Clear cart after successful order
            _cartItems.value = emptyList()
            _selectedCustomer.value = null
        }
    }

    fun resetOrder() {
        _selectedCustomer.value = null
        _cartItems.value = emptyList()
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