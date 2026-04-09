package com.redn.farm.ui.screens.order

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.CartItem
import com.redn.farm.data.pricing.SalesChannel
import com.redn.farm.utils.CurrencyFormatter
import java.util.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeOrderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrderHistory: () -> Unit,
    onNavigateToActiveSrps: () -> Unit = {},
    viewModel: TakeOrderViewModel = hiltViewModel()
) {
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showNewOrderDialog by remember { mutableStateOf(false) }
    
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val channel by viewModel.channel.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        launch {
            viewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
        }
        launch {
            viewModel.orderPlaced.collectLatest { showNewOrderDialog = true }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Take Order") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            viewModel.placeOrder()
                        },
                        enabled = selectedCustomer != null && cartItems.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .semantics { contentDescription = "Place order" }
                    ) {
                        Text(
                            text = "Order",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    IconButton(onClick = onNavigateToActiveSrps) {
                        Icon(Icons.Filled.AttachMoney, "Active SRPs")
                    }
                    IconButton(onClick = onNavigateToOrderHistory) {
                        Icon(Icons.Default.History, "Order History")
                    }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = CurrencyFormatter.format(cartTotal),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Customer Selection
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showCustomerDialog = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Customer",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = selectedCustomer?.fullName ?: "Select Customer",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (selectedCustomer != null) {
                        Text(
                            text = selectedCustomer?.contact ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sales channel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SalesChannel.ALL.forEach { key ->
                    FilterChip(
                        selected = channel == key,
                        onClick = { viewModel.setChannel(key) },
                        label = { Text(SalesChannel.label(key)) },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Product Button
            OutlinedButton(
                onClick = { showProductDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, "Add Product")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Product")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cart Items
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    OrderItemCard(
                        orderItem = item,
                        onQuantityChange = { newQty ->
                            viewModel.updateQuantity(item.product.product_id, newQty)
                        },
                        onRemove = {
                            viewModel.removeFromCart(item.product.product_id)
                        },
                        showUnitToggle = viewModel.productSupportsDualUnit(item.product),
                        onToggleUnit = { viewModel.toggleCartItemUnit(item.product.product_id) }
                    )
                }
            }

            // Order summary with statistics
            OrderSummaryCard(
                items = cartItems,
                total = cartTotal
            )
        }

        if (showCustomerDialog) {
            CustomerSelectionDialog(
                onDismiss = { showCustomerDialog = false },
                onCustomerSelected = { customer ->
                    viewModel.selectCustomer(customer)
                    showCustomerDialog = false
                }
            )
        }

        if (showProductDialog) {
            ProductSelectionDialog(
                onDismiss = { showProductDialog = false },
                onProductSelected = { product, quantity, isPerKg ->
                    viewModel.addToCart(product, quantity, isPerKg)
                    showProductDialog = false
                }
            )
        }

        if (showNewOrderDialog) {
            AlertDialog(
                onDismissRequest = { /* Do nothing, force user to choose */ },
                title = { Text("Order Placed Successfully") },
                text = { Text("Would you like to enter another order?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showNewOrderDialog = false
                            viewModel.resetOrder()
                        }
                    ) {
                        Text("Yes, New Order")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNewOrderDialog = false
                            onNavigateToOrderHistory()
                        }
                    ) {
                        Text("No, View Orders")
                    }
                }
            )
        }
    }
}

@Composable
private fun OrderSummaryCard(
    items: List<CartItem>,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Summary statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Products: ${items.size}",  // Unique products count
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Total Kg: ${
                                String.format(Locale.US, "%.2f", items
                                    .filter { it.isPerKg }
                                    .sumOf { it.quantity }
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Total Pcs: ${
                                String.format(Locale.US, "%.0f", items
                                    .filter { !it.isPerKg }
                                    .sumOf { it.quantity }
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Text(
                    text = CurrencyFormatter.format(total),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
} 