package com.redn.farm.data.pricing

/**
 * Illustrative SRP per kg for activation preview (Phase 2).
 * Delegates channel math to [PricingChannelEngine].
 */
object PresetPreviewCalculator {

    private const val EPS = 1e-9

    fun derivedAdditionalCostPerKg(
        haulingWeightKg: Double,
        haulingFees: List<HaulingFeeItem>
    ): Double {
        if (haulingWeightKg <= EPS) return 0.0
        val sum = haulingFees.sumOf { it.amount }
        return sum / haulingWeightKg
    }

    data class ChannelDetailedResult(
        val channelKey: String,
        val srpPerKg: Double,
        val coreCostPerKg: Double // "C" = costPerSellableKg + additionalCostPerKg
    )

    fun previewSrpsPerKg(
        bulkCost: Double,
        bulkQuantityKg: Double,
        spoilageRate: Double,
        additionalCostPerKg: Double,
        channels: ChannelsConfiguration
    ): List<ChannelDetailedResult> {
        require(bulkCost > 0 && bulkQuantityKg > 0)
        require(spoilageRate in 0.0..1.0 - EPS)
        val sellable = bulkQuantityKg * (1.0 - spoilageRate)
        require(sellable > EPS)
        val costPerSellableKg = bulkCost / sellable
        val coreC = costPerSellableKg + additionalCostPerKg

        return listOf(
            "online" to channels.online,
            "reseller" to channels.reseller,
            "offline" to channels.offline
        ).map { (key, cfg) ->
            ChannelDetailedResult(
                channelKey = key,
                srpPerKg = PricingChannelEngine.channelSrpPerKg(costPerSellableKg, additionalCostPerKg, cfg),
                coreCostPerKg = coreC
            )
        }
    }
}
