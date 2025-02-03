package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.RemittanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemittanceDao {
    @Query("SELECT * FROM remittances ORDER BY date DESC")
    fun getAllRemittances(): Flow<List<RemittanceEntity>>

    @Insert
    suspend fun insert(remittance: RemittanceEntity)

    @Update
    suspend fun update(remittance: RemittanceEntity)

    @Delete
    suspend fun delete(remittance: RemittanceEntity)

    @Query("DELETE FROM remittances")
    suspend fun truncate()
}