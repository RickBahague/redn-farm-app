package com.redn.farm.ui.screens.database

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.export.CsvExportService
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseMigrationScreen(
    onDatabaseReady: () -> Unit,
    viewModel: DatabaseMigrationViewModel = viewModel(factory = DatabaseMigrationViewModel.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showExistingDatabaseDialog by remember { mutableStateOf(false) }
    var showExportProgressDialog by remember { mutableStateOf(false) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    
    val migrationState by viewModel.migrationState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkExistingDatabase()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Database Setup") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (migrationState) {
                is DatabaseMigrationState.Checking -> {
                    CircularProgressIndicator()
                }
                is DatabaseMigrationState.ExistingDatabaseFound -> {
                    if (!showExistingDatabaseDialog) {
                        showExistingDatabaseDialog = true
                    }
                }
                is DatabaseMigrationState.CreatingNewDatabase -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Creating new database...")
                    }
                }
                is DatabaseMigrationState.Ready -> {
                    LaunchedEffect(Unit) {
                        onDatabaseReady()
                    }
                }
                is DatabaseMigrationState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Error: ${(migrationState as DatabaseMigrationState.Error).message}")
                        Button(onClick = { viewModel.checkExistingDatabase() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Dialog for existing database found
    if (showExistingDatabaseDialog) {
        AlertDialog(
            onDismissRequest = { /* Do nothing, force user to choose */ },
            title = { Text("Existing Database Found") },
            text = { 
                Text(
                    "An existing database has been found. Would you like to export the data before proceeding?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExistingDatabaseDialog = false
                        showExportProgressDialog = true
                        scope.launch {
                            try {
                                val exportService = CsvExportService(context)
                                
                                // Export all tables
                                viewModel.getAllData()?.let { data ->
                                    exportService.exportProducts(data.products)
                                    exportService.exportProductPrices(data.productPrices)
                                    exportService.exportCustomers(data.customers)
                                    exportService.exportOrders(data.orders)
                                    exportService.exportOrderItems(data.orderItems)
                                    exportService.exportEmployees(data.employees)
                                    exportService.exportEmployeePayments(data.employeePayments)
                                    exportService.exportFarmOperations(data.farmOperations)
                                    exportService.exportAcquisitions(data.acquisitions)
                                    exportService.exportRemittances(data.remittances)
                                }
                                
                                showExportProgressDialog = false
                                showExportSuccessDialog = true
                            } catch (e: Exception) {
                                showExportProgressDialog = false
                                exportError = e.message
                            }
                        }
                    }
                ) {
                    Text("Yes, Export Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExistingDatabaseDialog = false
                        viewModel.proceedWithExistingDatabase()
                    }
                ) {
                    Text("No, Keep Existing Data")
                }
            }
        )
    }

    // Export progress dialog
    if (showExportProgressDialog) {
        AlertDialog(
            onDismissRequest = { /* Do nothing while exporting */ },
            title = { Text("Exporting Data") },
            text = { 
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Exporting database tables...")
                }
            },
            confirmButton = { }
        )
    }

    // Export success dialog
    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Do nothing, force user to proceed */ },
            title = { Text("Export Successful") },
            text = { 
                Text("All data has been successfully exported. The existing database will be kept.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportSuccessDialog = false
                        viewModel.proceedWithExistingDatabase()
                    }
                ) {
                    Text("Continue")
                }
            }
        )
    }

    // Export error dialog
    exportError?.let { error ->
        AlertDialog(
            onDismissRequest = { exportError = null },
            title = { Text("Export Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(
                    onClick = {
                        exportError = null
                        viewModel.proceedWithExistingDatabase()
                    }
                ) {
                    Text("Continue Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        exportError = null
                        showExistingDatabaseDialog = true
                    }
                ) {
                    Text("Try Again")
                }
            }
        )
    }
} 