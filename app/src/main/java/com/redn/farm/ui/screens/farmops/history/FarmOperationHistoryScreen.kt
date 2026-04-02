package com.redn.farm.ui.screens.farmops.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.utils.PrinterUtils
import com.redn.farm.utils.buildFarmOperationLog
import kotlinx.coroutines.launch
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.ui.screens.farmops.FarmOperationCard
import com.redn.farm.ui.screens.farmops.FarmOperationDialog
import com.redn.farm.ui.screens.farmops.FarmOperationFilters
import com.redn.farm.ui.screens.farmops.FarmOperationsViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmOperationHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: FarmOperationsViewModel = hiltViewModel()
) {
    var showFilters by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<FarmOperation?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FarmOperation?>(null) }

    val operations by viewModel.operations.collectAsState()
    val products by viewModel.products.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Operation History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
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
                FarmOperationFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    selectedType = selectedType,
                    onTypeSelected = viewModel::updateSelectedType,
                    dateRange = dateRange,
                    onDateRangeSelected = viewModel::updateDateRange,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(operations) { operation ->
                    FarmOperationCard(
                        operation = operation,
                        onEditClick = { showEditDialog = operation },
                        onDeleteClick = { showDeleteDialog = operation },
                        onPrintClick = {
                            scope.launch {
                                val ok = PrinterUtils.printMessage(
                                    context,
                                    buildFarmOperationLog(operation),
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

        // Edit Dialog
        showEditDialog?.let { operation ->
            FarmOperationDialog(
                operation = operation,
                products = products,
                onDismiss = { showEditDialog = null },
                onSave = { updatedOperation ->
                    viewModel.updateOperation(updatedOperation)
                    showEditDialog = null
                }
            )
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { operation ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Operation") },
                text = { Text("Are you sure you want to delete this operation?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteOperation(operation)
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
    }
}

@Composable
private fun FarmOperationHistoryCard(operation: FarmOperation) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = operation.operation_type.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = operation.details,
                style = MaterialTheme.typography.bodyMedium
            )
            if (operation.area.isNotBlank()) {
                Text(
                    text = "Area: ${operation.area}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (operation.weather_condition.isNotBlank()) {
                Text(
                    text = "Weather: ${operation.weather_condition}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (operation.personnel.isNotBlank()) {
                Text(
                    text = "Personnel: ${operation.personnel}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Operation Date: ${operation.operation_date.format(dateFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Last Updated: ${operation.date_updated.format(dateFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class SortOption(val displayName: String) {
    DATE_DESC("Latest First"),
    DATE_ASC("Oldest First"),
    TYPE("By Operation Type"),
    AREA("By Area")
} 