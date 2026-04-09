package com.redn.farm.ui.screens.eod

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.repository.DayCloseRepository
import com.redn.farm.data.util.InventoryFifoAllocator
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import kotlinx.coroutines.launch
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val printedBy = remember(context) {
        SessionManager(context.applicationContext).getUsername() ?: "unknown"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Outstanding Inventory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val text = viewModel.buildPrintText(printedBy)
                            if (text == null) {
                                scope.launch { snackbarHostState.showSnackbar("Nothing to print") }
                            } else {
                                scope.launch {
                                    val ok = PrinterUtils.printMessage(context, text, alignment = 0)
                                    snackbarHostState.showSnackbar(if (ok) "Sent to printer" else "Print failed")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Print, "Print")
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
                if (state.allLineCount == 0) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No outstanding inventory.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (state.filteredLines.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No matching products for current filters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    OutstandingInventoryList(
                        state = state,
                        padding = padding,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutstandingInventoryList(
    state: OutstandingInventoryUiState.Ready,
    padding: PaddingValues,
    viewModel: OutstandingInventoryViewModel,
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }
    var searchText by remember { mutableStateOf(state.searchQuery) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Total value (filtered): ${CurrencyFormatter.format(state.totalValue)}",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.setSearchQuery(it)
                },
                label = { Text("Search product") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Search),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("At-risk only (≥3 d)", style = MaterialTheme.typography.labelMedium)
                Switch(
                    checked = state.atRiskOnly,
                    onCheckedChange = { viewModel.setAtRiskOnly(it) },
                )
            }
            if (state.categories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    FilterChip(
                        selected = state.category == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("All") },
                    )
                    Spacer(Modifier.width(6.dp))
                    state.categories.forEach { cat ->
                        FilterChip(
                            selected = state.category == cat,
                            onClick = {
                                viewModel.setCategory(if (state.category == cat) null else cat)
                            },
                            label = { Text(cat) },
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AgingChip("≥3 days", AmberColor)
                AgingChip("≥7 days", RedColor)
            }
        }

        items(state.filteredLines, key = { it.productId }) { line ->
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
                        String.format(
                            "Outstanding: %.3f kg · %s",
                            line.totalRemainingKg,
                            CurrencyFormatter.format(line.displayValuePhp),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        String.format(
                            "Acquired %.3f kg · Sold %.3f kg · Spoilage %.3f kg",
                            line.totalAcquiredKg,
                            line.totalSoldKg,
                            line.priorPostedSpoilageKg,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        String.format(
                            "Theoretical on hand: %.3f kg · WAC: %s/kg",
                            line.theoreticalOnHandKg,
                            CurrencyFormatter.format(line.weightedAverageCostPerKg),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (line.usesActualFromDayClose) {
                        Text(
                            "Uses physical count from finalized day close",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    line.oldestUnsoldDateMillis?.let { millis ->
                        val dateStr = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(millis), ZoneId.systemDefault()
                        ).format(dateFmt)
                        Text(
                            "Oldest unsold lot: $dateStr · ${line.daysOnHand} days on hand",
                            style = MaterialTheme.typography.labelSmall,
                            color = agingColor,
                        )
                    }
                }
                if (line.lots.isNotEmpty()) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Hide lots" else "View lots")
                    }
                }
            }

            if (expanded && line.lots.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
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
            String.format("%.3f kg remaining · %d days on hand", lot.remainingQtyKg, lot.ageDays),
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
