package com.redn.farm.ui.screens.manage.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.pricing.OrderPricingResolver
import com.redn.farm.data.pricing.SalesChannel
import com.redn.farm.utils.CurrencyFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

data class UnifiedHistoryRow(
    val sortMillis: Long,
    val manual: ProductPrice? = null,
    val acquisition: Acquisition? = null,
)

fun mergeProductPriceHistory(
    manuals: List<ProductPrice>,
    acquisitions: List<Acquisition>,
): List<UnifiedHistoryRow> = buildList {
    manuals.forEach { p ->
        add(UnifiedHistoryRow(sortMillis = p.date_created, manual = p))
    }
    acquisitions.forEach { a ->
        val created = a.created_at.takeIf { it > 0 }
        val sortKey = if (created != null) max(a.date_acquired, created) else a.date_acquired
        add(UnifiedHistoryRow(sortMillis = sortKey, acquisition = a))
    }
}.sortedByDescending { it.sortMillis }

@Composable
fun UnifiedHistoryRowContent(
    row: UnifiedHistoryRow,
    dateFmt: DateTimeFormatter,
    currentActiveAcquisitionId: Int?,
    canOpenPreset: Boolean,
    onPresetClick: (String) -> Unit,
) {
    fun formatMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFmt)

    when {
        row.manual != null -> {
            val p = row.manual
            Text(formatMillis(p.date_created), style = MaterialTheme.typography.labelMedium)
            Text("Manual fallback", style = MaterialTheme.typography.titleSmall)
            p.per_kg_price?.takeIf { it > 0 }?.let {
                Text("Per kg: ${CurrencyFormatter.format(it)}", style = MaterialTheme.typography.bodyMedium)
            }
            p.per_piece_price?.takeIf { it > 0 }?.let {
                Text("Per piece: ${CurrencyFormatter.format(it)}", style = MaterialTheme.typography.bodyMedium)
            }
            p.discounted_per_kg_price?.takeIf { it > 0 }?.let {
                Text("Discounted kg: ${CurrencyFormatter.format(it)}", style = MaterialTheme.typography.bodySmall)
            }
            p.discounted_per_piece_price?.takeIf { it > 0 }?.let {
                Text("Discounted pc: ${CurrencyFormatter.format(it)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        row.acquisition != null -> {
            val a = row.acquisition
            Text(formatMillis(a.date_acquired), style = MaterialTheme.typography.labelMedium)
            val title = if (a.srp_custom_override) "Acquisition (custom SRP)" else "Acquisition (preset SRP)"
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (currentActiveAcquisitionId != null && a.acquisition_id == currentActiveAcquisitionId) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Current SRP") },
                    )
                }
            }
            Text(
                "Lot #${a.acquisition_id} · ${String.format(Locale.getDefault(), "%.3f", a.quantity)} ${if (a.is_per_kg) "kg" else "pc"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val oKg = OrderPricingResolver.srpFromAcquisition(a, SalesChannel.ONLINE, true)
            val rKg = OrderPricingResolver.srpFromAcquisition(a, SalesChannel.RESELLER, true)
            val fKg = OrderPricingResolver.srpFromAcquisition(a, SalesChannel.OFFLINE, true)
            Text(
                "Online kg ${oKg?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                    "Reseller ${rKg?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                    "Store ${fKg?.let { CurrencyFormatter.format(it) } ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            val oPc = OrderPricingResolver.srpFromAcquisition(a, SalesChannel.ONLINE, false)
            val rPc = OrderPricingResolver.srpFromAcquisition(a, SalesChannel.RESELLER, false)
            val fPc = OrderPricingResolver.srpFromAcquisition(a, SalesChannel.OFFLINE, false)
            if (listOf(oPc, rPc, fPc).any { it != null && it > 0 }) {
                Text(
                    "Per piece · Online ${oPc?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                        "Reseller ${rPc?.let { CurrencyFormatter.format(it) } ?: "—"} · " +
                        "Store ${fPc?.let { CurrencyFormatter.format(it) } ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val ref = a.preset_ref?.trim().orEmpty()
            if (ref.isNotEmpty()) {
                if (canOpenPreset) {
                    TextButton(onClick = { onPresetClick(ref) }) {
                        Text("Preset: $ref")
                    }
                } else {
                    Text(
                        "Preset: $ref",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
