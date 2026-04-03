package com.redn.farm.data.pricing

import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.ProductPrice

/**
 * Resolves unit price for order lines from acquisition SRPs (INV-US-06) with PRD-US-06 fallback.
 */
object OrderPricingResolver {

    fun srpFromAcquisition(acquisition: Acquisition?, channel: String, isPerKg: Boolean): Double? {
        val acq = acquisition ?: return null
        val ch = SalesChannel.normalize(channel)
        return if (isPerKg) {
            when (ch) {
                SalesChannel.ONLINE -> acq.srp_online_per_kg
                SalesChannel.RESELLER -> acq.srp_reseller_per_kg
                else -> acq.srp_offline_per_kg
            }
        } else {
            when (ch) {
                SalesChannel.ONLINE ->
                    acq.srp_online_per_piece ?: acq.srp_online_per_kg?.let { perKg ->
                        acq.piece_count?.takeIf { it > 0 }?.let { pc ->
                            PricingChannelEngine.perPieceSrp(perKg, pc)
                        }
                    }
                SalesChannel.RESELLER ->
                    acq.srp_reseller_per_piece ?: acq.srp_reseller_per_kg?.let { perKg ->
                        acq.piece_count?.takeIf { it > 0 }?.let { pc ->
                            PricingChannelEngine.perPieceSrp(perKg, pc)
                        }
                    }
                else ->
                    acq.srp_offline_per_piece ?: acq.srp_offline_per_kg?.let { perKg ->
                        acq.piece_count?.takeIf { it > 0 }?.let { pc ->
                            PricingChannelEngine.perPieceSrp(perKg, pc)
                        }
                    }
            }
        }
    }

    /**
     * PRD-US-06: discounted fallbacks apply when the order channel is reseller.
     */
    fun fallbackUnitPrice(productPrice: ProductPrice?, channel: String, isPerKg: Boolean): Double {
        val pp = productPrice ?: return 0.0
        val useDiscounted = SalesChannel.normalize(channel) == SalesChannel.RESELLER
        return if (isPerKg) {
            when {
                useDiscounted && pp.discounted_per_kg_price != null -> pp.discounted_per_kg_price!!
                else -> pp.per_kg_price ?: 0.0
            }
        } else {
            when {
                useDiscounted && pp.discounted_per_piece_price != null -> pp.discounted_per_piece_price!!
                else -> pp.per_piece_price ?: 0.0
            }
        }
    }

    fun resolveUnitPrice(
        acquisition: Acquisition?,
        channel: String,
        isPerKg: Boolean,
        productPrice: ProductPrice?
    ): Double {
        val srp = srpFromAcquisition(acquisition, channel, isPerKg)
        if (srp != null && srp > 0) return srp
        return fallbackUnitPrice(productPrice, channel, isPerKg)
    }

    /** Minimum positive per-kg SRP across channels for "from ₱X" summary (ORD-US-08). */
    fun minPerKgSrpAcrossChannels(acquisition: Acquisition?): Double? {
        val acq = acquisition ?: return null
        val values = listOfNotNull(
            acq.srp_online_per_kg,
            acq.srp_reseller_per_kg,
            acq.srp_offline_per_kg
        ).filter { it > 0 }
        return values.minOrNull()
    }
}
