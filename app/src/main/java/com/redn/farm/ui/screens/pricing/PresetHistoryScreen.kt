package com.redn.farm.ui.screens.pricing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.local.entity.PricingPresetEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetHistoryScreen(
    onNavigateBack: () -> Unit,
    onPresetClick: (String) -> Unit,
    viewModel: PresetHistoryViewModel = hiltViewModel()
) {
    val presets by viewModel.presets.collectAsState()
    val dateFmt = androidx.compose.runtime.remember {
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<PricingPresetEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Preset history") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets, key = { it.preset_id }) { preset ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPresetClick(preset.preset_id) }
                        ) {
                            Text(preset.preset_name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                dateFmt.format(Date(preset.saved_at)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (preset.is_active) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = { pendingDelete = preset }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete preset",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        pendingDelete?.let { preset ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete preset?") },
                text = {
                    Text(
                        "Permanently remove “${preset.preset_name}”? This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteInactivePreset(
                                presetId = preset.preset_id,
                                onSuccess = {
                                    pendingDelete = null
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Preset deleted")
                                    }
                                },
                                onError = { msg ->
                                    pendingDelete = null
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
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
