package com.redn.farm.data.model

data class ProductPrice(
    val price_id: Int = 0,
    val product_id: String,
    val per_kg_price: Double?,
    val per_piece_price: Double?,
    val discounted_per_kg_price: Double? = null,
    val discounted_per_piece_price: Double? = null,
    val date_created: Long = System.currentTimeMillis()
)

data class ProductPriceList(
    val product_prices: List<ProductPrice>
)
