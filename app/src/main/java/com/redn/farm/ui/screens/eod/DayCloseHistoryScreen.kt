package com.redn.farm.ui.screens.eod

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.local.entity.DayCloseEntity
import com.redn.farm.data.util.DateWindowHelper
import com.redn.farm.utils.CurrencyFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class HistoryRangeFilter { All, Last30Days, Last90Days }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCloseHistoryScreen(
    onNavigateBack: () -> Unit,
    onOpenClose: (Long) -> Unit,
    viewModel: DayCloseHistoryViewModel = hiltViewModel(),
) {
    val closes by viewModel.closes.collectAsState(initial = emptyList())
    var rangeFilter by remember { mutableStateOf(HistoryRangeFilter.All) }

    val filteredCloses = remember(closes, rangeFilter) {
        val now = System.currentTimeMillis()
        when (rangeFilter) {
            HistoryRangeFilter.All -> closes
            HistoryRangeFilter.Last30Days -> {
                val cutoff = DateWindowHelper.startOfDay(now - 30L * 24 * 60 * 60 * 1000)
                closes.filter { it.business_date >= cutoff }
            }
            HistoryRangeFilter.Last90Days -> {
                val cutoff = DateWindowHelper.startOfDay(now - 90L * 24 * 60 * 60 * 1000)
                closes.filter { it.business_date >= cutoff }
            }
        }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day Close History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = rangeFilter == HistoryRangeFilter.All,
                    onClick = { rangeFilter = HistoryRangeFilter.All },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = rangeFilter == HistoryRangeFilter.Last30Days,
                    onClick = { rangeFilter = HistoryRangeFilter.Last30Days },
                    label = { Text("30 days") },
                )
                FilterChip(
                    selected = rangeFilter == HistoryRangeFilter.Last90Days,
                    onClick = { rangeFilter = HistoryRangeFilter.Last90Days },
                    label = { Text("90 days") },
                )
            }
            if (filteredCloses.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No day closes in this range.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCloses, key = { it.close_id }) { close ->
                        DayCloseHistoryCard(
                            close = close,
                            dateFmt = dateFmt,
                            onClick = { onOpenClose(close.business_date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCloseHistoryCard(
    close: DayCloseEntity,
    dateFmt: DateTimeFormatter,
    onClick: () -> Unit,
) {
    val dateLabel = remember(close.business_date) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(close.business_date), ZoneId.systemDefault()
        ).format(dateFmt)
    }
    val closedAtLabel = remember(close.closed_at) {
        close.closed_at?.let { at ->
            LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault()).format(dateFmt)
        } ?: "—"
    }
    val marginText = remember(close.gross_margin_amount, close.gross_margin_percent) {
        close.gross_margin_amount?.let { amt ->
            val pct = close.gross_margin_percent?.let { " (${String.format("%.1f", it)}%)" } ?: ""
            "${CurrencyFormatter.format(amt)}$pct"
        } ?: "—"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(dateLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Orders: ${close.total_orders} · ${CurrencyFormatter.format(close.total_collected)} collected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Sales: ${CurrencyFormatter.format(close.total_sales_amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Margin: $marginText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Closed by: ${close.closed_by} · Closed at: $closedAtLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (close.is_finalized) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Finalized",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
