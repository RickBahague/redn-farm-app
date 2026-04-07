package com.redn.farm.ui.screens.eod

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.repository.DayCloseRepository
import com.redn.farm.data.util.InventoryFifoAllocator
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AmberColor = Color(0xFFFFA000)
private val RedColor = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingInventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: OutstandingInventoryViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load() }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Outstanding Inventory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is OutstandingInventoryUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is OutstandingInventoryUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is OutstandingInventoryUiState.Ready -> {
                if (state.lines.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No outstanding inventory.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    OutstandingInventoryList(lines = state.lines, padding = padding)
                }
            }
        }
    }
}

@Composable
private fun OutstandingInventoryList(
    lines: List<DayCloseRepository.OutstandingProductLine>,
    padding: PaddingValues,
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AgingChip("≥3 days", AmberColor)
                AgingChip("≥7 days", RedColor)
            }
            Spacer(Modifier.height(4.dp))
        }

        items(lines, key = { it.productId }) { line ->
            OutstandingProductCard(line = line, dateFmt = dateFmt)
        }
    }
}

@Composable
private fun OutstandingProductCard(
    line: DayCloseRepository.OutstandingProductLine,
    dateFmt: DateTimeFormatter,
) {
    val agingColor = when (line.agingFlag) {
        DayCloseRepository.AgingFlag.RED -> RedColor
        DayCloseRepository.AgingFlag.AMBER -> AmberColor
        DayCloseRepository.AgingFlag.NORMAL -> MaterialTheme.colorScheme.onSurface
    }

    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(line.productName, style = MaterialTheme.typography.titleSmall, color = agingColor)
                    Text(
                        String.format("%.3f kg remaining", line.totalRemainingKg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    line.oldestUnsoldDateMillis?.let { millis ->
                        val dateStr = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(millis), ZoneId.systemDefault()
                        ).format(dateFmt)
                        Text(
                            "Oldest lot: $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = agingColor,
                        )
                    }
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide lots" else "View lots")
                }
            }

            if (expanded) {
                Divider(Modifier.padding(vertical = 4.dp))
                line.lots.filter { it.remainingQtyKg > 0 }.forEach { lot ->
                    LotRow(lot = lot, dateFmt = dateFmt)
                }
            }
        }
    }
}

@Composable
private fun LotRow(lot: InventoryFifoAllocator.LotResult, dateFmt: DateTimeFormatter) {
    val dateStr = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(lot.dateAcquired), ZoneId.systemDefault()
    ).format(dateFmt)
    val agingColor = when {
        lot.ageDays >= 7 -> RedColor
        lot.ageDays >= 3 -> AmberColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = agingColor)
        Text(
            String.format("%.3f kg · %d days", lot.remainingQtyKg, lot.ageDays),
            style = MaterialTheme.typography.labelSmall,
            color = agingColor
        )
    }
}

@Composable
private fun AgingChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
