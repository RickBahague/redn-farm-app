package com.redn.farm.data.pricing

import kotlin.math.ceil
import kotlin.math.round

/**
 * Shared per-channel SRP math (FR-PC-14, PricingReference §5.2).
 * Used by [PresetPreviewCalculator] and [SrpCalculator].
 */
object PricingChannelEngine {

    private const val EPS = 1e-9

    fun channelSrpPerKg(
        costPerSellableKg: Double,
        additionalCostPerKg: Double,
        cfg: ChannelConfig
    ): Double {
        val markup = cfg.markupPercent
        val margin = cfg.marginPercent
        val priceAfterCore = when {
            markup != null -> costPerSellableKg * (1.0 + markup / 100.0)
            margin != null && margin in (EPS)..(100.0 - EPS) ->
                costPerSellableKg / (1.0 - margin / 100.0)
            else -> costPerSellableKg
        }
        var running = priceAfterCore + additionalCostPerKg
        for (fee in cfg.fees) {
            running += when (fee.type.lowercase()) {
                "pct" -> running * (fee.amount / 100.0)
                else -> fee.amount
            }
        }
        return applyRounding(running, cfg.roundingRule)
    }

    /** Fractional tiers: ×0.5, ×0.25, ×0.1 then channel rounding (PricingReference §5.2 note). */
    fun fractionalPackageSrps(srpPerKg: Double, roundingRule: String): Triple<Double, Double, Double> {
        return Triple(
            applyRounding(srpPerKg * 0.5, roundingRule),
            applyRounding(srpPerKg * 0.25, roundingRule),
            applyRounding(srpPerKg * 0.1, roundingRule)
        )
    }

    /** US-6 / FR-PC-14: per-piece always ceil to whole PHP, independent of kg rounding. */
    fun perPieceSrp(srpPerKg: Double, pieceCount: Int): Double {
        require(pieceCount > 0)
        return ceil((srpPerKg / pieceCount) - EPS)
    }

    fun applyRounding(value: Double, rule: String): Double {
        return when (rule) {
            "ceil_whole_peso" -> ceil(value - EPS)
            "nearest_whole_peso" -> round(value).toDouble()
            "nearest_0.25" -> (round(value / 0.25) * 0.25)
            else -> ceil(value - EPS)
        }
    }
}
