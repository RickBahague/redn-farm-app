package com.redn.farm.data.pricing

import kotlin.math.abs

/**
 * Pure acquisition → SRP pipeline (INV-US-05, FR-PC-10 / FR-PC-14).
 * No Android / Room types.
 */
object SrpCalculator {

    private const val EPS = 1e-9

    data class Input(
        /** Total lot cost B = quantity × price_per_unit */
        val bulkCost: Double,
        val bulkQuantityKg: Double,
        val spoilageRate: Double,
        val additionalCostPerKg: Double,
        val channels: ChannelsConfiguration,
        val pieceCount: Int?
    )

    data class Output(
        val sellableQuantityKg: Double,
        val costPerSellableKg: Double,
        val srpOnlinePerKg: Double,
        val srpResellerPerKg: Double,
        val srpOfflinePerKg: Double,
        val srpOnline500g: Double,
        val srpOnline250g: Double,
        val srpOnline100g: Double,
        val srpReseller500g: Double,
        val srpReseller250g: Double,
        val srpReseller100g: Double,
        val srpOffline500g: Double,
        val srpOffline250g: Double,
        val srpOffline100g: Double,
        val srpOnlinePerPiece: Double?,
        val srpResellerPerPiece: Double?,
        val srpOfflinePerPiece: Double?
    )

    sealed class Result {
        data class Ok(val output: Output) : Result()
        data class Error(val message: String) : Result()
    }

    fun compute(input: Input): Result {
        if (input.bulkCost <= EPS) return Result.Error("Bulk cost must be positive")
        if (input.bulkQuantityKg <= EPS) return Result.Error("Quantity (kg) must be positive")
        if (input.spoilageRate < 0 || input.spoilageRate >= 1.0 - EPS) {
            return Result.Error("Spoilage must be in [0, 1)")
        }
        val sellable = input.bulkQuantityKg * (1.0 - input.spoilageRate)
        if (sellable <= EPS) return Result.Error("Sellable quantity is zero (check spoilage)")

        val c = input.bulkCost / sellable

        val onlineKg = PricingChannelEngine.channelSrpPerKg(c, input.additionalCostPerKg, input.channels.online)
        val resellerKg = PricingChannelEngine.channelSrpPerKg(c, input.additionalCostPerKg, input.channels.reseller)
        val offlineKg = PricingChannelEngine.channelSrpPerKg(c, input.additionalCostPerKg, input.channels.offline)

        val (o5, o25, o10) = PricingChannelEngine.fractionalPackageSrps(
            onlineKg,
            input.channels.online.roundingRule
        )
        val (r5, r25, r10) = PricingChannelEngine.fractionalPackageSrps(
            resellerKg,
            input.channels.reseller.roundingRule
        )
        val (f5, f25, f10) = PricingChannelEngine.fractionalPackageSrps(
            offlineKg,
            input.channels.offline.roundingRule
        )

        val pc = input.pieceCount
        val op = if (pc != null && pc > 0) PricingChannelEngine.perPieceSrp(onlineKg, pc) else null
        val rp = if (pc != null && pc > 0) PricingChannelEngine.perPieceSrp(resellerKg, pc) else null
        val fp = if (pc != null && pc > 0) PricingChannelEngine.perPieceSrp(offlineKg, pc) else null

        return Result.Ok(
            Output(
                sellableQuantityKg = sellable,
                costPerSellableKg = c,
                srpOnlinePerKg = onlineKg,
                srpResellerPerKg = resellerKg,
                srpOfflinePerKg = offlineKg,
                srpOnline500g = o5,
                srpOnline250g = o25,
                srpOnline100g = o10,
                srpReseller500g = r5,
                srpReseller250g = r25,
                srpReseller100g = r10,
                srpOffline500g = f5,
                srpOffline250g = f25,
                srpOffline100g = f10,
                srpOnlinePerPiece = op,
                srpResellerPerPiece = rp,
                srpOfflinePerPiece = fp
            )
        )
    }

    /** Derive kg quantity for spoilage pipeline from acquisition fields (INV-US-05). */
    fun bulkQuantityKg(quantity: Double, isPerKg: Boolean, pieceCount: Int?): Double? {
        return when {
            isPerKg -> quantity
            else -> {
                val n = pieceCount ?: return null
                if (n <= 0) return null
                quantity / n
            }
        }
    }

    fun costsDiffer(a: Double, b: Double): Boolean = abs(a - b) > 0.001
}
