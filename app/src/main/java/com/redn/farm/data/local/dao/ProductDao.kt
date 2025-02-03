package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY product_name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>): List<Long>

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE product_id = :productId")
    suspend fun deleteProduct(productId: String)

    @Query("SELECT * FROM products WHERE product_id = :productId")
    fun getProduct(productId: String): Flow<ProductEntity?>

    @Query("DELETE FROM products")
    suspend fun truncate()
} 