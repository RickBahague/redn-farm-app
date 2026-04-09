package com.redn.farm.ui.screens.farmops

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.data.model.FarmOperationType
import com.redn.farm.data.model.Product
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import com.redn.farm.utils.MillisDateRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FARM_OP_WEATHER_OPTIONS = listOf("Hot/Dry", "Rainy", "Cloudy")

private fun normalizeFarmOpWeather(saved: String): String {
    val t = saved.trim()
    if (t in FARM_OP_WEATHER_OPTIONS) return t
    val lower = t.lowercase(Locale.getDefault())
    return when {
        lower.contains("rain") -> "Rainy"
        lower.contains("cloud") -> "Cloudy"
        lower.contains("hot") || lower.contains("dry") -> "Hot/Dry"
        else -> FARM_OP_WEATHER_OPTIONS.first()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmOperationFormScreen(
    operationIdKey: String,
    onNavigateBack: () -> Unit,
    viewModel: FarmOperationsViewModel
) {
    val isNew = operationIdKey == "new"
    val operations by viewModel.operations.collectAsState()
    val products by viewModel.products.collectAsState()

    val existing = remember(operationIdKey, operations) {
        if (isNew) null else operations.find { it.operation_id == operationIdKey.toIntOrNull() }
    }

    if (!isNew && operations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isNew && operations.isNotEmpty() && existing == null) {
        LaunchedEffect(operationIdKey, operations) {
            onNavigateBack()
        }
        return
    }

    var selectedType by remember(operationIdKey) { mutableStateOf(FarmOperationType.SOWING) }
    var details by remember(operationIdKey) { mutableStateOf("") }
    var area by remember(operationIdKey) { mutableStateOf("") }
    var weather by remember(operationIdKey) {
        mutableStateOf(if (isNew) FARM_OP_WEATHER_OPTIONS.first() else "")
    }
    var personnel by remember(operationIdKey) {
        mutableStateOf(
            if (operationIdKey == "new") viewModel.loggedInUsernameOrEmpty() else ""
        )
    }
    var operationDateMillis by remember(operationIdKey) {
        mutableStateOf(MillisDateRange.startOfDayMillis(System.currentTimeMillis()))
    }
    var selectedProduct by remember(operationIdKey) { mutableStateOf<Product?>(null) }
    var productSectionExpanded by remember(operationIdKey) { mutableStateOf(false) }
    var showProductSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember(operationIdKey) { mutableStateOf(false) }
    var weatherMenuExpanded by remember(operationIdKey) { mutableStateOf(false) }

    LaunchedEffect(showProductSheet) {
        if (showProductSheet) {
            searchQuery = ""
            viewModel.refreshProductListFromDb()
        }
    }

    LaunchedEffect(existing, products) {
        val op = existing ?: return@LaunchedEffect
        selectedType = op.operation_type
        details = op.details
        area = op.area
        weather = normalizeFarmOpWeather(op.weather_condition)
        personnel = op.personnel
        operationDateMillis = MillisDateRange.startOfDayMillis(op.operation_date)
        selectedProduct = op.product_id?.let { pid -> products.find { it.product_id == pid } }
        productSectionExpanded = op.product_id != null
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.product_name.contains(searchQuery, ignoreCase = true) ||
                it.product_description.contains(searchQuery, ignoreCase = true)
        }
    }
    val focusManager = LocalFocusManager.current

    val canSave = details.isNotBlank()

    fun performSave() {
        if (!canSave) return
        val now = System.currentTimeMillis()
        val op = FarmOperation(
            operation_id = existing?.operation_id ?: 0,
            operation_type = selectedType,
            operation_date = operationDateMillis,
            details = details,
            area = area,
            weather_condition = weather,
            personnel = personnel,
            product_id = selectedProduct?.product_id,
            product_name = selectedProduct?.product_name ?: "",
            date_created = existing?.date_created ?: now,
            date_updated = now
        )
        if (isNew) viewModel.addOperation(op) else viewModel.updateOperation(op)
        onNavigateBack()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = operationDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            operationDateMillis = MillisDateRange.startOfDayMillis(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showProductSheet) {
        Dialog(
            onDismissRequest = {
                showProductSheet = false
                searchQuery = ""
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 520.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                Text("Select product", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Search),
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        item {
                            Text(
                                text = if (products.isEmpty()) {
                                    "No products yet. Add them under Manage Products."
                                } else {
                                    "No products match your search."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(filteredProducts, key = { it.product_id }) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProduct = product
                                        showProductSheet = false
                                        searchQuery = ""
                                    }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(product.product_name, style = MaterialTheme.typography.titleSmall)
                                    if (product.product_description.isNotEmpty()) {
                                        Text(
                                            product.product_description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Log operation" else "Edit operation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { performSave() }, enabled = canSave) {
                        Text("Save")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { performSave() },
                        enabled = canSave
                    ) {
                        Text("Save operation")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Operation type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    FarmOperationType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.toString()) },
                            onClick = {
                                selectedType = type
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Date", style = MaterialTheme.typography.labelMedium)
                    Text(
                        dateFormatter.withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(operationDateMillis)),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Details") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Default),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Area") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )
                ExposedDropdownMenuBox(
                    expanded = weatherMenuExpanded,
                    onExpandedChange = { weatherMenuExpanded = !weatherMenuExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = weather,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Weather") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = weatherMenuExpanded)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = weatherMenuExpanded,
                        onDismissRequest = { weatherMenuExpanded = false }
                    ) {
                        FARM_OP_WEATHER_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    weather = option
                                    weatherMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = personnel,
                onValueChange = { personnel = it },
                label = { Text("Personnel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { productSectionExpanded = !productSectionExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Related product (optional)", style = MaterialTheme.typography.titleSmall)
                Icon(
                    imageVector = if (productSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = productSectionExpanded) {
                OutlinedCard(
                    onClick = { showProductSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            selectedProduct?.product_name ?: "Tap to select product",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
