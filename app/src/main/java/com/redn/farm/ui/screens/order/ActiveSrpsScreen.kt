package com.redn.farm.ui.screens.order

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.utils.CurrencyFormatter
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
    viewModel: ActiveSrpsViewModel = viewModel(factory = ActiveSrpsViewModel.Factory)
) {
    val rows by viewModel.rows.collectAsState()
    val activePresetName by viewModel.activePresetName.collectAsState()
    val activePresetActivatedAt by viewModel.activePresetActivatedAt.collectAsState()
    var selectedChannel by remember { mutableStateOf(SalesChannel.ONLINE) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.getDefault()) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active SRPs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Channel selector updates SRP prices across the whole list.
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

                        val (perKg, per500g, per250g, perPiece) = when (selectedChannel) {
                            SalesChannel.ONLINE -> listOf(acq.srp_online_per_kg, acq.srp_online_500g, acq.srp_online_250g, acq.srp_online_per_piece)
                            SalesChannel.RESELLER -> listOf(acq.srp_reseller_per_kg, acq.srp_reseller_500g, acq.srp_reseller_250g, acq.srp_reseller_per_piece)
                            else -> listOf(acq.srp_offline_per_kg, acq.srp_offline_500g, acq.srp_offline_250g, acq.srp_offline_per_piece)
                        }

                        fun fmtOpt(v: Double?): String = v?.let { CurrencyFormatter.format(it) } ?: "—"

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
                                        Text(
                                            text = "Per kg: ${fmtOpt(perKg)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Per 500g: ${fmtOpt(per500g)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }

                                Text(
                                    text = "Per piece: ${fmtOpt(perPiece)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                AnimatedVisibility(expanded) {
                                    SrpChannelBlock(
                                        label = selectedChannelLabel,
                                        perKg = perKg,
                                        per500g = per500g,
                                        per250g = per250g,
                                        perPiece = perPiece
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
private fun SrpChannelBlock(
    label: String,
    perKg: Double?,
    per500g: Double?,
    per250g: Double?,
    perPiece: Double?
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = buildString {
                fun fmt(v: Double?) = v?.let { CurrencyFormatter.format(it) } ?: "—"
                append("Per kg: ")
                append(fmt(perKg))
                append(" · Per 500g: ")
                append(fmt(per500g))
                append(" · Per 250g: ")
                append(fmt(per250g))
            },
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Per piece: ${perPiece?.let { CurrencyFormatter.format(it) } ?: "—"}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
