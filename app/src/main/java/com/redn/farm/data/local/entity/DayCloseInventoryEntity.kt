package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "day_close_inventory",
    foreignKeys = [
        ForeignKey(
            entity = DayCloseEntity::class,
            parentColumns = ["close_id"],
            childColumns = ["close_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["close_id", "product_id"])
    ]
)
data class DayCloseInventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val close_id: Int,
    val product_id: String,
    val product_name: String,

    // Running stock ledger fields (EOD-US-03)

    /** SUM of all acquisition quantities (in kg) from beginning of time through close date. */
    val total_acquired_all_time: Double,

    /** SUM of all order item quantities (in kg) for this product, up to and including close date. */
    val total_sold_through_close_date: Double,

    /**
     * Cumulative variance adjustments posted in all *previous* day closes.
     * Positive = prior surplus logged; negative = prior shrinkage logged.
     */
    val prior_posted_variance: Double = 0.0,

    /**
     * Computed: total_acquired_all_time − total_sold_through_close_date − prior_posted_variance.
     * This is the expected on-hand quantity before today's physical count.
     */
    val adjusted_theoretical_remaining: Double,

    /** Quantity sold specifically on this business date (for COGS line detail). */
    val sold_this_close_date: Double = 0.0,

    /** Physical count entered by user; null = not yet counted. */
    val actual_remaining: Double? = null,

    /** actual_remaining − adjusted_theoretical_remaining; null when not yet counted. */
    val variance_qty: Double? = null,

    /**
     * Weighted average cost per unit (kg) = SUM(acquisitions.total_amount) / SUM(acquisitions.quantity_kg).
     * D1 rule: always in kg; per-piece acquisitions converted via quantity / piece_count.
     */
    val weighted_avg_cost_per_unit: Double,

    /** variance_qty × weighted_avg_cost_per_unit; null when not yet counted. */
    val variance_cost: Double? = null,

    /** False when user explicitly excludes this product from the count. Default true. */
    val is_counted: Boolean = true,
)
