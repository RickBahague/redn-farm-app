package com.redn.farm.ui.screens.manage.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter
import com.redn.farm.data.model.Customer
import com.redn.farm.data.model.CustomerType
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCustomersScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageCustomersViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Customer?>(null) }
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Determine screen width for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp

    LaunchedEffect(uiState) {
        if (uiState is ManageCustomersViewModel.UiState.Success) {
            // Reset UI state after showing success message
            showDeleteConfirmation = null
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Manage Customers") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, "Add Customer")
                        }
                    }
                )

                // Show messages below the TopAppBar
                AnimatedVisibility(
                    visible = uiState is ManageCustomersViewModel.UiState.Error || 
                             uiState is ManageCustomersViewModel.UiState.Success
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = when (uiState) {
                            is ManageCustomersViewModel.UiState.Error -> MaterialTheme.colorScheme.errorContainer
                            is ManageCustomersViewModel.UiState.Success -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        contentColor = when (uiState) {
                            is ManageCustomersViewModel.UiState.Error -> MaterialTheme.colorScheme.onErrorContainer
                            is ManageCustomersViewModel.UiState.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (uiState) {
                                    is ManageCustomersViewModel.UiState.Error -> 
                                        (uiState as ManageCustomersViewModel.UiState.Error).message
                                    is ManageCustomersViewModel.UiState.Success -> 
                                        (uiState as ManageCustomersViewModel.UiState.Success).message
                                    else -> ""
                                },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.dismissMessage() }) {
                                Icon(Icons.Default.Close, "Dismiss")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .imePadding()
            ) {
                // Search field with compact padding
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("Search customers") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    singleLine = true
                )

                val isFiltering = searchQuery.isNotBlank()
                if (customers.isEmpty() && uiState !is ManageCustomersViewModel.UiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isFiltering) "No matching customers" else "No customers yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isFiltering) "Try adjusting your search." else "Add your first customer to start recording orders.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isFiltering) "Add customer" else "Add customer")
                            }
                        }
                    }
                } else {
                    // Customer grid/list based on screen width
                    if (isWideScreen) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 300.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(customers) { customer ->
                                CustomerCard(
                                    customer = customer,
                                    onEdit = { customerToEdit = customer },
                                    onDelete = { showDeleteConfirmation = customer }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(customers) { customer ->
                                CustomerCard(
                                    customer = customer,
                                    onEdit = { customerToEdit = customer },
                                    onDelete = { showDeleteConfirmation = customer }
                                )
                            }
                        }
                    }
                }
            }

            // Show loading indicator
            if (uiState is ManageCustomersViewModel.UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Add/Edit dialog
        if (showAddDialog || customerToEdit != null) {
            CustomerDialog(
                customer = customerToEdit,
                onDismiss = {
                    showAddDialog = false
                    customerToEdit = null
                },
                onSave = { customer ->
                    if (customerToEdit == null) {
                        viewModel.addCustomer(customer)
                    } else {
                        viewModel.updateCustomer(customer)
                    }
                    showAddDialog = false
                    customerToEdit = null
                }
            )
        }

        // Delete confirmation dialog
        showDeleteConfirmation?.let { customer ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Delete Customer") },
                text = { Text("Are you sure you want to delete ${customer.fullName}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCustomer(customer)
                            showDeleteConfirmation = null
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
                    TextButton(onClick = { showDeleteConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Customer Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = customer.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Text(
                            text = customer.contact,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (customer.fullAddress.isNotBlank()) {
                            Text(
                                text = customer.fullAddress,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = customer.customer_type.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerDialog(
    customer: Customer? = null,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var firstname by remember { mutableStateOf(customer?.firstname ?: "") }
    var lastname by remember { mutableStateOf(customer?.lastname ?: "") }
    var contact by remember { mutableStateOf(customer?.contact ?: "") }
    var customerType by remember { mutableStateOf(customer?.customer_type ?: CustomerType.RETAIL) }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var city by remember { mutableStateOf(customer?.city ?: "") }
    var province by remember { mutableStateOf(customer?.province ?: "") }
    var postalCode by remember { mutableStateOf(customer?.postal_code ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "Add Customer" else "Edit Customer") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Basic Information Section
                OutlinedTextField(
                    value = firstname,
                    onValueChange = { firstname = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lastname,
                    onValueChange = { lastname = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Customer Type Selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = customerType.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        CustomerType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.toString()) },
                                onClick = {
                                    customerType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Address Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = province,
                        onValueChange = { province = it },
                        label = { Text("Province") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text("Postal Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        Customer(
                            customer_id = customer?.customer_id ?: 0,
                            firstname = firstname,
                            lastname = lastname,
                            contact = contact,
                            customer_type = customerType,
                            address = address,
                            city = city,
                            province = province,
                            postal_code = postalCode
                        )
                    )
                },
                enabled = firstname.isNotEmpty() && lastname.isNotEmpty()
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