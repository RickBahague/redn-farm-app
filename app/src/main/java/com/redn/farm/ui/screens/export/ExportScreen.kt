package com.redn.farm.ui.screens.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val exportState by viewModel.exportState.collectAsState()
    
    // State for confirmation dialogs
    var showCustomersConfirmation by remember { mutableStateOf(false) }
    var showProductsConfirmation by remember { mutableStateOf(false) }
    var showAcquisitionsConfirmation by remember { mutableStateOf(false) }
    
    // State for export confirmation dialogs
    var showExportDialog by remember { mutableStateOf<String?>(null) }
    
    var clearTableSelection by remember { mutableStateOf(emptySet<ClearableTable>()) }
    var showClearDependencyDialog by remember { mutableStateOf(false) }
    var clearDependencyAdditions by remember { mutableStateOf<Set<ClearableTable>?>(null) }
    var showClearFinalConfirm by remember { mutableStateOf<Set<ClearableTable>?>(null) }
    
    // State for export success dialog: label + one or more files
    var showExportSuccessDialog by remember { mutableStateOf<Pair<String, List<File>>?>(null) }
    val isAdmin by viewModel.isAdmin.collectAsState()
    var bundleSelection by remember { mutableStateOf(ExportBundleTable.entries.toSet()) }
    
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
        files: List<File>?,
        onDismiss: () -> Unit
    ) {
        if (dataType != null && !files.isNullOrEmpty()) {
            val dir = files.first().parentFile?.absolutePath.orEmpty()
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                title = { Text("Export successful") },
                text = {
                    Column {
                        Text(dataType)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${files.size} file(s)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Folder: $dir",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
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

    if (showClearDependencyDialog && clearDependencyAdditions != null) {
        val add = clearDependencyAdditions!!
        AlertDialog(
            onDismissRequest = {
                showClearDependencyDialog = false
                clearDependencyAdditions = null
            },
            title = { Text("Include dependent tables?") },
            text = {
                Text(ClearableTable.dependencyPromptMessage(add))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearFinalConfirm = clearTableSelection + add
                        showClearDependencyDialog = false
                        clearDependencyAdditions = null
                    }
                ) {
                    Text("Add & continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearDependencyDialog = false
                        clearDependencyAdditions = null
                    }
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    showClearFinalConfirm?.let { toClear ->
        AlertDialog(
            onDismissRequest = { showClearFinalConfirm = null },
            icon = {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Clear selected tables?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This permanently deletes data. It cannot be undone.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("The following will be cleared:", style = MaterialTheme.typography.labelLarge)
                    toClear.sortedBy { it.ordinal }.forEach { t ->
                        Text("• ${t.label}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSelectedTables(toClear)
                        showClearFinalConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Clear permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFinalConfirm = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Render export success dialog
    showExportSuccessDialog?.let { (dataType, files) ->
        ExportSuccessDialog(
            dataType = dataType,
            files = files,
            onDismiss = {
                showExportSuccessDialog = null
                viewModel.dismissMessage()
            }
        )
    }

    // Render export confirmation dialog
    ExportConfirmationDialog(
        dataType = showExportDialog,
        onConfirm = {
            when (showExportDialog) {
                "Users" -> viewModel.exportUsers()
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        val paths = when {
                            !state.files.isNullOrEmpty() -> state.files
                            else -> state.file?.let { listOf(it) }
                        }
                        if (paths != null) {
                            showExportSuccessDialog = Pair(state.message ?: "Export", paths)
                        }
                        showExportDialog = null
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

                if (isAdmin) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Selective export",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Pick tables and export one CSV per table with the same timestamp (EXP-US-01 batch).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = { bundleSelection = ExportBundleTable.entries.toSet() }) {
                                    Text("Select all")
                                }
                                TextButton(onClick = { bundleSelection = emptySet() }) {
                                    Text("Select none")
                                }
                            }
                            ExportBundleTable.entries.forEach { table ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = table in bundleSelection,
                                        onCheckedChange = { checked ->
                                            bundleSelection =
                                                if (checked) bundleSelection + table else bundleSelection - table
                                        }
                                    )
                                    Text(
                                        text = table.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                            val n = bundleSelection.size
                            Button(
                                onClick = { viewModel.exportSelectedBundle(bundleSelection) },
                                enabled = n > 0,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (n == 0) "Export selected tables"
                                    else "Export $n table${if (n == 1) "" else "s"}"
                                )
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Clear tables (EXP-US-02)",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Select tables to truncate. Dependent tables are suggested automatically. Clears run in a safe order.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(onClick = { clearTableSelection = ClearableTable.entries.toSet() }) {
                                    Text("Select all")
                                }
                                TextButton(onClick = { clearTableSelection = emptySet() }) {
                                    Text("Select none")
                                }
                            }
                            ClearableTable.entries.forEach { table ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = table in clearTableSelection,
                                        onCheckedChange = { checked ->
                                            clearTableSelection =
                                                if (checked) clearTableSelection + table else clearTableSelection - table
                                        },
                                    )
                                    Text(
                                        text = table.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                            }
                            val n = clearTableSelection.size
                            Button(
                                onClick = {
                                    if (clearTableSelection.isEmpty()) return@Button
                                    val add = ClearableTable.suggestedDependencyAdditions(clearTableSelection)
                                    if (add != null) {
                                        clearDependencyAdditions = add
                                        showClearDependencyDialog = true
                                    } else {
                                        showClearFinalConfirm = clearTableSelection
                                    }
                                },
                                enabled = n > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (n == 0) "Clear selected tables…" else "Clear $n table(s)…")
                            }
                        }
                    }
                }

                // Export Section (CSV only)
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
                        ActionButton(
                            text = "Users",
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = { showExportDialog = "Users" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Customers",
                            icon = { Icon(Icons.Default.PeopleAlt, contentDescription = null) },
                            onClick = { showExportDialog = "Customers" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Employees",
                            icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            onClick = { showExportDialog = "Employees" },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Orders
                        Text(
                            text = "Orders",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Orders",
                            icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                            onClick = { showExportDialog = "Orders" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Order Items",
                            icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                            onClick = { showExportDialog = "Order Items" },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Farm Operations
                        Text(
                            text = "Farm Operations",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "FarmOps",
                            icon = { Icon(Icons.Default.Agriculture, contentDescription = null) },
                            onClick = { showExportDialog = "Farm Operations" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Inventory (Acquisitions)",
                            icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                            onClick = { showExportDialog = "Acquisitions" },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Products
                        Text(
                            text = "Products",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Products",
                            icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                            onClick = { showExportDialog = "Products" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Prices",
                            icon = { Icon(Icons.Default.PriceChange, contentDescription = null) },
                            onClick = { showExportDialog = "Product Prices" },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Payments & Transactions
                        Text(
                            text = "Payments & Transactions",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Employee payments",
                            icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                            onClick = { showExportDialog = "Employee Payments" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Remittances",
                            icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                            onClick = { showExportDialog = "Remittances" },
                            modifier = Modifier.fillMaxWidth()
                        )
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