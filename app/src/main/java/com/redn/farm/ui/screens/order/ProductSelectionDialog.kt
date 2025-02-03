package com.redn.farm.ui.screens.order

import com.redn.farm.utils.CurrencyFormatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.Product
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectionDialog(
    onDismiss: () -> Unit,
    onProductSelected: (Product, Double, Boolean) -> Unit,
    viewModel: TakeOrderViewModel = viewModel(factory = TakeOrderViewModel.Factory)
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf("") }
    var isPerKg by remember { mutableStateOf(true) }
    
    val products by viewModel.products.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product selection
                OutlinedCard(
                    onClick = { /* Show product selection */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Select Product",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = selectedProduct?.product_name ?: "Choose a product",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Product list when no product is selected
                if (selectedProduct == null) {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products) { product ->
                            ProductItem(
                                product = product,
                                onClick = { selectedProduct = product }
                            )
                        }
                    }
                } else {
                    // Show current prices when product is selected
                    val productPrice = viewModel.getLatestProductPrice(selectedProduct!!.product_id)
                    productPrice?.let { price ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Per Kg: ${price.per_kg_price?.let { CurrencyFormatter.format(it) } ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Per Piece: ${price.per_piece_price?.let { CurrencyFormatter.format(it) } ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Quantity input
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                quantity = it
                            }
                        },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Per kg/piece switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isPerKg) "Per Kilogram" else "Per Piece")
                        Switch(
                            checked = isPerKg,
                            onCheckedChange = { isPerKg = it }
                        )
                    }

                    // Show real-time total calculation with Philippine Peso
                    val currentUnitPrice = if (isPerKg) {
                        productPrice?.per_kg_price
                    } else {
                        productPrice?.per_piece_price
                    } ?: 0.0

                    quantity.toDoubleOrNull()?.let { qty ->
                        val total = qty * currentUnitPrice
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Unit Price: ${CurrencyFormatter.format(currentUnitPrice)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Total: ${CurrencyFormatter.format(total)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedProduct?.let { product ->
                        quantity.toDoubleOrNull()?.let { qty ->
                            onProductSelected(product, qty, isPerKg)
                        }
                    }
                },
                enabled = selectedProduct != null && 
                         quantity.isNotEmpty() && 
                         quantity.toDoubleOrNull() != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ProductItem(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = product.product_name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = product.product_description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 