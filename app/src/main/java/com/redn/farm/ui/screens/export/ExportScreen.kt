package com.redn.farm.ui.screens.export

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = viewModel(factory = ExportViewModel.Factory),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()
    
    // State for confirmation dialogs
    var showCustomersConfirmation by remember { mutableStateOf(false) }
    var showProductsConfirmation by remember { mutableStateOf(false) }
    var showAcquisitionsConfirmation by remember { mutableStateOf(false) }
    
    // State for export confirmation dialogs
    var showExportDialog by remember { mutableStateOf<String?>(null) }
    
    // State for truncate confirmation dialogs
    var showTruncateDialog by remember { mutableStateOf<String?>(null) }
    
    // State for export success dialog
    var showExportSuccessDialog by remember { mutableStateOf<Pair<String, File?>?>(null) }
    
    // Confirmation Dialog for Sample Data Generation
    @Composable
    fun ConfirmationDialog(
        show: Boolean,
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        if (show) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        }
                    ) {
                        Text("Generate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Truncate Confirmation Dialog
    @Composable
    fun TruncateConfirmationDialog(
        tableName: String?,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        if (tableName != null) {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Close, contentDescription = null) },
                title = { Text("Clear ${tableName}") },
                text = { 
                    Column {
                        Text("WARNING: This action cannot be undone!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Are you sure you want to clear all ${tableName.lowercase()} data?")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Export Confirmation Dialog
    @Composable
    fun ExportConfirmationDialog(
        dataType: String?,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        if (dataType != null) {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                title = { Text("Export ${dataType}") },
                text = { Text("Are you sure you want to export all ${dataType.lowercase()} data?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        }
                    ) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Export Success Dialog
    @Composable
    fun ExportSuccessDialog(
        dataType: String?,
        file: File?,
        onDismiss: () -> Unit
    ) {
        if (dataType != null) {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                title = { Text("Export Successful") },
                text = { 
                    Column {
                        Text("${dataType} data has been successfully exported.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "File saved to: ${file?.absolutePath ?: "exports directory"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }

    // Render confirmation dialogs
    ConfirmationDialog(
        show = showCustomersConfirmation,
        title = "Generate Sample Customers",
        message = "This will generate sample customer data. Are you sure you want to proceed?",
        onConfirm = { viewModel.generateSampleData() },
        onDismiss = { showCustomersConfirmation = false }
    )
    
    ConfirmationDialog(
        show = showProductsConfirmation,
        title = "Generate Sample Products",
        message = "This will generate sample product data. Are you sure you want to proceed?",
        onConfirm = { viewModel.generateSampleProducts() },
        onDismiss = { showProductsConfirmation = false }
    )
    
    ConfirmationDialog(
        show = showAcquisitionsConfirmation,
        title = "Generate Sample Acquisitions",
        message = "This will generate sample acquisition data. Are you sure you want to proceed?",
        onConfirm = { viewModel.generateSampleAcquisitions() },
        onDismiss = { showAcquisitionsConfirmation = false }
    )

    // Render truncate confirmation dialog
    TruncateConfirmationDialog(
        tableName = showTruncateDialog,
        onConfirm = {
            when (showTruncateDialog) {
                "Customers" -> viewModel.truncateCustomers()
                "Employees" -> viewModel.truncateEmployees()
                "Orders" -> viewModel.truncateOrders()
                "Order Items" -> viewModel.truncateOrderItems()
                "Farm Operations" -> viewModel.truncateFarmOperations()
                "Products" -> viewModel.truncateProducts()
                "Product Prices" -> viewModel.truncateProductPrices()
                "Employee Payments" -> viewModel.truncateEmployeePayments()
                "Remittances" -> viewModel.truncateRemittances()
                "Acquisitions" -> viewModel.truncateAcquisitions()
            }
        },
        onDismiss = { showTruncateDialog = null }
    )

    // Render export success dialog
    showExportSuccessDialog?.let { (dataType, file) ->
        ExportSuccessDialog(
            dataType = dataType,
            file = file,
            onDismiss = { showExportSuccessDialog = null }
        )
    }

    // Render export confirmation dialog
    ExportConfirmationDialog(
        dataType = showExportDialog,
        onConfirm = {
            when (showExportDialog) {
                "Customers" -> viewModel.exportCustomers()
                "Employees" -> viewModel.exportEmployees()
                "Orders" -> viewModel.exportOrders()
                "Order Items" -> viewModel.exportOrderItems()
                "Farm Operations" -> viewModel.exportFarmOperations()
                "Products" -> viewModel.exportProducts()
                "Product Prices" -> viewModel.exportProductPrices()
                "Employee Payments" -> viewModel.exportEmployeePayments()
                "Remittances" -> viewModel.exportRemittances()
                "Acquisitions" -> viewModel.exportAcquisitions()
            }
        },
        onDismiss = { showExportDialog = null }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Export Data") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Handle export success state
            LaunchedEffect(exportState) {
                when (exportState) {
                    is ExportViewModel.ExportState.Success -> {
                        val state = exportState as ExportViewModel.ExportState.Success
                        if (state.file != null) {
                            showExportSuccessDialog = Pair(showExportDialog ?: "Data", state.file)
                            showExportDialog = null
                        }
                    }
                    else -> Unit
                }
            }

            // Message Section
            when (val state = exportState) {
                is ExportViewModel.ExportState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is ExportViewModel.ExportState.Success -> {
                    state.message?.let { message ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                IconButton(
                                    onClick = { viewModel.dismissMessage() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                is ExportViewModel.ExportState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.message,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            IconButton(
                                onClick = { viewModel.dismissMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sample Data Generation Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sample Data Generation",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Generate sample data for testing purposes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ActionButton(
                            text = "Sample Customers",
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = { showCustomersConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Sample Products",
                            icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                            onClick = { showProductsConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Sample Acquisitions",
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                            onClick = { showAcquisitionsConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Export Section with Clear Data buttons
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Export Data",
                            style = MaterialTheme.typography.titleMedium
                        )
                        // People
                        Text(
                            text = "People",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Customers",
                                icon = { Icon(Icons.Default.PeopleAlt, contentDescription = null) },
                                onClick = { showExportDialog = "Customers" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Customers" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Employees",
                                icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                onClick = { showExportDialog = "Employees" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Employees" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        
                        // Orders
                        Text(
                            text = "Orders",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Orders",
                                icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                                onClick = { showExportDialog = "Orders" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Orders" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Order Items",
                                icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                                onClick = { showExportDialog = "Order Items" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Order Items" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        
                        // Farm Operations
                        Text(
                            text = "Farm Operations",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "FarmOps",
                                icon = { Icon(Icons.Default.Agriculture, contentDescription = null) },
                                onClick = { showExportDialog = "Farm Operations" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Farm Operations" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Inventory",
                                icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                                onClick = { showExportDialog = "Acquisitions" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Acquisitions" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }

                        // Products
                        Text(
                            text = "Products",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Products",
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                onClick = { showExportDialog = "Products" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Products" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Prices",
                                icon = { Icon(Icons.Default.PriceChange, contentDescription = null) },
                                onClick = { showExportDialog = "Product Prices" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Product Prices" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        
                        // Payments & Transactions
                        Text(
                            text = "Payments & Transactions",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Employee",
                                icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                                onClick = { showExportDialog = "Employee Payments" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Employee Payments" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Remittance",
                                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                                onClick = { showExportDialog = "Remittances" },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Clear",
                                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                                onClick = { showTruncateDialog = "Remittances" },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
} 