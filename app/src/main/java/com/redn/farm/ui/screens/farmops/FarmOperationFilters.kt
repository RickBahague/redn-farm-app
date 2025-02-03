package com.redn.farm.ui.screens.farmops

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.redn.farm.data.model.FarmOperationType
import java.time.ZoneId
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmOperationFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedType: FarmOperationType?,
    onTypeSelected: (FarmOperationType?) -> Unit,
    dateRange: Pair<LocalDateTime?, LocalDateTime?>,
    onDateRangeSelected: (Pair<LocalDateTime?, LocalDateTime?>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search Operations") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Operation Type filter
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedType?.toString() ?: "All Types",
                onValueChange = {},
                readOnly = true,
                label = { Text("Operation Type") },
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
                DropdownMenuItem(
                    text = { Text("All Types") },
                    onClick = {
                        onTypeSelected(null)
                        expanded = false
                    }
                )
                FarmOperationType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.toString()) },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Date Range Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // From Date
            OutlinedCard(
                onClick = { showStartDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "From",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = dateRange.first?.format(dateFormatter) ?: "Select Date",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // To Date
            OutlinedCard(
                onClick = { showEndDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "To",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = dateRange.second?.format(dateFormatter) ?: "Select Date",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Clear Filters Button
        TextButton(
            onClick = {
                onSearchQueryChange("")
                onTypeSelected(null)
                onDateRangeSelected(null to null)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Clear Filters")
        }
    }

    // Start Date Picker Dialog
    if (showStartDatePicker) {
        val currentStartDate = dateRange.first ?: LocalDateTime.now()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentStartDate
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(millis),
                                ZoneId.systemDefault()
                            ).withHour(0).withMinute(0).withSecond(0)
                            onDateRangeSelected(selectedDate to dateRange.second)
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
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

    // End Date Picker Dialog
    if (showEndDatePicker) {
        val currentEndDate = dateRange.second ?: LocalDateTime.now()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentEndDate
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(millis),
                                ZoneId.systemDefault()
                            ).withHour(23).withMinute(59).withSecond(59)
                            onDateRangeSelected(dateRange.first to selectedDate)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
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
} 