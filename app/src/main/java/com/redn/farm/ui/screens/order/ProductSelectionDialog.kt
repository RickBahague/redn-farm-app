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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.BorderStroke

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
    var searchQuery by remember { mutableStateOf("") }
    
    val products by viewModel.products.collectAsState()
    val isWideScreen = LocalConfiguration.current.screenWidthDp > 600
    
    // Filter products based on search query
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { product ->
                product.product_name.contains(searchQuery, ignoreCase = true) ||
                product.product_description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedProduct == null) "Select Product" else "Add Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedProduct == null) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search Products") },
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

                    // Product grid
                    LazyVerticalGrid(
                        columns = if (isWideScreen) GridCells.Fixed(2) else GridCells.Fixed(1),
                        modifier = Modifier.height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredProducts,
                            key = { it.product_id }
                        ) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProduct = product }
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
                                    if (product.product_description.isNotEmpty()) {
                                        Text(
                                            text = product.product_description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (filteredProducts.isEmpty()) {
                            item(span = { GridItemSpan(if (isWideScreen) 2 else 1) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (searchQuery.isNotEmpty()) {
                                        Text(
                                            text = "No products found",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Product details card
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = selectedProduct!!.product_name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (selectedProduct!!.product_description.isNotEmpty()) {
                                Text(
                                    text = selectedProduct!!.product_description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Unit type selection
                    val productPrice = viewModel.getLatestProductPrice(selectedProduct!!.product_id)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isPerKg = true },
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (isPerKg) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Per Kilogram",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = productPrice?.per_kg_price?.let { 
                                        CurrencyFormatter.format(it)
                                    } ?: "N/A",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isPerKg) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isPerKg = false },
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (!isPerKg) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Per Piece",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = productPrice?.per_piece_price?.let { 
                                        CurrencyFormatter.format(it)
                                    } ?: "N/A",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (!isPerKg) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quantity input
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total calculation
                    val currentUnitPrice = if (isPerKg) {
                        productPrice?.per_kg_price
                    } else {
                        productPrice?.per_piece_price
                    } ?: 0.0

                    quantity.toDoubleOrNull()?.let { qty ->
                        val total = qty * currentUnitPrice
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Unit Price:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(currentUnitPrice),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total:",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(total),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
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
            TextButton(
                onClick = {
                    if (selectedProduct == null) {
                        onDismiss()
                    } else {
                        selectedProduct = null
                        quantity = ""
                    }
                }
            ) {
                Text(if (selectedProduct == null) "Cancel" else "Back")
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
            if (product.product_description.isNotEmpty()) {
                Text(
                    text = product.product_description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 