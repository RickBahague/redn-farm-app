package com.redn.farm.ui.screens.order.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.Order
import com.redn.farm.data.model.OrderItem
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import com.redn.farm.utils.CurrencyFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import java.time.LocalDate
import com.redn.farm.utils.PrinterUtils
import androidx.compose.material.icons.filled.Print
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.BorderStroke

private fun formatDate(timestamp: Long): String {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    ).format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrderScreen(
    orderId: Int,
    onNavigateBack: () -> Unit,
    viewModel: OrderHistoryViewModel = viewModel(factory = OrderHistoryViewModel.Factory)
) {
    var order by remember { mutableStateOf<Order?>(null) }
    var orderItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var showEditItemDialog by remember { mutableStateOf<OrderItem?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showPaymentConfirmDialog by remember { mutableStateOf<Boolean?>(null) }
    var hasChanges by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteItemDialog by remember { mutableStateOf<OrderItem?>(null) }
    var showDeleteOrderDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(orderId) {
        viewModel.getOrder(orderId).collectLatest { loadedOrder ->
            order = loadedOrder
        }
    }

    LaunchedEffect(orderId) {
        viewModel.getOrderItems(orderId).collectLatest { items ->
            orderItems = items
        }
    }

    // Initialize printer
    LaunchedEffect(Unit) {
        // Remove this since PrinterUtils is an object
        // printer = PrinterUtil(context)
    }

    // Clean up printer on dispose
    DisposableEffect(Unit) {
        onDispose {
            // No need for this since PrinterUtils handles its own lifecycle
            // printer?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${if (order?.is_paid == true) "View" else "Edit"} Order #$orderId") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Delete button - only show for unpaid orders
                    if (order?.is_paid == false) {
                        IconButton(
                            onClick = { showDeleteOrderDialog = true }
                        ) {
                            Icon(Icons.Default.Delete, "Delete Order")
                        }
                    }
                    // Print button
                    IconButton(
                        onClick = {
                            order?.let { currentOrder ->
                                try {
                                    // Launch in a coroutine since printMessage is suspend
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val message = buildString {
                                            appendLine("REDN GREENS FRESH")
                                            appendLine("Order #${currentOrder.order_id}")
                                            appendLine("Date: ${formatDate(currentOrder.order_date)}")
                                            appendLine("Customer: ${currentOrder.customerName}")
                                            appendLine("Contact: ${currentOrder.customerContact}")
                                            appendLine("--------------------------------")
                                            
                                            orderItems.forEach { item ->
                                                appendLine("${item.product_name} - ${CurrencyFormatter.format(item.total_price)}")
                                                appendLine("${item.quantity}${if (item.is_per_kg) "kg" else "pc"} x ${CurrencyFormatter.format(item.price_per_unit)}")
                                            }
                                            
                                            appendLine("--------------------------------")
                                            appendLine("Total: ${CurrencyFormatter.format(currentOrder.total_amount)}")
                                            appendLine(if (currentOrder.is_paid) "PAID" else "UNPAID")
                                            appendLine("\n[ ] PREPARED FOR DELIVERY")
                                            appendLine("\n[ ] DELIVERED")
                                            appendLine("\nMarami pong salamat!")
                                            appendLine("\nvisit: https://www.facebook.com/redngreen03/")
                                            appendLine("Mobile: 0998.849.0469")
                                        }

                                        val success = PrinterUtils.printMessage(context, message)
                                        if (success) {
                                            Toast.makeText(context, "Print job sent successfully", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Printer error: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    e.printStackTrace()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Print, "Print Order")
                    }
                    if (hasChanges && order?.is_paid == false) {
                        TextButton(
                            onClick = {
                                order?.let { currentOrder ->
                                    viewModel.saveOrder(
                                        order = currentOrder,
                                        items = orderItems,
                                        onComplete = onNavigateBack
                                    )
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            order?.let { currentOrder ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Customer Info Card - 60% width
                    Card(
                        modifier = Modifier.weight(0.6f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentOrder.customerName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = currentOrder.customerContact,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = LocalDateTime.ofInstant(
                                        Instant.ofEpochMilli(currentOrder.order_date),
                                        ZoneId.systemDefault()
                                    ).format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!currentOrder.is_paid) {
                                    IconButton(
                                        onClick = { showDatePicker = true }
                                    ) {
                                        Icon(Icons.Default.Edit, "Edit Date")
                                    }
                                }
                            }
                        }
                    }

                    // Payment Status Card - 40% width
                    if (!currentOrder.is_paid) {
                        Card(
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Payment Status",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (currentOrder.is_paid) "Paid" else "Unpaid",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = currentOrder.is_paid,
                                        onCheckedChange = { newStatus -> 
                                            showPaymentConfirmDialog = newStatus
                                        }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Delivered",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = currentOrder.is_delivered,
                                        onCheckedChange = { newDeliveryStatus ->
                                            val updatedOrder = currentOrder.copy(
                                                is_delivered = newDeliveryStatus,
                                                order_update_date = System.currentTimeMillis()
                                            )
                                            order = updatedOrder
                                            hasChanges = true
                                            viewModel.updateOrderDeliveryStatus(orderId, newDeliveryStatus)
                                        },
                                        enabled = !currentOrder.is_paid
                                    )
                                }
                            }
                        }
                    }
                }

                // Order Items Card
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Order Items",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (!currentOrder.is_paid) {
                                IconButton(onClick = { showAddProductDialog = true }) {
                                    Icon(Icons.Default.Add, "Add Product")
                                }
                            }
                        }
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(orderItems) { item ->
                                OrderItemRow(
                                    item = item,
                                    onEditItem = { if (!currentOrder.is_paid) showEditItemDialog = it },
                                    onRemoveItem = { itemToRemove -> 
                                        if (!currentOrder.is_paid) {
                                            showDeleteItemDialog = itemToRemove
                                        }
                                    },
                                    isEditable = !currentOrder.is_paid
                                )
                            }
                        }

                        // Total amount at the bottom of items card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Amount",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = CurrencyFormatter.format(currentOrder.total_amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Dialogs remain unchanged
        order?.let { currentOrder ->
            if (!currentOrder.is_paid) {
                showEditItemDialog?.let { item ->
                    EditOrderItemDialog(
                        item = item,
                        onDismiss = { showEditItemDialog = null },
                        onSave = { updatedItem ->
                            orderItems = orderItems.map { 
                                if (it.id == updatedItem.id) updatedItem else it 
                            }
                            hasChanges = true
                            showEditItemDialog = null
                        },
                        viewModel = viewModel
                    )
                }

                if (showAddProductDialog) {
                    ProductSelectionDialog(
                        onDismiss = { showAddProductDialog = false },
                        onProductSelected = { product, quantity, isPerKg ->
                            val productPrice = viewModel.getLatestProductPrice(product.product_id)
                            val unitPrice = if (isPerKg) {
                                productPrice?.per_kg_price
                            } else {
                                productPrice?.per_piece_price
                            } ?: 0.0
                            
                            val newItem = OrderItem(
                                order_id = orderId,
                                product_id = product.product_id,
                                product_name = product.product_name,
                                quantity = quantity,
                                price_per_unit = unitPrice,
                                is_per_kg = isPerKg,
                                total_price = quantity * unitPrice
                            )
                            
                            // Update order total
                            order?.let { currentOrder ->
                                val newTotal = currentOrder.total_amount + newItem.total_price
                                order = currentOrder.copy(total_amount = newTotal)
                            }
                            
                            orderItems = orderItems + newItem
                            hasChanges = true
                            showAddProductDialog = false
                        },
                        viewModel = viewModel
                    )
                }
            }

            showPaymentConfirmDialog?.let { newStatus ->
                AlertDialog(
                    onDismissRequest = { showPaymentConfirmDialog = null },
                    title = { Text("Confirm Payment Status Change") },
                    text = { 
                        Text(
                            "Are you sure you want to mark this order as ${if (newStatus) "paid" else "unpaid"}? " +
                            if (newStatus) "This action cannot be undone." else ""
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                order?.let { currentOrder ->
                                    val updatedOrder = currentOrder.copy(
                                        is_paid = newStatus,
                                        order_update_date = System.currentTimeMillis()
                                    )
                                    order = updatedOrder
                                    hasChanges = true
                                    viewModel.updatePaymentStatus(orderId, newStatus)
                                }
                                showPaymentConfirmDialog = null
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showPaymentConfirmDialog = null }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    onDateSelected = { selectedDate ->
                        val updatedOrder = currentOrder.copy(
                            order_date = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            order_update_date = System.currentTimeMillis()
                        )
                        order = updatedOrder
                        hasChanges = true
                        viewModel.updateOrderDate(orderId, selectedDate.atStartOfDay())
                        showDatePicker = false
                    },
                    initialDate = LocalDate.ofInstant(
                        Instant.ofEpochMilli(currentOrder.order_date),
                        ZoneId.systemDefault()
                    )
                )
            }

            // Add delete item confirmation dialog
            showDeleteItemDialog?.let { itemToDelete ->
                AlertDialog(
                    onDismissRequest = { showDeleteItemDialog = null },
                    title = { Text("Delete Item") },
                    text = { 
                        Text("Are you sure you want to delete ${itemToDelete.product_name} from the order?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                orderItems = orderItems - itemToDelete
                                // Update order total by subtracting the removed item's total
                                order = order?.copy(
                                    total_amount = (order?.total_amount ?: 0.0) - itemToDelete.total_price
                                )
                                hasChanges = true
                                showDeleteItemDialog = null
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteItemDialog = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Add delete order confirmation dialog
            if (showDeleteOrderDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteOrderDialog = false },
                    title = { Text("Delete Order") },
                    text = { Text("Are you sure you want to delete this order? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteOrder(orderId)
                                showDeleteOrderDialog = false
                                onNavigateBack()
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteOrderDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Date") },
        text = {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                modifier = Modifier.padding(16.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedDate)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerInfoCard(
    order: Order,
    viewModel: OrderHistoryViewModel,
    onOrderUpdated: (Order) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var isDelivered by remember(order.is_delivered) { mutableStateOf(order.is_delivered) }
    val orderDate = remember(order.order_date) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(order.order_date),
            ZoneId.systemDefault()
        )
    }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Customer Information",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = order.customerName)
            Text(text = order.customerContact)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Order Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Date: ${orderDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!order.is_paid) {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.Edit, "Edit Date")
                    }
                }
            }

            // Delivery Status Switch
            if (!order.is_paid) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delivered")
                    Switch(
                        checked = isDelivered,
                        onCheckedChange = { newDeliveryStatus ->
                            isDelivered = newDeliveryStatus
                            val updatedOrder = order.copy(
                                is_delivered = newDeliveryStatus,
                                order_update_date = System.currentTimeMillis()
                            )
                            viewModel.updateOrderDeliveryStatus(order.order_id, newDeliveryStatus)
                            onOrderUpdated(updatedOrder)
                        }
                    )
                }
            } else {
                // Show delivery status as text for paid orders
                Text(
                    text = if (isDelivered) "Delivered" else "Pending Delivery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDelivered) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                val updatedOrder = order.copy(
                    order_date = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    order_update_date = System.currentTimeMillis()
                )
                onOrderUpdated(updatedOrder)
                viewModel.updateOrderDate(
                    orderId = order.order_id,
                    newDate = selectedDate.atStartOfDay()
                )
                showDatePicker = false
            },
            initialDate = orderDate.toLocalDate()
        )
    }
}

@Composable
private fun PaymentStatusSwitch(
    isPaid: Boolean,
    onStatusChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Payment Status",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isPaid) "Paid" else "Unpaid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPaid) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = isPaid,
                onCheckedChange = onStatusChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.error,
                    uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    }
}

@Composable
private fun OrderItemsCard(
    items: List<OrderItem>, 
    onEditItem: (OrderItem) -> Unit, 
    onRemoveItem: (OrderItem) -> Unit,
    isEditable: Boolean
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Order Items",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(items) { item ->
                    OrderItemRow(
                        item = item, 
                        onEditItem = onEditItem, 
                        onRemoveItem = onRemoveItem,
                        isEditable = isEditable
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(
    item: OrderItem,
    onEditItem: (OrderItem) -> Unit,
    onRemoveItem: (OrderItem) -> Unit,
    isEditable: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.product_name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Qty: ${item.quantity}${if (item.is_per_kg) "kg" else "pc"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "@${CurrencyFormatter.format(item.price_per_unit)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = CurrencyFormatter.format(item.total_price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isEditable) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onEditItem(item) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onRemoveItem(item) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalAmountCard(
    total: Double,
    items: List<OrderItem>
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Statistics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Products: ${items.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Total Kg: ${
                                String.format(Locale.US, "%.2f", items
                                    .filter { it.is_per_kg }
                                    .sumOf { it.quantity }
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Total Pcs: ${
                                String.format(Locale.US, "%.0f", items
                                    .filter { !it.is_per_kg }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditOrderItemDialog(
    item: OrderItem,
    onDismiss: () -> Unit,
    onSave: (OrderItem) -> Unit,
    viewModel: OrderHistoryViewModel
) {
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    var isPerKg by remember { mutableStateOf(item.is_per_kg) }
    var useDiscountedPrice by remember { mutableStateOf(false) }
    val productPrice = viewModel.getLatestProductPrice(item.product_id)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Product Name
                Text(
                    text = item.product_name,
                    style = MaterialTheme.typography.titleMedium
                )

                // Regular Prices Section
                Text(
                    text = "Regular Prices",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Per Kg: ${CurrencyFormatter.format(productPrice?.per_kg_price ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Per Piece: ${CurrencyFormatter.format(productPrice?.per_piece_price ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Discounted Prices Section
                if (productPrice?.discounted_per_kg_price != null || productPrice?.discounted_per_piece_price != null) {
                    Text(
                        text = "Discounted Prices",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Per Kg: ${CurrencyFormatter.format(productPrice.discounted_per_kg_price ?: 0.0)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Per Piece: ${CurrencyFormatter.format(productPrice.discounted_per_piece_price ?: 0.0)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            quantity = it
                        }
                    },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Price Type Selection
                if (productPrice?.let { price ->
                    (isPerKg && price.discounted_per_kg_price != null) ||
                    (!isPerKg && price.discounted_per_piece_price != null)
                } == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use Discounted Price")
                        Switch(
                            checked = useDiscountedPrice,
                            onCheckedChange = { useDiscountedPrice = it }
                        )
                    }
                }

                // Per kg/piece switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isPerKg) "Per Kilogram" else "Per Piece")
                    Switch(
                        checked = isPerKg,
                        onCheckedChange = { isPerKg = it }
                    )
                }

                // Show current price based on selection
                val currentUnitPrice = if (isPerKg) {
                    if (useDiscountedPrice) productPrice?.discounted_per_kg_price else productPrice?.per_kg_price
                } else {
                    if (useDiscountedPrice) productPrice?.discounted_per_piece_price else productPrice?.per_piece_price
                } ?: 0.0

                quantity.toDoubleOrNull()?.let { qty ->
                    val total = qty * currentUnitPrice
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Unit Price: ${CurrencyFormatter.format(currentUnitPrice)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (useDiscountedPrice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total: ${CurrencyFormatter.format(total)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    quantity.toDoubleOrNull()?.let { newQuantity ->
                        val newUnitPrice = if (isPerKg) {
                            if (useDiscountedPrice) productPrice?.discounted_per_kg_price else productPrice?.per_kg_price
                        } else {
                            if (useDiscountedPrice) productPrice?.discounted_per_piece_price else productPrice?.per_piece_price
                        } ?: 0.0
                        
                        onSave(item.copy(
                            quantity = newQuantity,
                            is_per_kg = isPerKg,
                            price_per_unit = newUnitPrice,
                            total_price = newQuantity * newUnitPrice
                        ))
                    }
                },
                enabled = quantity.isNotEmpty() && quantity.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSelectionDialog(
    onDismiss: () -> Unit,
    onProductSelected: (Product, Double, Boolean) -> Unit,
    viewModel: OrderHistoryViewModel
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf("") }
    var isPerKg by remember { mutableStateOf(true) }
    var useDiscountedPrice by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val products by viewModel.products.collectAsState()
    val isWideScreen = LocalConfiguration.current.screenWidthDp > 600

    // Filter products based on search query
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { product ->
                product.product_name.contains(searchQuery, ignoreCase = true) ||
                product.product_description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedProduct == null) "Select Product" else "Add Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedProduct == null) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search Products") },
                        leadingIcon = { Icon(Icons.Default.Search, "Search") },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "Clear search")
                                }
                            }
                        } else null
                    )

                    // Product list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(filteredProducts) { product ->
                            val productPrice = viewModel.getLatestProductPrice(product.product_id)
                            ListItem(
                                headlineContent = { Text(product.product_name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProduct = product }
                            )
                        }
                    }
                } else {
                    // Product details and quantity input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedProduct?.let { product ->
                            Text(
                                text = product.product_name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            val productPrice = viewModel.getLatestProductPrice(product.product_id)

                            // Regular Prices Section
                            Text(
                                text = "Regular Prices",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Per Kg: ${CurrencyFormatter.format(productPrice?.per_kg_price ?: 0.0)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Per Piece: ${CurrencyFormatter.format(productPrice?.per_piece_price ?: 0.0)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Discounted Prices Section
                            if (productPrice?.discounted_per_kg_price != null || productPrice?.discounted_per_piece_price != null) {
                                Text(
                                    text = "Discounted Prices",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Per Kg: ${CurrencyFormatter.format(productPrice?.discounted_per_kg_price ?: 0.0)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Per Piece: ${CurrencyFormatter.format(productPrice?.discounted_per_piece_price ?: 0.0)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { 
                                    if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                        quantity = it
                                    }
                                },
                                label = { Text("Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Price Type Selection
                            if (productPrice?.let { price ->
                                (isPerKg && price.discounted_per_kg_price != null) ||
                                (!isPerKg && price.discounted_per_piece_price != null)
                            } == true) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Use Discounted Price")
                                    Switch(
                                        checked = useDiscountedPrice,
                                        onCheckedChange = { useDiscountedPrice = it }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isPerKg) "Per Kilogram" else "Per Piece")
                                Switch(
                                    checked = isPerKg,
                                    onCheckedChange = { isPerKg = it }
                                )
                            }

                            // Show total based on current selection
                            quantity.toDoubleOrNull()?.let { qty ->
                                val currentUnitPrice = if (isPerKg) {
                                    if (useDiscountedPrice) productPrice?.discounted_per_kg_price else productPrice?.per_kg_price
                                } else {
                                    if (useDiscountedPrice) productPrice?.discounted_per_piece_price else productPrice?.per_piece_price
                                } ?: 0.0

                                val total = qty * currentUnitPrice
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Unit Price: ${CurrencyFormatter.format(currentUnitPrice)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (useDiscountedPrice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Total: ${CurrencyFormatter.format(total)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedProduct?.let { product ->
                        quantity.toDoubleOrNull()?.let { qty ->
                            val productPrice = viewModel.getLatestProductPrice(product.product_id)
                            val currentUnitPrice = if (isPerKg) {
                                if (useDiscountedPrice) productPrice?.discounted_per_kg_price else productPrice?.per_kg_price
                            } else {
                                if (useDiscountedPrice) productPrice?.discounted_per_piece_price else productPrice?.per_piece_price
                            } ?: 0.0

                            onProductSelected(product, qty, isPerKg)
                        }
                    }
                },
                enabled = selectedProduct != null && quantity.isNotEmpty() && quantity.toDoubleOrNull() != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (selectedProduct == null) {
                        onDismiss()
                    } else {
                        selectedProduct = null
                        quantity = ""
                    }
                }
            ) {
                Text(if (selectedProduct == null) "Cancel" else "Back")
            }
        }
    )
} 