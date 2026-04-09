package com.redn.farm.ui.screens.pricing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetActivationPreviewScreen(
    onNavigateBack: () -> Unit,
    onActivationComplete: () -> Unit,
    viewModel: PresetActivationPreviewViewModel = hiltViewModel()
) {
    val preset by viewModel.preset.collectAsState()
    val rows by viewModel.previewRows.collectAsState()
    val activated by viewModel.activated.collectAsState()
    val err by viewModel.error.collectAsState()

    LaunchedEffect(activated) {
        if (activated) onActivationComplete()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Activate preset") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Review illustrative SRPs before activation. Example: bulk cost ₱5,000, quantity 100 kg, using this preset’s spoilage and additional ₱/kg.",
                style = MaterialTheme.typography.bodyMedium
            )
            preset?.let { p ->
                Text("Preset: ${p.preset_name}", style = MaterialTheme.typography.titleMedium)
            }
            err?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Text("Per-channel SRP (₱/kg)", style = MaterialTheme.typography.titleSmall)
            rows.forEach { row ->
                Text(
                    "${row.channelKey}: ${row.srpPerKg.toInt()} ₱/kg (illustrative)",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                "Confirming will deactivate any current active preset and set this one active.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { viewModel.confirmActivate() },
                modifier = Modifier.fillMaxWidth(),
                enabled = preset != null && !activated
            ) {
                Text("Confirm activation")
            }
            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}
