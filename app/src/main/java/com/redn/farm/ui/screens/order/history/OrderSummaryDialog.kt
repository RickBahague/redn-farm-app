package com.redn.farm.ui.screens.order.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import kotlinx.coroutines.launch

@Composable
fun OrderSummaryDialog(
    summary: OrderHistoryViewModel.OrderSummary,
    onDismiss: () -> Unit
) {
    // Calculate totals
    val totalKg = summary.productSummaries
        .filter { it.isPerKg }
        .sumOf { it.totalQuantity }
    
    val totalPieces = summary.productSummaries
        .filter { !it.isPerKg }
        .sumOf { it.totalQuantity }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }

    fun formatSummaryForPrint(): String {
        return buildString {
            appendLine("ORDER SUMMARY")
            appendLine("=============")
            appendLine()
            
            // Products Section
            appendLine("PRODUCTS (${summary.uniqueProductCount} unique items)")
            appendLine("-------------------------")
            summary.productSummaries.forEach { product ->
                // Truncate product name if longer than 20 chars
                val displayName = if (product.productName.length > 20) {
                    product.productName.substring(0, 17) + "..."
                } else {
                    product.productName.padEnd(20)
                }
                
                // Format: "Product Name          99.99 kg"
                appendLine(
                    String.format(
                        "%s %6.2f %s",
                        displayName,
                        product.totalQuantity,
                        if (product.isPerKg) "kg" else "pc"
                    )
                )
            }
            appendLine()
            
            // Totals Section
            appendLine("SUMMARY")
            appendLine("-------")
            if (totalKg > 0) {
                appendLine(String.format("Total Kilograms: %.2f kg", totalKg))
            }
            if (totalPieces > 0) {
                appendLine(String.format("Total Pieces: %.0f pc", totalPieces))
            }
            appendLine(String.format("Unique Customers: %d", summary.uniqueCustomerCount))
            appendLine(String.format("Gross Est: %s", CurrencyFormatter.format(summary.totalAmount)))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order Summary") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Products Section
                item {
                    Text(
                        text = "Products (${summary.uniqueProductCount} unique items)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(summary.productSummaries) { product ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = product.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format(
                                "%.2f %s",
                                product.totalQuantity,
                                if (product.isPerKg) "kg" else "pcs"
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Customer Summary
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Totals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Total Quantities
                    if (totalKg > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Kilograms:")
                            Text(String.format("%.2f kg", totalKg))
                        }
                    }
                    
                    if (totalPieces > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Pieces:")
                            Text(String.format("%.0f pcs", totalPieces))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Customer Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unique Customers:")
                        Text(summary.uniqueCustomerCount.toString())
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:")
                        Text(
                            text = CurrencyFormatter.format(summary.totalAmount),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { 
                        if (!isPrinting) {
                            isPrinting = true
                            scope.launch {
                                try {
                                    PrinterUtils.printMessage(context, formatSummaryForPrint(), alignment = 0)
                                } finally {
                                    isPrinting = false
                                }
                            }
                        }
                    },
                    enabled = !isPrinting
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPrinting) "Printing..." else "Print")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
} 