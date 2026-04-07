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
}
