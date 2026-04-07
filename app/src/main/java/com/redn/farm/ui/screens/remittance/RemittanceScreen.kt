package com.redn.farm.ui.screens.remittance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Remittance
import com.redn.farm.data.model.RemittanceEntryType
import com.redn.farm.utils.CurrencyFormatter
import com.redn.farm.utils.PrinterUtils
import com.redn.farm.utils.buildRemittanceSlip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope

private enum class CashEntryFilter {
    ALL, REMITTANCE, DISBURSEMENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemittanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToForm: (String) -> Unit,
    viewModel: RemittanceViewModel = hiltViewModel()
) {
    var pendingDeleteRemittance by remember { mutableStateOf<Remittance?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var cashFilter by remember { mutableStateOf(CashEntryFilter.ALL) }

    val remittances by viewModel.remittances.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val filteredRemittances = remember(remittances, searchQuery, cashFilter) {
        remittances.filter { remittance ->
            val typeOk = when (cashFilter) {
                CashEntryFilter.ALL -> true
                CashEntryFilter.REMITTANCE -> !RemittanceEntryType.isDisbursement(remittance.entry_type)
                CashEntryFilter.DISBURSEMENT -> RemittanceEntryType.isDisbursement(remittance.entry_type)
            }
            typeOk && (
                remittance.remarks.contains(searchQuery, ignoreCase = true) ||
                    CurrencyFormatter.format(remittance.amount).contains(searchQuery, ignoreCase = true) ||
                    dateFormatter.format(Date(remittance.date)).contains(searchQuery, ignoreCase = true) ||
                    RemittanceEntryType.label(remittance.entry_type).contains(searchQuery, ignoreCase = true)
                )
        }
    }

    val totalRemittanceAmount = filteredRemittances
        .filter { !RemittanceEntryType.isDisbursement(it.entry_type) }
        .sumOf { it.amount }
    val totalDisbursementAmount = filteredRemittances
        .filter { RemittanceEntryType.isDisbursement(it.entry_type) }
        .sumOf { it.amount }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    val canAddRemittance = viewModel.canAddRemittance()
    val canAddDisbursement = viewModel.canAddDisbursement()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Remittances & disbursements") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (canAddRemittance) {
                        IconButton(onClick = { onNavigateToForm("new") }) {
                            Icon(Icons.Default.Payments, contentDescription = "Add remittance")
                        }
                    }
                    if (canAddDisbursement) {
                        IconButton(onClick = { onNavigateToForm("new_disbursement") }) {
                            Icon(Icons.Default.AccountBalance, contentDescription = "Add disbursement")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (cashFilter) {
                        CashEntryFilter.ALL -> {
                            Text(
                                text = "Remittances (filtered)",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = CurrencyFormatter.format(totalRemittanceAmount),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Disbursements (filtered)",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = CurrencyFormatter.format(totalDisbursementAmount),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        CashEntryFilter.REMITTANCE -> {
                            Text(
                                text = "Total remittances",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = CurrencyFormatter.format(totalRemittanceAmount),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        CashEntryFilter.DISBURSEMENT -> {
                            Text(
                                text = "Total disbursements",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = CurrencyFormatter.format(totalDisbursementAmount),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = "Showing ${filteredRemittances.size} of ${remittances.size} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = cashFilter == CashEntryFilter.ALL,
                    onClick = { cashFilter = CashEntryFilter.ALL },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = cashFilter == CashEntryFilter.REMITTANCE,
                    onClick = { cashFilter = CashEntryFilter.REMITTANCE },
                    label = { Text("Remittances") }
                )
                FilterChip(
                    selected = cashFilter == CashEntryFilter.DISBURSEMENT,
                    onClick = { cashFilter = CashEntryFilter.DISBURSEMENT },
                    label = { Text("Disbursements") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text("Search") },
                placeholder = { Text("Amount, date, remarks, type") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRemittances, key = { it.remittance_id }) { remittance ->
                    val canEditRow = viewModel.canEdit(remittance)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = CurrencyFormatter.format(remittance.amount),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = RemittanceEntryType.label(remittance.entry_type),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (RemittanceEntryType.isDisbursement(remittance.entry_type)) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val ok = PrinterUtils.printMessage(
                                                    context,
                                                    buildRemittanceSlip(remittance),
                                                    alignment = 0,
                                                )
                                                snackbarHostState.showSnackbar(
                                                    if (ok) "Sent to printer" else "Print failed"
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Print",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (canEditRow) {
                                        IconButton(
                                            onClick = {
                                                onNavigateToForm(remittance.remittance_id.toString())
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { pendingDeleteRemittance = remittance }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Date: ${dateFormatter.format(Date(remittance.date))}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (remittance.date_updated > remittance.date) {
                                Text(
                                    text = "Updated: ${dateFormatter.format(Date(remittance.date_updated))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Created: ${dateFormatter.format(Date(remittance.date))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (remittance.remarks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = remittance.remarks,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteRemittance?.let { remittance ->
        val kind = RemittanceEntryType.label(remittance.entry_type).lowercase(Locale.getDefault())
        AlertDialog(
            onDismissRequest = { pendingDeleteRemittance = null },
            title = { Text("Delete $kind?") },
            text = {
                Text(
                    "Remove this entry of ${CurrencyFormatter.format(remittance.amount)} " +
                        "on ${dateFormatter.format(Date(remittance.date))}? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRemittance(remittance)
                        pendingDeleteRemittance = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRemittance = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
