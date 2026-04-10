package com.redn.farm.utils

import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.data.model.Order
import com.redn.farm.data.model.OrderItem
import com.redn.farm.data.model.Remittance
import com.redn.farm.data.model.RemittanceEntryType
import com.redn.farm.data.model.lifetimeOutstandingAdvance
import com.redn.farm.data.model.netPayAmount
import com.redn.farm.data.model.periodTotals
import com.redn.farm.data.pricing.PricingChannelEngine
import com.redn.farm.data.pricing.SalesChannel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val THERMAL_LINE_WIDTH = 32

fun thermalDividerHeavy(width: Int = THERMAL_LINE_WIDTH): String = "=".repeat(width)

fun thermalDividerLight(width: Int = THERMAL_LINE_WIDTH): String = "-".repeat(width)

/** Left label + right value within [width] monospace columns (PU-01). */
fun formatThermalLine(label: String, value: String, width: Int = THERMAL_LINE_WIDTH): String {
    val gap = width - label.length - value.length - 3
    return if (gap > 0) label + " ".repeat(gap) + value
    else (label.take((width - value.length - 1).coerceAtLeast(1)) + " " + value).take(width)
}

fun centerThermalLine(text: String, width: Int = THERMAL_LINE_WIDTH): String {
    val t = text.trim()
    if (t.length >= width) return t.take(width)
    val pad = (width - t.length) / 2 - 2
    return " ".repeat(pad) + t
}

private val isoDate: DateTimeFormatter =
    DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault())
private val detailDateTime: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

fun formatOrderDetailDate(millis: Long): String =
    detailDateTime.format(Instant.ofEpochMilli(millis))

fun formatThermalDate(millis: Long): String =
    isoDate.format(Instant.ofEpochMilli(millis))

/** CUR-01 / PRN-07 — same body as historical OrderDetail receipt. */
fun buildOrderReceiptText(order: Order, items: List<OrderItem>): String = buildString {
    appendLine("REDN GREENS FRESH")
    appendLine("Order #${order.order_id}")
    appendLine("Channel: ${SalesChannel.label(SalesChannel.normalize(order.channel))}")
    appendLine("Date: ${formatOrderDetailDate(order.order_date)}")
    appendLine("Customer: ${order.customerName}")
    appendLine("Contact: ${order.customerContact}")
    appendLine(thermalDividerLight())
    items.forEach { item ->
        appendLine(formatThermalLine(item.product_name, CurrencyFormatter.format(item.total_price)))
        val unitSuffix = if (item.is_per_kg) "/kg" else "/pc"
        appendLine(
            "   ${item.quantity}${if (item.is_per_kg) "kg" else "pc"} x ${CurrencyFormatter.format(item.price_per_unit)}$unitSuffix"
        )
    }
    appendLine(thermalDividerLight())
    appendLine(formatThermalLine("Total:", CurrencyFormatter.format(order.total_amount)))
    appendLine(if (order.is_paid) "PAID" else "UNPAID")
    appendLine(if (order.is_delivered) "DELIVERED" else "NOT DELIVERED")
}

fun buildEmployeePaymentVoucher(
    employeeName: String,
    datePaidMillis: Long,
    receivedMillis: Long?,
    gross: Double,
    cashAdvance: Double,
    liquidated: Double,
    netPay: Double,
): String = buildString {
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine("PAYMENT VOUCHER"))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Employee :", employeeName.take(THERMAL_LINE_WIDTH - 11)))
    appendLine(formatThermalLine("Date Paid:", formatThermalDate(datePaidMillis)))
    appendLine(
        formatThermalLine(
            "Received :",
            receivedMillis?.let { formatThermalDate(it) } ?: "—"
        )
    )
    appendLine(thermalDividerLight())
    appendLine(formatThermalLine("Gross Wage", CurrencyFormatter.format(gross)))
    appendLine(formatThermalLine("Cash Advance", CurrencyFormatter.format(cashAdvance)))
    appendLine(thermalDividerLight())
    appendLine(formatThermalLine("NET PAY", CurrencyFormatter.format(netPay)))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Liquidated", CurrencyFormatter.format(liquidated)))
    appendLine(centerThermalLine("(audit/outstanding only)"))
    appendLine(thermalDividerLight())
    appendLine()
    appendLine("Signature: ___________________")
    appendLine()
    appendLine(thermalDividerHeavy())
}

