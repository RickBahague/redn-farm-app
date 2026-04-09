package com.redn.farm.ui.screens.manage.employees

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.model.Employee
import com.redn.farm.ui.components.alphaNumericKeyboardOptions
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageEmployeesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPayments: (Int, String) -> Unit,
    onNavigateToEmployeeForm: (String) -> Unit,
    viewModel: ManageEmployeesViewModel = hiltViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf<Employee?>(null) }
    
    val employees by viewModel.employees.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Employees") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEmployeeForm("new") }) {
                        Icon(Icons.Default.Add, "Add Employee")
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
            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Search") },
                placeholder = { Text("Search by name, ID, or contact") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = alphaNumericKeyboardOptions(imeAction = ImeAction.Search),
            )

            val isFiltering = searchQuery.isNotBlank()
            if (employees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isFiltering) "No matching employees" else "No employees yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isFiltering) "Try adjusting your search." else "Add your first employee to start tracking payments.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onNavigateToEmployeeForm("new") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add employee")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(employees) { employee ->
                        EmployeeCard(
                            employee = employee,
                            dateFormatter = dateFormatter,
                            onEditClick = {
                                onNavigateToEmployeeForm(employee.employee_id.toString())
                            },
                            onDeleteClick = { showDeleteDialog = employee },
                            onPaymentClick = {
                                onNavigateToPayments(employee.employee_id, employee.fullName)
                            }
                        )
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { employee ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Employee") },
            text = { Text("Are you sure you want to delete ${employee.fullName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEmployee(employee)
                        showDeleteDialog = null
                    }
                    ,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
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

@Composable
private fun EmployeeCard(
    employee: Employee,
    dateFormatter: SimpleDateFormat,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPaymentClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = employee.formattedId,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = employee.fullName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = employee.contact,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row {
                    IconButton(onClick = onPaymentClick) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Employee payments",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            if (employee.date_updated > employee.date_created) {
                Text(
                    text = "Updated: ${dateFormatter.format(Date(employee.date_updated))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Created: ${dateFormatter.format(Date(employee.date_created))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 