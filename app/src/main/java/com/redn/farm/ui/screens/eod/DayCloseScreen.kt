package com.redn.farm.ui.screens.eod

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.local.entity.DayCloseEntity
import com.redn.farm.data.local.entity.DayCloseInventoryEntity
import com.redn.farm.utils.CurrencyFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCloseScreen(
    businessDateMillis: Long,
    username: String,
    role: String,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToOutstandingInventory: () -> Unit,
    viewModel: DayCloseViewModel = hiltViewModel(),
) {
    LaunchedEffect(businessDateMillis) {
        viewModel.open(businessDateMillis, username, role)
    }

    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNegativeMarginDialog by remember { mutableStateOf(false) }

    LaunchedEffect(event) {
        when (val e = event) {
            is DayCloseEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(e.message)
                viewModel.consumeEvent()
            }
            is DayCloseEvent.ConfirmNegativeMargin -> {
                showNegativeMarginDialog = true
                viewModel.consumeEvent()
            }
            is DayCloseEvent.Finalized -> {
                snackbarHostState.showSnackbar("Day close finalized")
                viewModel.consumeEvent()
            }
            null -> Unit
        }
    }

    if (showNegativeMarginDialog) {
        AlertDialog(
            onDismissRequest = { showNegativeMarginDialog = false },
            title = { Text("Negative margin") },
            text = { Text("Gross margin is negative. Finalize anyway?") },
            confirmButton = {
                TextButton(onClick = {
                    showNegativeMarginDialog = false
                    viewModel.confirmFinalizeWithNegativeMargin(username)
                }) { Text("Finalize") }
            },
            dismissButton = {
                TextButton(onClick = { showNegativeMarginDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Day Close") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "History")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DayCloseUiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is DayCloseUiState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { Text(state.message, color = MaterialTheme.colorScheme.error) }
            }
            is DayCloseUiState.Ready -> {
                DayCloseReadyContent(
                    state = state,
                    username = username,
                    padding = padding,
                    onRefreshInventory = { viewModel.refreshInventorySnapshot(username) },
                    onEnterCount = { idx, kg -> viewModel.enterActualCount(idx, kg) },
                    onSaveCash = { cash, remarks -> viewModel.saveCash(cash, remarks, username) },
                    onFinalize = { viewModel.requestFinalize(username) },
                    onOutstandingInventory = onNavigateToOutstandingInventory,
                )
            }
        }
    }
}

@Composable
private fun DayCloseReadyContent(
    state: DayCloseUiState.Ready,
    username: String,
    padding: PaddingValues,
    onRefreshInventory: () -> Unit,
    onEnterCount: (Int, Double) -> Unit,
    onSaveCash: (Double?, String?) -> Unit,
    onFinalize: () -> Unit,
    onOutstandingInventory: () -> Unit,
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }
    val dateLabel = remember(state.close.business_date) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(state.close.business_date), ZoneId.systemDefault()
        ).format(dateFmt)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleLarge,
            )
            if (state.close.is_finalized) {
                AssistChip(
                    onClick = {},
                    label = { Text("Finalized") },
                    leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                )
            }
        }

        // ── Sales Summary (EOD-US-01 / EOD-US-02) ────────────────────────
        item {
            DayCloseSectionCard(title = "Sales Summary") {
                DayCloseRow("Orders", state.close.total_orders.toString())
                DayCloseRow("Gross Revenue", CurrencyFormatter.format(state.close.gross_revenue_today ?: 0.0))
                DayCloseRow("Collected", CurrencyFormatter.format(state.close.collected_revenue_today ?: 0.0))
                state.close.snapshot_all_unpaid_count?.let { count ->
                    DayCloseRow("Unpaid Orders", "$count")
                }
                state.close.snapshot_all_unpaid_amount?.let { amt ->
                    DayCloseRow("Unpaid Amount", CurrencyFormatter.format(amt))
                }
            }
        }

        // ── COGS / Margin (EOD-US-07) ─────────────────────────────────────
        item {
            DayCloseSectionCard(title = "Cost & Margin") {
                DayCloseRow("COGS (WAC-based)", CurrencyFormatter.format(state.close.total_cogs_today ?: 0.0))
                DayCloseRow(
                    "Gross Margin",
                    state.close.gross_margin_amount?.let { amt ->
                        val pct = state.close.gross_margin_percent?.let { " (${String.format("%.1f", it)}%%)" } ?: ""
                        "${CurrencyFormatter.format(amt)}$pct"
                    } ?: "—"
                )
            }
        }

        // ── Inventory Close (EOD-US-03 / EOD-US-04 / EOD-US-05) ──────────
        item {
            DayCloseSectionCard(title = "Inventory") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${state.inventoryLines.size} products",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRefreshInventory) { Text("Refresh") }
                }
            }
        }

        // Inventory lines
        itemsIndexed(state.inventoryLines) { idx, line ->
            InventoryLineCard(
                line = line,
                canEdit = state.canEditInventoryCounts && !state.close.is_finalized,
                onEnterCount = { kg -> onEnterCount(idx, kg) },
            )
        }

        // ── Outstanding Inventory teaser (EOD-US-10, D6) ──────────────────
        item {
            OutlinedCard(onClick = onOutstandingInventory, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Outstanding Inventory", style = MaterialTheme.typography.titleSmall)
                    Text("View full report →", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── Cash Reconciliation (EOD-US-06) ───────────────────────────────
        item {
            CashReconciliationCard(
                close = state.close,
                canEdit = !state.close.is_finalized,
                onSave = onSaveCash,
            )
        }

        // ── Finalize button (EOD-US-08) ───────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onFinalize,
                enabled = !state.close.is_finalized,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Text(if (state.close.is_finalized) "Finalized" else "Finalize Day Close")
            }
        }
    }
}

