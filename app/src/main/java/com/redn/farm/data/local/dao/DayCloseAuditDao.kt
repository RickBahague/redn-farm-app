package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.DayCloseAuditEntity

@Dao
interface DayCloseAuditDao {

    @Insert
    suspend fun insert(entry: DayCloseAuditEntity): Long

    @Query("SELECT * FROM day_close_audit WHERE close_id = :closeId ORDER BY at_millis ASC")
    suspend fun getByCloseId(closeId: Int): List<DayCloseAuditEntity>
}
