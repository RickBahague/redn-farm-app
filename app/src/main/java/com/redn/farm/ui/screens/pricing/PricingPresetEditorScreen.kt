package com.redn.farm.ui.screens.pricing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.ui.components.NumericPadOutlinedTextField
import com.redn.farm.ui.components.NumericPadOutlinedTextFieldForDouble
import com.redn.farm.ui.components.NumericPadOutlinedTextFieldForNullableDouble
import com.redn.farm.data.pricing.ChannelConfig
import com.redn.farm.data.pricing.ChannelFee

private val roundingOptions = listOf(
    "ceil_whole_peso" to "Ceil whole ₱",
    "nearest_whole_peso" to "Nearest whole ₱",
    "nearest_0.25" to "Nearest ₱0.25"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingPresetEditorScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccessNavigateToPresetHistory: () -> Unit,
    viewModel: PricingPresetEditorViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            viewModel.clearSaveMessage()
            onSaveSuccessNavigateToPresetHistory()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Preset editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }) {
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
                        onClick = { viewModel.save() }
                    ) {
                        Text("Save preset")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("General", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = form.presetName,
                onValueChange = { viewModel.updateForm { f -> f.copy(presetName = it) } },
                label = { Text("Preset name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Spoilage & hauling", style = MaterialTheme.typography.titleMedium)
            NumericPadOutlinedTextField(
                value = form.spoilageRateText,
                onValueChange = { viewModel.updateForm { f -> f.copy(spoilageRateText = it) } },
                label = { Text("Default spoilage rate (0–0.99)") },
                padTitle = "Default spoilage rate",
                modifier = Modifier.fillMaxWidth(),
                maxDecimalPlaces = 4,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Direct ₱/kg override")
                Switch(
                    checked = form.useDirectAdditionalPerKg,
                    onCheckedChange = { viewModel.updateForm { f -> f.copy(useDirectAdditionalPerKg = it) } }
                )
            }
            if (form.useDirectAdditionalPerKg) {
                NumericPadOutlinedTextField(
                    value = form.directAdditionalPerKgText,
                    onValueChange = { viewModel.updateForm { f -> f.copy(directAdditionalPerKgText = it) } },
                    label = { Text("Additional cost per kg (₱)") },
                    padTitle = "Additional cost per kg",
                    modifier = Modifier.fillMaxWidth(),
                    maxDecimalPlaces = 2,
                )
            } else {
                NumericPadOutlinedTextField(
                    value = form.haulingWeightKgText,
                    onValueChange = { viewModel.updateForm { f -> f.copy(haulingWeightKgText = it) } },
                    label = { Text("Hauling weight (kg)") },
                    padTitle = "Hauling weight (kg)",
                    modifier = Modifier.fillMaxWidth(),
                    maxDecimalPlaces = 3,
                )
                Text("Hauling fee lines", style = MaterialTheme.typography.labelLarge)
                form.haulingFees.forEachIndexed { index, fee ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = fee.label,
                            onValueChange = { viewModel.updateHaulingFee(index, it, fee.amount) },
                            label = { Text("Label") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        NumericPadOutlinedTextFieldForDouble(
                            value = fee.amount,
                            emptyWhenZero = true,
                            onValueChange = { viewModel.updateHaulingFee(index, fee.label, it) },
                            label = { Text("₱") },
                            padTitle = "Hauling fee (₱)",
                            modifier = Modifier.weight(0.8f),
                            rememberKey = index,
                            maxDecimalPlaces = 2,
                        )
                        IconButton(onClick = { viewModel.removeHaulingFee(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
                OutlinedButton(onClick = { viewModel.addHaulingFee() }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add fee line", modifier = Modifier.padding(start = 8.dp))
                }
            }

            Text("Channels", style = MaterialTheme.typography.titleMedium)
            ChannelEditorBlock(title = "Online", config = form.channels.online, onChange = viewModel::updateChannelOnline)
            ChannelEditorBlock(title = "Reseller", config = form.channels.reseller, onChange = viewModel::updateChannelReseller)
            ChannelEditorBlock(title = "Offline", config = form.channels.offline, onChange = viewModel::updateChannelOffline)

            Text("Categories", style = MaterialTheme.typography.titleMedium)
            form.categories.forEachIndexed { index, cat ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = cat.name,
                            onValueChange = { nameVal ->
                                viewModel.updateCategory(index) { c -> c.copy(name = nameVal) }
                            },
                            label = { Text("Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { viewModel.removeCategory(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove category")
                        }
                    }
                    NumericPadOutlinedTextFieldForNullableDouble(
                        value = cat.spoilageRate,
                        onValueChange = { v ->
                            viewModel.updateCategory(index) { c -> c.copy(spoilageRate = v) }
                        },
                        label = { Text("Spoilage override (optional)") },
                        padTitle = "Category spoilage",
                        modifier = Modifier.fillMaxWidth(),
                        rememberKey = index to "spoilage",
                        maxDecimalPlaces = 4,
                    )
                    NumericPadOutlinedTextFieldForNullableDouble(
                        value = cat.additionalCostPerKg,
                        onValueChange = { v ->
                            viewModel.updateCategory(index) { c -> c.copy(additionalCostPerKg = v) }
                        },
                        label = { Text("Additional ₱/kg override (optional)") },
                        padTitle = "Additional ₱/kg",
                        modifier = Modifier.fillMaxWidth(),
                        rememberKey = index to "addkg",
                        maxDecimalPlaces = 2,
                    )
                }
            }
            OutlinedButton(onClick = { viewModel.addCategory() }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add category", modifier = Modifier.padding(start = 8.dp))
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelEditorBlock(
    title: String,
    config: ChannelConfig,
    onChange: (ChannelConfig) -> Unit
) {
    var roundingExpanded by remember { mutableStateOf(false) }
    val useMarkup = config.markupPercent != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = useMarkup,
                onClick = {
                    onChange(
                        config.copy(
                            markupPercent = config.markupPercent ?: 35.0,
                            marginPercent = null
                        )
                    )
                },
                label = { Text("Markup %") }
            )
            FilterChip(
                selected = !useMarkup,
                onClick = {
                    onChange(
                        config.copy(
                            markupPercent = null,
                            marginPercent = config.marginPercent ?: 20.0
                        )
                    )
                },
                label = { Text("Margin %") }
            )
        }
        if (useMarkup) {
            NumericPadOutlinedTextFieldForDouble(
                value = config.markupPercent ?: 0.0,
                emptyWhenZero = false,
                onValueChange = { onChange(config.copy(markupPercent = it, marginPercent = null)) },
                label = { Text("Markup %") },
                padTitle = "$title — markup %",
                modifier = Modifier.fillMaxWidth(),
                rememberKey = "$title-markup",
                maxDecimalPlaces = 2,
            )
        } else {
            NumericPadOutlinedTextFieldForDouble(
                value = config.marginPercent ?: 0.0,
                emptyWhenZero = false,
                onValueChange = { onChange(config.copy(marginPercent = it, markupPercent = null)) },
                label = { Text("Margin %") },
                padTitle = "$title — margin %",
                modifier = Modifier.fillMaxWidth(),
                rememberKey = "$title-margin",
                maxDecimalPlaces = 2,
            )
        }
        ExposedDropdownMenuBox(
            expanded = roundingExpanded,
            onExpandedChange = { roundingExpanded = it }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                value = roundingOptions.find { it.first == config.roundingRule }?.second ?: config.roundingRule,
                onValueChange = {},
                label = { Text("Rounding") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roundingExpanded) }
            )
            ExposedDropdownMenu(
                expanded = roundingExpanded,
                onDismissRequest = { roundingExpanded = false }
            ) {
                roundingOptions.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onChange(config.copy(roundingRule = key))
                            roundingExpanded = false
                        }
                    )
                }
            }
        }

        if (config.fees.isNotEmpty()) {
            Text("Channel fees", style = MaterialTheme.typography.labelMedium)
        }
        config.fees.forEachIndexed { i, fee ->
            var feeTypeExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = fee.label,
                    onValueChange = { lbl ->
                        val updated = config.fees.toMutableList()
                            .also { it[i] = fee.copy(label = lbl) }
                        onChange(config.copy(fees = updated))
                    },
                    label = { Text("Label") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = feeTypeExpanded,
                    onExpandedChange = { feeTypeExpanded = it },
                    modifier = Modifier.weight(0.7f)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = if (fee.type == "pct") "%" else "₱",
                        onValueChange = {},
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = feeTypeExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = feeTypeExpanded,
                        onDismissRequest = { feeTypeExpanded = false }
                    ) {
                        listOf("fixed" to "₱ Fixed", "pct" to "% Pct").forEach { (key, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = {
                                    val updated = config.fees.toMutableList()
                                        .also { it[i] = fee.copy(type = key) }
                                    onChange(config.copy(fees = updated))
                                    feeTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                NumericPadOutlinedTextFieldForDouble(
                    value = fee.amount,
                    emptyWhenZero = true,
                    onValueChange = { amt ->
                        val updated = config.fees.toMutableList()
                            .also { it[i] = fee.copy(amount = amt) }
                        onChange(config.copy(fees = updated))
                    },
                    label = { Text("Amount") },
                    padTitle = "$title — fee amount",
                    modifier = Modifier.weight(0.8f),
                    rememberKey = "$title-fee-$i",
                    maxDecimalPlaces = 4,
                )
                IconButton(onClick = {
                    onChange(config.copy(fees = config.fees.filterIndexed { idx, _ -> idx != i }))
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove fee")
                }
            }
        }
        OutlinedButton(
            onClick = {
                onChange(config.copy(fees = config.fees + ChannelFee("fee", "fixed", 0.0)))
            }
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add channel fee", modifier = Modifier.padding(start = 4.dp))
        }
    }
}
