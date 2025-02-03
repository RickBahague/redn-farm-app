package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.ProductPriceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductPriceDao {
    @Query("SELECT * FROM product_prices ORDER BY date_created DESC")
    fun getAllProductPrices(): Flow<List<ProductPriceEntity>>

    @Query("""
        SELECT * FROM product_prices 
        WHERE product_id = :productId 
        ORDER BY date_created DESC 
        LIMIT 1
    """)
    fun getLatestPrice(productId: String): Flow<ProductPriceEntity?>

    @Query("""
        SELECT pp.* 
        FROM product_prices pp
        INNER JOIN (
            SELECT product_id, MAX(date_created) as max_date
            FROM product_prices
            GROUP BY product_id
        ) latest 
        ON pp.product_id = latest.product_id 
        AND pp.date_created = latest.max_date
    """)
    fun getLatestPrices(): Flow<List<ProductPriceEntity>>

    @Query("""
        SELECT pp.* 
        FROM product_prices pp
        INNER JOIN (
            SELECT product_id, MAX(date_created) as max_date
            FROM product_prices
            GROUP BY product_id
        ) latest 
        ON pp.product_id = latest.product_id 
        AND pp.date_created = latest.max_date
    """)
    suspend fun getLatestPricesSync(): List<ProductPriceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(price: ProductPriceEntity): Long

    @Update
    suspend fun update(price: ProductPriceEntity)

    @Delete
    suspend fun delete(price: ProductPriceEntity)

    @Query("SELECT * FROM product_prices WHERE product_id = :productId ORDER BY date_created DESC")
    fun getPriceHistory(productId: String): Flow<List<ProductPriceEntity>>

    @Query("DELETE FROM product_prices")
    suspend fun truncate()
} 