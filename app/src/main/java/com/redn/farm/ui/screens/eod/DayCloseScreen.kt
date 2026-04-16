package com.redn.farm.ui.screens.eod

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import com.redn.farm.data.local.dao.ProductSoldQtyBreakdown
import com.redn.farm.data.local.entity.DayCloseEntity
import com.redn.farm.data.local.entity.DayCloseInventoryEntity
import com.redn.farm.data.pricing.SalesChannel
import com.redn.farm.data.repository.DayCloseRepository
import com.redn.farm.data.repository.DayCloseSoldQty
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private val AmberInventoryAging = Color(0xFFFFA000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCloseScreen(
    businessDateMillis: Long,
    username: String,
    role: String,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToOutstandingInventory: () -> Unit,
    onNavigateToOrderDetail: (Int) -> Unit,
    viewModel: DayCloseViewModel = hiltViewModel(),
) {
    LaunchedEffect(businessDateMillis) {
        viewModel.open(businessDateMillis, username, role)
    }

    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNegativeMarginDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            is DayCloseEvent.CashRemarksRequired -> {
                snackbarHostState.showSnackbar(
                    "Add reconciliation remarks for any non-zero discrepancy before finalizing."
                )
                viewModel.consumeEvent()
            }
            null -> Unit
        }
    }

    if (showNegativeMarginDialog) {
        AlertDialog(
            onDismissRequest = { showNegativeMarginDialog = false },
            title = { Text("Negative margin") },
            text = { Text("Today's COGS exceeds collected revenue. Finalize anyway?") },
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
                    padding = padding,
                    onRefreshInventory = { viewModel.refreshInventorySnapshot(username) },
                    onEnterCount = { pid, raw -> viewModel.enterActualCount(pid, raw) },
                    onSetCounted = { pid, counted -> viewModel.setLineCounted(pid, counted) },
                    onSaveCash = { cash, remarks -> viewModel.saveCash(cash, remarks, username) },
                    onSaveNotes = { notes -> viewModel.saveNotes(notes, username) },
                    onReview = { viewModel.startReview() },
                    onCancelReview = { viewModel.cancelReview() },
                    onFinalize = { viewModel.requestFinalize(username) },
                    onUnfinalize = { viewModel.unfinalize(username) },
                    onOutstandingInventory = onNavigateToOutstandingInventory,
                    onOrderClick = onNavigateToOrderDetail,
                    onPrint = {
                        val text = viewModel.buildEodPrintText(username) ?: return@DayCloseReadyContent
                        scope.launch {
                            val ok = PrinterUtils.printMessage(context, text, alignment = 0)
                            snackbarHostState.showSnackbar(if (ok) "Sent to printer" else "Print failed")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DayCloseReadyContent(
    state: DayCloseUiState.Ready,
    padding: PaddingValues,
    onRefreshInventory: () -> Unit,
    onEnterCount: (String, Double) -> Unit, // value in display units (kg or pc per inventory row label)
    onSetCounted: (String, Boolean) -> Unit,
    onSaveCash: (Double?, String?) -> Unit,
    onSaveNotes: (String?) -> Unit,
    onReview: () -> Unit,
    onCancelReview: () -> Unit,
    onFinalize: () -> Unit,
    onUnfinalize: () -> Unit,
    onOutstandingInventory: () -> Unit,
    onOrderClick: (Int) -> Unit,
    onPrint: () -> Unit,
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }
    val dateLabel = remember(state.close.business_date) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(state.close.business_date), ZoneId.systemDefault()
        ).format(dateFmt)
    }
    var showZeroTheoretical by remember { mutableStateOf(false) }

    val nowMillis = remember { System.currentTimeMillis() }
    val displayInventory = remember(state.inventoryLines, showZeroTheoretical) {
        if (showZeroTheoretical) state.inventoryLines
        else state.inventoryLines.filter { line ->
            abs(line.adjusted_theoretical_remaining) > 1e-6 ||
                abs(line.sold_this_close_date) > 1e-6 ||
                abs(line.total_acquired_all_time) > 1e-6
        }
    }

    val spoilageTotal = remember(state.inventoryLines) {
        state.inventoryLines
            .filter { it.is_counted }
            .sumOf { line ->
                val v = line.variance_qty
                if (v != null && v > 0) line.variance_cost ?: 0.0 else 0.0
            }
    }

    val snap = state.snapshot
    val drawerExpected = snap.expectedCashFromOrders - snap.remittedToday
    val diffExpectedRemitted = snap.expectedCashFromOrders - snap.remittedToday

    val marginColor = when {
        (state.close.gross_margin_amount ?: 0.0) < 0 -> MaterialTheme.colorScheme.error
        (state.close.gross_margin_amount ?: 0.0) > 0 -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val netRecoveredColor = when {
        snap.cumulative.netRecovered < 0 -> MaterialTheme.colorScheme.error
        snap.cumulative.netRecovered > 0 -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurface
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.close.is_finalized) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Finalized") },
                        leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    )
                }
                FilledTonalButton(onClick = onPrint) {
                    Icon(Icons.Default.Print, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.close.is_finalized) "Print" else "Print draft")
                }
            }
        }

        // Warnings (EOD-US-01 AC4)
        item {
            if (snap.unpaidTodayCount > 0 || snap.acquisitionsNoSalesCount > 0) {
                DayCloseSectionCard(title = "Warnings") {
                    if (snap.unpaidTodayCount > 0) {
                        AssistChip(onClick = {}, label = { Text("${snap.unpaidTodayCount} unpaid order(s) today") })
                    }
                    if (snap.acquisitionsNoSalesCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${snap.acquisitionsNoSalesCount} product(s) acquired today with no sales recorded today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            DayCloseSectionCard(title = "Sales Summary") {
                val daily = snap.dailyBreakdown
                DayCloseRow("Orders", state.close.total_orders.toString())
                DayCloseRow("Gross revenue", CurrencyFormatter.format(state.close.gross_revenue_today ?: 0.0))
                DayCloseRow("Collected", CurrencyFormatter.format(state.close.collected_revenue_today ?: 0.0))
                DayCloseRow("Paid orders (today)", "${daily.paid_count} · ${CurrencyFormatter.format(daily.paid_amount)}")
                DayCloseRow("Unpaid orders (today)", "${daily.unpaid_count} · ${CurrencyFormatter.format(daily.unpaid_amount)}")
                DayCloseRow("Delivered orders (today)", daily.delivered_count.toString())
                DayCloseRow(
                    "Outstanding (today)",
                    CurrencyFormatter.format(snap.outstandingRevenueToday),
                )
                state.close.snapshot_all_unpaid_count?.let { count ->
                    DayCloseRow("All unpaid orders (#)", "$count")
                }
                state.close.snapshot_all_unpaid_amount?.let { amt ->
                    DayCloseRow("All unpaid amount", CurrencyFormatter.format(amt))
                }
                Spacer(Modifier.height(6.dp))
                Text("By channel", style = MaterialTheme.typography.labelMedium)
                if (snap.byChannel.isEmpty()) {
                    Text("—", style = MaterialTheme.typography.bodySmall)
                } else {
                    snap.byChannel.forEach { row ->
                        val label = SalesChannel.label(SalesChannel.normalize(row.channel))
                        DayCloseRow(
                            label,
                            "${row.order_count} · ${CurrencyFormatter.format(row.total_sales)}",
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Top products (revenue)", style = MaterialTheme.typography.labelMedium)
                if (snap.topProducts.isEmpty()) {
                    Text("—", style = MaterialTheme.typography.bodySmall)
                } else {
                    snap.topProducts.forEach { p ->
                        DayCloseRow(
                            p.product_name.take(28),
                            "${DayCloseSoldQty.formatTopProductQtyLine(p.qty_kg_sold, p.qty_pc_sold)} · ${CurrencyFormatter.format(p.revenue)}",
                        )
                    }
                }
            }
        }

        item {
            DayCloseSectionCard(title = "Cost & margin") {
                DayCloseRow("COGS (WAC)", CurrencyFormatter.format(state.close.total_cogs_today ?: 0.0))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Gross margin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        state.close.gross_margin_amount?.let { amt ->
                            val pct = state.close.gross_margin_percent?.let { " (${String.format("%.1f", it)}%)" } ?: ""
                            "${CurrencyFormatter.format(amt)}$pct"
                        } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = marginColor,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text("Cumulative position", style = MaterialTheme.typography.labelMedium)
                DayCloseRow(
                    "Acquisition investment (all time)",
                    CurrencyFormatter.format(snap.cumulative.totalAcquisitionInvestment),
                )
                DayCloseRow(
                    "Collected revenue (all time)",
                    CurrencyFormatter.format(snap.cumulative.totalRevenueCollectedAllTime),
                )
                DayCloseRow(
                    "Outstanding inventory value",
                    CurrencyFormatter.format(snap.cumulative.outstandingInventoryValue),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Net recovered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        CurrencyFormatter.format(snap.cumulative.netRecovered),
                        style = MaterialTheme.typography.bodySmall,
                        color = netRecoveredColor,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text("Other outflows (today)", style = MaterialTheme.typography.labelMedium)
                DayCloseRow("Wages paid", CurrencyFormatter.format(snap.wagesTotalToday))
                DayCloseRow("Sales remittances", CurrencyFormatter.format(snap.remittedToday))
                DayCloseRow("Disbursements", CurrencyFormatter.format(snap.disbursementsToday))
            }
        }

        item {
            DayCloseSectionCard(title = "Inventory") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${displayInventory.size} products shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Spoilage cost (counted, +var): ${CurrencyFormatter.format(spoilageTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Row {
                        FilterChip(
                            selected = showZeroTheoretical,
                            onClick = { showZeroTheoretical = !showZeroTheoretical },
                            label = { Text("Zero theoretical") },
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = onRefreshInventory,
                            enabled = !state.close.is_finalized,
                        ) { Text("Refresh") }
                    }
                }
            }
        }

        itemsIndexed(displayInventory, key = { _, line -> line.product_id }) { _, line ->
            val unitLabel = snap.inventoryUnitByProduct[line.product_id] ?: "kg"
            val piecesPerKg = snap.inventoryPiecesPerKgByProduct[line.product_id]
            InventoryLineCard(
                line = line,
                unitLabel = unitLabel,
                piecesPerKg = piecesPerKg,
                soldTodayBreakdown = snap.inventorySoldTodayBreakdownByProduct[line.product_id],
                soldThroughCloseBreakdown = snap.inventorySoldThroughCloseBreakdownByProduct[line.product_id],
                revenueToday = snap.inventoryDayRevenueByProduct[line.product_id],
                lastAcqDetail = snap.lastAcqDetailByProduct[line.product_id],
                lastAcqMillis = snap.lastAcqMillisByProduct[line.product_id],
                nowMillis = nowMillis,
                canEdit = state.canEditInventoryCounts && !state.close.is_finalized,
                onEnterCount = { v -> onEnterCount(line.product_id, v) },
                onSetCounted = { counted -> onSetCounted(line.product_id, counted) },
            )
        }

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
                    Text(
                        "View full report →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            CashReconciliationCard(
                close = state.close,
                expectedCashOrders = snap.expectedCashFromOrders,
                remittedToday = snap.remittedToday,
                disbursementsToday = snap.disbursementsToday,
                digitalCount = snap.digital.order_count,
                digitalTotal = snap.digital.total_amount,
                drawerExpected = drawerExpected,
                diffExpectedRemitted = diffExpectedRemitted,
                canEdit = !state.close.is_finalized,
                onSave = onSaveCash,
            )
        }

        item {
            DayCloseSectionCard(title = "Outstanding orders (unpaid)") {
                val cap = 15
                val head = snap.unpaidOrders.take(cap)
                val more = (snap.unpaidOrders.size - cap).coerceAtLeast(0)
                val totalOutstanding = snap.unpaidOrders.sumOf { it.order.total_amount }
                DayCloseRow("Count", snap.unpaidOrders.size.toString())
                DayCloseRow("Total", CurrencyFormatter.format(totalOutstanding))
                head.forEach { od ->
                    val daysOld = ChronoUnit.DAYS.between(
                        Instant.ofEpochMilli(od.order.order_date).atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochMilli(state.close.business_date).atZone(ZoneId.systemDefault()).toLocalDate(),
                    )
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onOrderClick(od.order.order_id) }
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(
                                "#${od.order.order_id} · ${od.customerName}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "${SalesChannel.label(SalesChannel.normalize(od.order.channel))} · ${CurrencyFormatter.format(od.order.total_amount)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "Age: $daysOld d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (more > 0) {
                    Text(
                        "…and $more more (see list in app)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            if (state.close.is_finalized && state.isAdmin) {
                OutlinedButton(
                    onClick = onUnfinalize,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Un-finalize (admin)") }
                Spacer(Modifier.height(8.dp))
            }
            if (!state.close.is_finalized && !state.inReviewStep) {
                OutlinedButton(
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Review summary")
                }
                Spacer(Modifier.height(8.dp))
            }
            if (!state.close.is_finalized && state.inReviewStep) {
                Text(
                    "Review mode: verify all sections, then confirm finalize.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCancelReview,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back to edit")
                    }
                    Button(
                        onClick = onFinalize,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Confirm finalize")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (state.close.is_finalized) {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Text("Finalized")
                }
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun EmployeeNotesCard(
    notes: String?,
    canEdit: Boolean,
    onSaveNotes: (String?) -> Unit,
) {
    var notesText by remember { mutableStateOf(notes ?: "") }
    DayCloseSectionCard(title = "Wages due notes (optional)") {
        Text(
            "Use this to record wages due but not yet paid. This note is optional and does not block finalize.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canEdit) {
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                minLines = 2,
                keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Default),
            )
            TextButton(
                onClick = { onSaveNotes(notesText.ifBlank { null }) },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Save notes") }
        } else {
            Text(notes ?: "—", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DayCloseSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun DayCloseRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InventoryLineCard(
    line: DayCloseInventoryEntity,
    unitLabel: String,
    piecesPerKg: Double?,
    soldTodayBreakdown: ProductSoldQtyBreakdown?,
    soldThroughCloseBreakdown: ProductSoldQtyBreakdown?,
    revenueToday: Double?,
    lastAcqDetail: DayCloseRepository.LastAcquisitionDetail?,
    lastAcqMillis: Long?,
    nowMillis: Long,
    canEdit: Boolean,
    onEnterCount: (Double) -> Unit,
    onSetCounted: (Boolean) -> Unit,
) {
    fun dq(kgStored: Double): Double =
        DayCloseSoldQty.inventoryDisplayQuantity(kgStored, unitLabel, piecesPerKg)

    var countText by remember(line.product_id, line.actual_remaining, line.is_counted, unitLabel, piecesPerKg) {
        mutableStateOf(
            line.actual_remaining?.let { dq(it).toString() } ?: "",
        )
    }

    val agingDays = lastAcqMillis?.let { ((nowMillis - it) / (24 * 60 * 60 * 1000)).toInt() } ?: 0
    val agingHighlight = agingDays >= 3

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                line.product_name,
                style = MaterialTheme.typography.titleSmall,
                color = if (agingHighlight) AmberInventoryAging else MaterialTheme.colorScheme.onSurface,
            )
            DayCloseRow("Acquired (all time, $unitLabel)", String.format("%.3f", dq(line.total_acquired_all_time)))
            val soldThroughValue =
                if (soldThroughCloseBreakdown != null &&
                    (soldThroughCloseBreakdown.qty_kg_sold > 1e-9 || soldThroughCloseBreakdown.qty_pc_sold > 1e-9)
                ) {
                    DayCloseSoldQty.formatTopProductQtyLine(
                        soldThroughCloseBreakdown.qty_kg_sold,
                        soldThroughCloseBreakdown.qty_pc_sold,
                    )
                } else if (abs(line.total_sold_through_close_date) > 1e-9) {
                    String.format("%.3f %s", dq(line.total_sold_through_close_date), unitLabel)
                } else {
                    "—"
                }
            DayCloseRow("Sold through close", soldThroughValue)
            DayCloseRow("Prior posted var. ($unitLabel)", String.format("%.3f", dq(line.prior_posted_variance)))
            val theoreticalDisplay = DayCloseSoldQty.inventoryTheoreticalDisplayQuantity(
                acquiredKg = line.total_acquired_all_time,
                soldThroughBreakdown = soldThroughCloseBreakdown,
                ledgerSoldKg = line.total_sold_through_close_date,
                priorVarianceKg = line.prior_posted_variance,
                unitLabel = unitLabel,
                piecesPerKg = piecesPerKg,
            )
            DayCloseRow(
                "Theoretical (adj.)",
                String.format("%.3f %s", theoreticalDisplay, unitLabel),
            )
            val soldTodayValue =
                if (soldTodayBreakdown != null &&
                    (soldTodayBreakdown.qty_kg_sold > 1e-9 || soldTodayBreakdown.qty_pc_sold > 1e-9)
                ) {
                    val q = DayCloseSoldQty.formatTopProductQtyLine(
                        soldTodayBreakdown.qty_kg_sold,
                        soldTodayBreakdown.qty_pc_sold,
                    )
                    "${q} · ${CurrencyFormatter.format(revenueToday ?: 0.0)}"
                } else if (abs(line.sold_this_close_date) > 1e-9) {
                    String.format("%.3f %s", dq(line.sold_this_close_date), unitLabel)
                } else {
                    "—"
                }
            DayCloseRow("Sold today", soldTodayValue)
            val lastMs = lastAcqDetail?.dateMillis ?: lastAcqMillis
            lastMs?.let { ms ->
                val fmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()) }
                val d = remember(ms) {
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(fmt)
                }
                DayCloseRow("Last acquisition", d)
            }
            lastAcqDetail?.let { detail ->
                DayCloseRow(
                    "Last qty / cost",
                    "${String.format("%.3f", detail.quantity)} ${detail.unitLabel} @ " +
                        "${CurrencyFormatter.format(detail.pricePerUnit)}/${detail.unitLabel}",
                )
            }
            line.variance_qty?.let { v ->
                DayCloseRow(
                    "Variance",
                    String.format(
                        "%.3f %s (${CurrencyFormatter.format(line.variance_cost ?: 0.0)})",
                        dq(v),
                        unitLabel,
                    ),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Include in count", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = line.is_counted,
                    onCheckedChange = onSetCounted,
                    enabled = canEdit,
                )
            }
            if (line.is_counted && canEdit) {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Actual count ($unitLabel)") },
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
            } else if (!line.is_counted) {
                Text("Excluded from spoilage total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            } else {
                line.actual_remaining?.let {
                    DayCloseRow("Actual count", String.format("%.3f %s", dq(it), unitLabel))
                }
            }
        }
    }
}

@Composable
private fun CashReconciliationCard(
    close: DayCloseEntity,
    expectedCashOrders: Double,
    remittedToday: Double,
    disbursementsToday: Double,
    digitalCount: Int,
    digitalTotal: Double,
    drawerExpected: Double,
    diffExpectedRemitted: Double,
    canEdit: Boolean,
    onSave: (Double?, String?) -> Unit,
) {
    var cashText by remember { mutableStateOf(close.cash_on_hand?.toString() ?: "") }
    var remarksText by remember { mutableStateOf(close.cash_reconciliation_remarks ?: "") }

    val diffColor = when {
        abs(diffExpectedRemitted) < 0.01 -> Color(0xFF2E7D32)
        diffExpectedRemitted > 0 -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.error
    }
    val counted = close.cash_on_hand
    val drawerVsHandColor = if (counted == null) {
        MaterialTheme.colorScheme.onSurface
    } else {
        when {
            abs(counted - drawerExpected) < 0.01 -> Color(0xFF2E7D32)
            counted > drawerExpected -> Color(0xFF2E7D32)
            else -> MaterialTheme.colorScheme.error
        }
    }

    DayCloseSectionCard(title = "Cash reconciliation") {
        Text(
            "Expected cash from orders = paid offline + reseller today.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Finalize rule: non-zero discrepancy requires remarks. " +
                "Uses counted cash when entered; otherwise expected minus remitted.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DayCloseRow("Expected (paid off+res)", CurrencyFormatter.format(expectedCashOrders))
        DayCloseRow("Remitted today", CurrencyFormatter.format(remittedToday))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Expected − remitted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                CurrencyFormatter.format(diffExpectedRemitted),
                style = MaterialTheme.typography.bodySmall,
                color = diffColor,
            )
        }
        DayCloseRow("Drawer expectation", CurrencyFormatter.format(drawerExpected))
        DayCloseRow("Disbursements (info)", CurrencyFormatter.format(disbursementsToday))
        Spacer(Modifier.height(4.dp))
        Text("Digital collections (online)", style = MaterialTheme.typography.labelMedium)
        DayCloseRow("Orders", digitalCount.toString())
        DayCloseRow("Amount", CurrencyFormatter.format(digitalTotal))

        if (canEdit) {
            OutlinedTextField(
                value = cashText,
                onValueChange = { cashText = it },
                label = { Text("Cash on hand (counted)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
            )
            counted?.let { c ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Discrepancy (counted - drawer)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        CurrencyFormatter.format(c - drawerExpected),
                        style = MaterialTheme.typography.bodySmall,
                        color = drawerVsHandColor,
                    )
                }
            }
            OutlinedTextField(
                value = remarksText,
                onValueChange = { remarksText = it },
                label = { Text("Reconciliation remarks") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                minLines = 2,
                keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Default),
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
