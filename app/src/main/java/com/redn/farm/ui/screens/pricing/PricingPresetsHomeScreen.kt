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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingPresetsHomeScreen(
    onNavigateBack: () -> Unit,
    onNewPreset: () -> Unit,
    onPresetHistory: () -> Unit,
    viewModel: PricingPresetsHomeViewModel = hiltViewModel()
) {
    val active by viewModel.activePreset.collectAsState()
    val dateFmt = rememberDateFmt()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pricing Presets") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Active preset", style = MaterialTheme.typography.titleMedium)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (active == null) {
                        Text(
                            "No active preset. Create one and activate it from history or detail.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(active!!.preset_name, style = MaterialTheme.typography.titleSmall)
                        Text("ID: ${active!!.preset_id}", style = MaterialTheme.typography.bodySmall)
                        active!!.activated_at?.let { at ->
                            Text(
                                "Activated: ${dateFmt.format(Date(at))} by ${active!!.activated_by ?: "—"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "Spoilage: ${(active!!.spoilage_rate * 100).format1()}% · A (₱/kg): ${
                                CurrencyFormatter.format(active!!.additional_cost_per_kg)
                            }",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNewPreset,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("New Preset", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = onPresetHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Text("History", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun rememberDateFmt() =
    androidx.compose.runtime.remember {
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    }

private fun Double.format1(): String = String.format(Locale.getDefault(), "%.1f", this)