fun buildAcquisitionReceivingSlip(
    acquisition: Acquisition,
    presetDisplayName: String?,
): String = buildString {
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine("RECEIVING SLIP"))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Acq ID :", acquisition.acquisition_id.toString().padStart(4, '0')))
    appendLine(formatThermalLine("Date   :", formatThermalDate(acquisition.date_acquired)))
    appendLine(formatThermalLine("Product:", acquisition.product_name.take(20)))
    appendLine(formatThermalLine("Location:", acquisition.location.printLabel()))
    appendLine(thermalDividerLight())
    val unit = if (acquisition.is_per_kg) "kg" else "pcs"
    appendLine(formatThermalLine("Quantity :", "${"%.2f".format(acquisition.quantity)} $unit"))
    val priceLabel = if (acquisition.is_per_kg) "Price/kg :" else "Price/pc :"
    appendLine(formatThermalLine(priceLabel, CurrencyFormatter.format(acquisition.price_per_unit)))
    appendLine(formatThermalLine("TOTAL    :", CurrencyFormatter.format(acquisition.total_amount)))
    appendLine(thermalDividerLight())
    if (acquisition.preset_ref != null) {
        appendLine(formatThermalLine("Preset   :", (presetDisplayName ?: acquisition.preset_ref.orEmpty()).take(18)))
        if (acquisition.is_per_kg) {
            appendLine(formatThermalLine("SRP Online :", acquisition.srp_online_per_kg?.let { "${CurrencyFormatter.format(it)}/kg" } ?: "—"))
            appendLine(formatThermalLine("SRP Reseller:", acquisition.srp_reseller_per_kg?.let { "${CurrencyFormatter.format(it)}/kg" } ?: "—"))
            appendLine(formatThermalLine("SRP Offline:", acquisition.srp_offline_per_kg?.let { "${CurrencyFormatter.format(it)}/kg" } ?: "—"))
        } else {
            val pc = acquisition.piece_count?.takeIf { it > 0 }
            fun piecePrice(stored: Double?, perKg: Double?) =
                stored ?: if (pc != null && perKg != null) PricingChannelEngine.perPieceSrp(perKg, pc) else null
            appendLine(formatThermalLine("SRP Online :", piecePrice(acquisition.srp_online_per_piece, acquisition.srp_online_per_kg)?.let { "${CurrencyFormatter.format(it)}/pc" } ?: "—"))
            appendLine(formatThermalLine("SRP Reseller:", piecePrice(acquisition.srp_reseller_per_piece, acquisition.srp_reseller_per_kg)?.let { "${CurrencyFormatter.format(it)}/pc" } ?: "—"))
            appendLine(formatThermalLine("SRP Offline:", piecePrice(acquisition.srp_offline_per_piece, acquisition.srp_offline_per_kg)?.let { "${CurrencyFormatter.format(it)}/pc" } ?: "—"))
        }
    }
    appendLine(thermalDividerHeavy())
    appendLine("Received by: ___________________")
    appendLine(thermalDividerHeavy())
}

private fun AcquisitionLocation.printLabel(): String = when (this) {
    AcquisitionLocation.FARM -> "FARM"
    AcquisitionLocation.MARKET -> "MARKET"
    AcquisitionLocation.SUPPLIER -> "SUPPLIER"
    AcquisitionLocation.OTHER -> "OTHER"
}

