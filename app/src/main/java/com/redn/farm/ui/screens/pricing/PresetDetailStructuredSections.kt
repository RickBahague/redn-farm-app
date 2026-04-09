package com.redn.farm.ui.screens.pricing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.redn.farm.data.pricing.CategoryOverride
import com.redn.farm.data.pricing.ChannelConfig
import com.redn.farm.data.pricing.ChannelFee
import com.redn.farm.data.pricing.ChannelsConfiguration
import com.redn.farm.data.pricing.HaulingFeeItem
import com.redn.farm.utils.CurrencyFormatter

private val roundingLabels = mapOf(
    "ceil_whole_peso" to "Ceil whole ₱",
    "nearest_whole_peso" to "Nearest whole ₱",
    "nearest_0.25" to "Nearest ₱0.25",
)

@Composable
fun PresetDetailHaulingFeesSection(
    fees: List<HaulingFeeItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Hauling fees", style = MaterialTheme.typography.titleSmall)
        if (fees.isEmpty()) {
            Text("— None", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            fees.forEach { fee ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(fee.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        CurrencyFormatter.format(fee.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun PresetDetailChannelsSection(
    channels: ChannelsConfiguration,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Channels", style = MaterialTheme.typography.titleSmall)
        PresetChannelReadOnlyBlock("Online", channels.online)
        HorizontalDivider()
        PresetChannelReadOnlyBlock("Reseller", channels.reseller)
        HorizontalDivider()
        PresetChannelReadOnlyBlock("Offline", channels.offline)
    }
}

@Composable
private fun PresetChannelReadOnlyBlock(
    title: String,
    config: ChannelConfig,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        val pricingLine = when {
            config.markupPercent != null -> "Markup ${config.markupPercent}%"
            config.marginPercent != null -> "Margin ${config.marginPercent}%"
            else -> "—"
        }
        Text(pricingLine, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Rounding: ${roundingLabels[config.roundingRule] ?: config.roundingRule}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (config.fees.isNotEmpty()) {
            Text("Channel fees", style = MaterialTheme.typography.labelMedium)
            config.fees.forEach { fee ->
                PresetChannelFeeRow(fee)
            }
        }
    }
}

@Composable
private fun PresetChannelFeeRow(fee: ChannelFee) {
    val typeLabel = if (fee.type == "pct") "%" else "₱ fixed"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "${fee.label} ($typeLabel)",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            if (fee.type == "pct") "${fee.amount}%" else CurrencyFormatter.format(fee.amount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun PresetDetailCategoriesSection(
    categories: List<CategoryOverride>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Categories", style = MaterialTheme.typography.titleSmall)
        if (categories.isEmpty()) {
            Text("— None", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            categories.forEach { cat ->
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(cat.name, style = MaterialTheme.typography.titleSmall)
                        cat.spoilageRate?.let {
                            Text(
                                "Spoilage override: $it",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        cat.additionalCostPerKg?.let {
                            Text(
                                "Additional ₱/kg: ${CurrencyFormatter.format(it)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (cat.spoilageRate == null && cat.additionalCostPerKg == null) {
                            Text(
                                "No overrides",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetDetailJsonFallback(
    label: String,
    raw: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            "Could not parse structured data. Raw value:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = raw.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
