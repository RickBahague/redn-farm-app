package com.redn.farm.data.pricing

import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.Product

/**
 * Default **order line** unit (per kg vs per piece) for new lines — **ORD-US-02**.
 *
 * When an **active acquisition** exists (INV-US-06), its costing basis [Acquisition.is_per_kg]
 * must drive the default so [OrderPricingResolver.resolveUnitPrice] reads the matching SRP columns.
 * Using [Product.unit_type] alone is wrong when the catalog says `kg` but the latest lot was
 * acquired per piece (or vice versa).
 */
fun defaultIsPerKgForOrderLine(product: Product, activeAcquisition: Acquisition?): Boolean {
    if (activeAcquisition != null) return activeAcquisition.is_per_kg
    return !product.unit_type.equals("piece", ignoreCase = true) &&
        !product.unit_type.equals("pieces", ignoreCase = true)
}