/** PRN-08 — max single print payload; split / narrow filters if exceeded. */
const val ACQUISITION_BATCH_REPORT_MAX_CHARS = 16_000

private val batchPrintedAtFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

/**
 * PRN-08 — filtered acquisition batch report (same rows as `AcquireProduceScreen` list).
 * @return `null` if body would exceed [ACQUISITION_BATCH_REPORT_MAX_CHARS].
 */
fun buildAcquisitionBatchReport(
    acquisitions: List<Acquisition>,
    searchQuery: String,
    locationFilter: AcquisitionLocation?,
    dateRange: Pair<Long?, Long?>,
    printedAtMillis: Long = System.currentTimeMillis(),
): String? {
    if (acquisitions.isEmpty()) return null
    val n = acquisitions.size
    val grandTotal = acquisitions.sumOf { it.total_amount }

    val filterSummary = buildString {
        val parts = mutableListOf<String>()
        if (locationFilter != null) parts.add("Loc ${locationFilter.printLabel()}")
        if (searchQuery.isNotBlank()) parts.add("\"${searchQuery.trim().take(24)}\"")
        append(if (parts.isEmpty()) "All" else parts.joinToString(" · "))
    }

    val body = buildString {
        appendLine(thermalDividerHeavy())
        appendLine(centerThermalLine("REDN GREENS FRESH"))
        appendLine(centerThermalLine("ACQUISITION REPORT"))
        appendLine(thermalDividerHeavy())
        appendLine(formatThermalLine("Printed :", batchPrintedAtFormatter.format(Instant.ofEpochMilli(printedAtMillis))))
        appendLine("Filter  :")
        wrapThermalWords(filterSummary, THERMAL_LINE_WIDTH - 2).forEach { line ->
            appendLine("> ${line.take(THERMAL_LINE_WIDTH - 2)}".take(THERMAL_LINE_WIDTH))
        }
        appendLine(formatThermalLine("Date    :", formatAcquisitionBatchDateRange(dateRange)))
        appendLine(thermalDividerLight())
        appendLine(formatThermalLine("Records :", n.toString()))
        appendLine(formatThermalLine("TOTAL   :", CurrencyFormatter.format(grandTotal)))
        appendLine(thermalDividerHeavy())

        acquisitions.forEachIndexed { index, acq ->
            appendLine(formatThermalLine("Acq ID (${index + 1}/$n):", acq.acquisition_id.toString().padStart(4, '0')))
            appendLine(formatThermalLine("Date   :", formatThermalDate(acq.date_acquired)))
            appendProductLinesForBatch(acq.product_name)
            val unit = if (acq.is_per_kg) "kg" else "pcs"
            appendLine(formatThermalLine("Qty    :", "${"%.2f".format(acq.quantity)} $unit"))
            appendLine(
                formatThermalLine(
                    "Price  :",
                    "${CurrencyFormatter.format(acq.price_per_unit)}/${if (acq.is_per_kg) "kg" else "pc"}",
                ),
            )
            appendLine(formatThermalLine("Total  :", CurrencyFormatter.format(acq.total_amount)))
            appendLine(formatThermalLine("Location:", acq.location.printLabel()))
            appendLine(thermalDividerLight())
        }

        appendLine(thermalDividerHeavy())
        appendLine(centerThermalLine("END OF REPORT"))
        appendLine("Verified by: _________________")
        appendLine(thermalDividerHeavy())
    }

    return if (body.length > ACQUISITION_BATCH_REPORT_MAX_CHARS) null else body
}

private fun formatAcquisitionBatchDateRange(range: Pair<Long?, Long?>): String {
    val (start, end) = range
    return when {
        start == null && end == null -> "All dates"
        start != null && end != null ->
            "${isoDate.format(Instant.ofEpochMilli(start))} – ${isoDate.format(Instant.ofEpochMilli(end))}"
        start != null -> "From ${isoDate.format(Instant.ofEpochMilli(start))}"
        else -> "Until ${isoDate.format(Instant.ofEpochMilli(end!!))}"
    }
}

