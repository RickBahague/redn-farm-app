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
    onProductSelected: (Product, Double, Boolean, Boolean) -> Unit,
    viewModel: TakeOrderViewModel = viewModel(factory = TakeOrderViewModel.Factory)
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf("") }
    var isPerKg by remember { mutableStateOf(true) }
    var useDiscountedPrice by remember { mutableStateOf(false) }
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

                    // Product list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts) { product ->
                            val productPrice = viewModel.getLatestProductPrice(product.product_id)
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
                                    
                                    // Regular Prices
//                                    Row(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        horizontalArrangement = Arrangement.SpaceBetween
//                                    ) {
//                                        Text(
//                                            text = "Per Kg: ${CurrencyFormatter.format(productPrice?.per_kg_price ?: 0.0)}",
//                                            style = MaterialTheme.typography.bodyMedium
//                                        )
//                                        Text(
//                                            text = "Per Piece: ${CurrencyFormatter.format(productPrice?.per_piece_price ?: 0.0)}",
//                                            style = MaterialTheme.typography.bodyMedium
//                                        )
//                                    }
//
//                                    // Discounted Prices if available
//                                    if (productPrice?.discounted_per_kg_price != null || productPrice?.discounted_per_piece_price != null) {
//                                        Row(
//                                            modifier = Modifier.fillMaxWidth(),
//                                            horizontalArrangement = Arrangement.SpaceBetween
//                                        ) {
//                                            Text(
//                                                text = "Disc/Kg: ${CurrencyFormatter.format(productPrice?.discounted_per_kg_price ?: 0.0)}",
//                                                style = MaterialTheme.typography.bodyMedium,
//                                                color = MaterialTheme.colorScheme.error
//                                            )
//                                            Text(
//                                                text = "Disc/Pc: ${CurrencyFormatter.format(productPrice?.discounted_per_piece_price ?: 0.0)}",
//                                                style = MaterialTheme.typography.bodyMedium,
//                                                color = MaterialTheme.colorScheme.error
//                                            )
//                                        }
//                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Product details and quantity input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedProduct?.let { product ->
                            Text(
                                text = product.product_name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            val productPrice = viewModel.getLatestProductPrice(product.product_id)

                            // Regular Prices Section
                            Text(
                                text = "Regular Prices",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Per Kg: ${CurrencyFormatter.format(productPrice?.per_kg_price ?: 0.0)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Per Piece: ${CurrencyFormatter.format(productPrice?.per_piece_price ?: 0.0)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Discounted Prices Section
                            if (productPrice?.discounted_per_kg_price != null || productPrice?.discounted_per_piece_price != null) {
                                Text(
                                    text = "Discounted Prices",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Per Kg: ${CurrencyFormatter.format(productPrice?.discounted_per_kg_price ?: 0.0)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Per Piece: ${CurrencyFormatter.format(productPrice?.discounted_per_piece_price ?: 0.0)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

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

                            // Price Type Selection
                            if (productPrice?.let { price ->
                                (isPerKg && price.discounted_per_kg_price != null) ||
                                (!isPerKg && price.discounted_per_piece_price != null)
                            } == true) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Use Discounted Price")
                                    Switch(
                                        checked = useDiscountedPrice,
                                        onCheckedChange = { useDiscountedPrice = it }
                                    )
                                }
                            }

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

                            // Show total based on current selection
                            quantity.toDoubleOrNull()?.let { qty ->
                                val currentUnitPrice = if (isPerKg) {
                                    if (useDiscountedPrice) productPrice?.discounted_per_kg_price else productPrice?.per_kg_price
                                } else {
                                    if (useDiscountedPrice) productPrice?.discounted_per_piece_price else productPrice?.per_piece_price
                                } ?: 0.0

                                val total = qty * currentUnitPrice
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Unit Price: ${CurrencyFormatter.format(currentUnitPrice)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (useDiscountedPrice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedProduct?.let { product ->
                        quantity.toDoubleOrNull()?.let { qty ->
                            val productPrice = viewModel.getLatestProductPrice(product.product_id)
                            val currentUnitPrice = if (isPerKg) {
                                if (useDiscountedPrice) productPrice?.discounted_per_kg_price else productPrice?.per_kg_price
                            } else {
                                if (useDiscountedPrice) productPrice?.discounted_per_piece_price else productPrice?.per_piece_price
                            } ?: 0.0

                            onProductSelected(product, qty, isPerKg, useDiscountedPrice)
                        }
                    }
                },
                enabled = selectedProduct != null && quantity.isNotEmpty() && quantity.toDoubleOrNull() != null
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