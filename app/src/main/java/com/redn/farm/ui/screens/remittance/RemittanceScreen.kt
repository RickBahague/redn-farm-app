package com.redn.farm.ui.screens.remittance

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.Remittance
import com.redn.farm.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemittanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: RemittanceViewModel = viewModel(
        factory = RemittanceViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Remittance?>(null) }
    var amount by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var searchQuery by remember { mutableStateOf("") }
    
    val remittances by viewModel.remittances.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Filter remittances based on search query
    val filteredRemittances = remittances.filter { remittance ->
        remittance.remarks.contains(searchQuery, ignoreCase = true) ||
        CurrencyFormatter.format(remittance.amount).contains(searchQuery, ignoreCase = true) ||
        dateFormatter.format(Date(remittance.date)).contains(searchQuery, ignoreCase = true)
    }

    // Calculate total remittances
    val totalRemittances = filteredRemittances.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remittances") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Remittance")
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
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Total Remittances",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = CurrencyFormatter.format(totalRemittances),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Showing ${filteredRemittances.size} of ${remittances.size} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text("Search") },
                placeholder = { Text("Search by amount, date, or remarks") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Remittances List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRemittances) { remittance ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(remittance.amount),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    // Edit button
                                    IconButton(
                                        onClick = {
                                            showEditDialog = remittance
                                            amount = remittance.amount.toString()
                                            remarks = remittance.remarks
                                            selectedDate = remittance.date
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    // Delete button
                                    IconButton(
                                        onClick = { viewModel.deleteRemittance(remittance) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // Remittance date
                            Text(
                                text = "Date: ${dateFormatter.format(Date(remittance.date))}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Show update date if it's different from create date
                            if (remittance.date_updated > remittance.date) {
                                Text(
                                    text = "Updated: ${dateFormatter.format(Date(remittance.date_updated))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Created: ${dateFormatter.format(Date(remittance.date))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Remarks if any
                            if (remittance.remarks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = remittance.remarks,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        RemittanceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amountValue, remarksValue, dateValue ->
                viewModel.addRemittance(amountValue, remarksValue, dateValue)
            },
            title = "Add Remittance"
        )
    }

    // Edit Dialog
    showEditDialog?.let { remittance ->
        RemittanceDialog(
            onDismiss = { showEditDialog = null },
            onConfirm = { amountValue, remarksValue, dateValue ->
                viewModel.updateRemittance(
                    remittance.copy(
                        amount = amountValue,
                        remarks = remarksValue,
                        date = dateValue,
                        date_updated = System.currentTimeMillis()
                    )
                )
            },
            title = "Edit Remittance",
            initialAmount = remittance.amount.toString(),
            initialRemarks = remittance.remarks,
            initialDate = remittance.date
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemittanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Long) -> Unit,
    title: String,
    initialAmount: String = "",
    initialRemarks: String = "",
    initialDate: Long = System.currentTimeMillis()
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var remarks by remember { mutableStateOf(initialRemarks) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isAmountError by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Only allow numbers and decimal point
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = newValue
                            isAmountError = false
                        }
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    isError = isAmountError,
                    supportingText = if (isAmountError) {
                        { Text("Please enter a valid amount") }
                    } else null,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Date Selection
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = dateFormatter.format(Date(selectedDate)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0) {
                        onConfirm(amountValue, remarks, selectedDate)
                        onDismiss()
                    } else {
                        isAmountError = true
                    }
                },
                enabled = amount.isNotEmpty()
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
            }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate
                ),
                showModeToggle = false,
                title = { Text("Select Date") }
            )
        }
    }
} 