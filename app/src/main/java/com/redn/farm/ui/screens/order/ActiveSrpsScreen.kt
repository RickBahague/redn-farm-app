package com.redn.farm.ui.screens.order

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.utils.buildSrpPriceList
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import com.redn.farm.utils.ThermalSrpPrintRow
import kotlinx.coroutines.launch
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.pricing.OrderPricingResolver
import com.redn.farm.data.pricing.SalesChannel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSrpsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActiveSrpsViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    val activePresetName by viewModel.activePresetName.collectAsState()
    val activePresetActivatedAt by viewModel.activePresetActivatedAt.collectAsState()
    var selectedChannel by remember { mutableStateOf(SalesChannel.ONLINE) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.getDefault()) }
    val acqDateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }
    val selectedChannelLabel = when (selectedChannel) {
        SalesChannel.ONLINE -> "Online"
        SalesChannel.RESELLER -> "Reseller"
        else -> "Offline"
    }
    val footerText = activePresetName?.let { name ->
        val activatedText = activePresetActivatedAt?.let { millis ->
            val instant = Instant.ofEpochMilli(millis)
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateFmt)
        } ?: "—"
        "Preset: $name · Activated $activatedText"
    } ?: ""

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun printPriceList() {
        scope.launch {
            if (rows.isEmpty()) {
                snackbarHostState.showSnackbar("No active preset — SRPs not computed")
                return@launch
            }
            val thermalRows = rows.map { row ->
                val acq = row.acquisition!!
                val ch = selectedChannel
                val perKg = OrderPricingResolver.srpFromAcquisition(acq, ch, isPerKg = true)
                val per500g = when (ch) {
                    SalesChannel.ONLINE -> acq.srp_online_500g
                    SalesChannel.RESELLER -> acq.srp_reseller_500g
                    else -> acq.srp_offline_500g
                }
                val perPiece = OrderPricingResolver.srpFromAcquisition(acq, ch, isPerKg = false)
                ThermalSrpPrintRow(
                    name = row.product.product_name,
                    perKg = perKg,
                    per500g = per500g,
                    perPiece = perPiece,
                    isPerKg = acq.is_per_kg,
                )
            }
            val content = buildSrpPriceList(
                channelLabel = selectedChannelLabel,
                presetName = activePresetName,
                asOfMillis = activePresetActivatedAt ?: System.currentTimeMillis(),
                rows = thermalRows,
            )
            val ok = PrinterUtils.printMessage(context, content, alignment = 0)
            snackbarHostState.showSnackbar(
                if (ok) "Sent to printer" else "Print failed"
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Active SRPs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { printPriceList() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print price list",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                OutlinedButton(
                    onClick = { printPriceList() },
                    enabled = rows.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                ) {
                    Text("Print price list")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedChannel == SalesChannel.ONLINE,
                    onClick = { selectedChannel = SalesChannel.ONLINE },
                    label = { Text("Online") }
                )
                FilterChip(
                    selected = selectedChannel == SalesChannel.RESELLER,
                    onClick = { selectedChannel = SalesChannel.RESELLER },
                    label = { Text("Reseller") }
                )
                FilterChip(
                    selected = selectedChannel == SalesChannel.OFFLINE,
                    onClick = { selectedChannel = SalesChannel.OFFLINE },
                    label = { Text("Offline") }
                )
            }
            Text(
                text = "List, details, and print use the selected channel ($selectedChannelLabel).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No products with a computed SRP yet. Record an acquisition with an active pricing preset.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rows, key = { it.product.product_id }) { row ->
                        var expanded by remember(row.product.product_id) { mutableStateOf(false) }
                        val acq = row.acquisition!!
                        val acqDateText = remember(acq.acquisition_id, acq.date_acquired) {
                            LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(acq.date_acquired),
                                ZoneId.systemDefault(),
                            ).format(acqDateFmt)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = row.product.product_name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        ActiveSrpsCollapsedSummary(
                                            acquisition = acq,
                                            channel = selectedChannel,
                                            channelLabel = selectedChannelLabel,
                                        )
                                        Text(
                                            text = "Acq #${acq.acquisition_id} · $acqDateText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }

                                AnimatedVisibility(expanded) {
                                    ActiveSrpsSelectedChannelDetail(
                                        acquisition = acq,
                                        channel = selectedChannel,
                                        channelTitle = selectedChannelLabel,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (footerText.isNotBlank()) {
                Text(
                    text = footerText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveSrpsCollapsedSummary(
    acquisition: Acquisition,
    channel: String,
    channelLabel: String,
) {
    val perKg = OrderPricingResolver.srpFromAcquisition(acquisition, channel, isPerKg = true)
    val perPiece = OrderPricingResolver.srpFromAcquisition(acquisition, channel, isPerKg = false)
    val kgPart = if (acquisition.is_per_kg && perKg != null && perKg > 0) {
        "${CurrencyFormatter.format(perKg)}/kg"
    } else null
    val pcPart = if (perPiece != null && perPiece > 0) {
        "${CurrencyFormatter.format(perPiece)}/pc"
    } else null
    val body = when {
        !acquisition.is_per_kg && pcPart != null -> "$channelLabel: $pcPart"
        !acquisition.is_per_kg -> null
        kgPart != null && pcPart != null -> "$channelLabel: $kgPart · $pcPart"
        kgPart != null -> "$channelLabel: $kgPart"
        pcPart != null -> "$channelLabel: $pcPart"
        else -> null
    }
    if (body != null) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    } else {
        Text(
            text = "$channelLabel: no SRP",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActiveSrpsSelectedChannelDetail(
    acquisition: Acquisition,
    channel: String,
    channelTitle: String,
) {
    val ch = SalesChannel.normalize(channel)
    val blockTitle = when (ch) {
        SalesChannel.ONLINE -> "Online"
        SalesChannel.RESELLER -> "Reseller"
        else -> "Store"
    }
    val hasChannel = when (ch) {
        SalesChannel.ONLINE -> acquisition.activeSrpHasChannelOnline()
        SalesChannel.RESELLER -> acquisition.activeSrpHasChannelReseller()
        else -> acquisition.activeSrpHasChannelOffline()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Do not use `return@Column` here: this composable is nested under LazyColumn items +
        // AnimatedVisibility; qualified returns in that subtree corrupt the slot table (crash).
        if (!hasChannel) {
            Text(
                text = "No SRP for $channelTitle on this acquisition.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val perKg = OrderPricingResolver.srpFromAcquisition(acquisition, channel, isPerKg = true)
            val perPiece = OrderPricingResolver.srpFromAcquisition(acquisition, channel, isPerKg = false)
            when (ch) {
                SalesChannel.ONLINE ->
                    ActiveSrpsChannelBlock(
                        title = blockTitle,
                        perKg = perKg ?: acquisition.srp_online_per_kg,
                        g500 = acquisition.srp_online_500g,
                        g250 = acquisition.srp_online_250g,
                        g100 = acquisition.srp_online_100g,
                        perPiece = perPiece,
                        isPerKg = acquisition.is_per_kg,
                    )
                SalesChannel.RESELLER ->
                    ActiveSrpsChannelBlock(
                        title = blockTitle,
                        perKg = perKg ?: acquisition.srp_reseller_per_kg,
                        g500 = acquisition.srp_reseller_500g,
                        g250 = acquisition.srp_reseller_250g,
                        g100 = acquisition.srp_reseller_100g,
                        perPiece = perPiece,
                        isPerKg = acquisition.is_per_kg,
                    )
                else ->
                    ActiveSrpsChannelBlock(
                        title = blockTitle,
                        perKg = perKg ?: acquisition.srp_offline_per_kg,
                        g500 = acquisition.srp_offline_500g,
                        g250 = acquisition.srp_offline_250g,
                        g100 = acquisition.srp_offline_100g,
                        perPiece = perPiece,
                        isPerKg = acquisition.is_per_kg,
                    )
            }
        }
    }
}

@Composable
private fun ActiveSrpsChannelBlock(
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
                    ActiveSrpsLabeledPriceRow(
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
                ActiveSrpsLabeledPriceRow(label = "500 g", amount = g500)
                ActiveSrpsLabeledPriceRow(label = "250 g", amount = g250)
                ActiveSrpsLabeledPriceRow(label = "100 g", amount = g100)
            }
            perPiece?.let {
                ActiveSrpsLabeledPriceRow(
                    label = "Per piece",
                    valueText = CurrencyFormatter.format(it),
                )
            }
        }
    }
}

@Composable
private fun ActiveSrpsLabeledPriceRow(label: String, amount: Double?) {
    ActiveSrpsLabeledPriceRow(
        label = label,
        valueText = amount?.let { CurrencyFormatter.format(it) } ?: "—",
    )
}

@Composable
private fun ActiveSrpsLabeledPriceRow(label: String, valueText: String) {
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

private fun Acquisition.activeSrpHasChannelOnline(): Boolean =
    srp_online_per_kg != null || srp_online_500g != null || srp_online_250g != null ||
        srp_online_100g != null || srp_online_per_piece != null

private fun Acquisition.activeSrpHasChannelReseller(): Boolean =
    srp_reseller_per_kg != null || srp_reseller_500g != null || srp_reseller_250g != null ||
        srp_reseller_100g != null || srp_reseller_per_piece != null

private fun Acquisition.activeSrpHasChannelOffline(): Boolean =
    srp_offline_per_kg != null || srp_offline_500g != null || srp_offline_250g != null ||
        srp_offline_100g != null || srp_offline_per_piece != null
