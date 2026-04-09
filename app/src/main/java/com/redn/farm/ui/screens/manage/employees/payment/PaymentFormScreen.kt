package com.redn.farm.ui.screens.manage.employees.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.utils.buildEmployeePaymentVoucher
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SignatureMode { DRAW, TYPE }

private fun isBase64ish(value: String): Boolean =
    value.contains("/") || value.contains("+") || value.contains("=")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFormScreen(
    employeeId: Int,
    employeeName: String,
    paymentId: Int,
    onNavigateBack: () -> Unit,
    viewModel: EmployeePaymentViewModel = hiltViewModel()
) {
    val isNew = paymentId <= 0
    val payments by viewModel.payments.collectAsState(initial = emptyList())
    val existing: EmployeePayment? =
        if (isNew) null else payments.find { it.payment_id == paymentId }
    val isFinalized = !isNew && existing?.is_finalized == true

    var grossWage by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var signatureMode by remember { mutableStateOf(SignatureMode.TYPE) }
    var datePaid by remember { mutableStateOf(System.currentTimeMillis()) }
    var dateReceived by remember { mutableStateOf<Long?>(null) }
    var cashAdvanceAmount by remember { mutableStateOf("") }
    var liquidatedAmount by remember { mutableStateOf("") }

    var showDatePaidPicker by remember { mutableStateOf(false) }
    var showDateReceivedPicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(paymentId, payments) {
        if (!isNew && payments.isNotEmpty() && payments.none { it.payment_id == paymentId }) {
            onNavigateBack()
        }
    }

    LaunchedEffect(paymentId) {
        if (paymentId > 0) return@LaunchedEffect
        grossWage = ""
        signature = ""
        signatureMode = SignatureMode.TYPE
        datePaid = System.currentTimeMillis()
        dateReceived = null
        cashAdvanceAmount = ""
        liquidatedAmount = ""
    }

    var seededEditId by remember(paymentId) { mutableIntStateOf(Int.MIN_VALUE) }
    LaunchedEffect(paymentId, existing) {
        if (isNew || existing == null) return@LaunchedEffect
        if (seededEditId == paymentId) return@LaunchedEffect
        grossWage = existing.amount.toString()
        signature = existing.signature
        signatureMode = if (isBase64ish(existing.signature)) SignatureMode.DRAW else SignatureMode.TYPE
        datePaid = existing.date_paid
        dateReceived = existing.received_date
        cashAdvanceAmount = existing.cash_advance_amount?.toString() ?: ""
        liquidatedAmount = existing.liquidated_amount?.toString() ?: ""
        seededEditId = paymentId
    }

    val grossValue = grossWage.toDoubleOrNull() ?: 0.0
    val advanceValue = cashAdvanceAmount.toDoubleOrNull() ?: 0.0
    val liquidatedValue = liquidatedAmount.toDoubleOrNull() ?: 0.0
    val netPay = grossValue + advanceValue
    val anyAmountFieldEntered =
        listOf(grossWage, cashAdvanceAmount).any { it.isNotBlank() }
    val netPayNegativeWarning = netPay < 0 && anyAmountFieldEntered

    val grossParsed = grossWage.toDoubleOrNull()
    val grossValid = grossParsed != null && grossParsed > 0

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val grossFocus = remember { FocusRequester() }
    val advanceFocus = remember { FocusRequester() }
    val liquidatedFocus = remember { FocusRequester() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isFinalized -> "View payment"
                            isNew -> "Add payment"
                            else -> "Edit payment"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val printVoucher: () -> Unit = printLambda@{
                        if (!grossValid || signature.isBlank()) {
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar("Enter gross wage and signature to print.")
                            }
                            return@printLambda
                        }
                        snackbarScope.launch {
                            val content = buildEmployeePaymentVoucher(
                                employeeName = employeeName,
                                datePaidMillis = datePaid,
                                receivedMillis = dateReceived,
                                gross = grossValue,
                                cashAdvance = advanceValue,
                                liquidated = liquidatedValue,
                                netPay = netPay,
                            )
                            val ok = PrinterUtils.printMessage(context, content, alignment = 0)
                            snackbarHostState.showSnackbar(
                                if (ok) "Sent to printer" else "Print failed — check printer"
                            )
                        }
                    }
                    if (isFinalized) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back")
                        }
                        OutlinedButton(
                            onClick = printVoucher,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = grossValid && signature.isNotBlank(),
                        ) {
                            Text("Print Voucher")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (!grossValid) {
                                        snackbarScope.launch {
                                            snackbarHostState.showSnackbar("Enter a gross wage greater than zero.")
                                        }
                                        return@Button
                                    }
                                    val g = grossWage.toDoubleOrNull() ?: return@Button
                                    if (isNew) {
                                        viewModel.addPayment(
                                            EmployeePayment(
                                                employee_id = employeeId,
                                                amount = g,
                                                signature = signature,
                                                date_paid = datePaid,
                                                received_date = dateReceived,
                                                cash_advance_amount = cashAdvanceAmount.toDoubleOrNull(),
                                                liquidated_amount = liquidatedAmount.toDoubleOrNull(),
                                                is_finalized = false,
                                            )
                                        )
                                    } else {
                                        val base = existing ?: return@Button
                                        viewModel.updatePayment(
                                            base.copy(
                                                amount = g,
                                                signature = signature,
                                                date_paid = datePaid,
                                                received_date = dateReceived,
                                                cash_advance_amount = cashAdvanceAmount.toDoubleOrNull(),
                                                liquidated_amount = liquidatedAmount.toDoubleOrNull(),
                                                is_finalized = false,
                                            )
                                        )
                                    }
                                    onNavigateBack()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save")
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                if (!grossValid) {
                                    snackbarScope.launch {
                                        snackbarHostState.showSnackbar("Enter a gross wage greater than zero.")
                                    }
                                    return@FilledTonalButton
                                }
                                if (signature.isBlank()) {
                                    snackbarScope.launch {
                                        snackbarHostState.showSnackbar("Signature is required to finalize.")
                                    }
                                    return@FilledTonalButton
                                }
                                val g = grossWage.toDoubleOrNull() ?: return@FilledTonalButton
                                if (isNew) {
                                    viewModel.addPayment(
                                        EmployeePayment(
                                            employee_id = employeeId,
                                            amount = g,
                                            signature = signature,
                                            date_paid = datePaid,
                                            received_date = dateReceived,
                                            cash_advance_amount = cashAdvanceAmount.toDoubleOrNull(),
                                            liquidated_amount = liquidatedAmount.toDoubleOrNull(),
                                            is_finalized = true,
                                        )
                                    )
                                } else {
                                    val base = existing ?: return@FilledTonalButton
                                    viewModel.updatePayment(
                                        base.copy(
                                            amount = g,
                                            signature = signature,
                                            date_paid = datePaid,
                                            received_date = dateReceived,
                                            cash_advance_amount = cashAdvanceAmount.toDoubleOrNull(),
                                            liquidated_amount = liquidatedAmount.toDoubleOrNull(),
                                            is_finalized = true,
                                        )
                                    )
                                }
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Finalize")
                        }
                        OutlinedButton(
                            onClick = printVoucher,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = grossValid && signature.isNotBlank(),
                        ) {
                            Text("Print Voucher")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Employee: $employeeName",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isFinalized) {
                    Text(
                        "Finalized — read only",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = grossWage,
                    onValueChange = { grossWage = it },
                    readOnly = isFinalized,
                    label = { Text("Gross wage (required)") },
                    supportingText = {
                        if (grossWage.isNotBlank() && !grossValid) {
                            Text("Must be a number greater than zero.")
                        }
                    },
                    isError = grossWage.isNotBlank() && !grossValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { advanceFocus.requestFocus() },
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(grossFocus),
                )

                OutlinedTextField(
                    value = cashAdvanceAmount,
                    onValueChange = { cashAdvanceAmount = it },
                    readOnly = isFinalized,
                    label = { Text("Cash advance (optional)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { liquidatedFocus.requestFocus() },
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(advanceFocus),
                )

                OutlinedTextField(
                    value = liquidatedAmount,
                    onValueChange = { liquidatedAmount = it },
                    readOnly = isFinalized,
                    label = { Text("Liquidated (optional)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(liquidatedFocus),
                )

                NetPaySummaryCard(
                    gross = grossValue,
                    cashAdvance = advanceValue,
                    liquidated = liquidatedValue,
                    netPay = netPay,
                    netPayNegativeWarning = netPayNegativeWarning
                )

                Text(
                    if (isFinalized) "Signature"
                    else "Signature (optional for save — required to finalize or print voucher)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isFinalized) {
                    Text(
                        text = "Signature: ${
                            if (signature.isNotBlank() && isBase64ish(signature)) {
                                "Captured (image)"
                            } else {
                                signature.ifBlank { "—" }
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = signatureMode == SignatureMode.DRAW,
                            onClick = {
                                signatureMode = SignatureMode.DRAW
                                signature = ""
                            },
                            label = { Text("Draw") }
                        )
                        FilterChip(
                            selected = signatureMode == SignatureMode.TYPE,
                            onClick = {
                                signatureMode = SignatureMode.TYPE
                                signature = ""
                            },
                            label = { Text("Type") }
                        )
                    }

                    if (signatureMode == SignatureMode.DRAW) {
                        Text(
                            "Sign here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SignatureCanvasField(
                            modifier = Modifier.fillMaxWidth(),
                            onSignatureBase64Change = { signature = it }
                        )
                    } else {
                        OutlinedTextField(
                            value = signature,
                            onValueChange = { signature = it },
                            label = { Text("Signature (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isFinalized) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Date paid (required)", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    dateFormatter.format(Date(datePaid)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    }
                } else {
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
                                Text("Date paid (required)", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    dateFormatter.format(Date(datePaid)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    }
                }

                if (isFinalized) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Date received (optional)", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    dateReceived?.let { dateFormatter.format(Date(it)) } ?: "Not set",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    }
                } else {
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
                                Text("Date received (optional)", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    dateReceived?.let { dateFormatter.format(Date(it)) } ?: "Not set",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(8.dp))
            }
        }
    }

    if (showDatePaidPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = datePaid)
        DatePickerDialog(
            onDismissRequest = { showDatePaidPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { datePaid = it }
                        showDatePaidPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePaidPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

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
                            if (it >= datePaid) dateReceived = it
                        }
                        showDateReceivedPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateReceivedPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun NetPaySummaryCard(
    gross: Double,
    cashAdvance: Double,
    liquidated: Double,
    netPay: Double,
    netPayNegativeWarning: Boolean
) {
    val amber = Color(0xFFFF8F00)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Summary", style = MaterialTheme.typography.labelLarge)
            SummaryMoneyRow(label = "Gross wage", amount = gross, prefix = "")
            SummaryMoneyRow(label = "Cash advance", amount = cashAdvance, summaryPrefix = SummaryPrefix.InNetPayPlus)
            SummaryLiquidatedRow(amount = liquidated)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Net pay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    CurrencyFormatter.format(netPay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netPayNegativeWarning) amber else MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    textAlign = TextAlign.End
                )
            }
            if (netPayNegativeWarning) {
                Text(
                    "Net pay is negative.",
                    style = MaterialTheme.typography.bodySmall,
                    color = amber,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private enum class SummaryPrefix { Plain, InNetPayPlus }

@Composable
private fun SummaryMoneyRow(
    label: String,
    amount: Double,
    prefix: String = "",
    summaryPrefix: SummaryPrefix = SummaryPrefix.Plain
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = when (summaryPrefix) {
                SummaryPrefix.InNetPayPlus -> "+ ${CurrencyFormatter.format(amount)}"
                SummaryPrefix.Plain -> when (prefix) {
                    "−" -> "− ${CurrencyFormatter.format(amount)}"
                    "+" -> "+ ${CurrencyFormatter.format(amount)}"
                    else -> CurrencyFormatter.format(amount)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SummaryLiquidatedRow(amount: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Liquidated:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.format(amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Recorded only — not included in net pay.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
    }
}
