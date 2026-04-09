package com.redn.farm.ui.screens.order.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import com.redn.farm.utils.MillisDateRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    dateRange: Pair<Long?, Long?>,
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit,
    onShowSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault())
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Search),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedCard(
                onClick = { showFromPicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "From",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = dateRange.first?.let { dateFormatter.format(Instant.ofEpochMilli(it)) } ?: "Any",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedCard(
                onClick = { showToPicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "To",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = dateRange.second?.let { dateFormatter.format(Instant.ofEpochMilli(it)) } ?: "Any",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    onSearchQueryChange("")
                    onDateRangeSelected(null to null)
                },
            ) {
                Text("Clear Filters")
            }

            TextButton(onClick = onShowSummary) {
                Text("Show Summary")
            }
        }
    }

    if (showFromPicker) {
        val fromMillis = dateRange.first ?: System.currentTimeMillis()
        val fromState = rememberDatePickerState(
            initialSelectedDateMillis = fromMillis,
            initialDisplayedMonthMillis = fromMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        fromState.selectedDateMillis?.let { ms ->
                            onDateRangeSelected(MillisDateRange.startOfDayMillis(ms) to dateRange.second)
                        }
                        showFromPicker = false
                    },
                    enabled = fromState.selectedDateMillis != null,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = fromState, showModeToggle = false)
        }
    }

    if (showToPicker) {
        val toMillis = dateRange.second ?: dateRange.first ?: System.currentTimeMillis()
        val toState = rememberDatePickerState(
            initialSelectedDateMillis = toMillis,
            initialDisplayedMonthMillis = toMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        toState.selectedDateMillis?.let { ms ->
                            onDateRangeSelected(dateRange.first to MillisDateRange.endOfDayMillis(ms))
                        }
                        showToPicker = false
                    },
                    enabled = toState.selectedDateMillis != null,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = toState, showModeToggle = false)
        }
    }
}
