package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.DayCloseInventoryEntity

@Dao
interface DayCloseInventoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<DayCloseInventoryEntity>)

    @Update
    suspend fun update(row: DayCloseInventoryEntity)

    @Query("SELECT * FROM day_close_inventory WHERE close_id = :closeId ORDER BY product_name ASC")
    suspend fun getByCloseId(closeId: Int): List<DayCloseInventoryEntity>

    @Query("DELETE FROM day_close_inventory WHERE close_id = :closeId")
    suspend fun deleteByCloseId(closeId: Int)

    /**
     * Sum of posted [DayCloseInventoryEntity.variance_qty] from all finalized closes
     * strictly before [beforeStartMillis] (start of the target business day).
     */
    @Query(
        """
        SELECT dci.product_id, COALESCE(SUM(COALESCE(dci.variance_qty, 0)), 0) AS total_variance
        FROM day_close_inventory dci
        INNER JOIN day_closes dc ON dci.close_id = dc.close_id
        WHERE dc.is_finalized = 1 AND dc.business_date < :beforeStartMillis
        GROUP BY dci.product_id
        """
    )
    suspend fun getPriorPostedVarianceByProduct(beforeStartMillis: Long): List<ProductPriorVarianceRow>
}

data class ProductPriorVarianceRow(
    val product_id: String,
    val total_variance: Double,
)
