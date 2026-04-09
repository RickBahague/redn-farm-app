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
        /** Preset / category rate — stored on snapshot; required in [0, 1). When [spoilageKg] is set (per-kg only), [spoilageRate] is not used for sellable kg but must still be valid. */
        val spoilageRate: Double,
        val additionalCostPerKg: Double,
        val channels: ChannelsConfiguration,
        val pieceCount: Double?,
        /**
         * When **false** (acquisition entered **per piece**), CLARIF-01 / INV-US-05: spoilage is **not**
         * applied — **Q_sell = Q**. When **true** (per kg lot), preset spoilage applies even if [pieceCount]
         * is set (per-piece SRP derived from per-kg).
         */
        val isPerKgAcquisition: Boolean = true,
        /**
         * **BUG-PRC-04 / CLARIF-01:** for **per-kg** acquisitions only — absolute unsellable mass in kg.
         * If non-null, sellable kg = Q minus this mass and preset rate is ignored for the divisor.
         */
        val spoilageKg: Double? = null,
    )

    data class Output(
        val sellableQuantityKg: Double,
        /** Bulk amortized cost per sellable kg: B / Q_sell (excludes hauling A). */
        val costPerSellableKg: Double,
        /** INV-US-05 unit cost C = costPerSellableKg + A (before channel markup); 0 if not applicable (custom SRP-only path). */
        val coreCostPerKg: Double,
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
        val srpOfflinePerPiece: Double?,
        /** [Input.bulkQuantityKg] — Q before spoilage (FR-PC-10). */
        val bulkQuantityKg: Double,
        /** Effective fraction of bulk lost (rate path: preset; absolute kg path: spoilageKg / Q). */
        val spoilageRate: Double,
        /** Non-null when absolute spoilage kg was applied (per-kg acquisitions). */
        val spoilageAbsoluteKg: Double? = null,
        /** Hauling / preset additional cost per kg A ([Input.additionalCostPerKg]). */
        val additionalCostPerKg: Double,
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
        val useAbsoluteKg = input.isPerKgAcquisition && input.spoilageKg != null
        if (useAbsoluteKg) {
            val kg = input.spoilageKg!!
            if (kg < 0 || kg >= input.bulkQuantityKg - EPS) {
                return Result.Error("Unsellable kg must be in [0, quantity kg)")
            }
        }
        val sellable: Double
        val spoilageAbsoluteKg: Double?
        val effectiveSpoilageForOutput: Double
        when {
            !input.isPerKgAcquisition -> {
                sellable = input.bulkQuantityKg
                spoilageAbsoluteKg = null
                effectiveSpoilageForOutput = 0.0
            }
            useAbsoluteKg -> {
                val kg = input.spoilageKg!!
                sellable = input.bulkQuantityKg - kg
                spoilageAbsoluteKg = kg
                effectiveSpoilageForOutput = kg / input.bulkQuantityKg
            }
            else -> {
                sellable = input.bulkQuantityKg * (1.0 - input.spoilageRate)
                spoilageAbsoluteKg = null
                effectiveSpoilageForOutput = input.spoilageRate
            }
        }
        if (sellable <= EPS) return Result.Error("Sellable quantity is zero (check spoilage)")

        // B/Q_sell; hauling A is folded into C inside channelSrpPerKg (INV-US-05).
        val c = input.bulkCost / sellable
        val coreC = c + input.additionalCostPerKg

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
                coreCostPerKg = coreC,
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
                srpOfflinePerPiece = fp,
                bulkQuantityKg = input.bulkQuantityKg,
                spoilageRate = effectiveSpoilageForOutput,
                spoilageAbsoluteKg = spoilageAbsoluteKg,
                additionalCostPerKg = input.additionalCostPerKg,
            )
        )
    }

    /** Derive kg quantity for spoilage pipeline from acquisition fields (INV-US-05). */
    fun bulkQuantityKg(quantity: Double, isPerKg: Boolean, pieceCount: Double?): Double? {
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

    /** Customer SRP/kg per channel (MGT-US-07); fractional + piece use each channel's rounding from [channels]. */
    fun outputFromCustomerSrpPerKg(
        channels: ChannelsConfiguration,
        srpOnlinePerKg: Double,
        srpResellerPerKg: Double,
        srpOfflinePerKg: Double,
        pieceCount: Double?,
    ): Result {
        if (srpOnlinePerKg <= EPS) return Result.Error("Online SRP per kg must be positive")
        if (srpResellerPerKg <= EPS) return Result.Error("Reseller SRP per kg must be positive")
        if (srpOfflinePerKg <= EPS) return Result.Error("Offline SRP per kg must be positive")
        val (o5, o25, o10) = PricingChannelEngine.fractionalPackageSrps(
            srpOnlinePerKg,
            channels.online.roundingRule,
        )
        val (r5, r25, r10) = PricingChannelEngine.fractionalPackageSrps(
            srpResellerPerKg,
            channels.reseller.roundingRule,
        )
        val (f5, f25, f10) = PricingChannelEngine.fractionalPackageSrps(
            srpOfflinePerKg,
            channels.offline.roundingRule,
        )
        val pc = pieceCount
        val op = if (pc != null && pc > 0) PricingChannelEngine.perPieceSrp(srpOnlinePerKg, pc) else null
        val rp = if (pc != null && pc > 0) PricingChannelEngine.perPieceSrp(srpResellerPerKg, pc) else null
        val fp = if (pc != null && pc > 0) PricingChannelEngine.perPieceSrp(srpOfflinePerKg, pc) else null
        return Result.Ok(
            Output(
                sellableQuantityKg = 0.0,
                costPerSellableKg = 0.0,
                coreCostPerKg = 0.0,
                srpOnlinePerKg = srpOnlinePerKg,
                srpResellerPerKg = srpResellerPerKg,
                srpOfflinePerKg = srpOfflinePerKg,
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
                srpOfflinePerPiece = fp,
                bulkQuantityKg = 0.0,
                spoilageRate = 0.0,
                spoilageAbsoluteKg = null,
                additionalCostPerKg = 0.0,
            ),
        )
    }

    /** Preset cost/spoilage context for preview; SRP numbers from custom customer prices per channel. */
    fun mergeCostContextWithCustomSrps(
        costContext: Output,
        channels: ChannelsConfiguration,
        srpOnlinePerKg: Double,
        srpResellerPerKg: Double,
        srpOfflinePerKg: Double,
        pieceCount: Double?,
    ): Result {
        return when (
            val r = outputFromCustomerSrpPerKg(
                channels,
                srpOnlinePerKg,
                srpResellerPerKg,
                srpOfflinePerKg,
                pieceCount,
            )
        ) {
            is Result.Error -> r
            is Result.Ok ->
                Result.Ok(
                    r.output.copy(
                        sellableQuantityKg = costContext.sellableQuantityKg,
                        costPerSellableKg = costContext.costPerSellableKg,
                        coreCostPerKg = costContext.coreCostPerKg,
                        bulkQuantityKg = costContext.bulkQuantityKg,
                        spoilageRate = costContext.spoilageRate,
                        spoilageAbsoluteKg = costContext.spoilageAbsoluteKg,
                        additionalCostPerKg = costContext.additionalCostPerKg,
                    ),
                )
        }
    }
}