private fun StringBuilder.appendProductLinesForBatch(productName: String) {
    val first = productName.take(18)
    appendLine(formatThermalLine("Product:", first))
    if (productName.length > 18) {
        wrapThermalWords(productName.drop(18), THERMAL_LINE_WIDTH - 2).forEach { rest ->
            appendLine("  ${rest.take(THERMAL_LINE_WIDTH - 2)}")
        }
    }
}

private fun compressedAcquisitionSrpPerKg(acq: Acquisition): String {
    fun short(v: Double?) = v?.let { "%.0f".format(it) } ?: "—"
    val on = short(acq.srp_online_per_kg)
    val r = short(acq.srp_reseller_per_kg)
    val o = short(acq.srp_offline_per_kg)
    if (on == "—" && r == "—" && o == "—") return "—"
    return "On $on · R $r · O $o".take(THERMAL_LINE_WIDTH)
}

/** Word-wrap for plain thermal lines (no indent). */
private fun wrapThermalWords(text: String, maxWidth: Int): List<String> {
    if (text.isBlank()) return listOf("—")
    val words = text.split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var current = ""
    for (w in words) {
        val candidate = if (current.isEmpty()) w else "$current $w"
        if (candidate.length <= maxWidth) {
            current = candidate
        } else {
            if (current.isNotEmpty()) {
                lines.add(current)
                current = ""
            }
            if (w.length > maxWidth) {
                var rem = w
                while (rem.isNotEmpty()) {
                    lines.add(rem.take(maxWidth))
                    rem = rem.drop(maxWidth)
                }
            } else {
                current = w
            }
        }
    }
    if (current.isNotEmpty()) lines.add(current)
    return lines
}

fun buildRemittanceSlip(remittance: Remittance): String = buildString {
    val isDisb = RemittanceEntryType.isDisbursement(remittance.entry_type)
    val title = if (isDisb) "DISBURSEMENT RECEIPT" else "REMITTANCE SLIP"
    val idLabel = if (isDisb) "Disbursement # :" else "Remittance # :"
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine(title))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine(idLabel, remittance.remittance_id.toString().padStart(4, '0')))
    appendLine(formatThermalLine("Date         :", formatThermalDate(remittance.date)))
    appendLine(formatThermalLine("Amount       :", CurrencyFormatter.format(remittance.amount)))
    appendLine(thermalDividerLight())
    appendLine("Remarks:")
    remittance.remarks.lines().forEach { line ->
        appendLine(line.take(THERMAL_LINE_WIDTH))
    }
    if (remittance.remarks.isBlank()) appendLine("—")
    appendLine(thermalDividerHeavy())
    appendLine("Acknowledged: ________________")
    appendLine(thermalDividerHeavy())
}

data class ThermalSrpPrintRow(
    val name: String,
    val perKg: Double?,
    val per500g: Double?,
    val perPiece: Double?,
    /** When false, customer price is **per piece** only (CLARIF-01 / BUG-ACQ-08). */
    val isPerKg: Boolean = true,
)

