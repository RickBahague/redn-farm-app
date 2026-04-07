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
import com.redn.farm.utils.CurrencyFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCloseHistoryScreen(
    onNavigateBack: () -> Unit,
    onOpenClose: (Long) -> Unit,
    viewModel: DayCloseHistoryViewModel = hiltViewModel(),
) {
    val closes by viewModel.closes.collectAsState(initial = emptyList())
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
        if (closes.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No day closes recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(closes, key = { it.close_id }) { close ->
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
