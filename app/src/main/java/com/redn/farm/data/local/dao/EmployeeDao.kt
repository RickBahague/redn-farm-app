package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY employee_id DESC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Insert
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)

    @Query("SELECT * FROM employees WHERE employee_id = :id")
    suspend fun getEmployee(id: Int): EmployeeEntity?

    @Query("DELETE FROM employees")
    suspend fun truncate()
} 