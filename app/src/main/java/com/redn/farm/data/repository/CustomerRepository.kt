package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.CustomerDao
import com.redn.farm.data.local.entity.CustomerEntity
import com.redn.farm.data.model.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(private val customerDao: CustomerDao) {
    fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers().map { entities ->
            entities.map { it.toCustomer() }
        }
    }

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return customerDao.searchCustomers(query).map { entities ->
            entities.map { it.toCustomer() }
        }
    }

    suspend fun addCustomer(customer: Customer): Long {
        return customerDao.insertCustomer(customer.toEntity())
    }

    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer.toEntity().copy(
            date_updated = System.currentTimeMillis()
        ))
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer.toEntity())
    }

    suspend fun truncate() {
        customerDao.truncate()
    }

    suspend fun getCustomerById(id: Int): Customer? =
        customerDao.getById(id)?.toCustomer()

    private fun CustomerEntity.toCustomer() = Customer(
        customer_id = customer_id,
        firstname = firstname,
        lastname = lastname,
        contact = contact,
        customer_type = customer_type,
        address = address,
        city = city,
        province = province,
        postal_code = postal_code,
        date_created = date_created,
        date_updated = date_updated
    )

    private fun Customer.toEntity() = CustomerEntity(
        customer_id = customer_id,
        firstname = firstname,
        lastname = lastname,
        contact = contact,
        customer_type = customer_type,
        address = address,
        city = city,
        province = province,
        postal_code = postal_code,
        date_created = date_created,
        date_updated = date_updated
    )
} 