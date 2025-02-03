package com.redn.farm.ui.screens.acquire

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.model.Product
import com.redn.farm.utils.CurrencyFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.style.TextOverflow
import java.time.ZoneId
import java.time.Instant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcquireProduceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AcquireProduceViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showProductSelection by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var acquisitionToEdit by remember { mutableStateOf<Acquisition?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    val acquisitions by viewModel.acquisitions.collectAsState()
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()

    // Determine screen width for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp

    // Add product selection dialog
    if (showProductSelection) {
        ProductSelectionDialog(
            products = products,
            onProductSelected = { product ->
                selectedProduct = product
                showProductSelection = false
            },
            onDismiss = { showProductSelection = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acquire Produce") },
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
                        Icon(Icons.Default.Add, "Add Acquisition")
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
                AcquireProduceFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    dateRange = selectedDateRange,
                    onDateRangeSelected = { viewModel.updateDateRange(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Acquisitions grid/list based on screen width
            if (isWideScreen) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = acquisitions,
                        key = { it.acquisition_id }
                    ) { acquisition ->
                        AcquisitionCard(
                            acquisition = acquisition,
                            onDelete = { viewModel.deleteAcquisition(acquisition) },
                            onEdit = {
                                acquisitionToEdit = acquisition
                                selectedProduct = Product(
                                    product_id = acquisition.product_id,
                                    product_name = acquisition.product_name,
                                    product_description = "",
                                    unit_type = if (acquisition.is_per_kg) "kg" else "piece",
                                    is_active = true
                                )
                                showAddDialog = true
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = acquisitions,
                        key = { it.acquisition_id }
                    ) { acquisition ->
                        AcquisitionCard(
                            acquisition = acquisition,
                            onDelete = { viewModel.deleteAcquisition(acquisition) },
                            onEdit = {
                                acquisitionToEdit = acquisition
                                selectedProduct = Product(
                                    product_id = acquisition.product_id,
                                    product_name = acquisition.product_name,
                                    product_description = "",
                                    unit_type = if (acquisition.is_per_kg) "kg" else "piece",
                                    is_active = true
                                )
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AcquisitionDialog(
                selectedProduct = selectedProduct,
                acquisitionToEdit = acquisitionToEdit,
                onSelectProduct = { showProductSelection = true },
                onDismiss = { 
                    showAddDialog = false 
                    selectedProduct = null
                    acquisitionToEdit = null 
                },
                onSave = { acquisition ->
                    if (acquisitionToEdit != null) {
                        viewModel.updateAcquisition(acquisition)
                    } else {
                        viewModel.addAcquisition(acquisition)
                    }
                    showAddDialog = false
                    selectedProduct = null
                    acquisitionToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcquisitionDialog(
    selectedProduct: Product?,
    acquisitionToEdit: Acquisition? = null,
    onSelectProduct: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Acquisition) -> Unit
) {
    var quantity by remember { mutableStateOf(acquisitionToEdit?.quantity?.toString() ?: "") }
    var pricePerUnit by remember { mutableStateOf(acquisitionToEdit?.price_per_unit?.toString() ?: "") }
    var isPerKg by remember { mutableStateOf(acquisitionToEdit?.is_per_kg ?: true) }
    var totalAmount by remember { mutableStateOf(acquisitionToEdit?.total_amount?.toString() ?: "") }
    var location by remember { mutableStateOf(acquisitionToEdit?.location ?: AcquisitionLocation.FARM) }
    var expanded by remember { mutableStateOf(false) }
    
    var selectedDate by remember { 
        mutableStateOf(
            if (acquisitionToEdit != null) {
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(acquisitionToEdit.date_acquired),
                    ZoneId.systemDefault()
                )
            } else {
                LocalDateTime.now()
            }
        )
    }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (acquisitionToEdit == null) "Add Acquisition" else "Edit Acquisition") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Selection
                OutlinedCard(
                    onClick = onSelectProduct,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Product",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = selectedProduct?.product_name ?: "Select product",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Date and Location Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Selection
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = dateFormatter.format(selectedDate),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Location Selection
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = location.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Location") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            AcquisitionLocation.values().forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc.toString()) },
                                    onClick = {
                                        location = loc
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Quantity and Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                quantity = it
                                if (it.isNotEmpty() && pricePerUnit.isNotEmpty()) {
                                    totalAmount = (it.toDouble() * pricePerUnit.toDouble()).toString()
                                }
                            }
                        },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pricePerUnit,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                pricePerUnit = it
                                if (it.isNotEmpty() && quantity.isNotEmpty()) {
                                    totalAmount = (it.toDouble() * quantity.toDouble()).toString()
                                }
                            }
                        },
                        label = { Text("Price/Unit") },
                        prefix = { Text("₱") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Total Amount
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            totalAmount = it
                        }
                    },
                    label = { Text("Total Amount") },
                    prefix = { Text("₱") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = if (isPerKg) "Unit - Per kg" else "Unit - Per pc",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Switch(
                        checked = isPerKg,
                        onCheckedChange = { isPerKg = it },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedProduct?.let { product ->
                        onSave(
                            Acquisition(
                                acquisition_id = acquisitionToEdit?.acquisition_id ?: 0,
                                product_id = product.product_id,
                                product_name = product.product_name,
                                quantity = quantity.toDoubleOrNull() ?: 0.0,
                                price_per_unit = pricePerUnit.toDoubleOrNull() ?: 0.0,
                                total_amount = totalAmount.toDoubleOrNull() ?: 0.0,
                                is_per_kg = isPerKg,
                                date_acquired = selectedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                location = location
                            )
                        )
                    }
                },
                enabled = selectedProduct != null && 
                         quantity.isNotEmpty() && 
                         pricePerUnit.isNotEmpty() &&
                         totalAmount.isNotEmpty()
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                ),
                showModeToggle = false
            )
        }
    }
}

@Composable
private fun AcquisitionCard(
    acquisition: Acquisition,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(acquisition.date_acquired) {
        val instant = Instant.ofEpochMilli(acquisition.date_acquired)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        dateFormatter.format(dateTime)
    }

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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = acquisition.product_name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Text(
                            text = "Quantity: ${acquisition.quantity} ${if (acquisition.is_per_kg) "kg" else "pcs"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = CurrencyFormatter.format(acquisition.total_amount),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = acquisition.location.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

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

@Composable
private fun ProductSelectionDialog(
    products: List<Product>,
    onProductSelected: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Product") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    ProductSelectionItem(
                        product = product,
                        onClick = { onProductSelected(product) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ProductSelectionItem(
    product: Product,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Column {
            Text(
                text = product.product_name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (product.product_description.isNotEmpty()) {
                Text(
                    text = product.product_description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
} 