fun buildSrpPriceList(
    channelLabel: String,
    presetName: String?,
    asOfMillis: Long?,
    rows: List<ThermalSrpPrintRow>,
): String = buildString {
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine("PRICE LIST — ${channelLabel.uppercase(Locale.getDefault())}"))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Preset:", presetName ?: "—"))
    appendLine(
        formatThermalLine(
            "As of:",
            asOfMillis?.let { formatThermalDate(it) } ?: "—"
        )
    )
    appendLine(thermalDividerLight())
    val hasKg = rows.any { it.isPerKg }
    val hasPc = rows.any { !it.isPerKg }
    val headerRight = when {
        hasKg && hasPc -> "/kg /500g *"
        hasPc -> "/pc"
        else -> "/kg    /500g"
    }
    appendLine(formatThermalLine("Product", headerRight))
    appendLine(thermalDividerLight())
    rows.forEach { r ->
        if (r.isPerKg) {
            val name = r.name.take(16)
            val kg = r.perKg?.let { "%.2f".format(it) } ?: "—"
            val h = r.per500g?.let { "%.2f".format(it) } ?: "—"
            appendLine(formatThermalLine(name, "$kg   $h"))
            if (r.perPiece != null) {
                appendLine(formatThermalLine("  /pc", CurrencyFormatter.format(r.perPiece)))
            }
        } else {
            val name = r.name.take(16)
            val pc = r.perPiece?.let { CurrencyFormatter.format(it) } ?: "—"
            appendLine(formatThermalLine(name, "$pc /pc"))
        }
    }
    appendLine(thermalDividerLight())
    if (hasKg && hasPc) {
        appendLine("* Mixed list: /pc = per-piece items")
        appendLine(thermalDividerLight())
    }
    appendLine("* All prices in PHP")
    appendLine(thermalDividerHeavy())
}

fun buildFarmOperationLog(operation: FarmOperation): String = buildString {
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine("FARM OPERATION LOG"))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Date     :", formatThermalDate(operation.operation_date)))
    appendLine(formatThermalLine("Type     :", operation.operation_type.name))
    appendLine(formatThermalLine("Product  :", operation.product_name.ifBlank { "—" }.take(20)))
    appendLine(formatThermalLine("Area     :", operation.area.ifBlank { "—" }.take(20)))
    appendLine(formatThermalLine("Personnel:", operation.personnel.ifBlank { "—" }.take(20)))
    appendLine(formatThermalLine("Weather  :", operation.weather_condition.ifBlank { "—" }.take(20)))
    appendLine(thermalDividerLight())
    appendLine("Details:")
    wrapThermalDetails(operation.details).forEach { appendLine(it) }
    appendLine(thermalDividerHeavy())
}

/** Wrap at ~30 chars after a 2-space indent (32-col thermal). */
private fun wrapThermalDetails(text: String): List<String> {
    val indent = "  "
    if (text.isBlank()) return listOf("${indent}—")
    val words = text.split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var current = indent
    for (w in words) {
        val add = if (current == indent) w else " $w"
        if (current.length + add.length <= THERMAL_LINE_WIDTH) {
            current += add
        } else {
            if (current.isNotBlank()) lines.add(current.take(THERMAL_LINE_WIDTH))
            current = indent + w
        }
    }
    if (current.isNotBlank()) lines.add(current.take(THERMAL_LINE_WIDTH))
    return lines
}

// ─── EOD / Outstanding inventory (PRN-08, PRN-09) ────────────────────────────

data class ThermalEodChannelRow(val label: String, val orderCount: Int, val amount: Double)
/** [qtyDisplay] e.g. `12.00 kg`, `5 pc`, or `1.00 kg + 3 pc` (matches Day Close screen). */
data class ThermalEodProductRow(val name: String, val qtyDisplay: String, val revenue: Double)
data class ThermalEodInventoryRow(
    val name: String,
    val theoreticalQty: Double,
    val actualQty: Double?,
    val varianceQty: Double?,
    val unitLabel: String,
)
data class ThermalEodUnpaidRow(val orderId: Int, val customer: String, val amount: Double)

/**
 * PRN-08 — End of Day summary (58 mm, 32 columns). Pass [isDraft] true when the close is not finalized.
 */
