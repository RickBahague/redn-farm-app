package com.redn.farm.ui.screens.farmops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.data.model.FarmOperationType
import com.redn.farm.data.model.Product
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmOperationDialog(
    operation: FarmOperation?,
    products: List<Product>,
    onDismiss: () -> Unit,
    onSave: (FarmOperation) -> Unit
) {
    var selectedType by remember { mutableStateOf(operation?.operation_type ?: FarmOperationType.SOWING) }
    var details by remember { mutableStateOf(operation?.details ?: "") }
    var area by remember { mutableStateOf(operation?.area ?: "") }
    var weather by remember { mutableStateOf(operation?.weather_condition ?: "") }
    var personnel by remember { mutableStateOf(operation?.personnel ?: "") }
    var operationDate by remember { mutableStateOf(operation?.operation_date ?: LocalDateTime.now()) }
    var selectedProduct by remember { 
        mutableStateOf<Product?>(
            if (operation?.product_id != null) {
                products.find { it.product_id == operation.product_id }
            } else null
        )
    }
    var showProductSelection by remember { mutableStateOf(false) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())

    // Determine screen width for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp

    if (showProductSelection) {
        AlertDialog(
            onDismissRequest = { showProductSelection = false },
            title = { Text("Select Product") },
            text = {
                LazyVerticalGrid(
                    columns = if (isWideScreen) GridCells.Fixed(2) else GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products) { product ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProduct = product
                                    showProductSelection = false
                                }
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = product.product_name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (product.product_description.isNotEmpty()) {
                                    Text(
                                        text = product.product_description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductSelection = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = operationDate
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            operationDate = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(millis),
                                java.time.ZoneId.systemDefault()
                            )
                        }
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
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (operation == null) "Add Operation" else "Edit Operation") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type and Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Operation Type Selection
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedType.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            FarmOperationType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.toString()) },
                                    onClick = {
                                        selectedType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

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
                                text = dateFormatter.format(operationDate),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // Details
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Area and Weather Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("Area") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weather,
                        onValueChange = { weather = it },
                        label = { Text("Weather") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Personnel
                OutlinedTextField(
                    value = personnel,
                    onValueChange = { personnel = it },
                    label = { Text("Personnel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Product Selection
                OutlinedCard(
                    onClick = { showProductSelection = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Related Product (Optional)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = selectedProduct?.product_name ?: "Select Product",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        FarmOperation(
                            operation_id = operation?.operation_id ?: 0,
                            operation_type = selectedType,
                            operation_date = operationDate,
                            details = details,
                            area = area,
                            weather_condition = weather,
                            personnel = personnel,
                            product_id = selectedProduct?.product_id,
                            product_name = selectedProduct?.product_name ?: "",
                            date_created = operation?.date_created ?: LocalDateTime.now(),
                            date_updated = LocalDateTime.now()
                        )
                    )
                },
                enabled = details.isNotBlank()
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