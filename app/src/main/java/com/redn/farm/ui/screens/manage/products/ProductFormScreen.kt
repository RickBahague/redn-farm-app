package com.redn.farm.ui.screens.manage.products

import android.util.Log
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.redn.farm.data.model.Product
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    viewModel: ManageProductsViewModel
) {
    val isNew = productId == "new"
    val products by viewModel.products.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val existing = remember(productId, products) {
        if (isNew) null else products.find { it.product_id == productId }
    }

    if (!isNew && products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isNew && products.isNotEmpty() && existing == null) {
        LaunchedEffect(productId, products) {
            onNavigateBack()
        }
        return
    }

    var name by remember(productId) { mutableStateOf("") }
    var description by remember(productId) { mutableStateOf("") }
    var unitType by remember(productId) { mutableStateOf("kg") }
    var category by remember(productId) { mutableStateOf("") }
    var defaultPieceCountStr by remember(productId) { mutableStateOf("") }
    var isActive by remember(productId) { mutableStateOf(true) }

    LaunchedEffect(existing) {
        val p = existing ?: return@LaunchedEffect
        name = p.product_name
        description = p.product_description
        unitType = p.unit_type.ifBlank { "kg" }
        category = p.category.orEmpty()
        defaultPieceCountStr = p.defaultPieceCount?.toString().orEmpty()
        isActive = p.is_active
    }

    val needsPieceCount = unitType.equals("piece", ignoreCase = true) ||
        unitType.equals("both", ignoreCase = true)
    val parsedDefaultPieces = defaultPieceCountStr.toIntOrNull()
    val canSave = name.isNotEmpty() && unitType.isNotBlank() &&
        (!needsPieceCount || parsedDefaultPieces != null)

    fun performSave() {
        if (!canSave) return
        scope.launch {
            try {
                if (isNew) {
                    val newId = "P${System.currentTimeMillis()}_${(1000..9999).random()}"
                    viewModel.insertProduct(
                        Product(
                            product_id = newId,
                            product_name = name,
                            product_description = description,
                            unit_type = unitType,
                            is_active = true,
                            category = category.trim().ifEmpty { null },
                            defaultPieceCount = if (needsPieceCount) parsedDefaultPieces else null
                        )
                    )
                } else {
                    val p = existing!!
                    val defaultPieceCount = if (needsPieceCount) parsedDefaultPieces else null
                    viewModel.updateProduct(
                        p.copy(
                            product_name = name,
                            product_description = description,
                            unit_type = unitType,
                            is_active = isActive,
                            category = category.trim().ifEmpty { null },
                            defaultPieceCount = defaultPieceCount
                        )
                    )
                }
                onNavigateBack()
            } catch (e: Exception) {
                Log.e("ProductFormScreen", "Save failed", e)
                snackbarHostState.showSnackbar(e.message ?: "Could not save")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add product" else "Edit product") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { performSave() },
                        enabled = canSave
                    ) {
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
                        Text(if (isNew) "Save product" else "Save changes")
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Product name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            Text("Unit type", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = unitType.equals("kg", ignoreCase = true),
                    onClick = { unitType = "kg" },
                    label = { Text("kg") }
                )
                FilterChip(
                    selected = unitType.equals("piece", ignoreCase = true),
                    onClick = { unitType = "piece" },
                    label = { Text("piece") }
                )
                FilterChip(
                    selected = unitType.equals("both", ignoreCase = true),
                    onClick = { unitType = "both" },
                    label = { Text("both") }
                )
            }
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (needsPieceCount) {
                OutlinedTextField(
                    value = defaultPieceCountStr,
                    onValueChange = { s ->
                        if (s.isEmpty() || s.all { it.isDigit() }) {
                            defaultPieceCountStr = s
                        }
                    },
                    label = { Text("Default piece count (pcs/kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!isNew) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
