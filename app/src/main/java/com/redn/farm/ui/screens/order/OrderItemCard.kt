package com.redn.farm.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderItemCard(
    orderItem: CartItem,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    showUnitToggle: Boolean = false,
    onToggleUnit: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        text = orderItem.product.product_name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${orderItem.quantity} ${if (orderItem.isPerKg) "kg" else "pcs"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, "Remove item")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showUnitToggle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (orderItem.isPerKg) "Sold by kg" else "Sold by piece",
                        style = MaterialTheme.typography.labelMedium
                    )
                    TextButton(onClick = onToggleUnit) {
                        Text("Switch unit")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity input
                OutlinedTextField(
                    value = orderItem.quantity.toString(),
                    onValueChange = { 
                        val newQty = it.toDoubleOrNull()
                        if (newQty != null && newQty > 0) {
                            onQuantityChange(newQty)
                        }
                    },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(120.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Price and Total
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SRP / unit: ${CurrencyFormatter.format(orderItem.price)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Total: ${CurrencyFormatter.format(orderItem.total)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
} 