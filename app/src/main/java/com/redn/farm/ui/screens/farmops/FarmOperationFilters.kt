package com.redn.farm.ui.screens.farmops

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.redn.farm.data.model.FarmOperationType
import com.redn.farm.utils.MillisDateRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmOperationFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedType: FarmOperationType?,
    onTypeSelected: (FarmOperationType?) -> Unit,
    dateRange: Pair<Long?, Long?>,
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault())
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        text = dateRange.first?.let { dateFormatter.format(Instant.ofEpochMilli(it)) }
                            ?: "Select Date",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

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
                        text = dateRange.second?.let { dateFormatter.format(Instant.ofEpochMilli(it)) }
                            ?: "Select Date",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

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

    if (showStartDatePicker) {
        val currentStartMillis = dateRange.first ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentStartMillis
        )

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateRangeSelected(
                                MillisDateRange.startOfDayMillis(millis) to dateRange.second
                            )
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

    if (showEndDatePicker) {
        val currentEndMillis = dateRange.second ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentEndMillis
        )

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateRangeSelected(
                                dateRange.first to MillisDateRange.endOfDayMillis(millis)
                            )
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
