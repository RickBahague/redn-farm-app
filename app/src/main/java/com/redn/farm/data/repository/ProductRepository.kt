package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.ProductDao
import com.redn.farm.data.local.dao.ProductPriceDao
import com.redn.farm.data.local.entity.ProductEntity
import com.redn.farm.data.local.entity.ProductPriceEntity
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.model.ProductFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val productPriceDao: ProductPriceDao
) {
    fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toProduct() }
        }
    }

    fun getAllProductPrices(): Flow<List<ProductPrice>> {
        return productPriceDao.getLatestPrices().map { prices ->
            prices.map { price ->
                ProductPrice(
                    price_id = price.price_id,
                    product_id = price.product_id,
                    per_kg_price = price.per_kg_price,
                    per_piece_price = price.per_piece_price,
                    discounted_per_kg_price = price.discounted_per_kg_price,
                    discounted_per_piece_price = price.discounted_per_piece_price,
                    date_created = price.date_created
                )
            }
        }
    }

    fun getLatestPrice(productId: String): Flow<ProductPrice?> {
        return productPriceDao.getLatestPrice(productId).map { price ->
            price?.let {
                ProductPrice(
                    price_id = it.price_id,
                    product_id = it.product_id,
                    per_kg_price = it.per_kg_price,
                    per_piece_price = it.per_piece_price,
                    discounted_per_kg_price = it.discounted_per_kg_price,
                    discounted_per_piece_price = it.discounted_per_piece_price,
                    date_created = it.date_created
                )
            }
        }
    }

    /** Full manual price history for **PRD-US-07** (newest first from DAO). */
    fun getPriceHistory(productId: String): Flow<List<ProductPrice>> {
        return productPriceDao.getPriceHistory(productId).map { entities ->
            entities.map { e ->
                ProductPrice(
                    price_id = e.price_id,
                    product_id = e.product_id,
                    per_kg_price = e.per_kg_price,
                    per_piece_price = e.per_piece_price,
                    discounted_per_kg_price = e.discounted_per_kg_price,
                    discounted_per_piece_price = e.discounted_per_piece_price,
                    date_created = e.date_created
                )
            }
        }
    }

    fun getFilteredProducts(filters: ProductFilters): Flow<List<Product>> {
        return productDao.getAllProducts().map { products ->
            var filteredProducts = products.map { it.toProduct() }

            // Apply search filter
            if (filters.searchQuery.isNotEmpty()) {
                val q = filters.searchQuery
                filteredProducts = filteredProducts.filter { product ->
                    product.product_name.contains(q, ignoreCase = true) ||
                        product.product_description.contains(q, ignoreCase = true) ||
                        product.product_id.contains(q, ignoreCase = true)
                }
            }
            if (filters.unitTypeFilter.isNotBlank()) {
                val u = filters.unitTypeFilter.trim()
                filteredProducts = filteredProducts.filter { product ->
                    product.unit_type.contains(u, ignoreCase = true)
                }
            }
            if (filters.categoryFilter.isNotBlank()) {
                val c = filters.categoryFilter.trim()
                filteredProducts = filteredProducts.filter { product ->
                    product.category?.contains(c, ignoreCase = true) == true
                }
            }

            // Apply out of stock filter
            if (!filters.showOutOfStock) {
                filteredProducts = filteredProducts.filter { it.is_active }
            }

            // Apply sorting
            filteredProducts = when (filters.sortBy) {
                "name" -> filteredProducts.sortedBy { it.product_name }
                "price" -> {
                    val prices = productPriceDao.getLatestPricesSync()
                    filteredProducts.sortedBy { product ->
                        prices.find { it.product_id == product.product_id }?.per_kg_price ?: Double.MAX_VALUE
                    }
                }
                else -> filteredProducts
            }

            filteredProducts
        }
    }

    suspend fun insertProduct(product: Product): Long {
        return productDao.insertProduct(
            ProductEntity(
                product_id = product.product_id,
                product_name = product.product_name,
                product_description = product.product_description,
                unit_type = product.unit_type,
                category = product.category,
                default_piece_count = product.defaultPieceCount,
                is_active = product.is_active
            )
        )
    }

    suspend fun insertProductPrice(price: ProductPrice): Long {
        return productPriceDao.insert(
            ProductPriceEntity(
                price_id = 0, // Let Room generate the ID
                product_id = price.product_id,
                per_kg_price = price.per_kg_price,
                per_piece_price = price.per_piece_price,
                discounted_per_kg_price = price.discounted_per_kg_price,
                discounted_per_piece_price = price.discounted_per_piece_price,
                date_created = price.date_created
            )
        )
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(
            ProductEntity(
                product_id = product.product_id,
                product_name = product.product_name,
                product_description = product.product_description,
                unit_type = product.unit_type,
                category = product.category,
                default_piece_count = product.defaultPieceCount,
                is_active = product.is_active
            )
        )
    }

    suspend fun deleteProduct(productId: String) {
        productDao.deleteProduct(productId)
    }

    suspend fun truncate() {
        productDao.truncate()
        productPriceDao.truncate()
    }

    suspend fun truncateProductPrices() {
        productPriceDao.truncate()
    }

    suspend fun updateProductPrice(productPrice: ProductPrice): Long {
        // Always insert a new price record instead of updating
        // This maintains price history
        return productPriceDao.insert(
            ProductPriceEntity(
                price_id = 0, // Let Room generate the ID
                product_id = productPrice.product_id,
                per_kg_price = productPrice.per_kg_price,
                per_piece_price = productPrice.per_piece_price,
                discounted_per_kg_price = productPrice.discounted_per_kg_price,
                discounted_per_piece_price = productPrice.discounted_per_piece_price,
                date_created = System.currentTimeMillis()
            )
        )
    }

    private fun ProductEntity.toProduct() = Product(
        product_id = product_id,
        product_name = product_name,
        product_description = product_description,
        unit_type = unit_type,
        is_active = is_active,
        category = category,
        defaultPieceCount = default_piece_count
    )
}