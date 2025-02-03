package com.redn.farm.data.model

import java.time.LocalDateTime

data class ProductPrice(
    val price_id: Int = 0,
    val product_id: String,
    val per_kg_price: Double?,
    val per_piece_price: Double?,
    val date_created: LocalDateTime = LocalDateTime.now()
)

data class ProductPriceList(
    val product_prices: List<ProductPrice>
) 