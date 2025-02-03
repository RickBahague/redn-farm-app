package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.FarmOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmOperationDao {
    @Query("SELECT * FROM farm_operations ORDER BY operation_date DESC")
    fun getAllOperations(): Flow<List<FarmOperationEntity>>

    @Query("""
        SELECT * FROM farm_operations 
        WHERE operation_type = :type 
        ORDER BY operation_date DESC
    """)
    fun getOperationsByType(type: String): Flow<List<FarmOperationEntity>>

    @Insert
    suspend fun insert(operation: FarmOperationEntity): Long

    @Update
    suspend fun update(operation: FarmOperationEntity)

    @Delete
    suspend fun delete(operation: FarmOperationEntity)

    @Query("DELETE FROM farm_operations")
    suspend fun truncate()

    @Query("""
        SELECT * FROM farm_operations 
        WHERE operation_date BETWEEN :startDate AND :endDate 
        ORDER BY operation_date DESC
    """)
    fun getOperationsInDateRange(startDate: Long, endDate: Long): Flow<List<FarmOperationEntity>>
} 