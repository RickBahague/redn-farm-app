package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.AcquisitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcquisitionDao {
    @Query("SELECT * FROM acquisitions ORDER BY date_acquired DESC")
    fun getAllAcquisitions(): Flow<List<AcquisitionEntity>>

    @Query("SELECT * FROM acquisitions WHERE product_id = :productId")
    fun getAcquisitionsForProduct(productId: String): Flow<List<AcquisitionEntity>>

    @Insert
    suspend fun insert(acquisition: AcquisitionEntity)

    @Update
    suspend fun update(acquisition: AcquisitionEntity)

    @Delete
    suspend fun delete(acquisition: AcquisitionEntity)

    @Query("DELETE FROM acquisitions")
    suspend fun truncate()
} 