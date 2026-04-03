package com.redn.farm.ui.screens.manage.customers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.redn.farm.data.model.Customer
import com.redn.farm.data.model.CustomerType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    viewModel: ManageCustomersViewModel
) {
    val isNew = customerId == "new"
    val customers by viewModel.customers.collectAsState()
    val scope = rememberCoroutineScope()

    val existing = remember(customerId, customers) {
        if (isNew) null else customers.find { it.customer_id == customerId.toIntOrNull() }
    }

    if (!isNew && customers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isNew && customers.isNotEmpty() && existing == null) {
        LaunchedEffect(customerId, customers) {
            onNavigateBack()
        }
        return
    }

    var firstname by remember(customerId) { mutableStateOf("") }
    var lastname by remember(customerId) { mutableStateOf("") }
    var contact by remember(customerId) { mutableStateOf("") }
    var customerType by remember(customerId) { mutableStateOf(CustomerType.RETAIL) }
    var address by remember(customerId) { mutableStateOf("") }
    var city by remember(customerId) { mutableStateOf("") }
    var province by remember(customerId) { mutableStateOf("") }
    var postalCode by remember(customerId) { mutableStateOf("") }
    var addressExpanded by remember(customerId) { mutableStateOf(false) }

    LaunchedEffect(existing) {
        val c = existing ?: return@LaunchedEffect
        firstname = c.firstname
        lastname = c.lastname
        contact = c.contact
        customerType = c.customer_type
        address = c.address
        city = c.city
        province = c.province
        postalCode = c.postal_code
    }

    val canSave = firstname.isNotEmpty() && lastname.isNotEmpty()

    fun performSave() {
        if (!canSave) return
        val c = Customer(
            customer_id = existing?.customer_id ?: 0,
            firstname = firstname,
            lastname = lastname,
            contact = contact,
            customer_type = customerType,
            address = address,
            city = city,
            province = province,
            postal_code = postalCode
        )
        scope.launch {
            if (isNew) viewModel.addCustomer(c) else viewModel.updateCustomer(c)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add customer" else "Edit customer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { performSave() }, enabled = canSave) {
                        Text("Save")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { performSave() },
                        enabled = canSave
                    ) {
                        Text(if (isNew) "Save customer" else "Save changes")
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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = firstname,
                onValueChange = { firstname = it },
                label = { Text("First name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = lastname,
                onValueChange = { lastname = it },
                label = { Text("Last name") },
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

            Text("Customer type", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerType.values().forEach { type ->
                    FilterChip(
                        selected = customerType == type,
                        onClick = { customerType = type },
                        label = { Text(type.toString()) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { addressExpanded = !addressExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Address details", style = MaterialTheme.typography.titleSmall)
                Icon(
                    imageVector = if (addressExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = addressExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Street / address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = province,
                            onValueChange = { province = it },
                            label = { Text("Province") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text("Postal code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
