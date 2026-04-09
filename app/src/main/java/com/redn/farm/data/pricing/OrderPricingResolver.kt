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

    /**
     * Whether the UI may offer a **per kg / per piece** toggle (BUG-ORD-07).
     *
     * Uses the same acquisition signals as [resolveUnitPrice]: [minPerKgSrpAcrossChannels] and
     * [minPerPieceSrpAcrossChannels] (per-piece includes **derived** from per-kg + [Acquisition.piece_count]).
     */
    fun productSupportsDualUnit(productPrice: ProductPrice?, acquisition: Acquisition?): Boolean {
        val pp = productPrice ?: return false
        val hasKg = (pp.per_kg_price ?: 0.0) > 0 || (pp.discounted_per_kg_price ?: 0.0) > 0
        val hasPc = (pp.per_piece_price ?: 0.0) > 0 || (pp.discounted_per_piece_price ?: 0.0) > 0
        val srpKg = minPerKgSrpAcrossChannels(acquisition) != null
        val srpPc = minPerPieceSrpAcrossChannels(acquisition) != null
        return (hasKg && hasPc) || (srpKg && srpPc) || (hasKg && srpPc) || (hasPc && srpKg)
    }

    /** Minimum positive per-kg SRP across channels for "from ₱X" summary (ORD-US-08, PRD-US-01). */
    fun minPerKgSrpAcrossChannels(acquisition: Acquisition?): Double? {
        val acq = acquisition ?: return null
        val values = listOfNotNull(
            acq.srp_online_per_kg,
            acq.srp_reseller_per_kg,
            acq.srp_offline_per_kg
        ).filter { it > 0 }
        return values.minOrNull()
    }

    /** Minimum positive per-piece SRP (resolved per channel, includes derived from per-kg × piece_count) — PRD-US-01. */
    fun minPerPieceSrpAcrossChannels(acquisition: Acquisition?): Double? {
        val acq = acquisition ?: return null
        val values = listOf(SalesChannel.ONLINE, SalesChannel.RESELLER, SalesChannel.OFFLINE)
            .mapNotNull { ch -> srpFromAcquisition(acq, ch, isPerKg = false)?.takeIf { it > 0 } }
        return values.minOrNull()
    }

    /** Non-null when the acquisition contributes at least one positive customer SRP used in the catalog. */
    fun catalogSrpSummaryAmounts(acquisition: Acquisition?): CatalogSrpSummary? {
        val acq = acquisition ?: return null
        val minKg = minPerKgSrpAcrossChannels(acq)?.takeIf { it > 0 }
        val minPc = minPerPieceSrpAcrossChannels(acq)?.takeIf { it > 0 }
        if (minKg == null && minPc == null) return null
        return CatalogSrpSummary(minPerKg = minKg, minPerPiece = minPc)
    }

    data class CatalogSrpSummary(val minPerKg: Double?, val minPerPiece: Double?)
}
