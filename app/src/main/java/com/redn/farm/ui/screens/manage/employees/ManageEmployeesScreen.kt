package com.redn.farm.ui.screens.manage.employees

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redn.farm.data.model.Employee
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageEmployeesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPayments: (Int, String) -> Unit,
    viewModel: ManageEmployeesViewModel = viewModel(
        factory = ManageEmployeesViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Employee?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Employee?>(null) }
    
    val employees by viewModel.employees.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Employees") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
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
                singleLine = true
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(employees) { employee ->
                    EmployeeCard(
                        employee = employee,
                        dateFormatter = dateFormatter,
                        onEditClick = { showEditDialog = employee },
                        onDeleteClick = { showDeleteDialog = employee },
                        onPaymentClick = { 
                            onNavigateToPayments(employee.employee_id, employee.fullName)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        EmployeeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { firstname, lastname, contact ->
                viewModel.addEmployee(firstname, lastname, contact)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { employee ->
        EmployeeDialog(
            employee = employee,
            onDismiss = { showEditDialog = null },
            onConfirm = { firstname, lastname, contact ->
                viewModel.updateEmployee(
                    employee.copy(
                        firstname = firstname,
                        lastname = lastname,
                        contact = contact
                    )
                )
                showEditDialog = null
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeDialog(
    employee: Employee? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var firstname by remember { mutableStateOf(employee?.firstname ?: "") }
    var lastname by remember { mutableStateOf(employee?.lastname ?: "") }
    var contact by remember { mutableStateOf(employee?.contact ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employee == null) "Add Employee" else "Edit Employee") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = firstname,
                    onValueChange = { firstname = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastname,
                    onValueChange = { lastname = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (firstname.isNotBlank() && lastname.isNotBlank()) {
                        onConfirm(firstname, lastname, contact)
                    }
                },
                enabled = firstname.isNotBlank() && lastname.isNotBlank()
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
} 