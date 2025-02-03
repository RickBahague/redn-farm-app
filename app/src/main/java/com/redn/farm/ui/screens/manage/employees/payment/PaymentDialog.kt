package com.redn.farm.ui.screens.manage.employees.payment

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.redn.farm.data.model.EmployeePayment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    payment: EmployeePayment?,
    employeeId: Int,
    employeeName: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, signature: String, datePaid: Long, dateReceived: Long?, 
                cashAdvanceAmount: Double?, liquidatedAmount: Double?) -> Unit
) {
    // Add logging to check the incoming payment values
    LaunchedEffect(payment) {
        Log.d("PaymentDialog", "Payment: $payment")
        Log.d("PaymentDialog", "Cash Advance: ${payment?.cash_advance_amount}")
        Log.d("PaymentDialog", "Liquidated: ${payment?.liquidated_amount}")
    }

    var amount by remember { mutableStateOf(payment?.amount?.toString() ?: "") }
    var signature by remember { mutableStateOf(payment?.signature ?: "") }
    var datePaid by remember { mutableStateOf(payment?.date_paid ?: System.currentTimeMillis()) }
    var dateReceived by remember { mutableStateOf(payment?.received_date) }
    var cashAdvanceAmount by remember(payment) { 
        mutableStateOf(payment?.cash_advance_amount?.toString() ?: "") 
    }
    var liquidatedAmount by remember(payment) { 
        mutableStateOf(payment?.liquidated_amount?.toString() ?: "") 
    }

    // Add logging for state changes
    LaunchedEffect(cashAdvanceAmount, liquidatedAmount) {
        Log.d("PaymentDialog", "Cash Advance State: $cashAdvanceAmount")
        Log.d("PaymentDialog", "Liquidated State: $liquidatedAmount")
    }
    
    var showDatePaidPicker by remember { mutableStateOf(false) }
    var showDateReceivedPicker by remember { mutableStateOf(false) }
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (payment == null) "Add Payment" else "Edit Payment") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Employee: $employeeName",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = cashAdvanceAmount,
                    onValueChange = { cashAdvanceAmount = it },
                    label = { Text("Cash Advance Amount (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = liquidatedAmount,
                    onValueChange = { liquidatedAmount = it },
                    label = { Text("Liquidated Amount (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = signature,
                    onValueChange = { signature = it },
                    label = { Text("Signature") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Paid Selection
                OutlinedCard(
                    onClick = { showDatePaidPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Date Paid",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = dateFormatter.format(Date(datePaid)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(Icons.Default.CalendarMonth, "Select date")
                    }
                }

                // Date Received Selection
                OutlinedCard(
                    onClick = { showDateReceivedPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Date Received (Optional)",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = dateReceived?.let { dateFormatter.format(Date(it)) } ?: "Not set",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(Icons.Default.CalendarMonth, "Select date")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount.toDoubleOrNull()?.let { amountValue ->
                        if (signature.isNotBlank()) {
                            onConfirm(
                                amountValue,
                                signature,
                                datePaid,
                                dateReceived,
                                cashAdvanceAmount.toDoubleOrNull(),
                                liquidatedAmount.toDoubleOrNull()
                            )
                        }
                    }
                },
                enabled = amount.toDoubleOrNull() != null && signature.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Date Paid Picker Dialog
    if (showDatePaidPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = datePaid
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePaidPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            datePaid = it
                        }
                        showDatePaidPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePaidPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    // Date Received Picker Dialog
    if (showDateReceivedPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateReceived ?: System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDateReceivedPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            if (it >= datePaid) {
                                dateReceived = it
                            }
                        }
                        showDateReceivedPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateReceivedPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
} 