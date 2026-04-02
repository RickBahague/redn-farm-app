package com.redn.farm.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.CartItem
import java.util.Locale

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
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "Remove item",
                        modifier = Modifier.size(20.dp)
                    )
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
                val step = if (orderItem.isPerKg) 0.25 else 1.0
                val currentQty = orderItem.quantity
                val minQty = if (orderItem.isPerKg) step else 1.0

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val next = currentQty - step
                                if (currentQty <= minQty || next <= 0.0) {
                                    onRemove()
                                } else {
                                    onQuantityChange(next)
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                        }

                        val qtyText = if (orderItem.isPerKg) {
                            String.format(Locale.US, "%.2f", currentQty)
                        } else {
                            currentQty.toInt().toString()
                        }

                        Text(
                            text = qtyText + " " + if (orderItem.isPerKg) "kg" else "pcs",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                val next = currentQty + step
                                onQuantityChange(next)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                        }
                    }
                }

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