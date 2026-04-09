package com.redn.farm.data.pricing

import kotlin.math.ceil
import kotlin.math.round

/**
 * Shared per-channel SRP math (INV-US-05 pipeline in USER_STORIES.md; fractional tiers §5.2).
 * Used by [PresetPreviewCalculator] and [SrpCalculator].
 */
object PricingChannelEngine {

    private const val EPS = 1e-9

    /**
     * INV-US-05: `C = costPerSellableKg + additionalCostPerKg` (i.e. B/Q_sell + A),
     * then `priceAfterCore = C × (1 + markup)` (or margin path on `C`), then per-channel fees, then rounding.
     */
    fun channelSrpPerKg(
        costPerSellableKg: Double,
        additionalCostPerKg: Double,
        cfg: ChannelConfig
    ): Double {
        val markup = cfg.markupPercent
        val margin = cfg.marginPercent
        val c = costPerSellableKg + additionalCostPerKg
        val priceAfterCore = when {
            markup != null -> c * (1.0 + markup / 100.0)
            margin != null && margin in (EPS)..(100.0 - EPS) ->
                c / (1.0 - margin / 100.0)
            else -> c
        }
        var running = priceAfterCore
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
    fun perPieceSrp(srpPerKg: Double, pieceCount: Double): Double {
        require(pieceCount > 0.0)
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
