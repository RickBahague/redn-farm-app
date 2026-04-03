package com.redn.farm.ui.screens.pricing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.pricing.PricingPresetGson
import com.redn.farm.utils.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetDetailScreen(
    onNavigateBack: () -> Unit,
    onRestoreToEditor: (String) -> Unit,
    onActivatePreview: (String) -> Unit,
    viewModel: PresetDetailViewModel = hiltViewModel()
) {
    val preset by viewModel.preset.collectAsState()
    val dateFmt = androidx.compose.runtime.remember {
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Preset detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val p = preset
        if (p == null) {
            Text("Loading…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val haulingFeesParsed = remember(p.hauling_fees_json) {
            runCatching { PricingPresetGson.haulingFeesFromJson(p.hauling_fees_json) }.getOrNull()
        }
        val channelsParsed = remember(p.channels_json) {
            runCatching { PricingPresetGson.channelsFromJson(p.channels_json) }.getOrNull()
        }
        val categoriesParsed = remember(p.categories_json) {
            runCatching { PricingPresetGson.categoriesFromJson(p.categories_json) }.getOrNull()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(p.preset_name, style = MaterialTheme.typography.headlineSmall)
            Text("ID: ${p.preset_id}", style = MaterialTheme.typography.bodySmall)
            Text("Saved: ${dateFmt.format(Date(p.saved_at))} by ${p.saved_by}")
            if (p.is_active) {
                Text(
                    "ACTIVE",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                p.activated_at?.let {
                    Text("Activated: ${dateFmt.format(Date(it))} by ${p.activated_by ?: "—"}")
                }
            }
            Text(
                "Spoilage: ${p.spoilage_rate} · Additional ₱/kg: ${
                    CurrencyFormatter.format(p.additional_cost_per_kg)
                } · Hauling weight: ${p.hauling_weight_kg} kg",
                style = MaterialTheme.typography.bodyMedium
            )
            if (haulingFeesParsed != null) {
                PresetDetailHaulingFeesSection(fees = haulingFeesParsed)
            } else {
                PresetDetailJsonFallback("Hauling fees", p.hauling_fees_json)
            }
            if (channelsParsed != null) {
                PresetDetailChannelsSection(channels = channelsParsed)
            } else {
                PresetDetailJsonFallback("Channels", p.channels_json)
            }
            if (categoriesParsed != null) {
                PresetDetailCategoriesSection(categories = categoriesParsed)
            } else {
                PresetDetailJsonFallback("Categories", p.categories_json)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onRestoreToEditor(p.preset_id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore as new draft")
                }
                if (!p.is_active) {
                    Button(
                        onClick = { onActivatePreview(p.preset_id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Activate…")
                    }
                }
            }

            if (!p.is_active) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete preset")
                }
            }
        }

        if (showDeleteConfirm && !p.is_active) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete preset?") },
                text = {
                    Text(
                        "Permanently remove “${p.preset_name}”? This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            viewModel.deleteInactivePreset(
                                onSuccess = { onNavigateBack() },
                                onError = { msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
