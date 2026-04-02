package com.redn.farm.ui.screens.manage.employees

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.model.Employee
import com.redn.farm.data.repository.EmployeeRepository
import com.redn.farm.security.Rbac
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ManageEmployeesViewModel(
    private val repository: EmployeeRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val employees = searchQuery
        .combine(repository.getAllEmployees()) { query, employees ->
            if (query.isEmpty()) {
                employees
            } else {
                employees.filter {
                    it.fullName.contains(query, ignoreCase = true) ||
                    it.contact.contains(query, ignoreCase = true) ||
                    it.formattedId.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addEmployee(firstname: String, lastname: String, contact: String) {
        viewModelScope.launch {
            if (!Rbac.canWriteEmployees(sessionManager.getRole())) return@launch
            repository.addEmployee(
                Employee(
                    firstname = firstname,
                    lastname = lastname,
                    contact = contact
                )
            )
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            if (!Rbac.canWriteEmployees(sessionManager.getRole())) return@launch
            repository.updateEmployee(
                employee.copy(
                    date_updated = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            if (!Rbac.canWriteEmployees(sessionManager.getRole())) return@launch
            repository.deleteEmployee(employee)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ManageEmployeesViewModel::class.java)) {
                val database = FarmDatabase.getDatabase(application)
                return ManageEmployeesViewModel(
                    EmployeeRepository(database.employeeDao()),
                    SessionManager(application)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 