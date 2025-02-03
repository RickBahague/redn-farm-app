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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.Order
import com.redn.farm.utils.CurrencyFormatter
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
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToView: (Int) -> Unit,
    viewModel: OrderHistoryViewModel = viewModel(factory = OrderHistoryViewModel.Factory)
) {
    var showDeleteDialog by remember { mutableStateOf<Order?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    val orders by viewModel.orders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val orderSummary by viewModel.orderSummary.collectAsState()

    Scaffold(
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(orders) { order ->
                    OrderHistoryCard(
                        order = order,
                        onEditClick = { onNavigateToEdit(order.order_id) },
                        onViewClick = { onNavigateToView(order.order_id) }
                    )
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
    onEditClick: () -> Unit,
    onViewClick: () -> Unit
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

                // Action buttons based on payment status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (order.is_paid) {
                        // View button for paid orders
                        IconButton(onClick = onViewClick) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "View order",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Edit and Delete buttons for unpaid orders
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit order",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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