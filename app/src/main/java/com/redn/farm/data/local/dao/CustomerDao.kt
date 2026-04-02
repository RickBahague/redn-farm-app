package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY firstname ASC, lastname ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("""
        SELECT * FROM customers 
        WHERE firstname LIKE '%' || :query || '%' 
        OR lastname LIKE '%' || :query || '%'
        OR contact LIKE '%' || :query || '%'
        ORDER BY firstname ASC, lastname ASC
    """)
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers")
    suspend fun truncate()

    @Query("SELECT * FROM customers WHERE customer_id = :id LIMIT 1")
    suspend fun getById(id: Int): CustomerEntity?
} 