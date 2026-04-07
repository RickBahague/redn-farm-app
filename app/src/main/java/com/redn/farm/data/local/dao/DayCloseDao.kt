package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.DayCloseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayCloseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DayCloseEntity): Long

    @Update
    suspend fun update(entity: DayCloseEntity)

    @Query("SELECT * FROM day_closes WHERE business_date = :businessDate LIMIT 1")
    suspend fun getByDate(businessDate: Long): DayCloseEntity?

    @Query("SELECT * FROM day_closes WHERE close_id = :closeId LIMIT 1")
    suspend fun getById(closeId: Int): DayCloseEntity?

    @Query("SELECT * FROM day_closes ORDER BY business_date DESC")
    fun getAllDesc(): Flow<List<DayCloseEntity>>

    @Query("SELECT * FROM day_closes WHERE is_finalized = 1 ORDER BY business_date DESC")
    fun getFinalizedDesc(): Flow<List<DayCloseEntity>>

    @Query("SELECT * FROM day_closes ORDER BY business_date DESC LIMIT 1")
    suspend fun getMostRecent(): DayCloseEntity?
}
