package com.redn.farm.data.pricing

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * JSON shapes stored in [com.redn.farm.data.local.entity.PricingPresetEntity].
 * Markup/margin: exactly one non-null per channel (enforced in editor UI).
 */
data class HaulingFeeItem(
    val label: String,
    val amount: Double
)

data class CategoryOverride(
    val name: String,
    val spoilageRate: Double? = null,
    val additionalCostPerKg: Double? = null
)

data class ChannelFee(
    val label: String,
    val type: String, // "fixed" | "pct"
    val amount: Double
)

data class ChannelConfig(
    val markupPercent: Double? = null,
    val marginPercent: Double? = null,
    val roundingRule: String = "ceil_whole_peso",
    val fees: List<ChannelFee> = emptyList()
)

data class ChannelsConfiguration(
    val online: ChannelConfig,
    val reseller: ChannelConfig,
    val offline: ChannelConfig
)

object PricingPresetGson {
    val gson: Gson = GsonBuilder().serializeNulls().create()

    fun channelsToJson(c: ChannelsConfiguration): String = gson.toJson(c)

    fun channelsFromJson(json: String): ChannelsConfiguration =
        gson.fromJson(json, ChannelsConfiguration::class.java)
            ?: defaultChannelsConfiguration()

    fun haulingFeesToJson(list: List<HaulingFeeItem>): String = gson.toJson(list)

    fun haulingFeesFromJson(json: String): List<HaulingFeeItem> {
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<HaulingFeeItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun categoriesToJson(list: List<CategoryOverride>): String = gson.toJson(list)

    fun categoriesFromJson(json: String): List<CategoryOverride> {
        if (json.isBlank() || json == "[]") return emptyList()
        val type = object : TypeToken<List<CategoryOverride>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun defaultChannelsConfiguration(): ChannelsConfiguration = ChannelsConfiguration(
        online = ChannelConfig(
            markupPercent = 35.0,
            marginPercent = null,
            roundingRule = "ceil_whole_peso",
            fees = emptyList()
        ),
        reseller = ChannelConfig(
            markupPercent = 25.0,
            marginPercent = null,
            roundingRule = "ceil_whole_peso",
            fees = emptyList()
        ),
        offline = ChannelConfig(
            markupPercent = 30.0,
            marginPercent = null,
            roundingRule = "ceil_whole_peso",
            fees = emptyList()
        )
    )

    fun defaultHaulingFees(): List<HaulingFeeItem> = listOf(
        HaulingFeeItem("driver", 2000.0),
        HaulingFeeItem("fuel", 4000.0),
        HaulingFeeItem("toll", 1000.0),
        HaulingFeeItem("handling", 200.0)
    )
}
