package com.redn.farm.ui.screens.manage.employees.payment

import android.app.DatePickerDialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.data.model.EmployeePaymentPeriodTotals
import com.redn.farm.data.model.lifetimeOutstandingAdvance
import com.redn.farm.data.model.netPayAmount
import com.redn.farm.data.model.periodTotals
import com.redn.farm.utils.buildEmployeePayrollSummary
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePaymentScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaymentForm: (paymentId: Int) -> Unit,
    employeeId: Int,
    employeeName: String,
    viewModel: EmployeePaymentViewModel = hiltViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf<EmployeePayment?>(null) }
    var selectedPeriod by remember { mutableStateOf(DateFilterPeriod.ALL) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var summaryExpanded by remember { mutableStateOf(false) }
    var customFromMillis by remember { mutableStateOf<Long?>(null) }
    var customToMillis by remember { mutableStateOf<Long?>(null) }
    
    val payments by viewModel.payments.collectAsState(initial = emptyList())

    val employeePayments = remember(payments, employeeId) {
        payments.filter { it.employee_id == employeeId }
    }

    val filteredPayments = remember(employeePayments, selectedPeriod, customFromMillis, customToMillis) {
        employeePayments.filter { payment ->
            matchesPeriod(payment.date_paid, selectedPeriod, customFromMillis, customToMillis)
        }
    }

    val lifetimeOutstanding = remember(employeePayments) {
        employeePayments.lifetimeOutstandingAdvance()
    }

    val periodTotals = remember(filteredPayments) { filteredPayments.periodTotals() }
    val periodNetPay = remember(filteredPayments) {
        filteredPayments.sumOf { it.netPayAmount() }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun printSummary() {
        scope.launch {
            val content = buildEmployeePayrollSummary(
                employeeName = employeeName,
                periodLabel = selectedPeriod.toSummaryLabel(customFromMillis, customToMillis),
                filteredPayments = filteredPayments,
                allEmployeePayments = employeePayments,
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
                title = { Text("Employee Payments - $employeeName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { printSummary() },
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print summary")
                    }
                    IconButton(onClick = { onNavigateToPaymentForm(-1) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add payment")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by:",
                    style = MaterialTheme.typography.bodyMedium
                )
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it },
                ) {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        modifier = Modifier.menuAnchor()
                    ) {
                        Text(selectedPeriod.label)
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select period",
                            Modifier.padding(start = 8.dp)
                        )
                    }

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                    ) {
                        DateFilterPeriod.entries.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.label) },
                                onClick = {
                                    selectedPeriod = period
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (selectedPeriod == DateFilterPeriod.CUSTOM_RANGE) {
                CustomRangeControls(
                    fromMillis = customFromMillis,
                    toMillis = customToMillis,
                    onPickFrom = {
                        showDatePicker(context, customFromMillis) { picked ->
                            customFromMillis = startOfDay(picked)
                            if (customToMillis != null && customToMillis!! < customFromMillis!!) {
                                customToMillis = endOfDay(picked)
                            }
                        }
                    },
                    onPickTo = {
                        showDatePicker(context, customToMillis ?: customFromMillis) { picked ->
                            customToMillis = endOfDay(picked)
                        }
                    },
                    onClear = {
                        customFromMillis = null
                        customToMillis = null
                    },
                )
            }

            EmployeePaymentSummaryBanner(
                outstanding = lifetimeOutstanding,
                periodLabel = selectedPeriod.toSummaryLabel(customFromMillis, customToMillis),
                paymentCount = filteredPayments.size,
                periodNetPay = periodNetPay,
                periodTotals = periodTotals,
                expanded = summaryExpanded,
                onToggleExpand = { summaryExpanded = !summaryExpanded },
                onPrintClick = { printSummary() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPayments) { payment ->
                    PaymentCard(
                        payment = payment,
                        onEditClick = { onNavigateToPaymentForm(payment.payment_id) },
                        onDeleteClick = { showDeleteDialog = payment }
                    )
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { payment ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Payment") },
                text = { Text("Are you sure you want to delete this payment?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePayment(payment)
                            showDeleteDialog = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

enum class DateFilterPeriod(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    CUSTOM_RANGE("Custom Range")
}

private fun DateFilterPeriod.toSummaryLabel(
    customFromMillis: Long?,
    customToMillis: Long?,
): String = when (this) {
    DateFilterPeriod.ALL -> "All Time"
    DateFilterPeriod.TODAY -> "Today"
    DateFilterPeriod.THIS_WEEK -> "This Week"
    DateFilterPeriod.THIS_MONTH ->
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
    DateFilterPeriod.LAST_MONTH -> "Last Month"
    DateFilterPeriod.LAST_3_MONTHS -> "Last 3 Months"
    DateFilterPeriod.LAST_6_MONTHS -> "Last 6 Months"
    DateFilterPeriod.CUSTOM_RANGE -> {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val from = customFromMillis?.let { fmt.format(Date(it)) } ?: "…"
        val to = customToMillis?.let { fmt.format(Date(it)) } ?: "…"
        "Custom: $from - $to"
    }
}

private fun startOfDay(epochMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun endOfDay(epochMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMillis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

private fun startOfFilterPeriodMillis(period: DateFilterPeriod): Long {
    val cal = Calendar.getInstance()
    when (period) {
        DateFilterPeriod.ALL -> return 0L
        DateFilterPeriod.TODAY -> {
            return startOfDay(cal.timeInMillis)
        }
        DateFilterPeriod.THIS_WEEK -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            return startOfDay(cal.timeInMillis)
        }
        DateFilterPeriod.THIS_MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            return startOfDay(cal.timeInMillis)
        }
        DateFilterPeriod.LAST_MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -1)
            return startOfDay(cal.timeInMillis)
        }
        DateFilterPeriod.LAST_3_MONTHS -> {
            cal.add(Calendar.MONTH, -3)
            return startOfDay(cal.timeInMillis)
        }
        DateFilterPeriod.LAST_6_MONTHS -> {
            cal.add(Calendar.MONTH, -6)
            return startOfDay(cal.timeInMillis)
        }
        DateFilterPeriod.CUSTOM_RANGE -> return 0L
    }
}

private fun matchesPeriod(
    paidAtMillis: Long,
    period: DateFilterPeriod,
    customFromMillis: Long?,
    customToMillis: Long?,
): Boolean {
    return when (period) {
        DateFilterPeriod.ALL -> true
        DateFilterPeriod.CUSTOM_RANGE -> {
            val from = customFromMillis ?: return true
            val to = customToMillis ?: Long.MAX_VALUE
            paidAtMillis in from..to
        }
        DateFilterPeriod.LAST_MONTH -> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startCurrentMonth = startOfDay(cal.timeInMillis)
            cal.add(Calendar.MONTH, -1)
            val startLastMonth = startOfDay(cal.timeInMillis)
            paidAtMillis in startLastMonth until startCurrentMonth
        }
        else -> paidAtMillis >= startOfFilterPeriodMillis(period)
    }
}

private fun showDatePicker(
    context: android.content.Context,
    initialMillis: Long?,
    onPicked: (Long) -> Unit,
) {
    val cal = Calendar.getInstance().apply {
        if (initialMillis != null) timeInMillis = initialMillis
    }
    DatePickerDialog(
        context,
        { _, y, m, d ->
            val picked = Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onPicked(picked.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH),
    ).show()
}

@Composable
private fun CustomRangeControls(
    fromMillis: Long?,
    toMillis: Long?,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onClear: () -> Unit,
) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val fromText = fromMillis?.let { fmt.format(Date(it)) } ?: "Pick start"
    val toText = toMillis?.let { fmt.format(Date(it)) } ?: "Pick end"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onPickFrom, modifier = Modifier.weight(1f)) { Text(fromText) }
        OutlinedButton(onClick = onPickTo, modifier = Modifier.weight(1f)) { Text(toText) }
        TextButton(onClick = onClear) { Text("Clear") }
    }
}

@Composable
private fun EmployeePaymentSummaryBanner(
    outstanding: Double,
    periodLabel: String,
    paymentCount: Int,
    periodNetPay: Double,
    periodTotals: EmployeePaymentPeriodTotals,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onPrintClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val payWord = if (paymentCount == 1) "payment" else "payments"
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleExpand),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Outstanding (all time)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                        )
                        Text(
                            text = CurrencyFormatter.format(outstanding),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "$periodLabel · $paymentCount $payWord",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                        )
                        Text(
                            text = "Net ${CurrencyFormatter.format(periodNetPay)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse summary" else "Expand summary",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                IconButton(onClick = onPrintClick) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = "Print summary",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
                    Text(
                        text = "Outstanding = sum(cash advances) − sum(liquidated) for this employee (ignores period filter).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                    )
                    Text(
                        text = "Period totals match the filter above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                    )
                    SummaryAmountRowTinted(
                        label = "Total gross",
                        amount = periodTotals.totalGross,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        amountColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    SummaryAmountRowTinted(
                        label = "Total cash advances",
                        amount = periodTotals.totalCashAdvance,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        amountColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    SummaryAmountRowTinted(
                        label = "Total liquidated",
                        amount = periodTotals.totalLiquidated,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        amountColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    SummaryAmountRowTinted(
                        label = "Net paid (gross + advances in period)",
                        amount = periodNetPay,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        amountColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryAmountRowTinted(
    label: String,
    amount: Double,
    labelColor: Color,
    amountColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = labelColor)
        Text(
            text = CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.bodySmall,
            color = amountColor,
        )
    }
}