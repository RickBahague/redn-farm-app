package com.redn.farm.utils

import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.data.model.Order
import com.redn.farm.data.model.OrderItem
import com.redn.farm.data.model.Remittance
import com.redn.farm.data.model.lifetimeOutstandingAdvance
import com.redn.farm.data.model.netPayAmount
import com.redn.farm.data.model.periodTotals
import com.redn.farm.data.pricing.SalesChannel
import java.time.Instant
import java.time.LocalDateTime
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

private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val detailDateTime: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())

fun formatOrderDetailDate(millis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(detailDateTime)

fun formatThermalDate(millis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(isoDate)

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
        appendLine(
            "   ${item.quantity}${if (item.is_per_kg) "kg" else "pc"} x ${CurrencyFormatter.format(item.price_per_unit)}"
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
        appendLine(
            formatThermalLine(
                "SRP Online :",
                acquisition.srp_online_per_kg?.let { "${CurrencyFormatter.format(it)}/kg" } ?: "—"
            )
        )
        appendLine(
            formatThermalLine(
                "SRP Reseller:",
                acquisition.srp_reseller_per_kg?.let { "${CurrencyFormatter.format(it)}/kg" } ?: "—"
            )
        )
        appendLine(
            formatThermalLine(
                "SRP Offline:",
                acquisition.srp_offline_per_kg?.let { "${CurrencyFormatter.format(it)}/kg" } ?: "—"
            )
        )
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

fun buildRemittanceSlip(remittance: Remittance): String = buildString {
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine("REMITTANCE SLIP"))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Remittance # :", remittance.remittance_id.toString().padStart(4, '0')))
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
    appendLine(formatThermalLine("Product", "/kg    /500g"))
    appendLine(thermalDividerLight())
    rows.forEach { r ->
        val name = r.name.take(16)
        val kg = r.perKg?.let { "%.2f".format(it) } ?: "—"
        val h = r.per500g?.let { "%.2f".format(it) } ?: "—"
        appendLine(formatThermalLine(name, "$kg   $h"))
        if (r.perPiece != null) {
            appendLine(formatThermalLine("  /pc", CurrencyFormatter.format(r.perPiece)))
        }
    }
    appendLine(thermalDividerLight())
    appendLine("* All prices in PHP")
    appendLine(thermalDividerHeavy())
}

fun buildFarmOperationLog(operation: FarmOperation): String = buildString {
    appendLine(thermalDividerHeavy())
    appendLine(centerThermalLine("REDN GREENS FRESH"))
    appendLine(centerThermalLine("FARM OPERATION LOG"))
    appendLine(thermalDividerHeavy())
    appendLine(formatThermalLine("Date     :", operation.operation_date.toLocalDate().format(isoDate)))
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
