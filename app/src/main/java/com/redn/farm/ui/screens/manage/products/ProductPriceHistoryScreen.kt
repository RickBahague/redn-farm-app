package com.redn.farm.ui.screens.manage.products

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPriceHistoryScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onOpenPresetDetail: (String) -> Unit = {},
    viewModel: ManageProductsViewModel,
) {
    val products by viewModel.products.collectAsState()
    val activeAcquisitionByProductId by viewModel.activeAcquisitionByProductId.collectAsState()
    val canViewPresetDetail by viewModel.canViewPresetDetail.collectAsState()
    val priceHistory by remember(productId) {
        viewModel.observePriceHistory(productId)
    }.collectAsState(initial = emptyList())
    val acquisitionHistory by remember(productId) {
        viewModel.observeAcquisitionHistory(productId)
    }.collectAsState(initial = emptyList())
    val currentActiveAcquisitionId =
        activeAcquisitionByProductId[productId]?.acquisition_id?.takeIf { it > 0 }
    val mergedHistory = remember(priceHistory, acquisitionHistory) {
        mergeProductPriceHistory(priceHistory, acquisitionHistory)
    }
    val titleName = products.find { it.product_id == productId }?.product_name ?: productId
    val historyDateFmt = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Price & SRP history") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Text(
                    text = titleName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    text = "Manual fallback entries and acquisitions (newest first).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            if (mergedHistory.isEmpty()) {
                item {
                    Text(
                        "No manual prices or acquisitions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                itemsIndexed(mergedHistory) { index, row ->
                    UnifiedHistoryRowContent(
                        row = row,
                        dateFmt = historyDateFmt,
                        currentActiveAcquisitionId = currentActiveAcquisitionId,
                        canOpenPreset = canViewPresetDetail,
                        onPresetClick = onOpenPresetDetail,
                    )
                    if (index < mergedHistory.lastIndex) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}
