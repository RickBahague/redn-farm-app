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
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.getDefault()) }

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
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows, key = { it.product.product_id }) { row ->
                    var expanded by remember(row.product.product_id) { mutableStateOf(false) }
                    val acq = row.acquisition!!
                    val updated = remember(acq.date_acquired, acq.created_at) {
                        val instant = Instant.ofEpochMilli(maxOf(acq.date_acquired, acq.created_at))
                        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateFmt)
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
                                    Text(
                                        text = "From ${CurrencyFormatter.format(row.summaryFromPerKg!!)}/kg (min across channels)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Last lot: #$updated · acquisition #${acq.acquisition_id}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                            AnimatedVisibility(expanded) {
                                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SrpChannelBlock("Online", acq.srp_online_per_kg, acq.srp_online_per_piece)
                                    SrpChannelBlock("Reseller", acq.srp_reseller_per_kg, acq.srp_reseller_per_piece)
                                    SrpChannelBlock("Store (offline)", acq.srp_offline_per_kg, acq.srp_offline_per_piece)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SrpChannelBlock(label: String, perKg: Double?, perPiece: Double?) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = buildString {
                append("Per kg: ")
                append(perKg?.let { CurrencyFormatter.format(it) } ?: "—")
                append(" · Per pc: ")
                append(perPiece?.let { CurrencyFormatter.format(it) } ?: "—")
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}
