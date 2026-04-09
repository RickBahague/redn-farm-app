package com.redn.farm.ui.screens.acquire

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.model.Product
import com.redn.farm.data.repository.AcquisitionDraftPricingPreview
import com.redn.farm.ui.components.NumericPadBottomSheet
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.MillisDateRange
import kotlin.math.abs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class AcquisitionNumericPadTarget {
    QUANTITY,
    PRICE_PER_UNIT,
    TOTAL_AMOUNT,
    SPOILAGE_KG,
    CUSTOM_SRP_ONLINE,
    CUSTOM_SRP_RESELLER,
    CUSTOM_SRP_OFFLINE,
}

private const val SRP_PREVIEW_DEBOUNCE_MS = 200L

/**
 * BUG-ACQ-02: quantity required; then either **total** or **price/unit** (or both).
 * If **total** is given and positive, unit price is **total / quantity** (total wins over a stale price field).
 */
private fun formatSrpField(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

/** BUG-PRC-04: optional absolute unsellable kg (per-kg only). Blank → use preset rate. */
private fun parseOptionalSpoilageKg(isPerKg: Boolean, spoilageKgStr: String): Double? {
    if (!isPerKg) return null
    val s = spoilageKgStr.trim()
    if (s.isEmpty()) return null
    return s.toDoubleOrNull()
}

private fun resolveAcquisitionQuantityPriceTotal(
    quantityStr: String,
    pricePerUnitStr: String,
    totalAmountStr: String,
): Triple<Double, Double, Double>? {
    val q = quantityStr.toDoubleOrNull() ?: return null
    if (q <= 0) return null
    val pRaw = pricePerUnitStr.toDoubleOrNull()
    val tRaw = totalAmountStr.toDoubleOrNull()
    return when {
        tRaw != null && tRaw > 0 -> Triple(q, tRaw / q, tRaw)
        pRaw != null && pRaw > 0 -> Triple(q, pRaw, q * pRaw)
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcquisitionFormScreen(
    acquisitionIdKey: String,
    onNavigateBack: () -> Unit,
    canViewPresetDetail: Boolean,
    onOpenPresetDetail: (String) -> Unit = {},
    viewModel: AcquireProduceViewModel,
) {
    val isNew = acquisitionIdKey == "new"
    val acquisitions by viewModel.acquisitions.collectAsState()
    val products by viewModel.products.collectAsState()

    val existing = remember(acquisitionIdKey, acquisitions) {
        if (isNew) null else acquisitions.find { it.acquisition_id == acquisitionIdKey.toIntOrNull() }
    }

    if (!isNew && acquisitions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isNew && acquisitions.isNotEmpty() && existing == null) {
        LaunchedEffect(acquisitionIdKey, acquisitions) {
            onNavigateBack()
        }
        return
    }

    var selectedProduct by remember(acquisitionIdKey) { mutableStateOf<Product?>(null) }
    var quantity by remember(acquisitionIdKey) { mutableStateOf("") }
    var pricePerUnit by remember(acquisitionIdKey) { mutableStateOf("") }
    var isPerKg by remember(acquisitionIdKey) { mutableStateOf(true) }
    var totalAmount by remember(acquisitionIdKey) { mutableStateOf("") }
    var pieceCountStr by remember(acquisitionIdKey) { mutableStateOf("") }
    var spoilageKgStr by remember(acquisitionIdKey) { mutableStateOf("") }
    var location by remember(acquisitionIdKey) { mutableStateOf(AcquisitionLocation.FARM) }
    var selectedDayMillis by remember(acquisitionIdKey) {
        mutableStateOf(MillisDateRange.startOfDayMillis(System.currentTimeMillis()))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showProductSheet by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }

    var pricingPreview by remember { mutableStateOf<AcquisitionDraftPricingPreview?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var srpPreviewExpanded by remember { mutableStateOf(false) }

    var useCustomCustomerSrp by remember(acquisitionIdKey) { mutableStateOf(false) }
    var customSrpOnline by remember(acquisitionIdKey) { mutableStateOf("") }
    var customSrpReseller by remember(acquisitionIdKey) { mutableStateOf("") }
    var customSrpOffline by remember(acquisitionIdKey) { mutableStateOf("") }

    var numericPadTarget by remember { mutableStateOf<AcquisitionNumericPadTarget?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(existing, products) {
        val acq = existing ?: return@LaunchedEffect
        selectedProduct = products.find { it.product_id == acq.product_id }
            ?: Product(
                product_id = acq.product_id,
                product_name = acq.product_name,
                product_description = "",
                unit_type = if (acq.is_per_kg) "kg" else "piece",
                is_active = true,
            )
        quantity = acq.quantity.toString()
        pricePerUnit = acq.price_per_unit.toString()
        isPerKg = acq.is_per_kg
        totalAmount = acq.total_amount.toString()
        pieceCountStr = acq.piece_count?.toString().orEmpty()
        location = acq.location
        selectedDayMillis = MillisDateRange.startOfDayMillis(acq.date_acquired)
        useCustomCustomerSrp = acq.srp_custom_override
        if (acq.srp_custom_override) {
            acq.srp_online_per_kg?.let { customSrpOnline = formatSrpField(it) }
            acq.srp_reseller_per_kg?.let { customSrpReseller = formatSrpField(it) }
            acq.srp_offline_per_kg?.let { customSrpOffline = formatSrpField(it) }
        }
        spoilageKgStr = acq.spoilage_kg?.toString().orEmpty()
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }
    val filteredProducts = remember(products, productSearchQuery) {
        if (productSearchQuery.isBlank()) products
        else products.filter {
            it.product_name.contains(productSearchQuery, ignoreCase = true) ||
                it.product_description.contains(productSearchQuery, ignoreCase = true)
        }
    }
    val productSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val padVisible = numericPadTarget != null
    val padTitle = when (numericPadTarget) {
        AcquisitionNumericPadTarget.QUANTITY -> "Quantity"
        AcquisitionNumericPadTarget.PRICE_PER_UNIT -> "Price/Unit"
        AcquisitionNumericPadTarget.TOTAL_AMOUNT -> "Total Amount"
        AcquisitionNumericPadTarget.SPOILAGE_KG -> "Unsellable kg (optional)"
        AcquisitionNumericPadTarget.CUSTOM_SRP_ONLINE -> "Online SRP/kg"
        AcquisitionNumericPadTarget.CUSTOM_SRP_RESELLER -> "Reseller SRP/kg"
        AcquisitionNumericPadTarget.CUSTOM_SRP_OFFLINE -> "Store SRP/kg"
        null -> ""
    }
    val padValue = when (numericPadTarget) {
        AcquisitionNumericPadTarget.QUANTITY -> quantity
        AcquisitionNumericPadTarget.PRICE_PER_UNIT -> pricePerUnit
        AcquisitionNumericPadTarget.TOTAL_AMOUNT -> totalAmount
        AcquisitionNumericPadTarget.SPOILAGE_KG -> spoilageKgStr
        AcquisitionNumericPadTarget.CUSTOM_SRP_ONLINE -> customSrpOnline
        AcquisitionNumericPadTarget.CUSTOM_SRP_RESELLER -> customSrpReseller
        AcquisitionNumericPadTarget.CUSTOM_SRP_OFFLINE -> customSrpOffline
        null -> ""
    }
    val padMaxDecimals = when (numericPadTarget) {
        AcquisitionNumericPadTarget.QUANTITY, AcquisitionNumericPadTarget.SPOILAGE_KG -> 3
        AcquisitionNumericPadTarget.PRICE_PER_UNIT, AcquisitionNumericPadTarget.TOTAL_AMOUNT -> 2
        AcquisitionNumericPadTarget.CUSTOM_SRP_ONLINE,
        AcquisitionNumericPadTarget.CUSTOM_SRP_RESELLER,
        AcquisitionNumericPadTarget.CUSTOM_SRP_OFFLINE,
        -> 2
        null -> 2
    }

    val previewDateMillis = selectedDayMillis

    LaunchedEffect(quantity, pricePerUnit, totalAmount) {
        val q = quantity.toDoubleOrNull()
        val p = pricePerUnit.toDoubleOrNull()
        val t = totalAmount.toDoubleOrNull()
        if (q != null && q > 0 && ((p != null && p > 0) || (t != null && t > 0))) {
            srpPreviewExpanded = true
        }
    }

    LaunchedEffect(
        quantity,
        pricePerUnit,
        totalAmount,
        isPerKg,
        pieceCountStr,
        selectedProduct?.product_id,
        previewDateMillis,
        location,
        existing?.acquisition_id,
        useCustomCustomerSrp,
        customSrpOnline,
        customSrpReseller,
        customSrpOffline,
        spoilageKgStr,
    ) {
        previewLoading = true
        delay(SRP_PREVIEW_DEBOUNCE_MS)
        val product = selectedProduct
        val resolved = resolveAcquisitionQuantityPriceTotal(quantity, pricePerUnit, totalAmount)
        if (product == null || resolved == null) {
            pricingPreview = null
            previewLoading = false
            return@LaunchedEffect
        }
        val (q, ppu, total) = resolved
        val pc = if (isPerKg) null else pieceCountStr.toDoubleOrNull()
        if (!isPerKg && pc == null) {
            pricingPreview = null
            previewLoading = false
            return@LaunchedEffect
        }
        val co = customSrpOnline.toDoubleOrNull()
        val cr = customSrpReseller.toDoubleOrNull()
        val cf = customSrpOffline.toDoubleOrNull()
        val draft = Acquisition(
            acquisition_id = existing?.acquisition_id ?: 0,
            product_id = product.product_id,
            product_name = product.product_name,
            quantity = q,
            price_per_unit = ppu,
            total_amount = total,
            is_per_kg = isPerKg,
            piece_count = pc,
            date_acquired = previewDateMillis,
            location = location,
            spoilage_kg = parseOptionalSpoilageKg(isPerKg, spoilageKgStr),
            srp_custom_override = useCustomCustomerSrp,
            srp_online_per_kg = if (useCustomCustomerSrp) co else null,
            srp_reseller_per_kg = if (useCustomCustomerSrp) cr else null,
            srp_offline_per_kg = if (useCustomCustomerSrp) cf else null,
        )
        pricingPreview = withContext(Dispatchers.Default) {
            viewModel.previewDraftPricing(draft)
        }
        previewLoading = false
    }

    val customSrpValid = !useCustomCustomerSrp ||
        (
            (customSrpOnline.toDoubleOrNull() ?: 0.0) > 0 &&
                (customSrpReseller.toDoubleOrNull() ?: 0.0) > 0 &&
                (customSrpOffline.toDoubleOrNull() ?: 0.0) > 0
            )

    val qtyKgForSpoilage = quantity.toDoubleOrNull()?.takeIf { isPerKg && it > 0 }
    val spoilageKgParsed = parseOptionalSpoilageKg(isPerKg, spoilageKgStr)
    val spoilageKgStrHasDigits = spoilageKgStr.isNotBlank()
    val spoilageKgValid = when {
        !isPerKg -> true
        !spoilageKgStrHasDigits -> true
        spoilageKgParsed == null -> false
        qtyKgForSpoilage == null -> false
        spoilageKgParsed < 0 -> false
        spoilageKgParsed >= qtyKgForSpoilage -> false
        else -> true
    }

    val canSave = selectedProduct != null &&
        resolveAcquisitionQuantityPriceTotal(quantity, pricePerUnit, totalAmount) != null &&
        (isPerKg || pieceCountStr.isNotEmpty()) &&
        customSrpValid &&
        spoilageKgValid

    fun performSave() {
        val product = selectedProduct ?: return
        val dateMillis = selectedDayMillis
        val pieceCount = if (isPerKg) null else pieceCountStr.toDoubleOrNull()
        val resolved = resolveAcquisitionQuantityPriceTotal(quantity, pricePerUnit, totalAmount) ?: return
        val (q, ppu, total) = resolved
        val sk = parseOptionalSpoilageKg(isPerKg, spoilageKgStr)
        val co = customSrpOnline.toDoubleOrNull()
        val cr = customSrpReseller.toDoubleOrNull()
        val cf = customSrpOffline.toDoubleOrNull()
        val saved = existing?.copy(
            product_id = product.product_id,
            product_name = product.product_name,
            quantity = q,
            price_per_unit = ppu,
            total_amount = total,
            is_per_kg = isPerKg,
            piece_count = pieceCount,
            date_acquired = dateMillis,
            location = location,
            spoilage_kg = sk,
            srp_custom_override = useCustomCustomerSrp,
            srp_online_per_kg = if (useCustomCustomerSrp) co else null,
            srp_reseller_per_kg = if (useCustomCustomerSrp) cr else null,
            srp_offline_per_kg = if (useCustomCustomerSrp) cf else null,
        ) ?: Acquisition(
            product_id = product.product_id,
            product_name = product.product_name,
            quantity = q,
            price_per_unit = ppu,
            total_amount = total,
            is_per_kg = isPerKg,
            piece_count = pieceCount,
            date_acquired = dateMillis,
            location = location,
            spoilage_kg = sk,
            srp_custom_override = useCustomCustomerSrp,
            srp_online_per_kg = if (useCustomCustomerSrp) co else null,
            srp_reseller_per_kg = if (useCustomCustomerSrp) cr else null,
            srp_offline_per_kg = if (useCustomCustomerSrp) cf else null,
        )
        if (existing != null) viewModel.updateAcquisition(saved) else viewModel.addAcquisition(saved)
        onNavigateBack()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDayMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDayMillis = MillisDateRange.startOfDayMillis(millis)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showProductSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showProductSheet = false
                productSearchQuery = ""
            },
            sheetState = productSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text("Select product", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = { productSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Search") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (productSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { productSearchQuery = "" }) {
                                Icon(Icons.Filled.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Search),
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredProducts, key = { it.product_id }) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProduct = product
                                    if (pieceCountStr.isEmpty()) {
                                        pieceCountStr = product.defaultPieceCount?.toString().orEmpty()
                                    }
                                    showProductSheet = false
                                    productSearchQuery = ""
                                },
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(product.product_name, style = MaterialTheme.typography.titleSmall)
                                if (product.product_description.isNotEmpty()) {
                                    Text(
                                        product.product_description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    NumericPadBottomSheet(
        visible = padVisible,
        title = padTitle,
        value = padValue,
        onValueChange = { newValue ->
            when (numericPadTarget) {
                AcquisitionNumericPadTarget.QUANTITY -> {
                    quantity = newValue
                    val q = newValue.toDoubleOrNull()
                    val t = totalAmount.toDoubleOrNull()
                    val p = pricePerUnit.toDoubleOrNull()
                    when {
                        !newValue.isBlank() && q != null && q > 0 && t != null && t > 0 -> {
                            pricePerUnit = (t / q).toString()
                        }
                        !newValue.isBlank() && q != null && p != null && p > 0 -> {
                            totalAmount = (q * p).toString()
                        }
                        else -> {
                            if (newValue.isBlank() || q == null || q <= 0) {
                                totalAmount = ""
                            }
                        }
                    }
                }
                AcquisitionNumericPadTarget.PRICE_PER_UNIT -> {
                    pricePerUnit = newValue
                    val q = quantity.toDoubleOrNull()
                    val p = newValue.toDoubleOrNull()
                    if (!newValue.isBlank() && q != null && p != null && p > 0) {
                        totalAmount = (q * p).toString()
                    } else if (newValue.isBlank()) {
                        totalAmount = ""
                    }
                }
                AcquisitionNumericPadTarget.TOTAL_AMOUNT -> {
                    totalAmount = newValue
                    val q = quantity.toDoubleOrNull()
                    val t = newValue.toDoubleOrNull()
                    if (!newValue.isBlank() && q != null && q > 0 && t != null && t > 0) {
                        pricePerUnit = (t / q).toString()
                    } else if (newValue.isBlank()) {
                        pricePerUnit = ""
                    }
                }
                AcquisitionNumericPadTarget.SPOILAGE_KG -> {
                    spoilageKgStr = newValue
                }
                AcquisitionNumericPadTarget.CUSTOM_SRP_ONLINE -> {
                    customSrpOnline = newValue
                }
                AcquisitionNumericPadTarget.CUSTOM_SRP_RESELLER -> {
                    customSrpReseller = newValue
                }
                AcquisitionNumericPadTarget.CUSTOM_SRP_OFFLINE -> {
                    customSrpOffline = newValue
                }
                null -> Unit
            }
        },
        decimalEnabled = true,
        maxDecimalPlaces = padMaxDecimals,
        onDismiss = { numericPadTarget = null },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add acquisition" else "Edit acquisition") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { performSave() }, enabled = canSave) {
                        Text("Save")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { performSave() },
                        enabled = canSave,
                    ) {
                        Text("Save acquisition")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedCard(
                onClick = { showProductSheet = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Text("Product", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = selectedProduct?.product_name ?: "Select product",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Date", style = MaterialTheme.typography.labelMedium)
                    Text(
                        dateFormatter.format(
                            Instant.ofEpochMilli(selectedDayMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Text("Location", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AcquisitionLocation.values().forEach { loc ->
                    FilterChip(
                        selected = location == loc,
                        onClick = { location = loc },
                        label = { Text(loc.toString()) },
                    )
                }
            }

            OutlinedTextField(
                value = quantity,
                onValueChange = {},
                label = { Text("Quantity") },
                readOnly = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = {
                        numericPadTarget = AcquisitionNumericPadTarget.QUANTITY
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Filled.Dialpad, contentDescription = "Open numeric pad")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = totalAmount,
                onValueChange = {},
                label = { Text("Total Amount") },
                prefix = { Text("₱") },
                readOnly = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = {
                        numericPadTarget = AcquisitionNumericPadTarget.TOTAL_AMOUNT
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Filled.Dialpad, contentDescription = "Open numeric pad")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = pricePerUnit,
                onValueChange = {},
                label = { Text("Price/Unit") },
                supportingText = {
                    Text("Optional if total is set — computed as total ÷ quantity")
                },
                prefix = { Text("₱") },
                readOnly = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = {
                        numericPadTarget = AcquisitionNumericPadTarget.PRICE_PER_UNIT
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Filled.Dialpad, contentDescription = "Open numeric pad")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Custom customer SRP / channel",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Override preset online · reseller · store SRP/kg for this line",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = useCustomCustomerSrp,
                    onCheckedChange = { useCustomCustomerSrp = it },
                )
            }

            AnimatedVisibility(visible = useCustomCustomerSrp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomSrpPadField(
                        label = "Online SRP/kg",
                        value = customSrpOnline,
                        onOpenPad = {
                            numericPadTarget = AcquisitionNumericPadTarget.CUSTOM_SRP_ONLINE
                            focusManager.clearFocus()
                        },
                    )
                    CustomSrpPadField(
                        label = "Reseller SRP/kg",
                        value = customSrpReseller,
                        onOpenPad = {
                            numericPadTarget = AcquisitionNumericPadTarget.CUSTOM_SRP_RESELLER
                            focusManager.clearFocus()
                        },
                    )
                    CustomSrpPadField(
                        label = "Store (offline) SRP/kg",
                        value = customSrpOffline,
                        onOpenPad = {
                            numericPadTarget = AcquisitionNumericPadTarget.CUSTOM_SRP_OFFLINE
                            focusManager.clearFocus()
                        },
                    )
                }
            }

            AcquisitionDraftPreviewPanel(
                loading = previewLoading,
                preview = pricingPreview,
                presetName = (pricingPreview as? AcquisitionDraftPricingPreview.Ok)?.presetName,
                isPerKg = isPerKg,
                pieceCountStr = pieceCountStr,
                expanded = srpPreviewExpanded,
                onExpandedChange = { srpPreviewExpanded = it },
                isCustomOverride = useCustomCustomerSrp,
            )

            val savedPresetRef = existing?.preset_ref?.trim().orEmpty()
            if (savedPresetRef.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Pricing preset (saved with this lot)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (canViewPresetDetail) {
                        TextButton(onClick = { onOpenPresetDetail(savedPresetRef) }) {
                            Text("Preset: $savedPresetRef")
                        }
                    } else {
                        Text(
                            "Preset: $savedPresetRef",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isPerKg) "Unit - Per kg" else "Unit - Per pc",
                    style = MaterialTheme.typography.labelMedium,
                )
                Switch(
                    checked = isPerKg,
                    onCheckedChange = { checked ->
                        isPerKg = checked
                        if (!checked) spoilageKgStr = ""
                    },
                    modifier = Modifier.height(32.dp),
                )
            }
            AnimatedVisibility(visible = isPerKg) {
                OutlinedTextField(
                    value = spoilageKgStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unsellable kg (optional)") },
                    supportingText = {
                        Text("Leave empty to use preset spoilage rate. Applies when unit is per kg.")
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            numericPadTarget = AcquisitionNumericPadTarget.SPOILAGE_KG
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Filled.Dialpad, contentDescription = "Open numeric pad")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(visible = !isPerKg) {
                OutlinedTextField(
                    value = pieceCountStr,
                    onValueChange = { s ->
                        val decimalRegex = Regex("^\\d*(\\.\\d*)?$")
                        val ok = s.isEmpty() || (s != "." && s.matches(decimalRegex))
                        if (ok) {
                            pieceCountStr = s
                        }
                    },
                    label = { Text("Pieces per kg") },
                    supportingText = {
                        Text("Used to convert total piece count into kg for pricing")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CustomSrpPadField(
    label: String,
    value: String,
    onOpenPad: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        prefix = { Text("₱") },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = onOpenPad) {
                Icon(Icons.Filled.Dialpad, contentDescription = "Open numeric pad")
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AcquisitionDraftPreviewPanel(
    loading: Boolean,
    preview: AcquisitionDraftPricingPreview?,
    presetName: String?,
    isPerKg: Boolean,
    pieceCountStr: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isCustomOverride: Boolean,
) {
    fun fmt(v: Double) = CurrencyFormatter.format(v)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse SRP preview" else "Expand SRP preview",
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = buildString {
                            append("SRP Preview")
                            if (!presetName.isNullOrBlank()) append(" (Preset: $presetName)")
                            if (isCustomOverride) append(" · Custom / channel")
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when {
                        loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        preview == null -> Text(
                            text = "Enter quantity and total (or price/unit) to preview SRPs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        preview is AcquisitionDraftPricingPreview.NoActivePreset -> Text(
                            text = "No active preset — SRPs will not be computed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        preview is AcquisitionDraftPricingPreview.Invalid -> Text(
                            text = preview.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        preview is AcquisitionDraftPricingPreview.Ok -> {
                            val o = preview.output
                            val spoilPct = o.spoilageRate * 100.0
                            val spoilPctStr = if (abs(spoilPct - spoilPct.toInt()) < 1e-6) {
                                "${spoilPct.toInt()}"
                            } else {
                                "%.1f".format(spoilPct)
                            }
                            val spoilLine = when {
                                !isPerKg -> "spoilage not applied (per piece)"
                                o.spoilageAbsoluteKg == null ->
                                    "spoilage $spoilPctStr% (preset rate)"
                                else ->
                                    "unsellable ${"%.2f".format(o.spoilageAbsoluteKg)} kg (~$spoilPctStr% of bulk)"
                            }

                            Text(
                                text = buildString {
                                    append("Q ${"%.2f".format(o.bulkQuantityKg)} kg · ")
                                    append(spoilLine)
                                    append(" · sellable ${"%.2f".format(o.sellableQuantityKg)} kg")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "C_bulk ${fmt(o.costPerSellableKg)}/kg + A ${fmt(o.additionalCostPerKg)}/kg = C ${fmt(o.coreCostPerKg)}/kg",
                                style = MaterialTheme.typography.bodySmall,
                            )

                            if (isPerKg) {
                                Text(
                                    text = "Online: ${fmt(o.srpOnlinePerKg)}/kg · ${fmt(o.srpOnline500g)}/500g · ${fmt(o.srpOnline250g)}/250g · ${fmt(o.srpOnline100g)}/100g",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "Reseller: ${fmt(o.srpResellerPerKg)}/kg · ${fmt(o.srpReseller500g)}/500g · ${fmt(o.srpReseller250g)}/250g · ${fmt(o.srpReseller100g)}/100g",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "Offline: ${fmt(o.srpOfflinePerKg)}/kg · ${fmt(o.srpOffline500g)}/500g · ${fmt(o.srpOffline250g)}/250g · ${fmt(o.srpOffline100g)}/100g",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                val pcPerKg = pieceCountStr?.toDoubleOrNull()
                                Text(
                                    text = "Online: ${o.srpOnlinePerPiece?.let { fmt(it) } ?: "—"}/pc · ref ${fmt(o.srpOnlinePerKg)}/kg",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "Reseller: ${o.srpResellerPerPiece?.let { fmt(it) } ?: "—"}/pc · ref ${fmt(o.srpResellerPerKg)}/kg",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "Offline: ${o.srpOfflinePerPiece?.let { fmt(it) } ?: "—"}/pc · ref ${fmt(o.srpOfflinePerKg)}/kg",
                                    style = MaterialTheme.typography.bodySmall,
                                )

                                if (pcPerKg != null && pcPerKg > 0) {
                                    Text(
                                        text = "Pieces per kg: ${pieceCountStr} pcs/kg",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
