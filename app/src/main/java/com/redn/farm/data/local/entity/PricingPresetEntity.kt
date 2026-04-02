package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pricing preset — immutable after save. Never update fields directly;
 * always insert a new record. Only [is_active], [activated_at], [activated_by]
 * are mutated when a preset is activated (MGT-US-06).
 *
 * [channels_json] shape:
 *   {
 *     "online":   {"markup":0.35, "margin":null, "rounding_rule":"ceil_whole_peso", "fees":[]},
 *     "reseller": {"markup":0.25, "margin":null, "rounding_rule":"ceil_whole_peso", "fees":[]},
 *     "offline":  {"markup":0.30, "margin":null, "rounding_rule":"ceil_whole_peso", "fees":[]}
 *   }
 *   Fee element shape: {"label":"delivery surcharge","type":"fixed|pct","amount":10.0}
 *
 * [hauling_fees_json] shape: [{"label":"driver fee","amount":500.0}, ...]
 *
 * [categories_json] shape: [{"name":"Vegetables","spoilageRate":0.20,"additionalCostPerKg":null}, ...]
 */
@Entity(tableName = "pricing_presets")
data class PricingPresetEntity(
    @PrimaryKey
    val preset_id: String,                          // UUID assigned on save
    val preset_name: String,                        // admin-entered or auto-generated
    val saved_at: Long,
    val saved_by: String,

    // Activation state (MGT-US-06)
    val is_active: Boolean = false,
    val activated_at: Long? = null,
    val activated_by: String? = null,

    // Store-wide defaults (MGT-US-01)
    val spoilage_rate: Double,
    val additional_cost_per_kg: Double,
    val hauling_weight_kg: Double,
    val hauling_fees_json: String,                  // JSON array of fee line items

    // Per-channel config: markup/margin, rounding rule, optional fees (MGT-US-02)
    val channels_json: String,

    // Per-category overrides (MGT-US-03)
    val categories_json: String = "[]"
)
