package com.redn.farm.ui.screens.acquire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Acquisition
import com.redn.farm.utils.buildAcquisitionBatchReport
import com.redn.farm.utils.buildAcquisitionReceivingSlip
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import java.time.ZoneId
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcquireProduceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAcquisitionForm: (String) -> Unit,
    viewModel: AcquireProduceViewModel = hiltViewModel()
) {
    var showFilters by remember { mutableStateOf(false) }
    
    val acquisitions by viewModel.acquisitions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    // Determine screen width for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp

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
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (acquisitions.isEmpty()) {
                                    snackbarHostState.showSnackbar("Nothing to print — adjust filters.")
                                    return@launch
                                }
                                val content = buildAcquisitionBatchReport(
                                    acquisitions = acquisitions,
                                    searchQuery = searchQuery,
                                    locationFilter = selectedLocation,
                                    dateRange = selectedDateRange,
                                )
                                if (content == null) {
                                    snackbarHostState.showSnackbar("List too long — narrow filters.")
                                    return@launch
                                }
                                val ok = PrinterUtils.printMessage(context, content, alignment = 0)
                                snackbarHostState.showSnackbar(
                                    if (ok) "Sent to printer" else "Print failed"
                                )
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = "Print filtered acquisition report",
                        )
                    }
                    IconButton(onClick = { onNavigateToAcquisitionForm("new") }) {
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

            if (acquisitions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No acquisitions recorded",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Record the first acquisition to compute SRPs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onNavigateToAcquisitionForm("new") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Record acquisition")
                        }
                    }
                }
            } else {
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
                                        onNavigateToAcquisitionForm(acquisition.acquisition_id.toString())
                                    },
                                    onPrint = {
                                        scope.launch {
                                            val content = buildAcquisitionReceivingSlip(
                                                acquisition,
                                                presetDisplayName = null,
                                            )
                                            val ok = PrinterUtils.printMessage(context, content, alignment = 0)
                                            snackbarHostState.showSnackbar(
                                                if (ok) "Sent to printer" else "Print failed"
                                            )
                                        }
                                    },
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
                                        onNavigateToAcquisitionForm(acquisition.acquisition_id.toString())
                                    },
                                    onPrint = {
                                        scope.launch {
                                            val content = buildAcquisitionReceivingSlip(
                                                acquisition,
                                                presetDisplayName = null,
                                            )
                                            val ok = PrinterUtils.printMessage(context, content, alignment = 0)
                                            snackbarHostState.showSnackbar(
                                                if (ok) "Sent to printer" else "Print failed"
                                            )
                                        }
                                    },
                                )
                            }
                        }
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
    onEdit: () -> Unit,
    onPrint: () -> Unit,
) {
    var srpExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val formattedDate = remember(acquisition.date_acquired) {
        dateFormatter.format(Instant.ofEpochMilli(acquisition.date_acquired))
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
                        //val srpLine = when {
                        //    !acquisition.is_per_kg && acquisition.srp_online_per_piece != null ->
                        //        "SRP online: ${CurrencyFormatter.format(acquisition.srp_online_per_piece)}/pc"
                        //    acquisition.srp_online_per_kg != null ->
                        //        "SRP online: ${CurrencyFormatter.format(acquisition.srp_online_per_kg)}/kg"
                        //    else -> null
                        //}
                        //if (srpLine != null) {
                        //    Text(
                        //        text = srpLine,
                        //        style = MaterialTheme.typography.bodySmall
                        //    )
                        //}
                        if (acquisition.srp_custom_override) {
                            Text(
                                text = "Custom customer SRP per channel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
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
                                AcquisitionSrpExpandedDetail(acquisition = acquisition)
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
                        onClick = onPrint,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print slip",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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

/** BUG-ACQ-04: per-channel blocks, labeled pack tiers, aligned price rows (not inline “·” paragraphs). */
@Composable
private fun AcquisitionSrpExpandedDetail(acquisition: Acquisition) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (acquisition.hasChannelSrpDetailOnline()) {
            AcquisitionSrpChannelBlock(
                title = "Online",
                perKg = acquisition.srp_online_per_kg,
                g500 = acquisition.srp_online_500g,
                g250 = acquisition.srp_online_250g,
                g100 = acquisition.srp_online_100g,
                perPiece = acquisition.srp_online_per_piece,
                isPerKg = acquisition.is_per_kg,
            )
        }
        if (acquisition.hasChannelSrpDetailReseller()) {
            AcquisitionSrpChannelBlock(
                title = "Reseller",
                perKg = acquisition.srp_reseller_per_kg,
                g500 = acquisition.srp_reseller_500g,
                g250 = acquisition.srp_reseller_250g,
                g100 = acquisition.srp_reseller_100g,
                perPiece = acquisition.srp_reseller_per_piece,
                isPerKg = acquisition.is_per_kg,
            )
        }
        if (acquisition.hasChannelSrpDetailOffline()) {
            AcquisitionSrpChannelBlock(
                title = "Store",
                perKg = acquisition.srp_offline_per_kg,
                g500 = acquisition.srp_offline_500g,
                g250 = acquisition.srp_offline_250g,
                g100 = acquisition.srp_offline_100g,
                perPiece = acquisition.srp_offline_per_piece,
                isPerKg = acquisition.is_per_kg,
            )
        }
    }
}

@Composable
private fun AcquisitionSrpChannelBlock(
    title: String,
    perKg: Double?,
    g500: Double?,
    g250: Double?,
    g100: Double?,
    perPiece: Double?,
    isPerKg: Boolean,
) {
    val hasPacks = isPerKg && (g500 != null || g250 != null || g100 != null)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (isPerKg) {
                perKg?.let {
                    AcquisitionSrpLabeledPriceRow(
                        label = "Per kg",
                        valueText = "${CurrencyFormatter.format(it)}/kg",
                    )
                }
            }
            if (hasPacks) {
                Text(
                    text = "Packs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                AcquisitionSrpLabeledPriceRow(label = "500 g", amount = g500)
                AcquisitionSrpLabeledPriceRow(label = "250 g", amount = g250)
                AcquisitionSrpLabeledPriceRow(label = "100 g", amount = g100)
            }
            perPiece?.let {
                AcquisitionSrpLabeledPriceRow(
                    label = "Per piece",
                    valueText = CurrencyFormatter.format(it),
                )
            }
        }
    }
}

@Composable
private fun AcquisitionSrpLabeledPriceRow(label: String, amount: Double?) {
    AcquisitionSrpLabeledPriceRow(
        label = label,
        valueText = amount?.let { CurrencyFormatter.format(it) } ?: "—",
    )
}

@Composable
private fun AcquisitionSrpLabeledPriceRow(label: String, valueText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun Acquisition.hasChannelSrpDetailOnline(): Boolean =
    srp_online_per_kg != null || srp_online_500g != null || srp_online_250g != null ||
        srp_online_100g != null || srp_online_per_piece != null

private fun Acquisition.hasChannelSrpDetailReseller(): Boolean =
    srp_reseller_per_kg != null || srp_reseller_500g != null || srp_reseller_250g != null ||
        srp_reseller_100g != null || srp_reseller_per_piece != null

private fun Acquisition.hasChannelSrpDetailOffline(): Boolean =
    srp_offline_per_kg != null || srp_offline_500g != null || srp_offline_250g != null ||
        srp_offline_100g != null || srp_offline_per_piece != null

private fun Acquisition.hasSrpDetail(): Boolean =
    srp_online_per_kg != null || srp_reseller_per_kg != null || srp_offline_per_kg != null ||
        srp_online_500g != null || srp_online_250g != null || srp_online_100g != null ||
        srp_reseller_500g != null || srp_reseller_250g != null || srp_reseller_100g != null ||
        srp_offline_500g != null || srp_offline_250g != null || srp_offline_100g != null ||
        srp_online_per_piece != null 