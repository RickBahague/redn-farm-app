package com.redn.farm.ui.screens.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.redn.farm.data.pricing.SalesChannel
import com.redn.farm.ui.components.NumericPadBottomSheet
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import com.redn.farm.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderProductPickerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TakeOrderViewModel,
) {
    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("") }
    var isPerKg by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var numericPadVisible by remember { mutableStateOf(false) }

    val products by viewModel.products.collectAsState()
    val channel by viewModel.channel.collectAsState()
    val focusManager = LocalFocusManager.current
    val selectedProduct = remember(products, selectedProductId) {
        products.firstOrNull { it.product_id == selectedProductId }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedProduct == null) "Select Product" else "Add Product") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedProduct == null) {
                                onNavigateBack()
                            } else {
                                selectedProductId = null
                                quantity = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (selectedProduct == null) "Back" else "Back to product list",
                        )
                    }
                },
                actions = {
                    if (selectedProduct != null) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                val product = selectedProduct
                                val qty = quantity.toDoubleOrNull() ?: return@TextButton
                                viewModel.addToCart(product, qty, isPerKg)
                                onNavigateBack()
                            },
                            enabled = quantity.toDoubleOrNull() != null,
                        ) {
                            Text("Add")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    singleLine = true,
                    keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Search),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredProducts) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProductId = product.product_id
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
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = selectedProduct.product_name,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Channel: ${SalesChannel.label(channel)} · unit price is set from active SRP (or manual fallback).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val previewPrice = viewModel.resolvePreviewUnitPrice(selectedProduct.product_id, isPerKg)
                Text(
                    text = "Unit price: ${CurrencyFormatter.format(previewPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                viewModel.getLatestProductPrice(selectedProduct.product_id)?.let { productPrice ->
                    Text(
                        text = "Manual fallback — kg ${CurrencyFormatter.format(productPrice.per_kg_price ?: 0.0)} · pc ${CurrencyFormatter.format(productPrice.per_piece_price ?: 0.0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Quantity") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = {
                            numericPadVisible = true
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Dialpad, contentDescription = "Open numeric pad")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (viewModel.productSupportsDualUnit(selectedProduct)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (isPerKg) "Per kilogram" else "Per piece")
                        Switch(
                            checked = isPerKg,
                            onCheckedChange = { isPerKg = it },
                        )
                    }
                }

                quantity.toDoubleOrNull()?.let { qty ->
                    Text(
                        text = "Line total: ${CurrencyFormatter.format(qty * previewPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

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