@Composable
private fun DayCloseSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun DayCloseRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InventoryLineCard(
    line: DayCloseInventoryEntity,
    canEdit: Boolean,
    onEnterCount: (Double) -> Unit,
) {
    var countText by remember(line.id) { mutableStateOf(line.actual_remaining?.toString() ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(line.product_name, style = MaterialTheme.typography.titleSmall)
            DayCloseRow("Theoretical (adj.)", String.format("%.3f kg", line.adjusted_theoretical_remaining))
            DayCloseRow("Sold today", String.format("%.3f kg", line.sold_this_close_date))
            line.variance_qty?.let { v ->
                DayCloseRow(
                    "Variance",
                    String.format("%.3f kg (${CurrencyFormatter.format(line.variance_cost ?: 0.0)})", v)
                )
            }
            if (canEdit) {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Actual count (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            countText.toDoubleOrNull()?.let { onEnterCount(it) }
                        }) {
                            Icon(Icons.Default.Check, "Save count")
                        }
                    }
                )
            } else {
                line.actual_remaining?.let {
                    DayCloseRow("Actual count", String.format("%.3f kg", it))
                }
            }
        }
    }
}

@Composable
private fun CashReconciliationCard(
    close: DayCloseEntity,
    canEdit: Boolean,
    onSave: (Double?, String?) -> Unit,
) {
    var cashText by remember { mutableStateOf(close.cash_on_hand?.toString() ?: "") }
    var remarksText by remember { mutableStateOf(close.cash_reconciliation_remarks ?: "") }

    DayCloseSectionCard(title = "Cash Reconciliation") {
        DayCloseRow("Collected", CurrencyFormatter.format(close.collected_revenue_today ?: 0.0))
        if (canEdit) {
            OutlinedTextField(
                value = cashText,
                onValueChange = { cashText = it },
                label = { Text("Cash on hand") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = remarksText,
                onValueChange = { remarksText = it },
                label = { Text("Remarks") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            TextButton(
                onClick = { onSave(cashText.toDoubleOrNull(), remarksText.ifBlank { null }) },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Save") }
        } else {
            close.cash_on_hand?.let { DayCloseRow("Cash on hand", CurrencyFormatter.format(it)) }
            close.cash_reconciliation_remarks?.let { DayCloseRow("Remarks", it) }
        }
    }
}
