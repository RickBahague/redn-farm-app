package com.redn.farm.ui.screens.farmops

import android.app.Application
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.utils.PrinterUtils
import com.redn.farm.utils.buildFarmOperationLog
import java.time.LocalDateTime
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmOperationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FarmOperationsViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<FarmOperation?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FarmOperation?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    val operations by viewModel.operations.collectAsState()
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Farm Operations") },
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
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Operation")
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

            val isFiltering = searchQuery.isNotBlank()
            if (operations.isEmpty()) {
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
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isFiltering) "No matching operations" else "No operations logged",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isFiltering) "Try adjusting your filters." else "Log your first farm activity.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Log operation")
                        }
                    }
                }
            } else {
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
        }

        // Add/Edit Dialog
        if (showAddDialog || showEditDialog != null) {
            FarmOperationDialog(
                operation = showEditDialog,
                products = products,
                onDismiss = {
                    showAddDialog = false
                    showEditDialog = null
                },
                onSave = { operation ->
                    if (showEditDialog != null) {
                        viewModel.updateOperation(operation)
                    } else {
                        viewModel.addOperation(operation)
                    }
                    showAddDialog = false
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