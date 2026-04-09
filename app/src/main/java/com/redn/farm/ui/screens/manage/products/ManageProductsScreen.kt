package com.redn.farm.ui.screens.manage.products

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.pricing.OrderPricingResolver
import kotlinx.coroutines.launch
import com.redn.farm.utils.CurrencyFormatter
import androidx.compose.foundation.clickable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.redn.farm.data.model.ProductActiveStatusFilter
import com.redn.farm.data.model.ProductFilters
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import com.redn.farm.ui.components.NumericPadBottomSheet

private enum class FallbackPadTarget { PER_KG, PER_PIECE }

private val productPriceDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd, yyyy")

private fun formatProductPriceDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(productPriceDateFormatter)

/** True when any manual fallback field is a positive price (latest row only — **PRD-US-01**). */
private fun hasManualFallbackPrice(price: ProductPrice?): Boolean {
    if (price == null) return false
    return listOfNotNull(
        price.per_kg_price,
        price.per_piece_price,
        price.discounted_per_kg_price,
        price.discounted_per_piece_price
    ).any { it > 0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProductForm: (String) -> Unit,
    onNavigateToPriceHistory: (String) -> Unit,
    viewModel: ManageProductsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val products by viewModel.products.collectAsState()
    val productPrices by viewModel.productPrices.collectAsState()
    val activeAcquisitionByProductId by viewModel.activeAcquisitionByProductId.collectAsState()
    val canMutate by viewModel.canMutateProducts.collectAsState()
    var pendingDelete by remember { mutableStateOf<Product?>(null) }
    var fallbackPriceTarget by remember { mutableStateOf<Product?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var activeFilters by remember { mutableStateOf<ProductFilters?>(null) }
    var filteredProducts by remember { mutableStateOf<List<Product>>(emptyList()) }

    val displayProducts = activeFilters?.let { filteredProducts } ?: products

    LaunchedEffect(activeFilters) {
        if (activeFilters == null) {
            filteredProducts = emptyList()
            return@LaunchedEffect
        }
        viewModel.getFilteredProductsFlow(activeFilters!!).collect { newProducts ->
            filteredProducts = newProducts
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    if (canMutate) {
                        IconButton(onClick = { onNavigateToProductForm("new") }) {
                            Icon(Icons.Default.Add, "Add Product")
                        }
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
                items(displayProducts) { product ->
                    val price = productPrices.find { it.product_id == product.product_id }
                    ProductCard(
                        product = product,
                        productPrice = price,
                        activeAcquisition = activeAcquisitionByProductId[product.product_id],
                        canMutate = canMutate,
                        onEditClick = { onNavigateToProductForm(product.product_id) },
                        onDeleteClick = { pendingDelete = product },
                        onSetFallbackPriceClick = { fallbackPriceTarget = product },
                        onPriceHistoryClick = { onNavigateToPriceHistory(product.product_id) },
                    )
                }
            }

        }

        pendingDelete?.let { product ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete product?") },
                text = {
                    Text(
                        "This will permanently delete “${product.product_name}”. " +
                            "If this product is referenced by orders or acquisitions, delete may fail."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    viewModel.deleteProduct(product.product_id)
                                    snackbarHostState.showSnackbar("Deleted: ${product.product_name}")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        e.message ?: "Could not delete product (it may be linked to existing data)."
                                    )
                                } finally {
                                    pendingDelete = null
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        fallbackPriceTarget?.let { product ->
            val current = productPrices.find { it.product_id == product.product_id }
            SetFallbackPriceSheet(
                product = product,
                currentPrice = current,
                onDismiss = { fallbackPriceTarget = null },
                onSave = { perKg, perPiece ->
                    scope.launch {
                        try {
                            viewModel.insertProductPrice(
                                ProductPrice(
                                    product_id = product.product_id,
                                    per_kg_price = perKg,
                                    per_piece_price = perPiece,
                                    discounted_per_kg_price = null,
                                    discounted_per_piece_price = null,
                                    date_created = System.currentTimeMillis()
                                )
                            )
                            snackbarHostState.showSnackbar("Saved fallback price: ${product.product_name}")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                e.message ?: "Could not save fallback price."
                            )
                        } finally {
                            fallbackPriceTarget = null
                        }
                    }
                }
            )
        }

        if (showFilters) {
            FilterDialog(
                onDismiss = { showFilters = false },
                onApplyFilters = { filters ->
                    activeFilters = filters
                    showFilters = false
                }
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    productPrice: ProductPrice?,
    activeAcquisition: Acquisition?,
    canMutate: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSetFallbackPriceClick: () -> Unit,
    onPriceHistoryClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canMutate, onClick = onEditClick)
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
                        text = product.product_name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = product.product_id,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(product.unit_type)
                            product.category?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (product.product_description.isNotBlank()) {
                        Text(
                            text = product.product_description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!product.is_active) {
                        AssistChip(
                            onClick = if (canMutate) onEditClick else { {} },
                            enabled = canMutate,
                            label = { Text("Inactive") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onPriceHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Price and SRP history",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (canMutate) {
                        IconButton(onClick = onSetFallbackPriceClick) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = "Set fallback price",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            // PRD-US-01: show acquisition SRP summary on the list; do not show manual peso amounts here (PRD-US-05).
            val srpSummary = activeAcquisition?.let { OrderPricingResolver.catalogSrpSummaryAmounts(it) }
            val hasAcquisitionSrp = srpSummary != null
            val manualFallbackOnly = !hasAcquisitionSrp && hasManualFallbackPrice(productPrice)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when {
                    hasAcquisitionSrp && srpSummary != null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            srpSummary.minPerKg?.let {
                                Text(
                                    text = "From ${CurrencyFormatter.format(it)}/kg",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            srpSummary.minPerPiece?.let {
                                Text(
                                    text = "From ${CurrencyFormatter.format(it)}/pc",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("Acquisition SRP") },
                        )
                    }
                    manualFallbackOnly -> {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("Manual price") },
                        )
                        Text(
                            text = "Open product to set or view fallback prices in history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("No price") },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetFallbackPriceSheet(
    product: Product,
    currentPrice: ProductPrice?,
    onDismiss: () -> Unit,
    onSave: (perKg: Double?, perPiece: Double?) -> Unit
) {
    var perKgStr by remember(product.product_id) { mutableStateOf(currentPrice?.per_kg_price?.toString().orEmpty()) }
    var perPieceStr by remember(product.product_id) { mutableStateOf(currentPrice?.per_piece_price?.toString().orEmpty()) }
    var numericPadTarget by remember { mutableStateOf<FallbackPadTarget?>(null) }
    val focusManager = LocalFocusManager.current

    val padVisible = numericPadTarget != null
    val padTitle = when (numericPadTarget) {
        FallbackPadTarget.PER_KG -> "Fallback price (per kg)"
        FallbackPadTarget.PER_PIECE -> "Fallback price (per piece)"
        null -> ""
    }
    val padValue = when (numericPadTarget) {
        FallbackPadTarget.PER_KG -> perKgStr
        FallbackPadTarget.PER_PIECE -> perPieceStr
        null -> ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Set fallback price",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = product.product_name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Manual fallback — used when no acquisition SRP exists.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = perKgStr,
                onValueChange = {},
                readOnly = true,
                label = { Text("Per kg (optional)") },
                prefix = { Text("₱") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        numericPadTarget = FallbackPadTarget.PER_KG
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Filled.Dialpad, contentDescription = "Open numeric pad")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = perPieceStr,
                onValueChange = {},
                readOnly = true,
                label = { Text("Per piece (optional)") },
                prefix = { Text("₱") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        numericPadTarget = FallbackPadTarget.PER_PIECE
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Filled.Dialpad, contentDescription = "Open numeric pad")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            perKgStr.toDoubleOrNull(),
                            perPieceStr.toDoubleOrNull()
                        )
                    },
                    enabled = perKgStr.toDoubleOrNull() != null || perPieceStr.toDoubleOrNull() != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }

    NumericPadBottomSheet(
        visible = padVisible,
        title = padTitle,
        value = padValue,
        onValueChange = { v ->
            when (numericPadTarget) {
                FallbackPadTarget.PER_KG -> perKgStr = v
                FallbackPadTarget.PER_PIECE -> perPieceStr = v
                null -> Unit
            }
        },
        decimalEnabled = true,
        maxDecimalPlaces = 2,
        onDismiss = { numericPadTarget = null }
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
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
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
                            date_created = System.currentTimeMillis()
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
                            text = formatProductPriceDate(price.date_created),
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
private fun FilterDialog(
    onDismiss: () -> Unit,
    onApplyFilters: (ProductFilters) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("name") } // "name", "price"
    var unitTypeFilter by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var activeStatus by remember { mutableStateOf(ProductActiveStatusFilter.ACTIVE_ONLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Products") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search name, ID, or description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Search),
                )
                OutlinedTextField(
                    value = unitTypeFilter,
                    onValueChange = { unitTypeFilter = it },
                    label = { Text("Unit type contains (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. kg, piece") },
                    singleLine = true,
                    keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = categoryFilter,
                    onValueChange = { categoryFilter = it },
                    label = { Text("Category contains (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Done),
                )

                Text("Active status", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = activeStatus == ProductActiveStatusFilter.ALL,
                        onClick = { activeStatus = ProductActiveStatusFilter.ALL },
                        label = { Text("All") },
                    )
                    FilterChip(
                        selected = activeStatus == ProductActiveStatusFilter.ACTIVE_ONLY,
                        onClick = { activeStatus = ProductActiveStatusFilter.ACTIVE_ONLY },
                        label = { Text("Active") },
                    )
                    FilterChip(
                        selected = activeStatus == ProductActiveStatusFilter.INACTIVE_ONLY,
                        onClick = { activeStatus = ProductActiveStatusFilter.INACTIVE_ONLY },
                        label = { Text("Inactive") },
                    )
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
                            sortBy = sortBy,
                            unitTypeFilter = unitTypeFilter,
                            categoryFilter = categoryFilter,
                            activeStatus = activeStatus,
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