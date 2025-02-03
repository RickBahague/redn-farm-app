package com.redn.farm.data.model

data class Product(
    val product_id: String,
    val product_name: String,
    val product_description: String,
    val unit_type: String,
    val is_active: Boolean = true
)

data class ProductList(
    val products: List<Product>
) 