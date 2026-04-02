package com.redn.farm.data.model

data class Acquisition(
    val acquisition_id: Int = 0,
    val product_id: String,
    val product_name: String,
    val quantity: Double,
    val price_per_unit: Double,
    val total_amount: Double,
    val is_per_kg: Boolean,
    val piece_count: Int? = null,
    val date_acquired: Long,
    /** DB insert time; preserved on update. Use `0` for new rows (repository assigns [System.currentTimeMillis]). */
    val created_at: Long = 0L,
    val location: AcquisitionLocation,
    val preset_ref: String? = null,
    val spoilage_rate: Double? = null,
    val additional_cost_per_kg: Double? = null,
    val hauling_weight_kg: Double? = null,
    val hauling_fees_json: String? = null,
    val channels_snapshot_json: String? = null,
    val srp_online_per_kg: Double? = null,
    val srp_reseller_per_kg: Double? = null,
    val srp_offline_per_kg: Double? = null,
    val srp_online_500g: Double? = null,
    val srp_online_250g: Double? = null,
    val srp_online_100g: Double? = null,
    val srp_reseller_500g: Double? = null,
    val srp_reseller_250g: Double? = null,
    val srp_reseller_100g: Double? = null,
    val srp_offline_500g: Double? = null,
    val srp_offline_250g: Double? = null,
    val srp_offline_100g: Double? = null,
    val srp_online_per_piece: Double? = null,
    val srp_reseller_per_piece: Double? = null,
    val srp_offline_per_piece: Double? = null
)

enum class AcquisitionLocation {
    FARM,
    MARKET,
    SUPPLIER,
    OTHER;

    override fun toString(): String {
        return when (this) {
            FARM -> "Farm"
            MARKET -> "Market"
            SUPPLIER -> "Supplier"
            OTHER -> "Other"
        }
    }
}