fun buildEodSummary(
    isDraft: Boolean,
    businessDateMillis: Long,
    totalOrders: Int,
    grossRevenue: Double,
    collected: Double,
    unpaidAllCount: Int?,
    unpaidAllAmount: Double?,
    channels: List<ThermalEodChannelRow>,
    topProducts: List<ThermalEodProductRow>,
    inventory: List<ThermalEodInventoryRow>,
    totalVarianceCost: Double,
    expectedCash: Double,
    remitted: Double,
    cashOnHand: Double?,
    remarks: String?,
    cogs: Double,
    margin: Double,
    marginPct: Double?,
    wagesToday: Double,
    unpaidOrders: List<ThermalEodUnpaidRow>,
    unpaidOrdersExtraCount: Int,
    outstandingPrintedTotal: Double,
    printedBy: String,
    printedAtMillis: Long = System.currentTimeMillis(),
    closedBy: String? = null,
    closedAtMillis: Long? = null,
): String = buildString {
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    if (isDraft) {
        appendLine(centerThermalLine("DRAFT — NOT FINAL"))
    }
    appendLine(centerThermalLine("END OF DAY REPORT"))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Date:", formatThermalDate(businessDateMillis)))
    appendLine(thermalDividerLight())
    appendLine("SALES")
    appendLine(formatThermalLine("Orders", totalOrders.toString()))
    appendLine(formatThermalLine("Gross", CurrencyFormatter.format(grossRevenue)))
    appendLine(formatThermalLine("Collected", CurrencyFormatter.format(collected)))
    unpaidAllCount?.let {
        appendLine(formatThermalLine("Unpaid #", it.toString()))
    }
    unpaidAllAmount?.let {
        appendLine(formatThermalLine("Unpaid amt", CurrencyFormatter.format(it)))
    }
    appendLine(thermalDividerLight())
    appendLine("BY CHANNEL")
    if (channels.isEmpty()) {
        appendLine("—")
    } else {
        channels.forEach { ch ->
            appendLine(formatThermalLine(ch.label.take(14), "${ch.orderCount} · ${CurrencyFormatter.format(ch.amount)}"))
        }
    }
    appendLine(thermalDividerLight())
    appendLine("TOP PRODUCTS (rev)")
    if (topProducts.isEmpty()) {
        appendLine("—")
    } else {
        topProducts.forEach { p ->
            appendLine(p.name.take(THERMAL_LINE_WIDTH))
            appendLine(
                formatThermalLine(
                    "  ${p.qtyDisplay}",
                    CurrencyFormatter.format(p.revenue),
                ),
            )
        }
    }
    appendLine(thermalDividerLight())
    appendLine("INVENTORY CLOSE")
    inventory.forEach { row ->
        appendLine(row.name.take(THERMAL_LINE_WIDTH))
        appendLine(
            formatThermalLine(
                " th",
                "%.2f %s".format(row.theoreticalQty, row.unitLabel),
            ),
        )
        appendLine(
            formatThermalLine(
                " act",
                row.actualQty?.let { "%.2f %s".format(it, row.unitLabel) } ?: "—",
            ),
        )
        row.varianceQty?.let { v ->
            appendLine(formatThermalLine(" var", "%.2f %s".format(v, row.unitLabel)))
        }
    }
    appendLine(formatThermalLine("Var cost", CurrencyFormatter.format(totalVarianceCost)))
    appendLine(thermalDividerLight())
    appendLine("CASH")
    appendLine(formatThermalLine("Exp cash", CurrencyFormatter.format(expectedCash)))
    appendLine(formatThermalLine("Remitted", CurrencyFormatter.format(remitted)))
    cashOnHand?.let {
        appendLine(formatThermalLine("On hand", CurrencyFormatter.format(it)))
    }
    remarks?.takeIf { it.isNotBlank() }?.let { r ->
        appendLine("Remarks:")
        wrapThermalWords(r, THERMAL_LINE_WIDTH).forEach { appendLine(it.take(THERMAL_LINE_WIDTH)) }
    }
    appendLine(thermalDividerLight())
    appendLine(formatThermalLine("COGS", CurrencyFormatter.format(cogs)))
    appendLine(
        formatThermalLine(
            "Margin",
            marginPct?.let { "${CurrencyFormatter.format(margin)} (${"%.1f".format(it)}%)" }
                ?: CurrencyFormatter.format(margin),
        ),
    )
    appendLine(thermalDividerLight())
    appendLine("OUTSTANDING ORDERS")
    if (unpaidOrders.isEmpty() && unpaidOrdersExtraCount == 0) {
        appendLine("—")
    } else {
        unpaidOrders.forEach { u ->
            appendLine("#${u.orderId} ${u.customer.take(12)}")
            appendLine(formatThermalLine(" amt", CurrencyFormatter.format(u.amount)))
        }
        if (unpaidOrdersExtraCount > 0) {
            appendLine("+$unpaidOrdersExtraCount more (see app)")
            appendLine(formatThermalLine("Listed total", CurrencyFormatter.format(outstandingPrintedTotal)))
        }
    }
    appendLine(thermalDividerLight())
    appendLine(formatThermalLine("Employee wages paid today", CurrencyFormatter.format(wagesToday)))
    appendLine(thermalDividerHeavy())
    if (isDraft) {
        appendLine(formatThermalLine("Printed by", printedBy.take(18)))
        appendLine(formatThermalLine("Printed at", formatThermalDate(printedAtMillis)))
    } else {
        appendLine(formatThermalLine("Closed by", (closedBy ?: printedBy).take(18)))
        appendLine(
            formatThermalLine(
                "Closed at",
                closedAtMillis?.let { formatThermalDate(it) } ?: formatThermalDate(printedAtMillis),
            ),
        )
    }
    appendLine(thermalDividerHeavy())
}

