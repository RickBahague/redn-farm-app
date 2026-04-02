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
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.utils.CurrencyFormatter
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

    Scaffold(
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
            Text("Hauling fees JSON", style = MaterialTheme.typography.labelLarge)
            Text(p.hauling_fees_json, style = MaterialTheme.typography.bodySmall)
            Text("Channels JSON", style = MaterialTheme.typography.labelLarge)
            Text(p.channels_json, style = MaterialTheme.typography.bodySmall)
            Text("Categories JSON", style = MaterialTheme.typography.labelLarge)
            Text(p.categories_json, style = MaterialTheme.typography.bodySmall)

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
        }
    }
}
