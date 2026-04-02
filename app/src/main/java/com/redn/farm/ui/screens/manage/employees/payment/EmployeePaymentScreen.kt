package com.redn.farm.ui.screens.manage.employees.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.data.model.lifetimeOutstandingAdvance
import com.redn.farm.data.model.periodTotals
import com.redn.farm.utils.CurrencyFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePaymentScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaymentForm: (paymentId: Int) -> Unit,
    employeeId: Int,
    employeeName: String,
    viewModel: EmployeePaymentViewModel = viewModel(
        factory = EmployeePaymentViewModel.Factory
    )
) {
    var showDeleteDialog by remember { mutableStateOf<EmployeePayment?>(null) }
    var selectedPeriod by remember { mutableStateOf(DateFilterPeriod.ALL) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    val payments by viewModel.payments.collectAsState(initial = emptyList())

    val employeePayments = remember(payments, employeeId) {
        payments.filter { it.employee_id == employeeId }
    }

    val periodStartMillis = remember(selectedPeriod) { startOfFilterPeriodMillis(selectedPeriod) }

    val filteredPayments = remember(employeePayments, selectedPeriod, periodStartMillis) {
        employeePayments.filter { payment ->
            when (selectedPeriod) {
                DateFilterPeriod.ALL -> true
                else -> payment.date_paid >= periodStartMillis
            }
        }
    }

    val lifetimeOutstanding = remember(employeePayments) {
        employeePayments.lifetimeOutstandingAdvance()
    }

    val periodTotals = remember(filteredPayments) { filteredPayments.periodTotals() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employee Payments - $employeeName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToPaymentForm(-1) }) {
                        Icon(Icons.Default.Add, "Add Payment")
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
            // Date Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            "Select period",
                            Modifier.padding(start = 8.dp)
                        )
                    }

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                    ) {
                        DateFilterPeriod.values().forEach { period ->
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    EmployeePaymentOutstandingCard(
                        outstanding = lifetimeOutstanding,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    EmployeePaymentPeriodSummaryCard(
                        paymentCount = filteredPayments.size,
                        totalGross = periodTotals.totalGross,
                        totalCashAdvance = periodTotals.totalCashAdvance,
                        totalLiquidated = periodTotals.totalLiquidated,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
    THIS_MONTH("This Month")
}

private fun startOfFilterPeriodMillis(period: DateFilterPeriod): Long {
    val cal = Calendar.getInstance()
    when (period) {
        DateFilterPeriod.ALL -> return 0L
        DateFilterPeriod.TODAY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        DateFilterPeriod.THIS_WEEK -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        DateFilterPeriod.THIS_MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
    }
    return cal.timeInMillis
}

@Composable
private fun EmployeePaymentOutstandingCard(
    outstanding: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Outstanding advance (all time)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "All payments for this employee; ignores the period filter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = CurrencyFormatter.format(outstanding),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EmployeePaymentPeriodSummaryCard(
    paymentCount: Int,
    totalGross: Double,
    totalCashAdvance: Double,
    totalLiquidated: Double,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Period summary ($paymentCount ${if (paymentCount == 1) "payment" else "payments"})",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Matches the filter above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            SummaryAmountRow(label = "Total gross", amount = totalGross)
            SummaryAmountRow(label = "Total cash advances", amount = totalCashAdvance)
            SummaryAmountRow(label = "Total liquidated", amount = totalLiquidated)
        }
    }
}

@Composable
private fun SummaryAmountRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}