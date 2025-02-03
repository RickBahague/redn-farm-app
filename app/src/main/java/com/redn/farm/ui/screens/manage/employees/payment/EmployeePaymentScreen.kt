package com.redn.farm.ui.screens.manage.employees.payment

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*
import com.redn.farm.ui.screens.manage.employees.payment.PaymentDialog
import com.redn.farm.ui.screens.manage.employees.payment.PaymentCard
import androidx.compose.material.icons.filled.ArrowDropDown
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePaymentScreen(
    onNavigateBack: () -> Unit,
    employeeId: Int,
    employeeName: String,
    viewModel: EmployeePaymentViewModel = viewModel(
        factory = EmployeePaymentViewModel.Factory
    )
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<EmployeePayment?>(null) }
    var showDeleteDialog by remember { mutableStateOf<EmployeePayment?>(null) }
    var selectedPeriod by remember { mutableStateOf(DateFilterPeriod.ALL) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    val payments by viewModel.payments.collectAsState(initial = emptyList())
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employee Payments - $employeeName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
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

            // Payments list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredPayments = payments.filter { payment -> 
                    payment.employee_id == employeeId &&
                    when (selectedPeriod) {
                        DateFilterPeriod.TODAY -> {
                            val today = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            payment.date_paid >= today
                        }
                        DateFilterPeriod.THIS_WEEK -> {
                            val weekStart = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            payment.date_paid >= weekStart
                        }
                        DateFilterPeriod.THIS_MONTH -> {
                            val monthStart = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            payment.date_paid >= monthStart
                        }
                        DateFilterPeriod.ALL -> true
                    }
                }
                
                items(filteredPayments) { payment ->
                    PaymentCard(
                        payment = payment,
                        onEditClick = { showEditDialog = payment },
                        onDeleteClick = { showDeleteDialog = payment }
                    )
                }
            }
        }

        // Add/Edit Dialog
        if (showAddDialog || showEditDialog != null) {
            PaymentDialog(
                payment = showEditDialog,
                employeeId = employeeId,
                employeeName = employeeName,
                onDismiss = {
                    showAddDialog = false
                    showEditDialog = null
                },
                onConfirm = { amount, signature, datePaid, dateReceived, cashAdvanceAmount, liquidatedAmount ->
                    if (showEditDialog != null) {
                        viewModel.updatePayment(
                            showEditDialog!!.copy(
                                amount = amount,
                                signature = signature,
                                date_paid = datePaid,
                                received_date = dateReceived,
                                cash_advance_amount = cashAdvanceAmount,
                                liquidated_amount = liquidatedAmount
                            )
                        )
                    } else {
                        viewModel.addPayment(
                            EmployeePayment(
                                employee_id = employeeId,
                                amount = amount,
                                signature = signature,
                                date_paid = datePaid,
                                received_date = dateReceived,
                                cash_advance_amount = cashAdvanceAmount,
                                liquidated_amount = liquidatedAmount
                            )
                        )
                    }
                    showAddDialog = false
                    showEditDialog = null
                }
            )
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