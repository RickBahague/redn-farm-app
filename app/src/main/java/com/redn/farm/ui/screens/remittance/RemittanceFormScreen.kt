package com.redn.farm.ui.screens.remittance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.redn.farm.data.model.RemittanceEntryType
import com.redn.farm.ui.components.NumericPadOutlinedTextField
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemittanceFormScreen(
    remittanceIdKey: String,
    onNavigateBack: () -> Unit,
    viewModel: RemittanceViewModel
) {
    val isNew = remittanceIdKey == "new" || remittanceIdKey == "new_disbursement"
    val isNewDisbursement = remittanceIdKey == "new_disbursement"
    val remittances by viewModel.remittances.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val existing = remember(remittanceIdKey, remittances) {
        if (isNew) null else remittances.find { it.remittance_id == remittanceIdKey.toIntOrNull() }
    }

    if (!isNew && remittances.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isNew && remittances.isNotEmpty() && existing == null) {
        LaunchedEffect(remittanceIdKey, remittances) {
            onNavigateBack()
        }
        return
    }

    var amount by remember(remittanceIdKey) { mutableStateOf("") }
    var remarks by remember(remittanceIdKey) { mutableStateOf("") }
    var isAmountError by remember(remittanceIdKey) { mutableStateOf(false) }
    var selectedDateMillis by remember(remittanceIdKey) {
        mutableStateOf(
            if (isNew) System.currentTimeMillis() else checkNotNull(existing).date
        )
    }
    var showDatePicker by remember(remittanceIdKey) { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(existing) {
        val r = existing ?: return@LaunchedEffect
        val a = r.amount
        amount = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
        remarks = r.remarks
        selectedDateMillis = r.date
    }

    val amountValue = amount.toDoubleOrNull()
    val canSave = amountValue != null && amountValue > 0 && !isSaving

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.saveSucceeded.collect {
            onNavigateBack()
        }
    }

    fun performSave() {
        val amt = amount.toDoubleOrNull()
        val millis = selectedDateMillis
        if (amt == null || amt <= 0) {
            isAmountError = true
            return
        }
        isAmountError = false
        if (isNew) {
            val entryType = if (isNewDisbursement) RemittanceEntryType.DISBURSEMENT else RemittanceEntryType.REMITTANCE
            viewModel.addRemittance(amt, remarks.trim(), millis, entryType)
        } else {
            val base = existing ?: return
            viewModel.updateRemittance(
                base.copy(
                    amount = amt,
                    remarks = remarks.trim(),
                    date = millis,
                    date_updated = System.currentTimeMillis(),
                )
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isNewDisbursement -> "Add disbursement"
                            isNew -> "Add remittance"
                            existing != null && RemittanceEntryType.isDisbursement(existing.entry_type) ->
                                "Edit disbursement"
                            else -> "Edit remittance"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { performSave() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSave
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.padding(top = 8.dp))
            NumericPadOutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    isAmountError = false
                },
                label = { Text("Amount") },
                padTitle = "Amount",
                isError = isAmountError,
                supportingText = if (isAmountError) {
                    { Text("Enter an amount greater than zero") }
                } else null,
                maxDecimalPlaces = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Date", style = MaterialTheme.typography.labelMedium)
                    Text(
                        dateFormatter.format(Date(selectedDateMillis)),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Remarks") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Default),
            )
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}
