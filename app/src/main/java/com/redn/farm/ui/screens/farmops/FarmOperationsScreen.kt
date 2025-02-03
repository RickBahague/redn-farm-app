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
import java.time.LocalDateTime

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

    Scaffold(
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(operations) { operation ->
                    FarmOperationCard(
                        operation = operation,
                        onEditClick = { showEditDialog = operation },
                        onDeleteClick = { showDeleteDialog = operation }
                    )
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