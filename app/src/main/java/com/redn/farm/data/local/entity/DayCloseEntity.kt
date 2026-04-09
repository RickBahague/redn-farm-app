package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "day_closes",
    indices = [
        Index("business_date", unique = true),
        Index(value = ["is_finalized", "business_date"])
    ]
)
data class DayCloseEntity(
    @PrimaryKey(autoGenerate = true)
    val close_id: Int = 0,

    /** Epoch millis of the start of the business day (midnight local time). UNIQUE. */
    val business_date: Long,

    val closed_by: String,

    /** Epoch millis when the close was finalized; null = draft/in-progress. */
    val closed_at: Long? = null,

    val is_finalized: Boolean = false,

    // Order snapshot
    val total_orders: Int = 0,
    val total_sales_amount: Double = 0.0,
    val total_collected: Double = 0.0,

    /** Count of orders that remain unpaid as of close time (snapshot). */
    val snapshot_all_unpaid_count: Int? = null,
    val snapshot_all_unpaid_amount: Double? = null,

    // Revenue and COGS for this business day (EOD-US-07)
    /** Gross revenue from orders on this business date. */
    val gross_revenue_today: Double? = null,
    /** Collected (paid) revenue on this business date. */
    val collected_revenue_today: Double? = null,
    /** Total cost of goods sold = qty_sold × WAC, for orders on this date. */
    val total_cogs_today: Double? = null,
    val gross_margin_amount: Double? = null,
    val gross_margin_percent: Double? = null,

    // Cash reconciliation (EOD-US-06)
    val cash_on_hand: Double? = null,
    val cash_reconciliation_remarks: String? = null,

    val notes: String? = null,
)
