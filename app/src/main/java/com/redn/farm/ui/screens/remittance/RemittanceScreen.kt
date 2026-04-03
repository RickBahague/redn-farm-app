package com.redn.farm.ui.screens.remittance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.redn.farm.utils.PrinterUtils
import com.redn.farm.utils.buildRemittanceSlip
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Remittance
import com.redn.farm.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemittanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToForm: (String) -> Unit,
    viewModel: RemittanceViewModel = hiltViewModel()
) {
    var pendingDeleteRemittance by remember { mutableStateOf<Remittance?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val remittances by viewModel.remittances.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val filteredRemittances = remittances.filter { remittance ->
        remittance.remarks.contains(searchQuery, ignoreCase = true) ||
            CurrencyFormatter.format(remittance.amount).contains(searchQuery, ignoreCase = true) ||
            dateFormatter.format(Date(remittance.date)).contains(searchQuery, ignoreCase = true)
    }

    val totalRemittances = filteredRemittances.sumOf { it.amount }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Remittances") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToForm("new") }) {
                        Icon(Icons.Default.Add, "Add Remittance")
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
                    Text(
                        text = "Total Remittances",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = CurrencyFormatter.format(totalRemittances),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Showing ${filteredRemittances.size} of ${remittances.size} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text("Search") },
                placeholder = { Text("Search by amount, date, or remarks") },
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
                items(filteredRemittances) { remittance ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(remittance.amount),
                                    style = MaterialTheme.typography.titleMedium
                                )
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
        AlertDialog(
            onDismissRequest = { pendingDeleteRemittance = null },
            title = { Text("Delete remittance?") },
            text = {
                Text(
                    "Remove this remittance of ${CurrencyFormatter.format(remittance.amount)} " +
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
