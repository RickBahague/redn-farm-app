package com.redn.farm.ui.screens.order

import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.ui.components.NumericPadBottomSheet
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Product
import androidx.compose.ui.platform.LocalConfiguration
import com.redn.farm.data.pricing.SalesChannel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectionDialog(
    onDismiss: () -> Unit,
    onProductSelected: (Product, Double, Boolean) -> Unit,
    viewModel: TakeOrderViewModel = hiltViewModel()
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf("") }
    var isPerKg by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var numericPadVisible by remember { mutableStateOf(false) }

    val products by viewModel.products.collectAsState()
    val channel by viewModel.channel.collectAsState()
    val focusManager = LocalFocusManager.current

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
                    .imePadding()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedProduct == null) {
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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProduct = product
                                        isPerKg = viewModel.defaultIsPerKgForProductLine(product)
                                        quantity = ""
                                    }
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
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedProduct?.let { product ->
                            Text(
                                text = product.product_name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Channel: ${SalesChannel.label(channel)} · unit price is set from active SRP (or manual fallback).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val previewPrice = viewModel.resolvePreviewUnitPrice(product.product_id, isPerKg)
                            Text(
                                text = "Unit price: ${CurrencyFormatter.format(previewPrice)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val productPrice = viewModel.getLatestProductPrice(product.product_id)
                            if (productPrice != null) {
                                Text(
                                    text = "Manual fallback — kg ${CurrencyFormatter.format(productPrice.per_kg_price ?: 0.0)} · pc ${CurrencyFormatter.format(productPrice.per_piece_price ?: 0.0)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = quantity,
                                onValueChange = {},
                                label = { Text("Quantity") },
                                readOnly = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        numericPadVisible = true
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(Icons.Default.Dialpad, contentDescription = "Open numeric pad")
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (viewModel.productSupportsDualUnit(product)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isPerKg) "Per kilogram" else "Per piece")
                                    Switch(
                                        checked = isPerKg,
                                        onCheckedChange = { isPerKg = it }
                                    )
                                }
                            }

                            quantity.toDoubleOrNull()?.let { qty ->
                                val unit = viewModel.resolvePreviewUnitPrice(product.product_id, isPerKg)
                                Text(
                                    text = "Line total: ${CurrencyFormatter.format(qty * unit)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.End)
                                )
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

    NumericPadBottomSheet(
        visible = numericPadVisible,
        title = "Quantity",
        value = quantity,
        onValueChange = { quantity = it },
        decimalEnabled = true,
        maxDecimalPlaces = 3,
        onDismiss = { numericPadVisible = false }
    )
}
