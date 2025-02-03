package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.EmployeeDao
import com.redn.farm.data.local.entity.EmployeeEntity
import com.redn.farm.data.model.Employee
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EmployeeRepository(
    private val employeeDao: EmployeeDao
) {
    fun getAllEmployees(): Flow<List<Employee>> {
        return employeeDao.getAllEmployees().map { entities ->
            entities.map { it.toEmployee() }
        }
    }

    suspend fun addEmployee(employee: Employee) {
        employeeDao.insertEmployee(employee.toEntity())
    }

    suspend fun updateEmployee(employee: Employee) {
        employeeDao.updateEmployee(employee.toEntity())
    }

    suspend fun deleteEmployee(employee: Employee) {
        employeeDao.deleteEmployee(employee.toEntity())
    }

    suspend fun truncate() = employeeDao.truncate()

    private fun EmployeeEntity.toEmployee() = Employee(
        employee_id = employee_id,
        firstname = firstname,
        lastname = lastname,
        contact = contact,
        date_created = date_created,
        date_updated = date_updated
    )

    private fun Employee.toEntity() = EmployeeEntity(
        employee_id = employee_id,
        firstname = firstname,
        lastname = lastname,
        contact = contact,
        date_created = date_created,
        date_updated = date_updated
    )
} 