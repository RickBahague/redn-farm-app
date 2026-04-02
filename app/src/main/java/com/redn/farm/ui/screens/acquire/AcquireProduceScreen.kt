package com.redn.farm.ui.screens.acquire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.repository.AcquisitionDraftPricingPreview
import com.redn.farm.data.model.Product
import com.redn.farm.utils.CurrencyFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.style.TextOverflow
import java.time.ZoneId
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcquireProduceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AcquireProduceViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showProductSelection by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var acquisitionToEdit by remember { mutableStateOf<Acquisition?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    val acquisitions by viewModel.acquisitions.collectAsState()
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    // Determine screen width for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp

    // Add product selection dialog
    if (showProductSelection) {
        ProductSelectionDialog(
            products = products,
            onProductSelected = { product ->
                selectedProduct = product
                showProductSelection = false
            },
            onDismiss = { showProductSelection = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Acquire Produce") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.FilterList else Icons.Default.FilterAlt,
                            contentDescription = "Filters"
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Acquisition")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showFilters) {
                AcquireProduceFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    dateRange = selectedDateRange,
                    onDateRangeSelected = { viewModel.updateDateRange(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Acquisitions grid/list based on screen width
            if (isWideScreen) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = acquisitions,
                        key = { it.acquisition_id }
                    ) { acquisition ->
                        key(acquisition.acquisition_id) {
                            AcquisitionCard(
                                acquisition = acquisition,
                                onDelete = { viewModel.deleteAcquisition(acquisition) },
                                onEdit = {
                                    acquisitionToEdit = acquisition
                                    selectedProduct = products.find { it.product_id == acquisition.product_id }
                                        ?: Product(
                                            product_id = acquisition.product_id,
                                            product_name = acquisition.product_name,
                                            product_description = "",
                                            unit_type = if (acquisition.is_per_kg) "kg" else "piece",
                                            is_active = true
                                        )
                                    showAddDialog = true
                                }
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = acquisitions,
                        key = { it.acquisition_id }
                    ) { acquisition ->
                        key(acquisition.acquisition_id) {
                            AcquisitionCard(
                                acquisition = acquisition,
                                onDelete = { viewModel.deleteAcquisition(acquisition) },
                                onEdit = {
                                    acquisitionToEdit = acquisition
                                    selectedProduct = products.find { it.product_id == acquisition.product_id }
                                        ?: Product(
                                            product_id = acquisition.product_id,
                                            product_name = acquisition.product_name,
                                            product_description = "",
                                            unit_type = if (acquisition.is_per_kg) "kg" else "piece",
                                            is_active = true
                                        )
                                    showAddDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AcquisitionDialog(
                selectedProduct = selectedProduct,
                acquisitionToEdit = acquisitionToEdit,
                onSelectProduct = { showProductSelection = true },
                previewDraft = { viewModel.previewDraftPricing(it) },
                onDismiss = { 
                    showAddDialog = false 
                    selectedProduct = null
                    acquisitionToEdit = null 
                },
                onSave = { acquisition ->
                    if (acquisitionToEdit != null) {
                        viewModel.updateAcquisition(acquisition)
                    } else {
                        viewModel.addAcquisition(acquisition)
                    }
                    showAddDialog = false
                    selectedProduct = null
                    acquisitionToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcquisitionDialog(
    selectedProduct: Product?,
    acquisitionToEdit: Acquisition? = null,
    onSelectProduct: () -> Unit,
    previewDraft: suspend (Acquisition) -> AcquisitionDraftPricingPreview,
    onDismiss: () -> Unit,
    onSave: (Acquisition) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }

    key(acquisitionToEdit?.acquisition_id ?: -1, selectedProduct?.product_id.orEmpty()) {
        var quantity by remember { mutableStateOf(acquisitionToEdit?.quantity?.toString() ?: "") }
        var pricePerUnit by remember { mutableStateOf(acquisitionToEdit?.price_per_unit?.toString() ?: "") }
        var isPerKg by remember { mutableStateOf(acquisitionToEdit?.is_per_kg ?: true) }
        var totalAmount by remember { mutableStateOf(acquisitionToEdit?.total_amount?.toString() ?: "") }
        var pieceCountStr by remember {
            mutableStateOf(
                acquisitionToEdit?.piece_count?.toString()
                    ?: selectedProduct?.defaultPieceCount?.toString().orEmpty()
            )
        }
        var location by remember { mutableStateOf(acquisitionToEdit?.location ?: AcquisitionLocation.FARM) }
        var expanded by remember { mutableStateOf(false) }

        var selectedDate by remember {
            mutableStateOf(
                if (acquisitionToEdit != null) {
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(acquisitionToEdit.date_acquired),
                        ZoneId.systemDefault()
                    )
                } else {
                    LocalDateTime.now()
                }
            )
        }

        var showDatePicker by remember { mutableStateOf(false) }

        var pricingPreview by remember { mutableStateOf<AcquisitionDraftPricingPreview?>(null) }
        var previewLoading by remember { mutableStateOf(false) }

        val previewDateMillis = selectedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        LaunchedEffect(
            quantity,
            pricePerUnit,
            totalAmount,
            isPerKg,
            pieceCountStr,
            selectedProduct?.product_id,
            previewDateMillis,
            location,
            acquisitionToEdit?.acquisition_id
        ) {
            previewLoading = true
            delay(280)
            val product = selectedProduct
            val q = quantity.toDoubleOrNull()
            val ppu = pricePerUnit.toDoubleOrNull()
            val total = totalAmount.toDoubleOrNull()
            if (product == null || q == null || ppu == null || total == null || q <= 0 || total <= 0) {
                pricingPreview = null
                previewLoading = false
                return@LaunchedEffect
            }
            val pc = if (isPerKg) null else pieceCountStr.toIntOrNull()
            if (!isPerKg && pc == null) {
                pricingPreview = null
                previewLoading = false
                return@LaunchedEffect
            }
            val draft = Acquisition(
                acquisition_id = acquisitionToEdit?.acquisition_id ?: 0,
                product_id = product.product_id,
                product_name = product.product_name,
                quantity = q,
                price_per_unit = ppu,
                total_amount = total,
                is_per_kg = isPerKg,
                piece_count = pc,
                date_acquired = previewDateMillis,
                location = location
            )
            pricingPreview = previewDraft(draft)
            previewLoading = false
        }

        AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (acquisitionToEdit == null) "Add Acquisition" else "Edit Acquisition") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Selection
                OutlinedCard(
                    onClick = onSelectProduct,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Product",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = selectedProduct?.product_name ?: "Select product",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Date and Location Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Selection
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = dateFormatter.format(selectedDate),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Location Selection
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = location.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Location") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            AcquisitionLocation.values().forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc.toString()) },
                                    onClick = {
                                        location = loc
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Quantity and Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                quantity = it
                                if (it.isNotEmpty() && pricePerUnit.isNotEmpty()) {
                                    totalAmount = (it.toDouble() * pricePerUnit.toDouble()).toString()
                                }
                            }
                        },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pricePerUnit,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                pricePerUnit = it
                                if (it.isNotEmpty() && quantity.isNotEmpty()) {
                                    totalAmount = (it.toDouble() * quantity.toDouble()).toString()
                                }
                            }
                        },
                        label = { Text("Price/Unit") },
                        prefix = { Text("₱") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Total Amount
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            totalAmount = it
                        }
                    },
                    label = { Text("Total Amount") },
                    prefix = { Text("₱") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AcquisitionDraftPreviewPanel(
                    loading = previewLoading,
                    preview = pricingPreview
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = if (isPerKg) "Unit - Per kg" else "Unit - Per pc",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Switch(
                        checked = isPerKg,
                        onCheckedChange = { isPerKg = it },
                        modifier = Modifier.height(32.dp)
                    )
                }
                AnimatedVisibility(visible = !isPerKg) {
                    OutlinedTextField(
                        value = pieceCountStr,
                        onValueChange = { s ->
                            if (s.isEmpty() || s.all { it.isDigit() }) {
                                pieceCountStr = s
                            }
                        },
                        label = { Text("Pieces per kg") },
                        supportingText = {
                            Text("Used to convert total piece count into kg for pricing")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedProduct?.let { product ->
                            val dateMillis =
                                selectedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val pieceCount = if (isPerKg) null else pieceCountStr.toIntOrNull()
                            val saved = acquisitionToEdit?.copy(
                                product_id = product.product_id,
                                product_name = product.product_name,
                                quantity = quantity.toDoubleOrNull() ?: 0.0,
                                price_per_unit = pricePerUnit.toDoubleOrNull() ?: 0.0,
                                total_amount = totalAmount.toDoubleOrNull() ?: 0.0,
                                is_per_kg = isPerKg,
                                piece_count = pieceCount,
                                date_acquired = dateMillis,
                                location = location
                            ) ?: Acquisition(
                                product_id = product.product_id,
                                product_name = product.product_name,
                                quantity = quantity.toDoubleOrNull() ?: 0.0,
                                price_per_unit = pricePerUnit.toDoubleOrNull() ?: 0.0,
                                total_amount = totalAmount.toDoubleOrNull() ?: 0.0,
                                is_per_kg = isPerKg,
                                piece_count = pieceCount,
                                date_acquired = dateMillis,
                                location = location
                            )
                            onSave(saved)
                        }
                    },
                    enabled = selectedProduct != null &&
                        quantity.isNotEmpty() &&
                        pricePerUnit.isNotEmpty() &&
                        totalAmount.isNotEmpty() &&
                        (isPerKg || pieceCountStr.isNotEmpty())
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

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
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
                DatePicker(
                    state = rememberDatePickerState(
                        initialSelectedDateMillis = selectedDate
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    ),
                    showModeToggle = false
                )
            }
        }
    }
}

@Composable
private fun AcquisitionDraftPreviewPanel(
    loading: Boolean,
    preview: AcquisitionDraftPricingPreview?
) {
    fun fmt(v: Double) = CurrencyFormatter.format(v)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "SRP preview (active preset)",
                style = MaterialTheme.typography.labelMedium
            )
            when {
                loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                preview == null -> Text(
                    text = "Enter quantity, price, and total to preview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                preview is AcquisitionDraftPricingPreview.NoActivePreset -> Text(
                    text = "No active pricing preset — save will store cost without computed SRPs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                preview is AcquisitionDraftPricingPreview.Invalid -> Text(
                    text = preview.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                preview is AcquisitionDraftPricingPreview.Ok -> {
                    val o = preview.output
                    Text(
                        text = "Sellable ${"%.2f".format(o.sellableQuantityKg)} kg · cost/kg " + fmt(o.costPerSellableKg),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Per kg — Online ${fmt(o.srpOnlinePerKg)} · Reseller ${fmt(o.srpResellerPerKg)} · Store ${fmt(o.srpOfflinePerKg)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Online packs — 500g ${fmt(o.srpOnline500g)} · 250g ${fmt(o.srpOnline250g)} · 100g ${fmt(o.srpOnline100g)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Reseller packs — 500g ${fmt(o.srpReseller500g)} · 250g ${fmt(o.srpReseller250g)} · 100g ${fmt(o.srpReseller100g)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Store packs — 500g ${fmt(o.srpOffline500g)} · 250g ${fmt(o.srpOffline250g)} · 100g ${fmt(o.srpOffline100g)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val op = o.srpOnlinePerPiece
                    val rp = o.srpResellerPerPiece
                    val fp = o.srpOfflinePerPiece
                    if (op != null && rp != null && fp != null) {
                        Text(
                            text = "Per piece — Online ${fmt(op)} · Reseller ${fmt(rp)} · Store ${fmt(fp)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AcquisitionCard(
    acquisition: Acquisition,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var srpExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(acquisition.date_acquired) {
        val instant = Instant.ofEpochMilli(acquisition.date_acquired)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        dateFormatter.format(dateTime)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = acquisition.product_name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Text(
                            text = "Quantity: ${acquisition.quantity} ${if (acquisition.is_per_kg) "kg" else "pcs"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = CurrencyFormatter.format(acquisition.total_amount),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val srpLine = when {
                            !acquisition.is_per_kg && acquisition.srp_online_per_piece != null ->
                                "SRP online: ${CurrencyFormatter.format(acquisition.srp_online_per_piece)}/pc"
                            acquisition.srp_online_per_kg != null ->
                                "SRP online: ${CurrencyFormatter.format(acquisition.srp_online_per_kg)}/kg"
                            else -> null
                        }
                        if (srpLine != null) {
                            Text(
                                text = srpLine,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (acquisition.hasSrpDetail()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { srpExpanded = !srpExpanded }
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All channel SRPs",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Icon(
                                    imageVector = if (srpExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (srpExpanded) "Collapse" else "Expand",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            AnimatedVisibility(visible = srpExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    acquisition.srp_online_per_kg?.let {
                                        Text("Online ${CurrencyFormatter.format(it)}/kg", style = MaterialTheme.typography.bodySmall)
                                    }
                                    acquisition.srp_reseller_per_kg?.let {
                                        Text("Reseller ${CurrencyFormatter.format(it)}/kg", style = MaterialTheme.typography.bodySmall)
                                    }
                                    acquisition.srp_offline_per_kg?.let {
                                        Text("Store ${CurrencyFormatter.format(it)}/kg", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        "Online packs: 500g ${acquisition.srp_online_500g?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                                            "250g ${acquisition.srp_online_250g?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                                            "100g ${acquisition.srp_online_100g?.let { CurrencyFormatter.format(it) } ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Reseller packs: 500g ${acquisition.srp_reseller_500g?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                                            "250g ${acquisition.srp_reseller_250g?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                                            "100g ${acquisition.srp_reseller_100g?.let { CurrencyFormatter.format(it) } ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Store packs: 500g ${acquisition.srp_offline_500g?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                                            "250g ${acquisition.srp_offline_250g?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                                            "100g ${acquisition.srp_offline_100g?.let { CurrencyFormatter.format(it) } ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    val op = acquisition.srp_online_per_piece
                                    val rp = acquisition.srp_reseller_per_piece
                                    val fp = acquisition.srp_offline_per_piece
                                    if (op != null || rp != null || fp != null) {
                                        Text(
                                            "Per piece — online ${op?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                                                "reseller ${rp?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                                                "store ${fp?.let { CurrencyFormatter.format(it) } ?: "—"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = acquisition.location.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun Acquisition.hasSrpDetail(): Boolean =
    srp_online_per_kg != null || srp_reseller_per_kg != null || srp_offline_per_kg != null ||
        srp_online_500g != null || srp_online_250g != null || srp_online_100g != null ||
        srp_reseller_500g != null || srp_reseller_250g != null || srp_reseller_100g != null ||
        srp_offline_500g != null || srp_offline_250g != null || srp_offline_100g != null ||
        srp_online_per_piece != null

@Composable
private fun ProductSelectionDialog(
    products: List<Product>,
    onProductSelected: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Product") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    ProductSelectionItem(
                        product = product,
                        onClick = { onProductSelected(product) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ProductSelectionItem(
    product: Product,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Column {
            Text(
                text = product.product_name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (product.product_description.isNotEmpty()) {
                Text(
                    text = product.product_description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
} 