/**
 * PRN-09 — Outstanding inventory report.
 */
fun buildOutstandingInventoryReport(
    lines: List<Triple<String, Double, Int>>,
    totalValue: Double,
    printedBy: String,
    asOfMillis: Long = System.currentTimeMillis(),
    titleExtra: String? = null,
): String = buildString {
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine("OUTSTANDING INV."))
    titleExtra?.let { appendLine(centerThermalLine(it.take(THERMAL_LINE_WIDTH))) }
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("As of", formatThermalDate(asOfMillis)))
    appendLine(thermalDividerLight())
    lines.forEach { (name, qtyKg, days) ->
        appendLine(name.take(THERMAL_LINE_WIDTH))
        appendLine(formatThermalLine(" qty (kg)", "%.3f".format(qtyKg)))
        appendLine(formatThermalLine(" days on hand", days.toString()))
        appendLine(thermalDividerLight())
    }
    appendLine(formatThermalLine("TOTAL VALUE", CurrencyFormatter.format(totalValue)))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Printed by", printedBy.take(18)))
    appendLine(thermalDividerHeavy())
}

fun buildEmployeePayrollSummary(
    employeeName: String,
    periodLabel: String,
    filteredPayments: List<EmployeePayment>,
    allEmployeePayments: List<EmployeePayment>,
): String = buildString {
    val totals = filteredPayments.periodTotals()
    val totalNet = filteredPayments.sumOf { it.netPayAmount() }
    val outstanding = allEmployeePayments.lifetimeOutstandingAdvance()
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine("PAYROLL SUMMARY"))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Employee :", employeeName.take(20)))
    appendLine(formatThermalLine("Period   :", periodLabel.take(20)))
    appendLine(formatThermalLine("Payments :", filteredPayments.size.toString()))
    appendLine(thermalDividerLight())
    appendLine(formatThermalLine("Total Gross Wage :", CurrencyFormatter.format(totals.totalGross)))
    appendLine(formatThermalLine("Total Advance    :", CurrencyFormatter.format(totals.totalCashAdvance)))
    appendLine(thermalDividerLight())
    appendLine(formatThermalLine("TOTAL NET PAY    :", CurrencyFormatter.format(totalNet)))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Outstanding Adv.:", CurrencyFormatter.format(outstanding)))
    appendLine(thermalDividerHeavy())
    appendLine("Prepared by: __________________")
    appendLine(thermalDividerHeavy())
}
