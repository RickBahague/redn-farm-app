package com.redn.farm.ui.screens.manage.products

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.repository.ProductRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import com.redn.farm.utils.CurrencyFormatter
import androidx.compose.foundation.clickable
import java.time.format.DateTimeFormatter
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.redn.farm.data.model.ProductFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageProductsViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { FarmDatabase.getDatabase(context) }
    val repository = remember { 
        ProductRepository(database.productDao(), database.productPriceDao()) 
    }
    
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var productPrices by remember { mutableStateOf<List<ProductPrice>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Product?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    val isReinitializing by viewModel.isReinitializing.collectAsState()
    val error by viewModel.error.collectAsState()

    // Collect products and prices using LaunchedEffect
    LaunchedEffect(Unit) {
        repository.getAllProducts().collect { newProducts ->
            products = newProducts
        }
    }

    LaunchedEffect(Unit) {
        repository.getAllProductPrices().collect { newPrices ->
            productPrices = newPrices
        }
    }

    // Refresh after reinitialization
    LaunchedEffect(isReinitializing) {
        if (!isReinitializing) {
            // No need to call refreshData() as the Flow will automatically emit new values
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Products") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.FilterList else Icons.Default.FilterAlt,
                            contentDescription = "Filters"
                        )
                    }
                    // Add button
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Product")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    val price = productPrices.find { it.product_id == product.product_id }
                    ProductCard(
                        product = product,
                        productPrice = price,
                        onEditClick = { showEditDialog = product }
                    )
                }
            }

            if (isReinitializing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(errorMessage)
                }
            }
        }

        showEditDialog?.let { product ->
            EditProductDialog(
                product = product,
                productPrice = productPrices.find { it.product_id == product.product_id },
                onDismiss = { showEditDialog = null },
                onSave = { updatedProduct, updatedPrice ->
                    scope.launch {
                        try {
                            repository.updateProduct(updatedProduct)
                            repository.updateProductPrice(updatedPrice)
                            showEditDialog = null
                        } catch (e: Exception) {
                            // Handle error
                            Log.e("ManageProductsScreen", "Error updating product", e)
                        }
                    }
                }
            )
        }

        if (showAddDialog) {
            AddProductDialog(
                onDismiss = { showAddDialog = false },
                onSave = { product, price ->
                    scope.launch {
                        try {
                            // Generate a unique product ID
                            val productId = "P${System.currentTimeMillis()}_${(1000..9999).random()}"
                            
                            // Insert the product with the generated ID
                            repository.insertProduct(product.copy(product_id = productId))
                            
                            // Insert the price with the same product ID
                            repository.insertProductPrice(price.copy(product_id = productId))
                            
                            showAddDialog = false
                        } catch (e: Exception) {
                            Log.e("ManageProductsScreen", "Error adding product", e)
                        }
                    }
                }
            )
        }

        if (showFilters) {
            FilterDialog(
                onDismiss = { showFilters = false },
                onApplyFilters = { filters ->
                    scope.launch {
                        // Apply filters to products list
                        repository.getFilteredProducts(filters).collect { filteredProducts ->
                            products = filteredProducts
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    productPrice: ProductPrice?,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
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
            productPrice?.let { price ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    price.per_kg_price?.let {
                        Text(
                            text = "Per Kg: ${CurrencyFormatter.format(it)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    price.per_piece_price?.let {
                        Text(
                            text = "Per Piece: ${CurrencyFormatter.format(it)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: Product,
    productPrice: ProductPrice?,
    onDismiss: () -> Unit,
    onSave: (Product, ProductPrice) -> Unit
) {
    var name by remember { mutableStateOf(product.product_name) }
    var description by remember { mutableStateOf(product.product_description) }
    var perKgPrice by remember { mutableStateOf(productPrice?.per_kg_price?.toString() ?: "") }
    var perPiecePrice by remember { mutableStateOf(productPrice?.per_piece_price?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = perKgPrice,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            perKgPrice = it
                        }
                    },
                    label = { Text("Price per kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = perPiecePrice,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            perPiecePrice = it
                        }
                    },
                    label = { Text("Price per piece") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedProduct = product.copy(
                        product_name = name,
                        product_description = description
                    )
                    val updatedPrice = ProductPrice(
                        product_id = product.product_id,
                        per_kg_price = perKgPrice.toDoubleOrNull(),
                        per_piece_price = perPiecePrice.toDoubleOrNull(),
                        date_created = LocalDateTime.now()
                    )
                    onSave(updatedProduct, updatedPrice)
                },
                enabled = name.isNotEmpty() && (perKgPrice.isNotEmpty() || perPiecePrice.isNotEmpty())
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPriceDialog(
    product: Product,
    currentPrice: ProductPrice?,
    onDismiss: () -> Unit,
    onConfirm: (ProductPrice) -> Unit
) {
    var perKgPrice by remember { mutableStateOf(currentPrice?.per_kg_price?.toString() ?: "") }
    var perPiecePrice by remember { mutableStateOf(currentPrice?.per_piece_price?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Price for ${product.product_name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = perKgPrice,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            perKgPrice = it
                        }
                    },
                    label = { Text("Price per Kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = perPiecePrice,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            perPiecePrice = it
                        }
                    },
                    label = { Text("Price per Piece") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ProductPrice(
                            product_id = product.product_id,
                            per_kg_price = perKgPrice.toDoubleOrNull(),
                            per_piece_price = perPiecePrice.toDoubleOrNull(),
                            date_created = LocalDateTime.now()
                        )
                    )
                    onDismiss()
                },
                enabled = perKgPrice.isNotEmpty() || perPiecePrice.isNotEmpty()
            ) {
                Text("Save")
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
private fun ProductPriceInputs(
    perKgPrice: String,
    onPerKgPriceChange: (String) -> Unit,
    perPiecePrice: String,
    onPerPiecePriceChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = perKgPrice,
            onValueChange = { 
                if (it.isEmpty() || it.toDoubleOrNull() != null) {
                    onPerKgPriceChange(it)
                }
            },
            label = { Text("Price per Kg") },
            prefix = { Text("₱") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = perPiecePrice,
            onValueChange = { 
                if (it.isEmpty() || it.toDoubleOrNull() != null) {
                    onPerPiecePriceChange(it)
                }
            },
            label = { Text("Price per Piece") },
            prefix = { Text("₱") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PriceHistoryCard(prices: List<ProductPrice>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Price History",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(prices) { price ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = price.date_created.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            price.per_kg_price?.let {
                                Text(
                                    text = "Kg: ${CurrencyFormatter.format(it)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            price.per_piece_price?.let {
                                Text(
                                    text = "Pc: ${CurrencyFormatter.format(it)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (Product, ProductPrice) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var perKgPrice by remember { mutableStateOf("") }
    var perPiecePrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = perKgPrice,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            perKgPrice = it
                        }
                    },
                    label = { Text("Price per kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = perPiecePrice,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            perPiecePrice = it
                        }
                    },
                    label = { Text("Price per piece") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newProduct = Product(
                        product_id = "", // Empty string for new product
                        product_name = name,
                        product_description = description,
                        unit_type = "", // Add default unit type
                        is_active = true
                    )
                    val newPrice = ProductPrice(
                        product_id = "", // Will be updated after product insertion
                        per_kg_price = perKgPrice.toDoubleOrNull(),
                        per_piece_price = perPiecePrice.toDoubleOrNull(),
                        date_created = LocalDateTime.now()
                    )
                    onSave(newProduct, newPrice)
                },
                enabled = name.isNotEmpty() && (perKgPrice.isNotEmpty() || perPiecePrice.isNotEmpty())
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    onDismiss: () -> Unit,
    onApplyFilters: (ProductFilters) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showOutOfStock by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("name") } // "name", "price"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Products") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = showOutOfStock,
                        onCheckedChange = { showOutOfStock = it }
                    )
                    Text("Show out of stock")
                }

                Text("Sort by:", modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortBy == "name",
                        onClick = { sortBy = "name" },
                        label = { Text("Name") }
                    )
                    FilterChip(
                        selected = sortBy == "price",
                        onClick = { sortBy = "price" },
                        label = { Text("Price") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApplyFilters(
                        ProductFilters(
                            searchQuery = searchQuery,
                            showOutOfStock = showOutOfStock,
                            sortBy = sortBy
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 