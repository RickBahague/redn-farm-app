package com.redn.farm.ui.screens.farmops

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.redn.farm.data.model.FarmOperation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FarmOperationCard(
    operation: FarmOperation,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPrintClick: () -> Unit = {},
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault())
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                    if (operation.product_name.isNotBlank()) {
                        Text(
                            text = "Product: ${operation.product_name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Date: ${dateFormatter.format(Instant.ofEpochMilli(operation.operation_date))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onPrintClick) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
} 