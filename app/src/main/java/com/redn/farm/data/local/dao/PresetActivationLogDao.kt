package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.PresetActivationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetActivationLogDao {

    @Insert
    suspend fun insert(log: PresetActivationLogEntity)

    @Query("SELECT * FROM preset_activation_log ORDER BY activated_at DESC")
    fun getAllLogs(): Flow<List<PresetActivationLogEntity>>

    @Query("DELETE FROM preset_activation_log")
    suspend fun truncate()
}
