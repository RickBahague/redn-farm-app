package com.redn.farm.ui.screens.order.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.Order
import com.redn.farm.data.pricing.SalesChannel
import com.redn.farm.utils.buildOrderReceiptText
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.animation.AnimatedVisibility
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrderDetail: (Int) -> Unit,
    viewModel: OrderHistoryViewModel = viewModel(factory = OrderHistoryViewModel.Factory)
) {
    var showDeleteDialog by remember { mutableStateOf<Order?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    val orders by viewModel.orders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val orderSummary by viewModel.orderSummary.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Order History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.FilterList else Icons.Default.FilterAlt,
                            contentDescription = "Filters"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showFilters) {
                OrderHistoryFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    dateRange = dateRange,
                    onDateRangeSelected = viewModel::updateDateRange,
                    onShowSummary = viewModel::calculateSummary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            val isFiltering = searchQuery.isNotBlank() ||
                dateRange.first != null ||
                dateRange.second != null

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isFiltering) "No matching orders" else "No orders yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isFiltering) {
                                "Try adjusting your filters."
                            } else {
                                "Start by taking the first customer order."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                if (isFiltering) {
                                    viewModel.updateSearchQuery("")
                                    viewModel.updateDateRange(null to null)
                                    showFilters = false
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Text(if (isFiltering) "Clear filters" else "Take first order")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orders) { order ->
                        OrderHistoryCard(
                            order = order,
                            onOpenDetail = { onNavigateToOrderDetail(order.order_id) },
                            onPrint = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Printing…")
                                    val (o, items) = viewModel.getOrderSnapshotForPrint(order.order_id)
                                    if (o == null) {
                                        snackbarHostState.showSnackbar("Print failed")
                                        return@launch
                                    }
                                    val ok = PrinterUtils.printMessage(
                                        context,
                                        buildOrderReceiptText(o, items),
                                        alignment = 0,
                                    )
                                    snackbarHostState.showSnackbar(
                                        if (ok) "Sent to printer" else "Print failed"
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { order ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Order") },
                text = { Text("Are you sure you want to delete this order?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteOrder(order.order_id)
                            showDeleteDialog = null
                        }
                        ,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Order Summary Dialog
        orderSummary?.let { summary ->
            OrderSummaryDialog(
                summary = summary,
                onDismiss = viewModel::clearSummary
            )
        }
    }
}

@Composable
private fun OrderHistoryCard(
    order: Order,
    onOpenDetail: () -> Unit,
    onPrint: () -> Unit,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    val orderDate = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(order.order_date),
        ZoneId.systemDefault()
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenDetail),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.order_id}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = orderDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SalesChannel.label(SalesChannel.normalize(order.channel)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = order.customerContact,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = CurrencyFormatter.format(order.total_amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
                }
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(
                        onClick = onPrint,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print receipt",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    // Payment status chip
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (order.is_paid)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (order.is_paid) "Paid" else "Unpaid",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (order.is_paid)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    // Delivery status chip
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (order.is_delivered)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (order.is_delivered) "Delivered" else "Pending",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (order.is_delivered)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 