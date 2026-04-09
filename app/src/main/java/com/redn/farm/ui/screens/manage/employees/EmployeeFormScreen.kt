package com.redn.farm.ui.screens.manage.employees

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.redn.farm.data.model.Employee
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormScreen(
    employeeIdKey: String,
    onNavigateBack: () -> Unit,
    viewModel: ManageEmployeesViewModel
) {
    val isNew = employeeIdKey == "new"
    val employees by viewModel.employees.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val focusManager = LocalFocusManager.current

    val existing = remember(employeeIdKey, employees) {
        if (isNew) null else employees.find { it.employee_id == employeeIdKey.toIntOrNull() }
    }

    if (!isNew && employees.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isNew && employees.isNotEmpty() && existing == null) {
        LaunchedEffect(employeeIdKey, employees) {
            onNavigateBack()
        }
        return
    }

    var firstname by remember(employeeIdKey) { mutableStateOf("") }
    var lastname by remember(employeeIdKey) { mutableStateOf("") }
    var contact by remember(employeeIdKey) { mutableStateOf("") }

    LaunchedEffect(existing) {
        val e = existing ?: return@LaunchedEffect
        firstname = e.firstname
        lastname = e.lastname
        contact = e.contact
    }

    val canSave = firstname.isNotBlank() && lastname.isNotBlank() && !isSaving

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
        if (!canSave) return
        if (isNew) {
            viewModel.addEmployee(
                firstname = firstname.trim(),
                lastname = lastname.trim(),
                contact = contact.trim()
            )
        } else {
            val base = existing ?: return
            viewModel.updateEmployee(
                Employee(
                    employee_id = base.employee_id,
                    firstname = firstname.trim(),
                    lastname = lastname.trim(),
                    contact = contact.trim(),
                    date_created = base.date_created,
                    date_updated = base.date_updated
                )
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add employee" else "Edit employee") },
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
            OutlinedTextField(
                value = firstname,
                onValueChange = { firstname = it },
                label = { Text("First name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            OutlinedTextField(
                value = lastname,
                onValueChange = { lastname = it },
                label = { Text("Last name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Contact") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (firstname.isNotBlank() && lastname.isNotBlank()) {
                            performSave()
                        }
                    }
                )
            )
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}
