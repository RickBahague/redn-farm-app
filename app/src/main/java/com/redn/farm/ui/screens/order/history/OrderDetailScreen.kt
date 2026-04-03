package com.redn.farm.ui.screens.order.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Customer
import com.redn.farm.data.model.Order
import com.redn.farm.data.model.OrderItem
import com.redn.farm.data.model.isOrderFinalized
import com.redn.farm.data.pricing.SalesChannel
import com.redn.farm.utils.buildOrderReceiptText
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun formatDate(timestamp: Long): String {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    ).format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    var order by remember { mutableStateOf<Order?>(null) }
    var orderItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var customer by remember { mutableStateOf<Customer?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(orderId) {
        viewModel.getOrder(orderId).collectLatest { loaded: Order? ->
            order = loaded
        }
    }
    LaunchedEffect(orderId) {
        viewModel.getOrderItems(orderId).collectLatest { items: List<OrderItem> ->
            orderItems = items
        }
    }
    LaunchedEffect(order?.customer_id) {
        val cid = order?.customer_id
        customer = if (cid != null) {
            withContext(Dispatchers.IO) {
                viewModel.getCustomer(cid)
            }
        } else {
            null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Order #$orderId") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            order?.let { currentOrder ->
                                scope.launch {
                                    val message = buildOrderReceiptText(currentOrder, orderItems)
                                    val ok = PrinterUtils.printMessage(context, message, alignment = 0)
                                    snackbarHostState.showSnackbar(
                                        if (ok) "Sent to printer" else "Print failed — check printer"
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Print, "Print")
                    }
                    if (order?.isOrderFinalized != true) {
                        IconButton(onClick = onNavigateToEdit) {
                            Icon(Icons.Default.Edit, "Edit order")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val o = order
        if (o == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Placed: ${formatDate(o.order_date)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Updated: ${formatDate(o.order_update_date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Channel: ${SalesChannel.label(SalesChannel.normalize(o.channel))}", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(o.customerName, style = MaterialTheme.typography.titleLarge)
                    Text(o.customerContact, style = MaterialTheme.typography.bodyLarge)
                    customer?.let { c ->
                        Text(
                            "Customer type: ${c.customer_type}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paid")
                        Switch(
                            checked = o.is_paid,
                            onCheckedChange = { newPaid ->
                                val previous = o.is_paid
                                viewModel.updatePaymentStatus(orderId, newPaid)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = if (newPaid) "Marked as paid" else "Marked as unpaid",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.updatePaymentStatus(orderId, previous)
                                    }
                                }
                            }
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Delivered")
                        Switch(
                            checked = o.is_delivered,
                            onCheckedChange = { newDel ->
                                val previous = o.is_delivered
                                viewModel.updateOrderDeliveryStatus(orderId, newDel)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = if (newDel) "Marked as delivered" else "Marked as not delivered",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.updateOrderDeliveryStatus(orderId, previous)
                                    }
                                }
                            }
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Items", style = MaterialTheme.typography.titleMedium)
                }
                items(orderItems, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.product_name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${item.quantity} ${if (item.is_per_kg) "kg" else "pcs"} × ${CurrencyFormatter.format(item.price_per_unit)}${if (item.is_per_kg) "/kg" else "/pc"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                CurrencyFormatter.format(item.total_price),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            CurrencyFormatter.format(o.total_amount),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            order?.let { currentOrder ->
                                scope.launch {
                                    val message = buildOrderReceiptText(currentOrder, orderItems)
                                    val ok = PrinterUtils.printMessage(context, message, alignment = 0)
                                    snackbarHostState.showSnackbar(
                                        if (ok) "Sent to printer" else "Print failed — check printer"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Print Receipt")
                    }

                    if (!o.isOrderFinalized) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToEdit,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit order")
                        }
                    }
                }
            }
        }
    }

}
