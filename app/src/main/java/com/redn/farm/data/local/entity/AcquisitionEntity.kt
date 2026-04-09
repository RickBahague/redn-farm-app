package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.redn.farm.data.model.AcquisitionLocation

@Entity(
    tableName = "acquisitions",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("product_id"),
        Index("preset_ref"),
        Index("date_acquired")
    ]
)
data class AcquisitionEntity(
    @PrimaryKey(autoGenerate = true)
    val acquisition_id: Int = 0,

    // Core fields
    val product_id: String,
    val product_name: String,
    val quantity: Double,
    val price_per_unit: Double,
    val total_amount: Double,
    val is_per_kg: Boolean,
    /** INV-US-01: pieces per kg (may be fractional). */
    val piece_count: Double? = null,
    val date_acquired: Long,                // user-entered acquisition date
    val created_at: Long = System.currentTimeMillis(), // INV-US-06: tiebreaker, never updated
    val location: AcquisitionLocation,

    // Preset traceability (INV-US-05)
    val preset_ref: String? = null,         // FK to pricing_presets.preset_id

    // Preset snapshot at save time — immutable audit trail
    val spoilage_rate: Double? = null,
    /** CLARIF / BUG-PRC-04: absolute unsellable kg when entered; null = rate-only path. */
    val spoilage_kg: Double? = null,
    val additional_cost_per_kg: Double? = null,
    val hauling_weight_kg: Double? = null,
    val hauling_fees_json: String? = null,  // JSON: [{label, amount}, ...]
    val channels_snapshot_json: String? = null, // same shape as pricing_presets.channels_json

    // Computed SRPs per kg
    val srp_online_per_kg: Double? = null,
    val srp_reseller_per_kg: Double? = null,
    val srp_offline_per_kg: Double? = null,

    // Computed SRPs — fractional packages
    val srp_online_500g: Double? = null,
    val srp_online_250g: Double? = null,
    val srp_online_100g: Double? = null,
    val srp_reseller_500g: Double? = null,
    val srp_reseller_250g: Double? = null,
    val srp_reseller_100g: Double? = null,
    val srp_offline_500g: Double? = null,
    val srp_offline_250g: Double? = null,
    val srp_offline_100g: Double? = null,

    // Computed SRPs per piece (null when piece_count is null)
    val srp_online_per_piece: Double? = null,
    val srp_reseller_per_piece: Double? = null,
    val srp_offline_per_piece: Double? = null,

    /** 1 = customer SRP/kg per channel was set manually on the form (MGT-US-07). */
    val srp_custom_override: Boolean = false,
)
